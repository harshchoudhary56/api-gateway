package com.apple.inc.gateway.config;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MongoDB reactive configuration for the API Gateway.
 *
 * <p>Configures:
 * <ul>
 *   <li>{@link MongoClient} — reactive driver connection</li>
 *   <li>{@link ReactiveMongoDatabaseFactory} — factory for database access</li>
 *   <li>{@link ReactiveMongoTemplate} — template for custom queries</li>
 *   <li>{@link ReactiveMongoTransactionManager} — transaction support for multi-document ops</li>
 * </ul>
 */
@Configuration
@EnableTransactionManagement
@EnableReactiveMongoRepositories(basePackages = "com.apple.inc.gateway.apirecord.repository")
public class MongoConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database:school_erp_gateway}")
    private String databaseName;

    @Bean
    public MongoClient reactiveMongoClient() {
        return MongoClients.create(mongoUri);
    }

    @Bean
    public ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory(MongoClient mongoClient) {
        return new SimpleReactiveMongoDatabaseFactory(mongoClient, databaseName);
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate(ReactiveMongoDatabaseFactory factory) {
        return new ReactiveMongoTemplate(factory);
    }

    @Bean
    public ReactiveMongoTransactionManager reactiveMongoTransactionManager(ReactiveMongoDatabaseFactory factory) {
        return new ReactiveMongoTransactionManager(factory);
    }
}

