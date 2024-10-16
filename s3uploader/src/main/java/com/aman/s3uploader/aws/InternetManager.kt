package com.aman.s3uploader.aws

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi

class InternetManager(context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val isInternetConnected: Boolean
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // For API level 23 and above
                checkInternetConnectionNewMethod()
            } else {
                // For API level below 23
                checkInternetConnectionOldMethod()
            }
        }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkInternetConnectionNewMethod(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> true
                else -> false
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            false
        }
    }

    private fun checkInternetConnectionOldMethod(): Boolean {
        return try {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            activeNetworkInfo != null && activeNetworkInfo.isConnected
        } catch (ex: Exception) {
            ex.printStackTrace()
            false
        }
    }
}