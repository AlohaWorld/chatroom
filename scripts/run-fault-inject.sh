#!/bin/sh
# 聊天室 Fault-Inject 故障注入工具启动脚本（POSIX sh）。
# 仅使用 POSIX 语法，兼容 bash / zsh。
#
# 用法：
#   sh scripts/run-fault-inject.sh [options]
#
# 参数（均可选）：
#   --host <host>   服务器地址（默认：127.0.0.1）
#   --port <port>   服务器端口（默认：9000）
#   --case <name>   注入的故障类型（默认：all）
#                  支持：
#                  - missing_fields   缺失必要字段
#                  - unknown_type     未知 msgType
#                  - bad_length       错误长度字段
#                  - too_large        超大帧
#                  - disconnect       连接后立即断开
#   -h | --help     显示帮助
#
# 说明：
# - 先构建：mvn -q -DskipTests package
# - 运行时依赖从 tools/target/classpath.txt 读取

set -eu

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
CLASSPATH_FILE="$PROJECT_ROOT/tools/target/classpath.txt"
CLASSES_DIR="$PROJECT_ROOT/tools/target/classes"

if [ ! -d "$CLASSES_DIR" ] || [ ! -f "$CLASSPATH_FILE" ]; then
  echo "[ERROR] 未找到构建产物，请先执行：mvn -q -DskipTests package" >&2
  exit 1
fi

HOST=127.0.0.1
PORT=9000
CASE=all

while [ $# -gt 0 ]; do
  case "$1" in
    --host) HOST="$2"; shift 2 ;;
    --port) PORT="$2"; shift 2 ;;
    --case) CASE="$2"; shift 2 ;;
    -h|--help)
      sed -n '1,120p' "$0" | sed 's/^# //;s/^#//'
      exit 0
      ;;
    *)
      echo "[ERROR] 未知参数: $1" >&2
      exit 1
      ;;
  esac
done

DEPS_CP=$(cat "$CLASSPATH_FILE")
CP="$CLASSES_DIR:$DEPS_CP"

JAVA_BIN=${JAVA_BIN:-java}
exec "$JAVA_BIN" -cp "$CP" com.example.chatroom.tools.FaultInjectMain \
  --host "$HOST" --port "$PORT" --case "$CASE"
