package com.cmd.myapplication.data.repositories

import android.util.Log
import com.cmd.myapplication.data.Meta
import com.cmd.myapplication.data.adapters.MqttClientAdapter
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Deprecated("Use BusDataRepository")
abstract class Repository<T>() {

    abstract fun request(ids: Array<String>, callback: RequestCallback<T>)

    abstract fun requestAll(callback: RequestCallback<T>)

    abstract fun requestOnce(ids: Array<String>, callback: RequestCallback<T>)

    suspend fun requestOnce(ids: Array<String>): List<T> = with(CompletableFuture<List<T>>()) {
        val unresolvedIds = mutableSetOf(*ids)
        val received = mutableListOf<T>()

        requestOnce(ids) { id, data ->
            unresolvedIds.remove(id)
            received.add(data)

            if (unresolvedIds.isEmpty()) {
                complete(received)
            }
        }

        await()
    }

    abstract fun ignore(ids: Array<String>)

    abstract fun ignoreAll()

    fun interface RequestCallback<T> {
        fun onReceived(id: String, data: T);
    }
}

/**
 * Abstract class that provides an abstraction layer to access the MqttClientAdapter asynchronously
 * or in a blocking manner.
 * @constructor Initialises the repository with an instance of [MqttClientAdapter],
 * the [Class] to which the data provided by the MQTT topic this repository accesses should be converted to
 * and the [Class] to which the metadata associated with this topic should be converted to
 */
abstract class BusDataRepository<T, U : Meta>(
    private val remoteDataSource: MqttClientAdapter,
    private val dataClass: Class<T>,
    private val metaClass: Class<U>,
) {
    /**
     * Topic that provides the data for this repository
     */
    protected abstract val dataTopicTemplate: String

    /**
     * Topic that provides the metadata associated with the data provided by this repository
     */
    protected abstract val metaTopicTemplate: String

    // used to timeout synchronous requests
    private val timeoutHandler = Executors.newSingleThreadScheduledExecutor()

    /**
     * Request bus data asynchronously.
     * @param ids IDs of bus stops or bus lines to request.
     * @param callback Callback to receive results.
     */
    fun requestAsync(ids: Array<String>, callback: RequestCallback<T>): AsyncRequest {
        for (id in ids.toSet()) {
            val topic = TopicTemplate.fill(dataTopicTemplate, id)

            remoteDataSource.listenTo(topic) { topic, payload ->
                val dataId = TopicTemplate.unfill(dataTopicTemplate, topic)

                val data = Gson().fromJson(
                    String(payload),
                    dataClass
                )

                callback.onReceived(dataId, data)
            }
        }

        return AsyncRequest(this, ids)
    }

    /**
     * Request data synchronously. Once the data for a requested ID is received the MQTT client
     * unsubscribes from the corresponding topic.
     * @param ids IDs of bus stops or bus lines to request.
     * @param timeout How long, in milliseconds, to wait for data.
     * @see requestAsync
     */
    suspend fun request(ids: Array<String>, timeout: Long = 0) = withContext(Dispatchers.IO) {
        return@withContext with(CompletableFuture<List<T>>()) {
            val receivedIds = mutableSetOf<String>()
            val receivedData = mutableSetOf<T>()

            requestAsync(ids) { id, data ->
                if (!receivedIds.contains(id)) {
                    receivedIds.add(id)
                    receivedData.add(data)

                    ignore(arrayOf(id))
                }

                if (receivedIds.size == ids.toSet().size) {
                    complete(receivedData.toList())
                }
            }

            if (timeout > 0) {
                timeoutHandler.schedule({ complete(emptyList()) }, timeout, TimeUnit.MILLISECONDS)
            }

            await()
        }
    }

    /**
     * Requests all data.
     * @param callback Callback to receive results.
     * @see requestAsync
     */
    fun requestAllAsync(callback: RequestCallback<T>) = requestAsync(arrayOf("+")) { id, data ->
        callback.onReceived(id, data)
    }


    /**
     * Request all data synchronously. Particularly useful when populating [BusDataViewModel] to
     * guarantee that all available data has been received.
     * @param timeout How long, in milliseconds, to wait for data.
     * @see requestAllAsync
     */
    suspend fun requestAll(timeout: Long = 0) = withContext(Dispatchers.IO) {
        return@withContext with(CompletableFuture<List<T>>()) {
            val metaTopic = TopicTemplate.fill(metaTopicTemplate)

            val receivedIds = mutableSetOf<String>()
            val receivedData = mutableSetOf<T>()

            Log.e(TAG, "requesting meta")

            remoteDataSource.listenTo(metaTopic) { topic, payload ->
                remoteDataSource.stopListeningTo(topic)

                val meta = Gson().fromJson(String(payload), metaClass)
                val size = meta.size

                Log.e(TAG, "received meta - size=$size")

                if (size == 0) {
                    // no data
                    complete(emptyList())
                }

                requestAllAsync { id, data ->
                    if (!receivedIds.contains(id)) {
                        receivedIds.add(id)
                        receivedData.add(data)
                    }

                    if (receivedIds.size == size) {
                        ignoreAll()
                        complete(receivedData.toList())
                    }
                }

                if (timeout > 0) {
                    timeoutHandler.schedule(
                        { complete(emptyList()) },
                        timeout,
                        TimeUnit.MILLISECONDS
                    )
                }
            }

            await()
        }
    }

    fun ignore(ids: Array<String>) = ids.forEach {
        val topic = TopicTemplate.fill(dataTopicTemplate, it)

        remoteDataSource.stopListeningTo(topic)
    }

    fun ignoreAll() = ignore(arrayOf("+"))

    fun interface RequestCallback<T> {
        fun onReceived(id: String, data: T);
    }

    class TopicTemplate {
        companion object {
            fun fill(
                template: String,
                id: String? = null,
            ) = if (id != null) String.format(template, id) else template


            fun unfill(template: String, filled: String): String {
                val i = template.indexOf("%s")
                val j = i + 2

                val start = template.substring(0, i)
                val end = template.substring(j)

                return filled.removeSurrounding(start, end)
            }
        }
    }

    /**
     * Class used to manage asynchronous requests.
     * @param owner [BusDataRepository] that made the request
     * @param ids Requested IDs
     */
    class AsyncRequest(
        private val owner: BusDataRepository<*, *>,
        private val ids: Array<String>,
    ) {
        fun close() {
            owner.ignore(ids)
        }
    }

    companion object {
        const val TAG = "BusDataRepository"
    }
}