package de.beatbrot.screenshotassistant.sheets

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Bundle
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.thebluealliance.spectrum.SpectrumDialog
import de.beatbrot.imagepainter.view.ImagePainterView
import de.beatbrot.screenshotassistant.R
import kotlinx.android.synthetic.main.sheet_colorsettings.*

class DrawSettingsSheet : Fragment(R.layout.sheet_colorsettings), IBottomSheet {

    override val isHideable = false

    var imagePainter: ImagePainterView? = null
        set(value) {
            field = value
            field?.setRedoStatusChangeListener {
                redoButton?.isEnabled = it
            }
            field?.setUndoStatusChangeListener {
                undoButton?.isEnabled = it
            }
        }

    var onHideListener: (() -> Unit)? = null

    private lateinit var colorPicker: SpectrumDialog.Builder

    private lateinit var viewModel: DrawSettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViewModel()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this)[DrawSettingsViewModel::class.java]

        viewModel.strokeColor.observe(this, Observer { newValue ->
            if (newValue == null) {
                return@Observer
            }
            initColorButton(colorButton, newValue)
            colorPicker.setSelectedColor(newValue)
            if (viewModel.editingMode.value == DrawMode.PEN) {
                imagePainter?.strokeColor = newValue
            } else {
                imagePainter?.strokeColor = ColorUtils.setAlphaComponent(newValue, 255 / 3)
            }
        })

        viewModel.strokeCap.observe(this, Observer { newValue ->
            imagePainter?.strokeCap = newValue
        })

        viewModel.strokeWidth.observe(this, Observer { newValue ->
            imagePainter?.strokeWidth = newValue
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        undoButton.isEnabled = imagePainter?.canUndo() ?: false
        redoButton.isEnabled = imagePainter?.canRedo() ?: false

        colorPicker = SpectrumDialog.Builder(context).apply {
            setTitle("Pick a color")
            setDismissOnColorSelected(true)
            setColors(intArrayOf(Color.BLACK, Color.RED, Color.GREEN, Color.YELLOW))
            setOnColorSelectedListener { positiveResult, color ->
                if (positiveResult) {
                    if (viewModel.editingMode.value == DrawMode.PEN) {
                        viewModel.penColor.postValue(color)
                    } else if (viewModel.editingMode.value == DrawMode.MARKER) {
                        viewModel.markerColor.postValue(color)
                    }
                }
            }
        }

        undoButton.setOnClickListener { imagePainter?.undo() }
        redoButton.setOnClickListener { imagePainter?.redo() }
        penSelector.setToggled(R.id.draw_mode, true)
        penSelector.onToggledListener = { _, toggle, selected ->
            if (selected) {
                viewModel.editingMode.value = when (toggle.id) {
                    R.id.draw_mode -> DrawMode.PEN
                    else -> DrawMode.MARKER
                }
            }
        }

        doneButton.setOnClickListener { onHideListener?.invoke() }
    }


    private fun initColorButton(view: View, @ColorInt color: Int) {
        view.foreground = ShapeDrawable(OvalShape()).apply {
            intrinsicHeight = 20
            intrinsicWidth = 20
            bounds = Rect(0, 0, 20, 20)
            paint.color = color
        }
        view.setOnClickListener { colorPicker.show() }
    }

    private fun SpectrumDialog.Builder.show() {
        build().show(childFragmentManager, "COLOR_PICKER")
    }
}
