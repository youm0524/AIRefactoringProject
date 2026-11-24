#!/usr/bin/env bash
set -euo pipefail

migration_dir="$VERIFICATION_BACKEND_DIR/src/main/resources/db/migration"
mysql_command=(mysql -h"$VERIFICATION_MYSQL_HOST" -P"$VERIFICATION_MYSQL_PORT" -u"$DB_USERNAME" "$VERIFICATION_DB_NAME")

for migration in \
    V1__create_core_tables.sql \
    V2__create_community_rooms.sql \
    V3__create_community_room_members.sql \
    V4__migrate_messages_to_community_rooms.sql; do
    MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" < "$migration_dir/$migration"
    if [[ $migration == V1__create_core_tables.sql ]]; then
        MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" --execute="
            INSERT INTO jobs (company, logo, color, title, category, type, location, deadline)
            VALUES
                ('충돌회사', 'A', '#000000', '공고 1', '개발', '신입', '서울', 'D-1'),
                ('충돌회사', 'B', '#ffffff', '공고 2', '개발', '신입', '서울', 'D-2');
        "
    fi
done

if MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" \
    < "$migration_dir/V5__normalize_company_relationships.sql" >/dev/null 2>&1; then
    echo "회사 정보 충돌 데이터에서 V5 마이그레이션이 성공했습니다." >&2
    exit 1
fi

echo "회사명은 같고 로고·색상이 다른 데이터의 V5 마이그레이션 실패 확인"
