package tech.qhuyy.insureinv.metrics

import org.bstats.bukkit.Metrics
import org.bstats.charts.SimplePie
import org.bukkit.Bukkit
import tech.qhuyy.insureinv.InsureInv
import java.util.concurrent.atomic.AtomicBoolean

class MetricsManager(
    private val plugin: InsureInv,
    private val pluginId: Int
) {
    private val started = AtomicBoolean(false)

    fun start() {
        if (!plugin.configManager.getMetrics()) {
            plugin.logger.info("bStats metrics disabled via config.")
            return
        }

        if (!started.compareAndSet(false, true)) return

        Metrics(plugin, pluginId).also { metrics ->
            registerPluginVersion(metrics)
            registerPlayerCount(metrics)
        }

        plugin.logger.info("bStats metrics enabled.")
    }

    private fun registerPluginVersion(metrics: Metrics) {
        metrics.addCustomChart(
            SimplePie("plugin_version") { plugin.pluginVersion }
        )
    }

    private fun registerPlayerCount(metrics: Metrics) {
        metrics.addCustomChart(
            SimplePie("player_count_range") {
                val count = Bukkit.getOnlinePlayers().size
                when {
                    count == 0 -> "0"
                    count <= 5 -> "1-5"
                    count <= 10 -> "6-10"
                    count <= 20 -> "11-20"
                    count <= 50 -> "21-50"
                    count <= 100 -> "51-100"
                    else -> "100+"
                }
            }
        )
    }
}