package video.stats.aggregator.bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import video.stats.aggregator.bot.model.Platform;
import video.stats.aggregator.bot.model.Video;
import video.stats.aggregator.bot.model.VideoStatus;
import video.stats.aggregator.bot.repository.VideoRepository;
import video.stats.aggregator.bot.service.platform.PlatformClient;
import video.stats.aggregator.bot.service.platform.PlatformClient.ApiException;
import video.stats.aggregator.bot.service.platform.PlatformClient.NotFoundException;
import video.stats.aggregator.bot.service.platform.PlatformClient.PlatformException;
import video.stats.aggregator.bot.service.platform.PlatformClient.UnavailableException;
import video.stats.aggregator.bot.service.platform.PlatformClient.VideoStats;
import video.stats.aggregator.bot.util.PlatformDetector;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VideoService {

    private static final Logger log = LoggerFactory.getLogger(VideoService.class);

    private final VideoRepository            repo;
    private final Map<Platform, PlatformClient> clients;

    public VideoService(VideoRepository repo, List<PlatformClient> clientList) {
        this.repo    = repo;
        this.clients = clientList.stream()
                .collect(Collectors.toMap(PlatformClient::getPlatform, Function.identity()));
    }


    public Video addVideo(String rawUrl) throws SQLException {
        PlatformDetector.Result detection = PlatformDetector.detect(rawUrl);

        if (!detection.isSupported()) {
            throw new IllegalArgumentException(
                    "Неподдерживаемая платформа или некорректная ссылка: " + rawUrl);
        }

        String   url      = detection.getNormalizedUrl();
        Platform platform = detection.getPlatform();
        String   videoId  = detection.getVideoId();

        Optional<Video> existing = repo.findByUrl(url);
        if (existing.isPresent()) {
            log.info("URL already tracked: {}", url);
            return existing.get();
        }

        Video video = new Video();
        video.setUrl(url);
        video.setPlatform(platform);
        video.setVideoId(videoId);
        video.setStatus(VideoStatus.PENDING);

        fetchAndApply(video);

        return repo.save(video);
    }

    public Video refreshVideo(long id) throws SQLException {
        Video video = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Видео с id=" + id + " не найдено"));

        fetchAndApply(video);
        repo.updateStats(video.getId(), video.getViews(), video.getTitle(),
                video.getStatus(), video.getErrorMessage());
        return video;
    }

    public List<Video> refreshAll() throws SQLException {
        List<Video> all = repo.findAll();
        for (Video v : all) {
            fetchAndApply(v);
            repo.updateStats(v.getId(), v.getViews(), v.getTitle(),
                    v.getStatus(), v.getErrorMessage());
        }
        return all;
    }


    public List<Video> getAllVideos() throws SQLException {
        return repo.findAll();
    }

    public long getTotalCount() throws SQLException {
        return repo.countAll();
    }

    public long getTotalViews() throws SQLException {
        return repo.sumViews();
    }

    public boolean deleteVideo(long id) throws SQLException {
        return repo.deleteById(id);
    }

    private void fetchAndApply(Video video) {
        PlatformClient client = clients.get(video.getPlatform());

        if (client == null) {
            video.setStatus(VideoStatus.ERROR);
            video.setErrorMessage("Клиент для платформы " + video.getPlatform() + " не зарегистрирован");
            return;
        }

        try {
            VideoStats stats = client.fetchStats(video.getVideoId());
            video.setViews(stats.viewCount());
            video.setTitle(stats.title());
            video.setStatus(VideoStatus.OK);
            video.setErrorMessage(null);
            video.setLastUpdated(LocalDateTime.now());
            log.info("Fetched stats for {} id={}: views={}", video.getPlatform(), video.getVideoId(), stats.viewCount());

        } catch (UnavailableException e) {
            log.warn("Platform {} unavailable for videoId={}: {}", video.getPlatform(), video.getVideoId(), e.getMessage());
            video.setStatus(VideoStatus.UNAVAILABLE);
            video.setErrorMessage(e.getMessage());

        } catch (NotFoundException e) {
            log.warn("Video not found: platform={} id={}", video.getPlatform(), video.getVideoId());
            video.setStatus(VideoStatus.NOT_FOUND);
            video.setErrorMessage(e.getMessage());

        } catch (ApiException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("API key")) {
                video.setStatus(VideoStatus.NO_API_KEY);
            } else {
                video.setStatus(VideoStatus.ERROR);
            }
            video.setErrorMessage(msg);
            log.warn("API error for {} id={}: {}", video.getPlatform(), video.getVideoId(), msg);

        } catch (PlatformException e) {
            log.error("Unexpected platform error", e);
            video.setStatus(VideoStatus.ERROR);
            video.setErrorMessage(e.getMessage());
        }
    }
}
