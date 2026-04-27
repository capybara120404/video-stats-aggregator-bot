package video.stats.aggregator.bot.domain.entity;

public enum VideoStatus {
    OK("✅ Ок"),
    UNAVAILABLE("⚠️ Недоступно"),
    NO_API_KEY("🔑 Нет API-ключа"),
    NOT_FOUND("🚫 Не найдено"),
    PENDING("⏳ Ожидание"),
    ERROR("❌ Ошибка");

    private final String label;

    VideoStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static VideoStatus fromString(String s) {
        if (s == null) {
            return PENDING;
        }
        try {
            return VideoStatus.valueOf(s);
        } catch (IllegalArgumentException e) {
            return ERROR;
        }
    }
}
