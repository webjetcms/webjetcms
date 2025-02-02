#!/bin/sh

#enable MacOS Xquartz connection
xhost +localhost

#start ssh-agent
#eval "$(ssh-agent -s)"

#add local SSH key to ssh-agent
ssh-add $HOME/.ssh/id_rsa
ssh-add -l