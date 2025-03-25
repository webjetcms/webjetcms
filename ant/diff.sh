#!/bin/bash

if [ $# -ne 2 ]; then
    echo "Usage: $0 <folder1> <folder2>"
    exit 1
fi

folder1_path="$1"
folder2_path="$2"

# List all files in first folder
find "$folder1_path" -type f | sed "s#${folder1_path}/##" | sort > files_in_folder1.txt

# List all files in second folder
find "$folder2_path" -type f | sed "s#${folder2_path}/##" | sort > files_in_folder2.txt

echo 'Files in folder1 that are not in folder2:'
comm -23 files_in_folder1.txt files_in_folder2.txt

echo ''
echo 'Files in folder2 that are not in folder1:'
comm -23 files_in_folder2.txt files_in_folder1.txt

# Clean up
rm files_in_folder1.txt files_in_folder2.txt