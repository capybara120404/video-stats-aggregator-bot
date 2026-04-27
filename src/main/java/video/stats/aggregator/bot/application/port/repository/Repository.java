package video.stats.aggregator.bot.application.port.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import video.stats.aggregator.bot.domain.entity.Video;
import video.stats.aggregator.bot.domain.entity.VideoStatus;

public interface Repository {
    Video save(Video video) throws SQLException;

    List<Video> findAll() throws SQLException;

    Optional<Video> findById(long id) throws SQLException;

    Optional<Video> findByUrl(String url) throws SQLException;

    long countAll() throws SQLException;

    long sumViews() throws SQLException;

    void updateStats(long id, long views, String title, VideoStatus status, String errorMsg) throws SQLException;

    boolean deleteById(long id) throws SQLException;
}