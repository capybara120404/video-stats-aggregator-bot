package video.stats.aggregator.bot.infrastructure.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import video.stats.aggregator.bot.application.port.client.PlatformClient;
import video.stats.aggregator.bot.domain.config.Config;
import video.stats.aggregator.bot.domain.entity.Platform;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class YouTubeClient implements PlatformClient {
    private static final Logger log = LoggerFactory.getLogger(YouTubeClient.class);
    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3/videos";

    private final String apiKey;
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();

    public YouTubeClient(Config config) {
        this.apiKey = config.getYoutubeApiKey();
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getHttpTimeoutSeconds()))
                .build();
    }

    @Override
    public Platform getPlatform() {
        return Platform.YOUTUBE;
    }

    @Override
    public VideoStats fetchStats(String videoId) throws PlatformException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException("YouTube API key not configured (YOUTUBE_API_KEY)");
        }

        String url = BASE_URL
                + "?part=statistics,snippet"
                + "&id=" + URLEncoder.encode(videoId, StandardCharsets.UTF_8)
                + "&key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

        log.debug("YouTube API request for videoId={}", videoId);

        HttpResponse<String> response;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Accept", "application/json")
                    .build();
            response = http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new UnavailableException("YouTube API unreachable: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UnavailableException("Request interrupted", e);
        }

        int status = response.statusCode();
        String body = response.body();

        if (status == 403) {
            throw new ApiException("YouTube API quota exceeded or key invalid (HTTP 403)");
        }
        if (status != 200) {
            throw new ApiException("YouTube API returned HTTP " + status);
        }

        return parseResponse(videoId, body);
    }

    private VideoStats parseResponse(String videoId, String body) throws PlatformException {
        try {
            JsonNode root = json.readTree(body);
            JsonNode items = root.path("items");

            if (!items.isArray() || items.isEmpty()) {
                throw new NotFoundException("Video not found or is private: " + videoId);
            }

            JsonNode item = items.get(0);
            JsonNode snippet = item.path("snippet");
            JsonNode stats = item.path("statistics");

            String title = snippet.path("title").asText("Unknown Title");
            long viewCount = stats.path("viewCount").asLong(0L);

            log.debug("YouTube videoId={} title='{}' views={}", videoId, title, viewCount);
            return new VideoStats(title, viewCount);

        } catch (IOException e) {
            throw new ApiException("Failed to parse YouTube API response: " + e.getMessage());
        }
    }
}
