package org.adpia.official.domain.recruit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.adpia.official.domain.recruit.RecruitBoardCode;
import org.adpia.official.domain.recruit.service.RecruitService;
import org.adpia.official.domain.recruit.service.RecruitService.Actor;
import org.adpia.official.dto.recruit.RecruitDraftCreateRequest;
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
@RequestMapping("/api/recruit")
public class RecruitController {

	private final RecruitService recruitService;
	private final ActorResolver actorResolver;

	@GetMapping("/{boardCode}/posts")
	public Page<RecruitPostResponse> list(
		@PathVariable RecruitBoardCode boardCode,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size
	) {
		Actor actor = actorResolver.resolveOrGuest();
		return recruitService.list(boardCode, PageRequest.of(page, size), actor);
	}

	@GetMapping("/posts/{id}")
	public RecruitPostResponse get(
		@PathVariable Long id,
		@RequestParam(required = false) String password
	) {
		Actor actor = actorResolver.resolveOrGuest();
		return recruitService.get(id, actor, password);
	}

	// (선택) 예전: 한번에 발행 작성
	@PostMapping("/{boardCode}/posts")
	public RecruitPostResponse create(
		@PathVariable RecruitBoardCode boardCode,
		@Valid @RequestBody RecruitPostUpsertRequest req
	) {
		Actor actor = actorResolver.resolveOrGuest();
		return recruitService.create(boardCode, req, actor);
	}

	@PatchMapping("/posts/{id}")
	public RecruitPostResponse update(
		@PathVariable Long id,
		@Valid @RequestBody RecruitPostUpsertRequest req,
		@RequestParam(required = false) String password
	) {
		Actor actor = actorResolver.resolveOrGuest();
		return recruitService.update(id, req, actor, password);
	}

	@DeleteMapping("/posts/{id}")
	public ResponseEntity<Void> delete(
		@PathVariable Long id,
		@RequestParam(required = false) String password
	) {
		Actor actor = actorResolver.resolveOrGuest();
		recruitService.delete(id, actor, password);
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

	@PostMapping("/{boardCode}/draft")
	public RecruitPostResponse createDraft(
		@PathVariable RecruitBoardCode boardCode,
		@RequestBody RecruitDraftCreateRequest req
	) {
		Actor actor = actorResolver.resolveOrGuest();
		return recruitService.createDraft(boardCode, req.getTitle(), actor);
	}

	@PostMapping("/posts/{id}/publish")
	public RecruitPostResponse publish(
		@PathVariable Long id,
		@Valid @RequestBody RecruitPostUpsertRequest req,
		@RequestParam(required = false) String password
	) {
		Actor actor = actorResolver.resolveOrGuest();
		return recruitService.publish(id, req, actor, password);
	}
}
