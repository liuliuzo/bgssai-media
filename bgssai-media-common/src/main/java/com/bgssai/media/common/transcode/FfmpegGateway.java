package com.bgssai.media.common.transcode;

import java.nio.file.Path;

/**
 * Local ffmpeg adapter. Missing binary must be reported as unavailable so jobs FAIL
 * instead of being marked READY.
 */
public interface FfmpegGateway {

    boolean available();

    void transcode(Path source, Path dest, int height) throws Exception;

    void extractCover(Path source, Path dest) throws Exception;
}
