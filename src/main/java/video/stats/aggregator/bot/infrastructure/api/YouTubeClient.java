package video.stats.aggregator.bot.infrastructure.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import video.stats.aggregator.bot.application.port.client.ApiException;
import video.stats.aggregator.bot.application.port.client.NotFoundException;
import video.stats.aggregator.bot.application.port.client.PlatformException;
import video.stats.aggregator.bot.domain.entity.Platform;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class YouTubeClient extends AbstractPlatformClient {
    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3/videos";
    private final String API_KEY;

    public YouTubeClient(HttpClient httpClient, ObjectMapper objectMapper, String apiKey) {
        super(httpClient, objectMapper);
        this.API_KEY = apiKey;
    }

    @Override
    public Platform getPlatform() {
        return Platform.YOUTUBE;
    }

    @Override
    public VideoStats fetchStats(String videoId) throws PlatformException {
        if (API_KEY == null || API_KEY.isBlank()) {
            throw new ApiException("API key missing");
        }

        String url = BASE_URL + "?part=statistics,snippet&id=" + URLEncoder.encode(videoId, StandardCharsets.UTF_8)
                + "&key=" + URLEncoder.encode(API_KEY, StandardCharsets.UTF_8);

        HttpResponse<String> response = send(HttpRequest.newBuilder().uri(URI.create(url)).GET().build());

        if (response.statusCode() != 200) {
            throw new ApiException("API returned " + response.statusCode());
        }

        try {
            JsonNode items = objectMapper.readTree(response.body()).path("items");
            if (items.isEmpty()) {
                throw new NotFoundException("Video not found: " + videoId);
            }
            JsonNode item = items.get(0);
            return new VideoStats(item.path("snippet").path("title").asText(),
                    item.path("statistics").path("viewCount").asLong(0L));
        } catch (Exception e) {
            throw new ApiException("Parse error: " + e.getMessage());
        }
    }
}
