package com.bgssai.media.user.controller;

import com.bgssai.media.common.service.CreatorVideoService;
import com.bgssai.media.common.web.BizException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

/**
 * Public READY rendition/cover bytes for the player. Originals are not served.
 */
@RestController
@RequestMapping("/api/creator/media")
public class CreatorMediaFileController {

    private final CreatorVideoService creatorVideoService;

    public CreatorMediaFileController(CreatorVideoService creatorVideoService) {
        this.creatorVideoService = creatorVideoService;
    }

    @GetMapping("/{videoId}/{asset}")
    public ResponseEntity<Resource> media(@PathVariable("videoId") Long videoId,
                                          @PathVariable("asset") String asset) {
        Path path = creatorVideoService.publicAsset(videoId, asset);
        FileSystemResource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            throw new BizException(404, "asset not found");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(CreatorVideoService.contentTypeForAsset(asset)))
                .body(resource);
    }
}
