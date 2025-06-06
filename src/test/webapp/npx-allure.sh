#!/bin/bash

#priklady spustenia:
#npx-allure.sh firefox http://iwcm.interway.sk
#npx-allure.sh chromium http://iwcm.interway.sk
#npx-allure.sh chromium https://demotest.webjetcms.sk

echo "Creating test/history directory"
pwd
ls -la ../../../
mkdir ../../../build
ls -la ../../../build
rm -rf ../../../build/test
mkdir ../../../build/test
mkdir ../../../build/test/allure-results
ls -la ../../../build/test
mkdir ../../../build/test/allure-results/history
ls -la ../../../build/test/history

# --------------------------------------------------------------------
# ARGS
if [[ -z "$1" ]]; then
        CODECEPT_BROWSER="chromium"
else
        CODECEPT_BROWSER="$1"
fi

if [[ -z "$2" ]]; then
        CODECEPT_URL="https://$CODECEPT_DEFAULT_DOMAIN_NAME"
else
        CODECEPT_URL="$2"
fi

if [[ -z "$3" ]]; then
        HOST_USER="tomcat"
else
        HOST_USER="$3"
fi

if [[ -z "$4" ]]; then
        HOST_NAME="webjet.srv.local"
else
        HOST_NAME="$4"
fi

if [[ -z "$5" ]]; then
        HOST_DIR="/www/tomcat/webapps/docs.webjetcms.sk/allure/"
else
        HOST_DIR="$5"
fi

if [ "firefox" = "$CODECEPT_BROWSER" ]; then
        BROWSER_VERSION="103.0"
else
        BROWSER_VERSION="105.0.5195.19"
fi

echo "Browser version=$BROWSER_VERSION"

#skopiruj z docs servera posledne history udaje, aby sa zobrazila historia testov na grafe
#rsync -rtlpPI tomcat@webjet2b.srv.iway.local:/www/tomcat/webapps/docs.webjetcms.sk/allure/chromium/history ../../../build/test
#echo "Executing: rsync -r $HOST_USER@$HOST_NAME:$HOST_DIR$CODECEPT_BROWSER/history ../../../build/test"
echo "Excecuting scp $HOST_USER@$HOST_NAME:$HOST_DIR$CODECEPT_BROWSER/history/* ../../../build/test/allure-results/history"
scp $HOST_USER@$HOST_NAME:$HOST_DIR$CODECEPT_BROWSER/history/* ../../../build/test/allure-results/history

ls -la ../../../build/test
ls -la ../../../build/test/allure-results/history

#vygeneruj enviroment.properties subor
#rm ../../../build/test/environment.properties
pwd
printf "Browser=$CODECEPT_BROWSER\nBrowser.Version=$BROWSER_VERSION\nStand=$CODECEPT_URL\n" > ../../../build/test/allure-results/environment.properties
echo -n "java.version=" >> ../../../build/test/allure-results/environment.properties
java --version | head -n 1 >> ../../../build/test/allure-results/environment.properties
echo -n "node.version=" >> ../../../build/test/allure-results/environment.properties
node --version | head -n 1 >> ../../../build/test/allure-results/environment.properties
echo -n "codeceptjs.version=" >> ../../../build/test/allure-results/environment.properties
npx codeceptjs --version | head -n 1 >> ../../../build/test/allure-results/environment.properties

ORIGINAL_DIR=$(pwd)
cd ../../..
./gradlew test
echo "$ORIGINAL_DIR"
cd "$ORIGINAL_DIR"

NODE_OPTIONS='--max-old-space-size=4000' CODECEPT_RESTART='session' CODECEPT_SHOW=false CODECEPT_BROWSER=$CODECEPT_BROWSER CODECEPT_URL=$CODECEPT_URL npx codeceptjs run --plugins allure
RET_CODE=$?

#skopiruj konfiguracne subory pre allure z gitu do test adresara
cp -r allure/ ../../../build/test/allure-results

#vygeneruj report do test-results adresara
npx allure generate --clean ../../../build/test/allure-results -o ../../../build/test-results

#uloz na docs server vysledok
rsync -rtlpPI --delete --inplace --quiet --chmod=ug+rwX ../../../build/test-results/ $HOST_USER@$HOST_NAME:$HOST_DIR$CODECEPT_BROWSER

#npx allure open ../../../build/test-results
#dostupne ako https://docs.webjetcms.sk/allure/$CODECEPT_BROWSER

#zaarchivuj vystup, aby sa to dalo nasledne ulozit ako artifakt v pipeline (pre istotu, keby sa rsync pokazil a zmazal subory)
tar cvfz ../../../build/test-results-$CODECEPT_BROWSER.tgz ../../../build/test-results/
tar cvfz ../../../build/test-screenshots-$CODECEPT_BROWSER.tgz ../../../build/test/screenshots/

echo "Done, return code=$RET_CODE"
exit $RET_CODE