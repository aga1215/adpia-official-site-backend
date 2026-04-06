package org.adpia.official.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PresignRequest(
	@NotBlank String boardCode,
	@NotNull Long postId,
	String contentType,
	@NotBlank String originalFilename
) {}