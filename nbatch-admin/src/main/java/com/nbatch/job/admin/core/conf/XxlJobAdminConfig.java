package com.nbatch.job.admin.core.conf;

import com.nbatch.job.admin.core.alarm.JobAlarmer;
import com.nbatch.job.admin.core.scheduler.XxlJobScheduler;
import com.nbatch.job.admin.mapper.IJobGroupMapper;
import com.nbatch.job.admin.mapper.IJobInfoMapper;
import com.nbatch.job.admin.mapper.IJobLogMapper;
import com.nbatch.job.admin.mapper.IJobLogReportMapper;
import com.nbatch.job.admin.mapper.IJobRegistryMapper;
import lombok.Getter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.Arrays;

/**
 * 配置类
 * @author Mr.ni
 */

@Component
public class XxlJobAdminConfig implements InitializingBean, DisposableBean {

    @Getter
    private static XxlJobAdminConfig adminConfig = null;


    // ---------------------- XxlJobScheduler ----------------------

    private XxlJobScheduler xxlJobScheduler;

    @Override
    public void afterPropertiesSet() throws Exception {
        adminConfig = this;
        xxlJobScheduler = new XxlJobScheduler();
        xxlJobScheduler.init();
    }

    @Override
    public void destroy() {
        xxlJobScheduler.destroy();
    }


    // ---------------------- XxlJobScheduler ----------------------

    // conf
    @Value("${xxl.job.i18n}")
    private String i18n;

    @Getter
    @Value("${xxl.job.accessToken}")
    private String accessToken;

    @Getter
    @Value("${xxl.job.timeout}")
    private int timeout;

    @Getter
    @Value("${spring.mail.from}")
    private String emailFrom;

    @Value("${xxl.job.triggerpool.fast.max}")
    private int triggerPoolFastMax;

    @Value("${xxl.job.triggerpool.slow.max}")
    private int triggerPoolSlowMax;

    @Value("${xxl.job.logretentiondays}")
    private int logretentiondays;

    // dao, service
    @Getter
    @Resource
    private IJobLogMapper jobLogMapper;
    @Getter
    @Resource
    private IJobInfoMapper jobInfoMapper;
    @Getter
    @Resource
    private IJobRegistryMapper jobRegistryMapper;
    @Getter
    @Resource
    private IJobGroupMapper jobGroupMapper;
    @Getter
    @Resource
    private IJobLogReportMapper jobLogReportMapper;
    @Getter
    @Resource
    private JavaMailSender mailSender;
    @Getter
    @Resource
    private DataSource dataSource;
    @Getter
    @Resource
    private JobAlarmer jobAlarmer;


    public String getI18n() {
        if (!Arrays.asList("zh_CN", "zh_TC", "en").contains(i18n)) {
            return "zh_CN";
        }
        return i18n;
    }

    public int getTriggerPoolFastMax() {
        if (triggerPoolFastMax < 200) {
            return 200;
        }
        return triggerPoolFastMax;
    }

    public int getTriggerPoolSlowMax() {
        if (triggerPoolSlowMax < 100) {
            return 100;
        }
        return triggerPoolSlowMax;
    }

    public int getLogretentiondays() {
        if (logretentiondays < 7) {
            // Limit greater than or equal to 7, otherwise close
            return -1;
        }
        return logretentiondays;
    }

}
