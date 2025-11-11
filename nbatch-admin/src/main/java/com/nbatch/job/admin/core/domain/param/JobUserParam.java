package com.nbatch.job.admin.core.domain.param;

import cn.hutool.core.text.StrPool;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.util.StringUtils;

/**
 * @description: 任务用户实体类
 * @author: Mr.ni
 * @date: 2025/11/6
 */
@Data
@Accessors(chain = true)
public class JobUserParam {

    /**
     * 主键ID
     */
    private String id;

    /**
     * 账号
     */
    private String username;

    /**
     * 密码加密信息
     */
    private String password;

    /**
     * 登录token
     */
    private String token;

    /**
     * 角色：0-普通用户、1-管理员
     */
    private Integer role;

    /**
     * 权限：执行器ID列表，多个逗号分割
     */
    private String permission;

}
