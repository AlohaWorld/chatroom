@echo off
REM 聊天室 JavaFX 客户端启动脚本（Windows 批处理）。
REM
REM 用法：
REM   scripts\run-fx.bat
REM
REM 说明：
REM - 先构建：mvn -q -DskipTests package
REM - 运行时依赖从 client-fx\target\classpath.txt 读取
REM - 显式添加 JavaFX 模块，避免模块解析失败

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..
for %%I in ("%PROJECT_ROOT%") do set PROJECT_ROOT=%%~fI

set CLASSPATH_FILE=%PROJECT_ROOT%\client-fx\target\classpath.txt
set CLASSES_DIR=%PROJECT_ROOT%\client-fx\target\classes

if not exist "%CLASSES_DIR%" (
  echo [ERROR] 未找到构建产物，请先执行：mvn -q -DskipTests package
  exit /b 1
)
if not exist "%CLASSPATH_FILE%" (
  echo [ERROR] 未找到依赖 classpath，请先执行：mvn -q -DskipTests package
  exit /b 1
)

for /f "usebackq delims=" %%i in ("%CLASSPATH_FILE%") do set DEPS_CP=%%i
set CP=%CLASSES_DIR%;%DEPS_CP%

java.exe -cp "%CP%" --add-modules=javafx.controls,javafx.graphics com.example.chatroom.client.fx.ChatFxApp
