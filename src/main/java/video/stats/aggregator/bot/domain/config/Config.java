package video.stats.aggregator.bot.domain.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {
    private static final Logger LOG = LoggerFactory.getLogger(Config.class);
    private static final String DEFAULT_BOT_USERNAME = "VideoStatsAggregatorBot";
    private static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/video_stats_aggregator_bot_db";
    private static final String DEFAULT_DB_USER = "postgres";
    private static final String DEFAULT_DB_PASSWORD = "postgres";
    private static final String DEFAULT_TIMEOUT = "10";

    private final String botToken;
    private final String botUsername;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final String youtubeApiKey;
    private final int httpTimeoutSeconds;

    private Config(Builder builder) {
        this.botToken = builder.botToken;
        this.botUsername = builder.botUsername;
        this.dbUrl = builder.dbUrl;
        this.dbUser = builder.dbUser;
        this.dbPassword = builder.dbPassword;
        this.youtubeApiKey = builder.youtubeApiKey;
        this.httpTimeoutSeconds = builder.httpTimeoutSeconds;
    }

    public static Config load() {
        String botToken = requireEnv("BOT_TOKEN");
        String botUsername = getEnv("BOT_USERNAME", DEFAULT_BOT_USERNAME);
        String dbUrl = getEnv("DB_URL", DEFAULT_DB_URL);
        String dbUser = getEnv("DB_USER", DEFAULT_DB_USER);
        String dbPassword = getEnv("DB_PASSWORD", DEFAULT_DB_PASSWORD);
        String ytKey = getEnv("YOUTUBE_API_KEY", "");
        int timeout = Integer.parseInt(getEnv("HTTP_TIMEOUT_SECONDS", DEFAULT_TIMEOUT));

        if (ytKey.isEmpty()) {
            LOG.warn("YOUTUBE_API_KEY is not set — YouTube view fetching will be unavailable!");
        }

        LOG.info("Config loaded: bot={}, db={}", botUsername, dbUrl);

        return new Builder()
                .botToken(botToken)
                .botUsername(botUsername)
                .dbUrl(dbUrl)
                .dbUser(dbUser)
                .dbPassword(dbPassword)
                .youtubeApiKey(ytKey)
                .httpTimeoutSeconds(timeout)
                .build();
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required environment variable not set: " + name);
        }
        return value.trim();
    }

    private static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return defaultValue;
    }

    public String getBotToken() {
        return botToken;
    }

    public String getBotUsername() {
        return botUsername;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public String getYoutubeApiKey() {
        return youtubeApiKey;
    }

    public int getHttpTimeoutSeconds() {
        return httpTimeoutSeconds;
    }

    public static class Builder {
        private String botToken;
        private String botUsername;
        private String dbUrl;
        private String dbUser;
        private String dbPassword;
        private String youtubeApiKey;
        private int httpTimeoutSeconds;

        public Builder botToken(String botToken) {
            this.botToken = botToken;
            return this;
        }

        public Builder botUsername(String botUsername) {
            this.botUsername = botUsername;
            return this;
        }

        public Builder dbUrl(String dbUrl) {
            this.dbUrl = dbUrl;
            return this;
        }

        public Builder dbUser(String dbUser) {
            this.dbUser = dbUser;
            return this;
        }

        public Builder dbPassword(String dbPassword) {
            this.dbPassword = dbPassword;
            return this;
        }

        public Builder youtubeApiKey(String youtubeApiKey) {
            this.youtubeApiKey = youtubeApiKey;
            return this;
        }

        public Builder httpTimeoutSeconds(int httpTimeoutSeconds) {
            this.httpTimeoutSeconds = httpTimeoutSeconds;
            return this;
        }

        public Config build() {
            return new Config(this);
        }
    }
}
