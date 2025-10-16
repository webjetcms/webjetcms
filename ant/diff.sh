#!/bin/bash

#execute as:
#ant/diff.sh src/main ../webjetcms_hotfix_main/src/main

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

#echo 'Files in folder1 that are not in folder2:'
#comm -23 files_in_folder1.txt files_in_folder2.txt

echo 'ADD this files into update.InitServlet'
echo ''
echo 'Files in folder2 that are not in folder1:'
comm -23 files_in_folder2.txt files_in_folder1.txt > output.txt

# Exclude lines starting with resources/ or webapp/WEB-INF/imgcache/
grep -v -e '^resources/' -e '^webapp/WEB-INF/imgcache/' -e '^webapp/admin/v9/dist/' output.txt > temp && mv temp output.txt

# Replace lines starting with webapp/ with /
sed -i '' -e 's/^webapp\//\//' output.txt
# Replace lines starting with java/ with /WEB-INF/classes/
sed -i '' -e 's/^java\//\/WEB-INF\/classes\//' output.txt

# Add files.add(" at the beginning and "); at the end of each line in output.txt
sed -i '' -e 's/^/        files.add("/' -e 's/$/");/' output.txt

cat output.txt

# Clean up
rm files_in_folder1.txt files_in_folder2.txt output.txt