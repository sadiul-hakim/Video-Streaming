package xyz.sadiulhakim.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class StreamUtil {
    public static byte[] extractBytes(InputStream stream, int length) throws IOException {
        byte[] data = new byte[length];
        int read = stream.read(data, 0, data.length);
        log.info("VideoService.extractBytes :: extracted {} bytes", read);
        return data;
    }
}
