@echo off
setlocal
cd /d "%~dp0"
echo === AstraxPetRPG 1.2.0 build ===
java -version >nul 2>&1
if errorlevel 1 (
  echo ERROR: Java not found. Install JDK 21 first.
  pause
  exit /b 1
)
mvn -version >nul 2>&1
if errorlevel 1 (
  echo ERROR: Maven not found. Install Apache Maven and add mvn to PATH.
  echo Then run this file again.
  pause
  exit /b 1
)
mvn --batch-mode clean package
if errorlevel 1 (
  echo BUILD FAILED. Check the Maven error above and verify internet access to PaperMC repository.
  pause
  exit /b 1
)
echo.
echo BUILD SUCCESS. JAR: %CD%\target\AstraxPetRPG-1.2.0.jar
pause
