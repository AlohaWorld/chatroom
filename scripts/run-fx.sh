#!/bin/sh
# 聊天室 JavaFX 客户端启动脚本（POSIX sh）。
# 仅使用 POSIX 语法，兼容 bash / zsh。
#
# 用法：
#   sh scripts/run-fx.sh
#
# 说明：
# - 先构建：mvn -q -DskipTests package
# - 构建后会生成可执行 jar（client-fx/target/client-fx-*.jar）
# - 运行依赖会复制到 client-fx/target/lib

set -eu

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
JAR_FILE=$(find "$PROJECT_ROOT/client-fx/target" -maxdepth 1 -type f -name 'client-fx-*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -n 1)
LIB_DIR="$PROJECT_ROOT/client-fx/target/lib"

if [ -z "${JAR_FILE:-}" ] || [ ! -d "$LIB_DIR" ]; then
  echo "[ERROR] 未找到构建产物，请先执行：mvn -q -DskipTests package" >&2
  exit 1
fi

JAVA_BIN=${JAVA_BIN:-java}
exec "$JAVA_BIN" -jar "$JAR_FILE"
