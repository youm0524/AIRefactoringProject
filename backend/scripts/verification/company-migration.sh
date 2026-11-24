#!/usr/bin/env bash
set -euo pipefail

migration_dir="$VERIFICATION_BACKEND_DIR/src/main/resources/db/migration"
mysql_command=(mysql -h"$VERIFICATION_MYSQL_HOST" -P"$VERIFICATION_MYSQL_PORT" -u"$DB_USERNAME" "$VERIFICATION_DB_NAME")

MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" < "$migration_dir/V1__create_core_tables.sql"
MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" --execute="
    INSERT INTO users (name, email, password_hash)
    VALUES ('검증사용자', 'verify@careerpick.kr', 'hash');

    INSERT INTO jobs (company, logo, color, title, category, type, location, deadline)
    VALUES
        ('같은회사', 'S', '#111111', '공고 1', '개발', '신입', '서울', 'D-1'),
        ('같은회사', 'S', '#111111', '공고 2', '개발', '경력', '서울', 'D-2'),
        ('다른회사', 'D', '#222222', '공고 3', '마케팅', '인턴', '경기', 'D-3');

    INSERT INTO chat_messages (job_id, user_id, author, text, created_at)
    VALUES
        (1, 1, '검증사용자', '첫 메시지', UTC_TIMESTAMP(6)),
        (2, 1, '검증사용자', '두 번째 메시지', UTC_TIMESTAMP(6)),
        (3, 1, '검증사용자', '세 번째 메시지', UTC_TIMESTAMP(6));
"
MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" < "$migration_dir/V2__create_community_rooms.sql"
MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" < "$migration_dir/V3__create_community_room_members.sql"
MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" < "$migration_dir/V4__migrate_messages_to_community_rooms.sql"

snapshot_before=$(MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" --batch --skip-column-names --execute="
    SELECT CONCAT_WS('|',
        (SELECT COUNT(*) FROM jobs),
        (SELECT GROUP_CONCAT(id ORDER BY id) FROM jobs),
        (SELECT COUNT(*) FROM community_rooms),
        (SELECT GROUP_CONCAT(id ORDER BY id) FROM community_rooms),
        (SELECT COUNT(*) FROM community_room_members),
        (SELECT GROUP_CONCAT(id ORDER BY id) FROM community_room_members),
        (SELECT COUNT(*) FROM chat_messages),
        (SELECT GROUP_CONCAT(id ORDER BY id) FROM chat_messages));
")

MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" < "$migration_dir/V5__normalize_company_relationships.sql"

snapshot_after=$(MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" --batch --skip-column-names --execute="
    SELECT CONCAT_WS('|',
        (SELECT COUNT(*) FROM jobs),
        (SELECT GROUP_CONCAT(id ORDER BY id) FROM jobs),
        (SELECT COUNT(*) FROM community_rooms),
        (SELECT GROUP_CONCAT(id ORDER BY id) FROM community_rooms),
        (SELECT COUNT(*) FROM community_room_members),
        (SELECT GROUP_CONCAT(id ORDER BY id) FROM community_room_members),
        (SELECT COUNT(*) FROM chat_messages),
        (SELECT GROUP_CONCAT(id ORDER BY id) FROM chat_messages));
")

read -r companies shared_company mismatches duplicate_rooms legacy_columns < <(
    MYSQL_PWD=$DB_PASSWORD "${mysql_command[@]}" --batch --skip-column-names --execute="
        SELECT
            (SELECT COUNT(*) FROM companies),
            (SELECT COUNT(DISTINCT company_id) FROM jobs WHERE id IN (1, 2)),
            (SELECT COUNT(*) FROM jobs job LEFT JOIN community_rooms room ON room.company_id = job.company_id WHERE room.id IS NULL),
            (SELECT COUNT(*) FROM (SELECT company_id FROM community_rooms GROUP BY company_id HAVING COUNT(*) > 1) duplicate),
            (SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND ((table_name = 'jobs' AND column_name IN ('company', 'logo', 'color', 'community_room_id'))
                    OR (table_name = 'community_rooms' AND column_name = 'company_name')));
    "
)

if [[ $snapshot_before != "$snapshot_after" || $companies -ne 2 || $shared_company -ne 1 ||
      $mismatches -ne 0 || $duplicate_rooms -ne 0 || $legacy_columns -ne 0 ]]; then
    echo "V5 기존 데이터 이관 검증이 실패했습니다." >&2
    exit 1
fi

echo "V5 공고·방·참여·메시지 수와 ID 보존, 회사 관계 이관 확인"
