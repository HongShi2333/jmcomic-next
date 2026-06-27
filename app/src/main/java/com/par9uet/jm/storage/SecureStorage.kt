package com.par9uet.jm.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class SecureStorage(
    context: Context,
    private val gson: Gson = GsonBuilder().create()
) {
    private val cryptoManager = CryptoManager()
    val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("jm-mobile-g-data", Context.MODE_PRIVATE)

    fun <T> set(key: String, t: T) {
        val json = gson.toJson(t)
        sharedPreferences.edit {
            putString(key, cryptoManager.encrypt(json))
        }
    }

    fun <T> get(key: String, type: java.lang.reflect.Type): T? {
        return try {
            getString(key)?.let {
                gson.fromJson(it, type)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getString(key: String): String? {
        val json = sharedPreferences.getString(key, null)
        return try {
            json?.let {
                cryptoManager.decrypt(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun remove(key: String) {
        sharedPreferences.edit {
            remove(key)
        }
    }
}
