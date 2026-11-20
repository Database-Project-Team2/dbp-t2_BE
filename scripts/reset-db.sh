#!/usr/bin/env bash
# DB를 db/init 시드 상태로 되돌린다. 컨테이너를 내리지 않고 테이블만 다시 만든다(실행 중인 서버의 커넥션 유지).
# 사용: scripts/reset-db.sh [컨테이너 이름]   (기본 dbp-t2-mysql)
set -euo pipefail
cd "$(dirname "$0")/.."
CONTAINER="${1:-dbp-t2-mysql}"
{
  echo "SET FOREIGN_KEY_CHECKS=0;"
  echo "USE employee_project_management;"
  echo "DROP TABLE IF EXISTS EVALUATION, PROJECT_MEMBER, PROJECT, CLIENT, EMPLOYEE;"
  echo "SET FOREIGN_KEY_CHECKS=1;"
  cat db/init/01_schema.sql db/init/02_seed.sql
} | docker exec -i "$CONTAINER" sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" 2>/dev/null'
echo "DB를 시드 상태로 초기화했습니다."
