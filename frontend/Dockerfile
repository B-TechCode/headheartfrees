# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Stage 1 - dependencies
# ---------------------------------------------------------------------------
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# ---------------------------------------------------------------------------
# Stage 2 - build
#
# IMPORTANT: NEXT_PUBLIC_* variables are inlined into the JavaScript bundle at
# BUILD time. They are not read from the environment at runtime, so pointing the
# app at a different API means rebuilding this image:
#
#   docker compose build --build-arg NEXT_PUBLIC_API_BASE_URL=https://api.example.com frontend
#
# Restarting the container with a new env var will NOT change the baked-in value.
# ---------------------------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /app

ARG NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL}
ENV NEXT_TELEMETRY_DISABLED=1

COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# ---------------------------------------------------------------------------
# Stage 3 - runtime
# Runs the standalone server bundle; no node_modules, no source.
# ---------------------------------------------------------------------------
FROM node:22-alpine AS runtime
WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

# `node` (uid 1000) ships with the image.
COPY --from=build --chown=node:node /app/public ./public
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static ./.next/static

USER node
EXPOSE 3000

CMD ["node", "server.js"]
