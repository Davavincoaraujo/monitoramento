package com.monitoring.api.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Processa variáveis de ambiente ANTES do Spring Boot inicializar
 * 
 * O Render.com fornece URLs no formato: postgresql://user:pass@host/db
 * O Spring Boot precisa de: jdbc:postgresql://user:pass@host/db
 * 
 * Este processor adiciona o prefixo jdbc: automaticamente
 */
public class DataSourceConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url = environment.getProperty("SPRING_DATASOURCE_URL");
        
        if (url != null && !url.startsWith("jdbc:")) {
            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", "jdbc:" + url);
            
            environment.getPropertySources()
                    .addFirst(new MapPropertySource("renderDatabaseUrlFix", props));
        }
    }
}
