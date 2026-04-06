package org.adpia.official.domain.file.controller;

import org.adpia.official.domain.file.service.PresignedUrlService;
import org.adpia.official.dto.file.DownloadPresignRequest;
import org.adpia.official.dto.file.DownloadPresignResponse;
import org.adpia.official.dto.file.PresignRequest;
import org.adpia.official.dto.file.PresignResponse;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

	private final PresignedUrlService presignedUrlService;

	@PostMapping("/presign")
	public PresignResponse presign(@Valid @RequestBody PresignRequest req) {
		var result = presignedUrlService.createPutUrl(
			req.boardCode(), req.postId(), req.contentType(), req.originalFilename()
		);
		return new PresignResponse(result.putUrl(), result.key(), result.fileUrl());
	}

	@PostMapping("/download-presign")
	public DownloadPresignResponse downloadPresign(@Valid @RequestBody DownloadPresignRequest req) {
		var result = presignedUrlService.createGetUrl(req.key(), req.contentType(), req.originalFilename());
		return new DownloadPresignResponse(result.url());
	}
}