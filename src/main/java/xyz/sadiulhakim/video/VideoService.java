package xyz.sadiulhakim.video;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import xyz.sadiulhakim.exception.JpaException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoService {

    @Value("${file.upload.base.path:''}")
    private String basePath;

    public final VideoRepository videoRepository;
    private final ResourceLoader resourceLoader;

    public Video save(Video video, MultipartFile file) {

        try {
            if (file == null || !file.getContentType().equals("video/mp4")) {
                throw new JpaException("VideoService :: Invalid file format.");
            }

            String filePath = uploadFile(file);
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
        deleteFile(video.getFilePath());

        videoRepository.deleteById(id);
    }

    private void deleteFile(String path) {
        Thread.ofVirtual().start(() -> {
            Path pathObj = Path.of(path);
            if (!Files.exists(pathObj)) {
                return;
            }

            try {
                Files.delete(pathObj);
            } catch (IOException e) {
                throw new JpaException("VideoService :: Could not delete file : " + path);
            }
        });
    }

    public ResponseEntity<Resource> stream(String range, String title) throws IOException {

        // Find the video and the file path
        Video video = findByTitle(title);
        //URI videoUri = new URI("file", video.getFilePath().replace("\\", "/"), null);
        Path path = Path.of(video.getFilePath());

        // Put some headers
        HttpHeaders headers = getStreamingHeaders(title);

        // Check range
        if (range == null || range.isEmpty()) {
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new FileSystemResource(path));
        }

        // Calculate start and end of the range
        long fileLength = path.toFile().length();
        long rangeStart;
        long rangeEnd;

        String[] ranges = range.replace("bytes=", "").split("-");
        rangeStart = Long.parseLong(ranges[0]);
        rangeEnd = ranges.length > 1 ? Long.parseLong(ranges[1]) : fileLength - 1;
        long contentLength = rangeEnd - rangeStart + 1;

        headers.add(HttpHeaders.CONTENT_RANGE, "bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength);
        headers.setContentLength(contentLength);

        InputStream inputStream = Files.newInputStream(path);
        inputStream.skip(rangeStart);
        InputStreamResource resource = new InputStreamResource(inputStream);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(headers)
                .body(resource);
    }

    private HttpHeaders getStreamingHeaders(String title) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + title + "\""); // Ensure it's not treated as an attachment
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        headers.add(HttpHeaders.EXPIRES, "0");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
        headers.add("X-Content-Type-Options", "nosniff");

        return headers;
    }

    private String uploadFile(MultipartFile file) throws IOException {


        String filename = file.getOriginalFilename();
        InputStream inputStream = file.getInputStream();
        String filePath = basePath + (UUID.randomUUID() + "_" + filename);

//        Thread.ofVirtual().start(() -> {
        try {
            Files.copy(inputStream, Path.of(filePath));
        } catch (Exception ex) {
            throw new JpaException("VideoService :: Could not upload file : " + ex.getMessage());
        }
//        });
        return filePath;
    }
}
