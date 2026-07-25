package org.nors.dev.codes.lpu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    /** Directory on disk for event photos. Override with APP_PICTURES_DIR. */
    private String picturesDir = "pictures";

    /** Directory on disk for kiosk tones. Override with APP_TONES_DIR. */
    private String tonesDir = "tones";

    /** ffmpeg binary path (used to locate ffprobe for tone duration checks). */
    private String ffmpegPath = "ffmpeg";

    public String getPicturesDir() {
        return picturesDir;
    }

    public void setPicturesDir(String picturesDir) {
        this.picturesDir = picturesDir;
    }

    public String getTonesDir() {
        return tonesDir;
    }

    public void setTonesDir(String tonesDir) {
        this.tonesDir = tonesDir;
    }

    public String getFfmpegPath() {
        return ffmpegPath;
    }

    public void setFfmpegPath(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }
}
