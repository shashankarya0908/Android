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
- **Navigation** is an auth-aware Compose root. Authenticated users reach Home, Discover, Search, category results, Book Details, My Library, and Reading Progress through the navigation graph.

## Current package layout

```text
com.fable.app/
  data/local/                  Room database, book cache and search history
  data/remote/googlebooks/     Retrofit service, Google Books DTOs and mapping
  data/remote/firestore/       UserBook and BookReview DTOs, mappers and stable document IDs
  data/repository/             Firebase Auth, Firestore, book, reading and review repositories
  di/                          dependency selection and ViewModel factory
  domain/model/                AuthUser, UserProfile, Book, UserBook, BookReview and statistics
  domain/repository/           auth, profile, book, reading and review contracts
  domain/usecase/              auth/profile/book/library/review use cases
  domain/validation/           registration, login and profile validation
  presentation/auth/           auth/profile screens and ViewModels
  presentation/books/           discovery, search, category and detail UI/ViewModels
  presentation/reading/         personal library, progress and statistics UI/ViewModel
  presentation/reviews/         book review UI/ViewModel and personal review history
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

`firestore.rules` restricts profile reads and writes to the signed-in owner, requires a valid normalized username, and verifies the username claim after an atomic write. Username claims cannot be listed. Public profile access is intentionally not enabled in Phase 2 and must be designed with privacy controls in the social phase.

## Reviews and ratings

- `ReviewRepository` keeps review data separate from reading progress. Room stores one row per `(userId, bookId)` and caches book review lists and personal history.
- Each rating/review is stored at `bookReviews/{sha256(bookId)}/reviews/{uid}` and mirrored at `users/{uid}/reviews/{sha256(bookId)}`. The Firestore batch keeps those views together; the deterministic reviewer document ID and Room composite key prevent duplicate user/book reviews.
- Book Details observes cached and Firestore reviews, derives average/count from current records, and exposes the signed-in user's rating/text for updates or deletion. My Reviews reads the private mirror and links back to each book.
- Firestore rules allow signed-in users to read book reviews while create/update/delete is restricted to the review owner. The user history mirror remains private. Live Firebase behavior is unverified without `google-services.json` and a configured project.

The application theme remains independent of auth/profile data. Theme persistence and selectable visual identities are deferred to the settings phase.
