package tech.qhuyy.insureinv.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import tech.qhuyy.insureinv.managers.ConfigManager
import tech.qhuyy.insureinv.managers.MessageManager
import tech.qhuyy.insureinv.storages.StorageManager
import tech.qhuyy.insureinv.utils.PlaceholderUtil

@Suppress("UNUSED")
class PlayerDeathListener(
    private val configManager: ConfigManager,
    private val storageManager: StorageManager,
    private val messageManager: MessageManager
) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (event.keepInventory) return

        val player = event.entity
        val playerData = storageManager.getPlayerData(player)

        if (!playerData.protectionEnabled) {
            messageManager.sendMessage(player, "protection-disabled")
            return
        }

        if (!playerData.hasCharges()) {
            messageManager.sendMessage(player, "no-charges")
            return
        }

        playerData.consumeCharge()
        storageManager.savePlayerData(playerData, async = true)

        event.keepInventory = true
        event.keepLevel = true
        event.drops.clear()
        event.droppedExp = 0

        messageManager.sendMessage(
            player, "protection-triggered",
            PlaceholderUtil.charges(playerData.charges, configManager.getMaxCharges())
        )
    }
}