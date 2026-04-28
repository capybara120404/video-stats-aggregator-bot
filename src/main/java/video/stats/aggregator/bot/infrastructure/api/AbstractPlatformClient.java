package video.stats.aggregator.bot.infrastructure.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import video.stats.aggregator.bot.application.port.client.PlatformClient;
import video.stats.aggregator.bot.application.port.client.PlatformException;
import video.stats.aggregator.bot.application.port.client.UnavailableException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public abstract class AbstractPlatformClient implements PlatformClient {
    protected final HttpClient httpClient;
    protected final ObjectMapper objectMapper;

    protected AbstractPlatformClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    protected HttpResponse<String> send(HttpRequest request) throws PlatformException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new UnavailableException("API unreachable: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UnavailableException("Request interrupted", e);
        }
    }
}
