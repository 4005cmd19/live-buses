package com.cmd.myapplication.data.test

import com.cmd.myapplication.data.repositories.RemoteDataSource
import com.hivemq.client.mqtt.datatypes.MqttQos

class TestClient: RemoteDataSource() {
    fun publish (topic: String, payload: ByteArray, retain: Boolean) {
        client.publishWith()
            .topic(topic)
            .qos(MqttQos.AT_MOST_ONCE)
            .retain(retain)
            .payload(payload)
            .send()
    }
}