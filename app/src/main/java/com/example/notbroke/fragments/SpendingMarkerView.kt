package com.example.notbroke.fragments

import android.content.Context
import android.widget.TextView
import com.example.notbroke.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.utils.MPPointF

class SpendingMarkerView(context: Context) : MarkerView(context, R.layout.marker_view) {

    private val markerText: TextView = findViewById(R.id.markerText)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            markerText.text = "R ${e.y.toInt()}"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}