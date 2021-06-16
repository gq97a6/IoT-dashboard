package com.netDashboard.tile.types.button

import android.content.res.ColorStateList
import android.widget.Button
import com.netDashboard.*
import com.netDashboard.tile.Tile
import com.netDashboard.tile.TilesAdapter
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.util.*

class ButtonTile(name: String, color: Int, width: Int, height: Int) :
    Tile(name, color, R.layout.tile_button, width, height) {

    var text = "Default value"
    private var counter = 0

    override fun onBindViewHolder(holder: TilesAdapter.TileViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        holder.itemView.findViewById<Button>(R.id.tb_button).setOnClickListener {
            holder.itemView.callOnClick()

            if (!isEdit) {
                text = counter.toString()

                holder.itemView.findViewById<Button>(R.id.tb_button).text = text

                //mqttd?.publish("123", "test_click")

                //createNotification(context!!, Random().nextInt().toString())
                createVibration(context!!, 50)

                setThemeColor(getRandomColor())
            }
        }

        holder.itemView.findViewById<Button>(R.id.tb_button).setOnLongClickListener {
            counter = 0

            holder.itemView.findViewById<Button>(R.id.tb_button).callOnClick()

            return@setOnLongClickListener true
        }

        holder.itemView.findViewById<Button>(R.id.tb_button).text = text

        setThemeColor(color)
    }

    override fun setThemeColor(color: Int) {
        super.setThemeColor(color)

        val button = holder?.itemView?.findViewById<Button>(R.id.tb_button)

        button?.backgroundTintList =
            ColorStateList.valueOf(
                color
            )

        button?.setTextColor(getContrastColor(color).alpha(75))
    }

    override fun onData(data: String, isLive: Boolean) {
        super.onData(data, isLive)
        counter += data.toIntOrNull() ?: 1
    }

    override fun onData(topic: String, message: MqttMessage, isLive: Boolean) {
        super.onData(topic, message, isLive)

        counter += message.toString().toIntOrNull() ?: 1
        text = counter.toString()

        holder?.itemView?.findViewById<Button>(R.id.tb_button)?.text = text
    }

    override fun onLock(isLocked: Boolean) {
        super.onLock(isLocked)

        holder?.itemView?.findViewById<Button>(R.id.tb_button)?.isEnabled = !isLocked
    }
}