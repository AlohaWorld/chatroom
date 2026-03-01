@echo off
REM 聊天室 JavaFX 客户端启动脚本（Windows 批处理）。
REM
REM 用法：
REM   scripts\run-fx.bat
REM
REM 说明：
REM - 先构建：mvn -q -DskipTests package
REM - 构建后会生成可执行 jar（client-fx\target\client-fx-*.jar）
REM - 运行依赖会复制到 client-fx\target\lib

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..
for %%I in ("%PROJECT_ROOT%") do set PROJECT_ROOT=%%~fI

set TARGET_DIR=%PROJECT_ROOT%\client-fx\target
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
if not exist "%TARGET_DIR%\client-fx-*.jar" (
  echo [ERROR] 未找到可执行 jar，请先执行：mvn -q -DskipTests package
  exit /b 1
)

for %%f in ("%TARGET_DIR%\client-fx-*.jar") do (
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

java.exe -jar "%JAR_FILE%"
