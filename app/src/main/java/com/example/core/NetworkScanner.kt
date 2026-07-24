package com.example.core

import android.content.Context
import android.net.wifi.WifiManager
import com.example.model.TvBrand
import com.example.model.TvDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class NetworkScanner(private val context: Context) {

    private val _discoveredTvs = MutableStateFlow<List<TvDevice>>(emptyList())
    val discoveredTvs: Flow<List<TvDevice>> = _discoveredTvs.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: Flow<Boolean> = _isScanning.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(1200, TimeUnit.MILLISECONDS)
        .readTimeout(1200, TimeUnit.MILLISECONDS)
        .build()

    suspend fun startScan(): List<TvDevice> = withContext(Dispatchers.IO) {
        if (_isScanning.value) return@withContext _discoveredTvs.value
        _isScanning.value = true

        val tvMap = ConcurrentHashMap<String, TvDevice>()

        // 1. Perform SSDP UDP Discovery
        try {
            val ssdpResults = performSsdpSearch()
            ssdpResults.forEach { tvMap[it.ipAddress] = it }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Perform Subnet IP Port Scanner for guaranteed detection
        val localIp = getLocalIpAddress()
        if (localIp.isNotEmpty() && localIp != "127.0.0.1") {
            val subnetPrefix = localIp.substringBeforeLast(".")
            scanSubnetPorts(subnetPrefix, tvMap)
        }

        val finalList = tvMap.values.toList()
        _discoveredTvs.value = finalList
        _isScanning.value = false
        finalList
    }

    private suspend fun performSsdpSearch(): List<TvDevice> = withContext(Dispatchers.IO) {
        val foundTvs = mutableListOf<TvDevice>()
        val multicastLock: WifiManager.MulticastLock? = try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.createMulticastLock("SmartRemoteMulticastLock").apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            null
        }

        try {
            val socket = DatagramSocket()
            socket.soTimeout = 2000

            val ssdpQuery = "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 2\r\n" +
                    "ST: ssdp:all\r\n\r\n"

            val sendData = ssdpQuery.toByteArray()
            val group = InetAddress.getByName("239.255.255.250")
            val sendPacket = DatagramPacket(sendData, sendData.size, group, 1900)

            socket.send(sendPacket)

            val recvBuf = ByteArray(2048)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < 2200) {
                try {
                    val recvPacket = DatagramPacket(recvBuf, recvBuf.size)
                    socket.receive(recvPacket)
                    val response = String(recvPacket.data, 0, recvPacket.length)
                    val senderIp = recvPacket.address.hostAddress ?: continue

                    val device = parseSsdpResponse(senderIp, response)
                    if (device != null && foundTvs.none { it.ipAddress == device.ipAddress }) {
                        foundTvs.add(device)
                    }
                } catch (e: Exception) {
                    // Socket timeout when no more packets arrive
                    break
                }
            }
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            multicastLock?.let {
                if (it.isHeld) it.release()
            }
        }
        foundTvs
    }

    private fun parseSsdpResponse(ipAddress: String, response: String): TvDevice? {
        val upper = response.uppercase()
        val brand = when {
            upper.contains("ROKU") -> TvBrand.ROKU
            upper.contains("SAMSUNG") || upper.contains("TIZEN") -> TvBrand.SAMSUNG
            upper.contains("LG") || upper.contains("WEBOS") -> TvBrand.LG
            upper.contains("GOOGLECAST") || upper.contains("CHROMECAST") || upper.contains("ANDROID") -> TvBrand.ANDROID_TV
            else -> TvBrand.GENERIC
        }

        val name = when (brand) {
            TvBrand.ROKU -> "Roku Smart TV ($ipAddress)"
            TvBrand.SAMSUNG -> "Samsung Tizen TV ($ipAddress)"
            TvBrand.LG -> "LG webOS TV ($ipAddress)"
            TvBrand.ANDROID_TV -> "Android TV / Google TV ($ipAddress)"
            TvBrand.GENERIC -> "Smart TV ($ipAddress)"
        }

        return TvDevice(
            id = "ssdp_$ipAddress",
            name = name,
            ipAddress = ipAddress,
            brand = brand,
            port = brand.defaultPort,
            isOnline = true
        )
    }

    private suspend fun scanSubnetPorts(
        subnetPrefix: String,
        tvMap: ConcurrentHashMap<String, TvDevice>
    ) = coroutineScope {
        // Probe IPs 1 to 254 in parallel batches
        val jobs = (1..254).map { i ->
            async(Dispatchers.IO) {
                val ip = "$subnetPrefix.$i"
                if (tvMap.containsKey(ip)) return@async

                val discovered = checkIpPorts(ip)
                if (discovered != null) {
                    tvMap[ip] = discovered
                }
            }
        }
        jobs.awaitAll()
    }

    private fun checkIpPorts(ip: String): TvDevice? {
        // Ports to probe: 8060 (Roku), 8001 (Samsung), 3000 (LG), 8008 (Android TV)
        val ports = listOf(
            8060 to TvBrand.ROKU,
            8001 to TvBrand.SAMSUNG,
            3000 to TvBrand.LG,
            8008 to TvBrand.ANDROID_TV
        )

        for ((port, brand) in ports) {
            try {
                val socket = Socket()
                socket.connect(java.net.InetSocketAddress(ip, port), 250)
                socket.close()

                // Further inspect Roku for detailed model name
                var modelName = brand.displayName
                if (brand == TvBrand.ROKU) {
                    try {
                        val req = Request.Builder().url("http://$ip:8060/query/device-info").build()
                        httpClient.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val body = resp.body?.string() ?: ""
                                val friendlyName = body.substringAfter("<friendly-device-name>", "")
                                    .substringBefore("</friendly-device-name>", "")
                                if (friendlyName.isNotEmpty()) modelName = friendlyName
                            }
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                return TvDevice(
                    id = "scanned_$ip",
                    name = modelName.ifEmpty { "${brand.displayName} ($ip)" },
                    ipAddress = ip,
                    brand = brand,
                    port = port,
                    isOnline = true,
                    modelName = modelName
                )
            } catch (e: Exception) {
                // Port closed / unreachable
            }
        }
        return null
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.InetAddress) {
                        val ip = address.hostAddress ?: ""
                        if (ip.contains(".")) return ip
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }
}
