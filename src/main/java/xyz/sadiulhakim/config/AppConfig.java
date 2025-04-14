package xyz.sadiulhakim.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(1000 * 60 * 10);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/hls/**")
                .addResourceLocations("file:F:\\VideoStreamingApp\\");
    }

    @Bean
    Executor defaultTaskExecutor(){
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
