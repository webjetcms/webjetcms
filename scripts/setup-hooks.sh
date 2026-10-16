#!/bin/sh
# Sets up git to use the shared hooks in .githooks directory.
# Run this script once after cloning the repository.
#
# Usage: sh scripts/setup-hooks.sh

set -e

HOOKS_DIR=".githooks"
BASELINE=".secrets.baseline"

# Verify we are in the root of the repository
if [ ! -d ".git" ]; then
    echo "ERROR: This script must be run from the root of the repository."
    exit 1
fi

# Configure git to use the shared hooks directory
git config core.hooksPath "$HOOKS_DIR"
echo "Git hooks path set to: $HOOKS_DIR"

# Check if detect-secrets is installed
if ! command -v detect-secrets >/dev/null 2>&1; then
    echo ""
    echo "WARNING: detect-secrets is not installed."
    echo "         Install it with: pip install detect-secrets"
    echo ""
else
    INSTALLED_VERSION=$(detect-secrets --version 2>/dev/null || echo "unknown")
    echo "detect-secrets version: $INSTALLED_VERSION"

    # Create baseline if it does not exist yet
    if [ ! -f "$BASELINE" ]; then
        echo "Creating initial secrets baseline..."
        detect-secrets scan \
            --exclude-files '\.gradle/' \
            --exclude-files 'build/' \
            --exclude-files 'node_modules/' \
            > "$BASELINE"
        echo "Baseline created: $BASELINE"
        echo "Please review and commit it: git add $BASELINE"
    else
        echo "Existing baseline found: $BASELINE"
        echo "To update the baseline run: detect-secrets scan > $BASELINE"
    fi
fi

echo ""
echo "Setup complete! The pre-commit hook will now scan staged files for secrets."
