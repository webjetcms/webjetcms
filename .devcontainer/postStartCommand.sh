#!/bin/sh

#set local DNS
sudo -- sh -c "echo '#LOCAL DNS FOR WEBJET' >> /etc/hosts"
sudo -- sh -c "echo '127.0.0.1    iwcm.interway.sk iwcm.iway.sk cms.iway.sk docs.interway.sk' >> /etc/hosts"
sudo -- sh -c "echo '192.168.100.80   gitlab.web.iway.local' >> /etc/hosts"
sudo -- sh -c "echo '192.168.101.179  maven.web.iway.local ' >> /etc/hosts"
