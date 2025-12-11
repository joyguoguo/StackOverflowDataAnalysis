#!/bin/bash
# 数据导入脚本 (Linux/Mac)

# 检查参数
if [ -z "$1" ]; then
    echo "Usage: ./import-data.sh [directory]"
    echo "Example: ./import-data.sh Sample_SO_data"
    echo ""
    echo "Using default directory: Sample_SO_data"
    IMPORT_DIR="Sample_SO_data"
else
    IMPORT_DIR=$1
fi

echo "========================================"
echo "Stack Overflow Data Import Tool"
echo "========================================"
echo "Import directory: $IMPORT_DIR"
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
echo "Starting data import..."
echo ""

# 使用 Spring Boot 运行导入工具（通过环境变量传递参数）
export IMPORT_DIRECTORY=$IMPORT_DIR
./mvnw spring-boot:run \
    -Dspring-boot.run.main-class=cs209a.finalproject_demo.importer.DataImporterApplication \
    -Dspring-boot.run.arguments=--import.directory=$IMPORT_DIR -q
if [ $? -ne 0 ]; then
    echo ""
    echo "Import failed. Please check errors above."
    exit 1
fi

echo ""
echo "Import completed!"

