package org.nors.dev.codes.lpu.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Two datasources:
 * <ul>
 *   <li><b>Primary</b> — this system's Postgres (event_users, events, time logs)</li>
 *   <li><b>Gate</b> — gate attendance Postgres (students, employees, RFID) read-only</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, GateAttendanceProperties.class})
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties eventDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource(DataSourceProperties eventDataSourceProperties) {
        return eventDataSourceProperties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean
    public DataSource gateDataSource(GateAttendanceProperties gate) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(gate.jdbcUrl());
        ds.setUsername(gate.getDbUsername());
        ds.setPassword(gate.getDbPassword());
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setPoolName("gate-db");
        ds.setMaximumPoolSize(8);
        ds.setMinimumIdle(0);
        ds.setReadOnly(true);
        // Don't kill app startup if gate Postgres is temporarily down.
        ds.setInitializationFailTimeout(-1);
        ds.setConnectionTimeout(10_000);
        return ds;
    }
}
