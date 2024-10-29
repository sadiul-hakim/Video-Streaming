package xyz.sadiulhakim.video;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import xyz.sadiulhakim.exception.JpaException;
import xyz.sadiulhakim.util.FileUtil;
import xyz.sadiulhakim.util.StreamUtil;
import xyz.sadiulhakim.util.VideoUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    @Value("${file.upload.base.path:''}")
    private String basePath;

    @Value("${max.chunk.size}")
    private int chunkSize;

    public final VideoRepository videoRepository;

    public Video save(Video video, MultipartFile file) {

        try {
            if (file == null || !Objects.equals(file.getContentType(), "video/mp4")) {
                throw new JpaException("VideoService :: Invalid file format.");
            }

            String filePath = FileUtil.uploadFile(basePath, file);

            video.setFilePath(filePath);
            if (video.getVideoId() == null || video.getVideoId().isEmpty()) {
                video.setVideoId(UUID.randomUUID().toString());
            }
            video.setContentType(file.getContentType());

            return videoRepository.save(video);
        } catch (Exception ex) {
            throw new JpaException("VideoService :: Could not upload file : " + ex.getMessage());
        }
    }

    public Video findById(String id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new JpaException("VideoService :: Video not found with id :" + id));
    }

    public Video findByTitle(String title) {
        return videoRepository.findByTitle(title)
                .orElseThrow(() -> new JpaException("VideoService :: Video not found with title :" + title));
    }

    public List<Video> findAll() {
        return videoRepository.findAll();
    }

    public void delete(String id) {
        Video video = findById(id);
        FileUtil.deleteFileAndFolders(video.getFilePath());
        videoRepository.deleteById(id);
    }

    public ResponseEntity<ResourceRegion> streamV2(String title, HttpHeaders headers) throws IOException {

        // Find the video and the file path
        Video videoObj = findByTitle(title);

        Path path = Path.of(videoObj.getFilePath());

        FileSystemResource video = new FileSystemResource(path);

        ResourceRegion region = VideoUtil.getResourceRegion(video, headers, chunkSize);

        return ResponseEntity
                .status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(region);
    }
}
