@echo off
echo ========================================
echo Stopping AI Code Mentor Services
echo ========================================
echo.

echo [INFO] Stopping services on ports 11435, 8080, and 3000...

REM Kill processes on port 11435 (llama.cpp)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":11435"') do (
    echo [INFO] Stopping llama.cpp server (PID: %%a)
    taskkill /F /PID %%a >nul 2>&1
)

REM Kill processes on port 8080 (Spring Boot)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080"') do (
    echo [INFO] Stopping Spring Boot backend (PID: %%a)
    taskkill /F /PID %%a >nul 2>&1
)

REM Kill processes on port 3000 (Frontend)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3000"') do (
    echo [INFO] Stopping frontend (PID: %%a)
    taskkill /F /PID %%a >nul 2>&1
)

REM Also kill processes on port 3001 (alternative frontend port)
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3001"') do (
    echo [INFO] Stopping frontend on port 3001 (PID: %%a)
    taskkill /F /PID %%a >nul 2>&1
)

echo.
echo [INFO] All services stopped.
echo.
pause

