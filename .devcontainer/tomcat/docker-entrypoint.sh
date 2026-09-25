#!/bin/sh

set -eu

poolman_file="/usr/local/tomcat/conf/poolman.xml"
certificate_file="/usr/local/tomcat/conf/webjet-https-keystore.p12"

for required_file in "${poolman_file}" "${certificate_file}"; do
    if [ ! -f "${required_file}" ]; then
        echo "Required Tomcat configuration is missing: ${required_file}" >&2
        exit 1
    fi
done

chmod 600 "${poolman_file}" "${certificate_file}"

exec "$@"

