package net.alphatab.android.sample

import alphaTab.LayoutMode
import alphaTab.Settings
import alphaTab.model.Score
import alphaTab.model.Track
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.contracts.ExperimentalContracts

@ExperimentalUnsignedTypes
@ExperimentalContracts
class MainViewModel  : ViewModel() {
    val score = MutableLiveData<Score?>()
    val tracks = MutableLiveData<List<Track>?>()
    val countIn = MutableLiveData(false)
    val metronome = MutableLiveData(false)
    val looping = MutableLiveData(false)
    val zoomLevel = MutableLiveData(100)
    val layout = MutableLiveData(LayoutMode.Page)
    val settings = MutableLiveData<Settings>().apply {
        value = Settings().apply {
            this.player.enableCursor = true
            this.player.enablePlayer = true
            this.player.enableUserInteraction = true
            this.display.barCountPerPartial = 4.0
            this.display.resources.barNumberFont
        }
    }
}
