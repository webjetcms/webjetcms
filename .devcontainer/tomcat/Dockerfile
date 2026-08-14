FROM tomcat:11.0.23-jdk17-temurin-noble

ENV TZ=Europe/Bratislava

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install --yes --no-install-recommends \
        imagemagick \
        locales \
        tzdata \
    && printf '%s\n' 'sk_SK.UTF-8 UTF-8' > /etc/locale.gen \
    && locale-gen \
    && update-locale LANG=sk_SK.UTF-8 LANGUAGE=sk_SK:sk \
    && ln -snf "/usr/share/zoneinfo/${TZ}" /etc/localtime \
    && printf '%s\n' "${TZ}" > /etc/timezone \
    && rm -rf /var/lib/apt/lists/* /usr/local/tomcat/webapps/*

ENV LANG=sk_SK.UTF-8 \
    LANGUAGE=sk_SK:sk \
    LC_ALL=sk_SK.UTF-8

COPY server.xml /usr/local/tomcat/conf/server.xml
COPY docker-entrypoint.sh /usr/local/bin/webjet-tomcat-entrypoint

RUN chmod 755 /usr/local/bin/webjet-tomcat-entrypoint

EXPOSE 80 443

ENTRYPOINT ["/usr/local/bin/webjet-tomcat-entrypoint"]
CMD ["catalina.sh", "run"]
