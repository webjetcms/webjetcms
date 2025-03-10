#!/bin/sh

echo "Removing old dist"
#rm -rf dist
echo "Removing node_modules"
#rm -rf node_modules
echo "Removing package-lock.json"
#rm package-lock.json
echo "installing node_modules"
#npm install

#
echo "Updating v8 version files"
cp node_modules/jquery/dist/jquery.js ../skins/webjet8/assets/global/plugins/
cp node_modules/jquery/dist/jquery.min.js ../skins/webjet8/assets/global/plugins/
cp node_modules/jquery/dist/jquery.min.map ../skins/webjet8/assets/global/plugins/

cp node_modules/bootstrap/dist/js/bootstrap.js ../skins/webjet8/assets/global/plugins/bootstrap/js/
cp node_modules/bootstrap/dist/js/bootstrap.js.map ../skins/webjet8/assets/global/plugins/bootstrap/js/
cp node_modules/bootstrap/dist/js/bootstrap.min.js ../skins/webjet8/assets/global/plugins/bootstrap/js/
cp node_modules/bootstrap/dist/js/bootstrap.min.js.map ../skins/webjet8/assets/global/plugins/bootstrap/js/

cp node_modules/bootstrap/dist/css/bootstrap.css ../skins/webjet8/assets/global/plugins/bootstrap/css/
cp node_modules/bootstrap/dist/css/bootstrap.css.map ../skins/webjet8/assets/global/plugins/bootstrap/css/
cp node_modules/bootstrap/dist/css/bootstrap.min.css ../skins/webjet8/assets/global/plugins/bootstrap/css/
cp node_modules/bootstrap/dist/css/bootstrap.min.css.map ../skins/webjet8/assets/global/plugins/bootstrap/css/

cp src/js/plugins/bootstrap-select-v1.14.0-gamma1.js ../skins/webjet8/assets/global/plugins/bootstrap-select/bootstrap-select.js
cp node_modules/@popperjs/core/dist/umd/popper.min.js ../skins/webjet8/assets/global/plugins/bootstrap/js/
cp node_modules/@popperjs/core/dist/umd/popper.min.js.map ../skins/webjet8/assets/global/plugins/bootstrap/js/

echo "Mame custom PUG loader, skontroluj ci uz nefixli zavislost na pug 3.0"
echo "Mame upravene xlsx/package.json - odstranene exports kvoli amcharts4, ak je nova verzia odstran z postinstall a zrus package replace-in-file"
#echo "Mame upraveny tui-image-editor, skontroluj ci uz nefixli fabricjs"

echo "Updating vanilla calendar PRO"

curl -o ../../components/_common/vannila-calendar-pro/styles/index.css "https://cdn.jsdelivr.net/npm/vanilla-calendar-pro@3/styles/index.css"
#curl -o ../../components/_common/vannila-calendar-pro/styles/layout.css "https://cdn.jsdelivr.net/npm/vanilla-calendar-pro@3/styles/layout.css"
curl -o ../../components/_common/vannila-calendar-pro/index.js "https://cdn.jsdelivr.net/npm/vanilla-calendar-pro@3/index.js"