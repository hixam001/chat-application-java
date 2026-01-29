@echo off
title Chat Application Launcher

REM --- Configuration ---
REM %~dp0 expands to the drive letter and path of the batch file itself.
REM This makes the script robust regardless of where it's launched from.
SET "BASE_DIR=%~dp0"
SET "SQLITE_JDBC_JAR=sqlite-jdbc-3.49.1.0.jar"
SET "SERVER_LOG_FILE=server_log.txt"

REM --- Output and Navigation ---
echo =========================================
echo Launching Chat Application Components
echo Script Directory: %BASE_DIR%
echo Server Output Log: %BASE_DIR%%SERVER_LOG_FILE%
echo =========================================
echo.

REM Navigate to the base directory where your JARs are located
cd /d "%BASE_DIR%"

REM --- Launch Server ---
echo Starting Chat Server in background...
REM Using javaw to run server without a visible console window.
REM Output redirected to server_log.txt in the BASE_DIR.
REM The empty title ("") is important for the 'start' command syntax.
start "" "javaw" -cp "ChatApp.jar;%SQLITE_JDBC_JAR%" com.chat.app.MainLauncher server > "%SERVER_LOG_FILE%" 2>&1

REM Give the server ample time to initialize and bind to the port.
REM This is crucial to prevent "Connection refused" errors on the client.
echo Waiting 15 seconds for server to initialize...
timeout /t 15 /nobreak >nul
echo Server initialization window closed.

REM --- Launch Client ---
echo.
echo Starting Chat Client GUI...
REM Using javaw for client GUI as it doesn't need console output.
start "" "javaw" -cp "ChatApp.jar;%SQLITE_JDBC_JAR%" com.chat.app.MainLauncher

REM --- Final Messages ---
echo.
echo All requested components launched.
echo Please check the server's output in "%SERVER_LOG_FILE%".
echo Client GUI(s) should appear shortly.
echo This launcher window will now close.
exit