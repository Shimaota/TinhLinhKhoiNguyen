@echo off
rem Run from the TLKN-Server folder so server.properties, Assest and ../DataGame resolve correctly.
cd /d "%~dp0"
if not exist server.properties (
  echo server.properties not found - copying server.properties.example
  copy server.properties.example server.properties >nul
)
if not exist target\tlnt-server.jar (
  call mvnw.cmd -B package || exit /b 1
)
java -Xms512m -Xmx2g -jar target\tlnt-server.jar
