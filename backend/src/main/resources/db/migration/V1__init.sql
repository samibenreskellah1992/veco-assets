-- Phase 1: baseline migration. Proves Flyway is wired to the datasource and
-- reserves the extension used for UUID generation by later entity tables.
-- The full domain schema (users, roles, sites, assets, movements,
-- inventories, audit_logs, ...) is created in Phase 2 migrations.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
