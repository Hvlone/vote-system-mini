package com.vote.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.vote.system.mapper")
public class VoteSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoteSystemApplication.class, args);
    }
}
