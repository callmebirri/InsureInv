package tech.qhuyy.insureinv.command

import net.kyori.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import tech.qhuyy.insureinv.InsureInv
import tech.qhuyy.insureinv.economy.EconomyManager
import tech.qhuyy.insureinv.managers.ConfigManager
import tech.qhuyy.insureinv.managers.MessageManager
import tech.qhuyy.insureinv.storages.StorageManager

data class CommandContext(
    val sender: CommandSender,
    val args: Array<String>,
    val plugin: InsureInv,
    val configManager: ConfigManager,
    val storageManager: StorageManager,
    val economyManager: EconomyManager,
    val messageManager: MessageManager,
    val audienceBukkit: BukkitAudiences
) {
    val player: Player?
        get() = sender as? Player

    val playerOrThrow: Player
        get() = player ?: throw IllegalStateException("Sender is not a player")

    fun arg(index: Int): String? = args.getOrNull(index)

    fun argInt(index: Int): Int? = args.getOrNull(index)?.toIntOrNull()

    fun argDouble(index: Int): Double? = args.getOrNull(index)?.toDoubleOrNull()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CommandContext

        if (sender != other.sender) return false
        if (!args.contentEquals(other.args)) return false
        if (plugin != other.plugin) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sender.hashCode()
        result = 31 * result + args.contentHashCode()
        result = 31 * result + plugin.hashCode()
        return result
    }
}
