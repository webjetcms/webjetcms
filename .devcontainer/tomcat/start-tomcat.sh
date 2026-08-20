#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repository_dir="$(cd -- "${script_dir}/../.." && pwd)"

container_name="webjetcms-tomcat11"
image_name="webjetcms-tomcat11-local"
war_file="${repository_dir}/build/libs/webjetcms.war"
certificate_file="${repository_dir}/build/certificates/https-keystore.p12"
poolman_file="${repository_dir}/src/main/resources/poolman-local.xml"
keystore_password="changeit"
startup_timeout_seconds=600
remove_container_on_exit=false
log_follower_pid=""
follow_logs=false

if (( $# > 1 )) || { (( $# == 1 )) && [[ "$1" != "--follow" ]]; }; then
    echo "Usage: $0 [--follow]" >&2
    exit 2
fi

if (( $# == 1 )); then
    follow_logs=true
fi

stop_log_follower() {
    if [[ -n "${log_follower_pid}" ]]; then
        kill "${log_follower_pid}" >/dev/null 2>&1 || true
        wait "${log_follower_pid}" >/dev/null 2>&1 || true
    fi
}

cleanup_on_exit() {
    exit_code=$?
    stop_log_follower
    if (( exit_code != 0 )) && [[ "${remove_container_on_exit}" == "true" ]]; then
        echo "Removing incomplete container ${container_name}..." >&2
        docker rm --force "${container_name}" >/dev/null 2>&1 || true
    fi
}

trap cleanup_on_exit EXIT

show_tomcat_logs() {
    docker logs --tail 500 "${container_name}" 2>&1 || true
}

fail_with_logs() {
    echo "Tomcat did not start successfully." >&2
    show_tomcat_logs
    exit 1
}

stop_tomcat_on_interrupt() {
    trap - INT TERM
    echo
    echo "Stopping ${container_name}..."
    docker stop --time 30 "${container_name}" >/dev/null 2>&1 || true
    if [[ "${remove_container_on_exit}" == "true" ]]; then
        docker rm --force "${container_name}" >/dev/null 2>&1 || true
        remove_container_on_exit=false
    fi
    exit 0
}

for required_command in docker curl; do
    if ! command -v "${required_command}" >/dev/null 2>&1; then
        echo "Required command not found: ${required_command}" >&2
        exit 1
    fi
done

if [[ ! -x "${repository_dir}/gradlew" ]]; then
    echo "Gradle wrapper is missing or is not executable: ${repository_dir}/gradlew" >&2
    exit 1
fi

if [[ ! -f "${poolman_file}" ]]; then
    echo "Local database configuration is missing: ${poolman_file}" >&2
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    echo "Docker daemon is not available. Start Docker and run this script again." >&2
    exit 1
fi

cd "${repository_dir}"

echo "Building and verifying the WebJET WAR archive..."
WEBJET_HTTPS_KEYSTORE_PASSWORD="${keystore_password}" \
    ./gradlew clean generateCertificate verifyBootWar --console=plain

if [[ ! -f "${war_file}" ]]; then
    echo "WAR archive was not created: ${war_file}" >&2
    exit 1
fi

if [[ ! -f "${certificate_file}" ]]; then
    echo "Development certificate was not created: ${certificate_file}" >&2
    exit 1
fi

echo "Building the Tomcat 11 image..."
docker build \
    --file "${script_dir}/Dockerfile" \
    --tag "${image_name}" \
    "${script_dir}"

if docker container inspect "${container_name}" >/dev/null 2>&1; then
    echo "Replacing existing container ${container_name}..."
    docker rm --force "${container_name}" >/dev/null
fi

echo "Creating container ${container_name}..."
docker create \
    --name "${container_name}" \
    --add-host host.docker.internal:host-gateway \
    --publish 127.0.0.1:80:80 \
    --publish 127.0.0.1:443:443 \
    --env "CATALINA_OPTS=-DwebjetDbname=/usr/local/tomcat/conf/poolman.xml -Dwebjet.keystore.password=${keystore_password}" \
    --env "WEBJET_KEYSTORE_PATH=file:/usr/local/tomcat/conf/webjet-https-keystore.p12" \
    --env "WEBJET_HTTPS_KEYSTORE_PASSWORD=${keystore_password}" \
    "${image_name}" >/dev/null
remove_container_on_exit=true

docker cp "${war_file}" "${container_name}:/usr/local/tomcat/webapps/ROOT.war"
docker cp "${poolman_file}" "${container_name}:/usr/local/tomcat/conf/poolman.xml"
docker cp "${certificate_file}" "${container_name}:/usr/local/tomcat/conf/webjet-https-keystore.p12"

echo "Starting Tomcat on http://localhost and https://localhost..."
if ! docker start "${container_name}" >/dev/null; then
    echo "Tomcat container could not be started. Check whether ports 80 and 443 are already in use." >&2
    exit 1
fi

if [[ "${follow_logs}" == "true" ]]; then
    trap stop_tomcat_on_interrupt INT TERM
    docker logs --follow "${container_name}" &
    log_follower_pid=$!
fi

deadline=$((SECONDS + startup_timeout_seconds))
while (( SECONDS < deadline )); do
    tomcat_logs="$(docker logs "${container_name}" 2>&1)"

    if grep -Eq "Failed to register .*Possibly already registered|LifecycleException|Error starting child" <<<"${tomcat_logs}"; then
        fail_with_logs
    fi

    if grep -q "WebJET bootstrap mode: setup" <<<"${tomcat_logs}"; then
        echo "WebJET started in setup mode. Check the connection in poolman-local.xml." >&2
        fail_with_logs
    fi

    if [[ "$(docker inspect --format '{{.State.Running}}' "${container_name}")" != "true" ]]; then
        fail_with_logs
    fi

    http_response=""
    https_response=""
    if grep -q "WebJET bootstrap mode: production" <<<"${tomcat_logs}"; then
        http_response="$(curl --silent --output /dev/null --write-out '%{http_code}|%{content_type}' \
            --connect-timeout 1 --max-time 3 http://127.0.0.1/captcha.jpg || true)"
        https_response="$(curl --insecure --silent --output /dev/null --write-out '%{http_code}|%{content_type}' \
            --connect-timeout 1 --max-time 3 https://127.0.0.1/captcha.jpg || true)"
    fi

    if [[ "${http_response}" == "200|image/jpeg"* && "${https_response}" == "200|image/jpeg"* ]]; then
        echo
        echo "WebJET is running:"
        echo "  HTTP:  http://localhost/"
        echo "  HTTPS: https://localhost/"
        echo
        echo "The HTTPS certificate is self-signed, so the browser will display a warning."
        echo "Logs: docker logs --follow ${container_name}"
        echo "Remove: docker rm --force ${container_name}"
        remove_container_on_exit=false

        if [[ "${follow_logs}" == "true" ]]; then
            echo "Press Ctrl+C to stop Tomcat."
            wait "${log_follower_pid}"
            log_follower_pid=""
            trap - INT TERM
        fi

        exit 0
    fi

    sleep 5
done

echo "Timed out after ${startup_timeout_seconds} seconds while waiting for WebJET." >&2
fail_with_logs
