# nbatch

nbatch is a Spring Boot based batch job scheduling and workflow orchestration platform. It extends the XXL-JOB scheduling model with workflow, job node, node dependency, node runtime status, and node-level log lifecycle capabilities. It is suitable for multi-node batch processing, cross-database data processing, script execution, and file import/export tasks.

## Features

- Admin center: job management, executor management, scheduling logs, GLUE code editing, and user management.
- Workflow orchestration: batch workflow management, node management, dependency configuration, and runtime batch status tracking.
- Node types: script, stored procedure, stored procedure returning string, SQL execution, file-to-database import, database-to-file export, and Bean mode.
- Node statuses: waiting, dispatched, running, completed, failed, skipped, and stopped.
- Rerun support: rerun latest batch, recover failed/stopped nodes, rerun a specific node and affected downstream nodes.
- Stop support: stop the latest workflow run and send stop commands to executors for running nodes.
- Execution lifecycle: executor registration, task dispatching, node log callback, node event pull and ACK, including `STARTED`, `SUCCESS`, `FAIL`, and `STOPPED` events.
- Data source support: MySQL, OpenGauss/Gauss, GBase, ClickHouse, and related JDBC adapters on the executor side.

## Modules

```text
nbatch
├── nbatch-core      # Scheduling core, executor communication, enums, callback models, thread models
├── nbatch-handler   # Node handlers for SQL, scripts, stored procedures, file import/export, and stop handling
├── nbatch-admin     # Web admin center, pages, APIs, scheduling threads, runtime job management
├── nbatch-consumer  # Sample executor application that registers to admin and executes concrete jobs
└── sql/init         # MySQL initialization script
```

## Tech Stack

- JDK 8
- Spring Boot 2.7.18
- Spring Framework 5.3.31
- MyBatis-Plus 3.5.15
- Freemarker
- Netty
- Hutool
- MySQL 8.x
- OpenGauss / GBase / ClickHouse JDBC drivers

## Prerequisites

1. Install JDK 8.
2. Install Maven 3.6+.
3. Prepare a MySQL database.
4. Copy the environment variable sample file and update `.env` for your local environment:

```bash
cp .env.example .env
```

5. Import the initialization script:

```bash
mysql -u <username> -p <database> < sql/init/mysql.sql
```

6. If you need to compile `nbatch-handler` or `nbatch-consumer`, make sure your local Maven repository can resolve `com.gbase:gbase-jdbc:9.5.0.7`. This driver is usually not available in Maven Central, so you may need to install it locally or publish it to a private repository.

```bash
mvn install:install-file \
  -Dfile=/path/to/gbase-jdbc-9.5.0.7.jar \
  -DgroupId=com.gbase \
  -DartifactId=gbase-jdbc \
  -Dversion=9.5.0.7 \
  -Dpackaging=jar
```

## Configuration

### Admin Center

Configuration file: `nbatch-admin/src/main/resources/application.yml`

Sensitive settings are loaded from the root `.env` file. Key variables:

```properties
NBATCH_ADMIN_DATASOURCE_URL=jdbc:mysql://localhost:3306/batch_dev?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8
NBATCH_ADMIN_DATASOURCE_USERNAME=<mysql-username>
NBATCH_ADMIN_DATASOURCE_PASSWORD=<mysql-password>
NBATCH_ACCESS_TOKEN=default_token
```

After startup, open: `http://localhost:8095/nbatch`

### Login Password Encryption

- The admin center generates a 2048-bit RSA key pair in memory on startup.
- The frontend obtains the public key from `/rsaPublicKey` and encrypts password fields with `RSA-OAEP/SHA-256`.
- The backend receives Base64 ciphertext, decrypts it with the private key, and then continues using the existing MD5 storage and verification logic.
- Frontend encryption depends on the browser WebCrypto API. HTTPS is recommended for production. Local `localhost` development usually works directly.

### Executor

Configuration file: `nbatch-consumer/src/main/resources/application.yml`

Sensitive settings are loaded from the root `.env` file. Key variables:

```properties
NBATCH_ADMIN_ADDRESSES=http://localhost:8095/nbatch
NBATCH_ACCESS_TOKEN=default_token
NBATCH_EXECUTOR_APP_NAME=nbatch-consumer
NBATCH_EXECUTOR_PORT=9999
NBATCH_GAUSS_DATASOURCE_URL=jdbc:opengauss://localhost:5432/postgres?currentSchema=public&serverTimezone=Asia/Shanghai
NBATCH_GAUSS_DATASOURCE_USERNAME=<gauss-username>
NBATCH_GAUSS_DATASOURCE_PASSWORD=<gauss-password>
```

`admin.addresses` and `accessToken` must match the admin center configuration.

## Build and Run

### Compile Admin Center Only

```bash
mvn -pl nbatch-admin -am -DskipTests compile
```

### Package Admin Center

```bash
mvn -pl nbatch-admin -am -DskipTests package
```

Run:

```bash
java -jar nbatch-admin/target/nbatch-admin-2.5.0.jar
```

### Compile the Full Project

```bash
mvn -DskipTests compile
```

If the full build fails because `com.gbase:gbase-jdbc:9.5.0.7` is missing, install or configure the corresponding JDBC driver first.

### Start Executor

```bash
mvn -pl nbatch-consumer -am -DskipTests package
java -jar nbatch-consumer/target/nbatch-consumer-2.5.0.jar
```

## Basic Usage

1. Start `nbatch-admin`.
2. Start `nbatch-consumer` and wait for automatic executor registration.
3. Confirm that `nbatch-consumer` is online in executor management.
4. Create nodes in job node management and configure node type, execution content, database type, retry policy, and other settings.
5. Create a workflow in batch workflow management and configure node dependencies.
6. Create a scheduled task in task management and bind it to the workflow.
7. Start the task and inspect scheduling logs, runtime job status, and runtime node logs.

## Workflow and Node Concepts

- Workflow: a group of orchestrated job nodes.
- Job node: the actual execution unit, such as SQL, script, stored procedure, file import, or file export.
- Node dependency: `current node -> dependency node`; a node can run only after its dependency node is completed.
- Runtime job: one workflow run batch.
- Runtime node: a node instance in a specific runtime job.

## Rerun Semantics

- Full rerun: reset all nodes in the latest runtime batch and execute the whole batch again.
- Recovery rerun: reset only failed, stopped, dispatched, or running nodes; completed and skipped nodes keep their original statuses.
- Node rerun: reset a specific runtime node. If other nodes depend on it, all affected downstream nodes are reset together.
- Initialization rerun: locate the runtime batch by the workflow initialization turn date, reset that batch, and clean later runtime records. If the initialization turn date or corresponding runtime batch does not exist, it is automatically initialized.

## Stop and Failure Strategies

- Stop workflow: stop the latest runtime job from the batch workflow list. Only waiting, dispatched, or running runtime jobs can be stopped.
- Stop scope: the admin center marks the runtime job as stopped and marks waiting, dispatched, and running runtime nodes as stopped.
- Executor stop: the admin center sends a `stopRunNode` command by executor address. The executor stops queued or running nodes by `nodeLogId` and reports a `STOPPED` event.
- Skip strategy: when a node uses the `skip` failure strategy, a failed node is marked as skipped and downstream dependencies can continue.
- Stop strategy: when a node uses `stop`, or when retries are exhausted, the node and runtime job enter the stopped status.
- Retry strategy: when a node uses `retry`, the remaining retry count is decreased and the node returns to waiting. After retries are exhausted, stop handling is applied.

## Change Report: 2026-06-11

### Backend

- Added workflow initialization turn date field `init_turn_date`. The first run prioritizes this date and defaults to the current day when it is not configured.
- Added rerun support based on initialization turn date for both workflow and node entries, while keeping existing full rerun, recovery rerun, and node rerun semantics unchanged.
- Added workflow detail and job node detail APIs returning basic information, run count, and runtime history for frontend detail dialogs.
- Changed job node deletion to cascade cleanup of related node data, including dependencies, runtime nodes, execution logs, detail logs, import configurations, and export configurations.
- Added creation-time range query support for execution logs and detail logs. Added `create_time` to detail logs and populated it during callback writes.
- Changed the node status lifecycle to executor local event-log pull mode. After successful dispatch, the server first marks nodes as `dispatched`; after actual executor start, a `STARTED` event updates them to `running`.
- Removed old node status callback branches and kept normal task callback plus runtime node detail log callback.
- Refactored `RunNodeHelper` to centralize runtime job status synchronization, runtime node log construction, and timeout constants.

### Frontend

- Simplified the batch workflow list to key fields, added workflow-name search, and opened detail dialogs by clicking workflow names.
- Simplified the job node list to key fields, added node-name search, and opened detail dialogs by clicking node names.
- Added latest runtime status filtering on the job node list, supporting waiting, dispatched, running, completed, and failed.
- Detail dialogs show basic information at the top and runtime history plus run-count statistics below.
- Added creation-time range pickers to execution logs and detail logs. Long text logs are truncated by default and can be viewed fully from details.
- Replaced dependency dropdowns with a node selection dialog, supporting search by node name or ID, single current node selection, multiple dependency selection, and selected-item echo.
- Added node-name search on the dependency relation list for large workflows with hundreds of nodes.

### Database

- `sql/init/mysql.sql` has been updated with new fields.
- Added upgrade script: `sql/upgrade/20260611_add_init_turn_date.sql`.
- Added upgrade script: `sql/upgrade/20260611_add_log_detail_create_time.sql`.

### Verification

- Verified with: `mvn -pl nbatch-admin -am -DskipTests compile`.
- Full compilation still requires local Maven resolution for `com.gbase:gbase-jdbc:9.5.0.7`.

## Change Report: 2026-06-13

### Backend

- Added runtime statuses `skipped` and `stopped`; runtime job details now include skipped and stopped counts.
- Added API for stopping the latest runtime job: `/work/stopLatestRunWork`. Only waiting, dispatched, and running latest runtime jobs can be stopped.
- Added executor stop command chain: `StopRunNodeParam`, `ExecutorBiz.stopRunNode`, `/stopRunNode` RPC entry, and `RunNodeStopHandler`.
- Executor thread pools can stop queued or running `BatchRunnable` instances by `nodeLogId` and write `STOPPED` local events.
- Runtime node event pulling now handles `STOPPED`; the admin center updates runtime node logs, node end time, and runtime job status after receiving it.
- Unified failure handling for `stop`, `skip`, and `retry`: skipped nodes satisfy downstream dependencies, while stopped nodes prevent the runtime job from continuing.
- Recovery rerun now supports stopped status and resets only failed, stopped, dispatched, or running nodes.

### Frontend

- Added `Stop Workflow` to the batch workflow action menu for the latest runtime job.
- Added `Skipped` and `Stopped` statistics columns to workflow runtime history.
- Adapted workflow and node status labels for skipped and stopped statuses.
- Updated recovery rerun confirmation text to cover failed, stopped, or stuck nodes; completed and skipped nodes are not rerun.

### Verification

- This README update was prepared based on code changes in commit `406648e`.
- Recommended verification before commit: `mvn -pl nbatch-admin -am -DskipTests compile`.

## Change Report: 2026-06-27

### Frontend

- Added workflow dependency preview on the dependency configuration page. It generates a node dependency flowchart from the current page state, including unsaved changes.
- The flowchart uses the direction `dependency node → current node`, and supports mouse-wheel zoom, canvas dragging, and reset.
- Node names are displayed in up to three lines to reduce truncation. Font size follows zoom changes while keeping a minimum readable size.
- Right-clicking a node shows both upstream dependencies and downstream dependent nodes, and related nodes can be clicked for quick positioning.
- Canvas dragging is bounded so the flowchart cannot be moved too far away from the visible area.
- Fixed preview button initialization timing and context-menu JavaScript string composition issues to avoid frontend runtime errors such as `openFlowPreview is not defined`.

### Executor and File Handling

- Refactored `JobHandlerHolder`, `BatchRunnable`, and thread-pool utilities to reduce coupling between node execution, latch release, and exception handling.
- Optimized `NbatchFileUtil` compression and decompression logic, including directory compression support and less duplicated validation code.
- Optimized CSV first-line reading and column-count detection in `NbatchCsvUtil`, `GBaseDialect`, and `MysqlDialect`, with compatibility for BOM, invisible characters, and blank lines.
- Optimized the file import flow to reduce duplicated processing between first-line detection and actual import.
- Adjusted `SpecialSqlUtil` special SQL handling and removed unnecessary execution-time output.

### Verification

- Verified FreeMarker template parsing and rendered JavaScript syntax.
- Verified with: `mvn -pl nbatch-admin -am -DskipTests compile`.
- Full compilation still requires local Maven resolution for `com.gbase:gbase-jdbc:9.5.0.7`.

## Common Development Commands

```bash
# Compile admin and dependencies
mvn -pl nbatch-admin -am -DskipTests compile

# Package admin
mvn -pl nbatch-admin -am -DskipTests package

# Compile core
mvn -pl nbatch-core -DskipTests compile

# Full compile
mvn -DskipTests compile
```

## Directory Notes

```text
nbatch-admin/src/main/java/com/nbatch/job/admin
├── controller      # Web controllers
├── service         # Business services
├── core/helper     # Scheduling, runtime job, and runtime node helper logic
├── core/thread     # Scheduling, registry, and log callback threads
├── mapper          # MyBatis-Plus mappers
└── core/domain     # PO, VO, Param, and other data models

nbatch-admin/src/main/resources
├── templates       # Freemarker pages
├── static          # Frontend static assets
├── sqlmap          # MyBatis XML mappings
└── application.yml # Admin configuration
```

## Notes

- Do not commit production database accounts, passwords, or internal IPs. The root `.env` file is ignored by `.gitignore`; keep only `.env.example` in commits.
- `nbatch-admin` can be compiled and run independently. `nbatch-handler` and `nbatch-consumer` depend on several database drivers; resolve Maven dependencies first when drivers are missing.
- The executor port `nbatch.job.executor.port` must be accessible from the admin center.
- The admin center and executor must use the same `accessToken`.
- The initialization SQL may contain demo data. Clean or replace it as needed for production environments.
