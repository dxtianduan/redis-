package com.hmdp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

/**
 * Knife4j / Swagger 接口文档配置。
 *
 * <p>启动项目后访问 <b>http://localhost:8081/doc.html</b> 即可看到全部接口，并可直接在页面上调试。</p>
 *
 * <p>接口是自动扫描出来的：{@link RequestHandlerSelectors#basePackage(String)} 指定扫描
 * {@code com.hmdp.controller} 整个包，所以以后新增 Controller 或接口，文档会自动出现，
 * 不需要任何手工维护。</p>
 *
 * <p><b>@EnableSwagger2WebMvc 的作用</b>：knife4j 3.0.2 依赖的 springfox-boot-starter 会同时启用
 * Swagger2 和 OpenAPI3 两套自动配置，两套都注册文档资源会导致 /swagger-resources 返回空、
 * 文档页面加载不出接口。加上这个注解显式指定走 Swagger2，与下面的
 * {@code DocumentationType.SWAGGER_2} 保持一致。</p>
 */
@Configuration
@EnableSwagger2WebMvc
public class Knife4jConfig {

    @Bean
    public Docket defaultApi2() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                // 扫描 com.hmdp.controller 包下的所有接口
                .apis(RequestHandlerSelectors.basePackage("com.hmdp.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("黑马点评 接口文档")
                .description("hm-dianping 本地接口调试。登录接口拿到的 token 填到右上角『全局参数』或请求头 authorization 里。")
                .version("1.0")
                .build();
    }
}
