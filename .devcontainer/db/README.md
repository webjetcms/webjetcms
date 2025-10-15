# WebJET CMS Database Containers

This directory contains Docker configurations for running test databases for WebJET CMS development.

## Available Databases

### Microsoft SQL Server
- **Files**: `docker-compose-mssql.yml`, `Dockerfile-mssql`
- **Port**: 1433
- **VS Code Tasks**: "Docker DB MSSQL Start", "Docker DB MSSQL Stop"

### MariaDB
- **Files**: `docker-compose-mariadb.yml`, `Dockerfile-mariadb`
- **Port**: 3306
- **VS Code Tasks**: "Docker DB MariaDB Start", "Docker DB MariaDB Stop"

### PostgreSQL
- **Files**: `docker-compose-pgsql.yml`, `Dockerfile-pgsql`
- **Port**: 5432
- **VS Code Tasks**: "Docker DB PostgreSQL Start", "Docker DB PostgreSQL Stop"

### Oracle Database Express Edition
- **Files**: `docker-compose-oracle.yml`, `Dockerfile-oracle`
- **Ports**: 1521 (database), 5500 (web console)
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

- All databases are initialized with the WebJET CMS schema from the corresponding SQL files in `src/main/webapp/WEB-INF/sql/`
- MariaDB uses utf8 charset to avoid key length issues with the legacy schema
- PostgreSQL creates tables in the `webjet_cms` schema
- Oracle may take longer to start (large image ~4GB) and initialization scripts may need manual execution
- Persistent volumes are created for each database to maintain data between container restarts

### Oracle Initialization Notes
Oracle containers work differently than MySQL/PostgreSQL for script initialization:
- The init script is mounted to `/opt/oracle/scripts/setup/` but may not auto-execute
- To manually run the WebJET schema setup:
  ```bash
  # After container is running
  docker exec oracle sqlplus system/${WEBJET_DB_PASS}@XEPDB1 @/opt/oracle/scripts/setup/webjet_init.sql
  ```
- The `webjetcms` user may need to be created manually if not auto-created
- Oracle requires significant system resources (minimum 2GB RAM recommended)

## Troubleshooting

1. **Permission denied**: Ensure Docker is running and you have permissions
2. **Port conflicts**: Stop any existing database services on the same ports
3. **Oracle startup issues**: Oracle container requires significant resources and may take 2-3 minutes to fully start
4. **Oracle initialization**: If WebJET schema is not automatically created, run manual initialization:
   ```bash
   docker exec oracle sqlplus system/${WEBJET_DB_PASS}@XEPDB1 @/opt/oracle/scripts/setup/webjet_init.sql
   ```
5. **MariaDB character encoding**: MariaDB is configured with utf8 charset for compatibility with legacy schema
6. **PostgreSQL schema**: Tables are created in `webjet_cms` schema, ensure your connection uses this schema
7. **Large Oracle image**: First Oracle startup requires downloading ~4GB image, subsequent starts are faster