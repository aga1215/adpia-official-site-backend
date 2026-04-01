package org.adpia.official.domain.recruit;

import lombok.Getter;

@Getter
public enum RecruitBoardCode {
	NOTICE,
	QA,
	NEWS,
	HUNDRED_QNA,
	THREE_MIN_SPEECH,
	SEMINAR,
	ACADEMIC,
	OPERATION;

	public boolean isPinnable() {
		return true;
	}

	public boolean isCommentEnabledByDefault() {
		return switch (this) {
			case NOTICE, SEMINAR, ACADEMIC,OPERATION -> false;
			case QA, NEWS, HUNDRED_QNA, THREE_MIN_SPEECH -> true;
		};
	}

	public boolean canGuestCreate() {
		return this == QA;
	}

	public boolean isAdminWriteOnly() {
		return this == NOTICE || this == NEWS;
	}

	public boolean allowGuestRead() {
		return switch (this) {
			case NOTICE, QA, NEWS -> true;
			case HUNDRED_QNA, THREE_MIN_SPEECH, SEMINAR, ACADEMIC, OPERATION -> false;
		};
	}
}