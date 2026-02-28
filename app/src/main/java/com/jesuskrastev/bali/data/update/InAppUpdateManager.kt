package com.jesuskrastev.bali.data.update

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.jesuskrastev.bali.domain.model.UpdateState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InAppUpdateManager @Inject constructor(
    private val appUpdateManager: AppUpdateManager
) {
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val installStateListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                _updateState.update {
                    UpdateState.Downloading(
                        bytesDownloaded = state.bytesDownloaded(),
                        totalBytes = state.totalBytesToDownload()
                    )
                }
            }
            InstallStatus.DOWNLOADED -> {
                _updateState.update { UpdateState.Downloaded }
            }
            InstallStatus.INSTALLING -> {
                _updateState.update { UpdateState.Installing }
            }
            InstallStatus.FAILED -> {
                _updateState.update { UpdateState.Failed("La instalación ha fallado") }
            }
            InstallStatus.CANCELED -> {
                _updateState.update { UpdateState.Idle }
            }
            else -> { /* No-op for other states */ }
        }
    }

    fun checkForUpdate() {
        _updateState.update { UpdateState.Checking }
        appUpdateManager.registerListener(installStateListener)

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { updateInfo ->
                when {
                    // Update available — check if IMMEDIATE is allowed (high priority)
                    updateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                        val isImmediate = updateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) &&
                                updateInfo.updatePriority() >= IMMEDIATE_UPDATE_PRIORITY
                        val isFlexible = updateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

                        if (isImmediate || isFlexible) {
                            _updateState.update {
                                UpdateState.Available(
                                    stalenessDays = updateInfo.clientVersionStalenessDays(),
                                    priority = updateInfo.updatePriority(),
                                    isImmediate = isImmediate
                                )
                            }
                        } else {
                            _updateState.update { UpdateState.NotAvailable }
                        }
                    }
                    // Already downloaded, pending install
                    updateInfo.installStatus() == InstallStatus.DOWNLOADED -> {
                        _updateState.update { UpdateState.Downloaded }
                    }
                    // Immediate update already in progress (e.g. user reopened app)
                    updateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        _updateState.update {
                            UpdateState.Available(
                                stalenessDays = updateInfo.clientVersionStalenessDays(),
                                priority = updateInfo.updatePriority(),
                                isImmediate = true
                            )
                        }
                    }
                    else -> {
                        _updateState.update { UpdateState.NotAvailable }
                    }
                }
            }
            .addOnFailureListener { exception ->
                _updateState.update {
                    UpdateState.Failed(exception.localizedMessage ?: "Error al buscar actualizaciones")
                }
            }
    }

    /**
     * Starts a flexible update — user can keep using the app while it downloads.
     * Shows the official Google Play bottom sheet.
     */
    fun startFlexibleUpdate(activity: Activity) {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { updateInfo ->
                if (updateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    updateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    appUpdateManager.startUpdateFlowForResult(
                        updateInfo,
                        activity,
                        AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE),
                        REQUEST_CODE_UPDATE
                    )
                }
            }
    }

    /**
     * Starts an immediate (forced) update — full-screen Google Play UI that blocks
     * the user from using the app until the update is installed.
     */
    fun startImmediateUpdate(activity: Activity) {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { updateInfo ->
                if (updateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    updateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                ) {
                    appUpdateManager.startUpdateFlowForResult(
                        updateInfo,
                        activity,
                        AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE),
                        REQUEST_CODE_UPDATE
                    )
                } else if (updateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    // Resume an immediate update that was already started
                    appUpdateManager.startUpdateFlowForResult(
                        updateInfo,
                        activity,
                        AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE),
                        REQUEST_CODE_UPDATE
                    )
                }
            }
    }

    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    /**
     * Checks if a previously downloaded update is waiting to be installed.
     * Should be called in onResume() to handle the case where the user
     * backgrounded the app during download.
     */
    fun checkForDownloadedUpdate() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { updateInfo ->
                if (updateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    _updateState.update { UpdateState.Downloaded }
                } else if (updateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    // Resume an interrupted immediate update
                    _updateState.update {
                        UpdateState.Available(
                            stalenessDays = updateInfo.clientVersionStalenessDays(),
                            priority = updateInfo.updatePriority(),
                            isImmediate = true
                        )
                    }
                }
            }
    }

    fun unregisterListener() {
        appUpdateManager.unregisterListener(installStateListener)
    }

    companion object {
        const val REQUEST_CODE_UPDATE = 1001

        /**
         * Updates with priority >= this value will trigger an IMMEDIATE (forced) update.
         * Set the priority per release in Google Play Console (Edit release > Advanced settings)
         * or via the Google Play Developer API (inAppUpdatePriority: 0-5).
         *
         * Priority 0-3 → Flexible update (user can skip)
         * Priority 4-5 → Immediate update (user is forced to update)
         */
        const val IMMEDIATE_UPDATE_PRIORITY = 4
    }
}
