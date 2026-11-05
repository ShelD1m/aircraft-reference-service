@echo off
cd /d "%~dp0"
echo WARNING: this will DELETE all changes in the demo PostgreSQL database.
choice /M "Delete demo database and recreate"
if errorlevel 2 exit /b 0
docker compose down -v
if errorlevel 1 goto failed
docker compose up --build -d
if errorlevel 1 goto failed
echo Reset completed. Open http://localhost:8080/swagger-ui.html
pause
exit /b 0
:failed
echo Error occurred. Check Docker Desktop and docker compose logs.
pause
exit /b 1
