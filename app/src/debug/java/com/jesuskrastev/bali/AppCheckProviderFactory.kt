package com.jesuskrastev.bali

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * The App Check provider for debug builds.
 *
 * A debug build is not installed from Play, so Play Integrity cannot attest it. This
 * provider prints a token to Logcat on first run that has to be registered once per
 * machine in Firebase console → App Check → Apps → Debug tokens; until it is, every AI
 * request from a debug build is rejected.
 *
 * It lives in the debug source set on purpose: the library backing it ships only as
 * `debugImplementation`, so the class that would let anyone forge an attestation never
 * reaches the released APK.
 *
 * @return the debug attestation provider.
 */
fun appCheckProviderFactory(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
