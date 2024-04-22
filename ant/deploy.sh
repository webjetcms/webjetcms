#!/bin/sh

echo "WARNING: CHECK VERSION IN build.xml"

date

./mount-license.sh

ant update-version

ant fixSrc

#ant deploy
ant -Dcompress=true deployRepoIwmspSk

#GITHUB: just run ant deployStaging
#then close repo and deploy it

cd ..

gradlew war

gradlew --info dependencyCheckAnalyze

open build/reports/dependency-check-report.html

gradlew -q dependencies --configuration runtimeClasspath

date