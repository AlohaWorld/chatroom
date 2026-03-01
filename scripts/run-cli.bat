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
REM - 构建后会生成可执行 jar（client-cli\target\client-cli-*.jar）
REM - 运行依赖会复制到 client-cli\target\lib

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..
for %%I in ("%PROJECT_ROOT%") do set PROJECT_ROOT=%%~fI

set TARGET_DIR=%PROJECT_ROOT%\client-cli\target
set LIB_DIR=%TARGET_DIR%\lib
set JAR_FILE=

if not exist "%TARGET_DIR%" (
  echo [ERROR] 未找到构建产物，请先执行：mvn -q -DskipTests package
  exit /b 1
)
if not exist "%LIB_DIR%" (
  echo [ERROR] 未找到依赖目录 target\lib，请先执行：mvn -q -DskipTests package
  exit /b 1
)
if not exist "%TARGET_DIR%\client-cli-*.jar" (
  echo [ERROR] 未找到可执行 jar，请先执行：mvn -q -DskipTests package
  exit /b 1
)

for %%f in ("%TARGET_DIR%\client-cli-*.jar") do (
  echo %%~nxf | findstr /I /C:"-sources.jar" /C:"-javadoc.jar" >nul || (
    if not defined JAR_FILE set JAR_FILE=%%~ff
  )
)

if not defined JAR_FILE (
  echo [ERROR] 未找到可执行 jar，请先执行：mvn -q -DskipTests package
  exit /b 1
)
if not exist "%JAR_FILE%" (
  echo [ERROR] 可执行 jar 路径无效：%JAR_FILE%
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
set ARGS=--host %HOST% --port %PORT% --heartbeatIntervalSec %HEARTBEAT_INTERVAL%
if not "%USER_ID%"=="" set ARGS=%ARGS% --userId %USER_ID%

java.exe -jar "%JAR_FILE%" %ARGS%
