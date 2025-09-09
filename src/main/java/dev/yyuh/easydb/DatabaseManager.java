package dev.yyuh.easydb;

import com.zaxxer.hikari.HikariDataSource;
import dev.yyuh.easydb.scheme.Example;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class DatabaseManager {

    private static DatabaseManager manager;
    private boolean isConnected = false;
    private static HikariDataSource dataSource;

    private final String driverClass;

    private final Map<Class<?>, DatabaseProvider<?>> providers = new HashMap<>();

    public DatabaseManager() {
        this.driverClass = "com.mysql.cj.jdbc.Driver";
        manager = this;
        connect();
    }

    public void connect() {
        dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClass);
        dataSource.setJdbcUrl("JDBC URL"); // TODO: Edit

        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(2);
        dataSource.setMaxLifetime(1800000);
        dataSource.setConnectionTimeout(30000);
        dataSource.setLeakDetectionThreshold(60000);
        dataSource.setPoolName("EXAMPLE"); // TODO: Edit

        final Properties properties = new Properties();
        properties.putAll(
                Map.of("cachePrepStmts", "true",
                        "prepStmtCacheSize", "250",
                        "prepStmtCacheSqlLimit", "2048",
                        "useServerPrepStmts", "true",
                        "useLocalSessionState", "true",
                        "useLocalTransactionState", "true"
                ));
        properties.putAll(
                Map.of(
                        "rewriteBatchedStatements", "true",
                        "cacheResultSetMetadata", "true",
                        "cacheServerConfiguration", "true",
                        "elideSetAutoCommits", "true",
                        "maintainTimeStats", "false")
        );
        dataSource.setDataSourceProperties(properties);

        try (final Connection conn =  dataSource.getConnection()) {
            this.isConnected  = true;
            register();
        } catch (SQLException e) {
            this.isConnected = false;
            throw new RuntimeException(e);
        }

    }

    public <T>CompletableFuture<Optional<T>> get(Class<T> clazz, UUID uuid) {
        if(!isConnected) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return CompletableFuture.supplyAsync(() -> (Optional<T>) providers.get(clazz).get(uuid));
    }

    public <T>CompletableFuture<Void> save(Class<T> clazz, T t) {
        if(!isConnected) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync(() -> {
            ((DatabaseProvider<T>) providers.get(clazz)).save(t);
            return null;
        });
    }

    private void register() {
        providers.put(Example.class, new ExampleDatabase(dataSource));

        for (final DatabaseProvider<?> provider : providers.values()) {
            provider.start();
        }
    }

    public static DatabaseManager getInstance() {
        if (manager == null) {
            new DatabaseManager();
            return manager;
        }
        return manager;
    }
}