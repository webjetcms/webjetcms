# WebJET CMS Database Containers
<!-- spellcheck-off -->

This directory contains Docker configurations for running test databases for WebJET CMS development. All ports are prefixed by number 1, so it will normally not conflict with your local instances of DB servers.

In VS Code you can directly run `Debug Docker [DBTYPE]` to start Docker DB instance and debug WebJET CMS. So you don't need to start DB instance manually. For first run follow info in [Database Connection Details](#database-connection-details) section.

After first start go to [WebJET CMS Setup page](http://localhost/wjerrorpages/setup/setup) to setup WebJET. There should be information populated from `poolman-docker-DBNAME.xml` config file. More info is on [official documentation](https://docs.webjetcms.sk/latest/en/install/setup/README).

## Usage

### Environment Variable

Set the database password environment variable:

```bash
export WEBJET_DB_PASS="your_password_here"
```

## Notes

- MariaDB uses `utf8mb4` charset
- PostgreSQL creates tables in the `webjet_cms` schema
- Oracle may take longer to start (large image ~4GB) and initialization scripts may need manual execution
- Persistent volumes are created for each database to maintain data between container restarts

## Available Databases

### MariaDB

- **Files**: `docker-compose-mariadb.yml`, `Dockerfile-mariadb`
- **Port**: 13306/3306
- **VS Code Tasks**: "Docker DB MariaDB Start", "Docker DB MariaDB Stop"

### Microsoft SQL Server

- **Files**: `docker-compose-mssql.yml`, `Dockerfile-mssql`
- **Port**: 11433/1433
- **VS Code Tasks**: "Docker DB MSSQL Start", "Docker DB MSSQL Stop"

### Oracle Database Express Edition

- **Files**: `docker-compose-oracle.yml`, `Dockerfile-oracle`
- **Ports**: 11521 (database), 15500 (web console)
- **VS Code Tasks**: "Docker DB Oracle Start", "Docker DB Oracle Stop"

### PostgreSQL

- **Files**: `docker-compose-pgsql.yml`, `Dockerfile-pgsql`
- **Port**: 15432
- **VS Code Tasks**: "Docker DB PgSQL Start", "Docker DB PgSQL Stop"

### Starting Databases

#### Using VS Code Tasks

1. Open Command Palette (Ctrl+Shift+P / Cmd+Shift+P)
2. Run "Tasks: Run Task"
3. Select the appropriate database start task

#### Using Command Line

```bash
cd .devcontainer/db/
export WEBJET_DB_PASS="your_password"

# MariaDB
docker compose -f docker-compose-mariadb.yml up -d

# PostgreSQL
docker compose -f docker-compose-pgsql.yml up -d

# Oracle
docker compose -f docker-compose-oracle.yml up -d

# Microsoft SQL Server
docker compose -f docker-compose-mssql.yml up -d
```

### Stopping Databases

Use the corresponding VS Code stop tasks or:

```bash
docker compose -f docker-compose-[database].yml down
```

## Database Connection Details

### MariaDB

- **Host**: host.docker.internal
- **Port**: 13306
- **Database**: webjetcms_autotest_web
- **Username**: root
- **Password**: ${WEBJET_DB_PASS}

### Microsoft SQL Server

- **Host**: host.docker.internal
- **Port**: 11433
- **Username**: sa
- **Password**: ${WEBJET_DB_PASS}

#### Microsoft SQL Initialization Notes

When you setup WebJET you need to check `Use different user for "CREATE DATABASE" command` and set username `sa` and password `${WEBJET_DB_PASS}`, setup will create new database using `sa` credentials. You can directly set `${WEBJET_DB_PASS}`, it will be replaced with real ENV variable value.

### Oracle

- **Host**: host.docker.internal
- **Port**: 11521
- **Service**: XEPDB1
- **Username**: webjetcms_autotest_web
- **Password**: ${WEBJET_DB_PASS}

#### Oracle Initialization Notes

Oracle containers work differently than MySQL/PostgreSQL for script initialization, so you need to execute following commands manually.

IMPORTANT: replace `${WEBJET_DB_PASS}` with real password, because your ENV variables are not populated inside container.

```bash
# After container is running
docker exec -it webjetcms-oracle bash
sqlplus system/${WEBJET_DB_PASS}@localhost/XEPDB1
```

Then create user and schema (paste commands line by line, do not forget to change `${WEBJET_DB_PASS}` to real password):

```sql
CREATE USER webjetcms_autotest_web IDENTIFIED BY "${WEBJET_DB_PASS}";
GRANT CONNECT, RESOURCE TO webjetcms_autotest_web;
ALTER USER webjetcms_autotest_web DEFAULT TABLESPACE users;
ALTER USER webjetcms_autotest_web QUOTA UNLIMITED ON users;
EXIT
```

### PostgreSQL

- **Host**: host.docker.internal
- **Port**: 15432
- **Database**: webjetcms_autotest_web
- **Username**: webjetcms_autotest_web
- **Password**: ${WEBJET_DB_PASS}
- **Schema**: webjet_cms