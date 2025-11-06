#!/bin/bash
# migrate to jakarta namespace, you need to have ../../webjetcms_jakarta folder checkd out with branch feature/57793-jakarta-test-src

cd ../../webjetcms_jakarta

echo "reseting local changes"
git checkout feature/57793-jakarta-test-src
git reset --hard
git pull origin feature/57793-jakarta-test-src

cd ../webjetcms/ant
echo "migrating to jakarta namespace"
ant -f jakarta-migration.xml