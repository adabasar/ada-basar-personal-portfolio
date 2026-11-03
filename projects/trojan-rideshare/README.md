# TrojanRideShare

An Android app that helps USC students find and share rides: post a ride, search for rides going your way, request to join, and split the cost.

**Team project, USC CSCI 310 (Software Engineering), Fall 2026 — in progress.**
Original repository: [htien314/CSCI310-Nikumusic](https://github.com/htien314/CSCI310-Nikumusic)

## What's here
The app's foundation: project setup, navigation graph, bottom navigation, shared UI state handling (`UiState`, `LoadingErrorView`), and the package layout (model / data / service / ui / util) that each feature builds on.

## Tech stack
Java · Android (XML layouts, Jetpack Navigation) · Firebase Authentication · Cloud Firestore · Google Places API · Gradle (Kotlin DSL)

## Design notes
- Money is stored as integer cents, never floating point
- Times are stored in UTC and shown in Los Angeles time, via an injectable `Clock` for testability
- Repositories return results through a `Callback<T>` interface

## Run it
Open the folder in Android Studio and run on an emulator (min SDK 26). The Firebase config file (`app/google-services.json`) is **not** included in this copy; add your own Firebase project's file to build. The team's original README is in `README.original.md`.
