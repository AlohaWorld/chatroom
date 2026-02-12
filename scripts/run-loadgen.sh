#!/bin/sh
# 聊天室 LoadGen 压测工具启动脚本（POSIX sh）。
# 仅使用 POSIX 语法，兼容 bash / zsh。
#
# 用法：
#   sh scripts/run-loadgen.sh [options]
#
# 参数（均可选）：
#   --host <host>        服务器地址（默认：127.0.0.1）
#   --port <port>        服务器端口（默认：9000）
#   --clients <n>        并发客户端数量（默认：200）
#   --msgRate <rate>     每秒发送消息数（默认：2000）
#   --durationSec <sec>  压测时长（秒，默认：30）
#   --payloadSize <n>    消息负载大小（字节，默认：128）
#   -h | --help          显示帮助
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
CLIENTS=200
MSG_RATE=2000
DURATION=30
PAYLOAD=128

while [ $# -gt 0 ]; do
  case "$1" in
    --host) HOST="$2"; shift 2 ;;
    --port) PORT="$2"; shift 2 ;;
    --clients) CLIENTS="$2"; shift 2 ;;
    --msgRate) MSG_RATE="$2"; shift 2 ;;
    --durationSec) DURATION="$2"; shift 2 ;;
    --payloadSize) PAYLOAD="$2"; shift 2 ;;
    -h|--help)
      sed -n '1,80p' "$0" | sed 's/^# //;s/^#//'
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
exec "$JAVA_BIN" -cp "$CP" com.example.chatroom.tools.LoadGenMain \
  --host "$HOST" --port "$PORT" --clients "$CLIENTS" --msgRate "$MSG_RATE" \
  --durationSec "$DURATION" --payloadSize "$PAYLOAD"
