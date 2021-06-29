package com.netDashboard.foreground_service.demons

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import kotlin.random.Random

class Mqttd(private val context: Context, private val URI: String) : Daemon() {

    var isEnabled = false
    var clientHandler = ClientHandler()
    var data: MutableLiveData<Pair<String?, MqttMessage?>> = MutableLiveData(Pair(null, null))

    init {
        start()
    }

    private fun start() {
        if (isEnabled) return

        isEnabled = true
        clientHandler.dispatch()
    }

    fun stop() {
        if (!isEnabled) return

        isEnabled = false
        clientHandler.dispatch()
    }

    fun publish(topic: String, msg: String, qos: Int = 1, retained: Boolean = false) {

        if (!clientHandler.client?.isConnected ?: false) return

        try {
            val message = MqttMessage()

            message.payload = msg.toByteArray()
            message.qos = qos
            message.isRetained = retained

            client?.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun subscribe(topic: String, qos: Int = 1) {

        if (!isConnected) return

        try {
            client?.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun unsubscribe(topic: String) {

        if (!isConnected) return

        try {
            client?.unsubscribe(topic, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    inner class ClientHandler(private val retryDelay: Long = 3000) {

        private var isDispatched = false
        lateinit var client: MqttAndroidClientExtended

        val isDone
            get() = client.isConnected == isEnabled
        var isDoneFlag = MutableLiveData(false)

        fun dispatch(force: Boolean = false) {
            if (client.isConnected == isEnabled) return

            if (isEnabled) {
                client.start()
            } else {
                client.stop()
            }

            if (isDispatched && !force) return
            if (isDone) {
                isDoneFlag.postValue(isDone)
                return
            }

            Handler(Looper.getMainLooper()).postDelayed({
                dispatch(true)
            }, retryDelay)
            isDispatched = true
        }

        inner class MqttAndroidClientExtended(
            context: Context,
            serverURI: String,
            clientId: String
        ) : MqttAndroidClient(context, serverURI, clientId) {

            var isBusy = false
            private var isReady = false
                get() = client != null && field

            fun start() {
                if (isBusy) return

                isBusy = true

                client = MqttAndroidClientExtended(context, URI, Random.nextInt().toString())
                client?.setCallback(object : MqttCallback {

                    override fun messageArrived(t: String?, m: MqttMessage) {
                        data.postValue(Pair(t ?: "", m))
                    }

                    override fun connectionLost(cause: Throwable?) {
                        clientHandler.dispatch()
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    }
                })

                val options = MqttConnectOptions()

                try {
                    client?.connect(options, null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            onConnectedFlag.postValue(true)
                        }

                        override fun onFailure(
                            asyncActionToken: IMqttToken?,
                            exception: Throwable?
                        ) {
                        }
                    })
                } catch (e: MqttException) {
                    e.printStackTrace()
                }

                isReady = true
                isBusy
            }

            fun stop() {
                if (!isReady || isBusy) return

                isBusy = true

                client?.unregisterResources()
                client?.close()

                try {
                    client?.disconnect(null, object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                        }

                        override fun onFailure(
                            asyncActionToken: IMqttToken?,
                            exception: Throwable?
                        ) {
                        }
                    })
                } catch (e: MqttException) {
                    e.printStackTrace()
                }

                client?.setCallback(null)
                client = null

                isReady = false
                isBusy = false
            }
        }
    }
}