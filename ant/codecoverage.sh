#!/bin/sh

date
echo "ENABLE logging to file in logback.xml AND ALSO appender-ref"
echo "press ENTER to continue"
read -n 1 -s

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
date
npm run parallel4
date
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
rsync -a ./build/jacoco/report/ ./docs/codecoverage-report

date