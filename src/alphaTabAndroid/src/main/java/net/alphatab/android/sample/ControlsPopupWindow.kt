package net.alphatab.android.sample

import alphaTab.LayoutMode
import alphaTab.model.Track
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class, ExperimentalUnsignedTypes::class)
@SuppressLint("InflateParams")
class ControlsPopupWindow(
    private val context: Context,
    private val mainViewModel: MainViewModel,
    onOpenFile: () -> Unit
) : PopupWindow(context) {
    private val mOpenButton: MaterialButton
    private val mTrackList: ListView

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.popup_controls, null)

        mOpenButton = view.findViewById(R.id.openFile)
        mOpenButton.setOnClickListener {
            onOpenFile()
            dismiss()
        }

        mTrackList = view.findViewById(R.id.trackList)
        mTrackList.adapter =
            TrackListAdapter(context, mainViewModel.score.value?.tracks?.toList() ?: emptyList())
        mTrackList.setOnItemClickListener { _, _, position, _ ->
            mainViewModel.tracks.value =
                mutableListOf((mTrackList.adapter as TrackListAdapter).getItem(position)!!)
            dismiss()
        }

        view.findViewById<MaterialButton>(R.id.back).setOnClickListener {
            dismiss()
        }

        initToggle(view.findViewById(R.id.countIn), mainViewModel.countIn.value ?: false) {
            mainViewModel.countIn.value = it
        }

        initToggle(view.findViewById(R.id.metronome), mainViewModel.metronome.value ?: false) {
            mainViewModel.metronome.value = it
        }

        initToggle(view.findViewById(R.id.looping), mainViewModel.looping.value ?: false) {
            mainViewModel.looping.value = it
        }

        val zoom = view.findViewById<MaterialButton>(R.id.zoom)
        @SuppressLint("SetTextI18n")
        zoom.text = "${mainViewModel.zoomLevel.value}%"
        zoom.setOnClickListener {
            PopupMenu(context, zoom).apply {
                setOnMenuItemClickListener {
                    val zoomLevel = it.title!!.trim('%').toString().toInt()
                    mainViewModel.zoomLevel.value = zoomLevel
                    this@ControlsPopupWindow.dismiss()
                    true
                }
                inflate(R.menu.zoom)
                show()

            }
        }

        val layout = view.findViewById<MaterialButton>(R.id.layout)
        @SuppressLint("SetTextI18n")
        layout.text = mainViewModel.layout.value!!.name
        layout.setOnClickListener {
            PopupMenu(context, layout).apply {
                setOnMenuItemClickListener {
                    mainViewModel.layout.value = when(it.title) {
                        "Page" -> LayoutMode.Page
                        "Horizontal" -> LayoutMode.Horizontal
                        else -> throw IllegalStateException("Unknown Layout")
                    }
                    this@ControlsPopupWindow.dismiss()
                    true
                }
                inflate(R.menu.layout)
                show()
            }
        }

        contentView = view
    }

    private fun initToggle(
        button: MaterialButton,
        initialState: Boolean,
        onChange: (newValue: Boolean) -> Unit
    ) {
        updateToggleColors(button, initialState)
        button.addOnCheckedChangeListener { _, isChecked ->
            updateToggleColors(button, isChecked)
            onChange(isChecked)
        }
    }

    private fun updateToggleColors(buttonView: MaterialButton, isChecked: Boolean) {
        val textColor = if (isChecked) R.color.checkedTextColor else R.color.uncheckedTextColor
        val iconColor = if (isChecked) R.color.checkedIconColor else R.color.uncheckedIconColor

        val textColorList = ColorStateList.valueOf(ContextCompat.getColor(context, textColor))
        val iconColorList = ColorStateList.valueOf(ContextCompat.getColor(context, iconColor))

        buttonView.iconTint = iconColorList
        buttonView.setTextColor(textColorList)
    }


    class TrackListAdapter(context: Context, tracks: List<Track>) :
        ArrayAdapter<Track>(context, android.R.layout.simple_list_item_1, tracks) {

        private val mInflater: LayoutInflater = LayoutInflater.from(context)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view =
                convertView ?: mInflater.inflate(android.R.layout.simple_list_item_1, parent, false)

            if (view !is TextView) {
                throw IllegalStateException("Expected simple_list_item_1 to be a TextView")
            }

            val item = getItem(position)
            view.text = item!!.name
            return view
        }
    }
}