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

**`Prefs` uses SharedPreferences, not DataStore.** It is read synchronously in
`ProcessTextActivity.onCreate` before deciding whether to render anything. DataStore's
suspend API would force a coroutine hop and reintroduce the visible flash the translucent
no-UI path exists to avoid.

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
`countries.json`, no flag images.

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

`PhoneNumberExtractor` has no Android dependencies, so its tests run on the plain JVM — no
Robolectric.

Driving the real entry point without making a selection by hand:

```bash
adb shell am start -a android.intent.action.PROCESS_TEXT -t text/plain -e android.intent.extra.PROCESS_TEXT "+34 612 345 678"
```

Test on a physical device — WhatsApp is impractical to install on an emulator.

---

Not affiliated with or endorsed by WhatsApp or Meta.
