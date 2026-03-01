#!/bin/sh
# 聊天室 CLI 客户端启动脚本（POSIX sh）。
# 仅使用 POSIX 语法，兼容 bash / zsh。
#
# 用法：
#   sh scripts/run-cli.sh [options]
#
# 参数（均可选）：
#   --host <host>                 服务器地址（默认：127.0.0.1）
#   --port <port>                 服务器端口（默认：9000）
#   --userId <id>                 启动后自动登录的 userId（默认：不自动登录）
#   --heartbeatIntervalSec <sec>  心跳间隔（秒，默认：5）
#   -h | --help                   显示帮助
#
# 说明：
# - 先构建：mvn -q -DskipTests package
# - 构建后会生成可执行 jar（client-cli/target/client-cli-*.jar）
# - 运行依赖会复制到 client-cli/target/lib

set -eu

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
JAR_FILE=$(find "$PROJECT_ROOT/client-cli/target" -maxdepth 1 -type f -name 'client-cli-*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -n 1)
LIB_DIR="$PROJECT_ROOT/client-cli/target/lib"

if [ -z "${JAR_FILE:-}" ] || [ ! -d "$LIB_DIR" ]; then
  echo "[ERROR] 未找到构建产物，请先执行：mvn -q -DskipTests package" >&2
  exit 1
fi

HOST=127.0.0.1
PORT=9000
USER_ID=""
HEARTBEAT_INTERVAL=5

while [ $# -gt 0 ]; do
  case "$1" in
    --host) HOST="$2"; shift 2 ;;
    --port) PORT="$2"; shift 2 ;;
    --userId) USER_ID="$2"; shift 2 ;;
    --heartbeatIntervalSec) HEARTBEAT_INTERVAL="$2"; shift 2 ;;
    -h|--help)
      sed -n '2,80p' "$0" | sed 's/^# //;s/^#//'
      exit 0
      ;;
    *)
      echo "[ERROR] 未知参数: $1" >&2
      exit 1
      ;;
  esac
done

ARGS="--host $HOST --port $PORT --heartbeatIntervalSec $HEARTBEAT_INTERVAL"
if [ -n "$USER_ID" ]; then
  ARGS="$ARGS --userId $USER_ID"
fi

JAVA_BIN=${JAVA_BIN:-java}
exec "$JAVA_BIN" -jar "$JAR_FILE" $ARGS
