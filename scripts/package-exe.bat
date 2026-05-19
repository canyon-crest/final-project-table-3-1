@echo off
REM Build fat JAR, then always create a runnable app-image.
REM If WiX is installed, also create an installer .exe.
REM Requires: Maven on PATH, JAVA_HOME pointing to a JDK that includes jpackage.

setlocal
cd /d "%~dp0\.."

call mvn -q -DskipTests package
if errorlevel 1 exit /b 1

if not defined JAVA_HOME (
  echo Set JAVA_HOME to a full JDK ^(not a JRE^) that includes jpackage.
  exit /b 1
)

set JPACKAGE="%JAVA_HOME%\bin\jpackage.exe"
if not exist %JPACKAGE% (
  echo Could not find jpackage at %JAVA_HOME%\bin\jpackage.exe
  exit /b 1
)

set OUT=dist
if not exist "%OUT%" mkdir "%OUT%"
if exist "%OUT%\CCAForum" rmdir /s /q "%OUT%\CCAForum"

REM Stage non-code runtime files into target so jpackage includes them.
if exist "target\assets" rmdir /s /q "target\assets"
if exist "assets" xcopy /e /i /y "assets" "target\assets" >nul
if exist "forum.properties.example" copy /y "forum.properties.example" "target\forum.properties.example" >nul
if exist "forum.properties" copy /y "forum.properties" "target\forum.properties" >nul

echo Creating runnable app-image...
%JPACKAGE% ^
  --type app-image ^
  --name "CCAForum" ^
  --dest "%OUT%" ^
  --input target ^
  --main-jar forum-app.jar ^
  --app-version 1.0 ^
  --vendor "CCA" ^
  --copyright "AP CSA project" ^
  --java-options "-Dfile.encoding=UTF-8"
if errorlevel 1 exit /b 1

REM Keep config + assets easy to edit next to the EXE.
if exist "assets" xcopy /e /i /y "assets" "%OUT%\CCAForum\assets" >nul
if exist "forum.properties" (
  copy /y "forum.properties" "%OUT%\CCAForum\forum.properties" >nul
) else if exist "forum.properties.example" (
  copy /y "forum.properties.example" "%OUT%\CCAForum\forum.properties.example" >nul
)
call :EmbedDbSettings "%OUT%\CCAForum\app\CCAForum.cfg" "forum.properties"

echo.
echo Runnable app created:
echo   %OUT%\CCAForum\CCAForum.exe

echo.
echo Trying to create installer .exe ^(requires WiX 3.x on PATH^)...
%JPACKAGE% ^
  --type exe ^
  --name "CCAForum" ^
  --dest "%OUT%" ^
  --input target ^
  --main-jar forum-app.jar ^
  --app-version 1.0 ^
  --vendor "CCA" ^
  --copyright "AP CSA project" ^
  --java-options "-Dfile.encoding=UTF-8"

if errorlevel 1 (
  echo Installer build skipped/failed. Install WiX 3.x to enable installer output.
  echo Download: https://wixtoolset.org/
) else (
  echo Installer created:
  echo   %OUT%\CCAForum.exe
)

REM Keep target folder tidy: retain only the final runnable JAR.
for %%F in ("target\*.jar") do (
  if /I not "%%~nxF"=="forum-app.jar" del /q "%%~fF" >nul 2>nul
)

endlocal
exit /b 0

:EmbedDbSettings
set "CFG=%~1"
set "PROPS=%~2"

if not exist "%CFG%" (
  echo Skipping embedded DB settings: launcher config not found.
  exit /b 0
)
if not exist "%PROPS%" (
  echo Skipping embedded DB settings: forum.properties not found.
  exit /b 0
)

set "EMBED_CFG=%CFG%"
set "EMBED_PROPS=%PROPS%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$cfg=$env:EMBED_CFG;$propsPath=$env:EMBED_PROPS;$props=@{};foreach($raw in [System.IO.File]::ReadAllLines($propsPath)){ $line=$raw.Trim(); if($line -eq '' -or $line.StartsWith('#')){ continue }; $idx=$line.IndexOf('='); if($idx -lt 0){ continue }; $props[$line.Substring(0,$idx).Trim()]=$line.Substring($idx+1).Trim() }; if(-not $props.ContainsKey('db.url') -or -not $props.ContainsKey('db.user')){ exit 0 }; $lines=[System.IO.File]::ReadAllLines($cfg); $filtered=New-Object System.Collections.Generic.List[string]; foreach($line in $lines){ if($line.StartsWith('java-options=-Dforum.db.url=') -or $line.StartsWith('java-options=-Dforum.db.user=') -or $line.StartsWith('java-options=-Dforum.db.password=')){ continue }; [void]$filtered.Add($line) }; [void]$filtered.Add('java-options=-Dforum.db.url=' + $props['db.url']); [void]$filtered.Add('java-options=-Dforum.db.user=' + $props['db.user']); if($props.ContainsKey('db.password')){ [void]$filtered.Add('java-options=-Dforum.db.password=' + $props['db.password']) }; [System.IO.File]::WriteAllLines($cfg, $filtered); exit 0" >nul
set "EMBED_EXIT=%errorlevel%"
if "%EMBED_EXIT%" NEQ "0" (
  echo Warning: could not embed DB settings into launcher config.
) else (
  echo Embedded DB settings into packaged launcher config.
)
exit /b 0
