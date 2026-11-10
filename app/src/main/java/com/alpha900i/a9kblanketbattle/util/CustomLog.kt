package com.alpha900i.a9kblanketbattle.util

import android.util.Log


/**
 * Log wrapper. Adds "app tag" to log messages, with additiondal tag when required
 */
class CustomLog {
    companion object {
        private const val TAG: String = "A9KBlanketBattle_LOG"
        fun d(message: String) {
            d("", message)
        }

        fun d(tag: String, message: String) {
            Log.d("$TAG/$tag", message)
        }

        fun e(message: String) {
            e("", message)
        }

        fun e(tag: String, message: String) {
            Log.e("$TAG/$tag", message)
        }
    }
}