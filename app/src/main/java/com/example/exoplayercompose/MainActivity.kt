package com.example.exoplayercompose

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.exoplayercompose.ui.theme.ExoplayerComposeTheme
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView

class MainActivity : ComponentActivity() {
    val TAG = "ExoplayerCompose"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExoplayerComposeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    PlayerScreen(this::getPlayerView,this::onPlayerViewUpdated,"https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4")
                }
            }
        }
    }

    private fun onPlayerViewUpdated(view : View) {
        Log.e(TAG,"onPlayerViewUpdated")
    }

    private fun getPlayerView(context: Context,exoPlayer: ExoPlayer?) : View {
        Log.e(TAG,"getPlayerView")
        val view =  LayoutInflater.from(context).inflate(R.layout.player_activity,null)
        (view as StyledPlayerView).player = exoPlayer
        return view
    }
}

@Composable
fun PlayerScreen(getPlayerView : (Context,ExoPlayer?) -> View,
                onPlayerViewUpdated : (View) -> Unit,
                videoUrl : String) {
    val context = LocalContext.current

    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val exoPlayer : ExoPlayer? = remember {
        ExoPlayer.Builder(context).build()
    }

    remember(key1 = videoUrl) {
        exoPlayer?.apply {
            setMediaItem(
                MediaItem.fromUri(
                    videoUrl
                )
            )
            prepare()
        }
    }
    AndroidView(factory = { context ->
        getPlayerView(context,exoPlayer)
    }, update = {
        onPlayerViewUpdated(it)
    })



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
            Log.e("ExoplayerCompose","exoPlayer.release()")
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