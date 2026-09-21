# Play Store release — Select & Chat

Everything needed for the first submission of `dev.jvald.selectandchat`, in the order the
Play Console asks for it. Copy is written to the console's character limits.

---

## 1. Signing and bundle (done)

| Item | Value |
| --- | --- |
| Upload keystore | `C:\Users\jvald\keystores\selectandchat-upload.jks` |
| Alias | `selectandchat-upload` |
| Algorithm | RSA 4096, valid 10 000 days |
| Password | in `keystore.properties` (untracked) and `C:\Users\jvald\keystores\selectandchat-upload-credentials.txt` |

**Back the keystore and its password up somewhere off this machine.** With Play App
Signing enabled, losing the upload key is recoverable via Google support; losing it
without Play App Signing means the app can never be updated again. Enable **Play App
Signing** when prompted — it is the default for new apps.

The build reads these from `keystore.properties` through `app/build.gradle.kts`. Neither
the key nor the password is in git.

**The signed bundle is built:**

```
app/build/outputs/bundle/release/app-release.aab   4.3 MB
```

versionCode 1, versionName 1.0, minSdk 33, targetSdk 36, signed SHA384withRSA with the
certificate above (valid to Feb 2054). Rebuild any time with:

```bash
./gradlew bundleRelease
```

---

## 2. Store listing copy

**App name** (30 max) — 14 used

```
Select & Chat
```

**Short description** (80 max) — 79 used

```
Select any phone number, tap, and open the WhatsApp chat. No saving the contact.
```

**Full description** (4000 max)

```
Select & Chat puts a "Chat on WhatsApp" item in Android's text-selection toolbar. Highlight a phone number anywhere on your phone — a browser, an email, a note, a listing, a receipt — tap it, and you land straight in that WhatsApp chat.

No saving the contact first. No copy-paste. No fiddling with country codes.

WHY IT EXISTS

Messaging an unsaved number normally means three or four steps: copy the number, open WhatsApp, work out whether it needs a country code, paste it somewhere that accepts it. Select & Chat collapses all of that into one tap on the number you are already looking at.

It works with numbers that are not in your contacts, which is the whole point — a plumber from a classified ad, a seller from a marketplace listing, a number in a confirmation email.

WHAT YOU GET

• Chat from any text selection — the action appears in the same floating toolbar as Copy and Share, system-wide.
• Smart number parsing — numbers are read with Google's libphonenumber, so local formats, spaces, dashes and country codes are all handled. A number that is clearly valid opens immediately; anything ambiguous asks you to confirm first rather than guessing.
• Manual entry fallback — if a selection turns out not to be a number, you get a sheet to type one instead of a dead end.
• Quick messages — save message templates you send often and attach one to the chat as you open it.
• Recent chats — the numbers you have opened, ready to reuse. Optional, and you can clear them whenever you like.
• Share-sheet support — share text containing a number to Select & Chat and it works the same way.
• Material 3 Expressive design — five themes, including a true-black AMOLED option, with dynamic colour on supported devices.
• Per-app language — set the app's language independently of the rest of your phone, from either the app's settings or Android's.

PRIVACY

Select & Chat requests no Android permissions at all. It makes no network calls of its own, contains no analytics, no crash reporting, no advertising and no tracking of any kind. Your recent chats, quick messages and settings are stored only in the app's private storage on your device and never leave it.

REQUIREMENTS

• Android 13 or newer.
• WhatsApp or WhatsApp Business installed.

Select & Chat is an independent app. It is not affiliated with, endorsed by, or connected to WhatsApp or Meta.
```

---

## 3. Graphics

| Asset | Spec | Status |
| --- | --- | --- |
| App icon | 512 × 512 PNG, 32-bit | **done** — `docs/store/play-icon-512.png` |
| Feature graphic | 1024 × 500 PNG | **done** — `docs/store/play-feature-graphic-1024x500.png` |
| Phone screenshots | 2–8, PNG/JPG, 16:9 or 9:16, min 320 px, max 3840 px | **done** — 4 in `docs/store/screenshots/`, 1080 × 2640 |
| Tablet screenshots | optional | skip |

The icon is rendered from the adaptive launcher icon's own geometry, through the central
72/108 window the launcher mask shows — so the listing icon matches the home screen
rather than being a shrunken copy of the full viewport. Regenerate with the scripts noted
in §11 if the launcher art ever changes.

Captured from the Razr at 1080 × 2640, in the AMOLED theme:

| File | Shows |
| --- | --- |
| `01-selection-toolbar.png` | the money shot — a number selected on a marketplace-style page, **Chat on WhatsApp** in the toolbar overflow |
| `02-new-chat.png` | New chat tab: quick messages, country picker, number field |
| `03-recents.png` | Recents tab, the save toggle and Manage history |
| `04-themes.png` | Settings sheet with the five-theme picker open |

**Check before uploading:** `02` and `03` show numbers left over from testing
(`949467785` and `+376 949 464`). If either is a real number, retake or redact — a store
screenshot is public. `01` uses the invented `+34 600 123 456`, which is safe.

Two more worth adding if you want a fuller listing: the confirm sheet for an ambiguous
number, and the Manage quick messages screen.

---

## 4. Data safety form

The manifest declares **zero permissions** and the app makes no network requests, so:

- *Does your app collect or share any of the required user data types?* → **No**
- *Is all of the user data collected by your app encrypted in transit?* → n/a once the above is No
- *Do you provide a way for users to request that their data is deleted?* → **No** (nothing is collected; on-device data is removed by uninstalling or via Manage history)

Recent chats and quick messages are **not** "collected" in Play's sense — that term covers
data leaving the device. Local-only storage is explicitly out of scope.

---

## 5. Content rating questionnaire

Category: **Utility / Productivity / Communication**. Every content question answers **No**
— no violence, sexuality, profanity, drugs, gambling, or user-generated content. The app
does not let users communicate with each other *within the app*; it hands off to WhatsApp.
Expected outcome: rated for everyone (PEGI 3 / ESRB Everyone).

---

## 6. Other declarations

| Question | Answer |
| --- | --- |
| Contains ads | **No** |
| In-app purchases | **No** |
| Target audience | 18+ (or 13+) — not designed for children |
| Government app | No |
| Financial features | None |
| Privacy policy URL | see §7 |

---

## 7. Privacy policy hosting

`docs/privacy-policy.html` is written and ready. To publish it free via GitHub Pages:

1. GitHub → repo → **Settings → Pages**
2. Source: *Deploy from a branch*, Branch: `master`, Folder: `/docs`
3. Save, wait ~1 minute

Resulting URL:

```
https://ja-valdepenas.github.io/whatsapp-smartselect/privacy-policy.html
```

> **Worth considering:** that URL contains "whatsapp", because the repository is still
> named `whatsapp-smartselect`. The app name and icon are clean, which is what the brand
> guidelines actually restrict, but a reviewer skimming for impersonation sees the URL too.
> Renaming the repo to `select-and-chat` removes the last trace and costs one click —
> GitHub redirects the old URL, and the git remote can be updated after.

---

## 8. App access instructions

Play reviewers test on a device that **may not have WhatsApp installed**, and the app's
entire function is a hand-off to WhatsApp. Say so explicitly, or risk a "broken
functionality" rejection:

```
No login is required; all functionality is available immediately.

This app requires WhatsApp (or WhatsApp Business) to be installed on the test device — its only function is to open a WhatsApp chat for a selected phone number.

To test:
1. Install WhatsApp and complete its setup.
2. Open any app containing a phone number in selectable text (for example, type a number into the Chrome address bar or a notes app). A number in international format such as +34 600 000 000 works best.
3. Long-press the number to select it.
4. In the floating selection toolbar, tap "Chat on WhatsApp" (it may be behind the toolbar's overflow "⋮" menu).
5. WhatsApp opens a chat with that number.

The app's own screen — launched from the launcher icon — shows recent chats, quick message templates and settings, and can be reviewed without WhatsApp installed.
```

---

## 9. Console flow

1. **Create app** — name `Select & Chat`, language, *App*, *Free*. Accept the declarations.
2. **Set up your app** (the task list) — work top to bottom; it gates the release.
   - Privacy policy URL → §7
   - App access → §8
   - Ads → No
   - Content rating → §5
   - Target audience → §6
   - Data safety → §4
   - Government apps / financial features → No
3. **Store listing** — copy from §2, upload graphics from §3.
4. **Production → Create new release**
   - Enable **Play App Signing** when offered.
   - Upload `app/build/outputs/bundle/release/app-release.aab`
   - Release name: `1 (1.0)`
   - Release notes: `First release.`
5. **Send for review.**

First-time reviews commonly take a few days and new personal developer accounts may need
the 12-tester closed-testing requirement satisfied first — the console will say so on the
Production track if it applies to this account.

---

## 10. For the next release

Bump both in `app/build.gradle.kts`:

```kotlin
versionCode = 2      // must increase for every upload
versionName = "1.1"  // what users see
```

Then `./gradlew bundleRelease` again — signing is already wired.

---

## 11. Regenerating the graphics

The icon and feature graphic are generated, not hand-drawn, so they stay in step with the
launcher art. The scripts live in this session's scratchpad; copy them into `tools/` if
you want them kept. Both need only Pillow:

```bash
python make_icon.py      # -> docs/store/play-icon-512.png
python make_feature.py   # -> docs/store/play-feature-graphic-1024x500.png
```
