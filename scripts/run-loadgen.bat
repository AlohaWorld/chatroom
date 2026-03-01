@echo off
REM 聊天室 LoadGen 压测工具启动脚本（Windows 批处理）。
REM
REM 用法：
REM   scripts\run-loadgen.bat [options]
REM
REM 参数（均可选）：
REM   --host <host>        服务器地址（默认：127.0.0.1）
REM   --port <port>        服务器端口（默认：9000）
REM   --clients <n>        并发客户端数量（默认：200）
REM   --msgRate <rate>     每秒发送消息数（默认：2000）
REM   --durationSec <sec>  压测时长（秒，默认：30）
REM   --payloadSize <n>    消息负载大小（字节，默认：128）
REM   -h | --help          显示帮助
REM
REM 说明：
REM - 先构建：mvn -q -DskipTests package
REM - 构建后会生成可执行 jar（tools\target\tools-*.jar）
REM - 运行依赖会复制到 tools\target\lib

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..
for %%I in ("%PROJECT_ROOT%") do set PROJECT_ROOT=%%~fI

set TARGET_DIR=%PROJECT_ROOT%\tools\target
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
if not exist "%TARGET_DIR%\tools-*.jar" (
  echo [ERROR] 未找到可执行 jar，请先执行：mvn -q -DskipTests package
  exit /b 1
)

set HOST=127.0.0.1
set PORT=9000
set CLIENTS=200
set MSG_RATE=2000
set DURATION=30
set PAYLOAD=128

:parse
if "%~1"=="" goto run
if "%~1"=="--host" (set HOST=%~2& shift & shift & goto parse)
if "%~1"=="--port" (set PORT=%~2& shift & shift & goto parse)
if "%~1"=="--clients" (set CLIENTS=%~2& shift & shift & goto parse)
if "%~1"=="--msgRate" (set MSG_RATE=%~2& shift & shift & goto parse)
if "%~1"=="--durationSec" (set DURATION=%~2& shift & shift & goto parse)
if "%~1"=="--payloadSize" (set PAYLOAD=%~2& shift & shift & goto parse)
if "%~1"=="-h" goto help
if "%~1"=="--help" goto help

echo [ERROR] 未知参数: %~1
exit /b 1

:help
more +1 "%~f0"
exit /b 0

:run
for %%f in ("%TARGET_DIR%\tools-*.jar") do (
  echo %%~nxf | findstr /I /C:"fault-inject" /C:"-sources.jar" /C:"-javadoc.jar" >nul || (
    if not defined JAR_FILE set JAR_FILE=%%~ff
  )
)

if not defined JAR_FILE (
  echo [ERROR] 未找到 LoadGen 可执行 jar，请先执行：mvn -q -DskipTests package
  exit /b 1
)
if not exist "%JAR_FILE%" (
  echo [ERROR] 可执行 jar 路径无效：%JAR_FILE%
  exit /b 1
)

java.exe -jar "%JAR_FILE%" ^
  --host %HOST% --port %PORT% --clients %CLIENTS% --msgRate %MSG_RATE% ^
  --durationSec %DURATION% --payloadSize %PAYLOAD%
