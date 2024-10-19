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

    @Deprecated
    public ResponseEntity<Resource> stream(String range, String title) throws IOException {

        // Find the video and the file path
        Video video = findByTitle(title);
        //URI videoUri = new URI("file", video.getFilePath().replace("\\", "/"), null);
        Path path = Path.of(video.getFilePath());

        // Put some headers
        HttpHeaders headers = VideoUtil.getStreamingHeaders(title);

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
        rangeEnd = Math.min(rangeStart + chunkSize, fileLength - 1);
        long contentLength = rangeEnd - rangeStart;

        headers.add(HttpHeaders.CONTENT_RANGE, "bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength);
        headers.setContentLength(contentLength);
        InputStream inputStream = Files.newInputStream(path);

        // Skip till the start of the range
        // Suppose we are loading from 5120 to 10,240, so skip from 0 to 5120
        long skip = inputStream.skip(rangeStart);
        log.info("VideoService.stream :: Skipped video : {}", skip);

        // Extract data from stream
        byte[] data = StreamUtil.extractBytes(inputStream, (int) contentLength);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(headers)
                .body(new ByteArrayResource(data));
    }

    public ResponseEntity<ResourceRegion> streamV2(String title, HttpHeaders headers) throws IOException {

        // Find the video and the file path
        Video videoObj = findByTitle(title);

        //URI videoUri = new URI("file", video.getFilePath().replace("\\", "/"), null);
        Path path = Path.of(videoObj.getFilePath());

        FileSystemResource video = new FileSystemResource(path);

        ResourceRegion region = VideoUtil.getResourceRegion(video, headers, chunkSize);

        return ResponseEntity
                .status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(region);
    }

    public Optional<Resource> getMasterFile(String title) {
        Video video = findByTitle(title);
        String filePath = video.getFilePath();
        String masterFile = filePath.substring(0, filePath.lastIndexOf(".")) + File.separator + "master.m3u8";

        if (!Files.exists(Path.of(masterFile))) {
            return Optional.empty();
        }

        return Optional.of(new FileSystemResource(masterFile));
    }

    public Optional<Resource> getSegmentFile(String title,String segmentName) {
        Video video = findByTitle(title);
        String filePath = video.getFilePath();
        String masterFile = filePath.substring(0, filePath.lastIndexOf(".")) + File.separator + segmentName;

        if (!Files.exists(Path.of(masterFile))) {
            return Optional.empty();
        }

        return Optional.of(new FileSystemResource(masterFile));
    }
}
