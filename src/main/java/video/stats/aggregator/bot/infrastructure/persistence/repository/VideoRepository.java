package video.stats.aggregator.bot.infrastructure.persistence.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import video.stats.aggregator.bot.application.port.repository.Repository;
import video.stats.aggregator.bot.domain.entity.Video;
import video.stats.aggregator.bot.domain.entity.VideoStatus;
import video.stats.aggregator.bot.infrastructure.persistence.DatabaseContext;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class VideoRepository implements Repository {
    private static final Logger LOGGER = LoggerFactory.getLogger(VideoRepository.class);
    private final DatabaseContext databaseContext;
    private final VideoResultSetMapper videoResultSetMapper;

    public VideoRepository(DatabaseContext databaseContext) {
        this.databaseContext = databaseContext;
        this.videoResultSetMapper = new VideoResultSetMapper();
    }

    @Override
    public Video save(Video video) throws SQLException {
        Video insertedVideo = attemptInsertion(video);
        if (insertedVideo != null) {
            return insertedVideo;
        }
        return resolveInsertionConflict(video.getUrl());
    }

    @Override
    public List<Video> findAll() throws SQLException {
        String selectAllQuery = "SELECT * FROM videos ORDER BY added_at ASC";
        List<Video> videos = new ArrayList<>();
        try (PreparedStatement preparedStatement = databaseContext.getConnection().prepareStatement(selectAllQuery);
                ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                videos.add(videoResultSetMapper.map(resultSet));
            }
        }
        return videos;
    }

    @Override
    public Optional<Video> findById(long id) throws SQLException {
        String selectByIdQuery = "SELECT * FROM videos WHERE id = ?";
        try (PreparedStatement preparedStatement = databaseContext.getConnection().prepareStatement(selectByIdQuery)) {
            preparedStatement.setLong(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next() ? Optional.of(videoResultSetMapper.map(resultSet)) : Optional.empty();
        }
    }

    @Override
    public Optional<Video> findByUrl(String url) throws SQLException {
        String selectByUrlQuery = "SELECT * FROM videos WHERE url = ?";
        try (PreparedStatement preparedStatement = databaseContext.getConnection().prepareStatement(selectByUrlQuery)) {
            preparedStatement.setString(1, url);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next() ? Optional.of(videoResultSetMapper.map(resultSet)) : Optional.empty();
        }
    }

    @Override
    public long countAll() throws SQLException {
        String countQuery = "SELECT COUNT(*) FROM videos";
        try (Statement statement = databaseContext.getConnection().createStatement();
                ResultSet resultSet = statement.executeQuery(countQuery)) {
            return resultSet.next() ? resultSet.getLong(1) : 0;
        }
    }

    @Override
    public long sumViews() throws SQLException {
        String sumQuery = "SELECT COALESCE(SUM(views), 0) FROM videos";
        try (Statement statement = databaseContext.getConnection().createStatement();
                ResultSet resultSet = statement.executeQuery(sumQuery)) {
            return resultSet.next() ? resultSet.getLong(1) : 0;
        }
    }

    @Override
    public void updateStats(long id, long views, String title, VideoStatus status, String errorMessage)
            throws SQLException {
        String updateQuery = """
                UPDATE videos
                SET views = ?, title = COALESCE(?, title),
                    status = ?, error_msg = ?, last_updated = NOW()
                WHERE id = ?
                """;
        try (PreparedStatement preparedStatement = databaseContext.getConnection().prepareStatement(updateQuery)) {
            preparedStatement.setLong(1, views);
            preparedStatement.setString(2, title);
            preparedStatement.setString(3, status.name());
            preparedStatement.setString(4, errorMessage);
            preparedStatement.setLong(5, id);
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public boolean deleteById(long id) throws SQLException {
        String deleteQuery = "DELETE FROM videos WHERE id = ? RETURNING id";
        try (PreparedStatement preparedStatement = databaseContext.getConnection().prepareStatement(deleteQuery)) {
            preparedStatement.setLong(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        }
    }

    private Video attemptInsertion(Video video) throws SQLException {
        String insertQuery = """
                INSERT INTO videos (url, platform, video_id, title, views, status, error_msg, last_updated)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (url) DO NOTHING
                RETURNING id, added_at
                """;
        try (PreparedStatement preparedStatement = databaseContext.getConnection().prepareStatement(insertQuery)) {
            bindVideoParameters(preparedStatement, video);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return populateGeneratedIdentifiers(video, resultSet);
            }
        }
        return null;
    }

    private Video resolveInsertionConflict(String url) throws SQLException {
        return findByUrl(url)
                .orElseThrow(() -> new SQLException("Conflict on save but URL not found: " + url));
    }

    private Video populateGeneratedIdentifiers(Video video, ResultSet resultSet) throws SQLException {
        video.setId(resultSet.getLong("id"));
        video.setAddedAt(convertTimestampToLocalDateTime(resultSet.getTimestamp("added_at")));
        LOGGER.debug("Saved new video id={}", video.getId());
        return video;
    }

    private void bindVideoParameters(PreparedStatement preparedStatement, Video video) throws SQLException {
        preparedStatement.setString(1, video.getUrl());
        preparedStatement.setString(2, video.getPlatform().name());
        preparedStatement.setString(3, video.getVideoId());
        preparedStatement.setString(4, video.getTitle());
        preparedStatement.setLong(5, video.getViews());
        preparedStatement.setString(6, video.getStatus().name());
        preparedStatement.setString(7, video.getErrorMessage());
        bindTimestampParameter(preparedStatement, 8, video.getLastUpdated());
    }

    private static void bindTimestampParameter(PreparedStatement preparedStatement, int parameterIndex,
            LocalDateTime localDateTime) throws SQLException {
        preparedStatement.setTimestamp(parameterIndex, localDateTime != null ? Timestamp.valueOf(localDateTime) : null);
    }

    private static LocalDateTime convertTimestampToLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
