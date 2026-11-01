# Development progress

## Phase 1 — Project inspection and setup

### Completed

- Inspected the target workspace; it contained no existing Android project, source files, Gradle configuration, or Git repository.
- Created a Kotlin Android application using Jetpack Compose and Material 3, with a Gradle wrapper and centralized version catalog.
- Added an initial neutral Compose theme, launch shell, architecture notes, README, and secret/configuration ignores.

### Verification

- `:app:assembleDebug` succeeded with JDK 21 and Android SDK 36.

## Phase 2 — Authentication and profile system

### Completed

- Added Firebase Auth and Cloud Firestore dependencies using Firebase BoM 35.1.0 and the Google Services plugin (conditionally applied when the real config file exists).
- Added email/password registration and login, logout, password reset, persistent auth-state observation, validation, loading states, and Firebase error messages.
- Added the requested UserProfile fields and a Firestore profile repository.
- Added transactional normalized username reservation and profile creation, as well as atomic username claim transfer during profile update.
- Added rollback of a just-created Auth account if profile creation fails.
- Added login, registration, reset, profile setup, authenticated home, and auth-aware startup screens.
- Added `firestore.rules` and documented the collections, ownership checks, and Firebase setup steps.
- Added unit tests for auth validation, username normalization, auth state changes, profile creation, registration rollback, and login error propagation.

### Configuration required for live Firebase use

- No `app/google-services.json` exists in this workspace and no Firebase project credentials were supplied.
- Create/register the Android app ID `com.fable.app` in Firebase, download its `google-services.json` to `app/google-services.json`, enable Email/Password Authentication, create Firestore, and publish `firestore.rules`.
- The app builds without that file, but authentication actions report the missing configuration. Live Firebase behavior has not been tested and is not claimed as verified.

### Verification

- `:app:assembleDebug :app:testDebugUnitTest` — **BUILD SUCCESSFUL**.
- 7 JVM unit tests passed; 0 failures, 0 errors, 0 skipped.
- Build used JDK 21, Android SDK 36, Gradle 8.13, Kotlin 2.4.21. Gradle reports a forward-looking deprecation warning because a future Kotlin 2.5 release will require Gradle 8.14.4; it does not block this build.
- No emulator/device tests or live Firebase tests were run.

## Phase 3 — Book discovery engine

### Completed

- Added a source-qualified Book domain model, paging model, repository boundary, and search/discovery/detail use cases.
- Integrated Google Books volumes search and detail endpoints through Retrofit, with query modes for title, author, ISBN, subject, and general discovery.
- Added safe DTO parsing and mapping, query builder, optional API key support, request timeouts, quota/HTTP/network handling, and Room cache/history persistence.
- Added authenticated Discover, Search, Genre, and Book Details destinations; reusable book cards/covers/carousels/grids, genre chips, and loading/empty/error states.
- Implemented debounced typed search, recent searches, duplicate-safe pagination, cover fallbacks, metadata display, and preview links.
- Did not add production sample content or fabricated popularity values.
- Updated README and architecture notes with API and package setup.

### Configuration and live API limitation

- An optional `GOOGLE_BOOKS_API_KEY` can be supplied as a local Gradle property or environment variable. See README for setup and key restrictions.
- An anonymous request to Google Books returned HTTP 429 because the caller's daily quota was zero. Error and cache fallback behavior is implemented; a successful live response is not claimed.
- Firebase configuration is still absent, so live authentication remains unverified as documented in Phase 2.

### Verification

- `:app:assembleDebug :app:testDebugUnitTest` — **BUILD SUCCESSFUL**.
- 18 JVM unit tests passed; 0 failures, 0 errors, 0 skipped (11 Phase 3 tests and 7 Phase 2 tests).
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Build used JDK 21, Android SDK 36, Gradle 8.13, Kotlin 2.4.21. Gradle emitted the existing forward-looking warning that Kotlin 2.5 will require Gradle 8.14.4; this did not block the build.
- The build and unit tests do not validate live Firebase or successful Google Books connectivity; the anonymous Google Books request was rate-limited as noted above.

## Phase 4 — Personal library and reading tracker

### Completed

- Added `ReadingStatus`, `UserBook`, `ReadingProgress`, monthly activity, reading history, and `ReadingStatistics` domain models with pure shelf transition, progress validation, and statistics logic.
- Added a user-scoped Room library table with `(userId, bookId)` uniqueness, migration from Room schema v1, pending sync state, and soft-delete tombstones.
- Added Firestore DTO mapping and private `users/{uid}/library/{sha256(bookId)}` documents. User IDs come from the authenticated session; deterministic IDs and Room constraints prevent duplicate entries.
- Added local-first write queueing, Firestore snapshot reconciliation, remote listener support, and version-checked acknowledgements. A newer local edit remains pending if an older write finishes later; failed/unacknowledged changes stay in Room and display as waiting to sync.
- Added Home, My Library shelves, search, category filtering, sort by title/author/date, shelf moves/removal, book detail integration, reading page/notes editing, and statistics/history screen.
- Added owner-only Firestore rules for library reads/writes and field/page validation. No composite index is required for the user-scoped subcollection read.
- Added seven JVM tests for stable cloud identity, shelf deduplication/transitions, progress calculation and validation, completion history, record-derived statistics, and repository failure presentation.

### Cloud configuration and limits

- Firebase config is still absent; Firestore rules have not been deployed or checked against a live Firebase project. Live read/write, offline queue recovery, and multi-device synchronization are not claimed as verified.
- Unsynced changes are retained in Room with a visible pending marker. The Firestore Android SDK queues offline writes; the app clears Room's marker only when the corresponding edit version is acknowledged.
- Firestore library entries store a book metadata snapshot alongside progress to preserve details while offline; changes to Google Books metadata are reflected when the book is re-added, not continuously refreshed into a saved snapshot.

### Verification

- `:app:assembleDebug :app:testDebugUnitTest` — **BUILD SUCCESSFUL**.
- 25 JVM unit tests passed; 0 failures, 0 errors, 0 skipped (7 Phase 4 tests; 18 from earlier phases).
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Build used JDK 21, Android SDK 36, Gradle 8.13, and Kotlin 2.4.21. The existing Gradle forward-compatibility warning remains non-blocking.
- No Firebase project configuration or emulator was available. Firestore security rules, live cloud writes, and offline recovery were not tested against Firebase; no live cloud behavior is claimed as verified.

## Phase 5 — Reviews and ratings

### Completed

- Added `BookReview` and review-summary domain models, 1–5-star validation, edit/create date preservation, owner checks, and per-reviewer average/count calculations.
- Added a Room `book_reviews` table with `(userId, bookId)` primary key and a v2→v3 migration, plus public-book and private-user Firestore DTO mapping.
- Added Firestore review writes as atomic batches to `bookReviews/{sha256(bookId)}/reviews/{uid}` and `users/{uid}/reviews/{sha256(bookId)}`. Firestore rules allow signed-in reads of book reviews and restrict writes/deletes to their owner.
- Integrated rating/review create, update, delete, average/count, reviewer/date display, empty/loading/error state and pending-sync feedback into Book Details.
- Added My Reviews history from Home with navigation back to each book's details.
- Added unit tests covering review creation, edit date behavior, deletion, duplicate key, rating bounds, ownership, average/count/empty summaries and repository error presentation.

### Cloud configuration and limits

- Firebase config is absent and no live Firestore project is available. Review reads, rule enforcement, batch writes and offline recovery are not claimed as live-verified.
- Average and review count are derived from current review records on the client; there is no server-side aggregate or additional dependency.

### Verification

- `gradlew.bat assembleDebug testDebugUnitTest` — **BUILD SUCCESSFUL**.
- 35 JVM unit tests passed; 0 failures, 0 errors, 0 skipped (10 Phase 5 tests; 25 from Phases 1–4).
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Build used JDK 21, Android SDK 36, Gradle 8.13, and Kotlin 2.4.21. Gradle's Kotlin 2.5 forward-compatibility warning remains non-blocking.
- No Firebase project configuration or emulator was available. Firestore rules, batch writes, public review reads, and offline synchronization were not verified against a live Firebase project.

## Phase 6 — Social networking

### Completed

- Added an email-free `PublicReaderProfile` model and separate Firestore `publicProfiles/{uid}` mirror with username, display name, bio, HTTPS profile image, favourite genres, and aggregate reading statistics. Existing signed-in accounts are mirrored when their private profile is observed; edits remain owner-scoped through the existing profile repository.
- Added normalized username-prefix discovery, reader profiles, owner profile editing, friend requests, Friends, and Activity Feed screens integrated into the authenticated navigation and Home.
- Added mirrored incoming/outgoing friend request documents keyed by participant UIDs. Transactions prevent self/duplicate/already-friends requests, allow accept/reject/cancel, permit retry after rejection/cancellation, and create/remove mirrored friendships atomically.
- Added friend profile/stat observation and accepted-friend activity subscriptions. Friend statistics are aggregate values derived from synced personal library records; no friend library entries or personal notes are read by social screens.
- Added stable activity records for library additions, reading starts, 10% (or 10-page) progress milestones, completions, and review publication/updates. Activities are published after successful cloud writes and contain no personal notes. Stable event IDs prevent progress/review edits from producing repeated feed items.
- Added Firestore rules for public-profile field allowlists, owner edits, request participant/state validation, reciprocal friendship creation/removal, and accepted-friend activity access. Social data is cloud-only; Room remains schema v3 and no migration was needed.
- Added 13 JVM unit tests for request creation/deduplication and transitions, self/ownership checks, friendship removal logic, public-profile privacy, activity generation/deduplication/filtering, and social error handling.

### Cloud configuration and limits

- No `app/google-services.json`, Firebase project, or emulator was available. Public profile mirrors, Firestore transactions/listeners, activity publishing, and the new security rules are not claimed as live-tested or deployed.
- Reader discovery is a signed-in prefix search over public profile fields, capped at 30 results. Friends' reading statistics are aggregate counts; detailed reading records remain private.
- Activity events are generated from confirmed Firestore library/review writes. Feed listeners merge up to 20 recent entries per friend and cap the visible feed at 100 entries.
- Social data is not cached locally in Room, so Phase 6 introduces no database schema or migration changes. With Firebase unavailable, the UI receives no fabricated readers, requests, friendships, or activity.

### Verification

- `gradlew.bat assembleDebug testDebugUnitTest` — **BUILD SUCCESSFUL**.
- 48 JVM unit tests passed; 0 failures, 0 errors, 0 skipped (13 Phase 6 tests; 35 from Phases 1–5).
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Build used JDK 21 and Android SDK 36. Existing non-blocking Gradle 8.13 forward-compatibility warning remains.
- Firestore rules and live Firebase behavior were not tested against a project or emulator.

### Remaining phases

Broader integration testing, release verification, and live Firebase/rules verification when project configuration is available.

## Phase 7 — Centralized theming system

### Completed

- Replaced the single shared palette with five complete theme definitions: Gothic, Gothic Romance, Dark Academia, Dreamscape, and Minimalist. Each defines a semantic Material 3 palette, heading/body typography, component shapes, page surface gradient, card border, and theme-specific accent token.
- Applied the selected theme at the root app level across signed-out authentication and all authenticated navigation destinations. Existing Compose screens consume the active `MaterialTheme`, so book discovery/details, personal library/progress, reviews, reader profiles, friends, and activity update together.
- Added a theme picker with preview cards built from the real theme definitions. The picker is reachable from sign-in, authenticated Home, and the owner's profile.
- Added per-user local persistence through SharedPreferences with an isolated anonymous preference for signed-out use. When Firebase is configured, choices also sync through the existing private `UserProfile.selectedTheme` field. Pending device selections remain active on cloud failure and are retried on a later profile/session restore.
- Kept `selectedTheme` backward-compatible: missing or unknown values restore as Minimalist. No Room schema, migration, Firestore rules, or public-profile fields needed to change.
- Added JVM tests for all theme definitions, legacy/default selection, account preference isolation, signed-out restore, local pending preference priority, profile sync, retry behavior, cloud failure handling, and local fallback messaging.
- Updated README and architecture documentation.

### Cloud configuration and limits

- No `app/google-services.json` or Firebase project is configured. The app's theme picker and device persistence work without Firebase; account-level profile sync and cross-device restoration are not live-verified.
- A cloud write failure leaves the chosen theme saved locally and records the pending sync state. Retry occurs when the profile/session is restored; automatic background retry while remaining in the same session is not implemented.
- JVM tests verify theme definitions and preference/view-model behavior, not Compose rendering on a device or deployed Firestore rules.

### Verification

- `gradlew.bat assembleDebug testDebugUnitTest` — **BUILD SUCCESSFUL**.
- 57 JVM unit tests passed; 0 failures, 0 errors, 0 skipped.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Build used JDK 21 and Android SDK 36. Gradle 8.13 emitted the existing non-blocking forward-compatibility warning for a future Kotlin 2.5 requirement.
- Firebase profile synchronization and all live Firebase features remain unverified without project configuration.

## Phase 8 — Integration audit and demo readiness

### Completed

- Audited the existing auth/profile, Google Books, Room cache, library/progress, review, social, activity, theme, navigation, repository, schema/migration, and Firestore rules paths without recreating the app or changing its database schema.
- Confirmed account-owned Room library/review rows and theme preferences use UID-scoped keys; the anonymous theme key is separate. Book metadata and search history remain device-wide caches.
- Fixed a review error-state defect: Firestore review snapshot listener errors were ignored and could look like an empty feed. Both book review and personal review listeners now propagate listener errors to the ViewModel. Previously received cached reviews remain in UI state when the ViewModel shows the error.
- Added a regression test for a review-feed failure after a cached review emission.
- Installed and launched the debug APK on the `Medium_Phone` emulator. The accessibility hierarchy showed the signed-out login UI and the Firebase setup notice; no app-process fatal exception was observed. Screenshot capture was black and touch navigation could not be reliably verified, so this is a limited launch smoke check, not full UI validation.
- Added the lecturer workflow and detailed coverage/limitations checklist in `PHASE_8_TEST_REPORT.md`.

### Verification

- `gradlew.bat assembleDebug testDebugUnitTest` — **BUILD SUCCESSFUL**.
- 58 JVM unit tests passed; 0 failures, 0 errors, 0 skipped.
- `git diff --check` — passed (Git line-ending conversion warnings only; no whitespace errors). No other Git operations were performed.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

### Remaining limitations

- Firebase configuration is absent. Authentication, Firestore writes/reads, deployed rules, account switching with real accounts, sync/reconnect, friend-only visibility, and cloud theme synchronization were not live-tested.
- The anonymous Google Books quota was rate-limited in Phase 3. Phase 8 did not repeat requests to avoid unnecessary quota usage. Search mapping, errors, and cache fallback are unit-tested, but successful live search, pagination, and cover loading need a configured API/quota.
- Full device UI interaction, visual rendering, and lecturer demonstration of authenticated workflows still require manual verification on a configured device/project.
- Phase 8 was complete before the final packaging phase.

## Phase 9 — Final release, documentation, GitHub, and viva preparation

### Completed

- Audited repository history, branch, remote, working-tree changes, ignore rules, and credential-file patterns before packaging. Local `main` and `origin/main` began at Phase 5 commit `03f2a248`; the Phase 6–8 implementation was present locally. The configured remote matches the requested GitHub repository.
- Prepared a project report, final test report, lecturer demonstration guide, and viva preparation document grounded in the implemented code and explicit about cloud/API verification limits.
- Updated the README and this progress log for the final project state.
- Created an editable 12-slide university presentation. The available headless emulator screenshots were not usable, so no mockup is presented as a real app capture.
- Final clean build, package/version and APK checks, one final commit, and normal fast-forward push are recorded after validation below.

### Final verification

- `gradlew.bat clean assembleDebug testDebugUnitTest` — **BUILD SUCCESSFUL**.
- 58 JVM unit tests passed; 0 failures, 0 errors, 0 skipped.
- APK `app/build/outputs/apk/debug/app-debug.apk`; package `com.fable.app`, version `1` / `0.1.0`, size `19,712,876` bytes.
- Presentation package/layout validation passed with 12 slides and 0 reported layout findings.
- GitHub result is provided in the final handoff; no history reset or force push is used.

### Remaining limitations

- Firebase project configuration is unavailable; live account and Firestore behaviors/security rules remain unverified.
- Successful Google Books live retrieval remains unverified; a prior anonymous request hit HTTP 429 quota limits.
- The headless emulator produced a black screenshot and unreliable touch input; manual visual/device verification remains advisable.
