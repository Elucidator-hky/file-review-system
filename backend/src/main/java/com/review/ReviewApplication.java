package com.review;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 文件审查系统启动类
 *
 * @author Claude
 * @date 2025-11-28
 */
@SpringBootApplication
@MapperScan("com.review.mapper")
public class ReviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReviewApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("文件审查系统启动成功！");
        System.out.println("API文档地址：http://localhost:8080/api/doc.html");
        System.out.println("========================================\n");
    }
}
