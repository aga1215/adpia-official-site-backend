package org.adpia.official.dto.recruit;

import java.time.LocalDateTime;
import java.util.List;

import lombok.*;
import org.adpia.official.domain.recruit.RecruitBoardCode;
import org.adpia.official.domain.recruit.RecruitPost;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruitPostResponse {

	private Long id;
	private RecruitBoardCode boardCode;
	private String title;

	private Long authorMemberId;
	private String authorType;
	private String authorName;

	private boolean secret;
	private boolean locked;
	private boolean pinned;
	private boolean commentEnabled;
	private boolean likeEnabled;
	private long likeCount;
	private boolean likedByMe;
	private int viewCount;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	private String thumbnailUrl;

	private List<RecruitBlockResponse> blocks;

	public static RecruitPostResponse from(RecruitPost post, List<RecruitBlockResponse> blocks) {
		return from(post, blocks, false, post.getAuthorName(), false, null);
	}

	public static RecruitPostResponse from(RecruitPost post, List<RecruitBlockResponse> blocks, boolean locked) {
		return from(post, blocks, locked, post.getAuthorName(), false, null);
	}

	public static RecruitPostResponse from(
		RecruitPost post,
		List<RecruitBlockResponse> blocks,
		boolean locked,
		String displayAuthorName
	) {
		return from(post, blocks, locked, displayAuthorName, false, null);
	}

	public static RecruitPostResponse from(
		RecruitPost post,
		List<RecruitBlockResponse> blocks,
		boolean locked,
		String displayAuthorName,
		boolean likedByMe
	) {
		return from(post, blocks, locked, displayAuthorName, likedByMe, null);
	}

	public static RecruitPostResponse from(
		RecruitPost post,
		List<RecruitBlockResponse> blocks,
		boolean locked,
		String displayAuthorName,
		boolean likedByMe,
		String thumbnailUrl
	) {
		return RecruitPostResponse.builder()
			.id(post.getId())
			.boardCode(post.getBoardCode())
			.title(post.getTitle())
			.authorMemberId(post.getAuthorMemberId())
			.authorType(post.getAuthorType().name())
			.authorName(displayAuthorName)
			.secret(post.isSecret())
			.locked(locked)
			.pinned(post.isPinned())
			.commentEnabled(post.isCommentEnabled())
			.likeEnabled(post.isLikeEnabled())
			.likeCount(post.getLikeCount())
			.likedByMe(likedByMe)
			.viewCount(post.getViewCount())
			.createdAt(post.getCreatedAt())
			.updatedAt(post.getUpdatedAt())
			.thumbnailUrl(thumbnailUrl)
			.blocks(blocks)
			.build();
	}
}