package com.example.exoplayercompose



import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import com.example.exoplayercompose.model.PlayerState
import com.example.exoplayercompose.ui.theme.ExoplayerComposeTheme
import com.example.exoplayercompose.viewmodels.MainActivityViewModel
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player.*
import com.google.android.exoplayer2.analytics.AnalyticsCollector
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.analytics.DefaultAnalyticsCollector
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.ads.AdsMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.Clock
import com.google.android.exoplayer2.util.Util

class MainActivity : ComponentActivity() {
    val TAG = "ExoplayerCompose"

    var playerView : StyledPlayerView? = null

    var adsLoader : ImaAdsLoader? = null

    lateinit var mainActivityViewModel : MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivityViewModel = ViewModelProvider.NewInstanceFactory().create(MainActivityViewModel::class.java)
        setContent {
            ExoplayerComposeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier,
                    color = MaterialTheme.colors.background,
                ) {
                    PlayerScreen(this::getPlayerView,this::onPlayerViewUpdated,mainActivityViewModel._playerState,mainActivityViewModel.analyticsListener,this::getMediaSource,this::onPlayerClicked, modifier = Modifier.wrapContentHeight())
                }
            }
        }
    }

    private fun onPlayerClicked() {
        mainActivityViewModel?.onPlayerClicked()
    }

    private fun getMediaSource(videoUrl : String,adUrl : String?,exoPlayer: ExoPlayer?,context : Context) : MediaSource {
        val dataSourceFactory = DefaultDataSource.Factory(context)
        var mediaSource = buildMediaSource(videoUrl,dataSourceFactory)
        if(adsLoader == null) {
            adsLoader = ImaAdsLoader.Builder(context).setAdEventListener { adEvent ->
                adEvent?.let {
                    Log.e("ExoplayerCompose","onAdEvent")
                }
            }.setAdErrorListener{ adErrorEvent ->
                adErrorEvent?.let {
                    Log.e("ExoplayerCompose","onAdError "+it.error.message)
                }
            }.build()
        }
        adsLoader?.setPlayer(exoPlayer)
        val mediaSourceFactory =
            DefaultMediaSourceFactory(dataSourceFactory)
        adUrl?.let {
            mediaSource = AdsMediaSource(
                mediaSource, DataSpec(Uri.parse(it)),
                Any(),
                mediaSourceFactory,
                adsLoader!!,
                {playerView}
            )
        }
        return mediaSource
    }

    private fun buildMediaSource(videoUrl : String, dataSourceFactory : DefaultDataSource.Factory) : MediaSource {
        //        @C.ContentType int type = Util.inferContentType(uri, overrideExtension);
        val type: @C.ContentType Int = Util.inferContentType(Uri.parse(videoUrl))
        val mediaItemBuilder: MediaItem.Builder = MediaItem.Builder().setUri(videoUrl)
        return when (type) {
            C.CONTENT_TYPE_DASH -> //                    return new DashMediaSource.Factory(dataSourceFactory).setDrmSessionManager(drmSessionManager).setLoadErrorHandlingPolicy(errorHandlingPolicy).createMediaSource(uri);
                DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItemBuilder.build())
            C.CONTENT_TYPE_SS -> //                    return new SsMediaSource.Factory(dataSourceFactory).setDrmSessionManager(drmSessionManager).setLoadErrorHandlingPolicy(errorHandlingPolicy).createMediaSource(uri);
                SsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItemBuilder.build())
            C.CONTENT_TYPE_HLS -> //                    return new HlsMediaSource.Factory(dataSourceFactory).setDrmSessionManager(drmSessionManager).setLoadErrorHandlingPolicy(errorHandlingPolicy).setAllowChunklessPreparation(true).createMediaSource(uri);
                HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true).createMediaSource(mediaItemBuilder.build())
            C.CONTENT_TYPE_OTHER -> //                    return new ProgressiveMediaSource.Factory(dataSourceFactory).setDrmSessionManager(drmSessionManager).setLoadErrorHandlingPolicy(errorHandlingPolicy).createMediaSource(uri);
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItemBuilder.build())
            else -> throw IllegalStateException("Unsupported type: $type")
        }
    }

    private fun onPlayerViewUpdated(view : View) {
        Log.e(TAG,"onPlayerViewUpdated")
    }

    private fun getPlayerView(context: Context) : View {
        Log.e(TAG,"getPlayerView")
        val view =  LayoutInflater.from(context).inflate(R.layout.player_activity,null)
        playerView = view as StyledPlayerView
        return view
    }
}

@Composable
fun PlayerControls(playbackState : Int,playWhenReady: Boolean,playerCurrentPosition: Long,totalDuration : Long, onPlayPauseAction :  ()-> Unit,onSeekForward: () -> Unit,onSeek : (Long) -> Unit,modifier: Modifier){

    Column(modifier = modifier.fillMaxHeight() , verticalArrangement = Arrangement.SpaceBetween , horizontalAlignment = Alignment.CenterHorizontally) {
        TopControls(modifier)
        CentreControls(
            playbackState = playbackState,
            playWhenReady = playWhenReady,
            onPlaybackAction = { onPlayPauseAction() },
            onSeekForward = { onSeekForward() },
            modifier = modifier
        )
        BottomControls(playerCurrentPosition,totalDuration,{seekTo -> onSeek(seekTo)},modifier)
    }

}

@Composable
fun TopControls(modifier: Modifier) {
    Text(text = "Top Controls" , color = Color.White)
}

@Composable
fun BottomControls(playerCurrentPosition : Long,totalDuration: Long, onSeek : (Long) -> Unit, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(text = "$playerCurrentPosition / $totalDuration", color = Color.White)
        val sliderValue  = if (totalDuration != 0L) {
            ((playerCurrentPosition).toFloat() / totalDuration.toFloat())
        } else {
            0
        }.toFloat()
        Slider(steps = 100, value = sliderValue, onValueChange =  {value -> onSeek((if(totalDuration==0L) 0L else (value*totalDuration)).toLong())})
    }
}

@Composable
fun CentreControls(playbackState: Int, playWhenReady: Boolean,onPlaybackAction : () -> Unit,onSeekForward : () -> Unit,modifier : Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically){

        if(playbackState == STATE_READY) {
            if(playWhenReady){
                Text(text = "Pause", modifier = Modifier.clickable {
                    onPlaybackAction()
                }, color = Color.White)
            } else{
                Text(text = "Play", modifier = Modifier.clickable {
                    onPlaybackAction()
                }, color = Color.White)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Text(text = "Seek 30 Seconds",modifier = Modifier.clickable { onSeekForward() }, color = Color.White)
        } else if(playbackState == STATE_BUFFERING) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun PlayerScreen(getPlayerView : (Context) -> View,
                onPlayerViewUpdated : (View) -> Unit,
                playerState: State<PlayerState>,
                 analyticsListener: AnalyticsListener,
                 getMediaSource : (String,String?,exoPlayer : ExoPlayer?,Context) -> MediaSource,
                 onPlayerClick : () -> Unit,
                modifier: Modifier) {
    val context = LocalContext.current

    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val exoPlayer : ExoPlayer? = rememberExoPlayer(context = context, analyticsListener = analyticsListener)

    Box(modifier = modifier){

        AndroidView(modifier = modifier.clickable {
            onPlayerClick()
        }, factory = { context ->
            val view = getPlayerView(context)
            (view as StyledPlayerView).player = exoPlayer
            view
        }, update = {
            onPlayerViewUpdated(it)
        })

        remember(playerState.value.videoUrl,playerState.value.adUrl) {
            playerState.value.videoUrl?.let {

                val mediaSource = getMediaSource(it,playerState.value.adUrl,exoPlayer,context)

                exoPlayer?.apply {
                    setMediaSource(mediaSource)
                    prepare()
                }
            }
        }

        if(playerState.value.isControllerVisible){
            PlayerControls(playerState.value.playbackState,playerState.value.playWhenReady,playerState.value.playerCurrentPosition,playerState.value.totalDuration, onPlayPauseAction = {exoPlayer?.playWhenReady = playerState.value.playWhenReady.not()} , onSeekForward = {exoPlayer?.seekForward()},
                {seekTo -> exoPlayer?.seekTo(seekTo)},modifier.matchParentSize()
            )
        }
    }





    DisposableEffect(lifecycle,exoPlayer) {

        val observer = LifecycleEventObserver { _, event ->

            when(event){
                Lifecycle.Event.ON_RESUME -> {
                    exoPlayer?.playWhenReady = true
                }

                Lifecycle.Event.ON_START -> {

                }

                Lifecycle.Event.ON_STOP -> {
                    exoPlayer?.playWhenReady = false
                }

                Lifecycle.Event.ON_DESTROY -> {
                    exoPlayer?.release()
                }
            }
        }
        lifecycle.addObserver(observer)

        onDispose {
            exoPlayer?.removeAnalyticsListener(analyticsListener)
            exoPlayer?.release()
            Log.e("ExoplayerCompose","on dispose exoPlayer.release()")
            lifecycle.removeObserver(observer)
        }

    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ExoplayerComposeTheme {
        Greeting("Android")
    }
}