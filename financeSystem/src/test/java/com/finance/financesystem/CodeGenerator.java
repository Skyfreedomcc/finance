package com.finance.financesystem; // 【注意】如果你的包名不是这个，这里会报错，请改成你实际的包名（看第一行）

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.sql.Types;
import java.util.Collections;

/**
 * 代码生成器：一键生成 Entity, Mapper, Service, Controller
 */
public class CodeGenerator {

    public static void main(String[] args) {
        // 1. 数据库配置
        FastAutoGenerator.create("jdbc:mysql://localhost:3306/finance_db?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&useSSL=false",
                        "root",
                        "123456") // 【重要】如果你修改了数据库密码，这里也要改！

                // 2. 全局配置
                .globalConfig(builder -> {
                    builder.author("AI_Assistant") // 作者名，你可以改成自己的名字
                            .outputDir(System.getProperty("user.dir") + "/src/main/java") // 输出路径：自动识别你的项目路径
                            .disableOpenDir(); // 生成后不打开文件夹
                })

                // 3. 包配置
                .packageConfig(builder -> {
                    builder.parent("com.finance.financesystem") // 【重要】父包名：请去 src/main/java 下确认你的主包名是什么，保持一致！
                            .pathInfo(Collections.singletonMap(OutputFile.xml, System.getProperty("user.dir") + "/src/main/resources/mapper")); // Mapper XML 生成到 resources 目录下
                })

                // 4. 策略配置
                .strategyConfig(builder -> {
                    builder.addInclude("sys_user", "finance_book", "finance_account", "finance_transaction", "finance_split") // 需要生成的表名

                            // Entity 策略
                            .entityBuilder()
                            .enableLombok() // 开启 Lombok，自动生成 Get/Set
                            .enableTableFieldAnnotation() // 开启字段注解

                            // Controller 策略
                            .controllerBuilder()
                            .enableRestStyle(); // 开启 @RestController
                })

                // 5. 模板引擎
                .templateEngine(new FreemarkerTemplateEngine())

                // 6. 执行！
                .execute();

        System.out.println("=========================================");
        System.out.println("代码生成成功！请去 src/main/java 查看成果！");
        System.out.println("=========================================");
    }
}