package org.adpia.official.domain.recruit.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.adpia.official.domain.member.MemberRole;
import org.adpia.official.domain.recruit.RecruitAuthorType;
import org.adpia.official.domain.recruit.RecruitComment;
import org.adpia.official.domain.recruit.RecruitPost;
import org.adpia.official.domain.recruit.repository.RecruitCommentLikeRepository;
import org.adpia.official.domain.recruit.repository.RecruitCommentRepository;
import org.adpia.official.domain.recruit.repository.RecruitPostRepository;
import org.adpia.official.dto.recruit.RecruitCommentCreateRequest;
import org.adpia.official.dto.recruit.RecruitCommentResponse;
import org.adpia.official.security.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecruitCommentService {

	private final RecruitPostRepository postRepository;
	private final RecruitCommentRepository commentRepository;
	private final RecruitCommentLikeRepository commentLikeRepository;
	private final PasswordHasher passwordHasher;

	private boolean isAdmin(RecruitService.Actor actor) {
		return !actor.isGuest() && (
			actor.role() == MemberRole.ROLE_SUPER_ADMIN || actor.role() == MemberRole.ROLE_PRESIDENT
		);
	}

	@Transactional(readOnly = true)
	public List<RecruitCommentResponse> list(Long postId, RecruitService.Actor actor) {
		RecruitPost post = postRepository.findByIdAndDeletedAtIsNull(postId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

		if (!post.getBoardCode().allowGuestRead() && actor.isGuest()) {
			throw new IllegalStateException("로그인 후 접근할 수 있습니다.");
		}

		List<RecruitComment> all = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);

		Map<Long, RecruitCommentResponse> map = new LinkedHashMap<>();
		List<RecruitCommentResponse> roots = new ArrayList<>();

		for (RecruitComment c : all) {
			boolean likedByMe = false;
			if (!actor.isGuest()) {
				likedByMe = commentLikeRepository.existsByCommentIdAndMemberId(c.getId(), actor.memberId());
			}

			RecruitCommentResponse dto = RecruitCommentResponse.from(c, likedByMe);
			map.put(dto.getId(), dto);
		}

		for (RecruitComment c : all) {
			RecruitCommentResponse dto = map.get(c.getId());
			if (c.getParentId() == null) {
				roots.add(dto);
			} else {
				RecruitCommentResponse parent = map.get(c.getParentId());
				if (parent != null) parent.getChildren().add(dto);
				else roots.add(dto);
			}
		}

		return roots;
	}

	@Transactional
	public RecruitCommentResponse create(Long postId, RecruitCommentCreateRequest req, RecruitService.Actor actor) {
		RecruitPost post = postRepository.findByIdAndDeletedAtIsNull(postId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

		if (!post.isCommentEnabled()) {
			throw new IllegalStateException("댓글이 비활성화된 게시글입니다.");
		}

		Long parentId = req.getParentId();
		if (parentId != null) {
			RecruitComment parent = commentRepository.findByIdAndDeletedFalse(parentId)
				.orElseThrow(() -> new IllegalArgumentException("부모 댓글이 존재하지 않습니다."));

			if (!Objects.equals(parent.getPostId(), postId)) {
				throw new IllegalStateException("잘못된 parentId 입니다.");
			}
		}

		boolean isGuest = actor.isGuest();

		String authorName = isGuest
			? required(req.getAuthorName(), "작성자 이름")
			: required(actor.displayName(), "작성자 이름");

		String pwHash = isGuest
			? passwordHasher.hash(required(req.getPassword(), "댓글 비밀번호"))
			: null;

		RecruitComment comment = RecruitComment.builder()
			.postId(postId)
			.parentId(parentId)
			.authorType(isGuest ? RecruitAuthorType.GUEST : RecruitAuthorType.MEMBER)
			.authorMemberId(isGuest ? null : actor.memberId())
			.authorName(authorName)
			.content(required(req.getContent(), "댓글 내용"))
			.passwordHash(pwHash)
			.deleted(false)
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.build();

		commentRepository.save(comment);
		return RecruitCommentResponse.from(comment, false);
	}

	@Transactional
	public void delete(Long commentId, RecruitService.Actor actor, String password) {
		RecruitComment c = commentRepository.findByIdAndDeletedFalse(commentId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

		if (isAdmin(actor)) {
			c.setDeleted(true);
			c.setUpdatedAt(LocalDateTime.now());
			return;
		}

		if (!actor.isGuest()) {
			if (c.getAuthorType() == RecruitAuthorType.MEMBER &&
				Objects.equals(c.getAuthorMemberId(), actor.memberId())) {
				c.setDeleted(true);
				c.setUpdatedAt(LocalDateTime.now());
				return;
			}
			throw new IllegalStateException("댓글 삭제 권한이 없습니다.");
		}

		if (c.getAuthorType() != RecruitAuthorType.GUEST) {
			throw new IllegalStateException("댓글 삭제 권한이 없습니다.");
		}
		if (password == null || password.isBlank() || !passwordHasher.matches(password, c.getPasswordHash())) {
			throw new IllegalStateException("비밀번호가 올바르지 않습니다.");
		}

		c.setDeleted(true);
		c.setUpdatedAt(LocalDateTime.now());
	}

	private static String required(String v, String field) {
		if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException(field + "을(를) 입력해주세요.");
		return v.trim();
	}
}