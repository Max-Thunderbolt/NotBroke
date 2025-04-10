 @echo off
echo Cleaning Android project build...

:: Stop any running Gradle daemons
echo Stopping Gradle daemons...
call gradlew.bat --stop

:: Wait a moment for processes to close
timeout /t 2 /nobreak > nul

:: Remove build directories
echo Removing build directories...
if exist "app\build" (
    rmdir /s /q "app\build"
    echo Removed app\build directory
)
if exist "build" (
    rmdir /s /q "build"
    echo Removed build directory
)
if exist ".gradle" (
    rmdir /s /q ".gradle"
    echo Removed .gradle directory
)

:: Clear Android Studio's cache (optional)
if exist "%LOCALAPPDATA%\Google\AndroidStudio*\system\caches" (
    echo Clearing Android Studio caches...
    rmdir /s /q "%LOCALAPPDATA%\Google\AndroidStudio*\system\caches"
)

echo.
echo Clean completed successfully!
echo You can now rebuild your project.
pause