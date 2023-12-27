package com.creator.common.utils

import com.creator.common.Constants
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okio.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import kotlin.experimental.and

object IPUtil {

    private const val TAG = "IPUtil"
    fun getIpv4Address(block: (ip: String) -> Unit) {
        Constants.IP.REQUEST_URL.forEach { url ->
            OkHttpClientUtil.asyncGet(url, object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                }

                override fun onResponse(call: Call, response: Response) {
                    var string = response.body.string()

                    LogUtil.d(TAG, string)
                    /*if (!isPublicIP(string)) {
                        string = ""
                    }*/
                    block(string)
                }

            })
        }

    }

    fun isPublicIP(ipAddress: String): Boolean {
        return isPublicIP(InetAddress.getByName(ipAddress))
    }

    fun isPublicIP(ipAddress: InetAddress): Boolean {
        when (ipAddress) {
            is Inet4Address -> {
                return isPublicIPv4(ipAddress)
            }

            is Inet6Address -> {
                return isPublicIPv6(ipAddress)
            }
        }
        return false;
    }

    fun isPublicIPv4(ipAddress: Inet4Address): Boolean {
        return try {
            val addressBytes = ipAddress.address

            // Check if it's a private IPv4 address range
            !(addressBytes[0] == 10.toByte() || (addressBytes[0] == 172.toByte() && (addressBytes[1] in 16..31))
                    || (addressBytes[0] == 192.toByte() && addressBytes[1] == 168.toByte()))
        } catch (e: Exception) {
            LogUtil.e(TAG, e.message.toString(), e)
            false
        }
    }

    fun isPublicIPv6(ipAddress: Inet6Address): Boolean {
        return try {
            val addressBytes = ipAddress.address
            !(addressBytes[0] == 0xfc.toByte() && (addressBytes[1] and 0xfe.toByte() == 0x80.toByte()))
        } catch (e: Exception) {
            LogUtil.e(TAG, e.message.toString(), e)
            false
        }
    }

}