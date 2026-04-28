package video.stats.aggregator.bot.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import video.stats.aggregator.bot.application.port.client.ApiException;
import video.stats.aggregator.bot.application.port.client.NotFoundException;
import video.stats.aggregator.bot.application.port.client.PlatformClient;
import video.stats.aggregator.bot.application.port.client.PlatformClient.VideoStats;
import video.stats.aggregator.bot.application.port.client.PlatformException;
import video.stats.aggregator.bot.application.port.client.UnavailableException;
import video.stats.aggregator.bot.application.port.repository.Repository;
import video.stats.aggregator.bot.domain.entity.Platform;
import video.stats.aggregator.bot.domain.entity.Video;
import video.stats.aggregator.bot.domain.entity.VideoStatus;
import video.stats.aggregator.bot.infrastructure.util.PlatformDetector;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VideoService {
    private static final Logger LOG = LoggerFactory.getLogger(VideoService.class);

    private final Repository repository;
    private final Map<Platform, PlatformClient> clients;

    public VideoService(Repository repository, List<PlatformClient> clientList) {
        this.repository = repository;
        this.clients = clientList.stream()
                .collect(Collectors.toMap(PlatformClient::getPlatform, Function.identity()));
    }

    public Video addVideo(String rawUrl) throws SQLException {
        PlatformDetector.Result detection = PlatformDetector.detect(rawUrl);

        if (!detection.isSupported()) {
            throw new IllegalArgumentException("Unsupported platform or invalid URL: " + rawUrl);
        }

        String url = detection.getNormalizedUrl();
        Optional<Video> existing = repository.findByUrl(url);

        if (existing.isPresent()) {
            Video video = existing.get();
            LOG.info("URL already tracked, refreshing: {}", url);
            updateVideoStats(video);
            video.setNewlyCreated(false);
            return video;
        }

        // 1. Создаем объект
        Video video = new Video();
        video.setUrl(url);
        video.setPlatform(detection.getPlatform());
        video.setVideoId(detection.getVideoId());
        video.setStatus(VideoStatus.PENDING);

        // 2. Сначала сохраняем в БД, чтобы репозиторий проставил ID
        Video savedVideo = repository.save(video);

        // 3. Теперь обновляем статистику, используя ID из savedVideo
        updateVideoStats(savedVideo);

        savedVideo.setNewlyCreated(true);
        return savedVideo;
    }

    public Video refreshVideo(long id) throws SQLException {
        Video video = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Video not found with id: " + id));

        updateVideoStats(video);
        return video;
    }

    public List<Video> refreshAll() throws SQLException {
        List<Video> all = repository.findAll();
        for (Video video : all) {
            updateVideoStats(video);
        }
        return all;
    }

    public List<Video> getAllVideos() throws SQLException {
        return repository.findAll();
    }

    public long getTotalCount() throws SQLException {
        return repository.countAll();
    }

    public long getTotalViews() throws SQLException {
        return repository.sumViews();
    }

    public boolean deleteVideo(long id) throws SQLException {
        return repository.deleteById(id);
    }

    private void updateVideoStats(Video video) throws SQLException {
        fetchAndApply(video);

        if (video.getId() == null) {
            LOG.error("Attempted to update stats for video without ID: {}", video.getUrl());
            return;
        }

        repository.updateStats(video.getId(), video.getViews(), video.getTitle(),
                video.getStatus(), video.getErrorMessage());
    }

    private void fetchAndApply(Video video) {
        PlatformClient client = clients.get(video.getPlatform());

        if (client == null) {
            video.setStatus(VideoStatus.ERROR);
            video.setErrorMessage("Client not registered for platform: " + video.getPlatform());
            return;
        }

        try {
            VideoStats stats = client.fetchStats(video.getVideoId());
            video.setViews(stats.viewCount());
            video.setTitle(stats.title());
            video.setStatus(VideoStatus.OK);
            video.setErrorMessage(null);
            video.setLastUpdated(LocalDateTime.now());
            LOG.info("Fetched stats for {} id={}: views={}", video.getPlatform(), video.getVideoId(),
                    stats.viewCount());

        } catch (UnavailableException e) {
            handleError(video, VideoStatus.UNAVAILABLE, e.getMessage());
        } catch (NotFoundException e) {
            handleError(video, VideoStatus.NOT_FOUND, e.getMessage());
        } catch (ApiException e) {
            VideoStatus status = e.getMessage().contains("API key") ? VideoStatus.NO_API_KEY : VideoStatus.ERROR;
            handleError(video, status, e.getMessage());
        } catch (PlatformException e) {
            handleError(video, VideoStatus.ERROR, e.getMessage());
        }
    }

    private void handleError(Video video, VideoStatus status, String message) {
        LOG.warn("Error for {} id={}: {}", video.getPlatform(), video.getVideoId(), message);
        video.setStatus(status);
        video.setErrorMessage(message);
    }
}
