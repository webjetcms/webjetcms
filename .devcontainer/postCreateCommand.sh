#!/bin/sh
sudo apt update
sudo apt install iputils-ping
sudo apt -y install imagemagick
sudo apt -y install libxtst6
sudo apt -y install libxi6

TZ=Europe/Bratislava
sudo -- sh -c "ln -snf /usr/share/zoneinfo/$TZ /etc/localtime"
sudo -- sh -c "echo $TZ > /etc/timezone"

#playwright setup
npx playwright install
npx playwright install-deps
