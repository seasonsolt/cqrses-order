package com.thin.cqrsesorder.infrastructure.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;

//@Configuration
//@EnableElasticsearchRepositories(basePackages = "com.thin.cqrsesorder.repository")
public class EsClientConfiguration {

    @Bean
    public ElasticsearchClient esClient() {
        RestClient httpClient = RestClient.builder(
                new HttpHost("localhost", 9300)
        ).build();

        ElasticsearchTransport transport = new RestClientTransport(
                httpClient,
                new JacksonJsonpMapper()
        );

        return new ElasticsearchClient(transport);
    }

}
