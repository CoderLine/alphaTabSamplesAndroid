package net.alphatab.android.sample

import alphaTab.AlphaTabView
import alphaTab.core.ecmaScript.Uint8Array
import alphaTab.importer.ScoreLoader
import alphaTab.model.Score
import alphaTab.synth.PlayerState
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import java.io.ByteArrayOutputStream
import kotlin.contracts.ExperimentalContracts
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalContracts::class, ExperimentalUnsignedTypes::class)
@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {

    private lateinit var mAlphaTabView: AlphaTabView
    private lateinit var mTrackName: TextView
    private lateinit var mSongName: TextView

    private lateinit var mViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mAlphaTabView = findViewById(R.id.alphatab)
        mTrackName = findViewById(R.id.trackName)
        mSongName = findViewById(R.id.songName)
        findViewById<View>(R.id.info).setOnClickListener {
            val popup = ControlsPopupWindow(
                this, mViewModel,
            ) {
                mOpenFile.launch(arrayOf("*/*"))
            }
            popup.width = ViewGroup.LayoutParams.MATCH_PARENT
            popup.height = ViewGroup.LayoutParams.MATCH_PARENT
            popup.showAtLocation(mAlphaTabView, Gravity.CENTER, 0, 0)
        }

        mViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        mViewModel.settings.observe(this) {
            mAlphaTabView.settings = it
        }
        mViewModel.tracks.observe(this) {
            mAlphaTabView.tracks = it
            val first = it?.firstOrNull()
            if (first != null) {
                mTrackName.text = first.name
                mSongName.text = "${first.score.title} - ${first.score.artist}"
            }
        }
        mViewModel.countIn.observe(this) {
            mAlphaTabView.api.countInVolume = if (it) 1.0 else 0.0
        }
        mViewModel.metronome.observe(this) {
            mAlphaTabView.api.metronomeVolume = if (it) 1.0 else 0.0
        }
        mViewModel.looping.observe(this) {
            mAlphaTabView.api.isLooping = it
        }
        mViewModel.zoomLevel.observe(this) {
            mAlphaTabView.settings.display.scale = it / 100.0
            mAlphaTabView.api.updateSettings()
            mAlphaTabView.renderTracks()
        }
        mViewModel.layout.observe(this) {
            mAlphaTabView.settings.display.layoutMode = it
            mAlphaTabView.api.updateSettings()
            mAlphaTabView.renderTracks()
        }

        val playPause = findViewById<ImageButton>(R.id.playPause)
        playPause.setOnClickListener {
            mAlphaTabView.api.playPause()
        }

        mAlphaTabView.api.playerStateChanged.on {
            val image =
                if (it.state == PlayerState.Playing) R.drawable.baseline_pause_24 else R.drawable.baseline_play_arrow_24
            playPause.setImageResource(image)
        }
        mAlphaTabView.api.playerReady.on {
            playPause.visibility = View.VISIBLE
        }

        val timePosition = findViewById<TextView>(R.id.timePosition)
        var previousTime = -1
        mAlphaTabView.api.playerPositionChanged.on {
            // prevent too many UI updates
            val currentSeconds = (it.currentTime / 1000).toInt()
            if (currentSeconds == previousTime) {
                return@on
            }

            previousTime = currentSeconds

            val currentTimePosition = it.currentTime.toDuration(DurationUnit.MILLISECONDS)
            val totalTimePosition = it.endTime.toDuration(DurationUnit.MILLISECONDS)

            val currentTimePositionText = currentTimePosition.toComponents { hours, minutes, seconds, _ -> "%02d:%02d".format(hours * 60 + minutes, seconds) }
            val totalTimePositionText = totalTimePosition.toComponents { hours, minutes, seconds, _ -> "%02d:%02d".format(hours * 60 + minutes, seconds) }

            timePosition.text = "$currentTimePositionText / $totalTimePositionText"
        }
    }

    private val mOpenFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        val uri = it ?: return@registerForActivityResult
        val score: Score
        try {
            val fileData = readFileData(uri)
            score = ScoreLoader.loadScoreFromBytes(fileData, mAlphaTabView.settings)
            Log.i("AlphaTab", "File loaded: ${score.title}")
        } catch (e: Exception) {
            Log.e("AlphaTab", "Failed to load file: $e, ${e.stackTraceToString()}")
            Toast.makeText(this, "Failed to load file: ${e.message}", Toast.LENGTH_LONG).show()
            return@registerForActivityResult
        }

        try {
            mViewModel.score.value = score
            mViewModel.tracks.value = arrayListOf(score.tracks[0])
        } catch (e: Exception) {
            Log.e("AlphaTab", "Failed to render file: $e, ${e.stackTraceToString()}")
            Toast.makeText(this, "Failed to render file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun readFileData(uri: Uri): Uint8Array {
        val inputStream = contentResolver.openInputStream(uri)
        inputStream.use {
            ByteArrayOutputStream().use {
                inputStream!!.copyTo(it)
                return Uint8Array(it.toByteArray().asUByteArray())
            }
        }
    }
}