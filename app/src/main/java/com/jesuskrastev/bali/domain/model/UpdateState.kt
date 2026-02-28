package com.jesuskrastev.bali.domain.model

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Available(val stalenessDays: Int?, val priority: Int, val isImmediate: Boolean) : UpdateState
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : UpdateState
    data object Downloaded : UpdateState
    data object Installing : UpdateState
    data object NotAvailable : UpdateState
    data class Failed(val error: String) : UpdateState
}
