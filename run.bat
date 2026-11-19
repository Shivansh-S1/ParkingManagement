@echo off
cd /d "%~dp0"
if not exist out mkdir out
echo Compiling SmartPark...
javac -encoding UTF-8 -d out src\*.java
if errorlevel 1 (
  echo.
  echo Compilation failed. Please check that JDK is installed and on PATH.
  pause
  exit /b 1
)
echo Starting SmartPark...
java -cp out Main
pause
