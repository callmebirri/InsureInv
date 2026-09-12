package tech.qhuyy.insureinv.economy.providers

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import tech.qhuyy.insureinv.economy.EconomyProvider

class VaultUnlockedProvider(
    private val economy: net.milkbowl.vault2.economy.Economy
) : EconomyProvider {
    private val pluginName = "InsureInv"

    override fun isAvailable(): Boolean = true

    @Suppress("DEPRECATION")
    override fun getBalance(player: OfflinePlayer): Double =
        economy.getBalance(pluginName, player.uniqueId).toDouble()

    override fun hasBalance(player: OfflinePlayer, amount: Double): Boolean =
        economy.has(pluginName, player.uniqueId, amount.toBigDecimal())

    override fun withdraw(player: OfflinePlayer, amount: Double): Boolean {
        return economy.withdraw(pluginName, player.uniqueId, amount.toBigDecimal()).transactionSuccess()
    }

    override fun deposit(player: OfflinePlayer, amount: Double): Boolean {
        return economy.deposit(pluginName, player.uniqueId, amount.toBigDecimal()).transactionSuccess()
    }

    @Suppress("DEPRECATION")
    override fun formatAmount(amount: Double): String = economy.format(amount.toBigDecimal())

    companion object {
        fun create(): VaultUnlockedProvider? {
            val pm = Bukkit.getPluginManager()
            val vaultUnlocked = pm.getPlugin("Vault") ?: return null
            if (!vaultUnlocked.isEnabled) return null

            return try {
                val economyClass = Class.forName("net.milkbowl.vault2.economy.Economy")

                @Suppress("UNCHECKED_CAST")
                val provider = Bukkit.getServicesManager()
                    .load(economyClass as Class<Any>) as? net.milkbowl.vault2.economy.Economy

                provider?.let { VaultUnlockedProvider(it) }
            } catch (_: ClassNotFoundException) {
                null
            } catch (_: Exception) {
                null
            }
        }
    }
}
