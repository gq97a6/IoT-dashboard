package com.netDashboard.tile.types.slider

import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.material.slider.Slider
import com.netDashboard.*
import com.netDashboard.tile.Tile
import com.netDashboard.tile.TilesAdapter
import org.eclipse.paho.client.mqttv3.MqttMessage

class SliderTile : Tile() {

    @Transient
    override val layout = R.layout.tile_slider

    @Transient
    override val mqttDefaultPubValue = "@value"
    override var mqttPubValue = mqttDefaultPubValue

    init {
        name = "slider"
    }

    var value = 0f
    private var from = 0f
    private var to = 100f
    private var step = 10f

    private var liveValue: Float
        get() {
            val slider = holder?.itemView?.findViewById<Slider>(R.id.ts_slider)

            return when {
                from < to -> slider?.value ?: 0f
                to < from -> slider?.valueFrom ?: 0f - (slider?.value ?: 0f)
                else -> 0f
            }
        }
        set(value) {
            val m = value % step
            val v = if (m <= step / 2) {
                value - m
            } else {
                value - m + step
            }

            this.value = v
            val slider = holder?.itemView?.findViewById<Slider>(R.id.ts_slider)

            when {
                from < to -> slider?.value = v
                to < from -> slider?.value = from - v
            }

            holder?.itemView?.findViewById<TextView>(R.id.ts_value)?.text = v.dezero()
        }

    override fun onBindViewHolder(holder: TilesAdapter.TileViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        val slider = holder.itemView.findViewById<Slider>(R.id.ts_slider)
        val background = holder.itemView.findViewById<View>(R.id.background)

        slider.isEnabled = !isEdit
        setRange(from, to, step)

        background.setOnTouchListener { v, e ->

            when (e.action) {
                MotionEvent.ACTION_DOWN -> v.performClick()
                MotionEvent.ACTION_UP -> holder.itemView.callOnClick()
            }

            if ((e.eventTime - e.downTime) > 0) {

                val params = slider.layoutParams as FrameLayout.LayoutParams
                params.width = getScreenWidth() - 100.toPx()
                slider.layoutParams = params

                val center = getScreenWidth() / 2
                val location = IntArray(2)
                background.getLocationOnScreen(location)
                val offset = center - location[0] - slider.width / 2

                e.setLocation(e.x - offset, e.y)

                slider.dispatchTouchEvent(e)
            }

            return@setOnTouchListener true
        }

        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(s: Slider) {
                holder.itemView.callOnClick()
            }

            override fun onStopTrackingTouch(s: Slider) {
                val topic = mqttTopics.pubs.get("base")
                onSend(topic.topic, mqttPubValue.replace("@value", liveValue.dezero()), topic.qos)
            }
        })

        slider.addOnChangeListener(Slider.OnChangeListener { _: Slider, value: Float, _: Boolean ->
            liveValue = value
        })

        setThemeColor(color)
    }

    override fun setThemeColor(color: Int) {
        super.setThemeColor(color)

        holder?.itemView?.findViewById<View>(R.id.background)?.setBackgroundColor(color)

        holder?.itemView?.findViewById<TextView>(R.id.ts_value)
            ?.setTextColor(getContrastColor(color).alpha(.75f))
    }

    private fun setRange(from: Float, to: Float, step: Float = 1f) {
        if (from == to || step !in 0.0000000001..1000000000.0) return
        val slider = holder?.itemView?.findViewById<Slider>(R.id.ts_slider) ?: return

        this.from = from
        this.to = to
        this.step = step

        if (from < to) {
            slider.valueFrom = from
            slider.valueTo = to
            slider.stepSize = step
        } else if (to < from) {
            slider.valueFrom = to
            slider.valueTo = from
            slider.stepSize = step
        }

        liveValue = if (value in from..to) value else slider.valueFrom
    }

    override fun onData(data: Pair<String?, MqttMessage?>): Boolean {
        if (!super.onData(data)) return false

        val value = data.second.toString().toFloatOrNull()
        if (value != null) liveValue = value

        return true
    }
}