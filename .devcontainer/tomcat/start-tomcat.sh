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
container_created=false
log_follower_pid=""
follow_logs=false
setup_mode=false
expected_bootstrap_mode="production"
minimum_setup_token_length=16

while (( $# > 0 )); do
    case "$1" in
        --follow)
            follow_logs=true
            ;;
        --setup)
            setup_mode=true
            expected_bootstrap_mode="setup"
            ;;
        *)
            echo "Usage: $0 [--follow] [--setup]" >&2
            exit 2
            ;;
    esac
    shift
done

setup_token=""
if [[ "${setup_mode}" == "true" ]]; then
    setup_token="${WEBJET_SETUP_TOKEN:-}"
    unset WEBJET_SETUP_TOKEN
    if (( ${#setup_token} < minimum_setup_token_length )); then
        echo "Tomcat setup mode requires WEBJET_SETUP_TOKEN with at least ${minimum_setup_token_length} characters." >&2
        exit 2
    fi
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
    elif [[ "${setup_mode}" == "true" && "${container_created}" == "true" ]] \
        && [[ "$(docker inspect --format '{{.State.Running}}' "${container_name}" 2>/dev/null || true)" != "true" ]]; then
        echo "Removing stopped setup container ${container_name}..."
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
    if [[ "${remove_container_on_exit}" == "true" || "${setup_mode}" == "true" ]]; then
        docker rm --force "${container_name}" >/dev/null 2>&1 || true
        remove_container_on_exit=false
        container_created=false
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
tomcat_jvm_args=(
    '-Dsun.net.client.defaultConnectTimeout=300000'
    '-Dsun.net.client.defaultReadTimeout=300000'
    '-Dfile.encoding=utf-8'
    '-Duser.language=sk'
    '-Duser.country=SK'
    '-Dwebjet.smtpServer=mxrelay.dev.iway.sk'
    '-Dmail.smtp.localhost=webjetcms-tomcat11.localhost'
    '-DwebjetDbname=/usr/local/tomcat/conf/poolman.xml'
    "-Dwebjet.keystore.password=${keystore_password}"
    '-Dwebjet.serverMonitoringEnableJPA=true'
    '-Dwebjet.webEnableIPs=1,2,3,4,5,6,7,8,9,0'
    '-Dwebjet.adminEnableIPs=1,2,3,4,5,6,7,8,9,0'
    '-Dwebjet.passwordAdminExpiryDays=0'
    '-Dwebjet.loggerUseAnsiColors=true'
)
container_environment_args=(
    --env "CATALINA_OPTS=${tomcat_jvm_args[*]}"
    --env "WEBJET_KEYSTORE_PATH=file:/usr/local/tomcat/conf/webjet-https-keystore.p12"
    --env "WEBJET_HTTPS_KEYSTORE_PASSWORD=${keystore_password}"
)
if [[ "${setup_mode}" == "true" ]]; then
    container_environment_args+=(
        --env "WEBJET_SETUP_ENABLED=true"
        --env WEBJET_SETUP_TOKEN
    )
    export WEBJET_SETUP_TOKEN="${setup_token}"
fi

docker create \
    --name "${container_name}" \
    --add-host host.docker.internal:host-gateway \
    --publish 127.0.0.1:80:80 \
    --publish 127.0.0.1:443:443 \
    "${container_environment_args[@]}" \
    "${image_name}" >/dev/null
unset WEBJET_SETUP_TOKEN
setup_token=""
container_created=true
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

    unexpected_bootstrap_mode="setup"
    if [[ "${expected_bootstrap_mode}" == "setup" ]]; then
        unexpected_bootstrap_mode="production"
    fi
    if grep -q "WebJET bootstrap mode: ${unexpected_bootstrap_mode}" <<<"${tomcat_logs}"; then
        echo "WebJET started in ${unexpected_bootstrap_mode} mode, but ${expected_bootstrap_mode} mode was requested." >&2
        fail_with_logs
    fi

    if [[ "$(docker inspect --format '{{.State.Running}}' "${container_name}")" != "true" ]]; then
        fail_with_logs
    fi

    http_response=""
    https_response=""
    setup_response=""
    if [[ "${expected_bootstrap_mode}" == "production" ]] && grep -q "WebJET bootstrap mode: production" <<<"${tomcat_logs}"; then
        http_response="$(curl --silent --output /dev/null --write-out '%{http_code}|%{content_type}' \
            --connect-timeout 1 --max-time 3 http://127.0.0.1/captcha.jpg || true)"
        https_response="$(curl --insecure --silent --output /dev/null --write-out '%{http_code}|%{content_type}' \
            --connect-timeout 1 --max-time 3 https://127.0.0.1/captcha.jpg || true)"
    elif [[ "${expected_bootstrap_mode}" == "setup" ]] && grep -q "WebJET bootstrap mode: setup" <<<"${tomcat_logs}"; then
        setup_response="$(curl --insecure --silent --output /dev/null --write-out '%{http_code}|%{redirect_url}' \
            --connect-timeout 1 --max-time 3 https://127.0.0.1/wjerrorpages/setup/setup || true)"
    fi

    application_ready=false
    if [[ "${expected_bootstrap_mode}" == "production" && "${http_response}" == "200|image/jpeg"* && "${https_response}" == "200|image/jpeg"* ]]; then
        application_ready=true
    elif [[ "${expected_bootstrap_mode}" == "setup" && "${setup_response}" == 30[12378]\|*/login* ]]; then
        application_ready=true
    fi

    if [[ "${application_ready}" == "true" ]]; then
        echo
        if [[ "${expected_bootstrap_mode}" == "setup" ]]; then
            echo "WebJET setup is running:"
            echo "  URL:      https://localhost/wjerrorpages/setup/setup"
            echo "  Username: setup"
            echo "  After setup, stop this task and run Docker Tomcat 11 Start."
        else
            echo "WebJET is running:"
            echo "  HTTP:  http://localhost/"
            echo "  HTTPS: https://localhost/"
        fi
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
