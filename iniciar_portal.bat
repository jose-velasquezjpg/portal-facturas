@echo off
setlocal
cd /d "%~dp0"
echo Iniciando Portal de Facturas TAI LOY...
echo Comprueba primero que PostgreSQL este iniciado y la base portal_facturas preparada.
mvn spring-boot:run
echo.
echo El proceso termino. Revisa los mensajes anteriores si hubo errores.
pause
