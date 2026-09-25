@echo off
setlocal
rem Explorer-double-click launcher for the unit tests. Runs the checkout this
rem script sits in, so what is tested is the working tree rather than whatever
rem copy was last synced into an install's mods/ folder. The install comes from
rem the build's own lookup - -PstarsectorRoot, STARSECTOR_HOME or the machine-local
rem gradle.properties. Extra arguments, -Plocale=<tag> among them, pass through
rem to Gradle.

cd /d "%~dp0.."
call gradlew.bat test %*
set rc=%errorlevel%
pause
exit /b %rc%
