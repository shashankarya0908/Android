# FABLE — Final Project Report

## 1. Abstract

Fable is an Android social reading application built with Kotlin and Jetpack Compose. It combines Google Books discovery with a personal reading library, progress tracking, ratings and reviews, public reader profiles, friend relationships, a friends-only activity feed, and five selectable themes. The project uses a small MVVM and repository architecture. Room stores book caches and user-scoped reading/review records; Firebase Auth and Cloud Firestore provide account and cloud features when configured. The codebase and local automated tests are complete for Phases 1–9. Live Firebase functionality and successful Google Books requests were not verified in this workspace because the required Firebase project configuration and API quota are unavailable.

## 2. Introduction

Readers often find books, record progress, write reviews, and talk about reading in separate places. Fable demonstrates how those activities can be brought into one Android application. Its emphasis is a clear student-sized architecture, predictable account ownership, recoverable local cache behavior, and an interface that changes consistently with the selected theme.

## 3. Problem statement

There is no single place in a reader's workflow for finding a book, saving it to a shelf, recording progress, reviewing it, and seeing what accepted friends have read. A university project that joins these functions must also distinguish private reading data from public profile and activity data and handle service failures without pretending that a cloud operation succeeded.

## 4. Objectives

- Provide typed book search, genre discovery, and book details.
- Maintain per-account Want to Read, Currently Reading, and Read shelves.
- Track pages, notes, completion history, and record-derived statistics.
- Support one 1–5-star rating and optional written review per user/book, including editing, deletion, summaries, and personal history.
- Support public reader profiles, normalized username discovery, friend requests, accepted friendships, and friend activity.
- Provide five persistent themes across the application.
- Demonstrate repository boundaries, local persistence, validation, ownership rules, and automated tests.

## 5. Proposed solution

Fable places screens in Jetpack Compose, exposes UI state through ViewModels, defines application models and repository contracts in the domain layer, and implements those contracts with Room, Retrofit, Firebase Auth, or Firestore adapters. Dependency wiring chooses Firebase adapters when Firebase is configured and explicit unavailable adapters otherwise. No fake users, friends, feed records, or successful cloud-sync messages are substituted for real service behavior.

## 6. Functional requirements

- Register, sign in, sign out, request a password reset, and complete or edit a username-based profile when Firebase is configured.
- Search books by title, author, ISBN, general text, or genre; open details and handle empty, cached, and failed results.
- Add, move, update, and remove personal library entries; validate reading progress and calculate statistics from saved records.
- Create, update, and delete a rating/review; see book summaries and the user's review history.
- Search public reader profiles; send, accept, reject, or cancel friend requests; remove friends; observe friend activity.
- Select and persist Gothic, Gothic Romance, Dark Academia, Dreamscape, or Minimalist theme.

## 7. Non-functional requirements

- Keep private email/authentication fields out of public profiles and keep reading notes out of social activity records.
- Use stable IDs and user-scoped record keys to reduce duplicate records and cross-account data mixing.
- Provide visible loading, empty, validation, and error states and retain pending local changes where implemented.
- Keep dependencies and backend design appropriate to a university demonstration.
- Build without local Firebase credentials and avoid claiming live integrations that were not tested.

## 8. Technology stack

- Kotlin 2.4.21, Android Gradle Plugin 8.13.2, Gradle wrapper 8.13.
- Jetpack Compose, Material 3, Android Navigation Compose, Lifecycle ViewModels.
- Coroutines and Flow for asynchronous repository work and observable UI state.
- Room 2.8.5 for local book cache, search history, library rows, and review rows (database schema v3).
- Firebase Authentication and Cloud Firestore via Firebase BoM 35.1.0, when configured.
- Retrofit 3.0.0, Gson converter, OkHttp 5.4.0, and Coil 3.3.0 for book requests and images.
- JUnit 4 and kotlinx-coroutines-test for JVM unit tests.

## 9. System architecture

```text
Compose screens and Navigation
              ↓
          ViewModels
              ↓
 Domain models, validation, and repository interfaces
              ↓
   ┌──────────┼──────────────┐
   ↓          ↓              ↓
 Room      Retrofit      Firebase adapters
 local     Google Books  Auth + Firestore
 cache     service       when configured
```

The dependency direction is presentation → domain ← data. The application composition root wires concrete repositories. ViewModels expose `StateFlow` UI/session state. Compose screens render that state and send user actions to ViewModels. More detail is in `ARCHITECTURE.md`.

## 10. Database and data models

The Room database has exported schema versions 1, 2, and 3. Version 2 added `user_books`; version 3 added `book_reviews`. `user_books` uses `(userId, bookId)` as its primary key. `book_reviews` uses `(userId, bookId)`. Pending flags and deletion markers support queued edits and acknowledgement-aware cleanup. Cached book metadata and recent search history are device-wide because they are not private user-authored records.

Firestore paths include private `users/{uid}` profile documents, username claims, per-user library and review history, book-scoped public reviews, separate allowlisted `publicProfiles/{uid}`, mirrored requests/friendships, and per-reader activities. User IDs come from the authenticated UID. Firestore rules are in the repository but were not deployed or emulator-validated in this environment.

## 11. Authentication workflow

Firebase Auth emits the current authenticated user. The session ViewModel observes that stream, then observes the matching private profile. A new account enters profile setup; a complete profile enters authenticated navigation. Registration reserves a normalized username and writes profile documents transactionally. Passwords stay with Firebase Auth. Without `google-services.json`, an unavailable repository reports setup is required and does not create a local fake account.

## 12. Book discovery workflow

The search ViewModel debounces queries and supports title, author, ISBN, and general query types. Genre discovery uses the subject query. The Retrofit service calls Google Books volumes endpoints; DTO mapping validates identifiers and optional fields and creates stable source-qualified book IDs. Room stores book metadata and up to ten recent searches. Search/detail failures can use cached books where a matching cache record exists; otherwise the UI receives a user-readable error. A previous anonymous request received HTTP 429, and successful live retrieval was not verified.

## 13. Library and reading progress

Each `UserBook` is scoped to the authenticated UID and stable `Book.id`. Shelf transitions preserve a single row. Page progress is nonnegative and cannot exceed a known total; completion history supports rereads and statistics. Personal notes are stored in the user's private library record and are not included in friend activity. Room keeps local records and pending markers while Firestore writes are acknowledged. Statistics are derived from saved library rows, not generated sample numbers.

## 14. Ratings and reviews

One review record per user and book carries a 1–5 rating, optional text, reviewer identity, book identity, and create/update timestamps. Room uses the composite user/book key. Firestore uses a deterministic book hash and reviewer UID for the public book view, plus a private per-user history copy. Average and count are calculated from current review records. Phase 8 fixed review observation errors being silently ignored; cached items now remain in state while the UI surfaces a listener error.

## 15. Social networking and activity feed

Reader search uses normalized username prefixes. The public profile model has username, display name, bio, image URL, favourite genres, and aggregate reading statistics, but no email or private notes. Friend requests are stored in deterministic mirrored paths, and accepted friendships are mirrored for both participants. The feed listens to accepted friends' activity paths and filters by the current friend set. Events represent library additions, reading starts, stable progress milestones, completion, and review publication/update. Personal notes are excluded. These Firestore paths and rules were not live-tested here.

## 16. Five-theme architecture

`FableThemeDefinition` centralizes each theme's Material 3 palette, typography, component shapes, page gradient, border, elevation, and accent. `FableApp` applies the selection around both signed-out and authenticated flows. SharedPreferences stores a local theme under the user's UID, with a separate anonymous key. With Firebase configured, the selected value also syncs through the existing private `UserProfile.selectedTheme` field. Cloud sync and cross-device restoration remain unverified.

## 17. Testing methodology and results

The project has 58 JVM unit tests across auth/profile behavior, Google Books query/mapping/repository handling, library/progress/statistics, reviews, social identity/request/activity logic, and theme persistence/state. The final clean build command was `gradlew.bat clean assembleDebug testDebugUnitTest`. Detailed counts and integration limits are in `FINAL_TEST_REPORT.md`.

The debug APK was installed and launched on the `Medium_Phone` Android emulator. The activity resumed and its accessibility hierarchy exposed the signed-out login form and Firebase setup notice, with no app-process fatal exception observed. The headless emulator screenshot was black and touch navigation was unreliable, so this was not a successful visual or interaction test. There are no live Firebase tests or successful live Google Books tests.

## 18. Limitations

- No `app/google-services.json`, Firebase project, test accounts, or deployed Firestore rules were available.
- Google Books quota was exhausted for a previous anonymous request; a configured API key/quota is needed to verify successful live search.
- Android UI interaction and visual rendering need manual verification on a visible emulator or device.
- Automated tests are JVM unit tests. They do not prove Firestore rule behavior, multi-device synchronization, service availability, or all Compose interactions.
- Release signing is not configured. The deliverable is a debug APK, suitable for a demonstration installation but not Play Store distribution.
- The project does not include a production account-deletion workflow or an administrative moderation system.

## 19. Future enhancements

- Configure a dedicated Firebase demo project, deploy and test rules with the Firebase Emulator Suite, and verify multi-account flows.
- Add Compose UI/instrumentation tests for primary workflows and test offline/reconnect behavior on devices.
- Configure a restricted Google Books API key and validate live quota-backed discovery.
- Add secure release signing outside source control if a distributable release build is needed.
- Consider account deletion/data cleanup, profile moderation/reporting, and privacy controls as separate future work.

## 20. Conclusion

Fable brings discovery, personal reading tracking, reviews, reader profiles, and friend activity into one Android project while separating screen state, domain behavior, and data adapters. It builds and its local JVM suite passes. The deliverable includes source, documentation, test results, a presentation, viva preparation, and a practical demonstration sequence. Live cloud/API behavior remains an explicit configuration and verification task rather than a claimed result.
