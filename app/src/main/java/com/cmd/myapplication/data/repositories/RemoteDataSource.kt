package com.cmd.myapplication.data.repositories

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient
import java.util.UUID

open class RemoteDataSource {
    protected val client: Mqtt3BlockingClient =
        MqttClient.builder().useMqttVersion3().identifier(UUID.randomUUID().toString())
            .serverHost(HOST).serverPort(PORT).useSslWithDefaultConfig().buildBlocking()

    init {
        client.connectWith().simpleAuth().username(APP_ID).password(APP_KEY).applySimpleAuth()
            .send()
    }

    fun listenTo(topic: String, callback: (topic: String, payload: ByteArray) -> Unit) {
        client.toAsync().subscribeWith().topicFilter(topic).qos(MqttQos.AT_MOST_ONCE).callback {
            callback(it.topic.toString(), it.payloadAsBytes)
        }.send()
//        CompletableFuture.supplyAsync { "" }.await()
    }

    fun stopListeningTo(topic: String) {
        client.unsubscribeWith().topicFilter(topic).send()
    }

    fun update(
        topic: String,
        payload: ByteArray,
        retain: Boolean = true,
        qos: MqttQos = MqttQos.AT_MOST_ONCE,
    ) {
        client.publishWith().topic(topic).payload(payload).retain(retain).qos(qos).send()
    }

    fun clearRequest(topic: String) {
        update(topic, ByteArray(0))
    }

    class RetainedObject(
        var topic: String,
        var message: String,
    )

    companion object {
        const val HOST = "61af2c55ce064492b81b2877be486551.s2.eu.hivemq.cloud"
        const val PORT = 8883
        const val APP_ID = "mqtt-4005-main"
        val APP_KEY = "MQTT-4005-connect".toByteArray()
    }
}