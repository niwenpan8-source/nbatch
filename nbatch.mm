<map version="1.0.1">
  <node TEXT="nbatch 批量作业调度平台">
    <node TEXT="项目定位">
      <node TEXT="Spring Boot 批处理平台"/>
      <node TEXT="基于 XXL-JOB 调度模型扩展"/>
      <node TEXT="支持作业流程编排"/>
      <node TEXT="支持多节点批处理"/>
      <node TEXT="支持跨库处理与脚本执行"/>
    </node>
    <node TEXT="核心模块">
      <node TEXT="nbatch-core">
        <node TEXT="调度核心"/>
        <node TEXT="执行器通信"/>
        <node TEXT="RPC 模型"/>
        <node TEXT="状态枚举"/>
        <node TEXT="日志事件模型"/>
      </node>
      <node TEXT="nbatch-admin">
        <node TEXT="Web 管理端"/>
        <node TEXT="任务管理"/>
        <node TEXT="执行器管理"/>
        <node TEXT="批次流程管理"/>
        <node TEXT="作业节点管理"/>
        <node TEXT="调度与监控线程"/>
      </node>
      <node TEXT="nbatch-handler">
        <node TEXT="节点处理器"/>
        <node TEXT="SQL 执行"/>
        <node TEXT="脚本执行"/>
        <node TEXT="存储过程"/>
        <node TEXT="文件导入导出"/>
        <node TEXT="停止节点处理"/>
      </node>
      <node TEXT="nbatch-consumer">
        <node TEXT="示例执行器"/>
        <node TEXT="注册到调度中心"/>
        <node TEXT="执行具体任务"/>
      </node>
      <node TEXT="sql">
        <node TEXT="初始化脚本"/>
        <node TEXT="升级脚本"/>
      </node>
    </node>
    <node TEXT="作业模型">
      <node TEXT="作业流程">
        <node TEXT="一组可编排节点"/>
        <node TEXT="顺序流程"/>
        <node TEXT="翻牌日期流程"/>
      </node>
      <node TEXT="作业节点">
        <node TEXT="脚本"/>
        <node TEXT="SQL"/>
        <node TEXT="存储过程"/>
        <node TEXT="文件入库"/>
        <node TEXT="数据库导出文件"/>
        <node TEXT="Bean 模式"/>
      </node>
      <node TEXT="节点依赖">
        <node TEXT="依赖前置节点"/>
        <node TEXT="已完成可继续"/>
        <node TEXT="已跳过可继续"/>
      </node>
      <node TEXT="运行作业"/>
      <node TEXT="运行节点"/>
    </node>
    <node TEXT="运行状态">
      <node TEXT="待执行"/>
      <node TEXT="已下发"/>
      <node TEXT="进行中"/>
      <node TEXT="执行完毕"/>
      <node TEXT="执行异常"/>
      <node TEXT="已跳过"/>
      <node TEXT="已停止"/>
    </node>
    <node TEXT="调度闭环">
      <node TEXT="执行器注册"/>
      <node TEXT="调度中心下发节点"/>
      <node TEXT="执行器接收任务"/>
      <node TEXT="本地事件日志">
        <node TEXT="STARTED"/>
        <node TEXT="SUCCESS"/>
        <node TEXT="FAIL"/>
        <node TEXT="STOPPED"/>
      </node>
      <node TEXT="调度中心拉取事件"/>
      <node TEXT="ACK 确认"/>
      <node TEXT="更新节点与作业状态"/>
    </node>
    <node TEXT="重跑能力">
      <node TEXT="一键重跑">
        <node TEXT="重置最新运行批次"/>
        <node TEXT="所有节点重新执行"/>
      </node>
      <node TEXT="恢复重跑">
        <node TEXT="重置异常节点"/>
        <node TEXT="重置停止节点"/>
        <node TEXT="重置已下发或运行中节点"/>
        <node TEXT="已完成和已跳过保持不变"/>
      </node>
      <node TEXT="节点重跑">
        <node TEXT="指定节点重跑"/>
        <node TEXT="后继受影响节点一起重跑"/>
      </node>
      <node TEXT="初始化重跑">
        <node TEXT="按初始化翻牌日期定位批次"/>
        <node TEXT="清理之后运行记录"/>
        <node TEXT="缺失批次时自动初始化"/>
      </node>
    </node>
    <node TEXT="停止能力">
      <node TEXT="停止最新运行作业"/>
      <node TEXT="待执行/已下发/进行中可停止"/>
      <node TEXT="调度中心标记已停止"/>
      <node TEXT="下发 stopRunNode"/>
      <node TEXT="执行器按 nodeLogId 停止"/>
      <node TEXT="上报 STOPPED 事件"/>
    </node>
    <node TEXT="失败策略">
      <node TEXT="stop 停止节点和作业"/>
      <node TEXT="skip 标记已跳过并继续"/>
      <node TEXT="retry 扣减重试次数"/>
      <node TEXT="重试耗尽后停止"/>
    </node>
    <node TEXT="前端能力">
      <node TEXT="批次流程列表">
        <node TEXT="查询作业名称"/>
        <node TEXT="查看详情"/>
        <node TEXT="一键重跑"/>
        <node TEXT="初始化重跑"/>
        <node TEXT="停止作业"/>
        <node TEXT="恢复重跑"/>
      </node>
      <node TEXT="作业节点列表">
        <node TEXT="查询节点名称"/>
        <node TEXT="按运行状态筛选"/>
        <node TEXT="查看详情"/>
      </node>
      <node TEXT="详情弹窗">
        <node TEXT="基础信息"/>
        <node TEXT="运行历史"/>
        <node TEXT="状态统计"/>
      </node>
      <node TEXT="日志页面">
        <node TEXT="创建时间范围查询"/>
        <node TEXT="长日志详情查看"/>
      </node>
      <node TEXT="节点依赖配置">
        <node TEXT="节点选择弹窗"/>
        <node TEXT="当前节点单选"/>
        <node TEXT="依赖节点多选"/>
      </node>
    </node>
    <node TEXT="数据源能力">
      <node TEXT="MySQL"/>
      <node TEXT="OpenGauss"/>
      <node TEXT="Gauss"/>
      <node TEXT="GBase"/>
      <node TEXT="ClickHouse"/>
    </node>
    <node TEXT="技术栈">
      <node TEXT="JDK 8"/>
      <node TEXT="Spring Boot 2.7.18"/>
      <node TEXT="Spring Framework 5.3.31"/>
      <node TEXT="MyBatis-Plus"/>
      <node TEXT="Freemarker"/>
      <node TEXT="Netty"/>
      <node TEXT="Hutool"/>
      <node TEXT="MySQL"/>
    </node>
  </node>
</map>
