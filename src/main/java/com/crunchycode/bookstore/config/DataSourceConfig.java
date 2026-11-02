package com.crunchycode.bookstore.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Production and Development DataSource Configuration.
 * Automatically resolves and adapts Cloud PaaS database URIs (Render PostgreSQL / Railway MySQL)
 * into standard JDBC connection strings while preserving local development configuration.
 */
@Configuration
@Profile("!test")
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    @Primary
    public DataSource dataSource(Environment env) {
        String rawUrl = env.getProperty("DATABASE_URL");
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = env.getProperty("MYSQL_URL");
        }

        HikariConfig config = new HikariConfig();

        if (rawUrl != null && !rawUrl.isBlank()) {
            if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
                log.info("Detected Cloud PostgreSQL URL (Render). Parsing connection parameters...");
                try {
                    URI uri = new URI(rawUrl);
                    String userInfo = uri.getUserInfo();
                    String username = "";
                    String password = "";
                    if (userInfo != null && userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        username = parts[0];
                        password = parts[1];
                    } else if (userInfo != null) {
                        username = userInfo;
                    }

                    String host = uri.getHost();
                    int port = uri.getPort() != -1 ? uri.getPort() : 5432;
                    String path = uri.getPath();
                    String database = (path != null && path.length() > 1) ? path.substring(1) : "crunchy_bookstore";

                    String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database + "?sslmode=require";
                    config.setJdbcUrl(jdbcUrl);
                    config.setUsername(username);
                    config.setPassword(password);
                    config.setDriverClassName("org.postgresql.Driver");
                    log.info("Successfully configured Cloud PostgreSQL connection to host: {}, port: {}, database: {}", host, port, database);
                } catch (Exception e) {
                    log.warn("Failed to parse cloud PostgreSQL URL. Falling back to properties: {}", e.getMessage());
                    fallbackToProperties(config, env);
                }
            } else if (rawUrl.startsWith("mysql://") || rawUrl.startsWith("mysql2://")) {
                log.info("Detected Cloud MySQL URL. Parsing connection parameters...");
                try {
                    URI uri = new URI(rawUrl);
                    String userInfo = uri.getUserInfo();
                    String username = "";
                    String password = "";
                    if (userInfo != null && userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        username = parts[0];
                        password = parts[1];
                    } else if (userInfo != null) {
                        username = userInfo;
                    }

                    String host = uri.getHost();
                    int port = uri.getPort() != -1 ? uri.getPort() : 3306;
                    String path = uri.getPath();
                    String database = (path != null && path.length() > 1) ? path.substring(1) : "crunchy_bookstore";

                    String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
                    config.setJdbcUrl(jdbcUrl);
                    config.setUsername(username);
                    config.setPassword(password);
                    config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                    log.info("Successfully configured Cloud MySQL connection to host: {}, port: {}, database: {}", host, port, database);
                } catch (Exception e) {
                    log.warn("Failed to parse cloud database URL. Falling back to application properties: {}", e.getMessage());
                    fallbackToProperties(config, env);
                }
            } else {
                fallbackToProperties(config, env);
            }
        } else {
            fallbackToProperties(config, env);
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(20000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }

    private void fallbackToProperties(HikariConfig config, Environment env) {
        String jdbcUrl = env.getProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/crunchy_bookstore?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        String username = env.getProperty("spring.datasource.username", "root");
        String password = env.getProperty("spring.datasource.password", "");
        String driver = env.getProperty("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driver);
    }
}
