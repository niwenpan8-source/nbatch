package com.nbatch.job.admin.core.conf;

import com.nbatch.job.admin.core.helper.RunNodeHelper;
import com.nbatch.job.admin.core.helper.RunWorkHelper;
import com.nbatch.job.admin.core.scheduler.JobScheduler;
import com.nbatch.job.admin.core.thread.RegistryAddressHelper;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeLogDetailMapper;
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
 */

@Component
public class JobAdminConfig implements InitializingBean, DisposableBean {

    @Getter
    private static JobAdminConfig adminConfig = null;

    private JobScheduler jobScheduler;

    @Override
    public void afterPropertiesSet() throws Exception {
        adminConfig = this;
        jobScheduler = new JobScheduler();
        jobScheduler.init();
    }

    @Override
    public void destroy() {
        jobScheduler.destroy();
    }

    // 配置
    @Value("${nbatch.job.i18n}")
    private String i18n;

    @Getter
    @Value("${nbatch.job.accessToken}")
    private String accessToken;

    @Getter
    @Value("${nbatch.job.timeout}")
    private int timeout;

    @Getter
    @Value("${spring.mail.from}")
    private String emailFrom;

    @Value("${nbatch.job.triggerpool.fast.max}")
    private int triggerPoolFastMax;

    @Value("${nbatch.job.triggerpool.slow.max}")
    private int triggerPoolSlowMax;

    // 数据访问对象和服务
    @Getter
    @Resource
    private IJobWorkMapper jobWorkMapper;
    @Getter
    @Resource
    private RegistryAddressHelper registryAddressHelper;
    @Getter
    @Resource
    private JavaMailSender mailSender;
    @Getter
    @Resource
    private DataSource dataSource;
    @Getter
    @Resource
    private RunNodeHelper runNodeHelper;
    @Getter
    @Resource
    private RunWorkHelper runWorkHelper;
    @Getter
    @Resource
    private IJobWorkRunNodeLogDetailMapper logDetailMapper;

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

}
