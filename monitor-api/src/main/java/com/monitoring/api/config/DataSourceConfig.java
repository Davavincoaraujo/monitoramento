package com.monitoring.api.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * Configuração do DataSource para ajustar URL do Render.com
 * 
 * O Render fornece URLs no formato: postgresql://user:pass@host/db
 * O Spring Boot precisa de: jdbc:postgresql://user:pass@host/db
 */
@Configuration
public class DataSourceConfig {

    private final Environment env;

    public DataSourceConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public DataSource dataSource() {
        String url = env.getProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/monitoring");
        
        // Se a URL não começa com jdbc:, adiciona o prefixo
        if (!url.startsWith("jdbc:")) {
            url = "jdbc:" + url;
        }
        
        String username = env.getProperty("spring.datasource.username", "monitor");
        String password = env.getProperty("spring.datasource.password", "monitor123");
        
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}
