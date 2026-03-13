package com.ordersystem.unified.support;

import com.ordersystem.unified.inventory.model.Product;
import com.ordersystem.unified.inventory.model.Stock;
import com.ordersystem.unified.inventory.repository.ProductRepository;
import com.ordersystem.unified.inventory.repository.StockRepository;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class PostgresIntegrationTestSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresIntegrationTestSupport.class);
    private static final DatabaseRuntime DATABASE = DatabaseRuntime.start();

    @Autowired(required = false)
    protected ProductRepository productRepository;

    @Autowired(required = false)
    protected StockRepository stockRepository;

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::jdbcUrl);
        registry.add("spring.datasource.username", DATABASE::username);
        registry.add("spring.datasource.password", DATABASE::password);
        registry.add("spring.datasource.driver-class-name", DATABASE::driverClassName);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/migration/postgres");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("test.database.provider", DATABASE::provider);
    }

    protected void seedStock(String productId, int quantity) {
        seedStock(productId, productId, BigDecimal.ZERO, "DEFAULT", quantity);
    }

    protected void seedStock(String productId, String productName, BigDecimal price, int quantity) {
        seedStock(productId, productName, price, "DEFAULT", quantity);
    }

    protected void seedStock(String productId,
                             String productName,
                             BigDecimal price,
                             String warehouseId,
                             int quantity) {
        if (productRepository == null || stockRepository == null) {
            return;
        }

        Product product = productRepository.findById(productId)
            .orElseGet(() -> {
                Product created = new Product(productId, productName, productId, price);
                created.setActive(true);
                return productRepository.save(created);
            });

        Stock stock = stockRepository.findByProductIdAndWarehouseId(productId, warehouseId)
            .orElseGet(() -> new Stock(product, warehouseId, 0));

        int currentAvailable = stock.getAvailableQuantity() != null ? stock.getAvailableQuantity() : 0;
        if (currentAvailable < quantity) {
            stock.addStock(quantity - currentAvailable);
            stockRepository.save(stock);
        }
    }

    protected void seedStocks(Map<String, Integer> inventoryLevels) {
        inventoryLevels.forEach(this::seedStock);
    }

    private record DatabaseRuntime(String provider,
                                   String jdbcUrl,
                                   String username,
                                   String password,
                                   String driverClassName) {

        private static final String DEFAULT_DRIVER = "org.postgresql.Driver";

        static DatabaseRuntime start() {
            DatabaseRuntime external = fromEnvironment();
            if (external != null) {
                LOGGER.info("Using externally configured PostgreSQL for tests");
                return external;
            }

            String provider = setting("test.db.provider", "TEST_DB_PROVIDER");
            if ("embedded".equalsIgnoreCase(provider)) {
                return startEmbedded();
            }
            if ("testcontainers".equalsIgnoreCase(provider)) {
                return startTestcontainers();
            }

            if (dockerAvailable()) {
                try {
                    return startTestcontainers();
                } catch (RuntimeException exception) {
                    LOGGER.warn("Testcontainers PostgreSQL startup failed, falling back to embedded PostgreSQL", exception);
                }
            } else {
                LOGGER.warn("Docker daemon unavailable for Testcontainers, falling back to embedded PostgreSQL");
            }

            return startEmbedded();
        }

        private static DatabaseRuntime fromEnvironment() {
            String jdbcUrl = setting("test.db.url", "TEST_DB_URL");
            if (jdbcUrl == null || jdbcUrl.isBlank()) {
                return null;
            }

            return new DatabaseRuntime(
                "external",
                jdbcUrl,
                settingOrDefault("test.db.username", "TEST_DB_USERNAME", "postgres"),
                settingOrDefault("test.db.password", "TEST_DB_PASSWORD", ""),
                settingOrDefault("test.db.driver", "TEST_DB_DRIVER", DEFAULT_DRIVER)
            );
        }

        private static DatabaseRuntime startTestcontainers() {
            PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("unified_order_test")
                .withUsername("test")
                .withPassword("test");
            postgres.start();
            registerShutdown(postgres, "Testcontainers PostgreSQL");
            LOGGER.info("Using Testcontainers PostgreSQL for tests");
            return new DatabaseRuntime(
                "testcontainers",
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword(),
                postgres.getDriverClassName()
            );
        }

        private static DatabaseRuntime startEmbedded() {
            try {
                EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                    .setPort(0)
                    .start();
                registerShutdown(postgres, "embedded PostgreSQL");
                LOGGER.info("Using embedded PostgreSQL for tests");
                return new DatabaseRuntime(
                    "embedded",
                    postgres.getJdbcUrl("postgres", "postgres"),
                    "postgres",
                    "postgres",
                    DEFAULT_DRIVER
                );
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to start embedded PostgreSQL for tests", exception);
            }
        }

        private static boolean dockerAvailable() {
            try {
                return DockerClientFactory.instance().isDockerAvailable();
            } catch (RuntimeException exception) {
                LOGGER.warn("Docker availability check failed", exception);
                return false;
            }
        }

        private static String setting(String propertyKey, String envKey) {
            String propertyValue = System.getProperty(propertyKey);
            if (propertyValue != null && !propertyValue.isBlank()) {
                return propertyValue;
            }
            return System.getenv(envKey);
        }

        private static String settingOrDefault(String propertyKey, String envKey, String fallback) {
            String value = setting(propertyKey, envKey);
            return value == null || value.isBlank() ? fallback : value;
        }

        private static void registerShutdown(AutoCloseable resource, String resourceName) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    resource.close();
                } catch (Exception exception) {
                    LOGGER.debug("Failed to close {}", resourceName, exception);
                }
            }));
        }
    }
}
