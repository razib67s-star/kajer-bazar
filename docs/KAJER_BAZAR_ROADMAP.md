# Kajer Bazar — Restart & Feature Roadmap

## Restart guardrails
- Confirm the exact source commit associated with CI run #236 before treating it as the application code base.
- Do not use runs #237 or #238 as the baseline.
- Diagnose and fix the reported email/password sign-in issue (including the unexpected Downloads-folder launch) before feature expansion.
- Validate every change with build, install/launch, and focused regression checks.

## Product direction
Kajer Bazar combines a social feed, job/service marketplace, product listings, and short video.

## Planned modules
1. Authentication and session recovery: email/password sign-in, signup, logout, saved sessions, clear errors; investigate Downloads launch symptom.
2. Social feed: post text/photos/videos, feed, likes, comments, shares, saves, follow/unfollow.
3. Profiles: avatar/cover, bio, skills, work portfolio, follower/following counts, profile posts.
4. Jobs and services: categories, searchable listings, filters, employer/worker details, applications, job alerts.
5. Marketplace: item listings, price, photos, seller profile, contact and report controls.
6. Camera and media: capture photo/video, gallery picker, upload progress, media validation and retry.
7. Messaging and notifications: conversations, image messages, follow/like/comment/job/order notifications.
8. Safety and trust: block/report, spam controls, admin moderation, verification request and reviews.
9. Promotion and monetization (later): promoted posts, business pages, ads, commission and premium features—only after core functionality and policy/payment requirements are reviewed.

## UI/navigation
Bottom navigation proposal: Home, Jobs, Create/Post, Videos, Profile.
Drawer/secondary navigation: Marketplace, Messages, Notifications, Services, Community/Safety, Settings, Help.

## Milestones
- M0: identify run #236 commit and repository baseline; preserve a separate restart branch.
- M1: login regression diagnosis and fix.
- M2: profiles and social feed.
- M3: jobs/services/marketplace.
- M4: media capture, video, messaging, notifications.
- M5: safety/admin, ratings, verification.
- M6: QA, signed installable APK, release notes.

## QA checklist
- Login/signup/session/logout and network-error handling.
- Ensure login does not open Downloads or unexpected external activity.
- Build and install; launch and navigation smoke test.
- CRUD and persistence checks for posts, profile, job listings, marketplace.
- Upload/camera/gallery permission and failure/retry checks.
- Messaging/notification checks.
- Report/block/moderation tests.
- Re-test regressions before sharing APK.

## Status
Roadmap document and feature branch created. The exact run #236 source commit is not yet confirmed; no application-code changes or APK build are claimed in this roadmap commit.
