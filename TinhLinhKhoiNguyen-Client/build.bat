@echo off
powershell -NoProfile -Command "$f='%~f0'; $b=[IO.File]::ReadAllBytes($f); if (($b -eq 13).Count -lt ($b -eq 10).Count) { $t=[Text.Encoding]::ASCII.GetString($b) -replace '\r?\n',[Environment]::NewLine; [IO.File]::WriteAllText($f,$t,[Text.Encoding]::ASCII); exit 7 }; exit 0" <nul
if "%ERRORLEVEL%"=="7" if not defined TLNT_EOL_FIXED (set "TLNT_EOL_FIXED=1" & echo Da sua xuong dong build.bat sang CRLF, chay lai... & "%~f0" %*)
rem The 2 lines above: if the line endings were converted to LF (Git, copying to another machine...) cmd cannot find goto/call labels
rem -> rewrite the file as CRLF and rerun. They must sit AT THE VERY TOP of the file (within the first 512 bytes) and use no labels.
rem This file uses ONLY ASCII characters: the line above rewrites it as ASCII.
setlocal EnableExtensions EnableDelayedExpansion
for /f "tokens=2 delims=:." %%c in ('chcp ^<nul') do set "OLD_CP=%%c"
chcp 65001 >nul <nul
cd /d "%~dp0"

rem ==========================================================================================
rem  QUICK BUILD of the TLNT client (PC + Android) + server
rem
rem    build.bat                 interactive menu
rem    build.bat pc              obfuscated PC game folder:   release\pc\TLNT\   (TLNT.bat + bundled JRE)
rem    build.bat setup           PC installer:                release\TLNT-Setup-<ver>.exe
rem    build.bat apk             signed + obfuscated release APK: release\TLNT-<ver>.apk
rem    build.bat apkdebug        debug APK (not obfuscated, quick test install): release\TLNT-<ver>-debug.apk
rem    build.bat aab             release AAB (Google Play):   release\TLNT-<ver>.aab
rem    build.bat web             Web client (HTML5/TeaVM):    release\web\
rem    build.bat all             setup + apk + web (in parallel)
rem    build.bat install         install the newest APK in release\ on a phone / emulator (adb)
rem    build.bat ios             check that the iOS code compiles + obfuscates (a real IPA can only be built on a Mac)
rem    build.bat server          server: ..\TinhLinhKhoiNguyen-Server\TLKN-Server\target\tlnt-server.jar
rem    build.bat test            run the client tests
rem    build.bat run             run the client from source (no packaging)
rem    build.bat clean           delete old build output (after strange build errors, big branch switches)
rem
rem  Extra parameters (placed AFTER the command, as key=value):
rem    host=1.2.3.4   port=7502   name=Server1      override the default server in client.properties
rem    "game=Game Name With Spaces"                 override the game name (wrap the whole pair in quotes if it contains spaces)
rem    (passed via TLNT_* environment variables, not -P: Windows turns accented characters on the command line into '?')
rem    obf=off|ascii|mixed|unicode                  obfuscation style (default: PC mixed, APK on)
rem    online                                       allow downloading libraries right away (default --offline; on missing
rem                                                 libraries it AUTOMATICALLY retries online, code errors are NOT rebuilt)
rem    nopause                                      do not pause at the end (when run from another script / CI)
rem  Example:  build.bat all host=203.0.113.10 port=7502
rem
rem  Change the game name / data folder / default server: edit client.properties (repo root) and rebuild.
rem  Version: gradle.properties (appVersionName, appVersionCode - every new APK must INCREASE versionCode).
rem  Log of the latest build: build\build.log
rem ==========================================================================================

set "ACTION="
set "PROPS="
set "ONLINE="
set "PAUSE_END="
set "GLOG=%~dp0build\build.log"
rem --parallel: PC + Android build at the same time; --build-cache: reuse previous results (enabled in gradle.properties)
set "GFLAGS=--parallel --build-cache --console=plain"

if "%~1"=="" goto menu
set "ACTION=%~1"
shift
goto parse

:menu
rem Opened by double-click: pause at the end so the messages can be read (otherwise the window closes itself)
set "PAUSE_END=1"
echo.
echo   ===== BUILD TLNT =====
echo    1. PC (thu muc game, da lam roi)
echo    2. Bo cai PC (Setup.exe)
echo    3. APK release (da ky + lam roi)
echo    4. Web client (TeaVM HTML5)
echo    5. Tat ca (bo cai PC + APK + Web)
echo    6. Chay client tu ma nguon
echo    7. Xoa build cu (clean)
echo.
set "PICK="
set /p "PICK=Chon [1-7]: "
for %%p in ("1=pc" "2=setup" "3=apk" "4=web" "5=all" "6=run" "7=clean") do (
  for /f "tokens=1,2 delims==" %%a in (%%p) do if "!PICK!"=="%%a" set "ACTION=%%b"
)
if not defined ACTION (echo Lua chon khong hop le & goto fail)
for %%a in (pc setup apk web all) do if /i "!ACTION!"=="%%a" (
  rem Print this machine's LAN IP (adapter with a gateway) to type it correctly - one wrong digit and the client cannot find the server
  for /f "delims=" %%i in ('powershell -NoProfile -Command "Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway } | ForEach-Object { $_.IPv4Address.IPAddress }" ^<nul') do echo    IP LAN may nay: %%i
  set "HOSTIN="
  set /p "HOSTIN=May chu (Enter = giu client.properties): "
  if defined HOSTIN set "TLNT_SERVER_HOST=!HOSTIN!"
)
goto go

:parse
rem cmd SPLITS arguments at '=': host=1.2.3.4 arrives here as 2 arguments "host" "1.2.3.4" (older versions had this bug: the server
rem override did not apply). "host=1.2.3.4" in quotes stays a single argument. Both forms are handled.
if "%~1"=="" goto go
set "KEY=%~1"
set "VAL="
set "ISKV="
for %%k in (host port name game obf ws wsurl) do if /i "!KEY!"=="%%k" set "ISKV=1"
if defined ISKV (
  set "VAL=%~2"
  shift
) else (
  for /f "tokens=1,* delims==" %%a in ("!KEY!") do (
    for %%k in (host port name game obf ws wsurl) do if /i "%%a"=="%%k" (set "ISKV=1" & set "KEY=%%a" & set "VAL=%%b")
  )
)
if defined ISKV (
  if /i "!KEY!"=="host"  set "TLNT_SERVER_HOST=!VAL!"
  if /i "!KEY!"=="port"  set "TLNT_SERVER_PORT=!VAL!"
  if /i "!KEY!"=="name"  set "TLNT_SERVER_NAME=!VAL!"
  if /i "!KEY!"=="game"  set "TLNT_GAME_NAME=!VAL!"
  if /i "!KEY!"=="ws"    set "TLNT_SERVER_WS_URL=!VAL!"
  if /i "!KEY!"=="wsurl" set "TLNT_SERVER_WS_URL=!VAL!"
  if /i "!KEY!"=="obf"   set "PROPS=!PROPS! -Pobf=!VAL!"
) else if /i "!KEY!"=="online" (
  set "ONLINE=1"
) else if /i "!KEY!"=="nopause" (
  set "PAUSE_END="
) else (
  echo Bo qua tham so khong hieu: !KEY!
)
shift
goto parse

:go
if not exist "client.properties" if exist "client.properties.example" (
  copy "client.properties.example" "client.properties" >nul
  echo    client.properties chua co: da tu dong tao tu client.properties.example
)
set "VER="
for /f "tokens=2 delims==" %%v in ('findstr /b /c:"appVersionName=" gradle.properties') do set "VER=%%v"
for /f "tokens=2 delims==" %%v in ('findstr /b /c:"appVersionCode=" gradle.properties') do set "VCODE=%%v"
if not defined VER set "VER=0.0.0"
set "VER=!VER: =!"
if not exist release mkdir release
if not exist build mkdir build
call :now T0

echo.
echo ^>^> %ACTION%  (version %VER%, code %VCODE%) %PROPS%
if defined TLNT_GAME_NAME      echo    ten game: !TLNT_GAME_NAME!
if defined TLNT_SERVER_HOST    echo    may chu:  !TLNT_SERVER_HOST!
if defined TLNT_SERVER_PORT    echo    cong:     !TLNT_SERVER_PORT!
if defined TLNT_SERVER_NAME    echo    ten MC:   !TLNT_SERVER_NAME!
if defined TLNT_SERVER_WS_URL  echo    ws url:   !TLNT_SERVER_WS_URL!
echo.

set "KNOWN="
for %%a in (pc setup apk apkdebug aab web all install ios server test run clean) do if /i "%ACTION%"=="%%a" set "KNOWN=1"
if not defined KNOWN (echo Lenh khong hop le: %ACTION%  ^(xem dau file build.bat^) & goto fail)

rem install / server do not need the client Gradle build
if /i not "%ACTION%"=="install" if /i not "%ACTION%"=="server" (call :check_java || goto fail)
for %%a in (apk apkdebug aab all install) do if /i "%ACTION%"=="%%a" (call :check_sdk || goto fail)
for %%a in (apk aab all) do if /i "%ACTION%"=="%%a" (call :check_release_host & call :check_keystore || goto fail)
goto do_%ACTION%

rem ---------------------------------------------------------------- commands
:do_pc
call :gradle :desktop:exe || goto fail
call :copy_pc || goto fail
goto ok

:do_setup
call :gradle :desktop:setup || goto fail
call :copy_pc || goto fail
call :copy_setup || goto fail
goto ok

:do_apk
call :gradle :android:assembleRelease || goto fail
call :copy_apk || goto fail
goto ok

:do_apkdebug
call :gradle :android:assembleDebug || goto fail
call :copy_out "android\build\outputs\apk\debug\android-debug.apk" "release\TLNT-%VER%-debug.apk" "APK DEBUG" || goto fail
goto ok

:do_aab
call :gradle :android:bundleRelease || goto fail
call :copy_out "android\build\outputs\bundle\release\android-release.aab" "release\TLNT-%VER%.aab" "AAB" || goto fail
call :copy_mapping
goto ok

:do_web
call :gradle :web:gdx_teavm_web_js_build || goto fail
call :copy_web || goto fail
goto ok

:do_all
call :gradle :desktop:setup :android:assembleRelease :web:gdx_teavm_web_js_build || goto fail
call :copy_pc || goto fail
call :copy_setup || goto fail
call :copy_apk || goto fail
call :copy_web || goto fail
goto ok

:do_install
set "APKF="
rem Newest APK in release\ (by modification time)
for /f "delims=" %%f in ('dir /b /o-d "release\*.apk" 2^>nul') do if not defined APKF set "APKF=release\%%f"
if not defined APKF (echo Chua co APK trong release\ - chay build.bat apk hoac apkdebug truoc & goto fail)
set "ADB=%SDK_DIR%\platform-tools\adb.exe"
if not exist "%ADB%" (echo Khong thay adb: %ADB% & goto fail)
"%ADB%" start-server >nul 2>&1
"%ADB%" devices > "build\adb-devices.txt" 2>nul
set "DEVS=0"
for /f "usebackq skip=1 tokens=1,2" %%a in ("build\adb-devices.txt") do if "%%b"=="device" set /a DEVS+=1
if "%DEVS%"=="0" (
  echo Khong thay may nao: cam cap + bat "Go loi USB", hoac mo gia lap ^(LDPlayer: "%ADB%" connect 127.0.0.1:5555^)
  goto fail
)
echo Cai %APKF% vao %DEVS% may...
for /f "usebackq skip=1 tokens=1,2" %%a in ("build\adb-devices.txt") do if "%%b"=="device" (
  echo  - %%a
  "%ADB%" -s %%a install -r "%APKF%"
  if errorlevel 1 (
    echo    Loi cai. Neu bao INSTALL_FAILED_UPDATE_INCOMPATIBLE: APK cu ky bang khoa khac ^(debug ^<-^> release^), go app roi cai lai.
    echo    Neu bao INSTALL_FAILED_VERSION_DOWNGRADE: tang appVersionCode trong gradle.properties.
    goto fail
  )
)
goto ok

:do_ios
call :gradle :ios:compileJava :ios:obfJar || goto fail
if exist "ios\build\obf\mapping.txt" copy /y "ios\build\obf\mapping.txt" "release\mapping-ios-%VER%.txt" >nul
echo IOS: ma Java bien dich + lam roi ma ^(ProGuard^) xong. Mapping: release\mapping-ios-%VER%.txt
echo      IPA that: tren Mac chay ./gradlew ios:createIPA ^(can Xcode + chung chi ky^)
goto ok

:do_server
if not exist "%~dp0..\TinhLinhKhoiNguyen-Server\TLKN-Server\mvnw.cmd" (echo Khong thay ..\TinhLinhKhoiNguyen-Server\TLKN-Server\mvnw.cmd & goto fail)
pushd "%~dp0..\TinhLinhKhoiNguyen-Server\TLKN-Server" || goto fail
call ".\mvnw.cmd" -o -q package -DskipTests
if errorlevel 1 (
  echo.
  echo ^>^> Build offline loi - thu lai co tai thu vien ^(online^)...
  call ".\mvnw.cmd" -q package -DskipTests
)
set "RC=!ERRORLEVEL!"
popd
if not "%RC%"=="0" goto fail
echo SERVER: ..\TinhLinhKhoiNguyen-Server\TLKN-Server\target\tlnt-server.jar
goto ok

:do_test
call :gradle :core:test || goto fail
echo TEST: tat ca xanh
goto ok

:do_run
rem No online retry on error: the game exiting with a non-zero code would otherwise be launched a second time.
rem No piping through Tee (keeps colors + normal keyboard input for the game window / debug console).
set "RUNFLAGS=--offline"
if "%ONLINE%"=="1" set "RUNFLAGS="
call "%~dp0gradlew.bat" %RUNFLAGS% --build-cache :desktop:run %PROPS%
if errorlevel 1 (
  echo.
  echo Game thoat voi ma loi. Neu loi la "No cached version ... offline mode": chay  build.bat run online
  goto fail
)
goto ok

:do_clean
call "%~dp0gradlew.bat" --stop >nul 2>&1
call "%~dp0gradlew.bat" --offline -q clean
rem android\libs: .so files are re-extracted on every build (copyAndroidNatives)
for %%d in ("build\merged-assets" "build\libs" "build\tmp" "android\libs\armeabi-v7a" "android\libs\arm64-v8a" "android\libs\x86" "android\libs\x86_64" "web\build" "ios\build" "release\web") do if exist "%%~d" rmdir /s /q "%%~d"
echo CLEAN: xong ^(release\ giu nguyen; JRE jlink desktop\build\... da xoa - lan build PC sau se tao lai^)
goto ok

rem ---------------------------------------------------------------- helpers
:gradle
rem Default --offline (fast, no network needed). Only rerun online when the error is caused by MISSING LIBRARIES;
rem code / test errors are reported immediately (previously a useless second build ran).
rem Output is shown on screen and written to build\build.log at the same time (Tee-Object).
if "%ONLINE%"=="1" (set "GOFF=") else (set "GOFF=--offline")
set "GARGS=%GOFF% %GFLAGS% %* %PROPS%"
call :gradle_tee
set "RC=%ERRORLEVEL%"
if "%RC%"=="75" (
  echo.
  echo ^>^> Thieu thu vien trong cache offline - thu lai co tai thu vien ^(online^)...
  set "GARGS=%GFLAGS% %* %PROPS%"
  call :gradle_tee
  set "RC=!ERRORLEVEL!"
)
if "%RC%"=="75" set "RC=1"
if not "%RC%"=="0" (
  echo.
  echo ^>^> Gradle loi ^(ma %RC%^). Log day du: build\build.log
  call :hint
)
exit /b %RC%

:gradle_tee
powershell -NoProfile -ExecutionPolicy Bypass -Command "[Console]::OutputEncoding=[Text.Encoding]::UTF8; cmd /d /c ('.\gradlew.bat ' + $env:GARGS + ' 2>&1') | Tee-Object -FilePath $env:GLOG; $rc=$LASTEXITCODE; if ($rc -ne 0 -and $env:GARGS -like '*--offline*' -and (Select-String -Path $env:GLOG -Quiet -Pattern 'available for offline mode','No cached version','offline mode is enabled')) { exit 75 }; exit $rc"
exit /b %ERRORLEVEL%

:hint
rem Fix hints for common errors (read from build\build.log)
powershell -NoProfile -Command "$l=Get-Content -LiteralPath $env:GLOG -Raw -ErrorAction SilentlyContinue; if(-not $l){exit}; $h=@{'Failed to clean up stale outputs'='Game dang chay (IDE / TLNT.bat) giu file - tat game roi build lai.';'Khong thay Inno Setup'='Cai Inno Setup 6: winget install JRSoftware.InnoSetup';'khong phai UTF-8'='client.properties phai luu UTF-8 (Notepad: Save As > UTF-8).';'SDK location not found'='Thieu Android SDK: sua sdk.dir trong client.properties.';'Unsupported class file major version'='Sai JDK: dat JAVA_HOME = JDK 20.';'OutOfMemoryError'='Gradle het RAM: giam org.gradle.jvmargs hoac tat bot chuong trinh.';'Keystore was tampered'='Sai mat khau keystore trong android\keystore.properties.';'used by another process'='File dang bi khoa (antivirus / Explorer / game dang mo) - dong lai roi build lai.'}; foreach($k in $h.Keys){ if($l -match [regex]::Escape($k)){ Write-Host ('   GOI Y: '+$h[$k]) -ForegroundColor Yellow } }"
exit /b 0

:check_java
rem AGP + the core/desktop toolchain need JDK 20. If JAVA_HOME is not set, try the default install location.
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto check_java_ver
for /d %%j in ("%ProgramFiles%\Java\jdk-20*" "%ProgramFiles%\Eclipse Adoptium\jdk-20*") do if exist "%%~j\bin\java.exe" set "JAVA_HOME=%%~j"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (echo    JAVA_HOME tu tim: %JAVA_HOME% & goto check_java_ver)
echo Khong thay JDK: cai JDK 20 ^(bo cai trong ..\TLKN-Dev\^) va dat JAVA_HOME.
exit /b 1
:check_java_ver
set "JV="
rem Write to a temp file then read it: for /f ('"path with spaces" ...') makes cmd drop the quotes
"%JAVA_HOME%\bin\java.exe" -version 2> "%TEMP%\tlnt-java-ver.txt"
for /f "usebackq tokens=3" %%v in (`findstr /i "version" "%TEMP%\tlnt-java-ver.txt"`) do if not defined JV set "JV=%%~v"
del "%TEMP%\tlnt-java-ver.txt" >nul 2>&1
set "JMAJOR="
for /f "tokens=1 delims=." %%m in ("%JV%") do set "JMAJOR=%%m"
if not defined JMAJOR set "JMAJOR=0"
if %JMAJOR% LSS 17 (echo JAVA_HOME = %JAVA_HOME% la Java %JV%: can JDK 20 ^(toi thieu 17 cho Gradle Android^). & exit /b 1)
exit /b 0

:check_sdk
set "SDK_DIR="
if exist "client.properties" (
  for /f "tokens=1,* delims==" %%a in ('findstr /b /c:"sdk.dir=" client.properties 2^>nul') do set "SDK_DIR=%%b"
)
if not defined SDK_DIR if exist "local.properties" (
  for /f "tokens=1,* delims==" %%a in ('findstr /b /c:"sdk.dir=" local.properties 2^>nul') do set "SDK_DIR=%%b"
)
if defined SDK_DIR set "SDK_DIR=!SDK_DIR:\:=:!"
if defined SDK_DIR set "SDK_DIR=!SDK_DIR:\\=\!"
if defined SDK_DIR set "SDK_DIR=!SDK_DIR:/=\!"
if not defined SDK_DIR if defined ANDROID_HOME set "SDK_DIR=%ANDROID_HOME%"
if not defined SDK_DIR if defined ANDROID_SDK_ROOT set "SDK_DIR=%ANDROID_SDK_ROOT%"
if not defined SDK_DIR (echo Thieu Android SDK: ghi sdk.dir=C:/Android/Sdk vao client.properties & exit /b 1)
if not exist "%SDK_DIR%\platforms" (echo Android SDK khong hop le: %SDK_DIR% ^(thieu platforms\^) & exit /b 1)
set "ANDROID_HOME=!SDK_DIR!"
exit /b 0

:check_keystore
if exist "android\keystore.properties" exit /b 0
echo Thieu android\keystore.properties + .jks: APK/AAB release se KHONG ky duoc ^(xem HUONG-DAN-BUILD.md muc 4^).
echo Muon cai thu nhanh thi dung:  build.bat apkdebug
exit /b 1

:check_release_host
rem A phone build pointing at localhost cannot connect (localhost = the phone itself)
if defined TLNT_SERVER_HOST (set "RH=%TLNT_SERVER_HOST%") else (
  set "RH="
  for /f "tokens=1,* delims==" %%a in ('findstr /b /c:"server.host=" client.properties') do set "RH=%%b"
)
if defined RH set "RH=!RH: =!"
if /i "%RH%"=="localhost" (
  echo CANH BAO: server.host = localhost - APK tren dien thoai se KHONG vao duoc may chu.
  echo           Dung:  build.bat apk host=IP_THAT   ^(gia lap / LAN: IP may PC, vd 192.168.1.x^)
  echo.
)
if "%RH%"=="127.0.0.1" echo CANH BAO: server.host = 127.0.0.1 - APK tren dien thoai se KHONG vao duoc may chu.
exit /b 0

:copy_out
rem %1 source  %2 destination  %3 display label
if not exist "%~1" (echo Khong thay file build ra: %~1 & exit /b 1)
copy /y "%~1" "%~2" >nul || exit /b 1
for %%f in ("%~2") do set /a "SZ=%%~zf/1048576"
echo %~3: %~2  ^(!SZ! MB^)
exit /b 0

:copy_apk
if not exist "android\build\outputs\apk\release\android-release.apk" (
  echo Chua ky duoc APK: thieu / sai android\keystore.properties + .jks ^(xem HUONG-DAN-BUILD.md^)
  exit /b 1
)
call :copy_out "android\build\outputs\apk\release\android-release.apk" "release\TLNT-%VER%.apk" "APK" || exit /b 1
call :copy_mapping
exit /b 0

:copy_mapping
rem Mapping for reading stack traces of obfuscated builds - KEEP one per release
if exist "android\build\outputs\mapping\release\mapping.txt" copy /y "android\build\outputs\mapping\release\mapping.txt" "release\mapping-apk-%VER%.txt" >nul
exit /b 0

:copy_setup
call :copy_out "desktop\build\setup\TLNT-Setup-%VER%.exe" "release\TLNT-Setup-%VER%.exe" "BO CAI" || exit /b 1
exit /b 0

:copy_web
if exist "release\web" rmdir /s /q "release\web"
robocopy "web\build\dist\js\webapp" "release\web" /e /mt /njh /njs /nfl /ndl /np >nul
if errorlevel 8 exit /b 1
echo WEB: release\web\index.html
exit /b 0

:copy_pc
if exist "release\pc\TLNT" rmdir /s /q "release\pc\TLNT"
if exist "release\pc\TLNT" (echo Khong xoa duoc release\pc\TLNT - game dang mo tu thu muc nay? Tat game roi build lai. & exit /b 1)
robocopy "desktop\build\exe\TLNT" "release\pc\TLNT" /e /mt /njh /njs /nfl /ndl /np >nul
if errorlevel 8 exit /b 1
if exist "desktop\build\obf\mapping.txt" copy /y "desktop\build\obf\mapping.txt" "release\mapping-pc-%VER%.txt" >nul
echo PC: release\pc\TLNT\TLNT.bat
exit /b 0

:now
rem %1 = name of the variable that receives 1/100 seconds since midnight (parses %TIME% both as " 9:05:03.07" and "09:05:03,07")
for /f "tokens=1-4 delims=:.," %%a in ("%TIME: =0%") do set /a "%1=((1%%a-100)*3600+(1%%b-100)*60+(1%%c-100))*100+(1%%d-100)"
exit /b 0

:ok
call :now T1
set /a "DT=(T1-T0)/100"
if %DT% LSS 0 set /a "DT+=86400"
set /a "DM=DT/60, DS=DT%%60"
echo.
echo ===== XONG trong %DM% phut %DS% giay =====
set "EXITCODE=0"
goto end

:fail
echo.
echo ===== LOI - xem thong bao phia tren =====
set "EXITCODE=1"

:end
if defined OLD_CP chcp %OLD_CP% >nul <nul
if "%PAUSE_END%"=="1" pause
endlocal & exit /b %EXITCODE%
