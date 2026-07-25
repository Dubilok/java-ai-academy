# Java AI Academy — Operations Runbook

**Audience:** On-call engineers and maintainers.
**Scope:** Backup, restore, routine ops, and incident playbooks for the production deployment.

---

## 1. Service Inventory

| Service | Technology | Port | State |
|---|---|---|---|
| `academy-api` | Spring Boot 3.3 (JRE 21) | 8080 | stateless |
| `academy-frontend` | Next.js 14 (Node 20) | 3000 | stateless |
| `academy-postgres` | PostgreSQL 16 | 5432 | stateful — primary data store |
| `academy-redis` | Redis 7 | 6379 | semi-stateful — cache + queues |
| Docker daemon | host | — | sandbox execution engine |

---

## 2. Backup

### 2.1 PostgreSQL full backup

```bash
# Dump to a timestamped file
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="backup_${TIMESTAMP}.sql.gz"

docker exec academy-postgres pg_dump \
  -U "${POSTGRES_USER}" \
  -d "${POSTGRES_DB}" \
  --format=plain \
  --no-owner \
  --no-acl \
  | gzip > "/backups/postgres/${BACKUP_FILE}"

echo "Backup written to /backups/postgres/${BACKUP_FILE}"
ls -lh "/backups/postgres/${BACKUP_FILE}"
```

**Schedule:** run nightly at 02:00 UTC via cron or a CI job. Keep 30 daily backups.

### 2.2 PostgreSQL continuous WAL archiving (production)

For production, enable WAL archiving in `postgresql.conf`:

```
wal_level = replica
archive_mode = on
archive_command = 'aws s3 cp %p s3://your-bucket/wal/%f'
```

This enables point-in-time recovery (PITR) and reduces RPO to seconds.

### 2.3 Redis backup

Redis uses append-only persistence (`appendonly yes`). For a consistent snapshot:

```bash
# Force a BGSAVE and copy the dump file
docker exec academy-redis redis-cli BGSAVE
sleep 5  # wait for background save to complete
docker cp academy-redis:/data/appendonly.aof "/backups/redis/appendonly_$(date +%Y%m%d_%H%M%S).aof"
```

Redis data is reconstructible from the database (submissions, XP, progress) so Redis backups are secondary priority.

### 2.4 Backup verification

Run weekly:

```bash
# Restore to a throwaway container and count tables
LATEST=$(ls -t /backups/postgres/*.sql.gz | head -1)
docker run --rm -e POSTGRES_PASSWORD=test -e POSTGRES_USER=test \
  postgres:16-alpine \
  sh -c "initdb -D /tmp/pg && pg_ctl -D /tmp/pg start \
    && gunzip -c ${LATEST} | psql -U test \
    && psql -U test -c '\dt' | wc -l"
```

A successful backup should list at least 14 tables.

---

## 3. Restore

### 3.1 Restore PostgreSQL from full dump

**Stop the API first** to avoid write conflicts during restore:

```bash
# 1. Stop the Spring Boot API (stateless, safe to kill)
docker compose stop academy-api

# 2. Drop and recreate the database
docker exec -it academy-postgres psql -U "${POSTGRES_USER}" \
  -c "DROP DATABASE IF EXISTS ${POSTGRES_DB};" \
  -c "CREATE DATABASE ${POSTGRES_DB};"

# 3. Restore from the chosen backup
BACKUP_FILE="/backups/postgres/backup_20260725_020001.sql.gz"
gunzip -c "${BACKUP_FILE}" | docker exec -i academy-postgres psql \
  -U "${POSTGRES_USER}" \
  -d "${POSTGRES_DB}"

# 4. Verify row counts in key tables
docker exec -it academy-postgres psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" \
  -c "SELECT schemaname, tablename, n_live_tup FROM pg_stat_user_tables ORDER BY n_live_tup DESC LIMIT 10;"

# 5. Restart the API
docker compose start academy-api
```

### 3.2 Restore Redis

```bash
# Stop Redis to prevent writes during restore
docker compose stop academy-redis

# Copy the AOF file into the container's data volume
docker run --rm \
  --volumes-from academy-redis \
  -v /backups/redis:/backup \
  alpine cp /backup/appendonly_20260725_020001.aof /data/appendonly.aof

# Restart Redis — it will replay the AOF on startup
docker compose start academy-redis

# Confirm Redis is healthy
docker exec academy-redis redis-cli ping  # → PONG
```

### 3.3 Point-in-time recovery (PostgreSQL WAL)

To restore to a specific timestamp (requires WAL archiving enabled):

```bash
# Download WAL files from S3
aws s3 sync s3://your-bucket/wal/ /var/lib/postgresql/wal_archive/

# Edit recovery.conf (PostgreSQL 16+: recovery_target_time in postgresql.conf)
cat >> /var/lib/postgresql/data/postgresql.conf <<EOF
restore_command = 'cp /var/lib/postgresql/wal_archive/%f %p'
recovery_target_time = '2026-07-25 03:30:00 UTC'
recovery_target_action = 'promote'
EOF

# Restart Postgres in recovery mode
docker compose restart academy-postgres
# Monitor logs until "database system is ready"
docker logs -f academy-postgres
```

---

## 4. Routine Operations

### 4.1 Rolling restart (zero-downtime)

The API and frontend are stateless; restart them freely:

```bash
docker compose pull academy-api academy-frontend
docker compose up -d --no-deps academy-api academy-frontend
docker compose ps
```

### 4.2 Database migration (Flyway)

Flyway runs automatically on API startup. To see the migration status:

```bash
docker exec academy-api java -jar app.jar --spring.flyway.validate-on-migrate=true
# Or inspect directly:
docker exec -it academy-postgres psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" \
  -c "SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;"
```

**Never edit an applied migration.** Add a new `V{n}__description.sql` file.

### 4.3 Orphaned container cleanup

The sandbox reaper runs inside the API process, but if the API crashes mid-execution:

```bash
# List any leaked runner containers
docker ps -a --filter "ancestor=java-ai-academy/runner:21" --format "{{.ID}} {{.Status}}"

# Kill and remove all of them
docker ps -a --filter "ancestor=java-ai-academy/runner:21" -q | xargs -r docker rm -f

echo "Cleanup done"
docker ps -a --filter "ancestor=java-ai-academy/runner:21"  # should be empty
```

### 4.4 Log rotation

API and frontend log to stdout (captured by Docker). Configure Docker's log driver:

```json
// /etc/docker/daemon.json
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "100m",
    "max-file": "5"
  }
}
```

Restart Docker after changing daemon config: `sudo systemctl restart docker`.

### 4.5 Check API health

```bash
curl -s http://localhost:8080/actuator/health | jq .
# Expected: {"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"}}}
```

---

## 5. Incident Playbooks

### 5.1 API returns 5xx errors

```bash
# 1. Check recent logs
docker logs --since 30m academy-api | grep -E "ERROR|WARN"

# 2. Check database connectivity
curl -s http://localhost:8080/actuator/health | jq '.components.db'

# 3. Check Redis connectivity
docker exec academy-redis redis-cli ping

# 4. Check disk space (write failures)
df -h /

# 5. Restart the API if the cause is a transient error
docker compose restart academy-api
```

### 5.2 Sandbox containers leaking

```bash
# Count active runner containers
LEAKED=$(docker ps -a --filter "ancestor=java-ai-academy/runner:21" -q | wc -l)
echo "Leaked containers: $LEAKED"

if [ "$LEAKED" -gt "0" ]; then
  docker ps -a --filter "ancestor=java-ai-academy/runner:21" --format "{{.ID}} {{.CreatedAt}} {{.Status}}"
  # Remove containers older than 2 minutes (the reaper should have caught them)
  docker ps -a --filter "ancestor=java-ai-academy/runner:21" \
    --format "{{.ID}} {{.CreatedAt}}" | \
    awk '{if ($2" "$3 <= strftime("%Y-%m-%d %H:%M", systime()-120)) print $1}' | \
    xargs -r docker rm -f
fi
```

### 5.3 Database disk full

```bash
# Check PostgreSQL data directory size
docker exec academy-postgres du -sh /var/lib/postgresql/data

# Identify largest tables
docker exec -it academy-postgres psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -c "
SELECT relname AS table,
       pg_size_pretty(pg_total_relation_size(relid)) AS total_size
FROM pg_catalog.pg_statio_user_tables
ORDER BY pg_total_relation_size(relid) DESC LIMIT 10;"

# submissions and ai_generation_log can grow large — archive old records:
# (run in psql)
DELETE FROM submissions
WHERE created_at < NOW() - INTERVAL '90 days'
  AND status IN ('PASSED', 'FAILED');
VACUUM ANALYZE submissions;
```

### 5.4 Redis memory limit hit

```bash
# Check memory usage
docker exec academy-redis redis-cli INFO memory | grep -E "used_memory_human|maxmemory"

# Flush rate-limit keys (safe — they will regenerate on next request)
docker exec academy-redis redis-cli --scan --pattern "rate:*" | xargs -r docker exec academy-redis redis-cli DEL

# If submission queue is backed up:
docker exec academy-redis redis-cli LLEN sandbox:submission-queue
```

### 5.5 JWT secret rotation

Rotating the JWT secret invalidates all active sessions — users must log in again.

```bash
# 1. Generate a new secret
NEW_SECRET=$(openssl rand -base64 48)
echo "New JWT_SECRET: $NEW_SECRET"

# 2. Update the secret in your platform's secret store
#    (e.g., GitHub repo secret, AWS Secrets Manager, .env on server)

# 3. Revoke all refresh tokens to force clean login
docker exec -it academy-postgres psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" \
  -c "UPDATE refresh_tokens SET revoked = true;"

# 4. Restart the API with the new secret
docker compose up -d --no-deps academy-api
```

---

## 6. Metrics and Monitoring

Key Prometheus metrics exposed at `/actuator/prometheus`:

| Metric | Alert threshold | What it means |
|---|---|---|
| `academy_submission_duration_seconds_p95` | > 5s | Sandbox is too slow — check Docker daemon |
| `academy_sandbox_failures_total` (rate) | > 10/min | Containers failing abnormally |
| `academy_ai_tokens_total` (rate) | > budget | AI spend spike — check `/admin/ai/usage` |
| `jvm_memory_used_bytes` | > 80% of limit | Tune `JAVA_OPTS` or increase pod memory |
| `hikaricp_connections_active` | > 90% of pool | DB connection pool exhausted |

---

## 7. Contact and Escalation

- On-call rotation: configure in your alerting platform (PagerDuty / OpsGenie).
- For AI provider outages: check status.anthropic.com and status.ai.google.dev.
- For Docker daemon issues: restart Docker Desktop or `sudo systemctl restart docker` on Linux.
