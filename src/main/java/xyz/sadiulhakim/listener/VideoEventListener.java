package xyz.sadiulhakim.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import xyz.sadiulhakim.event.VideoEvent;
import xyz.sadiulhakim.util.VideoUtil;

import java.io.File;
import java.io.IOException;

@Component
public class VideoEventListener {

    @Async("defaultTaskExecutor")
    @EventListener
    void listenToVideoCreatedEvent(VideoEvent event) throws IOException, InterruptedException {

        File file = new File(event.filePath());
        String fileNameWithoutExtension = file.getName().split("\\.")[0];
        VideoUtil.processVideo(event.basePath(), event.filePath(), fileNameWithoutExtension);
    }
}
