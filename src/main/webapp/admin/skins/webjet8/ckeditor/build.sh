#!/bin/bash
#Build CKEditor dist folder from WebJET github version
WJ_PATH=$PWD
cd ../../../../../../../../github.com_webjetcms/libs-ckeditor4/dev/builder
build.sh

#Zmaz nepotrebne subory
rm -rf release/ckeditor/.github
rm -rf release/ckeditor/adapters
rm -rf release/ckeditor/samples
rm -rf release/ckeditor/vendor
rm -rf release/ckeditor/bender*
rm -rf release/ckeditor/*.md
rm -rf release/ckeditor/skins/kama
rm -rf release/ckeditor/skins/moono-lisa

echo $WJ_PATH
#zmaz dist priecinok
rm -rf $WJ_PATH/dist
mkdir $WJ_PATH/dist

#zmaz povodne src priecinky
rm -rf $WJ_PATH/adapters
rm -rf $WJ_PATH/core
rm -rf $WJ_PATH/dev
rm -rf $WJ_PATH/lang
rm -rf $WJ_PATH/plugins
rm -rf $WJ_PATH/skins
rm -f $WJ_PATH/ckeditor-src.js
rm -f $WJ_PATH/ckeditor.js
rm -f $WJ_PATH/config.js
rm -f $WJ_PATH/contents.js
rm -f $WJ_PATH/CHANGES.md
rm -f $WJ_PATH/README.md
rm -f $WJ_PATH/styles.js

#nakopiruj aktualne subory
cp -R -p release/ckeditor/ $WJ_PATH/dist
cp -R -p ../../plugins/webjetcomponents/samples $WJ_PATH/dist/plugins/webjetcomponents

