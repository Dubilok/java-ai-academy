# Java AI Academy — API Contract Summary

Base path: `/api/v1`. JSON only. Auth: `Authorization: Bearer <jwt>`.

## Public endpoints
- `POST /auth/register` — email + password → `{ accessToken, refreshToken }`
- `POST /auth/login` — credentials → `{ accessToken, refreshToken }`
- `POST /auth/refresh` — `{ refreshToken }` → new token pair
- `GET /courses` — paginated published courses `{ items: [], nextCursor }`

## Catalog (authenticated)
- `GET /courses/{id}` — course with module/lecture tree
- `GET /lectures/{id}` — markdown content + task stubs (no testCode)
- `GET /tasks/{id}` — task description + templateCode (**never** testCode)

## Submissions
- `POST /tasks/{id}/submissions` — `{ source }` → `202 { submissionId }`
- `GET /submissions/{id}` — `{ status: PENDING|PASSED|FAILED, logs }`

## Progress & profile
- `GET /me` — `{ id, email, role, xpPoints, crystals, level, streak, createdAt }`
- `GET /me/progress` — per-course `{ courseId, totalTasks, passedTasks, completionPercent }`

## Admin (ROLE_ADMIN)
- `POST /admin/ai/generate-course` — `{ technology }` → `{ jobId, technology, status: RUNNING }`
- `GET /admin/ai/jobs/{id}` — job status + courseId/errorMessage
- `GET /admin/ai/usage` — FinOps: token counts, cost by agent/user/course
- `GET /admin/mcp/tools` — list available MCP tools
- `POST /admin/mcp/tools/{name}/call` — `{ input: {...} }` → `{ toolName, output, isError }`

## Data shapes
- Cursor pagination: `?cursor=<base64>&limit=<n>` → `{ items: [], nextCursor: null|string }`
- Errors: RFC 7807 `application/problem+json` `{ type, title, status, detail }`
- XP formula: `level = min(50, floor(sqrt(xpPoints / 100)) + 1)`

## Task structure (admin/AI view)
```json
{
  "id": "uuid",
  "title": "string",
  "description": "string",
  "difficulty": "EASY|MEDIUM|HARD",
  "templateCode": "string",
  "testCode": "string (never student-visible)",
  "solutionCode": "string (never student-visible)",
  "xpReward": 100
}
```
