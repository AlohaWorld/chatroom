@echo off
REM 聊天室 Server 启动脚本（Windows 批处理）。
REM
REM 用法：
REM   scripts\run-server.bat [options]
REM
REM 参数（均可选）：
REM   --port <port>                    服务端 TCP 端口（默认：9000）
REM   --mode <naive|backpressure>      并发/背压模式（默认：naive）
REM   --heartbeatIntervalSec <sec>     心跳间隔（秒，默认：5）
REM   --heartbeatMissThreshold <n>     连续丢心跳 N 次后断开（默认：3）
REM   --maxFrameBytes <bytes>          最大帧大小（字节，默认：1048576 = 1MB）
REM   --backpressureUnwritableThreshold <n>
REM       仅 backpressure 模式：连续不可写 N 次后断开慢客户端（默认：3）
REM   --metricsPort <port>
REM       指标 HTTP 端口（默认：server 端口 + 1）
REM   --bizThreads <n>
REM       业务线程池大小（默认：4）
REM   -h | --help                      显示帮助
REM
REM 说明：
REM - 先构建：mvn -q -DskipTests package
REM - 构建后会生成可执行 jar（server\target\server-*.jar）
REM - 运行依赖会复制到 server\target\lib

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..
for %%I in ("%PROJECT_ROOT%") do set PROJECT_ROOT=%%~fI

set TARGET_DIR=%PROJECT_ROOT%\server\target
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
if not exist "%TARGET_DIR%\server-*.jar" (
  echo [ERROR] 未找到可执行 jar，请先执行：mvn -q -DskipTests package
  exit /b 1
)

for %%f in ("%TARGET_DIR%\server-*.jar") do (
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

REM 默认值
set PORT=9000
set MODE=naive
set HEARTBEAT_INTERVAL=5
set HEARTBEAT_MISS=3
set MAX_FRAME_BYTES=1048576
set BACKPRESSURE_THRESHOLD=3
set METRICS_PORT=
set BIZ_THREADS=4

:parse
if "%~1"=="" goto run
if "%~1"=="--port" (set PORT=%~2& shift & shift & goto parse)
if "%~1"=="--mode" (set MODE=%~2& shift & shift & goto parse)
if "%~1"=="--heartbeatIntervalSec" (set HEARTBEAT_INTERVAL=%~2& shift & shift & goto parse)
if "%~1"=="--heartbeatMissThreshold" (set HEARTBEAT_MISS=%~2& shift & shift & goto parse)
if "%~1"=="--maxFrameBytes" (set MAX_FRAME_BYTES=%~2& shift & shift & goto parse)
if "%~1"=="--backpressureUnwritableThreshold" (set BACKPRESSURE_THRESHOLD=%~2& shift & shift & goto parse)
if "%~1"=="--metricsPort" (set METRICS_PORT=%~2& shift & shift & goto parse)
if "%~1"=="--bizThreads" (set BIZ_THREADS=%~2& shift & shift & goto parse)
if "%~1"=="-h" goto help
if "%~1"=="--help" goto help

echo [ERROR] 未知参数: %~1
exit /b 1

:help
more +1 "%~f0"
exit /b 0

:run
set ARGS=--port %PORT% --mode %MODE% --heartbeatIntervalSec %HEARTBEAT_INTERVAL% --heartbeatMissThreshold %HEARTBEAT_MISS% --maxFrameBytes %MAX_FRAME_BYTES% --backpressureUnwritableThreshold %BACKPRESSURE_THRESHOLD% --bizThreads %BIZ_THREADS%
if not "%METRICS_PORT%"=="" set ARGS=%ARGS% --metricsPort %METRICS_PORT%

java.exe -jar "%JAR_FILE%" %ARGS%
