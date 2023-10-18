#!/bin/bash
#Re/instaluje vsetky NPM zavislosti a skripty

cd src/main/webapp/admin/v9
rm -rf node_modules
npm install
npm audit
rm -rf dist
npm run prod

cd ../../../../test/webapp
rm -rf node_modules
npm install
npm audit

cd ../../../
./gradlew compileJava --refresh-dependencies --info
