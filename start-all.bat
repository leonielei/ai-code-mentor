@echo off
echo ========================================
echo Starting AI Code Mentor Services (CUDA)
echo ========================================
echo.

REM === 可选：限制用哪块 GPU，例如只用 GPU 0 ===
REM set CUDA_VISIBLE_DEVICES=0

REM Check if llama.cpp server is already running
netstat -ano | findstr ":11435" >nul
if %errorlevel% == 0 (
    echo [INFO] llama.cpp server is already running on port 11435
) else (
    echo [INFO] Starting llama.cpp server (CUDA build)...
    REM 启用 GGML 统一内存，VRAM 不够时用系统内存而不是直接崩
    REM 注意：环境变量需要在启动的 cmd 中设置
    start "llama.cpp Server (CUDA)" cmd /k "set GGML_CUDA_ENABLE_UNIFIED_MEMORY=1 && cd llama-cpp && server.exe -m models/deepseek-coder-6.7b-instruct.Q2_K.gguf -ngl 20 -c 4096 -t 4 -b 512 -n 2048 --cont-batching --port 11435 --host 0.0.0.0"
    timeout /t 5 /nobreak >nul
)

REM Check if Spring Boot backend is already running
netstat -ano | findstr ":8080" >nul
if %errorlevel% == 0 (
    echo [INFO] Spring Boot backend is already running on port 8080
) else (
    echo [INFO] Starting Spring Boot backend...
    start "Spring Boot Backend" cmd /k "cd backend && mvn spring-boot:run"
    timeout /t 10 /nobreak >nul
)

REM Check if frontend is already running
netstat -ano | findstr ":3000" >nul
if %errorlevel% == 0 (
    echo [INFO] Frontend is already running on port 3000
) else (
    echo [INFO] Starting frontend...
    start "Frontend Dev Server" cmd /k "npm run dev"
    timeout /t 5 /nobreak >nul
)

echo.
echo ========================================
echo All services are starting...
echo ========================================
echo.
echo Services:
echo   - llama.cpp server: http://localhost:11435
echo   - Spring Boot backend: http://localhost:8080
echo   - Frontend: http://localhost:3000
echo.
echo Press any key to exit (services will continue running)...
pause >nul
