package com.cmd.myapplication.data.utils

object SearchUtils {
    fun isMatch(query: String, target: String): Boolean {
        return target.contains(query) || query.contains(target)
    }
}