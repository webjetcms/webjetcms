# WebJET CMS Database Containers
<!-- spellcheck-off -->

This directory contains Docker configurations for running test databases for WebJET CMS development.

## Available Databases

### Microsoft SQL Server

- **Files**: `docker-compose-mssql.yml`, `Dockerfile-mssql`
- **Port**: 1433
- **VS Code Tasks**: "Docker DB MSSQL Start", "Docker DB MSSQL Stop"

### MariaDB

- **Files**: `docker-compose-mariadb.yml`, `Dockerfile-mariadb`
- **Port**: 13306/3306
- **VS Code Tasks**: "Docker DB MariaDB Start", "Docker DB MariaDB Stop"

### PostgreSQL

- **Files**: `docker-compose-pgsql.yml`, `Dockerfile-pgsql`
- **Port**: 15432
- **VS Code Tasks**: "Docker DB PostgreSQL Start", "Docker DB PostgreSQL Stop"

### Oracle Database Express Edition

- **Files**: `docker-compose-oracle.yml`, `Dockerfile-oracle`
- **Ports**: 11521 (database), 15500 (web console)
- **VS Code Tasks**: "Docker DB Oracle Start", "Docker DB Oracle Stop"

## Usage

### Environment Variable

Set the database password environment variable:

```bash
export WEBJET_DB_PASS="your_password_here"
```

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

- **Host**: localhost
- **Port**: 3306
- **Database**: webjetcms
- **Username**: webjetcms
- **Password**: ${WEBJET_DB_PASS}

### PostgreSQL

- **Host**: localhost
- **Port**: 5432
- **Database**: webjetcms
- **Username**: webjetcms
- **Password**: ${WEBJET_DB_PASS}
- **Schema**: webjet_cms

### Oracle

- **Host**: localhost
- **Port**: 1521
- **Service**: XEPDB1
- **Username**: webjetcms
- **Password**: ${WEBJET_DB_PASS}

### Microsoft SQL Server

- **Host**: localhost
- **Port**: 1433
- **Username**: sa
- **Password**: ${WEBJET_DB_PASS}

## Notes

- MariaDB uses `utf8` charset to avoid key length issues with the legacy schema
- PostgreSQL creates tables in the `webjet_cms` schema
- Oracle may take longer to start (large image ~4GB) and initialization scripts may need manual execution
- Persistent volumes are created for each database to maintain data between container restarts

### Oracle Initialization Notes

Oracle containers work differently than MySQL/PostgreSQL for script initialization, so you need to execute following commands manually.

IMPORTANT: replace `${WEBJET_DB_PASS}` with real password, because your ENV variables are not populated inside container.

```bash
# After container is running
docker exec -it webjetcms-oracle bash
sqlplus system/${WEBJET_DB_PASS}@localhost/XEPDB1
```

Then create user and schema:

```sql
CREATE USER webjetcms_autotest_web IDENTIFIED BY "${WEBJET_DB_PASS}";
GRANT CONNECT, RESOURCE TO webjetcms_autotest_web;
ALTER USER webjetcms_autotest_web DEFAULT TABLESPACE users;
ALTER USER webjetcms_autotest_web QUOTA UNLIMITED ON users;
```