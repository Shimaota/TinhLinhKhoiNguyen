@echo off
rem Launches the game (unpacked build, no installer). Runs the bundled JRE's javaw.exe directly (digitally signed,
rem not falsely flagged by antivirus like a self-compiled exe). Working directory = this folder; configuration
rem (game name, server) is built into app\TLNT.jar.
rem RAM: small initial heap (64 MB), max 1 GB; G1 returns unused heap to Windows every 30 s when idle.
rem NOTE: the game folder path must NOT contain Vietnamese accented characters (Java cannot load java.dll).
start "" /D "%~dp0" "%~dp0runtime\bin\javaw.exe" -Xms64m -Xmx1g -XX:+UseG1GC -XX:G1PeriodicGCInterval=30000 -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=30 -XX:+UseStringDeduplication -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar "%~dp0app\TLNT.jar" %*
