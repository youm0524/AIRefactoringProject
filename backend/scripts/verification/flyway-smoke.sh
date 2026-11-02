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

if [[ $verification_migrations -eq 0 || $verification_failures -ne 0 ]]; then
    echo "Flyway 적용 결과가 올바르지 않습니다: 성공 ${verification_migrations}, 실패 ${verification_failures}" >&2
    exit 1
fi

echo "Flyway ${verification_migrations}개, Hibernate 매핑과 OpenAPI 기동 검증 완료"
