package video.stats.aggregator.bot.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import video.stats.aggregator.bot.model.Platform;

public class PlatformDetector {
    private static final Pattern YT_WATCH = Pattern.compile("[?&]v=([A-Za-z0-9_-]{11})");
    private static final Pattern YT_SHORT = Pattern.compile("youtu\\.be/([A-Za-z0-9_-]{11})");
    private static final Pattern YT_SHORTS = Pattern.compile("/shorts/([A-Za-z0-9_-]{11})");
    private static final Pattern YT_EMBED = Pattern.compile("/embed/([A-Za-z0-9_-]{11})");

    private static final Pattern RUTUBE_VIDEO = Pattern.compile("/video/([a-f0-9]{32})");
    private static final Pattern RUTUBE_EMBED = Pattern.compile("/play/embed/([a-f0-9]{32})");

    private PlatformDetector() {
    }

    public static Result detect(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank())
            return Result.unknown(rawUrl);

        String url = rawUrl.trim();

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        String host = extractHost(url);
        if (host == null)
            return Result.unknown(rawUrl);

        if (host.contains("youtube.com") || host.contains("youtu.be")) {
            String id = matchFirst(url, YT_WATCH, YT_SHORT, YT_SHORTS, YT_EMBED);
            if (id != null)
                return new Result(Platform.YOUTUBE, id, url);
        }

        if (host.contains("rutube.ru")) {
            String id = matchFirst(url, RUTUBE_VIDEO, RUTUBE_EMBED);
            if (id != null)
                return new Result(Platform.RUTUBE, id, url);
        }

        return Result.unknown(url);
    }

    private static String extractHost(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            return host != null ? host.toLowerCase() : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @SafeVarargs
    private static String matchFirst(String url, Pattern... patterns) {
        for (Pattern p : patterns) {
            Matcher m = p.matcher(url);
            if (m.find())
                return m.group(1);
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

        static Result unknown(String url) {
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
