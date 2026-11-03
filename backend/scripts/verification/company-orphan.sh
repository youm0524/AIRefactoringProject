#!/usr/bin/env bash
set -euo pipefail

migration_dir="$VERIFICATION_BACKEND_DIR/src/main/resources/db/migration"
mysql_command=(mysql -h"$VERIFICATION_MYSQL_HOST" -P"$VERIFICATION_MYSQL_PORT" -u"$DB_USERNAME" "$VERIFICATION_DB_NAME")

MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" < "$migration_dir/V1__create_core_tables.sql"
MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" --execute="
    INSERT INTO jobs (company, logo, color, title, category, type, location, deadline)
    VALUES ('고아회사', 'O', '#000000', '공고', '개발', '신입', '서울', 'D-1');
"
MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" < "$migration_dir/V2__create_community_rooms.sql"
MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" --execute="DELETE FROM jobs;"
MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" < "$migration_dir/V3__create_community_room_members.sql"
MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" < "$migration_dir/V4__migrate_messages_to_community_rooms.sql"

if MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" \
    < "$migration_dir/V5__normalize_company_relationships.sql" >/dev/null 2>&1; then
    echo "공고 없는 방 데이터에서 V5 마이그레이션이 성공했습니다." >&2
    exit 1
fi

echo "공고 없는 방 데이터의 V5 마이그레이션 실패 확인"
