package org.nors.dev.codes.lpu.config;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Applies idempotent DDL patches to this system's (primary) database
 * before Hibernate schema validation runs.
 */
@Configuration
public class SchemaMigrationConfig {

    private static final Logger log = LogManager.getLogger(SchemaMigrationConfig.class);
    private static final String MIGRATION_RESOURCE = "db/schema-migrations.sql";

    @Bean
    public SchemaMigrator schemaMigrator(DataSource dataSource) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<String> statements = loadStatements(MIGRATION_RESOURCE);
        for (String sql : statements) {
            jdbc.execute(sql);
        }
        log.info("Schema migration applied from {} ({} statement(s))", MIGRATION_RESOURCE, statements.size());
        return new SchemaMigrator();
    }

    private static List<String> loadStatements(String classpathLocation) {
        try {
            ClassPathResource resource = new ClassPathResource(classpathLocation);
            String content;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
            )) {
                content = reader.lines().collect(Collectors.joining("\n"));
            }

            List<String> statements = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (String rawLine : content.split("\n")) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }
                current.append(rawLine).append('\n');
                if (line.endsWith(";")) {
                    String sql = current.toString().trim();
                    if (sql.endsWith(";")) {
                        sql = sql.substring(0, sql.length() - 1).trim();
                    }
                    if (!sql.isEmpty()) {
                        statements.add(sql);
                    }
                    current.setLength(0);
                }
            }
            String trailing = current.toString().trim();
            if (!trailing.isEmpty()) {
                if (trailing.endsWith(";")) {
                    trailing = trailing.substring(0, trailing.length() - 1).trim();
                }
                if (!trailing.isEmpty()) {
                    statements.add(trailing);
                }
            }
            return statements;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load schema migrations from " + classpathLocation, ex);
        }
    }

    public static final class SchemaMigrator {
    }
}
