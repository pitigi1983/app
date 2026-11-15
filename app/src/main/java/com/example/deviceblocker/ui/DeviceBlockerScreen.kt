package com.example.deviceblocker.ui

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.deviceblocker.FirewallService

@Composable
fun DeviceBlockerScreen() {
    val context = LocalContext.current
    val blockedDestinations = remember {
        mutableStateListOf<String>().apply {
            addAll(FirewallService.getBlockedDestinations())
        }
    }
    var newDestination by remember { mutableStateOf("") }
    var scannedDevices by remember { mutableStateOf<List<String>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "WI-FI Bloker",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "VPN Firewall za blokiranje uređaja na mreži",
                style = MaterialTheme.typography.bodyLarge
            )
            Button(
                onClick = {
                    val intent = Intent(context, FirewallService::class.java)
                    ContextCompat.startForegroundService(context, intent)
                }
            ) {
                Text("Pokreni firewall")
            }
            Button(
                onClick = {
                    context.stopService(Intent(context, FirewallService::class.java))
                }
            ) {
                Text("Zaustavi firewall")
            }

            // Nova funkcionalnost za skeniranje uređaja
            Button(
                onClick = {
                    isScanning = true
                    // U stvarnoj implementaciji, ovo bi trebalo da koristi ARP skeniranje ili slično
                    // Za demo svrhe, koristimo fiksne vrednosti
                    scannedDevices = listOf(
                        "192.168.1.100 - Samsung Galaxy S23",
                        "192.168.1.101 - iPhone 14",
                        "192.168.1.102 - Laptop Dell"
                    )
                    isScanning = false
                }
            ) {
                if (isScanning) {
                    Text("Skeniranje...")
                } else {
                    Text("Skeniraj uređaje na mreži")
                }
            }

            if (scannedDevices.isNotEmpty()) {
                Text(
                    text = "Uređaji na mreži:",
                    style = MaterialTheme.typography.titleMedium
                )
                scannedDevices.forEach { device ->
                    Text(
                        text = device,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(
                        onClick = {
                            // Dodaj uređaj na listu blokiranja
                            val ip = device.split(" - ")[0]
                            FirewallService.addBlockedDestination(ip)
                            if (ip !in blockedDestinations) {
                                blockedDestinations.add(ip)
                            }
                            val intent = Intent(context, FirewallService::class.java)
                            ContextCompat.startForegroundService(context, intent)
                        }
                    ) {
                        Text("Blokiraj uređaj")
                    }
                }
            }

            Text(
                text = "Ručno dodavanje IP adrese:",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = newDestination,
                onValueChange = { newDestination = it },
                label = { Text("IP adresa") }
            )
            Button(
                onClick = {
                    val destination = newDestination.trim()
                    if (destination.isNotEmpty()) {
                        FirewallService.addBlockedDestination(destination)
                        if (destination !in blockedDestinations) {
                            blockedDestinations.add(destination)
                        }
                        newDestination = ""
                        val intent = Intent(context, FirewallService::class.java)
                        ContextCompat.startForegroundService(context, intent)
                    }
                }
            ) {
                Text("Dodaj na listu blokiranja")
            }

            Text(
                text = "Blokirane IP adrese:",
                style = MaterialTheme.typography.titleMedium
            )

            blockedDestinations.forEach { destination ->
                Text(
                    text = destination,
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(
                    onClick = {
                        FirewallService.removeBlockedDestination(destination)
                        blockedDestinations.remove(destination)
                        val intent = Intent(context, FirewallService::class.java)
                        ContextCompat.startForegroundService(context, intent)
                    }
                ) {
                    Text("Odblokiraj")
                }
            }
        }
    }
}
