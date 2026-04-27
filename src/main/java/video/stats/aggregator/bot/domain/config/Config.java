package video.stats.aggregator.bot.domain.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {
    private static final Logger log = LoggerFactory.getLogger(Config.class);

    private final String botToken;
    private final String botUsername;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final String youtubeApiKey;
    private final int httpTimeoutSeconds;

    private Config(String botToken, String botUsername,
            String dbUrl, String dbUser, String dbPassword,
            String youtubeApiKey, int httpTimeoutSeconds) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.youtubeApiKey = youtubeApiKey;
        this.httpTimeoutSeconds = httpTimeoutSeconds;
    }

    public static Config load() {
        String botToken = requireEnv("BOT_TOKEN");
        String botUsername = getEnv("BOT_USERNAME", "VideoStatsAggregatorBot");
        String dbUrl = getEnv("DB_URL", "jdbc:postgresql://localhost:5432/videobot");
        String dbUser = getEnv("DB_USER", "postgres");
        String dbPassword = getEnv("DB_PASSWORD", "postgres");
        String ytKey = getEnv("YOUTUBE_API_KEY", "VideoStatsAggregatorBot");
        int timeout = Integer.parseInt(getEnv("HTTP_TIMEOUT_SECONDS", "10"));

        if (ytKey.isEmpty()) {
            log.warn("YOUTUBE_API_KEY is not set — YouTube view fetching will be unavailable!");
        }

        log.info("Config loaded: bot={}, db={}", botUsername, dbUrl);
        return new Config(botToken, botUsername, dbUrl, dbUser, dbPassword, ytKey, timeout);
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
        return (value != null && !value.isBlank()) ? value.trim() : defaultValue;
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
}
