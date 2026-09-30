package com.jesuskrastev.bali.ui.screens.paywall

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.data.analytics.AnalyticsTracker
import com.jesuskrastev.bali.util.MainDispatcherRule
import com.revenuecat.purchases.customercenter.CustomerCenterManagementOption
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class CustomerCenterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val analytics = mock<AnalyticsTracker>()
    private val listener = CustomerCenterViewModel(analytics).listener

    @Test
    fun `opening the Customer Center tracks nothing beyond the screen view`() {
        verifyNoInteractions(analytics)
    }

    @Test
    fun `answering the cancellation survey tracks the chosen reason`() {
        listener.onFeedbackSurveyCompleted("too_expensive")

        verify(analytics).subscriptionCancelReason("too_expensive")
    }

    @Test
    fun `tapping cancel tracks the intent to leave`() {
        listener.onManagementOptionSelected(CustomerCenterManagementOption.Cancel)

        verify(analytics).customerCenterOptionSelected("cancel")
    }

    @Test
    fun `being sent to Google Play tracks the hand-off`() {
        listener.onShowingManageSubscriptions()

        verify(analytics).subscriptionManagementOpened()
    }

    @Test
    fun `every management option has a stable analytics name`() {
        assertThat(analyticsNameOf(CustomerCenterManagementOption.Cancel)).isEqualTo("cancel")
        assertThat(analyticsNameOf(CustomerCenterManagementOption.MissingPurchase))
            .isEqualTo("missing_purchase")
        assertThat(analyticsNameOf(CustomerCenterManagementOption.CustomUrl(mock<Uri>())))
            .isEqualTo("custom_url")
        assertThat(analyticsNameOf(CustomerCenterManagementOption.CustomAction("help", null)))
            .isEqualTo("custom_action")
    }
}
