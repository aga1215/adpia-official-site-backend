package org.adpia.official.domain.hundred_qna.controller;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.adpia.official.domain.recruit.RecruitBoardCode;
import org.adpia.official.domain.recruit.service.RecruitLikeService;
import org.adpia.official.domain.recruit.service.RecruitService;
import org.adpia.official.domain.recruit.service.RecruitService.Actor;
import org.adpia.official.domain.recruit.service.RecruitStatsService;
import org.adpia.official.dto.recruit.HundredQnaCommentStatResponse;
import org.adpia.official.dto.recruit.RecruitPostPinRequest;
import org.adpia.official.dto.recruit.RecruitPostResponse;
import org.adpia.official.dto.recruit.RecruitPostUpsertRequest;
import org.adpia.official.security.ActorResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hundred-qna")
public class HundredQnaController {

	private final RecruitService recruitService;
	private final ActorResolver actorResolver;
	private final RecruitLikeService recruitLikeService;
	private final RecruitStatsService recruitStatsService;

	@GetMapping("/posts")
	public Page<RecruitPostResponse> list(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size
	) {
		Actor actor = actorResolver.resolveOrGuest();
		return recruitService.list(RecruitBoardCode.HUNDRED_QNA, PageRequest.of(page, size), actor);
	}

	@GetMapping("/posts/{id}")
	public RecruitPostResponse get(@PathVariable Long id) {
		Actor actor = actorResolver.resolveOrGuest();
		return recruitService.get(id, actor, null);
	}

	@PostMapping("/posts")
	public RecruitPostResponse create(@Valid @RequestBody RecruitPostUpsertRequest req) {
		Actor actor = actorResolver.resolveOrGuest();
		return recruitService.create(RecruitBoardCode.HUNDRED_QNA, req, actor);
	}

	@PatchMapping("/posts/{id}")
	public RecruitPostResponse update(
		@PathVariable Long id,
		@Valid @RequestBody RecruitPostUpsertRequest req
	) {
		Actor actor = actorResolver.resolveOrGuest();
		return recruitService.update(id, req, actor, null);
	}

	@DeleteMapping("/posts/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		Actor actor = actorResolver.resolveOrGuest();
		recruitService.delete(id, actor, null);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/posts/{id}/pin")
	public ResponseEntity<Void> pin(
		@PathVariable Long id,
		@Valid @RequestBody RecruitPostPinRequest req
	) {
		Actor actor = actorResolver.resolveOrGuest();
		recruitService.updatePinned(id, req.getPinned(), actor);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/posts/{id}/like")
	public ResponseEntity<Void> likePost(@PathVariable Long id) {
		Actor actor = actorResolver.resolveOrGuest();
		recruitLikeService.likePost(id, actor);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/posts/{id}/like")
	public ResponseEntity<Void> unlikePost(@PathVariable Long id) {
		Actor actor = actorResolver.resolveOrGuest();
		recruitLikeService.unlikePost(id, actor);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/comment-stats")
	public List<HundredQnaCommentStatResponse> getCommentStats() {
		Actor actor = actorResolver.resolveOrGuest();
		return recruitStatsService.getHundredQnaCommentStats(actor);
	}
}