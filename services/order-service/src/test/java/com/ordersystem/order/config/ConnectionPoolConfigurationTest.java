package com.ordersystem.order.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test para validar otimizações do Connection Pool HikariCP
 * Target: Aumentar throughput de 500 → 1000 req/sec
 */
@SpringBootTest
@ActiveProfiles("test")
class ConnectionPoolConfigurationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldConfigureOptimizedConnectionPoolSize() {
        // Given: DataSource deve ser HikariCP
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        
        // Then: Verificar pool size otimizado para 1000 req/sec
        assertThat(hikariDataSource.getMaximumPoolSize())
                .as("Maximum pool size deve ser 25 para suportar 1000 req/sec")
                .isEqualTo(25);
                
        assertThat(hikariDataSource.getMinimumIdle())
                .as("Minimum idle deve ser 10 para manter conexões prontas")
                .isEqualTo(10);
    }

    @Test
    void shouldConfigureOptimizedConnectionTimeout() {
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        
        // Then: Connection timeout otimizado para 10s
        assertThat(hikariDataSource.getConnectionTimeout())
                .as("Connection timeout deve ser 10000ms para falha rápida")
                .isEqualTo(10000);
    }

    @Test
    void shouldConfigurePoolMonitoring() {
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        
        // Then: MBean registration para monitoring
        assertThat(hikariDataSource.isRegisterMbeans())
                .as("MBean registration deve estar habilitado para monitoring")
                .isTrue();
                
        // Pool name para identificação
        assertThat(hikariDataSource.getPoolName())
                .as("Pool name deve estar configurado")
                .isEqualTo("OrderServiceHikariCP");
    }

    @Test
    void shouldConfigureLeakDetection() {
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        
        // Then: Leak detection configurado
        assertThat(hikariDataSource.getLeakDetectionThreshold())
                .as("Leak detection deve estar em 60s")
                .isEqualTo(60000);
    }

    @Test
    void shouldConfigureLifetimeSettings() {
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        
        // Then: Lifetime settings otimizados
        assertThat(hikariDataSource.getMaxLifetime())
                .as("Max lifetime deve ser 20 minutos")
                .isEqualTo(1200000);
                
        assertThat(hikariDataSource.getIdleTimeout())
                .as("Idle timeout deve ser 5 minutos")
                .isEqualTo(300000);
    }
}