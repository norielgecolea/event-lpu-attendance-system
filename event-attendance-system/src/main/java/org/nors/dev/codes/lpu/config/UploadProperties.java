package org.nors.dev.codes.lpu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    /** Directory on disk for event photos. Override with APP_PICTURES_DIR. */
    private String picturesDir = "pictures";

    public String getPicturesDir() {
        return picturesDir;
    }

    public void setPicturesDir(String picturesDir) {
        this.picturesDir = picturesDir;
    }
}
