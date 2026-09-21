package com.jesuskrastev.bali.ui.navigation

import androidx.lifecycle.ViewModel
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Relays navigation changes to analytics, so the Compose navigation host can report screen
 * views without holding an [AnalyticsTracker] itself.
 */
@HiltViewModel
class ScreenTrackingViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    /**
     * Reports that a destination became the visible one.
     *
     * @param route the destination's route pattern as the navigation library exposes it, or
     *   null for destinations without one
     */
    fun onDestinationChanged(route: String?) {
        screenNameOf(route)?.let(analyticsTracker::screenViewed)
    }
}

/**
 * Turns a type-safe route pattern into a stable screen name by dropping the package, the
 * arguments and the `Route` suffix: `com.jesuskrastev.bali.ui.navigation.TestRoute?topic={topic}`
 * becomes `Test`.
 *
 * @param route the route pattern of a destination, or null when it has none
 * @return the screen name, or null when there is nothing to report
 */
internal fun screenNameOf(route: String?): String? =
    route
        ?.substringBefore('?')
        ?.substringBefore('/')
        ?.substringAfterLast('.')
        ?.removeSuffix("Route")
        ?.takeIf { it.isNotBlank() }
