# FABLE Phase 8 — Integration Test and Demo Readiness Report

## Summary

The existing Phase 1–7 project was audited and built without recreating the app or changing the Room schema or Firestore rules. One review-flow error-reporting defect was fixed. The full JVM unit suite passes. Live Firebase flows and successful live Google Books access could not be verified in this workspace.

## Features and code paths tested

### Automated JVM tests

The 58-test suite covers:

- Authentication/profile validation and auth use-case/session behavior.
- Google Books query building, response mapping, repository cache fallback, and API error mapping.
- Reading shelf transitions, duplicate/stable identity behavior, progress validation, completion history, statistics, and repository errors.
- Review creation/edit/delete, rating and ownership validation, summary averages/counts, empty states, persistence view-model behavior, and error presentation.
- Social profile privacy, friend request transitions/duplicate and self-request rules, friendship behavior, activity generation/deduplication/filtering, and error presentation.
- All five theme definitions, default/unknown fallback, UID preference separation, signed-out persistence, cloud-pending priority, sync/retry behavior, and local fallback.
- Phase 8 regression: cached review data remains in the review UI state and a feed error is shown when the review observation flow fails after emitting cached data.

### Static integration audit

Inspected authentication/profile flows, navigation, Google Books adapters, Room entities/DAO/schema exports/migrations, library and review repositories, social repositories/ViewModels, theme storage, Firestore rules, and the project documentation.

- Library records use `(userId, bookId)` identity and review records use `(userId, bookId)` locally. Query methods for personal libraries/review history are scoped by the signed-in UID. Theme preferences are keyed per UID, with a distinct anonymous key. Search history and book metadata are device-wide caches.
- Private reading notes are not part of activity models or activity DTOs. Public reader profile mapping excludes email and authentication data.
- Theme preference sync reuses the existing private `selectedTheme` profile field. No new Room migration or Firestore rule was required.
- Firestore rules were reviewed as source only; they were not run against an emulator or deployed project.

### Emulator smoke check

Installed and launched the debug APK on the `Medium_Phone` AVD. The activity was resumed, the accessibility hierarchy contained the signed-out login form and the explicit “Firebase setup is required” message, and no app-process fatal exception was observed in the captured log. The screenshot captured from this headless emulator was black and taps did not reliably navigate, so visual rendering and interaction are **not** counted as verified device tests.

## Test and build results

- Command: `gradlew.bat assembleDebug testDebugUnitTest`
- Build: **SUCCESSFUL**
- Unit tests: **58 passed, 0 failed, 0 errors, 0 skipped**
- `git diff --check`: **passed**; Git printed line-ending conversion warnings only.
- APK: `app/build/outputs/apk/debug/app-debug.apk`
- Toolchain: JDK 21, Android SDK 36, Gradle 8.13. Gradle emitted the existing non-blocking warning that Kotlin 2.5 will require Gradle 8.14.4.

No commits, pushes, merges, or other Git operations were performed.

## Bug discovered and fixed

**Review feed errors were silently ignored.** Both the book-scoped and personal review Firestore snapshot listeners only handled successful snapshots. A listener error could therefore leave the UI looking like the book had no reviews. The repository now closes the observation flow with the listener error. The ViewModel exposes the error while keeping the latest cached review list in state. A regression test covers the cached-data-plus-error UI state.

No other confirmed code defect was changed during this audit. Behavior that could not be exercised without live services is listed as unverified rather than assumed correct.

## Firebase verification status

`app/google-services.json` is absent and no Firebase project or emulator configuration was available. Consequently, none of these were live-verified:

- Registration, login, invalid credentials, password reset, sign-out, or persisted session restoration.
- Username reservation/uniqueness, profile setup/edit, and switching between real accounts.
- Firestore library/review writes, offline queue/reconnection, conflict behavior, or cross-device data.
- Friend request transactions, profile ownership, friend-only activity reads, private notes protection under deployed rules, or activity publishing.
- Theme profile synchronization and cross-device restoration.

The app deliberately does not create fake accounts, friends, activity, or simulated cloud success. With Firebase unavailable, the UI reports that authentication setup is required and uses unavailable adapters for account-owned cloud features.

## Google Books verification status

The Google Books adapter, query modes, mapping, error handling, and Room-cache fallback have JVM coverage. A previous anonymous request during Phase 3 received HTTP 429 because the caller's daily quota was exhausted. Phase 8 did not repeat live requests. Successful live search, pagination, details, and cover retrieval remain unverified; use a project API key with quota for the manual check.

## Manual device testing checklist

Perform these checks with a Firebase project configured, rules deployed, Google Books API/quota available, and at least two test accounts. Clear app data only on a disposable test installation; do not erase a lecturer's account data.

### Authentication and ownership

- [ ] Register account A with a unique username; verify profile setup completes.
- [ ] Attempt account B with the same normalized username; verify the claim is rejected without leaving a partial profile.
- [ ] Sign in with valid and invalid credentials; request a reset; sign out; relaunch and verify session restoration.
- [ ] Sign in as B and confirm A's library, review history, notes, theme preference, friend requests, and friends are not shown as B's private data.

### Discovery and library

- [ ] Search title, author, ISBN, and a genre; paginate and open a result's details.
- [ ] Verify missing-cover fallback, no-results state, API error/rate-limit state, and cache fallback after network loss.
- [ ] Add a book to Want to Read, move it to Currently Reading, set progress, save a private note, mark Read, and remove another test entry.
- [ ] Verify negative/over-total progress is rejected; verify shelf changes do not duplicate entries and completed dates/statistics are consistent.
- [ ] Search, filter, and sort the shelves; confirm reading notes are absent from profiles and activities.

### Reviews and social features

- [ ] Create, edit, and delete a 1–5 star rating/review; verify average, count, dates, and My Reviews navigation.
- [ ] Confirm deleting a review leaves the library entry intact and another account cannot edit/delete it.
- [ ] Search a reader, inspect the public profile, send/accept/reject/cancel requests, remove a friend, and check duplicate/self requests.
- [ ] Confirm the feed shows only accepted friends' additions, starts, milestones, completions, and review changes; confirm no private notes appear.

### Themes, offline behavior, and demonstration

- [ ] Preview and select Gothic, Gothic Romance, Dark Academia, Dreamscape, and Minimalist; check auth, discovery, details, library, review, social, and navigation surfaces.
- [ ] Restart the app and verify the theme remains selected. Switch accounts and verify each account's preference is isolated; verify signed-out selection remains anonymous and does not overwrite account preference.
- [ ] Disable network, make a library/review change, reconnect, and verify the pending state clears without duplicate entries. Confirm cached books remain viewable.
- [ ] Complete the lecturer sequence below on a configured test account; show the Firebase configuration/setup state clearly if services are unavailable.

## Recommended lecturer demonstration sequence

1. Sign in with a prepared test account and show the reader profile.
2. Search for a title or author and open Book Details.
3. Add the book to Want to Read, then move it to Currently Reading.
4. Update page progress and show the private notes field without exposing the note in public activity.
5. Rate and review the book; show the average/count and My Reviews history.
6. Search for a second prepared test reader, send/accept a friend request, and open the friend's public profile.
7. Show the Friends list and a friends-only activity item.
8. Switch through the five themes and restart once to demonstrate persistence.
9. Show reading statistics calculated from the saved library entries.

## Remaining limitations and known demo risks

- Without Firebase credentials/configuration, steps requiring sign-in or real cloud records cannot be demonstrated. Do not describe the app's offline/unconfigured screen as a working authentication demo.
- Firestore security rules are not live-validated. Rule deployment/configuration must be completed before presenting cross-account privacy or social authorization as verified.
- Google Books previously hit an anonymous quota limit; a lecturer demo may show a handled error or cached data unless a valid quota-backed API key is configured.
- The emulator smoke check confirmed activity startup and accessibility semantics, but its headless screenshot was black and taps did not reliably navigate. A visual and touch-interaction check on an Android device or visible emulator remains required.
- Cross-device sync, offline reconnection/conflict behavior, and multiple-account transitions need manual service-backed testing. The passing JVM suite does not prove these integrations.
- The build has a non-blocking Gradle/Kotlin forward-compatibility warning.

Phase 8 is complete. Phase 9 has not been started.
