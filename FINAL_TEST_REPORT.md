# FABLE — Final Test Report

## Result

- Final command: `gradlew.bat clean assembleDebug testDebugUnitTest`
- Build result: **SUCCESSFUL** (Gradle 8.13, JDK 21, Android SDK 36)
- JVM tests: **58 passed, 0 failed, 0 errors, 0 skipped**
- Package ID: `com.fable.app`
- Version: `versionCode 1`, `versionName 0.1.0`
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- APK size: **19,712,876 bytes** (approximately 18.8 MiB)
- Release signing: not configured. No release keystore was created; the debug APK is the demonstration package.

The build emitted a non-blocking Gradle forward-compatibility warning: Gradle 8.13 is deprecated for a future Kotlin 2.5 plugin requirement of at least Gradle 8.14.4. Native library stripping also reported two libraries that were packaged without stripping. Neither warning failed the build.

## Automated coverage

The 58 JVM tests cover:

- Auth validation and authentication/profile use cases.
- Google Books query construction, DTO mapping, API errors, and cached repository fallback.
- Reading shelf transitions, stable identity, progress bounds, completion history, statistics, and failure presentation.
- Review creation, editing, deletion, rating bounds, ownership, summary averages/counts, and repository/ViewModel error paths.
- Social profile privacy, request lifecycle/deduplication, friendship transitions/removal, activity generation/deduplication, feed filtering, and error presentation.
- All five theme definitions, legacy fallback, local per-user keys, signed-out selection, pending/synced cloud state, and sync failure/retry behavior.
- Phase 8 review-feed regression: listener/observation failure is shown while a previously emitted cached review remains visible.

These are JVM tests. They do not run the Android UI, Firebase rules, or a live Google Books request.

## Actual integration verification

- Installed the final debug APK on the `Medium_Phone` AVD and launched `com.fable.app.MainActivity`.
- The activity reached `RESUMED`; the accessibility hierarchy showed the sign-in screen and the explicit Firebase setup notice. No app-process fatal exception was observed in the captured log.
- The headless AVD screenshot was black, and touch navigation was unreliable. This verifies launch/accessibility semantics only; visual rendering and touch-driven workflows are not verified.
- No configured Firebase app, emulator, test account, or deployed Firestore rules were available. Authentication, user switching, Firestore reads/writes, security rules, social transactions/feed visibility, and cross-device or reconnect sync remain unverified.
- The last known live Google Books request, during Phase 3, returned HTTP 429 for the anonymous caller. Phase 9 did not repeat network calls to avoid unnecessary quota use. Successful live discovery and cover retrieval remain unverified.
- `.gitignore` excludes `google-services.json`, `local.properties`, environment secrets, signing keys, build outputs, Gradle caches, and IDE caches. Credential filename/key-pattern checks found no such files or key-like literals to commit.

## Known issues and limitations

- A Firebase project/configuration and published rules are required to demonstrate real account, profile, library, review, and social functionality.
- A quota-backed, restricted Google Books API key may be required for live search.
- Full interaction and appearance need a visible emulator or physical-device test; the available headless emulator did not provide a usable screenshot.
- Offline queue/reconnection and multi-account behavior still require service-backed manual tests. Passing unit tests are not proof of those integrations.
- Secure release signing is not configured; only a debug APK is provided.

## Recommended final manual checks

1. Configure Firebase Auth and Firestore, publish `firestore.rules`, and create two disposable reader accounts.
2. Verify registration, duplicate normalized username rejection, login/logout/session restore, profile setup, and account switching.
3. Verify UID isolation for library rows, notes, reviews/history, local theme preferences, and friend/request data.
4. Verify Google Books title/author/ISBN/genre search, pagination, details, missing cover, empty results, quota errors, and cached fallback.
5. Exercise shelf transitions, page bounds, completion dates/statistics, review CRUD/averages, request lifecycle, friend-only feed, and notes privacy.
6. Switch all five themes, restart the app, and confirm per-account selection and cloud sync.
7. Disable/re-enable connectivity and verify pending writes and acknowledgements without duplicates.

Use `FABLE_DEMO_GUIDE.md` for the lecturer run-of-show and service-unavailable fallbacks.
