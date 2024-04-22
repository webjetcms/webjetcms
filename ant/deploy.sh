#!/bin/sh

echo "WARNING: CHECK VERSION IN build.xml"

date

./mount-license.sh

ant update-version

ant fixSrc

#ant deploy
ant -Dcompress=true deployRepoIwmspSk

cd ..

gradlew war

gradlew --info dependencyCheckAnalyze

open build/reports/dependency-check-report.html

gradlew -q dependencies --configuration runtimeClasspath

date