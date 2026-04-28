package video.stats.aggregator.bot.infrastructure.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import video.stats.aggregator.bot.application.port.client.ApiException;
import video.stats.aggregator.bot.application.port.client.NotFoundException;
import video.stats.aggregator.bot.application.port.client.PlatformException;
import video.stats.aggregator.bot.application.port.client.UnavailableException;
import video.stats.aggregator.bot.domain.entity.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RuTubeClient extends AbstractPlatformClient {
    private static final String BASE_URL = "https://rutube.ru/api/video/";

    public RuTubeClient(HttpClient httpClient, ObjectMapper objectMapper) {
        super(httpClient, objectMapper);
    }

    @Override
    public Platform getPlatform() {
        return Platform.RUTUBE;
    }

    @Override
    public VideoStats fetchStats(String videoId) throws PlatformException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + videoId + "/"))
                .header("Accept", "application/json")
                .header("User-Agent", "VideoStatsAggregatorBot/1.0")
                .GET()
                .build();

        HttpResponse<String> response = send(req);
        int status = response.statusCode();

        if (status == 404) {
            throw new NotFoundException("Video not found: " + videoId);
        }
        if (status == 403 || status == 429) {
            throw new UnavailableException("Rate limited", null);
        }
        if (status != 200) {
            throw new ApiException("API returned " + status);
        }

        try {
            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("detail")) {
                throw new NotFoundException(root.path("detail").asText());
            }
            return new VideoStats(root.path("title").asText("Unknown"), root.path("hits").asLong(0L));
        } catch (Exception e) {
            throw new ApiException("Parse error: " + e.getMessage());
        }
    }
}
