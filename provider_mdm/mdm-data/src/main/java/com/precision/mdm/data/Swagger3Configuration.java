package com.precision.mdm.data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class Swagger3Configuration {

	@Bean
	public OpenAPI customOpenAPI(@Value("${application-description}") String appDescription,
			@Value("${application-version}") String appVersion) {

		return new OpenAPI().info(new Info().title("MDM Data API").version(appVersion).description(appDescription)
				.termsOfService("http://swagger.io/terms/")
				.license(new License().name("Apache 2.0").url("http://springdoc.org")));
	}

}
