package com.splitwiseplusplus.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Logs web-server startup success and detailed root cause on startup failure.
 */
@Component
@Slf4j
public class StartupFailureLogger implements ApplicationListener<ApplicationFailedEvent> {

    @Override
    public void onApplicationEvent(ApplicationFailedEvent event) {
        Throwable root = event.getException();
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        log.error("Application startup failed: {}", root.getMessage(), event.getException());
    }

    @Component
    @Slf4j
    static class WebServerStartupLogger implements ApplicationListener<WebServerInitializedEvent> {
        @Override
        public void onApplicationEvent(WebServerInitializedEvent event) {
            log.info("Web server started and listening on port {}", event.getWebServer().getPort());
        }
    }
}
