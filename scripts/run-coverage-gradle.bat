@echo off
setlocal
rem Explorer-double-click launcher for the tests with a JaCoCo report under
rem build/reports/. Runs the checkout this script sits in, so what is measured is
rem the working tree rather than whatever copy was last synced into an install's
rem mods/ folder. The install comes from the build's own lookup - -PstarsectorRoot,
rem STARSECTOR_HOME or the machine-local gradle.properties. Extra arguments,
rem -Plocale=<tag> among them, pass through to Gradle.

cd /d "%~dp0.."
call gradlew.bat coverage %*
set rc=%errorlevel%
pause
exit /b %rc%
