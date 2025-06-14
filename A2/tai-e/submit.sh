#!/bin/bash

# 创建目标目录A2
mkdir -p A2

# 查找并复制ConstantPropagation.java
find . -type f -name "ConstantPropagation.java" -exec cp {} A2/ \;

# 查找并复制Solver.java
find . -type f -name "Solver.java" -exec cp {} A2/ \;

# 查找并复制WorkListSolver.java
find . -type f -name "WorkListSolver.java" -exec cp {} A2/ \;

echo "文件复制完成，已将所有匹配文件复制到A2目录。"

# 压缩A2目录为A2.zip
zip -j A2.zip A2/*

# 验证压缩结果
if [ $? -eq 0 ]; then
    echo "压缩成功：A2目录已压缩为A2.zip"
else
    echo "压缩失败：执行zip命令时出错"
    exit 1
fi