#!/bin/sh

rm -rf ../../../build/test/
rm -rf ../../../build/test-results/
if [ "$1" = "current" ]; then
  npm run a11y:current
elif [ "$1" = "pause" ]; then
  npm run a11y:pause
else
  npm run a11y
fi

echo "Setting categories"
cp allure/categories.json ../../../build/test/allure-results

echo "Generating report"
npx allure generate --clean ../../../build/test/allure-results -o ../../../build/test-results

echo "Injecting custom CSS"
cp allure-custom.css ../../../build/test-results/allure-custom.css
sed -i '' 's|</head>|   <link rel="stylesheet" href="allure-custom.css">\n</head>|' ../../../build/test-results/index.html

echo "Opening report"
npx allure open ../../../build/test-results -p 3001