package com.example.exoplayercompose

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.exoplayercompose.player.CustomLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.analytics.AnalyticsCollector
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.analytics.DefaultAnalyticsCollector
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.util.Clock

@Composable
fun rememberExoPlayer(context : Context ,analyticsListener: AnalyticsListener) = remember {
    val analyticsCollector : AnalyticsCollector = DefaultAnalyticsCollector(Clock.DEFAULT)
    analyticsCollector.addListener(analyticsListener)
    val trackSelector = DefaultTrackSelector(context, AdaptiveTrackSelection.Factory())
    trackSelector.setParameters(TrackSelectionParameters.Builder(context).build())
    ExoPlayer.Builder(context)
        .setTrackSelector(trackSelector)
        .setAnalyticsCollector(analyticsCollector)
        .setBandwidthMeter(DefaultBandwidthMeter.getSingletonInstance(context))
        .setSeekForwardIncrementMs(30000)
        .setSeekBackIncrementMs(30000)
        .setLoadControl(CustomLoadControl.Builder().build())
        .build()
}