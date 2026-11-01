# Fable

Fable is an Android social reading app for discovering books, tracking a personal reading life, and connecting with other readers.

## Current status

Phases 1–5 are implemented. Email/password flows and profile persistence use Firebase Auth and Cloud Firestore when configured. The app remains buildable without Firebase credentials and clearly reports that account actions require setup. Live Firebase behavior has not been verified because this workspace has no Firebase project configuration. Authenticated users also have Google Books discovery, search, genre exploration, book details, cached search pages, recent searches, personal shelves, a reading tracker, and book ratings/reviews.

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
- `usernames/{normalizedUsername}`: a private uniqueness claim containing the owning `uid` and normalized username. Authenticated clients can get a claim for reservation checks, but cannot list claims. Create/delete is restricted to the owning authenticated account. Profile writes and username claims are checked atomically by Firestore rules using `getAfter()`.
- Passwords are managed only by Firebase Authentication and are never written to Firestore.
- `bookReviews/{bookKey}/reviews/{uid}`: a signed-in-readable review/rating record keyed by stable book hash and reviewer UID. A user may create, edit, or delete only their own record. Average and count are calculated from the current review records.
- `users/{uid}/reviews/{bookKey}`: a private mirror for the user's review history. Batch writes keep the public book view and private history copy together.

The current rules intentionally do not expose profiles publicly. Public profile reads will need a separate privacy design in the social phase.

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
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

35 JVM unit tests cover authentication/profile behavior, book discovery, reading shelves/progress, and Phase 5 review creation, editing, deletion, uniqueness, ownership, rating summaries, empty state, and UI error presentation. Tests do not prove live Firebase behavior or a successful live Google Books response.

## Remaining work

Friend requests, activity feed, theme persistence, broader integration testing, and public profile privacy rules are planned for later phases.
