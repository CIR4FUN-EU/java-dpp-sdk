@echo off
setlocal

if exist mvnw.cmd (
    call mvnw.cmd test
    exit /b %errorlevel%
)

where mvn >nul 2>nul
if errorlevel 1 (
    echo Maven was not found on PATH. Install Maven or add the Maven Wrapper to the repo.
    exit /b 1
)

mvn test
exit /b %errorlevel%
