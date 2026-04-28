package video.stats.aggregator.bot.infrastructure.util;

import video.stats.aggregator.bot.domain.entity.Platform;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlatformDetector {

    private static final Map<Platform, Pattern[]> PLATFORM_PATTERNS = Map.of(
            Platform.YOUTUBE, new Pattern[] {
                    Pattern.compile("[?&]v=([A-Za-z0-9_-]{11})"),
                    Pattern.compile("youtu\\.be/([A-Za-z0-9_-]{11})"),
                    Pattern.compile("/shorts/([A-Za-z0-9_-]{11})"),
                    Pattern.compile("/embed/([A-Za-z0-9_-]{11})")
            },
            Platform.RUTUBE, new Pattern[] {
                    Pattern.compile("/video/([a-f0-9]{32})"),
                    Pattern.compile("/play/embed/([a-f0-9]{32})")
            });

    private PlatformDetector() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Result detect(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return Result.unknown(rawUrl);
        }

        String url = normalizeUrl(rawUrl.trim());
        String host = extractHost(url);

        if (host == null) {
            return Result.unknown(url);
        }

        return detectPlatform(host, url);
    }

    private static String normalizeUrl(String url) {
        return (!url.startsWith("http://") && !url.startsWith("https://"))
                ? "https://" + url
                : url;
    }

    private static String extractHost(String url) {
        try {
            return Optional.ofNullable(new URI(url).getHost())
                    .map(String::toLowerCase)
                    .orElse(null);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static Result detectPlatform(String host, String url) {
        for (Map.Entry<Platform, Pattern[]> entry : PLATFORM_PATTERNS.entrySet()) {
            if (isHostMatch(host, entry.getKey())) {
                String videoId = findVideoId(url, entry.getValue());
                if (videoId != null) {
                    return new Result(entry.getKey(), videoId, url);
                }
            }
        }
        return Result.unknown(url);
    }

    private static boolean isHostMatch(String host, Platform platform) {
        return switch (platform) {
            case YOUTUBE -> host.contains("youtube.com") || host.contains("youtu.be");
            case RUTUBE -> host.contains("rutube.ru");
            default -> false;
        };
    }

    private static String findVideoId(String url, Pattern[] patterns) {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    public static final class Result {
        private final Platform platform;
        private final String videoId;
        private final String normalizedUrl;

        public Result(Platform platform, String videoId, String normalizedUrl) {
            this.platform = platform;
            this.videoId = videoId;
            this.normalizedUrl = normalizedUrl;
        }

        public static Result unknown(String url) {
            return new Result(Platform.UNKNOWN, null, url != null ? url : "");
        }

        public Platform getPlatform() {
            return platform;
        }

        public String getVideoId() {
            return videoId;
        }

        public String getNormalizedUrl() {
            return normalizedUrl;
        }

        public boolean isSupported() {
            return platform != Platform.UNKNOWN && videoId != null;
        }
    }
}
