package tech.qhuyy.insureinv.utils

import org.bukkit.entity.Player

object PlaceholderUtil {

    fun of(vararg pairs: Pair<String, String>): Map<String, String> {
        return pairs.toMap()
    }

    fun player(player: Player): Map<String, String> {
        return of("player" to player.name)
    }

    fun charges(charges: Int, max: Int): Map<String, String> {
        return of(
            "charges" to charges.toString(),
            "max" to max.toString()
        )
    }

    fun economy(price: Double, balance: Double, amount: Int): Map<String, String> {
        return of(
            "price" to String.format("%.2f", price),
            "balance" to String.format("%.2f", balance),
            "amount" to amount.toString()
        )
    }

    fun status(enabled: Boolean): Map<String, String> {
        return of(
            "status" to if (enabled) "enabled" else "disabled",
            "toggle" to if (enabled) "ON" else "OFF"
        )
    }

    fun method(method: String): Map<String, String> {
        return of("method" to method)
    }

    fun pagination(page: Int, total: Int): Map<String, String> {
        return of(
            "page" to page.toString(),
            "total" to total.toString()
        )
    }

    fun merge(vararg maps: Map<String, String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (map in maps) {
            result.putAll(map)
        }
        return result
    }
}