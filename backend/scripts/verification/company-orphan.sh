#!/usr/bin/env bash
set -euo pipefail

migration_dir="$VERIFICATION_BACKEND_DIR/src/main/resources/db/migration"
mysql_command=(mysql -h"$VERIFICATION_MYSQL_HOST" -P"$VERIFICATION_MYSQL_PORT" -u"$DB_USERNAME" "$VERIFICATION_DB_NAME")

MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" < "$migration_dir/V1__create_core_tables.sql"
MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" < "$migration_dir/V2__create_cover_letter_tables.sql"
MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" --execute="
    INSERT INTO jobs (company, logo, color, title, category, type, location, deadline)
    VALUES ('고아회사', 'O', '#000000', '공고', '개발', '신입', '서울', 'D-1');
"
MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" < "$migration_dir/V3__create_company_community_schema.sql"

if MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" --execute="
    INSERT INTO community_rooms (company_id, created_at)
    VALUES (999999, UTC_TIMESTAMP(6));
" >/dev/null 2>&1; then
    echo "회사 없는 방 데이터 삽입이 성공했습니다." >&2
    exit 1
fi

echo "회사 없는 방 데이터의 FK 거부 확인"
