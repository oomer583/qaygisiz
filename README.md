# Qayğısız

**An Android app that catches scam SMS on an elderly person's phone and warns their family — not them.**

Built for EurekaDev 2026 (Coding Track) by Ömər Kərimli, 14, Baku, Azerbaijan.

---

## The problem

Scam SMS works because it targets the person least able to judge it. An elderly person gets a message saying their bank account is blocked, with a link. They are frightened, they are not going to inspect a domain name, and they often do not want to bother their children with it.

Every existing app I looked at warns **the person who received the message**. That is the one person whose judgement the scam has already defeated.

## What Qayğısız does differently

The warning goes to a **designated family member** over Telegram. The elderly user is never shown a scary dialog, never asked to make a decision, and never has to understand what a shortened URL is. Their son or daughter gets a message in plain language and calls them.

## How it works

```
SMS arrives
   |
   v
[ on-device scoring ]      free, instant, no network
   |  lookalike bank domains, URL shorteners, bare IP links,
   |  punycode, suspicious TLDs, urgency wording, sender mismatch
   |
   +-- clean  -> nothing happens, nothing leaves the phone
   |
   v  suspicious or dangerous
[ AI explanation ]         OpenRouter (free models) -> Gemini fallback
   |  turns the technical findings into two plain sentences
   v
[ Telegram ]               sent to the family member's chat
```

The local layer exists so the app is free to run. Only flagged messages ever leave the device — a normal SMS is scored and forgotten without a single network call.

The model layer has four levels of fallback (three free OpenRouter models, then Gemini). A warning system that goes silent because one provider is busy is not a warning system.

## Privacy

- Clean messages never leave the phone.
- Only a flagged message's text is sent to the model provider, for the explanation.
- No account, no server of our own, no analytics.
- The Telegram bot token and chat ID live in the app's private storage on the device.

## Languages

Azerbaijani, English and Russian. The detection layer returns reason **codes**, not strings, so adding a language is one file. The AI explanation follows the selected language.

## Build it yourself

```bash
git clone https://github.com/oomer583/qaygisiz.git
cd qaygisiz
```

Two files are not in the repository because they hold credentials. Create them:

**1. `app/src/main/java/com/omer/qaygisiz/Secrets.kt`** — copy `Secrets.kt.example` and fill in what you have. All four values may be left empty; the app will simply ask for them on its setup screen.

**2. `keystore.properties`** — only needed for a signed release build. Skip it and `assembleDebug` still works.

```bash
./gradlew test            # unit tests for the detection layer, no device needed
./gradlew assembleDebug   # installable APK
```

## Using it

1. Install the APK on the phone you want to protect.
2. In Telegram, create a bot with [@BotFather](https://t.me/BotFather) and copy the token.
3. The family member who should receive the warnings opens the bot and presses Start.
4. Open `https://api.telegram.org/bot<TOKEN>/getUpdates` and read the `chat.id`.
5. Enter the token and the chat ID on the app's setup screen, choose a language, say who is being protected, grant the SMS permission, and press **Send a test message**.

Step 2 to 4 is the part that still asks too much of an ordinary user. Replacing it with a single "Link Telegram" button is the next thing on the list.

## Tests

`LinkScannerTest` covers the detection layer with 8 cases: a normal message, an official bank link, a shortened link with urgency wording, a lookalike bank domain, a bare IP address, an urgent message with no link at all, Azerbaijani diacritics, and a multi-part message. They run on the JVM, no emulator required.

## Tech

Kotlin, Jetpack Compose, Material 3. `BroadcastReceiver` for SMS, `SharedPreferences` for settings, `HttpURLConnection` for the two HTTP calls. No third-party libraries — the dependency list is what Android Studio generates plus nothing.

`minSdk 24`, so it runs on phones from 2016 onward. The people this app protects do not have new phones.

## What is not done yet

- One shared bot with a one-tap pairing flow, instead of asking the user to create their own.
- Reading WhatsApp messages through a notification listener.
- A history screen showing what was flagged and when.
- Measurement: a corpus of real scam messages, with caught/missed numbers published here.

## Licence

MIT.
