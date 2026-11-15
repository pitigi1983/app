package com.example.deviceblocker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress

class FirewallService : VpnService() {

    fun blockDestination(destination: String) {
        addBlockedDestination(destination)
    }

    fun unblockDestination(destination: String): Boolean {
        return removeBlockedDestination(destination)
    }

    // Nova funkcija za detekciju uređaja na mreži
    fun detectDevicesOnNetwork(): List<String> {
        val devices = mutableListOf<String>()

        try {
            // Detektuj uređaje koristeći ARP tabelu
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Za Android 8.0+ koristimo pravilnu metodu za skeniranje mreže
                devices.addAll(scanNetworkWithArp())
            } else {
                // Fallback za starije verzije - koristi demo podatke
                devices.add("192.168.1.100")
                devices.add("192.168.1.101")
                devices.add("192.168.1.102")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return devices
    }

    private fun scanNetworkWithArp(): List<String> {
        val devices = mutableListOf<String>()

        try {
            // Pravilna metoda za skeniranje mreže koristeći ARP tabelu
            val process = Runtime.getRuntime().exec("ip neigh")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                if (line?.contains("192.168") == true &&
                    !line.contains("incomplete") &&
                    !line.contains("REACHABLE")) {

                    // Parsiraj IP adresu iz linije
                    val ip = parseIPFromArpLine(line)
                    if (!ip.isNullOrEmpty() && ip != "192.168.1.1" && ip != "192.168.1.0") {
                        devices.add(ip)
                    }
                }
            }

            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return devices
    }

    private fun parseIPFromArpLine(line: String): String? {
        // Parsiranje IP adrese iz ARP linije
        val parts = line.split("\\s+".toRegex())
        if (parts.isNotEmpty()) {
            val ip = parts[0]
            // Proveri da li je validna IPv4 adresa
            if (ip.matches(Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+$"))) {
                return ip
            }
        }
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("DeviceBlocker")
            .setContentText("VPN firewall je aktivan")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }

        val builder = Builder()
        builder.setSession("DeviceBlocker")
        builder.addAddress("10.0.0.2", 32)
        if (blockedDestinations.isEmpty()) {
            builder.addRoute("0.0.0.0", 0)
        }
        builder.addDnsServer("8.8.8.8")

        // Dodajemo funkcionalnost za automatsko blokiranje uređaja
        val devices = detectDevicesOnNetwork()
        for (device in devices) {
            addBlockedDestination(device)
        }

        if (builder.establish() != null) {
            return START_STICKY
        }

        stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onRevoke() {
        stopSelf()
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "DeviceBlocker VPN",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        private val blockedDestinations = mutableSetOf<String>()
        private const val NOTIFICATION_CHANNEL_ID = "deviceblocker_vpn_channel"

        fun getBlockedDestinations(): Set<String> = blockedDestinations.toSet()

        fun addBlockedDestination(destination: String) {
            blockedDestinations.add(destination)
        }

        fun removeBlockedDestination(destination: String): Boolean {
            return blockedDestinations.remove(destination)
        }
    }
}
