package com.cmd.myapplication.data.repositories

abstract class Repository<T> {

    abstract fun request(ids: Array<String>, callback: (id: String, T) -> Unit)

    abstract fun requestAll(callback: (id: String, T) -> Unit)

    abstract fun requestOnce (ids: Array<String>, callback: (id: String, T) -> Unit)

    abstract fun ignore(ids: Array<String>)

    abstract fun ignoreAll()
}
