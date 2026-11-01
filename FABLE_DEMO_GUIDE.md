# FABLE — Lecturer Demonstration Guide

## Before the demonstration

- Use a visible Android device or emulator and install `app/build/outputs/apk/debug/app-debug.apk`.
- For the complete demonstration, configure the Firebase Android app (`app/google-services.json`), enable Email/Password Authentication, create Firestore, and deploy the current `firestore.rules`.
- Prepare two test accounts and establish one friend relationship before class if you want to show the friends list and activity feed quickly.
- Configure a restricted Google Books API key through a local Gradle property or environment variable named `GOOGLE_BOOKS_API_KEY`; verify quota before presenting.
- Keep personal notes and real email addresses out of projected public screens. Use test accounts and non-sensitive demo text.
- Do not describe the unconfigured sign-in screen as successful authentication, or local pending indicators as cloud synchronization.

## 5–7 minute run of show

### 0:00–0:30 — Launch and sign in

Open Fable and sign in with the prepared account. Point out the reader username in the app bar and the Home shortcuts. If starting from registration, use the prepared unique username and complete the profile fields.

### 0:30–1:10 — Discover a book

Open Discover or Search. Search by title or author, then briefly show the title/author search mode. Open a result to Book Details and point out the metadata and cover fallback behavior.

### 1:10–2:00 — Save and track a book

From Book Details, add the book to Want to Read. Open My Library, move it to Currently Reading, and enter a page value within the displayed total. If time permits, add a clearly private note, then save and show the progress state.

### 2:00–2:45 — Rate and review

Return to Book Details. Choose a star rating, write a short non-sensitive review, and save it. Show the average/count and then open My Reviews to demonstrate the personal review history and link back to the book.

### 2:45–3:45 — Reader profile and friendships

Open Discover Readers and search for the second prepared account. Visit its public profile, show aggregate reading statistics, and send a request. Accept it from the second account, or begin with a pre-accepted friendship to save time. Show Friends and navigate to the friend's profile.

### 3:45–4:30 — Activity feed

Open Activity Feed and point out a library, progress, completion, or review event from the accepted friend. Explain that activity records exclude private reading notes and the feed is restricted to accepted friends by the Firestore rules.

### 4:30–5:15 — Themes

Open Appearance from Home or the owner's profile. Preview Gothic, Gothic Romance, Dark Academia, Dreamscape, and Minimalist, then choose one. Show that the current app palette, typography, and component shapes change together. Restarting the app demonstrates persistence.

### 5:15–5:45 — Statistics and wrap-up

Open Reading Statistics. Explain that completion and page totals are calculated from saved library records. Close by describing the Compose/ViewModel/domain/repository split and the current service-verification limits.

For a strict five-minute slot, pre-establish the friendship, omit profile editing, and show one theme switch and one statistics view.

## Fallbacks

### Firebase configuration missing

The app displays “Firebase setup is required” on the signed-out screen. Use this only to explain that authentication is real Firebase Auth and the workspace has no project config. Do not claim sign-in, profile data, shelves, reviews, friendships, activity, or cloud sync were demonstrated. Theme selection and local anonymous theme persistence can still be shown. For the complete sequence, set up a Firebase project and two test accounts before class.

### Google Books quota or network unavailable

Show the handled error state and explain that a prior anonymous request hit HTTP 429. If a matching cached result is available, demonstrate that fallback and label it as cached data. Do not claim a successful live API result without one. The preferred fallback is to configure a restricted API key and check quota before the session.

### Emulator screenshot or touch problems

Phase 8's headless emulator launched the activity and exposed the login screen through its accessibility hierarchy, but screenshot capture was black and touch navigation unreliable. Use a physical Android device or a visible emulator. Do not insert generated mockups as if they were actual application screenshots. If no device works, show the code and test report, explain the limitation, and present screenshots captured manually from a known-good configured device later.

### Offline writes

If the network is unavailable, distinguish cached books and local pending library/review entries from cloud-confirmed records. Reconnect and verify the pending state clears before saying the operation synchronized. The cross-service offline path was not live-validated in this workspace.

## Manual screenshot checklist

Capture genuine screenshots on a working configured device. Hide personal email addresses, tokens, private notes, and notifications before capture. Suggested files:

- [ ] Home with the signed-in demo username.
- [ ] Discover or Search results with a visible query.
- [ ] Book Details and Reviews section.
- [ ] My Library with a saved shelf entry and progress.
- [ ] My Reviews history.
- [ ] Reader public profile with aggregate statistics only.
- [ ] Friends list and Friend Requests screen.
- [ ] Activity Feed showing an accepted friend's event.
- [ ] Theme selector and one alternate theme applied.
- [ ] Reading Statistics generated from the test account's saved records.
