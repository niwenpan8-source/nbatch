package com.nbatch.job.executor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * 启动类
 * @author Mr.ni
 */
@SpringBootApplication
public class NbatchConsumerApplication {

	public static void main(String[] args) {
        SpringApplication.run(NbatchConsumerApplication.class, args);
	}

}