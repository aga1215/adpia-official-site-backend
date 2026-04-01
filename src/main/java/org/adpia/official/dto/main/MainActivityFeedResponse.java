package org.adpia.official.dto.main;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MainActivityFeedResponse {
	private ActivityCard main;
	private List<ActivityCard> side;

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class ActivityCard {
		private Long id;
		private String title;
		private String thumbnailUrl;
		private String createdAt;
	}
}