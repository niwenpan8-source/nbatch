package com.nbatch.job.admin;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动类
 * @author Mr.ni
 */
@Slf4j
@EnableScheduling
@MapperScan("com.nbatch.**.mapper")
@SpringBootApplication
public class NbatchAdminApplication {

	public static void main(String[] args) {
        SpringApplication.run(NbatchAdminApplication.class, args);
	}

}