package video.stats.aggregator.bot.model;

public enum Platform {
    YOUTUBE("YouTube", "🎬"),
    RUTUBE("RuTube", "📹"),
    UNKNOWN("Unknown", "❓");

    private final String displayName;
    private final String emoji;

    Platform(String displayName, String emoji) {
        this.displayName = displayName;
        this.emoji = emoji;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmoji() {
        return emoji;
    }

    public String format() {
        return emoji + " " + displayName;
    }
}
