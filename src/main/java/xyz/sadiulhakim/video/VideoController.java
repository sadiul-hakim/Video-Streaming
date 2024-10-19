package xyz.sadiulhakim.video;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/video")
@RequiredArgsConstructor
public class VideoController {
    private final VideoService videoService;

    @PostMapping("")
    public ModelAndView save(@RequestParam MultipartFile file,
                             @RequestParam String title,
                             @RequestParam String description, ModelAndView modelAndView) {

        title = URLDecoder.decode(title.trim(), StandardCharsets.UTF_8);
        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);

        Video save = videoService.save(video, file);
        List<Video> videos = videoService.findAll();
        modelAndView.setViewName("index");
        modelAndView.addObject("videos", videos);
        modelAndView.addObject("saved", save != null);

        return modelAndView;
    }

    @GetMapping("/view")
    public ModelAndView view(@RequestParam String title, ModelAndView modelAndView) {
        title = URLDecoder.decode(title.trim(), StandardCharsets.UTF_8);
        Video video = videoService.findByTitle(title);
        modelAndView.setViewName("view");
        modelAndView.addObject("video", video);

        return modelAndView;
    }

    @GetMapping(path = "/stream")
    public ResponseEntity<ResourceRegion> stream(@RequestParam String title, @RequestHeader HttpHeaders headers) {

        try {
            return videoService.streamV2(title, headers);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    @GetMapping("/delete")
    public ModelAndView delete(@RequestParam String id, ModelAndView modelAndView) {
        videoService.delete(id);

        List<Video> videos = videoService.findAll();
        modelAndView.addObject("videos", videos);
        modelAndView.addObject("deleted", true);
        modelAndView.setViewName("index");

        return modelAndView;
    }
}
