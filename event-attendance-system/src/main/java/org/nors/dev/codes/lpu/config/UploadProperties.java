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

    /** Resize and re-encode uploaded event photos to JPEG. */
    private boolean photoOptimizationEnabled = true;

    /** Longest edge in pixels for stored event photos (kiosk-friendly). */
    private int photoMaxDimension = 1600;

    /** JPEG quality from 0.1 (smallest) to 1.0 (largest). */
    private float photoJpegQuality = 0.82f;

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

    public boolean isPhotoOptimizationEnabled() {
        return photoOptimizationEnabled;
    }

    public void setPhotoOptimizationEnabled(boolean photoOptimizationEnabled) {
        this.photoOptimizationEnabled = photoOptimizationEnabled;
    }

    public int getPhotoMaxDimension() {
        return photoMaxDimension;
    }

    public void setPhotoMaxDimension(int photoMaxDimension) {
        this.photoMaxDimension = photoMaxDimension;
    }

    public float getPhotoJpegQuality() {
        return photoJpegQuality;
    }

    public void setPhotoJpegQuality(float photoJpegQuality) {
        this.photoJpegQuality = photoJpegQuality;
    }
}
