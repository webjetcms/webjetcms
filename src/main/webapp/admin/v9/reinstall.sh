#!/bin/sh

echo "Removing old dist"
rm -rf dist
echo "Removing node_modules"
rm -rf node_modules
echo "Removing package-lock.json"
rm package-lock.json
echo "installing node_modules"
npm install
echo "Mame custom PUG loader, skontroluj ci uz nefixli zavislost na pug 3.0"
echo "Mame upravene xlsx/package.jsp - odstranene exports kvoli amcharts4, ak je nova verzia odstran z postinstall a zrus package replace-in-file"
echo "Mame upraveny tui-image-editor, skontroluj ci uz nefixli fabricjs"