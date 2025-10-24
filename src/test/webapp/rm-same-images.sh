#!/bin/bash
# .git/hooks/pre-commit

# Získaj zoznam zmenených JPG súborov
CHANGED_FILES=$(git diff --name-only --diff-filter=M | grep -E "\.jpg$|\.jpeg$|\.png$|\.gif$")

#clear diff dir
DIFF_BASE_DIR="../../../build/images-diff"
rm -rf "$DIFF_BASE_DIR"

for FILE in $CHANGED_FILES; do
  # Získaj starú verziu súboru z posledného commitu
  git show HEAD:"$FILE" > ../../../build/tmp/old_image.png
  ./compare-images.sh "../../../$FILE" ../../../build/tmp/old_image.png
done