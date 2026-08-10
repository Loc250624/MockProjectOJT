package com.ojtsu26.elearning.config;

import com.ojtsu26.elearning.web.MaintenanceModeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String avatarDirectory;
    private final MaintenanceModeInterceptor maintenanceModeInterceptor;

    public WebConfig(@Value("${app.upload.avatar-directory:uploads/avatars}") String avatarDirectory,
                     MaintenanceModeInterceptor maintenanceModeInterceptor) {
        this.avatarDirectory = avatarDirectory;
        this.maintenanceModeInterceptor = maintenanceModeInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(maintenanceModeInterceptor)
                .addPathPatterns("/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(avatarDirectory).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");

        Path uploadsPath = Paths.get("uploads").toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsPath.toUri() + "/");
    }

}
