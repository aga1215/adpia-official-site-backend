package org.adpia.official.domain.recruit;

public enum RecruitBoardCode {
	NOTICE,
	QA,
	NEWS,

	HUNDRED_QNA,
	THREE_MIN_SPEECH,

	SEMINAR,
	ACADEMIC,
	OPERATION,

	COMMUNITY_NOTICE,
	AD_CHANCE,
	ACTIVITY_PHOTO,
	OB_BOARD;

	public boolean isPinnable() {
		return true;
	}

	public boolean isCommentEnabledByDefault() {
		return switch (this) {
			case NOTICE, NEWS, SEMINAR, ACADEMIC, OPERATION,
				 COMMUNITY_NOTICE, AD_CHANCE -> false;

			case QA, HUNDRED_QNA, THREE_MIN_SPEECH,
				 ACTIVITY_PHOTO, OB_BOARD -> true;
		};
	}

	public boolean canGuestCreate() {
		return this == QA;
	}

	public boolean isAdminWriteOnly() {
		return this == NOTICE
			|| this == NEWS
			|| this == SEMINAR
			|| this == ACADEMIC
			|| this == OPERATION
			|| this == COMMUNITY_NOTICE
			|| this == AD_CHANCE
			|| this == ACTIVITY_PHOTO;
	}

	public boolean allowGuestRead() {
		return this == NOTICE || this == NEWS || this == QA;
	}
}