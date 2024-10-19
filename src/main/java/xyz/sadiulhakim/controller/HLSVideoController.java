package xyz.sadiulhakim.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.sadiulhakim.video.VideoService;

import java.util.Optional;

@RestController
@RequestMapping("/hls-video")
@RequiredArgsConstructor
public class HLSVideoController {
    private final VideoService videoService;

    @GetMapping("/master/{title}")
    public ResponseEntity<Resource> getMasterFile(@PathVariable String title) {
        Optional<Resource> masterFile = videoService.getMasterFile(title);
        return masterFile.map(resource -> ResponseEntity
                .ok()
                .header(
                        HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl"
                )
                .body(resource)).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .build());
    }

    @GetMapping("/segment/{title}/{segment}.ts")
    public ResponseEntity<Resource> getSegmentFile(@PathVariable String title, @PathVariable String segment) {
        Optional<Resource> masterFile = videoService.getSegmentFile(title, segment);
        return masterFile.map(resource -> ResponseEntity
                .ok()
                .header(
                        HttpHeaders.CONTENT_TYPE, "video/mp2t"
                )
                .body(resource)).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .build());
    }
}
