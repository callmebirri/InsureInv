package tech.qhuyy.insureinv.managers

import org.bukkit.configuration.file.FileConfiguration
import tech.qhuyy.insureinv.InsureInv
import tech.qhuyy.insureinv.economy.EconomyType
import tech.qhuyy.insureinv.storages.StorageType

@Suppress("unused")
class ConfigManager(
    private val plugin: InsureInv
) {
    private var config: FileConfiguration = plugin.config
    private var cachedDefaultCharge: Int = 0

    init {
        plugin.saveDefaultConfig()
        reload()
    }

    fun reload() {
        plugin.reloadConfig()
        config = plugin.config
        validateConfig()
    }

    private fun validateConfig() {
        val requiredPaths = listOf(
            "storage.method",
            "economy.default-charge",
            "economy.price-per-charge",
            "economy.max-charges-per-player"
        )

        for (path in requiredPaths) {
            if (!config.contains(path)) {
                plugin.logger.warning("Missing configuration value: $path")
            }
        }

        if (getPricePerCharge() <= 0) {
            plugin.logger.warning("Invalid price-per-charge, must be positive!")
        }

        if (getMaxCharges() <= 0) {
            plugin.logger.warning("Invalid max-charges-per-player, must be positive!")
        }

        val rawDefaultCharge = config.getInt("economy.default-charge", 0)
        val max = getMaxCharges()
        cachedDefaultCharge = if (rawDefaultCharge !in 0..max) {
            plugin.logger.warning("Default charge must be between 0 and $max! Clamping to ${rawDefaultCharge.coerceIn(0, max)}")
            rawDefaultCharge.coerceIn(0, max)
        } else {
            rawDefaultCharge
        }
    }

    fun getStorageMethod(): StorageType = StorageType.fromString(
        config.getString("storage.method", "sqlite")
    )

    fun getMySQLHost(): String = config.getString("storage.mysql.host", "localhost") ?: "localhost"
    fun getMySQLPort(): Int = config.getInt("storage.mysql.port", 3306)
    fun getMySQLDatabase(): String = config.getString("storage.mysql.database", "hyticinv") ?: "hyticinv"
    fun getMySQLUsername(): String = config.getString("storage.mysql.username", "root") ?: "root"
    fun getMySQLPassword(): String = config.getString("storage.mysql.password", "password") ?: "password"

    fun getMySQLMaxPoolSize(): Int = config.getInt("storage.mysql.pool.maximum-pool-size", 10)
    fun getMySQLMinIdle(): Int = config.getInt("storage.mysql.pool.minimum-idle", 2)
    fun getMySQLConnectionTimeout(): Long = config.getLong("storage.mysql.pool.connection-timeout", 30000)
    fun getMySQLIdleTimeout(): Long = config.getLong("storage.mysql.pool.idle-timeout", 600000)
    fun getMySQLMaxLifetime(): Long = config.getLong("storage.mysql.pool.max-lifetime", 1800000)

    fun getSQLitePath(): String {
        val configPath = config.getString("storage.sqlite.path", "data.db") ?: "data.db"
        return if (configPath.startsWith("/") || configPath.contains(":")) {
            configPath
        } else {
            "${plugin.dataFolder}/$configPath"
        }
    }

    fun getJsonPath(): String {
        val configPath = config.getString("storage.json.path", "data.json") ?: "data.json"
        return if (configPath.startsWith("/") || configPath.contains(":")) {
            configPath
        } else {
            "${plugin.dataFolder}/$configPath"
        }
    }

    fun getDefaultCharge(): Int = cachedDefaultCharge
    fun getPricePerCharge(): Double = config.getDouble("economy.price-per-charge", 100.0)
    fun getMaxCharges(): Int = config.getInt("economy.max-charges-per-player", 10)

    fun setPricePerCharge(price: Double) {
        config.set("economy.price-per-charge", price)
        plugin.saveConfig()
    }

    fun setMaxCharges(max: Int) {
        config.set("economy.max-charges-per-player", max)
        plugin.saveConfig()
    }

    fun isPrefixEnabled(): Boolean = config.getBoolean("messages.enable-prefix", true)
    fun getPrefix(): String = config.getString("messages.prefix", "") ?: ""

    fun getEconomyProviderType(): EconomyType {
        return EconomyType.fromString(
            config.getString("economy.provider")?.uppercase()
        )
    }

    fun getMetrics(): Boolean = config.getBoolean("metrics.enabled", true)
}
