package xyz.sadiulhakim.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import xyz.sadiulhakim.video.Video;
import xyz.sadiulhakim.video.VideoService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PageController {
    private final VideoService videoService;

    @GetMapping("/")
    public ModelAndView home(ModelAndView modelAndView) {

        List<Video> videos = videoService.findAll();
        modelAndView.setViewName("index");
        modelAndView.addObject("videos", videos);
        return modelAndView;
    }
}
