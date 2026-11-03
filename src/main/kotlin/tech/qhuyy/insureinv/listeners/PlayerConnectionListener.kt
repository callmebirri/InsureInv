package tech.qhuyy.insureinv.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import tech.qhuyy.insureinv.storages.StorageManager

@Suppress("UNUSED")
class PlayerConnectionListener(
    private val storageManager: StorageManager
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        storageManager.preloadPlayerData(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        storageManager.unloadPlayerData(event.player)
    }
}
