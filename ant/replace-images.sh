#!/bin/sh
# Replace images across client static-file directories, mainly for WebJET Cloud.
# Used to replace copyright-infringing images in design templates
#
# Usage: ./ant/replace-images.sh <URL_PATH> <REPLACEMENT_IMAGE> [BASE_SEARCH_PATH]
#
# Arguments:
#   URL_PATH          - URL path to search for, e.g. /images/template/temp5/foto1a.jpg
#   REPLACEMENT_IMAGE - path to the replacement image file, e.g. /tmp/foto1a.jpg
#   BASE_SEARCH_PATH  - (optional) base directory to search, default: /mnt/shared/static/
#
# Example:
#   ./ant/replace-images.sh /images/template/temp5/foto1a.jpg /tmp/foto1a.jpg
#   ./ant/replace-images.sh /images/template/temp5/foto1a.jpg /tmp/foto1a.jpg /www/tomcat_c2/webapps/
#
# NOTE: Run as root to preserve file ownership (uses cp --preserve=ownership)

if [ $# -lt 2 ] || [ $# -gt 3 ]; then
    echo "Usage: $0 <URL_PATH> <REPLACEMENT_IMAGE> [BASE_SEARCH_PATH]"
    echo ""
    echo "  URL_PATH          - URL path to search for, e.g. /images/template/temp5/foto1a.jpg"
    echo "  REPLACEMENT_IMAGE - path to the replacement image file"
    echo "  BASE_SEARCH_PATH  - (optional) base directory to search, default: /mnt/shared/static/"
    exit 1
fi

URL_PATH="$1"
REPLACEMENT_IMAGE="$2"
BASE_DIR="${3:-/mnt/shared/static/}"

# Validate replacement image exists
if [ ! -f "$REPLACEMENT_IMAGE" ]; then
    echo "ERROR: Replacement image not found: $REPLACEMENT_IMAGE"
    exit 1
fi

# Validate base search directory exists
if [ ! -d "$BASE_DIR" ]; then
    echo "ERROR: Base search directory not found: $BASE_DIR"
    exit 1
fi

echo "Searching for: *${URL_PATH}"
echo "Replacement:   ${REPLACEMENT_IMAGE}"
echo "Search path:   ${BASE_DIR}"
echo ""

# Find all matching files
MATCHES=$(find "$BASE_DIR" -path "*${URL_PATH}" -type f)

if [ -z "$MATCHES" ]; then
    echo "No matching files found."
    exit 0
fi

# Display found matches
COUNT=0
echo "Found matches:"
echo "---"
for FILE in $MATCHES; do
    COUNT=$((COUNT + 1))
    echo "  ${COUNT}. ${FILE}"
done
echo "---"
echo "Total: ${COUNT} file(s)"
echo ""

# Ask for confirmation
printf "Replace all %d file(s) with %s? [Y/n]: " "$COUNT" "$REPLACEMENT_IMAGE"
read -r CONFIRM

if [ "$CONFIRM" != "Y" ] && [ "$CONFIRM" != "y" ]; then
    echo "Aborted."
    exit 0
fi

echo ""

# Replace files
SUCCESS=0
FAILED=0
for FILE in $MATCHES; do
    if cp --preserve=ownership "$REPLACEMENT_IMAGE" "$FILE"; then
        echo "  OK: ${FILE}"
        SUCCESS=$((SUCCESS + 1))
    else
        echo "  FAILED: ${FILE}"
        FAILED=$((FAILED + 1))
    fi
done

echo ""
echo "Done. Replaced: ${SUCCESS}, Failed: ${FAILED}, Total: ${COUNT}"
