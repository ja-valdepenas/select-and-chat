# Select & Chat

Adds a **Chat on WhatsApp** item to Android's text-selection toolbar. Select a phone
number anywhere on the phone, tap it, and land straight in that WhatsApp chat — no
saving the contact, no copy-paste, no country-code fiddling.

Works with unsaved numbers, because `https://wa.me/<international digits>` opens a chat
with any number.

No permissions. No network calls of its own. Nothing leaves the device.

## How it works

An activity with an `android.intent.action.PROCESS_TEXT` intent filter appears in the
floating selection toolbar, labelled by its `android:label`. Same mechanism Edge uses for
"Add to Edge". On tap, the selection is parsed with libphonenumber and handed to WhatsApp
via an explicit-package `ACTION_VIEW` intent.

## Things that are the way they are for a reason

**The toolbar item shows up for *every* text selection, not just numbers.** Android has no
content-type matching for `PROCESS_TEXT` — you cannot scope it to phone numbers. So
validation happens after the tap, and a selection that resolves to nothing falls back to a
sheet with manual entry rather than a dead end.

**There is no "Call on WhatsApp" action, deliberately.** No public deep link starts a
WhatsApp call to an arbitrary number. The only working route queries `ContactsContract`
for a `vnd.android.cursor.item/vnd.com.whatsapp.voip.call` row, needs `READ_CONTACTS`, and
only works for numbers *already saved as contacts* — i.e. never for the unsaved numbers
this app exists for. Inside the chat the call button is one tap away.

**Matches carry a `confident` flag.** libphonenumber runs in up to three passes, and the
last, forgiving one will resolve `Order #100045678` to a phone number because nine digits
is a valid Spanish length. Only *confident* single matches skip straight to WhatsApp;
anything looser is shown for confirmation first.

**Settings live in DataStore, but the no-UI path reads them synchronously.**
`SettingsRepository` exposes a `Flow<AppSettings>` for the ViewModel and a
`blockingSnapshot()` for `ProcessTextActivity`, which decides whether to render anything
at all inside `onCreate`. A coroutine hop there would reintroduce the visible flash that
the translucent no-UI path exists to avoid, so that one caller blocks on a single file
read — the same thing the SharedPreferences it replaced was doing. The three old
SharedPreferences files are carried over once by a `DataMigration`.

**minSdk is 33, on purpose.** Below Android 13 the per-app language store has to be
backported by AppCompat, which means AppCompat themes on the window and a pre-Material-3
look leaking into anything the platform draws. With 33 as the floor the app talks to
`LocaleManager` directly, both activities are plain `ComponentActivity`, and the window
theme is platform `Theme.DeviceDefault.DayNight` purely to dress the launch frame until
`setContent` runs.

**The language is not stored by this app at all.** It is written through `LocaleManager`,
so Settings here and System settings > Apps > Select & Chat > Language are reading and
editing the same value rather than two that can disagree. "System default" clears the
override rather than writing one, which is what keeps a first launch following the device.

**Colour is a tonal system, not a list of hex values.** See `ui/theme/Color.kt`: a source
green is expanded into tonal palettes, and every Material role is that palette picked at a
fixed tone, so contrast falls out of the tone arithmetic instead of being eyeballed. The
five theme options — System, Light, Dark, Green, AMOLED — are the same role assignment
over four different *neutral* ramps, which is why the accents and every contrast pairing
are identical across all three dark ones. `SYSTEM` resolves to Green in the dark, because
that is the app's own identity rather than plain grey.

**Turning off "Save recent chats" does not delete what is already saved.** An earlier
build wiped the history when the switch went off, on the grounds that a toggle leaving old
data behind would be a lie. That conflates two things the user may want separately, and it
makes an undoable switch destructive. The switch now only stops new chats being recorded;
deleting lives behind **Manage history**, and clearing everything asks first because it
affects many records at once.

**Quick-message text is never touched.** Messages carry an id so they can be edited in
place, and the text is stored exactly as typed — not normalised, not translated with the
rest of the UI. It is URL-encoded only at the point it becomes a `wa.me?text=` parameter.

**Number length warnings come from libphonenumber, not a table.** Valid lengths differ
per country (8 in El Salvador, 10 in the US, 11 in Germany) and several countries accept
more than one length, so `isPossibleNumberWithReason()` is consulted instead of a
hardcoded maximum. Note that an over-length number often reports `INVALID_LENGTH` rather
than `TOO_LONG` for exactly that reason - both are treated as "will not work". The
warning stays silent while a number is merely incomplete, so it never nags mid-typing.

**The country is detected once, then never again.** On first run only, the region is read
from the SIM, falling back to the network, then the device locale. Neither
`TelephonyManager` getter needs a runtime permission - verified on-device, since the app
declares none. After that first guess it is a manual setting, so travelling or roaming
never silently rewrites the number you are about to message.

**Every screen sits inside a root `Surface`.** Without one, Compose never paints
`colorScheme.background` (you get the Android window background from `themes.xml`
instead) and `LocalContentColor` stays at its default black - which looked exactly like
"the theme setting does nothing".

**The country list is generated at runtime**, from libphonenumber's supported regions, the
JDK's localized country names, and flag emoji built from ISO codes. No bundled
`countries.json`, no flag images. It is a function of a `Locale` rather than a lazy
singleton, because the app's language can change while it is running and a list built once
at first use would keep showing "Germany" after a switch to Spanish.

**The split button is two ordinary `Button`s, not `SplitButtonLayout`.** In material3
1.5.0-alpha18 the shape types `SplitButtonDefaults` returns do not line up with what
`Button` accepts, so the geometry — round on the outside, tight on the seam — is built
here. Each half keeps its own touch target, focus stop and semantics, which is the point
of the component.

**Circle to Search / Google Lens will not show the item.** That overlay has a fixed,
Google-controlled action list. The item appears in the standard selection toolbar — Chrome,
Gmail, Messages, and most apps.

## Build

```bash
./gradlew :app:assembleDebug
```

Requires JDK 17 and Android SDK 36.

> **Note for this machine:** the system `PATH` contains a stray `"` character, which breaks
> the Gradle test-executor JVM launch (`Could not find or load main class Files\...`). Until
> it is fixed, prefix Gradle commands with a clean PATH:
> `PATH="/c/Program Files/Java/jdk-17/bin:/c/Windows/system32:/c/Windows:/usr/bin" ./gradlew ...`

## Test

```bash
./gradlew :app:testDebugUnitTest
```

The JVM tests cover the pure layer — number extraction, the theme and language mappings,
the quick-message and history rules, the `wa.me` URL encoding, the country search — plus
two localization guards: every translatable string has a Spanish counterpart, and no
user-visible text is hardcoded in a composable. No Robolectric.

The Compose UI and accessibility tests need a device or emulator:

```bash
./gradlew :app:connectedDebugAndroidTest
```

They cover chip selection and its semantics, long-press Edit/Delete and the accessible
alternatives to it, country search by name and by calling code, the two halves of the
split button, and that Clear history is not reachable as a button on the Recents screen.

Driving the real entry point without making a selection by hand:

```bash
adb shell am start -a android.intent.action.PROCESS_TEXT -t text/plain -e android.intent.extra.PROCESS_TEXT "+34 612 345 678"
```

Test on a physical device — WhatsApp is impractical to install on an emulator.

---

Not affiliated with or endorsed by WhatsApp or Meta.
