#!/bin/bash
# 数据采集工具启动脚本 (Linux/Mac)

# 检查参数
if [ -z "$1" ]; then
    echo "Usage: ./collect-data.sh [count] [output_dir] [api_key]"
    echo "Example: ./collect-data.sh 1000 Sample_SO_data rl_iyRnJYLzYdHXWn2GhMRt9iSk6"
    echo ""
    echo "Using default values: count=1000, output_dir=Sample_SO_data"
    echo "Note: You can also set SO_API_KEY environment variable instead"
    COUNT=1000
    OUTPUT=Sample_SO_data
    API_KEY=""
else
    COUNT=$1
    OUTPUT=${2:-Sample_SO_data}
    API_KEY=${3:-""}
fi

# 如果命令行没有提供，尝试从环境变量获取
if [ -z "$API_KEY" ]; then
    API_KEY=${SO_API_KEY:-""}
fi

echo "========================================"
echo "Stack Overflow Data Collector"
echo "========================================"
echo "Target count: $COUNT"
echo "Output directory: $OUTPUT"
echo "API Key: ${API_KEY:-not set}"
echo "========================================"
echo ""

# 先编译项目
echo "Compiling project..."
./mvnw clean compile -q
if [ $? -ne 0 ]; then
    echo "Failed to compile project. Please check errors above."
    exit 1
fi

echo ""
echo "Starting data collection..."
echo ""

# 使用 Maven exec 插件运行采集工具
if [ -z "$API_KEY" ]; then
    ./mvnw exec:java -Dexec.mainClass="cs209a.finalproject_demo.collector.SimpleDataCollector" -Dexec.args="$COUNT $OUTPUT" -q
else
    ./mvnw exec:java -Dexec.mainClass="cs209a.finalproject_demo.collector.SimpleDataCollector" -Dexec.args="$COUNT $OUTPUT $API_KEY" -q
fi
if [ $? -ne 0 ]; then
    echo ""
    echo "Collection failed. Please check errors above."
    exit 1
fi

echo ""
echo "Collection completed!"

