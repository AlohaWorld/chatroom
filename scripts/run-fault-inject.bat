@echo off
REM 聊天室 Fault-Inject 故障注入工具启动脚本（Windows 批处理）。
REM
REM 用法：
REM   scripts\run-fault-inject.bat [options]
REM
REM 参数（均可选）：
REM   --host <host>   服务器地址（默认：127.0.0.1）
REM   --port <port>   服务器端口（默认：9000）
REM   --case <name>   注入的故障类型（默认：all）
REM                  支持：
REM                  - missing_fields   缺失必要字段
REM                  - unknown_type     未知 msgType
REM                  - bad_length       错误长度字段
REM                  - too_large        超大帧
REM                  - disconnect       连接后立即断开
REM   -h | --help     显示帮助
REM
REM 说明：
REM - 先构建：mvn -q -DskipTests package
REM - 构建后会生成可执行 jar（tools\target\tools-*-fault-inject.jar）
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
if not exist "%TARGET_DIR%\tools-*-fault-inject.jar" (
  echo [ERROR] 未找到 fault-inject 可执行 jar，请先执行：mvn -q -DskipTests package
  exit /b 1
)

set HOST=127.0.0.1
set PORT=9000
set CASE=all

:parse
if "%~1"=="" goto run
if "%~1"=="--host" (set HOST=%~2& shift & shift & goto parse)
if "%~1"=="--port" (set PORT=%~2& shift & shift & goto parse)
if "%~1"=="--case" (set CASE=%~2& shift & shift & goto parse)
if "%~1"=="-h" goto help
if "%~1"=="--help" goto help

echo [ERROR] 未知参数: %~1
exit /b 1

:help
more +1 "%~f0"
exit /b 0

:run
for %%f in ("%TARGET_DIR%\tools-*-fault-inject.jar") do (
  if not defined JAR_FILE set JAR_FILE=%%~ff
)

if not defined JAR_FILE (
  echo [ERROR] 未找到 fault-inject 可执行 jar，请先执行：mvn -q -DskipTests package
  exit /b 1
)
if not exist "%JAR_FILE%" (
  echo [ERROR] 可执行 jar 路径无效：%JAR_FILE%
  exit /b 1
)

java.exe -jar "%JAR_FILE%" ^
  --host %HOST% --port %PORT% --case %CASE%
