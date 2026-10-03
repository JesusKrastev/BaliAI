package com.jesuskrastev.bali.ui.screens.subscription

import com.google.common.truth.Truth.assertThat
import com.jesuskrastev.bali.domain.model.PremiumSubscription
import org.junit.Test
import java.time.ZoneOffset

class ManageSubscriptionViewModelTest {

    private val renewal = 1_798_761_600_000L // 2027-01-01T00:00:00Z

    private fun describe(subscription: PremiumSubscription?) =
        describeSubscription(subscription, ZoneOffset.UTC)

    @Test
    fun `no entitlement is inactive`() {
        assertThat(describe(null)).isEqualTo(ManageSubscriptionUiState.Inactive)
    }

    @Test
    fun `an auto-renewing plan shows its renewal date`() {
        val state = describe(PremiumSubscription(true, false, renewal, null))

        assertThat(state).isEqualTo(
            ManageSubscriptionUiState.Active("Activa", "Se renueva el 1 de enero de 2027.", false)
        )
    }

    @Test
    fun `a cancelled plan says until when access lasts and that no more charges come`() {
        val state = describe(PremiumSubscription(false, false, renewal, null))

        assertThat(state).isEqualTo(
            ManageSubscriptionUiState.Active(
                "Cancelada",
                "Seguirás teniendo acceso completo hasta el 1 de enero de 2027. No se te volverá a cobrar.",
                true
            )
        )
    }

    @Test
    fun `a trial warns when charging starts`() {
        val state = describe(PremiumSubscription(true, true, renewal, null)) as ManageSubscriptionUiState.Active

        assertThat(state.headline).isEqualTo("Prueba gratis")
        assertThat(state.detail).contains("1 de enero de 2027")
    }

    @Test
    fun `an entitlement that never expires has no date`() {
        val state = describe(PremiumSubscription(true, false, null, null)) as ManageSubscriptionUiState.Active

        assertThat(state.detail).isEqualTo("Tu acceso a Bali no caduca.")
    }
}
