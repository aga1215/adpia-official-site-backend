package org.adpia.official.email.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

	private static final String EMAIL_CODE_KEY_PREFIX = "EMAIL_CODE:";

	private final JavaMailSender mailSender;
	private final StringRedisTemplate redisTemplate;

	@Value("${adpia.email.verification.code-ttl-minutes:5}")
	private long codeTtlMinutes;

	@Value("${adpia.email.verification.from-address}")
	private String fromAddress;


	@Value("${adpia.email.verification.subject:[ADPIA] 이메일 인증 코드 안내}")
	private String subject;


	private final Random random = new Random();

	public void sendVerificationCode(String email) {
		String code = generateCode();

		String key = EMAIL_CODE_KEY_PREFIX + email;
		redisTemplate
			.opsForValue()
			.set(key, code, Duration.ofMinutes(codeTtlMinutes));

		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(
				mimeMessage,
				false,
				StandardCharsets.UTF_8.name()
			);

			helper.setTo(email);

			helper.setFrom(new InternetAddress(
				fromAddress,
				"ADPIA 운영진",
				StandardCharsets.UTF_8.name()
			));

			helper.setSubject(subject);

			helper.setText(buildEmailText(code), false);

			mailSender.send(mimeMessage);
		} catch (Exception e) {
			throw new RuntimeException("이메일 발송 중 오류가 발생했습니다.", e);
		}
	}

	public boolean verifyCode(String email, String code) {
		String key = EMAIL_CODE_KEY_PREFIX + email;
		String savedCode = redisTemplate.opsForValue().get(key);

		if (savedCode == null) {
			return false;
		}

		boolean match = savedCode.equals(code);

		if (match) {
			redisTemplate.delete(key);
		}

		return match;
	}

	private String generateCode() {
		int number = random.nextInt(900000) + 100000;
		return String.valueOf(number);
	}

	private String buildEmailText(String code) {
		return """
                안녕하세요, ADPIA입니다.

                아래 인증 코드를 입력해주세요.

                인증 코드: %s

                이 코드는 %d분 동안만 유효합니다.

                감사합니다.
                """.formatted(code, codeTtlMinutes);
	}
}
