package com.netDashboard.tile

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.netDashboard.*
import com.netDashboard.foreground_service.ForegroundService
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.io.*
import java.util.*

abstract class Tile(
    val type: String,
    var name: String,
    var color: Int,
    private var layout: Int,
    var width: Int,
    var height: Int
) : Serializable {

    val id: Long?

    var isColouredByTheme = false

    var isEdit = false
        set(value) {
            field = value; onEdit(value)
        }

    var flag = ""
        private set

    var service: ForegroundService? = null
    var context: Context? = null
    var holder: TilesAdapter.TileViewHolder? = null

    private var spanCount = 1

    //MQTT
    var mqttEnabled = false
    var mqttSubTopics: MutableList<String> = mutableListOf("abc") //TMP ("abc")
    var mqttPubTopics: MutableList<String> = mutableListOf()
    var mqttPubConfirmation = false
    var mqttQoS = 0
    var mqttPayloadJSON = false
    var mqttOutputJSON = false

    //Bluetooth
    var bltPattern = ""
    var bltDelimiter = ""
    var bltRequestToGet = ""
    var bltPayloadJSON = false
    var bltOutputJSON = ""

    init {
        id = Random().nextLong()
    }

    fun getItemViewType(context: Context, spanCount: Int): Int {
        this.context = context
        this.spanCount = spanCount

        return layout
    }

    open fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TilesAdapter.TileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)

        return TilesAdapter.TileViewHolder(view)
    }

    open fun onBindViewHolder(holder: TilesAdapter.TileViewHolder, position: Int) {
        this.holder = holder

        val view = holder.itemView
        val params = view.layoutParams

        params.height = ((getScreenWidth() - view.paddingLeft * 2) / spanCount) * height
        view.layoutParams = params

        onEdit(isEdit)
        flag(flag)
    }

    fun areItemsTheSame(oldItem: Tile, newItem: Tile): Boolean {
        return oldItem == newItem
    }

    fun areContentsTheSame(oldItem: Tile, newItem: Tile): Boolean {
        return oldItem.id == newItem.id
    }

    fun toggleFlag(flag: String) {
        if(this.flag.isNotEmpty()) {
            flag("")
        } else {
            flag(flag)
        }
    }

    fun flag(flag: String = "") {
        this.flag = flag

        val flagMark = holder?.itemView?.findViewById<View>(R.id.flag_mark)
        val flagBackground = holder?.itemView?.findViewById<View>(R.id.flag_background)

        when (flag) {
            "swap" -> flagMark?.setBackgroundResource(R.drawable.icon_swap_flag)
            "remove" -> flagMark?.setBackgroundResource(R.drawable.icon_remove_flag)
            "lock" -> flagMark?.setBackgroundResource(R.drawable.icon_lock_flag)
        }

        if (flag.isNotEmpty()) {
            flagMark?.backgroundTintList = ColorStateList.valueOf(getContrastColor(color))
            flagBackground?.backgroundTintList =
                ColorStateList.valueOf(getContrastColor(color, true).alpha(60))

            flagMark?.visibility = View.VISIBLE
            flagBackground?.visibility = View.VISIBLE
        } else {
            flagMark?.visibility = View.GONE
            flagBackground?.visibility = View.GONE
        }
    }

    open fun setThemeColor(color: Int) {
        this.color = color
    }

    open fun onClick() {}

    open fun onLongClick() {}

    open fun onEdit(isEdit: Boolean) {}

    //open fun onData() {}
}