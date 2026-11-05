@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo Starting PostgreSQL + NSI Spring Boot API...
docker compose up --build -d
if errorlevel 1 (
  echo Cannot start Docker Compose. Make sure Docker Desktop is running.
  pause
  exit /b 1
)
echo.
echo Once the container starts, open http://localhost:8080/swagger-ui.html
echo To see logs: docker compose logs -f api
echo To reset: docker compose down -v  ^&^&  docker compose up --build -d
pause
