#!/bin/bash

set -e  # Exit immediately if any command fails
set -u  # Treat unset variables as an error
set -o pipefail  # Catch errors in pipelines

# Define constants
EDITOR_ZIP_PATH="src/js/plugins/Editor-2.3.2.zip"
XLSX_PACKAGE_JSON="node_modules/xlsx/package.json"
ELFINDER_SRC="node_modules/@webjetcms/elfinder/dist"
ELFINDER_DEST="../elfinder"

# Function to log messages
log() {
    echo "[INFO] $1"
}

# Run DataTables Editor install script with ZIP path
log "Running DataTables Editor install script..."
node node_modules/datatables.net-editor/install.js "$EDITOR_ZIP_PATH"

# Modify xlsx package.json to replace "exports" with "exportsall"
log "Modifying xlsx package.json..."
npx replace-in-file "exports" "exportsall" "$XLSX_PACKAGE_JSON"

# Remove jQuery UI external files
log "Removing jQuery UI external files..."
rm -rf node_modules/jquery-ui/external/jquery*

# Create destination folders for elFinder
log "Creating destination folders for elFinder..."
mkdir -p "$ELFINDER_DEST/js/i18n" "$ELFINDER_DEST/js/worker" "$ELFINDER_DEST/css" "$ELFINDER_DEST/img" "$ELFINDER_DEST/sounds"

# Copy elFinder assets
log "Copying elFinder assets..."
cp "$ELFINDER_SRC/css/elfinder.full.css" "$ELFINDER_DEST/css"
cp "$ELFINDER_SRC/css/theme.css" "$ELFINDER_DEST/css"

cp "$ELFINDER_SRC/js/elfinder.full.js" "$ELFINDER_DEST/js"
cp "$ELFINDER_SRC/js/i18n/"*.js "$ELFINDER_DEST/js/i18n"
cp "$ELFINDER_SRC/js/worker/"*.js "$ELFINDER_DEST/js/worker"

cp "$ELFINDER_SRC/img/"*.{png,gif,svg} "$ELFINDER_DEST/img" 2>/dev/null || true
cp "$ELFINDER_SRC/sounds/"*.wav "$ELFINDER_DEST/sounds" 2>/dev/null || true

log "Post-installation script completed successfully."