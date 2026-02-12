@echo off
REM 聊天室 CLI 客户端启动脚本（Windows 批处理）。
REM
REM 用法：
REM   scripts\run-cli.bat [options]
REM
REM 参数（均可选）：
REM   --host <host>                 服务器地址（默认：127.0.0.1）
REM   --port <port>                 服务器端口（默认：9000）
REM   --userId <id>                 启动后自动登录的 userId（默认：不自动登录）
REM   --heartbeatIntervalSec <sec>  心跳间隔（秒，默认：5）
REM   -h | --help                   显示帮助
REM
REM 说明：
REM - 先构建：mvn -q -DskipTests package
REM - 运行时依赖从 client-cli\target\classpath.txt 读取

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..
for %%I in ("%PROJECT_ROOT%") do set PROJECT_ROOT=%%~fI

set CLASSPATH_FILE=%PROJECT_ROOT%\client-cli\target\classpath.txt
set CLASSES_DIR=%PROJECT_ROOT%\client-cli\target\classes

if not exist "%CLASSES_DIR%" (
  echo [ERROR] 未找到构建产物，请先执行：mvn -q -DskipTests package
  exit /b 1
)
if not exist "%CLASSPATH_FILE%" (
  echo [ERROR] 未找到依赖 classpath，请先执行：mvn -q -DskipTests package
  exit /b 1
)

set HOST=127.0.0.1
set PORT=9000
set USER_ID=
set HEARTBEAT_INTERVAL=5

:parse
if "%~1"=="" goto run
if "%~1"=="--host" (set HOST=%~2& shift & shift & goto parse)
if "%~1"=="--port" (set PORT=%~2& shift & shift & goto parse)
if "%~1"=="--userId" (set USER_ID=%~2& shift & shift & goto parse)
if "%~1"=="--heartbeatIntervalSec" (set HEARTBEAT_INTERVAL=%~2& shift & shift & goto parse)
if "%~1"=="-h" goto help
if "%~1"=="--help" goto help

echo [ERROR] 未知参数: %~1
exit /b 1

:help
more +1 "%~f0"
exit /b 0

:run
for /f "usebackq delims=" %%i in ("%CLASSPATH_FILE%") do set DEPS_CP=%%i
set CP=%CLASSES_DIR%;%DEPS_CP%

set ARGS=--host %HOST% --port %PORT% --heartbeatIntervalSec %HEARTBEAT_INTERVAL%
if not "%USER_ID%"=="" set ARGS=%ARGS% --userId %USER_ID%

java.exe -cp "%CP%" com.example.chatroom.client.cli.CliMain %ARGS%
