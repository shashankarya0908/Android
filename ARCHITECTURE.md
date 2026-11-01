# Architecture

Fable is a single Android app module organized by feature at the presentation layer and by responsibility in the data and domain layers.

## Dependency direction

```text
presentation  ->  domain  <-  data
      app composition root wires implementations to domain interfaces
```

- **Presentation** contains Compose screens and ViewModels. ViewModels consume domain repositories/use cases and expose `StateFlow` UI/session state.
- **Domain** contains app models, repository interfaces, validation, and use cases. It does not depend on Firebase or Compose.
- **Data** implements domain repository interfaces. Firebase Auth and Firestore are confined to Firebase repository implementations; Retrofit, Google Books DTOs/mappers, and Room persistence implement book discovery and personal reading below the domain boundary.
- **DI** is the small composition root that chooses Firebase implementations when Firebase is configured and safe unavailable implementations otherwise.
- **Navigation** is an auth-aware Compose root. Authenticated users reach Home, Discover, Search, category results, Book Details, My Library, Reading Progress, My Reviews, reader discovery/profiles, friend requests, Friends, and the activity feed through the navigation graph.

## Current package layout

```text
com.fable.app/
  data/local/                  Room database, book cache and search history
  data/remote/googlebooks/     Retrofit service, Google Books DTOs and mapping
  data/remote/firestore/       UserBook and BookReview DTOs, mappers and stable document IDs
  data/repository/             Firebase Auth, Firestore, book, reading, review and social repositories
  di/                          dependency selection and ViewModel factory
  domain/model/                AuthUser, UserProfile, public reader, social, Book, UserBook, review and statistics models
  domain/repository/           auth, profile, book, reading, review and social contracts
  domain/usecase/              auth/profile/book/library/review use cases
  domain/validation/           registration, login and profile validation
  presentation/auth/           auth/profile screens and ViewModels
  presentation/books/           discovery, search, category and detail UI/ViewModels
  presentation/reading/         personal library, progress and statistics UI/ViewModel
  presentation/reviews/         book review UI/ViewModel and personal review history
  presentation/social/          reader discovery/profile, requests, friends, and activity feed
  presentation/home/           temporary authenticated home
  presentation/theme/          centralized Compose theme
```

## Authentication data flow

- Firebase Auth persists the signed-in account. An auth state listener feeds the session `StateFlow`; the app shows a loading state during startup and then routes to login, profile setup, or home.
- Registration first creates the Auth account. A Firestore transaction then reserves the normalized username and creates `users/{uid}`. If the Firestore transaction fails, the newly created Auth account is deleted on a best-effort basis.
- Username changes move the old and new username claims and update the profile in one Firestore transaction.
- Composables never access Firebase. Auth and profile ViewModels call domain use cases/repositories.
- If `app/google-services.json` is missing, dependency wiring uses explicit unavailable repositories. No fake user or local-only authentication is created.

## Book discovery data flow

- `BookRepository` is the domain boundary for discovery, typed search, book details, and recent searches. The Google Books implementation is replaceable without changing screens or ViewModels.
- Retrofit calls the Google Books volumes API. DTO mapping validates required identifiers/titles, safely handles optional metadata, normalizes cover URLs, and generates IDs from source plus source ID rather than titles.
- Room caches result pages and up to ten recent queries. Search screens debounce input, guard pagination, and remove duplicate books. Cached results can be returned if a request fails.
- Google Books credentials are optional and supplied via `GOOGLE_BOOKS_API_KEY` Gradle property or environment variable. Build-time configuration is documented in README. A public anonymous API request during verification was rate-limited (HTTP 429), so live success requires a configured project/key and quota.
- Discover collections use API results; the app does not insert sample books or invent trending statistics. The popular discoveries group is sorted from the API-provided ratings count when available.

## Personal library and synchronization

- `ReadingRepository` is scoped by the authenticated Firebase UID. `UserBook` uses the stable `Book.id`; Room enforces a composite `(userId, bookId)` primary key and Firestore uses a deterministic SHA-256 document ID under `users/{uid}/library`.
- Shelf moves preserve a single entry. Completion dates remain in completion history when a book returns to Currently Reading; marking the same completed entry Read again does not duplicate the same completion. A new completion after a reread records a new date.
- Reading progress validation is in the domain layer. Page percentage is null when page count is unknown and otherwise uses current page divided by total pages. Room writes are marked pending before network synchronization. Version-matched acknowledgements prevent older requests from clearing a newer edit; delete tombstones remain until the matching cloud delete succeeds.
- Firestore listeners reconcile clean cloud snapshots into Room. Dirty local rows take precedence until uploaded, while the Firestore Android SDK queues offline writes. A write remains visibly pending if acknowledgement fails. Live cloud synchronization requires Firebase setup and is not asserted from JVM tests.
- Reading statistics are derived from stored user-book records. Completion count and average use distinct books; total pages combine completion history and the current page of in-progress reads; monthly activity groups completion dates. The feature needs no composite Firestore index because it reads a user's subcollection without compound filters.

## Firestore security

`firestore.rules` keeps `users/{uid}` private to its owner, requires a valid normalized username, and verifies username claims after atomic writes. Username claims cannot be listed. Social discovery reads a separate public profile allowlist that excludes email and auth data. Profile mutations are owner-only. Friend request rules bind sender/recipient UIDs to path IDs, permit only valid state transitions, and allow repeated requests only after a rejected/cancelled state. Mirrored friendship creation requires an accepted request and its reciprocal relationship; removal is limited to the involved users. Activity reads require an accepted friend record; activity records have a strict field allowlist that excludes personal notes.

## Reviews and ratings

- `ReviewRepository` keeps review data separate from reading progress. Room stores one row per `(userId, bookId)` and caches book review lists and personal history.
- Each rating/review is stored at `bookReviews/{sha256(bookId)}/reviews/{uid}` and mirrored at `users/{uid}/reviews/{sha256(bookId)}`. The Firestore batch keeps those views together; the deterministic reviewer document ID and Room composite key prevent duplicate user/book reviews.
- Book Details observes cached and Firestore reviews, derives average/count from current records, and exposes the signed-in user's rating/text for updates or deletion. My Reviews reads the private mirror and links back to each book.
- Firestore rules allow signed-in users to read book reviews while create/update/delete is restricted to the review owner. The user history mirror remains private. Live Firebase behavior is unverified without `google-services.json` and a configured project.

## Centralized theme system

- `FableThemeDefinition` is the single source for each theme's Material 3 `ColorScheme`, semantic palette, type styles, shapes, and `FableThemeTokens` (page brush, card border, ornament, and elevation). The five definitions are Gothic, Gothic Romance, Dark Academia, Dreamscape, and Minimalist.
- `FableApp` observes theme settings and applies `FableTheme` around the full auth-aware UI tree. App surfaces use `MaterialTheme`; changing the selected definition updates authentication, discovery, book detail, reading, review, and social destinations consistently. The picker renders previews from the same definitions used by the app.
- `ThemePreferenceRepository` is a small domain boundary implemented by `LocalThemePreferenceRepository` using Android SharedPreferences. Preferences are keyed by authenticated UID, with a distinct anonymous key while signed out. This makes the local choice immediately available offline and separates cached choices between accounts.
- With Firebase configured, `ThemeSettingsViewModel` syncs the selected theme through the existing `UserRepository.updateProfile` call to the owner's existing private `UserProfile.selectedTheme` field. No new public profile fields, Firestore collections/rules, Room entities, or migrations are introduced. Older/missing theme values default to Minimalist.
- A local selection marked pending remains the displayed choice until cloud sync succeeds. Sync errors preserve the local choice and pending marker, and a subsequent profile/session restore retries. Without Firebase, the UI reports device-only persistence and does not claim cloud sync. Live sync remains unverified without project configuration.

## Phase 8 integration audit notes

- Room library and review rows are keyed by authenticated UID; repositories receive that UID from the authenticated navigation/session layer. Shared theme preferences are UID-keyed, with a separate anonymous key. Search history and book metadata cache are intentionally device-wide because they contain no user-authored private data.
- Firebase Auth/Firestore and Google Books are adapters behind repository interfaces. Without their configuration/network, the app uses explicit unavailable auth, library, review, and social adapters or cached book results; it does not fabricate accounts or cloud records.
- Review snapshot listener errors now close the repository flow with the underlying failure. The review ViewModel surfaces the message while preserving already-emitted cached reviews, instead of silently presenting a sync failure as an empty review list. A unit regression test covers this state; live listener behavior still needs a configured Firebase project.
- Phase 8 did not change Room schema version 3 or Firestore rules. Rules were inspected as source but not evaluated by the Firebase emulator or deployed project.

## Social profiles, requests, and activity

- `PublicReaderProfile` contains username, display name, bio, HTTPS profile image URL, favourite genres, and aggregate reading statistics. It deliberately has no email field. `FirestoreUserRepository` mirrors profile changes into `publicProfiles/{uid}`; signed-in existing accounts receive the same mirror when their owner profile is observed.
- Public statistics are counts derived from the owner's synchronized Room library records. Friends see only aggregate counts, not library records or personal notes. Social content remains cloud-only, so the Room database stays at schema v3 and no Phase 6 migration is needed.
- Reader search uses a normalized-username prefix query capped at 30 profiles. Requests are stored in mirrored per-user incoming/outgoing paths with participant UIDs as document IDs. Accept writes two friend records atomically; remove clears both and closes the accepted request state so a later request can be made.
- Library activity is published only after a Firestore library write succeeds. Progress uses 10% milestones or 10-page milestones when page count is unknown. Book additions and completion timestamps have stable IDs. Review activity uses one stable record per author/book and changes from published to updated.
- The feed subscribes only to accepted friends' activity subcollections, merges recent entries, and filters by the current accepted-friend set. Activity fields carry book ID/title/cover, reader, event, progress or public review details, and timestamp; they never copy reading notes.
- `firestore.rules` has been updated for public profile allowlists, requests, reciprocal friendships, and friend-only activity reads. This workspace has no Firebase config or emulator, so rules deployment and live synchronization remain unverified.
