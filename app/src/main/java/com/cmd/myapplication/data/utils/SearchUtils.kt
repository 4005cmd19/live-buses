package com.cmd.myapplication.data.utils

object SearchUtils {
    fun isMatch(query: String, target: String): Boolean {
        val q = query.lowercase()
        val t = target.lowercase()

        return t.contains(q) || q.contains(t)
    }
}