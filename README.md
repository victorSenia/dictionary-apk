# Dictionary

Dictionary is an Android app for vocabulary training. It combines word playback, text-to-speech, grammar exercises, and a local database so you can study imported word lists and practice them in different ways.

## Main features

- Learn words by listening to the source word, spelling, and translations with configurable delays and repetition
- Filter words by language, topic, and other criteria
- Import vocabulary from bundled asset files or external plain-text files
- Save words into a local database, edit them later, and export them again
- Train with dedicated modes for:
  - word matching
  - article matching
  - ordering words in a sentence
  - grammar filtering and grammar practice
- Choose voices for text-to-speech and tune pitch and speed
- Store reusable configuration presets

## Typical workflow

1. Parse a word list from assets or import one from a text file.
2. Review or filter the loaded words.
3. Import the current set into the local database if you want to keep and edit it.
4. Practice with playback or one of the training screens.
5. Adjust knowledge values, voices, and playback settings as needed.

## Bundled content

The app ships with sample data in `app/src/main/assets`, including:

- vocabulary lists
- grammar files
- sentence exercises
- default parsing configuration

The default parse configuration in `app/src/main/assets/parseWordsConfig.properties` is set up for German source words with English/Ukrainian targets.

## Build

Requirements:

- Android Studio / Android SDK 36
- JDK 8+
- Android 12+ device or emulator (`minSdk 31`)

Build debug APK:

```powershell
.\gradlew.bat assembleDebug
```

Run unit tests:

```powershell
.\gradlew.bat testDebugUnitTest
```

Run instrumented tests:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## External tools and dependencies

- Hosted dictionary editor: <https://victorsenia.github.io/dictionary-editor/>
- The app depends on `common-1.0.0.jar`, published from <https://github.com/victorSenia/dictionary/releases>

## Permissions

- foreground media playback service permissions for audio playback
