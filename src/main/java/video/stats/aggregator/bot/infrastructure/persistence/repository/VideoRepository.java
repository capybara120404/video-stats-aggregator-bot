package video.stats.aggregator.bot.infrastructure.persistence.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import video.stats.aggregator.bot.application.port.repository.Repository;
import video.stats.aggregator.bot.domain.entity.Platform;
import video.stats.aggregator.bot.domain.entity.Video;
import video.stats.aggregator.bot.domain.entity.VideoStatus;
import video.stats.aggregator.bot.infrastructure.persistence.DatabaseContext;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VideoRepository implements Repository {
    private static final Logger log = LoggerFactory.getLogger(VideoRepository.class);
    private final DatabaseContext context;

    public VideoRepository(DatabaseContext context) {
        this.context = context;
    }

    @Override
    public Video save(Video video) throws SQLException {
        String sql = """
                INSERT INTO videos (url, platform, video_id, title, views, status, error_msg, last_updated)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (url) DO NOTHING
                RETURNING id, added_at
                """;
        try (PreparedStatement preparedStatement = context.getConnection().prepareStatement(sql)) {
            preparedStatement.setString(1, video.getUrl());
            preparedStatement.setString(2, video.getPlatform().name());
            preparedStatement.setString(3, video.getVideoId());
            preparedStatement.setString(4, video.getTitle());
            preparedStatement.setLong(5, video.getViews());
            preparedStatement.setString(6, video.getStatus().name());
            preparedStatement.setString(7, video.getErrorMessage());
            setTimestamp(preparedStatement, 8, video.getLastUpdated());
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                video.setId(resultSet.getLong("id"));
                video.setAddedAt(toLocalDateTime(resultSet.getTimestamp("added_at")));
                log.debug("Saved new video id={}", video.getId());
                return video;
            }
        }
        return findByUrl(video.getUrl())
                .orElseThrow(() -> new SQLException("Conflict on save but URL not found: " + video.getUrl()));
    }

    @Override
    public List<Video> findAll() throws SQLException {
        String sql = "SELECT * FROM videos ORDER BY added_at ASC";
        List<Video> result = new ArrayList<>();
        try (PreparedStatement ps = context.getConnection().prepareStatement(sql);
                ResultSet resultSet = ps.executeQuery()) {
            while (resultSet.next()) {
                result.add(map(resultSet));
            }
        }
        return result;
    }

    @Override
    public Optional<Video> findById(long id) throws SQLException {
        String sql = "SELECT * FROM videos WHERE id = ?";
        try (PreparedStatement preparedStatement = context.getConnection().prepareStatement(sql)) {
            preparedStatement.setLong(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
        }
    }

    @Override
    public Optional<Video> findByUrl(String url) throws SQLException {
        String sql = "SELECT * FROM videos WHERE url = ?";
        try (PreparedStatement preparedStatement = context.getConnection().prepareStatement(sql)) {
            preparedStatement.setString(1, url);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
        }
    }

    @Override
    public long countAll() throws SQLException {
        try (Statement statement = context.getConnection().createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM videos")) {
            return resultSet.next() ? resultSet.getLong(1) : 0;
        }
    }

    @Override
    public long sumViews() throws SQLException {
        try (Statement statement = context.getConnection().createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COALESCE(SUM(views), 0) FROM videos")) {
            return resultSet.next() ? resultSet.getLong(1) : 0;
        }
    }

    @Override
    public void updateStats(long id, long views, String title, VideoStatus status, String errorMsg)
            throws SQLException {
        String sql = """
                UPDATE videos
                SET views = ?, title = COALESCE(?, title),
                    status = ?, error_msg = ?, last_updated = NOW()
                WHERE id = ?
                """;
        try (PreparedStatement preparedStatement = context.getConnection().prepareStatement(sql)) {
            preparedStatement.setLong(1, views);
            preparedStatement.setString(2, title);
            preparedStatement.setString(3, status.name());
            preparedStatement.setString(4, errorMsg);
            preparedStatement.setLong(5, id);
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public boolean deleteById(long id) throws SQLException {
        String sql = "DELETE FROM videos WHERE id = ? RETURNING id";
        try (PreparedStatement preparedStatement = context.getConnection().prepareStatement(sql)) {
            preparedStatement.setLong(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        }
    }

    private Video map(ResultSet resultSet) throws SQLException {
        Video video = new Video();
        video.setId(resultSet.getLong("id"));
        video.setUrl(resultSet.getString("url"));
        video.setPlatform(Platform.valueOf(resultSet.getString("platform")));
        video.setVideoId(resultSet.getString("video_id"));
        video.setTitle(resultSet.getString("title"));
        video.setViews(resultSet.getLong("views"));
        video.setStatus(VideoStatus.fromString(resultSet.getString("status")));
        video.setErrorMessage(resultSet.getString("error_msg"));
        video.setLastUpdated(toLocalDateTime(resultSet.getTimestamp("last_updated")));
        video.setAddedAt(toLocalDateTime(resultSet.getTimestamp("added_at")));
        return video;
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private static void setTimestamp(PreparedStatement preparedStatement, int parameterIndex, LocalDateTime timestamp)
            throws SQLException {
        preparedStatement.setTimestamp(parameterIndex, timestamp != null ? Timestamp.valueOf(timestamp) : null);
    }
}
