# Desktop player fixtures

Generated with ffmpeg for libVLC smoke (`npm run smoke:vlc`).

Covers owner matrix containers where encoders are available on the build host.
Notes:

- `.ape`: Monkey's Audio encoder often absent; libVLC still lists APE as supported when the system VLC build includes the demuxer. Fixture may be omitted.
- `.alac`: stored as CAF/M4A-wrapped ALAC file named `sample.alac`.
- HLS: `sample.m3u8` + `hls/` segments.
- Codecs: `sample-hevc.mp4` (H.265), `sample-av1.mkv` (AV1), `sample.webm` (VP9), `sample.mpg` (MPEG-2).
