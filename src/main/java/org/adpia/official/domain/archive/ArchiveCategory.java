package org.adpia.official.domain.archive;

import org.adpia.official.domain.recruit.RecruitBoardCode;

import lombok.Getter;

public enum ArchiveCategory {
	COMPETITION_PT("competition-pt", RecruitBoardCode.COMPETITION_PT),
	SOCIAL_PROJECT("social-project", RecruitBoardCode.SOCIAL_PROJECT),
	AD_CONTEST("ad-contest", RecruitBoardCode.AD_CONTEST),
	AD_INTRODUCTION("ad-introduction", RecruitBoardCode.AD_INTRODUCTION),

	HUNDRED_QNA("hundred-qna", RecruitBoardCode.HUNDRED_QNA),
	THREE_MIN_SPEECH("three-minute-speech", RecruitBoardCode.THREE_MIN_SPEECH);

	private final String path;
	@Getter
	private final RecruitBoardCode boardCode;

	ArchiveCategory(String path, RecruitBoardCode boardCode) {
		this.path = path;
		this.boardCode = boardCode;
	}

	public static ArchiveCategory from(String value) {
		for (ArchiveCategory category : values()) {
			if (category.path.equalsIgnoreCase(value)) {
				return category;
			}
		}
		throw new IllegalArgumentException("지원하지 않는 ARCHIVE 카테고리입니다: " + value);
	}
}