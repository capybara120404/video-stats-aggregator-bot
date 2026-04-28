package video.stats.aggregator.bot.infrastructure.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import video.stats.aggregator.bot.domain.config.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class DatabaseContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseContext.class);
    private static final int CONNECTION_POOL_SIZE = 3;
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final long RETRY_DELAY_MILLISECONDS = 3_000L;
    private static final String DATABASE_SCHEMA_SQL = """
            CREATE TABLE IF NOT EXISTS videos (
                id           BIGSERIAL PRIMARY KEY,
                url          TEXT        NOT NULL UNIQUE,
                platform     VARCHAR(50) NOT NULL,
                video_id     VARCHAR(500),
                title        TEXT,
                views        BIGINT      NOT NULL DEFAULT 0,
                status       VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                error_msg    TEXT,
                last_updated TIMESTAMPTZ,
                added_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
            );

            CREATE INDEX IF NOT EXISTS idx_videos_platform ON videos (platform);
            CREATE INDEX IF NOT EXISTS idx_videos_added_at ON videos (added_at DESC);
            """;

    private final Config configuration;
    private final List<Connection> connectionPool;

    public DatabaseContext(Config configuration) {
        this.configuration = configuration;
        this.connectionPool = new ArrayList<>(CONNECTION_POOL_SIZE);
    }

    public void initialize() {
        LOGGER.info("Initialising database at {}", configuration.getDbUrl());
        establishConnectionPool();
        applyDatabaseSchema();
        LOGGER.info("Database initialised successfully.");
    }

    public synchronized Connection getConnection() throws SQLException {
        for (int index = 0; index < connectionPool.size();) {
            Connection existingConnection = connectionPool.get(index);
            if (isConnectionValid(existingConnection)) {
                return existingConnection;
            }
            tryReconnectSlot(index);

            return connectionPool.get(index);
        }

        return createAndStoreNewConnection();
    }

    private void establishConnectionPool() {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                clearAndPopulatePool();
                LOGGER.info("Connection pool created ({} connections).", CONNECTION_POOL_SIZE);
                return;
            } catch (SQLException exception) {
                handleConnectionFailure(attempt, exception);
            }
        }
    }

    private void clearAndPopulatePool() throws SQLException {
        connectionPool.clear();
        for (int i = 0; i < CONNECTION_POOL_SIZE; i++) {
            connectionPool.add(createNewConnection());
        }
    }

    private void handleConnectionFailure(int currentAttempt, SQLException exception) {
        LOGGER.warn("DB connection attempt {}/{} failed: {}", currentAttempt, MAX_RETRY_ATTEMPTS,
                exception.getMessage());
        if (currentAttempt == MAX_RETRY_ATTEMPTS) {
            throw new RuntimeException("Cannot connect to database after " + MAX_RETRY_ATTEMPTS + " attempts",
                    exception);
        }
        pauseExecution(RETRY_DELAY_MILLISECONDS);
    }

    private boolean isConnectionValid(Connection connection) throws SQLException {
        return connection != null && !connection.isClosed() && connection.isValid(2);
    }

    private void tryReconnectSlot(int slotIndex) {
        try {
            Connection newConnection = createNewConnection();
            connectionPool.set(slotIndex, newConnection);
            LOGGER.debug("Reconnected pool slot {}.", slotIndex);
        } catch (SQLException exception) {
            LOGGER.warn("Pool slot {} reconnection failed, trying next.", slotIndex);
        }
    }

    private Connection createAndStoreNewConnection() throws SQLException {
        Connection freshConnection = createNewConnection();
        if (!connectionPool.isEmpty()) {
            connectionPool.set(0, freshConnection);
        } else {
            connectionPool.add(freshConnection);
        }
        return freshConnection;
    }

    private Connection createNewConnection() throws SQLException {
        return DriverManager.getConnection(
                configuration.getDbUrl(),
                configuration.getDbUser(),
                configuration.getDbPassword());
    }

    private void applyDatabaseSchema() {
        try (Statement statement = getConnection().createStatement()) {
            statement.execute(DATABASE_SCHEMA_SQL);
            LOGGER.debug("Schema applied.");
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to apply database schema", exception);
        }
    }

    private static void pauseExecution(long durationMilliseconds) {
        try {
            Thread.sleep(durationMilliseconds);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}
