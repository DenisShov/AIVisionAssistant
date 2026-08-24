package com.example.multimodalassistant.ui.state

sealed interface Loadable<out T> {
    data object Idle : Loadable<Nothing>
    data object Loading : Loadable<Nothing>
    data class Ready<T>(val value: T) : Loadable<T>
    data class Failed(val message: String) : Loadable<Nothing>
}

internal fun <T> Loadable<T>.valueOrNull(): T? = (this as? Loadable.Ready)?.value

internal fun Loadable<*>.errorOrNull(): String? = (this as? Loadable.Failed)?.message
