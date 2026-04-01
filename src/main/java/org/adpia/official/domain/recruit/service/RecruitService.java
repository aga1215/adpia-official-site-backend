package org.adpia.official.domain.recruit.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.adpia.official.domain.member.Member;
import org.adpia.official.domain.member.MemberRole;
import org.adpia.official.domain.member.repository.MemberRepository;
import org.adpia.official.domain.recruit.*;
import org.adpia.official.domain.recruit.repository.RecruitPostBlockRepository;
import org.adpia.official.domain.recruit.repository.RecruitPostLikeRepository;
import org.adpia.official.domain.recruit.repository.RecruitPostRepository;
import org.adpia.official.dto.recruit.RecruitBlockRequest;
import org.adpia.official.dto.recruit.RecruitBlockResponse;
import org.adpia.official.dto.recruit.RecruitPostResponse;
import org.adpia.official.dto.recruit.RecruitPostUpsertRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecruitService {

	private final RecruitPostRepository postRepository;
	private final RecruitPostBlockRepository blockRepository;
	private final RecruitPostLikeRepository postLikeRepository;
	private final PasswordEncoder passwordEncoder;
	private final MemberRepository memberRepository;

	@Transactional
	public RecruitPostResponse createDraft(RecruitBoardCode boardCode, String title, Actor actor) {
		validateCreatePermission(boardCode, actor);

		boolean isGuest = actor.isGuest();

		RecruitPost post = RecruitPost.builder()
			.boardCode(boardCode)
			.status(RecruitPostStatus.DRAFT)
			.title((title == null || title.isBlank()) ? "제목 없음" : title.trim())
			.authorType(isGuest ? RecruitAuthorType.GUEST : RecruitAuthorType.MEMBER)
			.authorMemberId(isGuest ? null : actor.memberId())
			.authorName(isGuest ? "GUEST" : actor.displayName())
			.secret(false)
			.commentEnabled(boardCode.isCommentEnabledByDefault())
			.likeEnabled(true)
			.pinned(false)
			.viewCount(0)
			.build();

		RecruitPost saved = postRepository.save(post);

		String displayAuthor = resolveDisplayAuthorName(saved);
		return RecruitPostResponse.from(saved, List.of(), false, displayAuthor, false);
	}

	@Transactional
	public RecruitPostResponse publish(Long postId, RecruitPostUpsertRequest req, Actor actor, String guestPasswordOrNull) {
		RecruitPost post = postRepository.findByIdAndDeletedAtIsNull(postId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

		String guestPw = resolveGuestPassword(post, req, guestPasswordOrNull);

		boolean isGuestAuthor = post.getAuthorType() == RecruitAuthorType.GUEST;
		boolean isFirstPublishFromDraft = post.getStatus() == RecruitPostStatus.DRAFT;

		if (isGuestAuthor && actor.isGuest() && isFirstPublishFromDraft) {
			if (!Boolean.TRUE.equals(req.getSecret())) {
				throw new IllegalStateException("외부 작성 글은 비밀글로만 작성할 수 있습니다.");
			}
			post.setSecret(true);
			post.setSecretPasswordHash(passwordEncoder.encode(requirePassword(guestPw)));
		} else {
			validateUpdateDeletePermission(post, actor, guestPw);
			applySecretPolicy(post, req);
		}

		post.setTitle(requireTitle(req.getTitle()));
		post.setCommentEnabled(post.getBoardCode().isCommentEnabledByDefault());

		List<RecruitBlockRequest> blocks = (req.getBlocks() == null) ? List.of() : req.getBlocks();
		validateBlocksForPublish(blocks);

		blockRepository.deleteByPostId(postId);
		saveBlocks(postId, blocks);

		post.setStatus(RecruitPostStatus.PUBLISHED);

		return get(postId, actor, req.getPassword());
	}

	@Transactional
	public RecruitPostResponse create(RecruitBoardCode boardCode, RecruitPostUpsertRequest req, Actor actor) {
		RecruitPostResponse draft = createDraft(boardCode, req.getTitle(), actor);
		return publish(draft.getId(), req, actor, req.getPassword());
	}

	@Transactional
	public RecruitPostResponse get(Long postId, Actor actor, String passwordOrNull) {
		RecruitPost post = postRepository.findByIdAndDeletedAtIsNull(postId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

		validateReadPermission(post.getBoardCode(), actor);

		if (post.getStatus() == RecruitPostStatus.DRAFT) {
			if (!canReadDraft(post, actor)) {
				throw new IllegalArgumentException("존재하지 않는 게시글입니다.");
			}
		} else {
			postRepository.incrementViewCount(postId);
		}

		boolean locked = isLockedForRead(post, actor, passwordOrNull);

		List<RecruitBlockResponse> blocks = locked
			? List.of()
			: blockRepository.findByPostIdOrderBySortOrderAsc(postId)
			.stream()
			.map(RecruitBlockResponse::from)
			.toList();

		String displayAuthor = resolveDisplayAuthorName(post);

		boolean likedByMe = false;
		if (!actor.isGuest()) {
			likedByMe = postLikeRepository.existsByPostIdAndMemberId(post.getId(), actor.memberId());
		}

		return RecruitPostResponse.from(post, blocks, locked, displayAuthor, likedByMe);
	}

	@Transactional(readOnly = true)
	public Page<RecruitPostResponse> list(RecruitBoardCode boardCode, Pageable pageable, Actor actor) {
		validateReadPermission(boardCode, actor);

		Page<RecruitPost> page = postRepository
			.findByBoardCodeAndStatusAndDeletedAtIsNullOrderByPinnedDescPinnedAtDescCreatedAtDesc(
				boardCode,
				RecruitPostStatus.PUBLISHED,
				pageable
			);

		return page.map(p -> {
			String displayAuthor = resolveDisplayAuthorName(p);
			return RecruitPostResponse.from(p, List.of(), false, displayAuthor, false);
		});
	}

	@Transactional
	public RecruitPostResponse update(Long postId, RecruitPostUpsertRequest req, Actor actor, String guestPasswordOrNull) {
		RecruitPost post = postRepository.findByIdAndDeletedAtIsNull(postId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

		String guestPw = resolveGuestPassword(post, req, guestPasswordOrNull);
		validateUpdateDeletePermission(post, actor, guestPw);

		post.setTitle(requireTitle(req.getTitle()));
		applySecretPolicy(post, req);
		post.setCommentEnabled(post.getBoardCode().isCommentEnabledByDefault());

		List<RecruitBlockRequest> blocks = (req.getBlocks() == null) ? List.of() : req.getBlocks();
		validateBlocksForPublish(blocks);

		blockRepository.deleteByPostId(postId);
		saveBlocks(postId, blocks);

		return get(postId, actor, req.getPassword());
	}

	@Transactional
	public void delete(Long postId, Actor actor, String guestPasswordOrNull) {
		RecruitPost post = postRepository.findByIdAndDeletedAtIsNull(postId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

		String guestPw = guestPasswordOrNull;
		validateUpdateDeletePermission(post, actor, guestPw);

		post.setDeletedAt(LocalDateTime.now());
	}

	@Transactional
	public void updatePinned(Long postId, boolean pinned, Actor actor) {
		RecruitPost post = postRepository.findByIdAndDeletedAtIsNull(postId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

		if (!post.getBoardCode().isPinnable()) {
			throw new IllegalStateException("고정 설정이 불가능한 게시판입니다.");
		}

		if (actor.isGuest()) {
			throw new IllegalStateException("권한이 없습니다.");
		}
		if (!(actor.role() == MemberRole.ROLE_SUPER_ADMIN || actor.role() == MemberRole.ROLE_PRESIDENT)) {
			throw new IllegalStateException("고정 설정 권한이 없습니다.");
		}

		post.setPinned(pinned);
		post.setPinnedAt(pinned ? LocalDateTime.now() : null);
	}

	private void validateCreatePermission(RecruitBoardCode boardCode, Actor actor) {
		if (boardCode.canGuestCreate()) {
			return;
		}

		if (actor.isGuest()) {
			throw new IllegalStateException("해당 게시판은 로그인 후 작성할 수 있습니다.");
		}

		if (boardCode.isAdminWriteOnly()) {
			if (!(actor.role() == MemberRole.ROLE_SUPER_ADMIN || actor.role() == MemberRole.ROLE_PRESIDENT)) {
				throw new IllegalStateException("해당 게시판 작성 권한이 없습니다.");
			}
		}
	}

	private void validateReadPermission(RecruitBoardCode boardCode, Actor actor) {
		if (boardCode.allowGuestRead()) {
			return;
		}

		if (actor.isGuest()) {
			throw new IllegalStateException("로그인 후 접근할 수 있습니다.");
		}
	}

	private void validateUpdateDeletePermission(RecruitPost post, Actor actor, String guestPasswordOrNull) {
		if (!actor.isGuest() && actor.role() == MemberRole.ROLE_SUPER_ADMIN) return;

		if (post.getAuthorType() == RecruitAuthorType.MEMBER) {
			if (actor.isGuest()) throw new IllegalStateException("권한이 없습니다.");
			if (post.getAuthorMemberId() == null || !post.getAuthorMemberId().equals(actor.memberId())) {
				throw new IllegalStateException("본인 글만 수정/삭제할 수 있습니다.");
			}
			return;
		}

		if (post.getAuthorType() == RecruitAuthorType.GUEST) {
			if (!post.isSecret()) {
				throw new IllegalStateException("외부 작성 글은 비밀글로만 수정/삭제할 수 있습니다.");
			}
			String raw = requirePassword(guestPasswordOrNull);
			if (post.getSecretPasswordHash() == null || !passwordEncoder.matches(raw, post.getSecretPasswordHash())) {
				throw new IllegalStateException("비밀번호가 일치하지 않습니다.");
			}
		}
	}

	private boolean canReadDraft(RecruitPost post, Actor actor) {
		if (!actor.isGuest() &&
			(actor.role() == MemberRole.ROLE_SUPER_ADMIN || actor.role() == MemberRole.ROLE_PRESIDENT)) {
			return true;
		}

		if (post.getAuthorType() == RecruitAuthorType.MEMBER
			&& !actor.isGuest()
			&& Objects.equals(post.getAuthorMemberId(), actor.memberId())) {
			return true;
		}

		return false;
	}

	private boolean isLockedForRead(RecruitPost post, Actor actor, String passwordOrNull) {
		if (!post.isSecret()) return false;

		if (!actor.isGuest() &&
			(actor.role() == MemberRole.ROLE_SUPER_ADMIN || actor.role() == MemberRole.ROLE_PRESIDENT)) {
			return false;
		}

		if (post.getAuthorType() == RecruitAuthorType.MEMBER
			&& !actor.isGuest()
			&& Objects.equals(post.getAuthorMemberId(), actor.memberId())) {
			return false;
		}

		if (post.getAuthorType() == RecruitAuthorType.GUEST) {
			if (passwordOrNull == null || passwordOrNull.isBlank()) return true;
			String hash = post.getSecretPasswordHash();
			if (hash == null || hash.isBlank()) return true;
			return !passwordEncoder.matches(passwordOrNull, hash);
		}

		return true;
	}

	private void validateBlocksForPublish(List<RecruitBlockRequest> blocks) {
		if (blocks == null || blocks.isEmpty()) return;
		for (RecruitBlockRequest b : blocks) {
			validateBlockForPublish(b);
		}
	}

	private void saveBlocks(Long postId, List<RecruitBlockRequest> blocks) {
		if (blocks == null || blocks.isEmpty()) return;

		for (RecruitBlockRequest b : blocks) {
			RecruitPostBlock entity = RecruitPostBlock.builder()
				.postId(postId)
				.sortOrder(b.getSortOrder())
				.type(b.getType())
				.text(b.getText())
				.url(b.getUrl())
				.meta(b.getMeta())
				.build();

			blockRepository.save(entity);
		}
	}

	private void validateBlockForPublish(RecruitBlockRequest b) {
		if (b.getType() == null) throw new IllegalArgumentException("block type이 필요합니다.");

		if (b.getType() == RecruitBlockType.TEXT) {
			if (b.getText() == null || b.getText().isBlank()) {
				throw new IllegalArgumentException("TEXT 블록은 text가 필요합니다.");
			}
		} else {
			if (b.getUrl() == null || b.getUrl().isBlank()) {
				throw new IllegalArgumentException(b.getType() + " 블록은 url이 필요합니다.");
			}
		}
	}

	private void applySecretPolicy(RecruitPost post, RecruitPostUpsertRequest req) {
		boolean nextSecret = Boolean.TRUE.equals(req.getSecret());
		post.setSecret(nextSecret);

		if (!nextSecret) {
			post.setSecretPasswordHash(null);
			return;
		}

		if (post.getAuthorType() == RecruitAuthorType.GUEST) {
			post.setSecretPasswordHash(passwordEncoder.encode(requirePassword(req.getPassword())));
			return;
		}

		if (req.getPassword() != null && !req.getPassword().isBlank()) {
			post.setSecretPasswordHash(passwordEncoder.encode(req.getPassword().trim()));
		} else {
			post.setSecretPasswordHash(null);
		}
	}

	private String resolveGuestPassword(RecruitPost post, RecruitPostUpsertRequest req, String guestPasswordOrNull) {
		String guestPw = guestPasswordOrNull;
		if ((guestPw == null || guestPw.isBlank()) && post.getAuthorType() == RecruitAuthorType.GUEST) {
			guestPw = req.getPassword();
		}
		return guestPw;
	}

	private String requirePassword(String pw) {
		if (pw == null || pw.isBlank()) throw new IllegalArgumentException("비밀번호가 필요합니다.");
		return pw;
	}

	private String requireTitle(String title) {
		if (title == null || title.isBlank()) throw new IllegalArgumentException("제목이 필요합니다.");
		return title.trim();
	}

	@SuppressWarnings("unused")
	private String requireGuestName(String name) {
		if (name == null || name.isBlank()) throw new IllegalArgumentException("작성자 이름이 필요합니다.");
		return name;
	}

	private String resolveDisplayAuthorName(RecruitPost post) {
		if (post.getAuthorType() == RecruitAuthorType.GUEST) {
			return post.getAuthorName();
		}

		Long memberId = post.getAuthorMemberId();
		if (memberId == null) return post.getAuthorName();

		return memberRepository.findById(memberId)
			.map(this::formatMemberDisplayName)
			.orElse(post.getAuthorName());
	}

	private String formatMemberDisplayName(Member m) {
		String name = safe(m.getName());
		String department = safe(m.getDepartment());

		String gen = (m.getGeneration() <= 0) ? " " : (m.getGeneration() + "기");

		String result = String.format("%s %s %s", gen, department, name).trim();
		return result.isBlank() ? "Member" : result;
	}

	private String safe(String v) {
		return v == null ? "" : v.trim();
	}

	public record Actor(Long memberId, String displayName, MemberRole role) {
		public boolean isGuest() { return memberId == null; }
		public static Actor guest() { return new Actor(null, "GUEST", null); }
	}
}