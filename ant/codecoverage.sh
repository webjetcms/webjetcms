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
gradlew test

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
rm -rf ./docs/developer/testing/codecoverage-report
mkdir ./docs/developer/testing/codecoverage-report
rsync -av --exclude '*.java.html' ./build/jacoco/report/ ./docs/developer/testing/codecoverage-report