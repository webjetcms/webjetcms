#!/bin/sh

echo "WARNING: CHECK VERSION IN build.xml"

date

./mount-license.sh

source ~/.nvm/nvm.sh
nvm install;
nvm use;
node -v;

ant update-version

#ant fixSrc

#ant deploy
ant -Dcompress=true deployRepoIwmspSk

#GITHUB: just run ant deployStaging
#then close repo and deploy it

cd ..

gradlew war

gradlew --info dependencyCheckAnalyze

open build/reports/dependency-check-report.html

#license report
gradlew --info generateLicenseReport
open build/reports/dependency-license/index.html
gradlew --info checkLicense
open build/reports/dependency-license/dependencies-without-allowed-license.json

echo "Reguired: npm install -g license-report"
license-report --package=src/main/webapp/admin/v9/package.json --config licensereport-npm.json > build/reports/dependency-license/npm.html
open build/reports/dependency-license/npm.html

gradlew -q dependencies --configuration runtimeClasspath

date