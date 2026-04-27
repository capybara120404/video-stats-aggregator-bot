package video.stats.aggregator.bot.service.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import video.stats.aggregator.bot.config.AppConfig;
import video.stats.aggregator.bot.model.Platform;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class RuTubeClient implements PlatformClient {

    private static final Logger log = LoggerFactory.getLogger(RuTubeClient.class);
    private static final String BASE_URL = "https://rutube.ru/api/video/";

    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();

    public RuTubeClient(AppConfig config) {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getHttpTimeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public Platform getPlatform() {
        return Platform.RUTUBE;
    }

    @Override
    public VideoStats fetchStats(String videoId) throws PlatformException {
        String url = BASE_URL + videoId + "/";
        log.debug("RuTube API request for videoId={}", videoId);

        HttpResponse<String> response;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (compatible; VideoStatsAggregatorBot/1.0)")
                    .build();
            response = http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new UnavailableException("RuTube API unreachable: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UnavailableException("Request interrupted", e);
        }

        int status = response.statusCode();
        String body = response.body();

        if (status == 404) {
            throw new NotFoundException("Video not found on RuTube: " + videoId);
        }
        if (status == 403 || status == 429) {
            throw new UnavailableException("RuTube returned HTTP " + status + " (rate-limited / blocked)",
                    new IOException("HTTP " + status));
        }
        if (status != 200) {
            throw new ApiException("RuTube API returned HTTP " + status);
        }

        return parseResponse(videoId, body);
    }

    private VideoStats parseResponse(String videoId, String body) throws PlatformException {
        try {
            JsonNode root = json.readTree(body);

            if (root.has("detail")) {
                String detail = root.path("detail").asText();
                throw new NotFoundException("RuTube API error for " + videoId + ": " + detail);
            }

            String title = root.path("title").asText("Unknown Title");
            long views = root.has("hits") ? root.path("hits").asLong(0L)
                    : root.path("views").asLong(0L);

            log.debug("RuTube videoId={} title='{}' views={}", videoId, title, views);
            return new VideoStats(title, views);

        } catch (IOException e) {
            throw new ApiException("Failed to parse RuTube API response: " + e.getMessage());
        }
    }
}
