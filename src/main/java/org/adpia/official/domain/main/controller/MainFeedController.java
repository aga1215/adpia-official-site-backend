package org.adpia.official.domain.main.controller;

import lombok.RequiredArgsConstructor;
import org.adpia.official.domain.main.service.MainFeedService;
import org.adpia.official.dto.main.MainActivityFeedResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/main")
public class MainFeedController {

	private final MainFeedService mainFeedService;

	@GetMapping("/activity-feed")
	public MainActivityFeedResponse getActivityFeed() {
		return mainFeedService.getActivityFeed();
	}
}