# ADPIA Official Website - Backend

ADPIA 공식 홈페이지의 백엔드 서버입니다.  
회원 인증, 게시판 정책, 관리자 기능, 비회원 Q&A, 파일 업로드를 담당합니다.

> Service: https://adpia.or.kr
> Frontend REPO: https://github.com/aga1215/adpia-official-site-frontend
---

## Overview

ADPIA Official Website는 대학생 광고 연합 동아리 ADPIA의 소개, 모집, Q&A, 커뮤니티, 아카이브 운영을 위한 실서비스 웹 플랫폼입니다.

Backend는 Spring Boot 기반 REST API 서버로, 인증/인가, 게시판 도메인, 관리자 기능, S3 업로드 흐름을 처리합니다.

---

## Role

- 1인 풀스택 개발 및 배포/운영
- Spring Boot 기반 REST API 설계 및 구현
- JWT + Redis 기반 인증 구조 구현
- 게시판별 권한/댓글/비밀글 정책 설계
- S3 Presigned URL 기반 파일 업로드 구현
- Docker, Nginx, GitHub Actions 기반 운영 환경 구성

---

## Tech Stack

### Backend

- Java 17
- Spring Boot 3.x
- Spring Security
- JPA / Hibernate
- PostgreSQL
- Redis
- AWS S3 Presigned URL

### Infra

- Docker / Docker Compose
- Nginx
- GitHub Actions
- Ubuntu VPS
- Let's Encrypt

---

## Main Features

- JWT 기반 회원가입 / 로그인
- Redis 기반 Refresh Token 관리 및 로그아웃 처리
- 관리자 회원 관리
- 게시판별 작성 권한, 댓글 허용, 비밀글 정책 분리
- 비회원 Q&A 작성 및 비밀번호 기반 비밀글 열람
- 블록 기반 게시글 본문 관리
- S3 Presigned URL 기반 이미지 / 파일 업로드
- 운영 환경 배포 및 HTTPS 적용

---

## Architecture

```txt
Client
  |
  v
Nginx
  |
  v
Spring Boot Backend
  |
  |-- PostgreSQL
  |-- Redis
  |-- AWS S3
```

---

## Domain Design

### Member

회원 권한은 등급에 따라 관리합니다.

```txt
활동기수 / OB -> ROLE_USER
회장단 -> ROLE_PRESIDENT
마스터 -> ROLE_SUPER_ADMIN
```

### Board

게시판은 하나의 게시글 도메인을 기반으로 하고, `boardCode`로 구분합니다.

예시 게시판:

```txt
NOTICE, QA, NEWS, ACTIVITY_PHOTO, OB_BOARD,
COMMUNITY_NOTICE, AD_CHANCE, SEMINAR, ACADEMIC, OPERATION
```

게시판마다 다음 정책을 다르게 적용합니다.

- 작성 권한
- 댓글 기본 허용 여부
- 비회원 작성 가능 여부
- 비밀글 허용 여부
- 상단 고정 가능 여부

---

## Auth Flow

```txt
1. 사용자가 로그인
2. 서버가 Access Token / Refresh Token 발급
3. Refresh Token은 Redis에 저장
4. 클라이언트는 Access Token으로 API 요청
5. 토큰 만료 시 Refresh API로 재발급
6. 로그아웃 시 토큰 무효화
```

---

## File Upload Flow

```txt
1. Frontend가 업로드할 파일 정보를 서버로 전송
2. Backend가 파일 타입을 검증하고 Presigned URL 발급
3. Frontend가 S3에 직접 업로드
4. 업로드된 S3 URL을 DB에 저장
```

서버를 거치지 않고 S3에 직접 업로드하도록 설계하여 VPS 서버의 부하를 줄였습니다.

---

## Technical Decisions

### 게시판별 정책 분리

ADPIA는 공지사항, Q&A, 활동사진, OB 게시판 등 게시판마다 접근 권한과 댓글 정책이 다릅니다.  
이를 Controller나 Service의 조건문으로 반복하지 않고, `BoardCode` 기준 정책으로 분리하여 유지보수성을 높였습니다.

### 비회원 Q&A와 비밀글 처리

비회원도 문의를 남길 수 있도록 하되, 개인정보 보호를 위해 비회원 게시글은 비밀글로 처리했습니다.  
비밀번호는 평문이 아닌 BCrypt 해시로 저장하고, 검증 후에만 본문을 열람할 수 있도록 구현했습니다.

### S3 Presigned URL 업로드

파일을 서버로 직접 업로드하면 운영 중인 VPS에 부담이 생길 수 있어, 백엔드는 Presigned URL만 발급하고 실제 업로드는 클라이언트가 S3로 직접 수행하도록 설계했습니다.

### 운영 환경 구성

실제 서비스 운영을 위해 Docker Compose, Nginx Reverse Proxy, HTTPS, GitHub Actions 기반 배포 흐름을 구성했습니다.

---

## Troubleshooting

- 게시판 enum 추가 시 PostgreSQL enum 마이그레이션 문제 해결
- 게시글 블록 meta 저장 과정에서 JSONB 타입 매핑 문제 해결
- S3 Presigned URL 업로드 시 Content-Type 불일치 문제 수정
- 운영 중 트래픽 증가로 인한 서버 응답 불안정 상황 점검 및 개선 방향 수립
