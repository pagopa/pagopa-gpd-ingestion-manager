package it.gov.pagopa.gpd.ingestion.manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class HttpClientConfiguration {
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }
}