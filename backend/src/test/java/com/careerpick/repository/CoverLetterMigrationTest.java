package com.careerpick.repository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "TEST_MIGRATION_DB_URL", matches = ".+")
class CoverLetterMigrationTest {
    @Test void upgradesV1WithoutLosingExistingData() throws Exception {
        String url = System.getenv("TEST_MIGRATION_DB_URL");
        String username = System.getenv("TEST_DB_USERNAME");
        String password = System.getenv("TEST_DB_PASSWORD");
        // 전용 빈 DB에서만 검증하며 기존 스키마는 초기화하지 않는다.
        try (var connection = DriverManager.getConnection(url, username, password);
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()")) {
            result.next();
            assertThat(result.getInt(1)).as("마이그레이션 검증에는 빈 전용 DB가 필요합니다").isZero();
        }
        Flyway.configure().dataSource(url, username, password).target("1").load().migrate();
        try (var connection = DriverManager.getConnection(url, username, password); var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO users(name, email, password_hash) VALUES ('보존 사용자', 'migration@test.kr', 'hash')");
        }
        var flyway = Flyway.configure().dataSource(url, username, password).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        try (var connection = DriverManager.getConnection(url, username, password);
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT name FROM users WHERE email = 'migration@test.kr'")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString(1)).isEqualTo("보존 사용자");
        }
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }
}
