package org.adpia.official.domain.file.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class PresignedUrlService {

	private final S3Presigner presigner;

	@Value("${AWS_S3_BUCKET}")
	private String bucket;

	@Value("${AWS_S3_PUBLIC_BASE_URL:}")
	private String publicBaseUrl;

	public PresignResult createPutUrl(String boardCode, Long postId, String contentType, String originalFilename) {
		validateBoardCode(boardCode);
		validateOriginalFilename(originalFilename);

		String normalizedContentType = normalizeContentType(contentType);
		String ext = guessExt(originalFilename, normalizedContentType);
		String key = buildKey(boardCode, postId, normalizedContentType, ext);

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(bucket)
			.key(key)
			.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(Duration.ofMinutes(10))
			.putObjectRequest(putObjectRequest)
			.build();

		PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);

		String fileUrl = buildPublicUrl(key);
		return new PresignResult(presigned.url().toString(), key, fileUrl);
	}

	public PresignGetResult createGetUrl(String key, String contentTypeOrNull, String filenameOrNull) {
		if (key == null || key.isBlank()) throw new IllegalArgumentException("key가 필요합니다.");

		String contentType = (contentTypeOrNull == null || contentTypeOrNull.isBlank())
			? "application/octet-stream"
			: contentTypeOrNull.trim();

		String filename = (filenameOrNull == null || filenameOrNull.isBlank())
			? "download"
			: filenameOrNull.trim();

		String encoded = encodeRFC5987(filename);
		String asciiFallback = toAsciiFallback(filename);

		String contentDisposition = String.format(
			"attachment; filename=\"%s\"; filename*=UTF-8''%s",
			asciiFallback,
			encoded
		);

		GetObjectRequest getReq = GetObjectRequest.builder()
			.bucket(bucket)
			.key(key)
			.responseContentDisposition(contentDisposition)
			.responseContentType(contentType)
			.build();

		GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
			.signatureDuration(Duration.ofMinutes(10))
			.getObjectRequest(getReq)
			.build();

		PresignedGetObjectRequest presigned = presigner.presignGetObject(presignReq);
		return new PresignGetResult(presigned.url().toString());
	}

	private String normalizeContentType(String contentType) {
		if (contentType == null || contentType.isBlank()) {
			return "application/octet-stream";
		}

		String ct = contentType.trim().toLowerCase();
		validateContentType(ct);
		return ct;
	}

	private static String encodeRFC5987(String s) {
		String enc = URLEncoder.encode(s, StandardCharsets.UTF_8);
		enc = enc.replace("+", "%20");
		enc = enc.replace("%7E", "~");
		return enc;
	}

	private static String toAsciiFallback(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c >= 32 && c <= 126 && c != '"' && c != '\\') sb.append(c);
			else sb.append('_');
		}
		String out = sb.toString().trim();
		return out.isBlank() ? "download" : out;
	}

	private String buildKey(String boardCode, Long postId, String contentType, String ext) {
		String typeFolder =
			contentType != null && contentType.startsWith("video") ? "videos"
				: contentType != null && contentType.startsWith("image") ? "images"
				: "files";

		String date = LocalDate.now().toString().replace("-", "");
		String postPart = (postId == null || postId <= 0) ? "temp" : String.valueOf(postId);

		String prefix = "recruit";
		if ("EXECUTIVES".equalsIgnoreCase(boardCode)) {
			prefix = "executives";
		}

		if ("executives".equals(prefix)) {
			return prefix + "/" + date + "/" + typeFolder + "/" + UUID.randomUUID() + ext;
		}

		return prefix + "/" + boardCode + "/" + date + "/" + postPart + "/" + typeFolder + "/" + UUID.randomUUID() + ext;
	}

	private String buildPublicUrl(String key) {
		if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
			String base = publicBaseUrl.endsWith("/")
				? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
				: publicBaseUrl;
			return base + "/" + key;
		}
		return "https://" + bucket + ".s3.amazonaws.com/" + key;
	}

	private void validateBoardCode(String boardCode) {
		if (boardCode == null || boardCode.isBlank()) throw new IllegalArgumentException("boardCode가 필요합니다.");
	}

	private void validateOriginalFilename(String originalFilename) {
		if (originalFilename == null || originalFilename.isBlank()) {
			throw new IllegalArgumentException("originalFilename이 필요합니다.");
		}
	}

	private void validateContentType(String contentType) {
		if (contentType == null || contentType.isBlank()) return;

		String ct = contentType.trim().toLowerCase();

		if (ct.startsWith("image/")) return;
		if (ct.startsWith("video/")) return;

		if (ct.equals("application/pdf")) return;
		if (ct.equals("application/zip")) return;
		if (ct.equals("text/plain")) return;

		if (ct.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) return;
		if (ct.equals("application/msword")) return;
		if (ct.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")) return;
		if (ct.equals("application/vnd.ms-powerpoint")) return;
		if (ct.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) return;
		if (ct.equals("application/vnd.ms-excel")) return;

		if (ct.equals("application/x-hwp")) return;
		if (ct.equals("application/haansofthwp")) return;
		if (ct.equals("application/vnd.hancom.hwp")) return;

		if (ct.equals("application/octet-stream")) return;

		throw new IllegalArgumentException("허용되지 않은 contentType: " + contentType);
	}

	private String guessExt(String filename, String contentType) {
		if (filename != null) {
			String trimmed = filename.trim();
			int dot = trimmed.lastIndexOf('.');
			if (dot >= 0 && dot < trimmed.length() - 1) {
				String ext = trimmed.substring(dot);
				if (ext.length() <= 10) return ext;
			}
		}

		if (contentType == null) return "";
		String ct = contentType.trim().toLowerCase();

		if (ct.equals("image/png")) return ".png";
		if (ct.equals("image/jpeg")) return ".jpg";
		if (ct.equals("image/jpg")) return ".jpg";
		if (ct.equals("image/gif")) return ".gif";
		if (ct.equals("image/webp")) return ".webp";
		if (ct.equals("image/svg+xml")) return ".svg";

		if (ct.equals("video/mp4")) return ".mp4";
		if (ct.equals("video/webm")) return ".webm";
		if (ct.equals("video/quicktime")) return ".mov";

		if (ct.equals("application/pdf")) return ".pdf";
		if (ct.equals("application/zip")) return ".zip";
		if (ct.equals("text/plain")) return ".txt";

		if (ct.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) return ".docx";
		if (ct.equals("application/msword")) return ".doc";
		if (ct.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")) return ".pptx";
		if (ct.equals("application/vnd.ms-powerpoint")) return ".ppt";
		if (ct.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) return ".xlsx";
		if (ct.equals("application/vnd.ms-excel")) return ".xls";

		if (ct.equals("application/x-hwp")) return ".hwp";
		if (ct.equals("application/haansofthwp")) return ".hwp";
		if (ct.equals("application/vnd.hancom.hwp")) return ".hwp";

		return "";
	}

	public record PresignResult(String putUrl, String key, String fileUrl) {}
	public record PresignGetResult(String url) {}
}