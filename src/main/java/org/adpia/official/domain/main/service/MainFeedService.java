package org.adpia.official.domain.main.service;

import lombok.RequiredArgsConstructor;
import org.adpia.official.domain.recruit.RecruitBlockType;
import org.adpia.official.domain.recruit.RecruitBoardCode;
import org.adpia.official.domain.recruit.RecruitPost;
import org.adpia.official.domain.recruit.RecruitPostBlock;
import org.adpia.official.domain.recruit.RecruitPostStatus;
import org.adpia.official.domain.recruit.repository.RecruitPostBlockRepository;
import org.adpia.official.domain.recruit.repository.RecruitPostRepository;
import org.adpia.official.dto.main.MainActivityFeedResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MainFeedService {

	private final RecruitPostRepository recruitPostRepository;
	private final RecruitPostBlockRepository recruitPostBlockRepository;

	@Transactional(readOnly = true)
	public MainActivityFeedResponse getActivityFeed() {
		List<RecruitPost> posts = recruitPostRepository
			.findByBoardCodeAndStatusAndDeletedAtIsNullOrderByPinnedDescPinnedAtDescCreatedAtDesc(
				RecruitBoardCode.ACTIVITY_PHOTO,
				RecruitPostStatus.PUBLISHED,
				PageRequest.of(0, 20)
			)
			.getContent();

		if (posts.isEmpty()) {
			return MainActivityFeedResponse.builder()
				.main(null)
				.side(List.of())
				.build();
		}

		RecruitPost mainPost = posts.get(0);
		Long mainPostId = mainPost.getId();

		List<MainActivityFeedResponse.ActivityCard> side = new ArrayList<>();
		for (RecruitPost post : posts) {
			if (post.getId().equals(mainPostId)) {
				continue;
			}
			side.add(toCard(post));
			if (side.size() == 2) {
				break;
			}
		}

		return MainActivityFeedResponse.builder()
			.main(toCard(mainPost))
			.side(side)
			.build();
	}

	private MainActivityFeedResponse.ActivityCard toCard(RecruitPost post) {
		String thumbnailUrl = recruitPostBlockRepository.findByPostIdOrderBySortOrderAsc(post.getId())
			.stream()
			.filter(block -> block.getType() == RecruitBlockType.IMAGE)
			.map(RecruitPostBlock::getUrl)
			.filter(url -> url != null && !url.isBlank())
			.findFirst()
			.orElse(null);

		return MainActivityFeedResponse.ActivityCard.builder()
			.id(post.getId())
			.title(post.getTitle())
			.thumbnailUrl(thumbnailUrl)
			.createdAt(post.getCreatedAt() != null ? post.getCreatedAt().toString() : null)
			.build();
	}
}