#!/bin/sh

date
echo ">>>>>>>>>>> Compiling project"

cd ..
gradlew clean
gradlew compileJava --refresh-dependencies --info
gradlew npminstall
gradlew npmbuild

date
echo ">>>>>>>>>>> Executing JUnit test"
gradlew cleanTest test

date
echo ">>>>>>>>>>> Starting app server"
webjetDbname=/poolman-local.xml gradlew appBeforeIntegrationTest

date
echo ">>>>>>>>>>> Executing e2e/codeceptjs tests"
cd src/test/webapp
npm run singlethread
npm run parallel8
cd ../../..

sleep 30

date
echo ">>>>>>>>>>> Stopping app server"
gradlew appAfterIntegrationTest

sleep 120

date
echo ">>>>>>>>>>> Killing task appBeforeIntegrationTest"
ps uax | grep gradle
echo ">>>>>>>>>>> Killing task appBeforeIntegrationTest"
pkill -f appBeforeIntegrationTest
echo ">>>>>>>>>>> Killing task appBeforeIntegrationTest"
ps uax | grep gradle

echo ">>>>>>>>>>> Updating docs structure with report"
rm -rf ./docs/codecoverage-report
mkdir ./docs/codecoverage-report
rsync -av ./build/jacoco/report/ ./docs/codecoverage-report