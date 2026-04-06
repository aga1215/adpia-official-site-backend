package org.adpia.official.domain.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.adpia.official.domain.member.service.PasswordResetService;
import org.adpia.official.dto.member.PasswordResetConfirmRequest;
import org.adpia.official.dto.member.PasswordResetSendCodeRequest;
import org.adpia.official.dto.member.PasswordResetVerifyCodeRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/password")
public class PasswordResetController {

	private final PasswordResetService passwordResetService;

	@PostMapping("/reset/send-code")
	public ResponseEntity<MessageResponse> sendCode(
		@RequestBody @Valid PasswordResetSendCodeRequest request
	) {
		passwordResetService.sendResetCode(request.email());
		return ResponseEntity.ok(new MessageResponse(true, "입력한 이메일로 인증 코드를 전송했습니다."));
	}

	@PostMapping("/reset/verify-code")
	public ResponseEntity<MessageResponse> verifyCode(
		@RequestBody @Valid PasswordResetVerifyCodeRequest request
	) {
		boolean success = passwordResetService.verifyResetCode(request.email(), request.code());

		if (!success) {
			return ResponseEntity.badRequest()
				.body(new MessageResponse(false, "인증 코드가 올바르지 않거나 만료되었습니다."));
		}

		return ResponseEntity.ok(new MessageResponse(true, "인증에 성공했습니다."));
	}

	@PostMapping("/reset/confirm")
	public ResponseEntity<MessageResponse> confirm(
		@RequestBody @Valid PasswordResetConfirmRequest request
	) {
		passwordResetService.resetPassword(
			request.email(),
			request.code(),
			request.newPassword(),
			request.confirmPassword()
		);

		return ResponseEntity.ok(new MessageResponse(true, "비밀번호가 재설정되었습니다."));
	}

	public record MessageResponse(boolean success, String message) {}
}