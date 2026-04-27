package video.stats.aggregator.bot.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import video.stats.aggregator.bot.db.DatabaseManager;
import video.stats.aggregator.bot.model.Platform;
import video.stats.aggregator.bot.model.Video;
import video.stats.aggregator.bot.model.VideoStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VideoRepository {
    private static final Logger log = LoggerFactory.getLogger(VideoRepository.class);
    private final DatabaseManager db;

    public VideoRepository(DatabaseManager db) {
        this.db = db;
    }

    public Video save(Video v) throws SQLException {
        String sql = """
                INSERT INTO videos (url, platform, video_id, title, views, status, error_msg, last_updated)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (url) DO NOTHING
                RETURNING id, added_at
                """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, v.getUrl());
            ps.setString(2, v.getPlatform().name());
            ps.setString(3, v.getVideoId());
            ps.setString(4, v.getTitle());
            ps.setLong(5, v.getViews());
            ps.setString(6, v.getStatus().name());
            ps.setString(7, v.getErrorMessage());
            setTimestamp(ps, 8, v.getLastUpdated());

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                v.setId(rs.getLong("id"));
                v.setAddedAt(toLocalDateTime(rs.getTimestamp("added_at")));
                log.debug("Saved new video id={}", v.getId());
                return v;
            }
        }
        // URL already exists — fetch existing
        return findByUrl(v.getUrl())
                .orElseThrow(() -> new SQLException("Conflict on save but URL not found: " + v.getUrl()));
    }

    public List<Video> findAll() throws SQLException {
        String sql = "SELECT * FROM videos ORDER BY added_at ASC";
        List<Video> result = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                result.add(map(rs));
        }
        return result;
    }

    public Optional<Video> findById(long id) throws SQLException {
        String sql = "SELECT * FROM videos WHERE id = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? Optional.of(map(rs)) : Optional.empty();
        }
    }

    public Optional<Video> findByUrl(String url) throws SQLException {
        String sql = "SELECT * FROM videos WHERE url = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, url);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? Optional.of(map(rs)) : Optional.empty();
        }
    }

    public long countAll() throws SQLException {
        try (Statement st = db.getConnection().createStatement();
                ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM videos")) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    public long sumViews() throws SQLException {
        try (Statement st = db.getConnection().createStatement();
                ResultSet rs = st.executeQuery("SELECT COALESCE(SUM(views), 0) FROM videos")) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    public void updateStats(long id, long views, String title, VideoStatus status, String errorMsg)
            throws SQLException {
        String sql = """
                UPDATE videos
                SET views = ?, title = COALESCE(?, title),
                    status = ?, error_msg = ?, last_updated = NOW()
                WHERE id = ?
                """;
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setLong(1, views);
            ps.setString(2, title);
            ps.setString(3, status.name());
            ps.setString(4, errorMsg);
            ps.setLong(5, id);
            ps.executeUpdate();
        }
    }

    public boolean deleteById(long id) throws SQLException {
        String sql = "DELETE FROM videos WHERE id = ? RETURNING id";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    private Video map(ResultSet rs) throws SQLException {
        Video v = new Video();
        v.setId(rs.getLong("id"));
        v.setUrl(rs.getString("url"));
        v.setPlatform(Platform.valueOf(rs.getString("platform")));
        v.setVideoId(rs.getString("video_id"));
        v.setTitle(rs.getString("title"));
        v.setViews(rs.getLong("views"));
        v.setStatus(VideoStatus.fromString(rs.getString("status")));
        v.setErrorMessage(rs.getString("error_msg"));
        v.setLastUpdated(toLocalDateTime(rs.getTimestamp("last_updated")));
        v.setAddedAt(toLocalDateTime(rs.getTimestamp("added_at")));
        return v;
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    private static void setTimestamp(PreparedStatement ps, int idx, LocalDateTime dt) throws SQLException {
        ps.setTimestamp(idx, dt != null ? Timestamp.valueOf(dt) : null);
    }
}
