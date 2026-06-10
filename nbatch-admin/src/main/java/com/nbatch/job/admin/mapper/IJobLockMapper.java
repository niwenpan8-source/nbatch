package com.nbatch.job.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nbatch.job.admin.core.domain.po.JobLockPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @description: 任务执行器锁
 * @author: Mr.ni
 * @date: 2025/11/6
 */
public interface IJobLockMapper extends BaseMapper<JobLockPo> {

    @Insert("insert ignore into nbatch_job_lock(lock_name) values(#{lockName})")
    int insertIgnore(@Param("lockName") String lockName);

    @Select("select lock_name from nbatch_job_lock where lock_name = #{lockName} for update")
    JobLockPo lockByName(@Param("lockName") String lockName);
}
