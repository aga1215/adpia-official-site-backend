package org.adpia.official.dto.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetVerifyCodeRequest(
	@NotBlank
	@Email
	String email,

	@NotBlank
	String code
) {}