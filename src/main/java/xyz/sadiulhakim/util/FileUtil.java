package xyz.sadiulhakim.util;

import org.springframework.web.multipart.MultipartFile;
import xyz.sadiulhakim.exception.JpaException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

public class FileUtil {

    public static String uploadFile(String basePath, MultipartFile file) throws IOException, InterruptedException {

        String filename = file.getOriginalFilename();
        String newName = (UUID.randomUUID() + "_" + filename);
        String filePath = basePath + newName;
        InputStream inputStream = file.getInputStream();

        try {
            Files.copy(inputStream, Path.of(filePath));
        } catch (Exception ex) {
            throw new JpaException("VideoService :: Could not upload file : " + ex.getMessage());
        }

        String fileNameWithoutExtension = newName.split("\\.")[0];
        VideoUtil.processVideo(basePath,filePath, fileNameWithoutExtension);

        return filePath;
    }

    public static void deleteFileAndFolders(String path) {
        Path filePath = Path.of(path);
        String fileName = filePath.getFileName().toString();
        Path parent = filePath.getParent();
        String folderName = fileName.substring(0, fileName.lastIndexOf("."));
        Path folderPath = parent.resolve(folderName);

        FileUtil.deleteFile(filePath);
        FileUtil.deleteFile(folderPath);
    }

    public static void deleteFile(Path path) {
        Thread.ofVirtual().start(() -> {

            try {

                if (path.toFile().isFile()) {
                    Files.deleteIfExists(path);
                } else {
                    deleteFolder(path);
                }

            } catch (IOException e) {
                throw new JpaException("VideoService :: Could not delete file : " + path);
            }
        });
    }

    public static void deleteFolder(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                throw new JpaException("VideoService :: Could not delete file : " + file);
            }
        });
    }

    public static void createFile(String pathText) throws IOException {
        Path path = Path.of(pathText);
        if (!Files.exists(path)) {
            Files.createDirectory(path);
        }
    }
}
