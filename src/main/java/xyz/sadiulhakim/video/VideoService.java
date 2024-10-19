package xyz.sadiulhakim.video;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import xyz.sadiulhakim.exception.JpaException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

    public void processVideo(String videoPath, String fileName) throws IOException {
        String hlsPathText = basePath + File.separator + fileName;
        createFile(hlsPathText);

        String hls360PathText = hlsPathText + "/360/";
        String hls720PathText = hlsPathText + "/720/";
        String hls1080PathText = hlsPathText + "/1080/";
        createFile(hls360PathText);
        createFile(hls720PathText);
        createFile(hls1080PathText);
    }

    private void createFile(String pathText) throws IOException {
        Path path = Path.of(pathText);
        if (!Files.exists(path)) {
            Files.createDirectory(path);
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
        byte[] data = extractBytes(inputStream, (int) contentLength);
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

        ResourceRegion region = getResourceRegion(video, headers);

        return ResponseEntity
                .status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(region);
    }

    private ResourceRegion getResourceRegion(FileSystemResource video, HttpHeaders headers) throws IOException {
        long contentLength = video.contentLength();
        HttpRange range = headers.getRange().stream().findFirst().orElse(null);

        if (range != null) {
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = Math.min(chunkSize, end - start + 1);
            return new ResourceRegion(video, start, rangeLength);
        } else {
            long rangeLength = Math.min(chunkSize, contentLength);
            return new ResourceRegion(video, 0, rangeLength);
        }
    }

    private byte[] extractBytes(InputStream stream, int length) throws IOException {
        byte[] data = new byte[length];
        int read = stream.read(data, 0, data.length);
        log.info("VideoService.extractBytes :: extracted {} bytes", read);
        return data;
    }

    private HttpHeaders getStreamingHeaders(String title) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + title + "\""); // Ensure it's not treated as an attachment
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        headers.add(HttpHeaders.EXPIRES, "0");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
        return headers;
    }

    private String uploadFile(MultipartFile file) throws IOException {

        String filename = file.getOriginalFilename();
        String newName = (UUID.randomUUID() + "_" + filename);
        String filePath = basePath + newName;
        InputStream inputStream = file.getInputStream();

        try {
            Files.copy(inputStream, Path.of(filePath));
        } catch (Exception ex) {
            throw new JpaException("VideoService :: Could not upload file : " + ex.getMessage());
        }

        //String fileNameWithoutExtension = newName.split("\\.")[0];
        //processVideo(filePath, fileNameWithoutExtension);

        return filePath;
    }
}
