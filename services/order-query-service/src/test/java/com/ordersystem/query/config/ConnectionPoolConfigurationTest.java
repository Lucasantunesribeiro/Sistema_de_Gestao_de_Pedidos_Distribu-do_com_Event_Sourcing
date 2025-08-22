package com.ordersystem.query.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test para validar otimizações do Connection Pool HikariCP no Query Service
 * Target: Reduzir latência queries 200ms → 50ms
 */
@SpringBootTest
@ActiveProfiles("test")
class ConnectionPoolConfigurationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldConfigureOptimizedConnectionPoolForReadWorkload() {
        // Given: DataSource deve ser HikariCP
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        
        // Then: Pool otimizado para read-heavy workload
        assertThat(hikariDataSource.getMaximumPoolSize())
                .as("Maximum pool size deve ser 25 para queries concorrentes")
                .isEqualTo(25);
                
        assertThat(hikariDataSource.getMinimumIdle())
                .as("Minimum idle deve ser 12 para read queries sempre prontas")
                .isEqualTo(12);
    }

    @Test
    void shouldConfigureOptimizedConnectionTimeoutForQueries() {
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        
        // Then: Connection timeout otimizado para queries
        assertThat(hikariDataSource.getConnectionTimeout())
                .as("Connection timeout deve ser 10000ms para queries rápidas")
                .isEqualTo(10000);
    }

    @Test
    void shouldConfigureQueryServicePoolMonitoring() {
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        
        // Then: Monitoring específico para Query Service
        assertThat(hikariDataSource.isRegisterMbeans())
                .as("MBean registration deve estar habilitado")
                .isTrue();
                
        assertThat(hikariDataSource.getPoolName())
                .as("Pool name deve identificar Query Service")
                .isEqualTo("QueryServiceHikariCP");
    }

    @Test
    void shouldConfigureReadOptimizedLifetime() {
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        
        // Then: Lifetime otimizado para read workload
        assertThat(hikariDataSource.getMaxLifetime())
                .as("Max lifetime deve ser 30 minutos para reads")
                .isEqualTo(1800000);
                
        assertThat(hikariDataSource.getIdleTimeout())
                .as("Idle timeout deve ser 10 minutos para manter conexões")
                .isEqualTo(600000);
    }
}