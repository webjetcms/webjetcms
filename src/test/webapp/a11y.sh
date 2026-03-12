#!/bin/sh

rm -rf ../../../build/test/
rm -rf ../../../build/test-results/
npm run a11y
echo "Generating report"
npx allure generate --clean ../../../build/test/allure-results -o ../../../build/test-results
echo "Opening report"
npx allure open ../../../build/test-results -p 3001