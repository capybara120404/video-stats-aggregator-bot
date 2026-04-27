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

public class DatabaseContext {
    private static final Logger log = LoggerFactory.getLogger(DatabaseContext.class);
    private static final int POOL_SIZE = 3;
    private static final int RETRY_COUNT = 5;
    private static final long RETRY_DELAY_MS = 3_000;

    private final Config config;
    private final List<Connection> pool = new ArrayList<>(POOL_SIZE);

    public DatabaseContext(Config config) {
        this.config = config;
    }

    public void initialize() {
        log.info("Initialising database at {}", config.getDbUrl());
        connectWithRetry();
        runSchema();
        log.info("Database initialised successfully.");
    }

    private void connectWithRetry() {
        for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
            try {
                pool.clear();
                for (int i = 0; i < POOL_SIZE; i++) {
                    pool.add(openConnection());
                }
                log.info("Connection pool created ({} connections).", POOL_SIZE);
                return;
            } catch (SQLException e) {
                log.warn("DB connection attempt {}/{} failed: {}", attempt, RETRY_COUNT, e.getMessage());
                if (attempt == RETRY_COUNT) {
                    throw new RuntimeException("Cannot connect to database after " + RETRY_COUNT + " attempts", e);
                }
                sleep(RETRY_DELAY_MS);
            }
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                config.getDbUrl(), config.getDbUser(), config.getDbPassword());
    }

    public synchronized Connection getConnection() throws SQLException {
        for (int i = 0; i < pool.size(); i++) {
            Connection c = pool.get(i);
            try {
                if (c == null || c.isClosed() || !c.isValid(2)) {
                    log.debug("Reconnecting pool slot {}.", i);
                    c = openConnection();
                    pool.set(i, c);
                }
                return c;
            } catch (SQLException ex) {
                log.warn("Pool slot {} reconnection failed, trying next.", i);
            }
        }
        Connection fresh = openConnection();
        pool.set(0, fresh);
        return fresh;
    }

    private static final String SCHEMA = """
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

    private void runSchema() {
        try (Statement st = getConnection().createStatement()) {
            st.execute(SCHEMA);
            log.debug("Schema applied.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to apply database schema", e);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
