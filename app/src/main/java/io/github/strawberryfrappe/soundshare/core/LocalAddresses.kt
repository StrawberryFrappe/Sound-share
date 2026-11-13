package io.github.strawberryfrappe.soundshare.core

import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

object LocalAddresses {
    /** This device's addresses on local networks (Wi-Fi or its own hotspot), for typing into the other device. */
    fun privateIpv4(): List<String> = try {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.asSequence() }
            .filter { it is Inet4Address && it.isSiteLocalAddress }
            .mapNotNull { it.hostAddress }
            .toList()
    } catch (_: SocketException) {
        emptyList()
    }
}
