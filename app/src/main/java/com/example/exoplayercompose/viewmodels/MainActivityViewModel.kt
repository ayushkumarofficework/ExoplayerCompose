package com.example.exoplayercompose.viewmodels

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exoplayercompose.model.PlayerState
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.STATE_READY
import com.google.android.exoplayer2.analytics.AnalyticsListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivityViewModel : ViewModel() {
    var playerControllerVisibilityJob : Job? = null
    fun onPlayerClicked() {
        playerControllerVisibilityJob?.cancel()
        playerControllerVisibilityJob = viewModelScope.launch {
            var isControllerVisible = playerState.value.isControllerVisible
            if(!isControllerVisible) {
                playerState.value = playerState.value.copy(isControllerVisible = playerState.value.isControllerVisible.not())
                delay(5000)
                playerState.value = playerState.value.copy(isControllerVisible = playerState.value.isControllerVisible.not())
            }else{
                playerState.value = playerState.value.copy(isControllerVisible = playerState.value.isControllerVisible.not())
            }
        }
    }

    private val playerState : MutableState<PlayerState> = mutableStateOf(PlayerState("https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4"))

    val _playerState : State<PlayerState>
        get() = playerState

    val analyticsListener = object : AnalyticsListener{
        override fun onEvents(player: Player, events: AnalyticsListener.Events) {
            super.onEvents(player, events)
            playerState.value = playerState.value.copy(playerCurrentPosition = player.currentPosition, bufferPercentage = player.bufferedPercentage, totalDuration = player.duration)
        }

        override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
            super.onPlaybackStateChanged(eventTime, state)
            playerState.value = playerState.value.copy(playbackState = state)
        }

        override fun onPlayWhenReadyChanged(
            eventTime: AnalyticsListener.EventTime,
            playWhenReady: Boolean,
            reason: Int
        ) {
            super.onPlayWhenReadyChanged(eventTime, playWhenReady, reason)
            playerState.value = playerState.value.copy(playWhenReady = playWhenReady)
        }
    }

}