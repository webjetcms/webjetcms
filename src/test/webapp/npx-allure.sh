#!/bin/bash

#priklady spustenia:
#npx-allure.sh firefox http://iwcm.interway.sk
#npx-allure.sh chromium http://iwcm.interway.sk
#npx-allure.sh chromium http://demo.webjetcms.sk

echo "Creating test/history directory"
pwd
rm -rf ../../../build/test
mkdir ../../../build/test
#ls -la ../../../build/test
mkdir ../../../build/test/history
#ls -la ../../../build/test/history

# --------------------------------------------------------------------
# ARGS
if [[ -z "$1" ]]; then
        CODECEPT_BROWSER="chromium"
else
        CODECEPT_BROWSER="$1"
fi

if [[ -z "$2" ]]; then
        CODECEPT_URL="http://demotest.webjetcms.sk"
else
        CODECEPT_URL="$2"
fi

if [[ -z "$3" ]]; then
        HOST_USER="tomcat_au20"
else
        HOST_USER="$3"
fi

if [[ -z "$4" ]]; then
        HOST_NAME="webjet2b.srv.iway.local"
else
        HOST_NAME="$4"
fi

if [[ -z "$5" ]]; then
        HOST_DIR="/www/tomcat_au20/webapps/docs.webjetcms.sk/allure/"
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
#TODO: tomcat_au20@
#rsync -rtlpPI tomcat_au20@webjet2b.srv.iway.local:/www/tomcat_au20/webapps/docs.webjetcms.sk/allure/chromium/history ../../../build/test
#echo "Executing: rsync -r $HOST_USER@$HOST_NAME:$HOST_DIR$CODECEPT_BROWSER/history ../../../build/test"
echo "Excecuting scp $HOST_USER@$HOST_NAME:$HOST_DIR$CODECEPT_BROWSER/history/* ../../../build/test/history"
scp $HOST_USER@$HOST_NAME:$HOST_DIR$CODECEPT_BROWSER/history/* ../../../build/test/history

ls -la ../../../build/test
ls -la ../../../build/test/history

#generovanie screenshotu
#CODECEPT_URL="http://demotest.webjetcms.sk" CODECEPT_SHOW=false npx codeceptjs run tests/admin/datatables.js --grep 'Nastavenie tabulky'

CODECEPT_RESTART='session' CODECEPT_SHOW=false CODECEPT_BROWSER=$CODECEPT_BROWSER CODECEPT_URL=$CODECEPT_URL npx codeceptjs run --plugins allure --steps
RET_CODE=$?

#skopiruj konfiguracne subory pre allure z gitu do test adresara
cp -r allure/ ../../../build/test

#vygeneruj enviroment.properties subor
#rm ../../../build/test/environment.properties
printf "Browser=$CODECEPT_BROWSER\nBrowser.Version=$BROWSER_VERSION\nStand=$CODECEPT_URL" >> ../../../build/test/environment.properties

#vygeneruj report do test-results adresara
npx allure generate --clean ../../../build/test -o ../../../build/test-results

#uloz na docs server vysledok
#TODO: tomcat_au20@
rsync -rtlpPI --delete --inplace --quiet --chmod=ug+rwX ../../../build/test-results/ $HOST_USER@$HOST_NAME:$HOST_DIR$CODECEPT_BROWSER

#npx allure open ../../../build/test-results
#dostupne ako https://docs.webjetcms.sk/allure/$CODECEPT_BROWSER

#zaarchivuj vystup, aby sa to dalo nasledne ulozit ako artifakt v pipeline (pre istotu, keby sa rsync pokazil a zmazal subory)
tar cvfz ../../../build/test-results-$CODECEPT_BROWSER.tgz ../../../build/test-results/

echo "Done, return code=$RET_CODE"
exit $RET_CODE