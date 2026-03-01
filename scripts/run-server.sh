#!/bin/sh
# 聊天室 Server 启动脚本（POSIX sh）。
# 仅使用 POSIX 语法，兼容 bash / zsh。
#
# 用法：
#   sh scripts/run-server.sh [options]
#
# 参数（均可选）：
#   --port <port>                    服务端 TCP 端口（默认：9000）
#   --mode <naive|backpressure>      并发/背压模式（默认：naive）
#   --heartbeatIntervalSec <sec>     心跳间隔（秒，默认：5）
#   --heartbeatMissThreshold <n>     连续丢心跳 N 次后断开（默认：3）
#   --maxFrameBytes <bytes>          最大帧大小（字节，默认：1048576 = 1MB）
#   --backpressureUnwritableThreshold <n>
#       仅 backpressure 模式：连续不可写 N 次后断开慢客户端（默认：3）
#   --metricsPort <port>
#       指标 HTTP 端口（默认：server 端口 + 1）
#   --bizThreads <n>
#       业务线程池大小（默认：4）
#   -h | --help                      显示帮助
#
# 说明：
# - 先构建：mvn -q -DskipTests package
# - 构建后会生成可执行 jar（server/target/server-*.jar）
# - 运行依赖会复制到 server/target/lib

set -eu

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
JAR_FILE=$(find "$PROJECT_ROOT/server/target" -maxdepth 1 -type f -name 'server-*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -n 1)
LIB_DIR="$PROJECT_ROOT/server/target/lib"

if [ -z "${JAR_FILE:-}" ] || [ ! -d "$LIB_DIR" ]; then
  echo "[ERROR] 未找到构建产物，请先执行：mvn -q -DskipTests package" >&2
  exit 1
fi

# 默认值
PORT=9000
MODE=naive
HEARTBEAT_INTERVAL=5
HEARTBEAT_MISS=3
MAX_FRAME_BYTES=1048576
BACKPRESSURE_THRESHOLD=3
METRICS_PORT=""
BIZ_THREADS=4

# 解析参数
while [ $# -gt 0 ]; do
  case "$1" in
    --port) PORT="$2"; shift 2 ;;
    --mode) MODE="$2"; shift 2 ;;
    --heartbeatIntervalSec) HEARTBEAT_INTERVAL="$2"; shift 2 ;;
    --heartbeatMissThreshold) HEARTBEAT_MISS="$2"; shift 2 ;;
    --maxFrameBytes) MAX_FRAME_BYTES="$2"; shift 2 ;;
    --backpressureUnwritableThreshold) BACKPRESSURE_THRESHOLD="$2"; shift 2 ;;
    --metricsPort) METRICS_PORT="$2"; shift 2 ;;
    --bizThreads) BIZ_THREADS="$2"; shift 2 ;;
    -h|--help)
      sed -n '2,90p' "$0" | sed 's/^# //;s/^#//'
      exit 0
      ;;
    *)
      echo "[ERROR] 未知参数: $1" >&2
      exit 1
      ;;
  esac
done

# 组装参数
ARGS="--port $PORT --mode $MODE --heartbeatIntervalSec $HEARTBEAT_INTERVAL --heartbeatMissThreshold $HEARTBEAT_MISS --maxFrameBytes $MAX_FRAME_BYTES --backpressureUnwritableThreshold $BACKPRESSURE_THRESHOLD --bizThreads $BIZ_THREADS"
if [ -n "$METRICS_PORT" ]; then
  ARGS="$ARGS --metricsPort $METRICS_PORT"
fi

# 启动
JAVA_BIN=${JAVA_BIN:-java}
exec "$JAVA_BIN" -jar "$JAR_FILE" $ARGS
