package org.adpia.official.domain.seminar.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.adpia.official.domain.recruit.RecruitBoardCode;
import org.adpia.official.domain.recruit.service.RecruitLikeService;
import org.adpia.official.domain.recruit.service.RecruitService;
import org.adpia.official.domain.recruit.service.RecruitService.Actor;
import org.adpia.official.domain.seminar.SeminarCategory;
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
@RequestMapping("/api/seminar/{category}")
public class SeminarController {

	private final RecruitService recruitService;
	private final RecruitLikeService recruitLikeService;
	private final ActorResolver actorResolver;

	private RecruitBoardCode resolveBoardCode(String category) {
		return SeminarCategory.from(category).getBoardCode();
	}

	@GetMapping("/posts")
	public Page<RecruitPostResponse> list(
		@PathVariable String category,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size
	) {
		Actor actor = actorResolver.resolveOrGuest();
		return recruitService.list(resolveBoardCode(category), PageRequest.of(page, size), actor);
	}

	@GetMapping("/posts/{id}")
	public RecruitPostResponse get(
		@PathVariable String category,
		@PathVariable Long id
	) {
		Actor actor = actorResolver.resolveOrGuest();
		return recruitService.get(id, actor, null);
	}

	@PostMapping("/posts")
	public RecruitPostResponse create(
		@PathVariable String category,
		@Valid @RequestBody RecruitPostUpsertRequest req
	) {
		Actor actor = actorResolver.resolveOrGuest();
		return recruitService.create(resolveBoardCode(category), req, actor);
	}

	@PatchMapping("/posts/{id}")
	public RecruitPostResponse update(
		@PathVariable String category,
		@PathVariable Long id,
		@Valid @RequestBody RecruitPostUpsertRequest req
	) {
		Actor actor = actorResolver.resolveOrGuest();
		return recruitService.update(id, req, actor, null);
	}

	@DeleteMapping("/posts/{id}")
	public ResponseEntity<Void> delete(
		@PathVariable String category,
		@PathVariable Long id
	) {
		Actor actor = actorResolver.resolveOrGuest();
		recruitService.delete(id, actor, null);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/posts/{id}/pin")
	public ResponseEntity<Void> pin(
		@PathVariable String category,
		@PathVariable Long id,
		@Valid @RequestBody RecruitPostPinRequest req
	) {
		Actor actor = actorResolver.resolveOrGuest();
		recruitService.updatePinned(id, req.getPinned(), actor);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/posts/{id}/like")
	public ResponseEntity<Void> likePost(
		@PathVariable String category,
		@PathVariable Long id
	) {
		Actor actor = actorResolver.resolveOrGuest();
		recruitLikeService.likePost(id, actor);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/posts/{id}/like")
	public ResponseEntity<Void> unlikePost(
		@PathVariable String category,
		@PathVariable Long id
	) {
		Actor actor = actorResolver.resolveOrGuest();
		recruitLikeService.unlikePost(id, actor);
		return ResponseEntity.noContent().build();
	}
}