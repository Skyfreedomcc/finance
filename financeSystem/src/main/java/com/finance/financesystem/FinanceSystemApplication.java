package com.finance.financesystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.finance.financesystem.mapper")
public class FinanceSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceSystemApplication.class, args);
    }

}
