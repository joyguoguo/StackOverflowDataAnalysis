@echo off
REM 数据采集工具启动脚本 (Windows)

REM 检查参数
if "%1"=="" (
    echo Usage: collect-data.bat [count] [output_dir] [api_key]
    echo Example: collect-data.bat 3500 Sample_SO_data2 rl_iyRnJYLzYdHXWn2GhMRt9iSk6
    echo.
    echo Using default values: count=3500, output_dir=Sample_SO_data2
    echo Note: You can also set SO_API_KEY environment variable instead
    set COUNT=3500
    set OUTPUT=Sample_SO_data2
    set API_KEY=
) else (
    set COUNT=%1
    if "%2"=="" (
        set OUTPUT=Sample_SO_data2
    ) else (
        set OUTPUT=%2
    )
    if "%3"=="" (
        set API_KEY=rl_iyRnJYLzYdHXWn2GhMRt9iSk6
    ) else (
        set API_KEY=%3
    )
)

REM 如果命令行没有提供，尝试从环境变量获取
if "%API_KEY%"=="" (
    set API_KEY=%SO_API_KEY%
)

echo ========================================
echo Stack Overflow Data Collector
echo ========================================
echo Target count: %COUNT%
echo Output directory: %OUTPUT%
echo API Key: %API_KEY%
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
echo Starting data collection...
echo.

REM 使用 Maven exec 插件运行采集工具
if "%API_KEY%"=="" (
    call mvnw.cmd exec:java -Dexec.mainClass="cs209a.finalproject_demo.collector.SimpleDataCollector" -Dexec.args="%COUNT% %OUTPUT%" -q
) else (
    call mvnw.cmd exec:java -Dexec.mainClass="cs209a.finalproject_demo.collector.SimpleDataCollector" -Dexec.args="%COUNT% %OUTPUT% %API_KEY%" -q
)
if errorlevel 1 (
    echo.
    echo Collection failed. Please check errors above.
    pause
    exit /b 1
)

echo.
echo Collection completed!
pause
