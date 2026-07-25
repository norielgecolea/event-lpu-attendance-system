package org.nors.dev.codes.lpu.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(UploadProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private final UploadProperties uploadProperties;

    public WebMvcConfig(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path picturesPath = Paths.get(uploadProperties.getPicturesDir()).toAbsolutePath().normalize();
        String picturesLocation = picturesPath.toUri().toString();
        if (!picturesLocation.endsWith("/")) {
            picturesLocation = picturesLocation + "/";
        }
        registry.addResourceHandler("/pictures/**")
                .addResourceLocations(picturesLocation);

        Path tonesPath = Paths.get(uploadProperties.getTonesDir()).toAbsolutePath().normalize();
        String tonesLocation = tonesPath.toUri().toString();
        if (!tonesLocation.endsWith("/")) {
            tonesLocation = tonesLocation + "/";
        }
        registry.addResourceHandler("/tones/**")
                .addResourceLocations(tonesLocation);
    }
}
