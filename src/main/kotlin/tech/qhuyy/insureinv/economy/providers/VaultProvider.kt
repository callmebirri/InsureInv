package tech.qhuyy.insureinv.economy.providers

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import tech.qhuyy.insureinv.economy.EconomyProvider

class VaultProvider(
    private val economy: net.milkbowl.vault.economy.Economy
) : EconomyProvider {
    override fun isAvailable(): Boolean = true

    override fun getBalance(player: OfflinePlayer): Double = economy.getBalance(player)

    override fun hasBalance(player: OfflinePlayer, amount: Double): Boolean = economy.has(player, amount)

    override fun withdraw(player: OfflinePlayer, amount: Double): Boolean {
        return economy.withdrawPlayer(player, amount).transactionSuccess()
    }

    override fun deposit(player: OfflinePlayer, amount: Double): Boolean {
        return economy.depositPlayer(player, amount).transactionSuccess()
    }

    override fun formatAmount(amount: Double): String = economy.format(amount)

    companion object {
        fun create(): VaultProvider? {
            val pm = Bukkit.getPluginManager()
            val vault = pm.getPlugin("Vault") ?: return null
            if (!vault.isEnabled) return null

            return try {
                val economyClass = Class.forName("net.milkbowl.vault.economy.Economy")
                @Suppress("UNCHECKED_CAST")
                val provider = Bukkit.getServicesManager()
                    .load(economyClass as Class<Any>) as? net.milkbowl.vault.economy.Economy

                provider?.let { VaultProvider(it) }
            } catch (_: ClassNotFoundException) {
                null
            } catch (_: Exception) {
                null
            }
        }
    }
}