package com.nbatch.job.admin.core.helper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogPo;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeLogMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 验证批量未闭环日志查询的批次隔离规则。
 */
@ExtendWith(MockitoExtension.class)
public class RunNodeLogHelperTest {

    @Mock
    private IJobWorkNodeMapper jobWorkNodeMapper;

    @Mock
    private IJobWorkRunNodeLogMapper jobWorkRunNodeLogMapper;

    private RunNodeLogHelper runNodeLogHelper;

    @BeforeAll
    public static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        assistant.setCurrentNamespace("test.JobWorkRunNodeLogPo");
        TableInfoHelper.initTableInfo(assistant, JobWorkRunNodeLogPo.class);
    }

    @BeforeEach
    public void setUp() {
        runNodeLogHelper = new RunNodeLogHelper();
        ReflectionTestUtils.setField(runNodeLogHelper, "jobWorkNodeMapper", jobWorkNodeMapper);
        ReflectionTestUtils.setField(runNodeLogHelper, "jobWorkRunNodeLogMapper", jobWorkRunNodeLogMapper);
    }

    @Test
    public void shouldIgnoreUnclosedLogFromPreviousTurnDate() {
        Date currentTurnDate = new Date(1_700_000_000_000L);
        Date previousTurnDate = new Date(1_699_913_600_000L);
        JobWorkNodePo node = new JobWorkNodePo();
        node.setNodeId("node-1");
        node.setWorkId("work-1");
        node.setTurnDate(currentTurnDate);

        JobWorkRunNodeLogPo previousLog = new JobWorkRunNodeLogPo();
        previousLog.setNodeLogId("log-previous");
        previousLog.setNodeId("node-1");
        previousLog.setWorkId("work-1");
        previousLog.setTurnDate(previousTurnDate);

        JobWorkRunNodeLogPo currentLog = new JobWorkRunNodeLogPo();
        currentLog.setNodeLogId("log-current");
        currentLog.setNodeId("node-1");
        currentLog.setWorkId("work-1");
        currentLog.setTurnDate(currentTurnDate);
        when(jobWorkRunNodeLogMapper.selectList(any())).thenReturn(Arrays.asList(previousLog, currentLog));

        Map<String, JobWorkRunNodeLogPo> result = runNodeLogHelper.findUnclosedMap(Arrays.asList(node));

        String key = RunNodeLogHelper.buildWorkNodeTurnKey("work-1", "node-1", currentTurnDate);
        assertEquals("log-current", result.get(key).getNodeLogId());
    }
}
