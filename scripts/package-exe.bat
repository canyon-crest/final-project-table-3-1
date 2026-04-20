@echo off
REM Build fat JAR then create a Windows installer-style .exe using jpackage (JDK 17+).
REM Requires: Maven on PATH, JAVA_HOME pointing to a JDK that includes jpackage.

setlocal
cd /d "%~dp0\.."

call mvn -q -DskipTests package
if errorlevel 1 exit /b 1

if not defined JAVA_HOME (
  echo Set JAVA_HOME to a full JDK ^(not a JRE^) that includes jpackage.
  exit /b 1
)

set OUT=dist
if not exist "%OUT%" mkdir "%OUT%"

"%JAVA_HOME%\bin\jpackage.exe" ^
  --type exe ^
  --name "CCAForum" ^
  --dest "%OUT%" ^
  --input target ^
  --main-jar forum-app.jar ^
  --app-version 1.0 ^
  --vendor "CCA" ^
  --copyright "AP CSA project" ^
  --java-options "-Dfile.encoding=UTF-8"

echo.
echo Output: %OUT%\CCAForum.exe ^(installer^) or application folder, depending on jpackage version.
endlocal
