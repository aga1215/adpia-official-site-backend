package org.adpia.official.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.adpia.official.domain.member.Member;
import org.adpia.official.domain.member.repository.MemberRepository;
import org.adpia.official.email.service.EmailVerificationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

	private final MemberRepository memberRepository;
	private final EmailVerificationService emailVerificationService;
	private final PasswordEncoder passwordEncoder;

	public void sendResetCode(String email) {
		if (!memberRepository.existsByEmail(email)) {
			return;
		}
		emailVerificationService.sendVerificationCode(email);
	}

	public boolean verifyResetCode(String email, String code) {
		return emailVerificationService.verifyCode(email, code);
	}

	@Transactional
	public void resetPassword(String email, String code, String newPassword, String confirmPassword) {
		if (!newPassword.equals(confirmPassword)) {
			throw new IllegalArgumentException("비밀번호 확인이 일치하지 않습니다.");
		}

		boolean verified = emailVerificationService.verifyCode(email, code);
		if (!verified) {
			throw new IllegalArgumentException("인증 코드가 올바르지 않거나 만료되었습니다.");
		}

		Member member = memberRepository.findByEmail(email)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

		member.setPassword(passwordEncoder.encode(newPassword));
	}
}