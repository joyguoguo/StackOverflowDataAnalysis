@echo off
REM 数据导入脚本 (Windows)

REM 检查参数
if "%1"=="" (
    echo Usage: import-data.bat [directory]
    echo Example: import-data.bat Sample_SO_data
    echo.
    echo Using default directory: Sample_SO_data
    set IMPORT_DIR=Sample_SO_data
) else (
    set IMPORT_DIR=%1
)

echo ========================================
echo Stack Overflow Data Import Tool
echo ========================================
echo Importing from local JSON files to PostgreSQL
echo Import directory: %IMPORT_DIR%
echo Target database: stackoverflow_java
echo ========================================
echo.

REM 先编译项目
echo Compiling project...
call mvnw.cmd clean compile -q
if errorlevel 1 (
    echo Failed to compile project. Please check errors above.
    pause
    exit /b 1
)

echo.
echo Starting data import...
echo.

REM 使用环境变量传递参数，然后运行导入工具
set IMPORT_DIRECTORY=%IMPORT_DIR%
call mvnw.cmd exec:java -Dexec.mainClass="cs209a.finalproject_demo.importer.DataImporterApplication" -q
if errorlevel 1 (
    echo.
    echo Import failed. Please check errors above.
    pause
    exit /b 1
)

echo.
echo Import completed!
pause

