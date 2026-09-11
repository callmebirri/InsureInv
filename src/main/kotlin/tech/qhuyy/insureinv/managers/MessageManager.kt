package tech.qhuyy.insureinv.managers

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import tech.qhuyy.insureinv.InsureInv
import java.io.File

@Suppress("UNUSED")
class MessageManager(
    private val plugin: InsureInv,
    private val configManager: ConfigManager,
    private val audienceBukkit: BukkitAudiences
) {
    private val miniMessage: MiniMessage = MiniMessage.miniMessage()
    private var messages: YamlConfiguration = YamlConfiguration()

    private fun audiencePlayer(player: Player): Audience = audienceBukkit.player(player)
    private fun audienceSender(sender: CommandSender): Audience = audienceBukkit.sender(sender)

    private val noPrefixKeys = setOf(
        "help.header", "help.footer", "help.buy", "help.toggle", "help.info",
        "help.usage", "help.usage-set", "help.usage-give", "help.usage-reset",
        "help.config", "help.config-setprice", "help.config-setmax", "help.config-reload",
        "help.help",
        "info.header", "info.footer", "status.enabled", "status.disabled"
    )

    private val placeholderPattern = Regex("\\{([^}]+)}")

    private val keyMapping = buildKeyMapping()

    init { reload() }

    fun reload() {
        val messagesFile = File(plugin.dataFolder, "messages.yml")
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false)
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile)
    }

    fun sendMessage(sender: CommandSender, messageKey: String, placeholders: Map<String, String> = emptyMap()) {
        val i18nKey = resolveKey(messageKey)
        val rawMessage = resolveMessage(i18nKey, placeholders)
        if (sender is Player) {
            val shouldAddPrefix = configManager.isPrefixEnabled() && !noPrefixKeys.contains(i18nKey)
            val fullMessage = if (shouldAddPrefix) configManager.getPrefix() + rawMessage else rawMessage
            audiencePlayer(sender).sendMessage(
                miniMessage.deserialize(fullMessage)
            )
        } else {
            audienceSender(sender).sendMessage(
                miniMessage.deserialize(rawMessage)
            )
        }
    }

    fun sendMessage(player: Player, messageKey: String, placeholders: Map<String, String> = emptyMap()) {
        val i18nKey = resolveKey(messageKey)
        val rawMessage = resolveMessage(i18nKey, placeholders)
        val shouldAddPrefix = configManager.isPrefixEnabled() && !noPrefixKeys.contains(i18nKey)
        val fullMessage = if (shouldAddPrefix) configManager.getPrefix() + rawMessage else rawMessage
        audiencePlayer(player).sendMessage(
            miniMessage.deserialize(fullMessage)
        )
    }

    fun parseMessage(message: String): Component {
        return miniMessage.deserialize(message)
    }

    fun resolve(key: String, placeholders: Map<String, String> = emptyMap()): String {
        val i18nKey = resolveKey(key)
        return resolveMessage(i18nKey, placeholders)
    }

    fun resolveComponent(key: String, placeholders: Map<String, String> = emptyMap()): Component {
        return miniMessage.deserialize(resolve(key, placeholders))
    }

    fun sendRawMessage(sender: CommandSender, message: String, placeholders: Map<String, String> = emptyMap()) {
        val resolvedMessage = replacePlaceholders(message, placeholders)
        val component = miniMessage.deserialize(resolvedMessage)
        audienceSender(sender).sendMessage(component)
    }

    private fun resolveKey(legacyKey: String): String {
        return keyMapping[legacyKey] ?: legacyKey
    }

    private fun resolveMessage(key: String, placeholders: Map<String, String>): String {
        val raw = messages.getString(key) ?: "<red>Missing: $key</red>"
        return replacePlaceholders(raw, placeholders)
    }

    private fun replacePlaceholders(message: String, placeholders: Map<String, String>): String {
        if (placeholders.isEmpty()) return message
        return placeholderPattern.replace(message) { matchResult ->
            val key = matchResult.groupValues[1]
            placeholders[key] ?: matchResult.value
        }
    }

    companion object {
        private fun buildKeyMapping(): Map<String, String> = mapOf(
            "buy-success" to "buy.success",
            "buy-insufficient-funds" to "buy.insufficient-funds",
            "buy-max-exceeded" to "buy.max-exceeded",
            "buy-invalid-amount" to "buy.invalid-amount",
            "toggle-on" to "toggle.on",
            "toggle-off" to "toggle.off",
            "protection-triggered" to "protection.triggered",
            "no-charges" to "protection.no-charges",
            "protection-disabled" to "protection.disabled",
            "info-header" to "info.header",
            "info-player" to "info.player",
            "info-charges" to "info.charges",
            "info-status" to "info.status",
            "info-total-purchases" to "info.total-purchases",
            "info-usage-count" to "info.usage-count",
            "info-footer" to "info.footer",
            "admin-set-success" to "admin.set-success",
            "admin-toggle-success" to "admin.toggle-success",
            "admin-give-success" to "admin.give-success",
            "admin-give-max-exceeded" to "admin.give-max-exceeded",
            "admin-reset-success" to "admin.reset-success",
            "price-updated" to "admin.price-updated",
            "max-updated" to "admin.max-updated",
            "reload-complete" to "system.reload-complete",
            "reload-storage-changed" to "system.reload-storage-changed",
            "no-permission" to "error.no-permission",
            "player-not-found" to "error.player-not-found",
            "invalid-amount" to "error.invalid-amount",
            "error-player-only" to "error.player-only",
            "error-economy-unavailable" to "error.economy-unavailable",
            "error-transaction-failed" to "error.transaction-failed",
            "usage-buy" to "usage.buy",
            "usage-set" to "usage.set",
            "usage-give" to "usage.give",
            "usage-reset" to "usage.reset",
            "usage-setprice" to "usage.setprice",
            "usage-setmax" to "usage.setmax",
            "usage-usage" to "usage.usage-group",
            "usage-config" to "usage.config-group",
            "help-header" to "help.header",
            "help-buy" to "help.buy",
            "help-toggle" to "help.toggle",
            "help-info" to "help.info",
            "help-usage" to "help.usage",
            "help-usage-set" to "help.usage-set",
            "help-usage-give" to "help.usage-give",
            "help-usage-reset" to "help.usage-reset",
            "help-config" to "help.config",
            "help-config-setprice" to "help.config-setprice",
            "help-config-setmax" to "help.config-setmax",
            "help-config-reload" to "help.config-reload",
            "help-help" to "help.help",
            "help-footer" to "help.footer",
            "status-enabled" to "status.enabled",
            "status-disabled" to "status.disabled"
        )
    }
}