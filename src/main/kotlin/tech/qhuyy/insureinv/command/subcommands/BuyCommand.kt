package tech.qhuyy.insureinv.command.subcommands

import org.bukkit.command.CommandSender
import tech.qhuyy.insureinv.command.CommandContext
import tech.qhuyy.insureinv.command.SubCommand
import tech.qhuyy.insureinv.utils.PlaceholderUtil

class BuyCommand : SubCommand {
    override val name = "buy"
    override val permission = "insureinv.use"
    override val requiresPlayer = true

    override fun execute(context: CommandContext) {
        val player = context.playerOrThrow
        val messageManager = context.messageManager
        val economyManager = context.economyManager
        val configManager = context.configManager
        val storageManager = context.storageManager

        if (!economyManager.isAvailable()) {
            messageManager.sendMessage(player, "error-economy-unavailable")
            return
        }

        val amount = context.argInt(1)
        if (amount == null || amount <= 0) {
            if (context.arg(1) == null) {
                messageManager.sendMessage(player, "usage-buy")
            } else {
                messageManager.sendMessage(player, "buy-invalid-amount")
            }
            return
        }

        val playerData = storageManager.getPlayerData(player)
        val maxCharges = configManager.getMaxCharges()
        val pricePerCharge = configManager.getPricePerCharge()
        val totalPrice = amount * pricePerCharge

        if (playerData.charges + amount > maxCharges) {
            val canBuy = maxCharges - playerData.charges
            messageManager.sendMessage(
                player, "buy-max-exceeded",
                PlaceholderUtil.of("amount" to canBuy.toString())
            )
            return
        }

        if (player.isOp || player.hasPermission("insureinv.admin")) {
            playerData.addCharges(amount)
            storageManager.savePlayerData(playerData)

            val balance = economyManager.getBalance(player)
            messageManager.sendMessage(
                player, "buy-success",
                PlaceholderUtil.merge(
                    PlaceholderUtil.economy(totalPrice, balance, amount),
                    PlaceholderUtil.charges(playerData.charges, maxCharges)
                )
            )
            return
        }

        val balance = economyManager.getBalance(player)
        if (!economyManager.hasBalance(player, totalPrice)) {
            messageManager.sendMessage(
                player, "buy-insufficient-funds",
                PlaceholderUtil.economy(totalPrice, balance, amount)
            )
            return
        }

        if (economyManager.withdraw(player, totalPrice)) {
            playerData.addCharges(amount)
            storageManager.savePlayerData(playerData)

            messageManager.sendMessage(
                player, "buy-success",
                PlaceholderUtil.merge(
                    PlaceholderUtil.economy(totalPrice, balance - totalPrice, amount),
                    PlaceholderUtil.charges(playerData.charges, maxCharges)
                )
            )
        } else {
            messageManager.sendMessage(player, "error-transaction-failed")
        }
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        return when (args.size) {
            2 -> listOf("1", "5", "10").filter { it.startsWith(args[1]) }
            else -> emptyList()
        }
    }
}
