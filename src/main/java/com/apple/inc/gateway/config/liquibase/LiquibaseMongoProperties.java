package com.apple.inc.gateway.config.liquibase;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@RefreshScope
@ConfigurationProperties(prefix = "spring.liquibase.mongodb")
public class LiquibaseMongoProperties {

    private String uri;
    private String context;
    private String database;
    private String changeLog;
}
