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

### Remaining phases

Reviews; friend requests; activity feed; theme persistence; broader integration tests; public profile privacy rules; release verification.
