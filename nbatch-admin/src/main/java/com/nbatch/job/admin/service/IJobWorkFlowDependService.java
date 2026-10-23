package com.nbatch.job.admin.service;

import com.nbatch.job.admin.core.domain.po.JobWorkFlowDependencyPo;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 作业流程依赖管理服务。
 */
public interface IJobWorkFlowDependService {

    /**
     * 保存一条流程依赖。
     *
     * @return 新增依赖ID
     */
    String save(JobWorkFlowDependencyPo dependency);

    /**
     * 删除流程组及其包含的节点、关联依赖和节点文件配置。
     */
    int deleteGroup(String workId, String groupId);

    /**
     * 导出当前作业的流程配置工作簿。
     * <p>
     * Sheet 顺序为组、各节点类型、依赖关系。
     */
    void export(String workId, HttpServletResponse response) throws IOException;

    /**
     * 下载流程配置导入模板。
     */
    void downloadTemplate(HttpServletResponse response) throws IOException;

    /**
     * 导入当前作业的流程配置工作簿。
     *
     * @return 成功导入的组、节点和依赖关系总数
     */
    int importExcel(String workId, MultipartFile file);
}
