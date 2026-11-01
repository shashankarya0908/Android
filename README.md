# Fable

Fable is an Android social reading app for discovering books, tracking a personal reading life, and connecting with other readers.

## Current status

Phases 1–9 are complete. Fable is a Kotlin and Jetpack Compose Android app for discovering books, maintaining a personal reading library, reviewing books, and connecting with readers. Phase 9 packages the final documentation and viva materials. The app builds without Firebase credentials, but this workspace has no Firebase configuration, so account and cloud features are not live-verified.

## Problem and project goals

Book discovery, personal tracking, reviews, and reader communities are often split across separate services. Fable demonstrates a single Android application that brings these reading activities together while keeping personal reading records private and presenting social profile information separately. The project goals are to support book discovery, a UID-scoped library and progress tracker, ratings/reviews, reader profiles and friend activity, and persistent visual theme selection using a clear MVVM/repository architecture.

## Architecture overview

```text
Jetpack Compose screens and Navigation
                ↓
          ViewModels / UI state
                ↓
      Domain models and repositories
          ↙                  ↘
 Room local cache          Data adapters
                         Firebase Auth/Firestore
                         Google Books API
```

Room stores cached books, search history, user-scoped library records, and review records. Firebase-backed repositories own account, profile, reading, review, and social synchronization. Google Books is accessed through Retrofit. Local code and automated tests are present for these flows, but the cloud services require their own configuration and have not been live-verified here. See [ARCHITECTURE.md](ARCHITECTURE.md) for data flows and boundaries.

## Features implemented

- Compose login, registration, password reset, initial profile setup, and authenticated home screens.
- Firebase Authentication session persistence and auth-aware startup navigation.
- Registration validation and username normalization.
- Transactional Firestore username reservation plus profile creation; a failed profile write removes the newly created Auth account where possible.
- Own-profile observation and updates, including username changes with atomic reservation transfer.
- Firestore security rules for profile ownership and username reservations.
- Google Books search by title, author, ISBN, subject, and general query, with paginated results and genre collections.
- Book detail screens, robust DTO mapping, shared source-qualified book IDs, Room-backed result caching, and up to ten recent searches.
- Debounced search with search-type filters, duplicate-safe pagination, image fallbacks, and network/HTTP error states.
- Personal Want to Read, Currently Reading, and Read shelves with library search, category filters, sorting, moves, removals, and book-detail links.
- Per-user reading progress, notes, dates, completion history, locally cached edits, and reading statistics calculated from saved library entries.
- One 1–5-star rating and optional written review per user and book, with edit/delete, book detail summaries, and personal review history.
- Public reader profiles with username search, display name, bio, HTTPS profile image, favourite genres, and reading statistics derived from saved library records. Email and authentication data stay in the private owner profile.
- Friend requests with accept/reject/cancel, duplicate and self-request prevention, accepted friends, and friend removal.
- Activity feed from accepted friends for library additions, reading starts, progress milestones, completions, and published/updated reviews. Personal notes are not included; progress events use stable milestones to avoid minor-update spam.
- Five centralized app themes: Gothic, Gothic Romance, Dark Academia, Dreamscape, and Minimalist. Each changes the color palette, typography, component shapes, page surfaces, and theme-specific accents. Theme previews and selection are available from sign-in and authenticated Home/profile.
- Theme choice saves immediately in a per-user local preference, including a separate anonymous preference when signed out. If Firebase is configured, the choice also updates the existing private `UserProfile.selectedTheme` field. Local selection remains usable if cloud sync fails; pending sync is retried on a later session restore. No Room schema change is needed.

## Technology

- Kotlin 2.4.21, Jetpack Compose, Material 3, MVVM and Clean Architecture boundaries.
- Firebase Authentication and Cloud Firestore, managed with Firebase BoM 35.1.0.
- Google Books API through Retrofit/OkHttp, with Room for local book cache and search history.
- Coroutines and Flow; manually wired dependency composition to keep the dependency setup small.
- Gradle 8.13 wrapper, Android SDK 36, Java 17 bytecode target.

## Firebase setup

1. Create or select a project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with package name `com.fable.app`.
3. Download that Android app's `google-services.json` and place it at `app/google-services.json` (beside `app/build.gradle.kts`). Do not use a config file from a different Firebase app.
4. In Firebase Authentication, enable the Email/Password provider.
5. Create a Cloud Firestore database.
6. Publish the rules from [`firestore.rules`](firestore.rules) using the Firestore Rules editor or Firebase CLI.
7. Sync Gradle and run the app. The Google Services Gradle plugin is applied when `app/google-services.json` is present. Without it, the app shows a setup message and authentication actions fail safely; no mock authentication is used.

### Firestore schema

- `users/{uid}`: the authenticated user's profile fields (`uid`, `username`, `displayName`, `email`, `bio`, `profileImageUrl`, `favouriteGenres`, `favouriteBooks`, `selectedTheme`, `createdAt`, `updatedAt`). Only the owner can read or change the profile in this phase.
- `selectedTheme` is the existing private profile field used for cloud preference sync. It stores a theme name; older or unknown values fall back to Minimalist. The public profile and Firestore rules do not need theme-specific changes.
- `usernames/{normalizedUsername}`: a private uniqueness claim containing the owning `uid` and normalized username. Authenticated clients can get a claim for reservation checks, but cannot list claims. Create/delete is restricted to the owning authenticated account. Profile writes and username claims are checked atomically by Firestore rules using `getAfter()`.
- Passwords are managed only by Firebase Authentication and are never written to Firestore.
- `bookReviews/{bookKey}/reviews/{uid}`: a signed-in-readable review/rating record keyed by stable book hash and reviewer UID. A user may create, edit, or delete only their own record. Average and count are calculated from the current review records.
- `users/{uid}/reviews/{bookKey}`: a private mirror for the user's review history. Batch writes keep the public book view and private history copy together.

### Social schema

- `publicProfiles/{uid}`: only public reader fields (`uid`, normalized username, display name, bio, HTTPS profile image URL, favourite genres, aggregate reading counts, and update time). It never contains email, passwords, private notes, or auth tokens. Existing signed-in profiles get this mirror when their private profile is observed.
- `users/{recipientUid}/incomingRequests/{senderUid}` and `users/{senderUid}/outgoingRequests/{recipientUid}`: mirrored request state with deterministic participant IDs. Transactions keep both copies in step; rejected and cancelled requests can be sent again, while pending requests and existing friendships prevent duplicates.
- `users/{uid}/friends/{friendUid}`: mirrored friendship records keyed by the other authenticated UID. Accept creates both records and remove deletes both in a transaction.
- `users/{readerUid}/activities/{activityId}`: stable-ID activity records written after a library/review cloud write succeeds. Rules allow reads only to that reader and accepted friends. This is a live Firestore feature; it is not represented by demo content.

Social records remain in Firestore, so Phase 6 does not change the Room schema or require a Room migration. Public aggregate statistics are updated from synced library records. Activity events omit personal notes.

`firestore.rules` restricts owner profile edits, exposes only the separate `publicProfiles` field allowlist to signed-in readers, validates request participants and state transitions, requires reciprocal accepted-request state for mirrored friendships, and limits activity reads to accepted friends. These rules are source code only; no Firebase configuration was available to deploy or live-test them.

## Reviews and ratings

Book Details shows the Fable average, rating/review count, reviewer usernames, stars, written reviews, and review dates. Each authenticated reviewer has one row per stable `Book.id`; a rating can be saved without written text. The user's review history is available from Home and links back to Book Details. Room caches entries and retains pending local writes; Firestore updates the public book-scoped record and private user history mirror in one batch. Live Firebase review synchronization is unverified until a Firebase project is configured.

### Personal library schema and sync

- `users/{uid}/library/{entryId}` stores one private `UserBook` per stable book ID. `entryId` is a deterministic SHA-256 digest of that ID; titles are never used for identity. The document contains a book snapshot plus shelf, progress, dates, notes, and completion history.
- Firestore rules restrict reads and writes to the matching signed-in UID and validate allowed fields, shelf status, page bounds, and note length. The collection is read by its user-specific path without compound filters, so no additional composite index is required.
- Room's `user_books` table is scoped by `(userId, bookId)`. Local edits remain visible until Firestore acknowledges the matching edit version. An older acknowledgement cannot clear a newer edit. Deletes stay as pending tombstones until acknowledged.
- Firestore's Android offline queue and the Room pending marker retain writes while offline. A Firestore listener refreshes clean cached entries; pending local edits win until uploaded. Network or rules failures leave pending edits visible as “Waiting to sync”. Live Firebase synchronization is unverified because this workspace has no Firebase project configuration.
- Statistics come from saved records: completed books count distinct shelf entries with completion history; pages include recorded completion page counts and current page progress; monthly activity follows completion dates. No reading values are generated when the library has no records.

## Google Books setup

The discovery engine builds without an API key, but a key may be needed when Google Books quota or project policy requires it. Enable the Google Books API in a Google Cloud project, create and restrict an API key to that API, then provide it locally as either a Gradle property or environment variable named `GOOGLE_BOOKS_API_KEY`. For example, add `GOOGLE_BOOKS_API_KEY=your-key` to `%USERPROFILE%\.gradle\gradle.properties` (outside this repository), or set the environment variable before building. The key is read into the app at build time; restrict it appropriately and do not commit it. Google Books supports `intitle:`, `inauthor:`, `isbn:`, and `subject:` query prefixes, and pagination uses `startIndex` with `maxResults` up to 40 ([API documentation](https://developers.google.com/books/docs/v1/using)).

Without a key, Google Books may reject requests depending on the caller's quota. An anonymous live request during Phase 3 verification returned HTTP 429 with the public caller's daily quota at zero. The app handles quota/HTTP failures and can show cached search results, but a successful live response was not verified in this environment.

## Build and test

Open this folder in Android Studio, sync Gradle, and use JDK 17 or later. The project was verified in this environment with JDK 21 and Android SDK 36.

PowerShell:

```powershell
.\gradlew.bat clean assembleDebug testDebugUnitTest
.\gradlew.bat installDebug
```

For a quicker incremental build, run:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

58 JVM unit tests cover authentication/profile behavior, book discovery, reading shelves/progress, reviews, Phase 6 social behavior, and Phase 7 theme definitions, per-user preference restoration, local fallback, and cloud-sync state. The Phase 8 regression test checks that a failed review feed is surfaced while cached reviews remain visible. Tests do not prove live Firebase behavior, deployed security rules, or a successful live Google Books response. See [FINAL_TEST_REPORT.md](FINAL_TEST_REPORT.md) and [PHASE_8_TEST_REPORT.md](PHASE_8_TEST_REPORT.md) for tested scope and manual checks.

## Demonstration and screenshots

Use [FABLE_DEMO_GUIDE.md](FABLE_DEMO_GUIDE.md) for a five-to-seven-minute lecturer sequence. No genuine application screenshots were available for inclusion in this README or the presentation: the available headless emulator screenshot was black, and touch interaction was unreliable. The demo guide includes a manual screenshot checklist. Do not use generated mockups as screenshots of the running app.

## Remaining work

Lecturer demonstrations of authenticated workflows require a configured Firebase project, deployed rules, at least two test accounts for social flows, and working Google Books quota. Without that configuration, the sign-in screen remains usable for viewing theme selection, but real account, library, review, and social actions are intentionally unavailable. Further limitations are recorded in [FINAL_PROJECT_REPORT.md](FINAL_PROJECT_REPORT.md).
