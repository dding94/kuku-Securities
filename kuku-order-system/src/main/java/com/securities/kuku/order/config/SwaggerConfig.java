package com.securities.kuku.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  @Bean
  public OpenAPI orderSystemOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Kuku Order System API")
                .version("v1")
                .description("주문 생성, 조회, 취소 API - Kuku Securities")
                .contact(new Contact().name("Kuku Securities Dev Team")));
  }
}
