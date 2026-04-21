#!/usr/bin/env bash
set -euo pipefail

# ===============================
# StudySystem migration template
# Run on OLD server to migrate to NEW server
# ===============================

# -------- Required params --------
NEW_SERVER_IP="117.72.56.226"
NEW_SERVER_USER="root"
NEW_SERVER_DEPLOY_DIR="/opt/studysystem"

OLD_DEPLOY_DIR="/opt/studysystem"
PROJECT_ROOT_ON_OLD="${OLD_DEPLOY_DIR}"
COMPOSE_DIR_ON_OLD="${PROJECT_ROOT_ON_OLD}/deploy"

# Optional: if SSH port is not 22
SSH_PORT="22"

# -------- Runtime vars --------
TS="$(date +%Y%m%d_%H%M%S)"
BACKUP_DIR="/tmp/studysystem_migration_${TS}"
mkdir -p "${BACKUP_DIR}"

echo "[1/7] Check required commands..."
for cmd in docker scp ssh tar; do
  command -v "${cmd}" >/dev/null 2>&1 || { echo "Missing command: ${cmd}"; exit 1; }
done

echo "[2/7] Export MySQL data from container..."
cd "${COMPOSE_DIR_ON_OLD}"
source .env
docker compose exec -T mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --databases "$MYSQL_DATABASE"' > "${BACKUP_DIR}/db.sql"

echo "[3/7] Package deploy files..."
tar -czf "${BACKUP_DIR}/deploy_bundle.tar.gz" \
  -C "${PROJECT_ROOT_ON_OLD}" deploy stu_backend/sql

echo "[4/7] Upload backup package to new server..."
scp -P "${SSH_PORT}" "${BACKUP_DIR}/deploy_bundle.tar.gz" "${BACKUP_DIR}/db.sql" \
  "${NEW_SERVER_USER}@${NEW_SERVER_IP}:/tmp/"

echo "[5/7] Restore files on new server..."
ssh -p "${SSH_PORT}" "${NEW_SERVER_USER}@${NEW_SERVER_IP}" "mkdir -p '${NEW_SERVER_DEPLOY_DIR}' && tar -xzf /tmp/deploy_bundle.tar.gz -C '${NEW_SERVER_DEPLOY_DIR}'"

echo "[6/7] Start services and import database on new server..."
ssh -p "${SSH_PORT}" "${NEW_SERVER_USER}@${NEW_SERVER_IP}" "bash -s" <<'EOF'
set -euo pipefail
NEW_SERVER_DEPLOY_DIR="/opt/studysystem"
cd "${NEW_SERVER_DEPLOY_DIR}/deploy"

if [ ! -f .env ]; then
  cp .env.example .env
  echo "Please edit ${NEW_SERVER_DEPLOY_DIR}/deploy/.env first, then rerun this script."
  exit 1
fi

docker compose up -d --build
source .env
cat /tmp/db.sql | docker compose exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD"'
docker compose restart backend
EOF

echo "[7/7] Migration done."
echo "Next step: point your domain DNS A record to ${NEW_SERVER_IP}."
