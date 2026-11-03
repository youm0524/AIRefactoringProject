#!/usr/bin/env bash
set -euo pipefail

verification_log="$VERIFICATION_BACKEND_DIR/build/verification-app.log"
verification_app_pid=

stop_app() {
    if [[ -n $verification_app_pid ]] && kill -0 "$verification_app_pid" 2>/dev/null; then
        kill "$verification_app_pid"
        wait "$verification_app_pid" 2>/dev/null || true
    fi
}
trap stop_app EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

(
    cd "$VERIFICATION_BACKEND_DIR"
    ./gradlew bootRun >"$verification_log" 2>&1
) &
verification_app_pid=$!

verification_ready=false
for _ in {1..30}; do
    if curl -fsS "http://127.0.0.1:$SERVER_PORT/v3/api-docs" >/dev/null 2>&1; then
        verification_ready=true
        break
    fi
    if ! kill -0 "$verification_app_pid" 2>/dev/null; then
        break
    fi
    sleep 1
done

if [[ $verification_ready != true ]]; then
    tail -80 "$verification_log" >&2
    exit 1
fi

verification_migrations=$(MYSQL_PWD=$DB_PASSWORD mysql \
    -h"$VERIFICATION_MYSQL_HOST" -P"$VERIFICATION_MYSQL_PORT" -u"$DB_USERNAME" "$VERIFICATION_DB_NAME" \
    --batch --skip-column-names \
    -e "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1;")
verification_failures=$(MYSQL_PWD=$DB_PASSWORD mysql \
    -h"$VERIFICATION_MYSQL_HOST" -P"$VERIFICATION_MYSQL_PORT" -u"$DB_USERNAME" "$VERIFICATION_DB_NAME" \
    --batch --skip-column-names \
    -e "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0;")
verification_v5=$(MYSQL_PWD=$DB_PASSWORD mysql \
    -h"$VERIFICATION_MYSQL_HOST" -P"$VERIFICATION_MYSQL_PORT" -u"$DB_USERNAME" "$VERIFICATION_DB_NAME" \
    --batch --skip-column-names \
    -e "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '5' AND success = 1;")

if [[ $verification_migrations -eq 0 || $verification_failures -ne 0 || $verification_v5 -ne 1 ]]; then
    echo "Flyway 적용 결과가 올바르지 않습니다: 성공 ${verification_migrations}, 실패 ${verification_failures}" >&2
    exit 1
fi

read -r verification_companies verification_jobs verification_rooms verification_mismatches verification_legacy_columns < <(
    MYSQL_PWD=$DB_PASSWORD mysql \
        -h"$VERIFICATION_MYSQL_HOST" -P"$VERIFICATION_MYSQL_PORT" -u"$DB_USERNAME" "$VERIFICATION_DB_NAME" \
        --batch --skip-column-names \
        -e "
            SELECT
                (SELECT COUNT(*) FROM companies),
                (SELECT COUNT(*) FROM jobs),
                (SELECT COUNT(*) FROM community_rooms),
                (SELECT COUNT(*) FROM jobs job LEFT JOIN community_rooms room ON room.company_id = job.company_id WHERE room.id IS NULL),
                (SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = DATABASE()
                      AND ((table_name = 'jobs' AND column_name IN ('company', 'logo', 'color', 'community_room_id'))
                        OR (table_name = 'community_rooms' AND column_name = 'company_name')));
        "
)

if [[ $verification_companies -ne $verification_rooms || $verification_jobs -eq 0 ||
      $verification_mismatches -ne 0 || $verification_legacy_columns -ne 0 ]]; then
    echo "회사 관계 이관 결과가 올바르지 않습니다: 회사 ${verification_companies}, 공고 ${verification_jobs}, 방 ${verification_rooms}, 불일치 ${verification_mismatches}, 기존 컬럼 ${verification_legacy_columns}" >&2
    exit 1
fi

verification_api_url="http://127.0.0.1:$SERVER_PORT/api"
verification_token=$(curl -fsS -X POST "$verification_api_url/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"demo@careerpick.kr","password":"career1234"}' | jq -r '.token')
verification_jobs_response=$(curl -fsS "$verification_api_url/jobs")
verification_job_id=$(jq -r '.[0].id' <<<"$verification_jobs_response")
verification_company_id=$(jq -r '.[0].companyId' <<<"$verification_jobs_response")
verification_join_response=$(curl -fsS -X PUT "$verification_api_url/jobs/$verification_job_id/community-membership" \
    -H "Authorization: Bearer $verification_token")
verification_room_id=$(jq -r '.room.id' <<<"$verification_join_response")
verification_join_company_id=$(jq -r '.room.companyId' <<<"$verification_join_response")
verification_rooms_response=$(curl -fsS "$verification_api_url/community-rooms" \
    -H "Authorization: Bearer $verification_token")
verification_joined_rooms=$(jq 'length' <<<"$verification_rooms_response")
verification_list_company_id=$(jq -r '.[0].companyId' <<<"$verification_rooms_response")
verification_messages=$(curl -fsS "$verification_api_url/community-rooms/$verification_room_id/messages" \
    -H "Authorization: Bearer $verification_token" | jq 'length')
verification_leave_status=$(curl -sS -o /dev/null -w '%{http_code}' -X DELETE \
    "$verification_api_url/community-rooms/$verification_room_id/membership" \
    -H "Authorization: Bearer $verification_token")
verification_rooms_after_leave=$(curl -fsS "$verification_api_url/community-rooms" \
    -H "Authorization: Bearer $verification_token" | jq 'length')
verification_rejoined_room_id=$(curl -fsS -X PUT "$verification_api_url/jobs/$verification_job_id/community-membership" \
    -H "Authorization: Bearer $verification_token" | jq -r '.room.id')

if [[ ! $verification_company_id =~ ^[0-9]+$ ||
      ! $verification_join_company_id =~ ^[0-9]+$ ||
      ! $verification_list_company_id =~ ^[0-9]+$ ||
      $verification_company_id -ne $verification_join_company_id ||
      $verification_company_id -ne $verification_list_company_id ||
      $verification_joined_rooms -ne 1 || $verification_messages -eq 0 ||
      $verification_leave_status -ne 204 || $verification_rooms_after_leave -ne 0 ||
      $verification_rejoined_room_id -ne $verification_room_id ]]; then
    echo "커뮤니티 API 회귀 검증이 실패했습니다." >&2
    exit 1
fi

curl -fsS -o /dev/null -X DELETE "$verification_api_url/community-rooms/$verification_room_id/membership" \
    -H "Authorization: Bearer $verification_token"

echo "Flyway ${verification_migrations}개, 회사 ${verification_companies}개 관계와 커뮤니티 API 회귀 검증 완료"
