package com.bgssai.media.common.transcode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Component
public class ProcessFfmpegGateway implements FfmpegGateway {

    private final String binary;

    public ProcessFfmpegGateway(@Value("${bgssai.media.ffmpeg.binary:ffmpeg}") String binary) {
        this.binary = (binary == null || binary.isBlank()) ? "ffmpeg" : binary.trim();
    }

    @Override
    public boolean available() {
        try {
            int code = run(new String[]{binary, "-version"}, 8, TimeUnit.SECONDS, null);
            return code == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public void transcode(Path source, Path dest, int height) throws Exception {
        Files.createDirectories(dest.getParent());
        String[] cmd = new String[]{
                binary, "-y", "-i", source.toAbsolutePath().toString(),
                "-vf", "scale=-2:" + height,
                "-c:v", "libx264", "-preset", "veryfast",
                "-c:a", "aac", "-movflags", "+faststart",
                dest.toAbsolutePath().toString()
        };
        int code = run(cmd, 5, TimeUnit.MINUTES, dest);
        if (code != 0 || !Files.isRegularFile(dest) || Files.size(dest) <= 0) {
            throw new IllegalStateException("ffmpeg transcode failed height=" + height + " exit=" + code);
        }
    }

    @Override
    public void extractCover(Path source, Path dest) throws Exception {
        Files.createDirectories(dest.getParent());
        String[] cmd = new String[]{
                binary, "-y", "-i", source.toAbsolutePath().toString(),
                "-ss", "0", "-vframes", "1", "-q:v", "2",
                dest.toAbsolutePath().toString()
        };
        int code = run(cmd, 30, TimeUnit.SECONDS, dest);
        if (code != 0 || !Files.isRegularFile(dest) || Files.size(dest) <= 0) {
            throw new IllegalStateException("ffmpeg cover extract failed exit=" + code);
        }
    }

    private int run(String[] cmd, long timeout, TimeUnit unit, Path expectedOut) throws Exception {
        Path logFile = Files.createTempFile("ffmpeg-", ".log");
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.redirectOutput(logFile.toFile());
            Process process = pb.start();
            boolean done = process.waitFor(timeout, unit);
            if (!done) {
                process.destroyForcibly();
                if (expectedOut != null) {
                    Files.deleteIfExists(expectedOut);
                }
                throw new IllegalStateException("ffmpeg timed out");
            }
            int code = process.exitValue();
            if (code != 0) {
                String out = Files.readString(logFile, StandardCharsets.UTF_8);
                if (out.length() > 400) {
                    out = out.substring(0, 400);
                }
                throw new IllegalStateException("ffmpeg exit=" + code + " " + out.trim());
            }
            return code;
        } finally {
            try {
                Files.deleteIfExists(logFile);
            } catch (Exception ignored) {
                // temp log
            }
        }
    }
}
