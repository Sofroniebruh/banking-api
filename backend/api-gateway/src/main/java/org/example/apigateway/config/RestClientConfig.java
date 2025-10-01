package org.example.apigateway.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @Qualifier("downstreamRestClient")
    public RestClient downstreamRestClient(RestClient.Builder builder) {
        return builder
                .requestInterceptor(new DownstreamHeadersInterceptor())
                .build();
    }
}