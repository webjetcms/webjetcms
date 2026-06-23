#!/bin/bash

set -e  # Exit immediately if any command fails
set -u  # Treat unset variables as an error
set -o pipefail  # Catch errors in pipelines

# Define constants
EDITOR_ZIP_PATH="src/js/plugins/Editor-2.5.2.zip"
XLSX_PACKAGE_JSON="node_modules/xlsx/package.json"
ELFINDER_SRC="node_modules/@webjetcms/elfinder/dist"
ELFINDER_DEST="../elfinder"

# Function to log messages
log() {
    echo "[INFO] $1"
}

# Run DataTables Editor install script with ZIP path
log "Running DataTables Editor install script..."
node node_modules/datatables.net-editor/install.js $EDITOR_ZIP_PATH

# Modify xlsx package.json to replace "exports" with "exportsall"
log "Modifying xlsx package.json..."
npx replace-in-file exports exportsall $XLSX_PACKAGE_JSON

# Remove jQuery UI external files
log "Removing jQuery UI external files..."
rm -rf node_modules/jquery-ui/external/jquery*

rm -rf node_modules/bootstrap-select/docs

# Create destination folders for elFinder
log "Creating destination folders for elFinder..."
mkdir -p $ELFINDER_DEST/js/i18n $ELFINDER_DEST/js/worker $ELFINDER_DEST/css $ELFINDER_DEST/img $ELFINDER_DEST/sounds

# Copy elFinder assets
log "Copying elFinder assets..."
cp $ELFINDER_SRC/css/elfinder.full.css $ELFINDER_DEST/css
cp $ELFINDER_SRC/css/theme.css $ELFINDER_DEST/css

cp $ELFINDER_SRC/js/elfinder.full.js $ELFINDER_DEST/js
cp $ELFINDER_SRC/js/i18n/elfinder.cs.js $ELFINDER_DEST/js/i18n
cp $ELFINDER_SRC/js/i18n/elfinder.de.js $ELFINDER_DEST/js/i18n
cp $ELFINDER_SRC/js/i18n/elfinder.sk.js $ELFINDER_DEST/js/i18n
cp $ELFINDER_SRC/js/worker/*.js $ELFINDER_DEST/js/worker

cp $ELFINDER_SRC/img/*.{png,gif,svg} $ELFINDER_DEST/img 2>/dev/null || true
cp $ELFINDER_SRC/sounds/*.wav $ELFINDER_DEST/sounds 2>/dev/null || true

# FIX: tabler icons-webfont filled names
# Create -filled-fixed version of SCSS by copying and modifying the standard version
# so you can have both outline and filled icons available in SCSS without conflicts.
log "Creating -filled-fixed version of tabler icons SCSS..."
TABLER_ICONS_DIR="node_modules/@tabler/icons-webfont/dist"
if [[ -f "$TABLER_ICONS_DIR/tabler-icons.scss" ]]; then
  cp "$TABLER_ICONS_DIR/tabler-icons-filled.scss" "$TABLER_ICONS_DIR/tabler-icons-filled-fixed.scss"
  # Add -filled suffix to all icon variable declarations
  sed -i.bak 's/\$ti-icon-\([a-z0-9-]*\):/\$ti-icon-\1-filled:/g' "$TABLER_ICONS_DIR/tabler-icons-filled-fixed.scss"
  # Add -filled suffix to content references (but avoid double-adding if already -filled)
  sed -i.bak 's/content: \$ti-icon-\([a-z0-9-]*\);/content: \$ti-icon-\1-filled;/g' "$TABLER_ICONS_DIR/tabler-icons-filled-fixed.scss"
  # Fix any accidental double -filled suffixes in content (e.g., -filled-filled)
  sed -i.bak 's/content: \$ti-icon-\([a-z0-9-]*\)-filled-filled;/content: \$ti-icon-\1-filled;/g' "$TABLER_ICONS_DIR/tabler-icons-filled-fixed.scss"
  # Add -filled suffix to CSS class selectors (e.g., .#{$ti-prefix}-caret-up:before -> .#{$ti-prefix}-caret-up-filled:before)
  sed -i.bak 's/\.\#{$ti-prefix}-\([a-z0-9-]*\):before/.#{$ti-prefix}-\1-filled:before/g' "$TABLER_ICONS_DIR/tabler-icons-filled-fixed.scss"
  # Clean up backup files
  rm -f "$TABLER_ICONS_DIR/tabler-icons-filled-fixed.scss.bak"
  log "Successfully created and modified tabler icons -filled-fixed version"
else
  log "WARNING: tabler icons SCSS not found at $TABLER_ICONS_DIR/tabler-icons.scss"
fi

log "Post-installation script completed successfully."