# nbatch

nbatch 是一个基于 Spring Boot 的批量作业调度与流程编排平台。项目在 XXL-JOB 调度模型基础上扩展了“作业流程 / 作业节点 / 节点依赖 / 节点运行状态 / 节点级日志闭环”等能力，适合用来管理多节点批处理、跨库数据处理、脚本执行和文件导入导出任务。

## 功能特性

- 调度中心：任务管理、执行器管理、调度日志、GLUE 代码编辑、用户管理。
- 作业流程：支持批次流程管理、节点管理、节点依赖配置、运行批次状态跟踪。
- 节点类型：支持脚本、存储过程、返回字符串存储过程、SQL 执行、文件入库、数据库导出文件、Bean 模式。
- 节点状态：支持待执行、进行中、执行完毕、执行异常。
- 重跑能力：支持最新运行批次一键重跑、异常节点恢复重跑、指定节点及其后继节点重跑。
- 执行闭环：执行器注册、任务下发、运行节点日志回传、运行节点事件拉取与 ACK。
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
- 恢复重跑：只重置异常或运行中的节点，已完成节点保持完成状态。
- 节点重跑：重置指定运行节点；如果该节点被其他节点依赖，则所有后继受影响节点也会一起重跑。

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
