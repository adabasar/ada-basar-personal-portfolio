# Minesweeper for Android

A Minesweeper game built from scratch as a native Android app.

## Gameplay
- 10 × 10 grid with hidden mines
- Tap to dig a cell; numbers show how many mines are next to it
- Empty areas open up automatically
- Switch to flag mode to mark suspected mines, with a live flag counter
- Built-in timer, and a results screen showing whether you won or lost and how long it took

## Tech stack
Java · Android SDK · GridLayout · Gradle

## Structure
- `MainActivity.java` — board setup, mine placement, reveal logic, flagging and timer
- `ResultActivity.java` — win/lose screen

## Run it
Open the folder in Android Studio and run it on an emulator or Android phone.
