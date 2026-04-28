package video.stats.aggregator.bot.application.port.client;

import video.stats.aggregator.bot.domain.entity.Platform;

public interface PlatformClient {
    Platform getPlatform();

    VideoStats fetchStats(String videoId) throws PlatformException;

    record VideoStats(String title, long viewCount) {
    }
}
