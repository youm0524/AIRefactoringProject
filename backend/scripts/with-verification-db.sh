#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 || ! $1 =~ ^[a-z0-9][a-z0-9-]*$ ]]; then
    echo "사용법: $0 <verification-name>" >&2
    exit 2
fi

verification_name=$1
script_dir=$(cd "$(dirname "$0")" && pwd -P)
backend_dir=$(cd "$script_dir/.." && pwd -P)
scenario="$script_dir/verification/$verification_name.sh"

if [[ ! -f $scenario ]]; then
    echo "검증 시나리오가 없습니다: $scenario" >&2
    exit 2
fi

verification_mysql_host=${VERIFICATION_MYSQL_HOST:-127.0.0.1}
verification_mysql_port=${VERIFICATION_MYSQL_PORT:-3307}
verification_root_user=${VERIFICATION_ROOT_USER:-root}
verification_root_password=${VERIFICATION_ROOT_PASSWORD:-root}
verification_app_user=${VERIFICATION_APP_USER:-careerpick}
verification_app_password=${VERIFICATION_APP_PASSWORD:-careerpick}
verification_db_key=${verification_name//-/_}
verification_db_name="careerpick_verify_${verification_db_key}_$(date +%Y%m%d%H%M%S)_$$"

if [[ $verification_db_name != careerpick_verify_* || ! $verification_app_user =~ ^[a-zA-Z0-9_]+$ ]]; then
    echo "안전하지 않은 검증 DB 설정입니다." >&2
    exit 2
fi

cleanup() {
    MYSQL_PWD=$verification_root_password mysql \
        -h"$verification_mysql_host" -P"$verification_mysql_port" -u"$verification_root_user" \
        -e "DROP DATABASE IF EXISTS \`$verification_db_name\`;" >/dev/null
}
trap cleanup EXIT

MYSQL_PWD=$verification_root_password mysql \
    -h"$verification_mysql_host" -P"$verification_mysql_port" -u"$verification_root_user" \
    -e "CREATE DATABASE \`$verification_db_name\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; GRANT ALL PRIVILEGES ON \`$verification_db_name\`.* TO '$verification_app_user'@'%';"

export DB_URL="jdbc:mysql://$verification_mysql_host:$verification_mysql_port/$verification_db_name?serverTimezone=Asia/Seoul&characterEncoding=UTF-8"
export DB_USERNAME=$verification_app_user
export DB_PASSWORD=$verification_app_password
export SERVER_PORT=$((18000 + $$ % 1000))
export VERIFICATION_DB_NAME=$verification_db_name
export VERIFICATION_MYSQL_HOST=$verification_mysql_host
export VERIFICATION_MYSQL_PORT=$verification_mysql_port
export VERIFICATION_BACKEND_DIR=$backend_dir

bash "$scenario"
