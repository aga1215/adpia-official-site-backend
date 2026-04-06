package org.adpia.official.dto.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
	@NotBlank
	@Email
	String email,

	@NotBlank
	String code,

	@NotBlank
	@Size(min = 8, max = 100)
	String newPassword,

	@NotBlank
	String confirmPassword
) {}