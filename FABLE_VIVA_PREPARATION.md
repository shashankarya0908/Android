# FABLE — Viva Preparation

## Explain the project simply

Fable is an Android app for a reader's complete book journey. A reader can discover books, save them to personal shelves, track pages, rate and review books, and connect with other readers. The app has five selectable visual themes. Kotlin and Jetpack Compose build the app. ViewModels hold screen state, domain interfaces describe app operations, and repositories connect those operations to Room, Google Books, Firebase Auth, and Firestore.

One important distinction: the app source and local unit tests are present, but this copy has no Firebase configuration and Google's anonymous request previously hit its rate limit. Therefore, I can explain the intended integrations and show local build/test evidence, but I must not claim that live sign-in, Firestore, rules, or successful API calls were verified here.

## Beginner-friendly architecture

```text
What the reader sees: Compose screens and Navigation
                    ↓
What the screen is doing: ViewModel and observable UI state
                    ↓
What the app needs: domain models, validation, repository interfaces
                    ↓
Where information is stored or fetched:
Room / Google Books API / Firebase Auth / Firestore
```

- **Compose** describes the interface in Kotlin. The UI redraws when the state it observes changes.
- **ViewModel** holds screen state and reacts to user actions across recompositions.
- **Domain** describes Fable concepts and repository contracts without depending on Firebase or screen code.
- **Repository** hides where data comes from. A screen asks for a repository operation instead of calling the database or network directly.
- **Room** is the local SQLite-backed cache/database. Reading and review rows are scoped to a user ID.
- **Firestore** is the configured cloud document database. User-owned data is written beneath the authenticated UID; social public profile data lives in a separate allowlisted collection.
- **Flow** represents values that can arrive over time, such as the auth session, a library listener, or search history. **Coroutines** run asynchronous work without blocking the UI thread.

## Important files

| File or package | Responsibility |
| --- | --- |
| `app/src/main/java/com/fable/app/MainActivity.kt` | Android entry point; wires dependencies and sets Compose content. |
| `presentation/FableApp.kt` | Observes auth/profile/theme state and selects signed-out, profile setup, or authenticated app flow. |
| `presentation/books/navigation/AuthenticatedBooksApp.kt` | Authenticated navigation graph, top/bottom bars, and feature destinations. |
| `presentation/auth/AuthenticationViewModel.kt` | Auth actions and auth/profile session state. |
| `presentation/books/BookSearchViewModel.kt` | Search, discovery, category, and book-details UI state. |
| `presentation/reading/ReadingViewModel.kt` | Library, shelf, progress, and sync UI state. |
| `presentation/reviews/ReviewViewModel.kt` | Book review and personal review-history actions/state. |
| `presentation/social/SocialViewModel.kt` | Reader profiles, friend requests, friends, and activity UI state. |
| `presentation/theme/FableTheme.kt` | Five centralized palette, typography, shapes, and surface-token definitions. |
| `domain/model/` | Book, UserBook, review, social, profile, and theme models plus pure business calculations. |
| `domain/repository/` | Interfaces that data implementations satisfy. |
| `data/local/FableDatabase.kt` and `BookDao.kt` | Room database schema v3, migrations, and local queries. |
| `data/repository/GoogleBooksRepository.kt` | Maps API operations, caches books, and falls back to cache on eligible failures. |
| `data/repository/FirestoreReadingRepository.kt` | User-scoped library persistence, sync reconciliation, and activity publishing. |
| `data/repository/FirestoreReviewRepository.kt` | Room-backed review caching and Firestore review/history writes. |
| `data/repository/FirestoreSocialRepository.kt` | Public reader lookup, requests, friendships, and activity feed reads. |
| `firestore.rules` | Source rules for data ownership and social access. They still need deployment and live validation. |

## Likely viva questions and answers

### Project and requirements

**1. What is Fable?**
An Android social reading app that joins book discovery, personal reading tracking, reviews, reader profiles, and friend activity.

**2. What problem does it address?**
Reading discovery, progress, reviews, and community are often separate tasks. Fable demonstrates them in one application with a personal library and a reader network.

**3. What are its main features?**
Google Books search and details, personal shelves/progress/statistics, book ratings and reviews, reader search/profiles, friend requests, a friends-only activity feed, and five themes.

**4. What did you prioritize as a university project?**
Understandable feature boundaries, stable user/book identity, testable business logic, and honest handling of service configuration instead of production-scale infrastructure.

### Kotlin, Compose, and state

**5. Why Kotlin?**
It is the language used by Android development and supports concise data classes, null safety, coroutines, and sealed types that fit app state modeling.

**6. What is Jetpack Compose?**
Compose is Android's declarative UI toolkit. Screen functions describe how the UI should look from current state.

**7. What is recomposition?**
When observed state changes, Compose reruns the relevant composable functions to update the screen. It does not mean rebuilding the entire app from scratch.

**8. What is a ViewModel for?**
It holds screen state and coordinates actions so UI state can survive configuration changes and stay separate from rendering code.

**9. What is `StateFlow` used for?**
It exposes the latest UI/session state and notifies active collectors when that state changes.

**10. What is a sealed session state useful for?**
It models mutually exclusive states such as loading, signed out, profile setup, ready, and failed, helping navigation handle each case explicitly.

**11. Why use Navigation Compose?**
It provides a single navigation graph for book, library, review, social, and theme destinations while preserving the authenticated app structure.

### Architecture and data

**12. Explain the dependency direction.**
Presentation depends on domain contracts. Data implements those contracts. Domain models and interfaces do not depend on Firebase or Compose.

**13. What is the repository pattern?**
A repository gives the rest of the app a stable way to request data without knowing whether it came from Room, an API, or Firestore.

**14. Why have domain repository interfaces?**
They keep app behavior testable and allow implementations to change without making screens depend on a vendor SDK.

**15. What does the dependency composition root do?**
`FableDependencies` creates the Room database and repositories. It selects Firebase-backed adapters only if a Firebase app is configured; otherwise it supplies explicit unavailable adapters.

**16. What does Room store?**
Book metadata cache, recent search history, user-scoped reading-library rows, and user-scoped review rows. The current schema version is 3.

**17. Why use migrations?**
Migrations let Room add the library and review tables while preserving data from earlier schema versions instead of destructively recreating the database.

**18. How do you avoid duplicate library entries?**
The Room primary key is the pair `(userId, bookId)`, and the Firestore library document ID is deterministic from the stable book ID.

**19. Why use a stable book ID instead of its title?**
Titles can be duplicated or edited. The source-qualified book ID identifies the same API record consistently across details, shelves, reviews, and navigation.

### Authentication and security

**20. Where are passwords stored?**
They are handled by Firebase Authentication. Fable does not write passwords to Firestore.

**21. How is username uniqueness handled?**
The username is normalized and reserved in a transaction with the profile write. Rules check that the reservation and profile agree after the transaction.

**22. How is user identity chosen for private records?**
The authenticated Firebase UID is passed from the session into the feature repositories. A user-entered username is not used as the authorization identity.

**23. How are emails kept private?**
Email is in the private owner profile. The separate public profile model and Firestore map omit email and authentication data.

**24. How are reading notes kept private?**
They live in the owner's library record. Activity models and Firestore activity maps do not include a notes field.

**25. Are Firestore rules verified?**
No. The source rules were inspected, but no Firebase project or emulator configuration was available to deploy or test them. I would not claim live security enforcement from a code review alone.

### API, sync, and features

**26. How does Google Books search work?**
The repository calls the volumes API via Retrofit. Query types map to title, author, ISBN, general, or subject queries, and responses are validated and mapped into Fable's Book model.

**27. What happens if the API fails?**
The repository tries the Room book cache for matching results. If there is no cached result, it returns a user-readable network/quota/service error instead of crashing.

**28. Was successful live Google Books access verified?**
No. A previous anonymous call received HTTP 429. The code has mapping and error/cache tests, but a successful live response needs configured quota/API access.

**29. How is reading progress validated?**
It must be at least zero and cannot exceed a known positive total page count. Unknown page counts do not produce a percentage.

**30. How are statistics calculated?**
They are derived from saved user-book records and completion history, rather than sample values.

**31. How are reviews unique?**
The reviewer is the authenticated UID, and the local composite key is `(userId, bookId)`. The cloud public review document is keyed by book and reviewer UID.

**32. How are friend requests deduplicated?**
Incoming/outgoing documents use deterministic participant IDs and transactions inspect existing request/friend records before writing.

**33. Who can see the activity feed?**
The repository subscribes to current accepted friends and filters the merged items by the accepted friend set. Firestore rules intend to limit reads to friends, but that rule has not been live-tested here.

**34. How does theme selection persist?**
SharedPreferences stores a local selection keyed by UID, with a separate anonymous key. When Firebase is configured, the same theme name syncs in the existing private `selectedTheme` field.

### Coroutines, testing, and design choices

**35. Why use coroutines?**
Network, database, and Firebase operations can suspend without blocking the Android UI thread.

**36. What is Flow used for?**
It streams changing values such as cached database rows, auth state, and repository observations. ViewModels collect these values and update `StateFlow` for Compose.

**37. What does the test suite cover?**
58 JVM unit tests cover validation/use-case behavior, book mapping/repository handling, reading rules/statistics, review behavior, social state/activity logic, and theme persistence/fallback. They do not replace device or cloud integration tests.

**38. What Phase 8 bug did you find?**
Review snapshot listener errors were ignored, so a service/rules failure could look like an empty review list. The repository now propagates the error; the UI keeps already-emitted cached reviews, and a regression test covers that state.

**39. Why not add a demo mode with fake friends?**
It would risk presenting simulated social/cloud behavior as real. The app instead shows an explicit setup requirement; demonstration data should come from prepared test accounts in a configured project.

**40. Why is there no release-signed APK?**
No secure release signing configuration is present. Creating a signing key for convenience would risk exposing credentials. The debug APK is the appropriate deliverable for a lecturer demonstration.

### Difficult questions and honest answers

**41. Can you prove one account cannot read another account's library?**
The code scopes Room queries and Firestore paths by authenticated UID, and rules are written to enforce owner checks. This workspace did not have two configured Firebase accounts or a rules emulator, so I can explain the design but cannot claim a live proof.

**42. What happens after a failed cloud write?**
For the library/review paths, a local pending record is retained where those repositories are active, and the UI can show a waiting-to-sync status. Actual retry/reconnect and conflict behavior should be verified with a Firebase project and network transitions.

**43. Can local cached data leak when users switch accounts?**
Library/review Room rows and theme settings are keyed by UID, and the authenticated navigation passes the current UID to repositories. Device-wide book metadata/search history are shared because they are not private account records. A multi-account live test is still recommended.

**44. What if two users send friend requests to each other at nearly the same time?**
The transaction checks the reverse pending request and reports that the incoming request should be accepted instead. This avoids intentionally creating reciprocal duplicate pending requests. The transaction/rules behavior still needs emulator/live verification.

**45. Are the public profile fields truly safe to expose?**
The public data class and mapper explicitly contain reader-facing fields and aggregate counts, not email, auth details, library entries, or notes. Deployed rules and production privacy policy still need review before real public launch.

**46. How does the app avoid activity spam?**
Progress events are generated at stable milestones, and activity documents use stable IDs. Repeated minor page changes should not create a new event each time. This logic has domain tests but needs live Firestore validation.

**47. Is review average stored on the server?**
No. The current UI derives average and count from the observed reviews. This keeps the university implementation simple but may not scale to a very large review set.

**48. What are the main limitations?**
Firebase setup/rules and successful live Google Books calls are unverified; the emulator screenshot/touch smoke check was unreliable; tests are JVM tests rather than full device/cloud integration tests; and secure release signing is not configured.

**49. What would you improve next?**
Configure a test Firebase project, validate rules with the emulator suite, add Compose UI tests, run multi-account/offline scenarios on a visible device, and supply a restricted API key with quota. Account deletion and reporting/moderation are possible future requirements.

**50. What is your clearest evidence that the project works?**
The clean Android build and 58 passing JVM tests establish compilation and the tested domain/view-model behavior. I would pair that evidence with a live configured demonstration before claiming the cloud flows work end to end.
