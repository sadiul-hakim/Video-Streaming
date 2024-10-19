package xyz.sadiulhakim.util;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;

import java.io.File;
import java.io.IOException;

public class VideoUtil {

    public static void processVideo(String basePath, String videoPath, String fileName) throws IOException, InterruptedException {

        String hlsPathText = basePath + File.separator + fileName;
        FileUtil.createFile(hlsPathText);

        String ffmpegCmd = String.format(
                "ffmpeg -i \"%s\" -c:v libx264 -c:a aac -strict -2 -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename \"%s/segment_%%3d.ts\"  \"%s/master.m3u8\" ",
                videoPath, hlsPathText, hlsPathText
        );

        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", ffmpegCmd);
        processBuilder.inheritIO();
        Process process = processBuilder.start();
        int exit = process.waitFor();
        if (exit != 0) {
            FileUtil.deleteFileAndFolders(videoPath);
            throw new RuntimeException("video processing failed!!");
        }


    }

    public static ResourceRegion getResourceRegion(FileSystemResource video, HttpHeaders headers, int chunkSize) throws IOException {
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

    public static HttpHeaders getStreamingHeaders(String title) {
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
}
