# nbatch

[中文](README.md)

`nbatch` is a Spring Boot batch scheduling and workflow orchestration platform. The admin center schedules, models, and controls workflows, while executors run SQL, scripts, stored procedures, and file tasks. It is suited to daily batches, data synchronization, file exchange, and other data-processing workloads that need explicit dependencies, traceable logs, and recoverable execution.

## Contents

- [Highlights](#highlights)
- [Quick Start](#quick-start)
- [Operating Guide](#operating-guide)
- [Execution and Retries](#execution-and-retries)
- [Logs and Troubleshooting](#logs-and-troubleshooting)
- [Production Guidance](#production-guidance)

## Highlights

- **Visual workflow design**: Manage groups, nodes, and dependencies on a flow canvas with drag-and-drop, zoom, auto-layout, and dependency maintenance.
- **Multiple node types**: Script, SQL, stored procedure, stored procedure with return value, file-to-database, database-to-file, and Spring Bean nodes.
- **Execution control**: Stop workflows, rerun workflows or nodes, rerun a node with its downstream dependents, and recover failed or stopped workflows.
- **Operational visibility**: Dashboard metrics, date trends, latest failed nodes, and logs filterable by workflow, group, node, and creation time.
- **Bulk maintenance**: Import, export, and template download for workflows. Workbooks separate groups, each node type, and dependencies into sheets with example rows.
- **Data sources and files**: Executor-side support for openGauss, GaussDB, ClickHouse, GBase, and common import/export and compression tasks.

## Architecture

```text
Browser
  |
  v
nbatch-admin (8095/nbatch) <--- registration / dispatch / callback ---> nbatch-consumer (8097)
  |                                                                       |
  v                                                                       v
MySQL metadata                                                     business data sources
                                                                    files / scripts / beans
```

| Module | Responsibility |
| --- | --- |
| `nbatch-core` | Scheduling, executor communication, shared models, and callbacks. |
| `nbatch-handler` | Node handlers and file utilities. |
| `nbatch-admin` | Admin UI, workflow design, scheduling, execution control, and log queries. |
| `nbatch-consumer` | Sample executor that registers with the admin center and runs nodes. |
| `sql` | Bootstrap and upgrade scripts. |

## Prerequisites

- JDK 8
- Maven 3.6+
- MySQL 8.x for admin metadata
- Optional: openGauss, GaussDB, ClickHouse, and GBase as required by nodes

The project uses Spring Boot 2.7.18, MyBatis-Plus, Freemarker, Netty, Hutool, and EasyExcel. GBase and GaussDB drivers may not be available from public Maven repositories. When using the bundled driver files, install them as described in [config/lib/mvn-add.txt](config/lib/mvn-add.txt), or resolve them from an internal repository.

## Quick Start

### 1. Create local configuration

```powershell
Copy-Item .env.example .env
```

On Linux/macOS:

```bash
cp .env.example .env
```

At minimum, verify these settings:

- `NBATCH_ADMIN_DATASOURCE_URL`, `NBATCH_ADMIN_DATASOURCE_USERNAME`, and `NBATCH_ADMIN_DATASOURCE_PASSWORD`
- `NBATCH_ACCESS_TOKEN`: it must match on the admin center and every executor; replace the default in production
- `NBATCH_ADMIN_ADDRESSES`: the address of the running admin center
- `NBATCH_EXECUTOR_IP` and `NBATCH_EXECUTOR_PORT`: an executor address reachable from the admin center

See [.env.example](.env.example) for all settings. Do not commit `.env`, which contains credentials and tokens.

### 2. Initialize the database

Create a MySQL database and import the bootstrap script:

```bash
mysql -u <username> -p <database> < sql/init/mysql.sql
```

The bootstrap script drops and recreates tables, so use it only for a new environment. For an existing installation, back up the database and apply scripts from [sql/update](sql/update) in version order.

### 3. Build and start

```bash
# Admin center
mvn -pl nbatch-admin -am -DskipTests package
java -jar nbatch-admin/target/nbatch-admin-2.5.0.jar

# Sample executor
mvn -pl nbatch-consumer -am -DskipTests package
java -jar nbatch-consumer/target/nbatch-consumer-2.5.0.jar
```

> **Local sign-in**
>
> - Admin URL: `http://localhost:8095/nbatch`
> - User: `admin`
> - Password: `123456`

Change the default administrator password after first sign-in. In production, also replace the access token and use HTTPS.

## Configuration Reference

Applications read the repository-root `.env` at startup through `spring.config.import=optional:file:.env[.properties]`.

| Area | Variables | Purpose |
| --- | --- | --- |
| Admin data source | `NBATCH_ADMIN_DATASOURCE_*` | MySQL metadata connection. |
| API authentication | `NBATCH_ACCESS_TOKEN` | Shared admin/executor token. |
| Executor registration | `NBATCH_ADMIN_ADDRESSES`, `NBATCH_EXECUTOR_*` | Admin address, executor identity, address, port, and directories. |
| File processing | `NBATCH_CONSUMER_FILE_*` | Executor temporary and remote-file directories. |
| Business data sources | `NBATCH_OPEN_GAUSS_*`, `NBATCH_GAUSS_DB_*`, `NBATCH_CLICKHOUSE_*`, `NBATCH_GBASE_*` | Configure only those used by nodes. |
| Mail and retention | `NBATCH_MAIL_*`, `NBATCH_*_LOG_RETENTION_DAYS` | Alert email and log cleanup. |

After the executor starts, confirm in **Executor Management** that it is registered and at least one instance is online. Nodes cannot be dispatched to an offline executor.

## Operating Guide

### First-Use Path

1. Start the admin center and executor, then confirm at least one executor is online in **Executor Management**.
2. Create a workflow in **Job Workflow** and configure its executor and schedule.
3. Open **Workflow Configuration**, add groups and nodes, then create dependencies on the canvas.
4. Start the schedule, or run an eligible rerun, and confirm the result in runtime logs.

> **Start Schedule** allows future triggers only. Use **Stop Current Batch** to request a stop for dispatched or running nodes.

### Page Entry Points

| Page | Daily purpose |
| --- | --- |
| Operations Dashboard | Review running nodes, recent failures, completions, executor count, and latest failed nodes. |
| Executor Management | Confirm that an executor is registered and has an online address. Check here first when a workflow cannot dispatch. |
| Job Workflow | Create workflows, configure schedules, and enter the canvas and execution controls. |

Create a workflow in **Job Workflow**, configure its executor and schedule type with a Cron/interval, then enter its **Workflow Configuration** page from the workflow list.

### Top Toolbar

Workflow configuration groups actions by use case:

| Area | Actions | Description |
| --- | --- | --- |
| Objects | Add Node, Add Group | New objects are added to the current canvas. The default group can be renamed but cannot be deleted. |
| Run | Rerun Workflow, Stop Current Batch, Recover Rerun | Only actions allowed by the current runtime state are shown; turn-card workflows can select a rerun date. |
| Logs | Runtime Logs | Opens the current workflow by default. A selected group or node is used as the initial scope. |
| Schedule | Start Schedule, Stop Schedule | Controls future scheduled triggers only; it does not stop dispatched nodes. |
| Workflow Configuration | Download Template, Import Workflow Configuration, Export Workflow Configuration | Bulk-maintains groups, nodes, and dependencies. |
| Canvas | Refresh and Auto-layout | Reloads workflow data and arranges the canvas by dependency. |

Adding, editing, or deleting an object, and creating or deleting a dependency, is persisted after the operation succeeds. There is no separate workflow-save action.

### Canvas Operations

1. Search the canvas by node or group name/ID. Selecting a result centers the canvas and selects the object.
2. Click `[-]` or `[+]` in a group header to collapse or expand it. Dragging a group moves its contained nodes together; dragging a node changes only that node position, and the group expands as needed.
3. Drag from the connection point on the right of a node or group to a target node or group to create a `predecessor -> successor` dependency. The successor can run only after its predecessor completes.
4. Select a node, group, or dependency line to show the nearby action buttons. Nodes provide logs, rerun, edit, and delete; groups provide logs, edit, and delete; a dependency line can only be deleted.
5. Use `-` and `+` to zoom, or the fit-canvas button to restore a useful view. For large workflows, use search and fit-to-canvas to navigate.

All deletion actions require confirmation. Deleting a group also removes its contained nodes, related dependencies, and node file configuration. Deleting a node removes its dependencies and file configuration. Groups can participate in dependencies.

![Workflow editor](docs/images/operation-flow-editor.png)

### Import and Export

The workflow import/export entry provides template download, definition export, and workbook import. Workbook sheets are ordered as follows:

1. `Groups`
2. One sheet for each node type
3. `Dependencies`

Each node-type sheet includes only its required fields, and templates include example rows. Before import, verify node IDs, group assignments, data-source names, executable content, and dependency references.

## Execution and Retries

Every node dispatch creates an independent execution log. Turn-card workflows advance by business date, while sequential workflows keep the current workflow execution state. When a node has multiple attempts, use `turnDate` with creation, start, and end times to identify the relevant attempt.

| Action | Purpose |
| --- | --- |
| Stop Current Batch | Sends stop requests to executors and waits for active nodes to close. Stop Schedule does not stop the current batch. |
| Rerun Workflow | Runs from the top **Run** menu, resets current workflow execution state, and executes the current definition. A turn-card workflow can select a rerun date. |
| Rerun Node | Runs from the selected-node action buttons after checking that downstream nodes are not active. |
| Rerun Dependents | Select this option in the node-rerun dialog to reset the selected node and downstream dependents, preventing stale downstream results. |
| Recover Rerun | Runs from the top **Run** menu and continues failed or stopped nodes without rerunning completed or skipped nodes. |

Retry settings are interpreted as follows:

- `-1`: retry indefinitely
- `0`: do not retry
- Greater than `0`: maximum number of retries

Ensure that nodes that write data, send files, or invoke external services are idempotent before rerunning them. Forced recovery bypasses confirmation that old tasks have exited; use it only after assessing the risk of concurrent execution.

### Common Actions

| Goal | Recommended action |
| --- | --- |
| Pause future scheduled triggers | Top **Schedule** -> **Stop Schedule**. |
| Stop an active batch | Top **Run** -> **Stop Current Batch**. |
| Run one node again | Select the node -> nearby action bar -> rerun. |
| Run a node and downstream nodes again | Select the node -> rerun -> select **Rerun Dependents**. |
| Continue failed or stopped nodes | Top **Run** -> **Recover Rerun**. |

## Logs and Troubleshooting

### Query Logs

Open **Runtime Logs** from the top toolbar. When a group or node is selected before opening it, that object becomes the initial scope. In the log dialog, search and select a scope from the **Log Scope** input:

- Select the current workflow to query all node logs for that workflow.
- Select a group to query logs for nodes in that group.
- Select a node to query only that node.
- Clear **Log Scope** to return to all logs for the current workflow.

Creation time can be combined with the scope filter. Both execution and detail logs show node name, node ID, turn date, status, execution message, and actual execution times. Hover long messages to view their full content.

### Investigation Order

1. Check the Operations Dashboard for a latest failed node or running node.
2. In Executor Management, confirm that the target executor is online and reachable from the admin center.
3. Open the node runtime log and confirm the latest status, `turnDate`, creation time, and execution message.
4. Then check target data-source connectivity, file-directory permissions, disk capacity, and network access.

![Operations dashboard](docs/images/operation-dashboard.png)

## Development and Verification

```bash
# Build all modules
mvn clean package -DskipTests

# Run admin tests; the root POM skips tests by default, so override it explicitly
mvn -pl nbatch-admin -am -Dmaven.test.skip=false test
```

## Production Guidance

- Never use the default user, default access token, or example data-source passwords in production.
- Use HTTPS and restrict network and operational access to the admin center, executors, databases, and file directories.
- Back up MySQL metadata regularly. Validate upgrade scripts and rehearse upgrades in a test environment first.
- Restrict file-task directories, capacity, and accepted sources. Do not pass untrusted paths directly to executors.
- Use least-privilege accounts for SQL, scripts, and stored procedures, and implement business-level idempotency before enabling reruns.

## License

This project is released under the repository [LICENSE](LICENSE).
