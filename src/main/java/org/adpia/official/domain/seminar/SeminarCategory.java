package org.adpia.official.domain.seminar;

import org.adpia.official.domain.recruit.RecruitBoardCode;

import lombok.Getter;

public enum SeminarCategory {
	ALL("all", RecruitBoardCode.SEMINAR),
	ACADEMIC("academic", RecruitBoardCode.ACADEMIC),
	OPERATION("operation", RecruitBoardCode.OPERATION);

	private final String path;
	@Getter
	private final RecruitBoardCode boardCode;

	SeminarCategory(String path, RecruitBoardCode boardCode) {
		this.path = path;
		this.boardCode = boardCode;
	}

	public static SeminarCategory from(String value) {
		for (SeminarCategory category : values()) {
			if (category.path.equalsIgnoreCase(value)) {
				return category;
			}
		}
		throw new IllegalArgumentException("지원하지 않는 세미나 카테고리입니다: " + value);
	}
}