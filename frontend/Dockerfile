# ── stage 1: dependency install ───────────────────────────────────────────────
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci --ignore-scripts

# ── stage 2: build ────────────────────────────────────────────────────────────
FROM node:20-alpine AS build
WORKDIR /app

# Copy installed modules from deps stage
COPY --from=deps /app/node_modules ./node_modules

# Copy source
COPY . .

# Disable telemetry in CI/container builds
ENV NEXT_TELEMETRY_DISABLED=1

RUN npm run build

# ── stage 3: runtime ──────────────────────────────────────────────────────────
FROM node:20-alpine AS runtime

# Run as non-root
RUN addgroup --gid 1001 academy && adduser --uid 1001 --ingroup academy --disabled-password academy
USER academy

WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

# Copy only what Next.js standalone output needs
COPY --from=build --chown=academy:academy /app/public ./public
COPY --from=build --chown=academy:academy /app/.next/standalone ./
COPY --from=build --chown=academy:academy /app/.next/static ./.next/static

EXPOSE 3000

ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

CMD ["node", "server.js"]
