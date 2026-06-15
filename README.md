# nbatch

nbatch 是一个基于 Spring Boot 的批量作业调度与流程编排平台。项目在 XXL-JOB 调度模型基础上扩展了“作业流程 / 作业节点 / 节点依赖 / 节点运行状态 / 节点级日志闭环”等能力，适合用来管理多节点批处理、跨库数据处理、脚本执行和文件导入导出任务。

## 功能特性

- 调度中心：任务管理、执行器管理、调度日志、GLUE 代码编辑、用户管理。
- 作业流程：支持批次流程管理、节点管理、节点依赖配置、运行批次状态跟踪。
- 节点类型：支持脚本、存储过程、返回字符串存储过程、SQL 执行、文件入库、数据库导出文件、Bean 模式。
- 节点状态：支持待执行、已下发、进行中、执行完毕、执行异常、已跳过、已停止。
- 重跑能力：支持最新运行批次一键重跑、异常/停止节点恢复重跑、指定节点及其后继节点重跑。
- 停止能力：支持停止流程最新一次运行作业，并向执行器下发运行节点停止指令。
- 执行闭环：执行器注册、任务下发、运行节点日志回传、运行节点事件拉取与 ACK，支持 STARTED、SUCCESS、FAIL、STOPPED 事件。
- 数据源能力：执行端支持 MySQL、OpenGauss/Gauss、GBase、ClickHouse 等数据源适配。

## 模块结构

```text
nbatch
├── nbatch-core      # 调度核心、执行器通信、枚举、回调模型、线程模型
├── nbatch-handler   # 节点处理器，包含 SQL、脚本、存储过程、文件导入导出等执行逻辑
├── nbatch-admin     # 调度中心 Web 管理端，包含页面、接口、调度线程、运行作业管理
├── nbatch-consumer  # 示例执行器应用，注册到 admin 并执行具体任务
└── sql/init         # MySQL 初始化脚本
```

## 技术栈

- JDK 8
- Spring Boot 2.7.18
- Spring Framework 5.3.31
- MyBatis-Plus 3.5.15
- Freemarker
- Netty
- Hutool
- MySQL 8.x
- OpenGauss / GBase / ClickHouse JDBC 驱动

## 环境准备

1. 安装 JDK 8。
2. 安装 Maven 3.6+。
3. 准备 MySQL 数据库。
4. 复制环境变量示例文件，并按本地环境修改 `.env`：

```bash
cp .env.example .env
```

5. 导入初始化脚本：

```bash
mysql -u <username> -p <database> < sql/init/mysql.sql
```

6. 如需编译 `nbatch-handler` 或 `nbatch-consumer`，确保本地 Maven 仓库可解析 `com.gbase:gbase-jdbc:9.5.0.7`。该驱动通常不是中央仓库依赖，可能需要手工安装到本地仓库或私服。

```bash
mvn install:install-file \
  -Dfile=/path/to/gbase-jdbc-9.5.0.7.jar \
  -DgroupId=com.gbase \
  -DartifactId=gbase-jdbc \
  -Dversion=9.5.0.7 \
  -Dpackaging=jar
```

## 配置说明

### 调度中心

配置文件：`nbatch-admin/src/main/resources/application.yml`

敏感配置从根目录 `.env` 读取，关键变量：

```properties
NBATCH_ADMIN_DATASOURCE_URL=jdbc:mysql://localhost:3306/batch_dev?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8
NBATCH_ADMIN_DATASOURCE_USERNAME=<mysql-username>
NBATCH_ADMIN_DATASOURCE_PASSWORD=<mysql-password>
NBATCH_ACCESS_TOKEN=default_token
```

启动后访问：`http://localhost:8095/nbatch`

### 执行器

配置文件：`nbatch-consumer/src/main/resources/application.yml`

敏感配置从根目录 `.env` 读取，关键变量：

```properties
NBATCH_ADMIN_ADDRESSES=http://localhost:8095/nbatch
NBATCH_ACCESS_TOKEN=default_token
NBATCH_EXECUTOR_APP_NAME=nbatch-consumer
NBATCH_EXECUTOR_PORT=9999
NBATCH_GAUSS_DATASOURCE_URL=jdbc:opengauss://localhost:5432/postgres?currentSchema=public&serverTimezone=Asia/Shanghai
NBATCH_GAUSS_DATASOURCE_USERNAME=<gauss-username>
NBATCH_GAUSS_DATASOURCE_PASSWORD=<gauss-password>
```

`admin.addresses` 和 `accessToken` 必须与调度中心配置保持一致。

## 构建与启动

### 只编译调度中心

```bash
mvn -pl nbatch-admin -am -DskipTests compile
```

### 打包调度中心

```bash
mvn -pl nbatch-admin -am -DskipTests package
```

运行：

```bash
java -jar nbatch-admin/target/nbatch-admin-2.5.0.jar
```

### 编译全量项目

```bash
mvn -DskipTests compile
```

如果全量编译失败并提示 `com.gbase:gbase-jdbc:9.5.0.7` 缺失，请先安装或配置对应 JDBC 驱动。

### 启动执行器

```bash
mvn -pl nbatch-consumer -am -DskipTests package
java -jar nbatch-consumer/target/nbatch-consumer-2.5.0.jar
```

## 使用流程

1. 启动 `nbatch-admin`。
2. 启动 `nbatch-consumer`，等待执行器自动注册。
3. 在执行器管理中确认 `nbatch-consumer` 在线。
4. 在作业节点管理中创建节点，配置节点类型、执行内容、数据库类型、重试策略等。
5. 在批次流程管理中创建作业流程，并配置节点依赖关系。
6. 在任务管理中创建调度任务并绑定作业流程。
7. 启动任务，查看调度日志、运行作业状态和运行节点日志。

## 作业与节点概念

- 作业流程：一组可编排的作业节点。
- 作业节点：实际执行单元，例如 SQL、脚本、存储过程、文件导入导出。
- 节点依赖：`当前节点 -> 依赖节点`，当前节点只有在依赖节点完成后才可执行。
- 运行作业：一次作业流程运行批次。
- 运行节点：某次运行作业中的节点实例。

## 重跑语义

- 一键重跑：重置最新运行批次的所有节点，整批重新执行。
- 恢复重跑：只重置异常、停止、已下发或运行中的节点，已完成和已跳过节点保持原状态。
- 节点重跑：重置指定运行节点；如果该节点被其他节点依赖，则所有后继受影响节点也会一起重跑。
- 初始化重跑：按作业配置的初始化翻牌日期定位运行批次，重置该批次并清理之后的运行记录；如果初始化翻牌日期或对应运行批次不存在，会自动初始化。

## 停止与失败策略

- 停止作业：在批次流程列表中对最新一次运行作业执行“停止作业”，仅允许待执行、已下发或进行中的运行作业停止。
- 停止范围：调度中心会将运行作业标记为已停止，并把待执行、已下发、进行中的运行节点标记为已停止。
- 执行器停止：调度中心按执行器地址下发 `stopRunNode` 指令，执行端根据 `nodeLogId` 停止队列中或运行中的节点，并上报 `STOPPED` 事件。
- 跳过策略：节点失败策略为 `skip` 时，失败节点标记为已跳过，后继依赖可以继续满足。
- 停止策略：节点失败策略为 `stop`，或重试次数耗尽且无法继续重试时，节点和运行作业会进入已停止状态。
- 重试策略：节点失败策略为 `retry` 时优先扣减剩余重试次数并回到待执行；重试耗尽后进入停止处理。

## 2026-06-11 修改报告

### 后端能力

- 新增作业初始化翻牌日期字段 `init_turn_date`，首次运行优先使用该日期，没有配置时默认使用当天。
- 新增按初始化翻牌日期重跑能力，覆盖批次流程和作业节点两个入口，并保持原有一键重跑、恢复重跑、节点重跑语义不变。
- 新增批次流程详情接口和作业节点详情接口，返回基础信息、运行次数和运行历史，支撑前端详情弹窗。
- 作业节点删除改为级联清理节点相关数据，包括依赖关系、运行节点、执行日志、详细日志、导入配置和导出配置。
- 执行日志和详细日志支持按创建时间范围查询，详细日志新增 `create_time` 字段并在回调写入时填充。
- 节点状态闭环改为执行器本地事件日志拉取模式，服务端下发成功后先标记为 `已下发`，执行端真正开始执行后再通过 `STARTED` 事件更新为 `进行中`。
- 清理旧节点状态回调分支，仅保留普通任务回调和运行节点详细日志回调。
- `RunNodeHelper` 完成结构整理，集中运行作业状态同步、运行节点日志构建和超时常量配置。

### 前端交互

- 批次流程列表精简为关键字段展示，支持按作业名称查询，点击作业名称打开详情弹窗。
- 作业节点列表精简为关键字段展示，支持按节点名称查询，点击节点名称打开详情弹窗。
- 作业节点列表新增按最新运行状态筛选，支持待执行、已下发、进行中、执行完毕、执行异常。
- 详情弹窗上方展示基础信息，下方展示运行历史列表和运行次数统计。
- 执行日志和详细日志增加创建时间范围选择器，长文本日志默认截断，点击“详情”查看完整内容。
- 节点依赖配置页由普通下拉升级为节点选择弹窗，支持按节点名称或 ID 搜索、当前节点单选、依赖节点多选和已选回显。
- 依赖关系列表支持按节点名称查询，适配几百个节点的大流程配置场景。

### 数据库变更

- `sql/init/mysql.sql` 已同步新增字段。
- 新增升级脚本：`sql/upgrade/20260611_add_init_turn_date.sql`。
- 新增升级脚本：`sql/upgrade/20260611_add_log_detail_create_time.sql`。

### 验证结果

- 已通过编译验证：`mvn -pl nbatch-admin -am -DskipTests compile`。
- 全量编译仍需本地 Maven 可解析 `com.gbase:gbase-jdbc:9.5.0.7`。

### 提交建议

- 建议提交前确认只暂存源码、模板、SQL 和 README 变更，不要提交本地运行产物。
- 推荐提交信息：`feat: enhance workflow rerun and node management UI`。

## 2026-06-13 修改报告

### 后端能力

- 运行状态枚举新增 `已跳过` 和 `已停止`，运行作业详情增加跳过数、停止数统计。
- 新增停止最新运行作业接口：`/work/stopLatestRunWork`，只允许待执行、已下发、进行中的最新运行作业停止。
- 新增执行器停止指令链路：`StopRunNodeParam`、`ExecutorBiz.stopRunNode`、`/stopRunNode` RPC 入口和 `RunNodeStopHandler`。
- 执行器线程池支持按 `nodeLogId` 停止队列中或运行中的 `BatchRunnable`，停止后写入 `STOPPED` 本地事件。
- 运行节点事件拉取新增 `STOPPED` 处理，调度中心收到后更新运行节点日志、节点结束时间和运行作业状态。
- 失败策略按 `stop`、`skip`、`retry` 统一处理：跳过节点可满足后继依赖，停止节点会阻断运行作业继续推进。
- 恢复重跑支持从已停止状态恢复，只重置异常、停止、已下发或运行中的节点。

### 前端交互

- 批次流程列表操作菜单新增“停止作业”，对最新一次运行作业执行停止。
- 批次流程详情运行历史新增“跳过”和“停止”统计列。
- 作业节点列表和批次流程列表状态标签适配“已跳过”“已停止”。
- 恢复重跑确认文案更新为异常、停止或卡住节点恢复，已完成和已跳过节点不重跑。

### 验证结果

- 本次 README 更新基于提交 `406648e` 的代码差异整理。
- 建议提交前执行：`mvn -pl nbatch-admin -am -DskipTests compile`。

## 常用开发命令

```bash
# 编译 admin 和依赖模块
mvn -pl nbatch-admin -am -DskipTests compile

# 打包 admin
mvn -pl nbatch-admin -am -DskipTests package

# 编译 core
mvn -pl nbatch-core -DskipTests compile

# 全量编译
mvn -DskipTests compile
```

## 目录说明

```text
nbatch-admin/src/main/java/com/nbatch/job/admin
├── controller      # Web 控制器
├── service         # 业务服务
├── core/helper     # 调度、运行作业、运行节点辅助逻辑
├── core/thread     # 调度线程、注册线程、日志回调处理
├── mapper          # MyBatis-Plus Mapper
└── core/domain     # PO、VO、Param 等数据模型

nbatch-admin/src/main/resources
├── templates       # Freemarker 页面
├── static          # 前端静态资源
├── sqlmap          # MyBatis XML
└── application.yml # 管理端配置
```

## 注意事项

- 不要把生产数据库账号、密码、内网 IP 直接提交到仓库。根目录 `.env` 已被 `.gitignore` 忽略，提交时只保留 `.env.example`。
- `nbatch-admin` 可独立编译运行；`nbatch-handler` 和 `nbatch-consumer` 依赖部分数据库驱动，缺失驱动时需要先处理 Maven 依赖。
- 执行器端口 `nbatch.job.executor.port` 需要可被调度中心访问。
- 调度中心和执行器的 `accessToken` 必须一致。
- 初始化 SQL 中可能包含演示数据，正式环境请按需清理或替换。
