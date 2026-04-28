package video.stats.aggregator.bot.domain.entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Video {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private Long id;
    private String url;
    private Platform platform;
    private String videoId;
    private String title;
    private long views;
    private VideoStatus status;
    private String errorMessage;
    private LocalDateTime lastUpdated;
    private LocalDateTime addedAt;
    private boolean newlyCreated;

    public Video() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        this.id = v;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String v) {
        this.url = v;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform v) {
        this.platform = v;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String v) {
        this.videoId = v;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String v) {
        this.title = v;
    }

    public long getViews() {
        return views;
    }

    public void setViews(long v) {
        this.views = v;
    }

    public VideoStatus getStatus() {
        return status;
    }

    public void setStatus(VideoStatus v) {
        this.status = v;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String v) {
        this.errorMessage = v;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime v) {
        this.lastUpdated = v;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime v) {
        this.addedAt = v;
    }

    public boolean isNewlyCreated() {
        return newlyCreated;
    }

    public void setNewlyCreated(boolean v) {
        this.newlyCreated = v;
    }

    public String getFormattedViews() {
        if (status == VideoStatus.PENDING)
            return "-";
        return String.format("%,d", views).replace(',', ' ');
    }

    public String getFormattedLastUpdated() {
        return lastUpdated != null ? lastUpdated.format(FMT) : "никогда";
    }

    public String getDisplayTitle() {
        if (title != null && !title.isBlank()) {
            return title;
        }
        String s = url.length() > 50 ? url.substring(0, 47) + "…" : url;
        return s;
    }

    @Override
    public String toString() {
        return "Video{id=" + id + ", platform=" + platform + ", views=" + views + "}";
    }
}
