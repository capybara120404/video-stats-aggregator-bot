package video.stats.aggregator.bot.infrastructure.persistence.repository;

import video.stats.aggregator.bot.domain.entity.Platform;
import video.stats.aggregator.bot.domain.entity.Video;
import video.stats.aggregator.bot.domain.entity.VideoStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

final class VideoResultSetMapper {
    Video map(ResultSet resultSet) throws SQLException {
        Video video = new Video();
        video.setId(resultSet.getLong("id"));
        video.setUrl(resultSet.getString("url"));
        video.setPlatform(Platform.valueOf(resultSet.getString("platform")));
        video.setVideoId(resultSet.getString("video_id"));
        video.setTitle(resultSet.getString("title"));
        video.setViews(resultSet.getLong("views"));
        video.setStatus(VideoStatus.fromString(resultSet.getString("status")));
        video.setErrorMessage(resultSet.getString("error_msg"));
        video.setLastUpdated(convertTimestampToLocalDateTime(resultSet.getTimestamp("last_updated")));
        video.setAddedAt(convertTimestampToLocalDateTime(resultSet.getTimestamp("added_at")));
        return video;
    }

    private static LocalDateTime convertTimestampToLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}