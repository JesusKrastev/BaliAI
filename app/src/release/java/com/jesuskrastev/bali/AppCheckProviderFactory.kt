package com.jesuskrastev.bali

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * The App Check provider for release builds.
 *
 * Play Integrity asks Google to vouch that this really is the app you published, running
 * on a genuine device — which is what keeps a stranger from calling the Firebase AI Logic
 * endpoint that now holds the Gemini credentials.
 *
 * @return the Play Integrity attestation provider.
 */
fun appCheckProviderFactory(): AppCheckProviderFactory =
    PlayIntegrityAppCheckProviderFactory.getInstance()
