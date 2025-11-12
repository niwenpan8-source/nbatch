package com.nbatch.job.admin.core.domain.po;

import cn.hutool.core.text.StrPool;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.util.StringUtils;

/**
 * @description: 任务用户实体类
 * @author: Mr.ni
 * @date: 2025/11/6
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("nbatch_job_user")
public class JobUserPo extends Model<JobUserPo> {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
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

    /**
     * 权限验证
     */
    public boolean validPermission(String jobGroup){
        if (this.role == 1) {
            return true;
        } else {
            if (StringUtils.hasText(this.permission)) {
                for (String permissionItem : this.permission.split(StrPool.COMMA)) {
                    if (String.valueOf(jobGroup).equals(permissionItem)) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

}
