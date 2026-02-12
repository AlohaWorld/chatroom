#!/bin/sh
# 聊天室 JavaFX 客户端启动脚本（POSIX sh）。
# 仅使用 POSIX 语法，兼容 bash / zsh。
#
# 用法：
#   sh scripts/run-fx.sh
#
# 说明：
# - 先构建：mvn -q -DskipTests package
# - 运行时依赖从 client-fx/target/classpath.txt 读取
# - 显式添加 JavaFX 模块，避免模块解析失败

set -eu

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
CLASSPATH_FILE="$PROJECT_ROOT/client-fx/target/classpath.txt"
CLASSES_DIR="$PROJECT_ROOT/client-fx/target/classes"

if [ ! -d "$CLASSES_DIR" ] || [ ! -f "$CLASSPATH_FILE" ]; then
  echo "[ERROR] 未找到构建产物，请先执行：mvn -q -DskipTests package" >&2
  exit 1
fi

DEPS_CP=$(cat "$CLASSPATH_FILE")
CP="$CLASSES_DIR:$DEPS_CP"

JAVA_BIN=${JAVA_BIN:-java}
exec "$JAVA_BIN" -cp "$CP" --add-modules=javafx.controls,javafx.graphics com.example.chatroom.client.fx.ChatFxApp
