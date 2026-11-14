package com.example.overlay

import com.example.model.FloatingIslandEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FloatingIslandEventBus {
  private val _activeEvent = MutableStateFlow<FloatingIslandEvent?>(null)
  val activeEvent: StateFlow<FloatingIslandEvent?> = _activeEvent.asStateFlow()

  private val _isExpanded = MutableStateFlow(false)
  val isExpanded: StateFlow<Boolean> = _isExpanded.asStateFlow()

  fun postEvent(event: FloatingIslandEvent, autoExpand: Boolean = true) {
    _activeEvent.value = event
    _isExpanded.value = autoExpand
  }

  fun setExpanded(expanded: Boolean) {
    _isExpanded.value = expanded
  }

  fun dismiss() {
    _isExpanded.value = false
    _activeEvent.value = null
  }
}
