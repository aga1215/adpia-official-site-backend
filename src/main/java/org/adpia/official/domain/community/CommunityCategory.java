package org.adpia.official.domain.community;

import org.adpia.official.domain.recruit.RecruitBoardCode;

import lombok.Getter;

public enum CommunityCategory {
	NOTICE("notice", RecruitBoardCode.COMMUNITY_NOTICE),
	ADCHANCE("adchance", RecruitBoardCode.AD_CHANCE),
	ACTIVITY("activity", RecruitBoardCode.ACTIVITY_PHOTO),
	OB("ob", RecruitBoardCode.OB_BOARD);

	private final String path;
	@Getter
	private final RecruitBoardCode boardCode;

	CommunityCategory(String path, RecruitBoardCode boardCode) {
		this.path = path;
		this.boardCode = boardCode;
	}

	public static CommunityCategory from(String value) {
		for (CommunityCategory category : values()) {
			if (category.path.equalsIgnoreCase(value)) {
				return category;
			}
		}
		throw new IllegalArgumentException("지원하지 않는 커뮤니티 카테고리입니다: " + value);
	}
}