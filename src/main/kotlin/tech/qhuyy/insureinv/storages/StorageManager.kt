package tech.qhuyy.insureinv.storages

import kotlinx.coroutines.*
import org.bukkit.entity.Player
import tech.qhuyy.insureinv.InsureInv
import tech.qhuyy.insureinv.managers.ConfigManager
import tech.qhuyy.insureinv.models.PlayerDataModel
import tech.qhuyy.insureinv.storages.backend.JsonStorage
import tech.qhuyy.insureinv.storages.backend.MySqlStorage
import tech.qhuyy.insureinv.storages.backend.SqliteStorage
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

class StorageManager(
    private val plugin: InsureInv,
    private val configManager: ConfigManager
) {
    private var currentBackend: StorageBackend? = null
    private val globalCache = ConcurrentHashMap<UUID, PlayerDataModel>()
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun initialize(): Boolean {
        scope.cancel()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val preferredMethod = configManager.getStorageMethod()
        plugin.logger.info("Attempting to initialize storage with method: $preferredMethod")
        val startupTime: Long = System.nanoTime()

        val backends = when (preferredMethod) {
            StorageType.MYSQL -> listOf(
                { MySqlStorage(plugin, configManager) },
                { SqliteStorage(plugin, configManager) },
                { JsonStorage(plugin, configManager) }
            )

            StorageType.SQLITE -> listOf(
                { SqliteStorage(plugin, configManager) },
                { JsonStorage(plugin, configManager) }
            )

            StorageType.JSON -> listOf(
                { JsonStorage(plugin, configManager) }
            )
        }

        for (backendFactory in backends) {
            val backend = backendFactory()
            if (backend.initialize()) {
                currentBackend = backend
                plugin.logger.info("Successfully initialized ${backend.getName()} storage in ${"%.2f".format((System.nanoTime() - startupTime) / 1_000_000.0)}ms")
                return true
            } else {
                plugin.logger.warning("Failed to initialize ${backend.getName()} storage, trying next option...")
            }
        }

        plugin.logger.severe("All storage backends failed to initialize!")
        return false
    }

    fun getPlayerData(player: Player): PlayerDataModel {
        return getPlayerData(player.uniqueId, player.name)
    }

    fun getPlayerData(uuid: UUID, username: String): PlayerDataModel {
        return globalCache.getOrPut(uuid) {
            runBlocking {
                currentBackend?.loadPlayerData(uuid)
            } ?: PlayerDataModel(
                uuid,
                username,
                configManager.getDefaultCharge()
            )
        }
    }

    fun savePlayerData(playerDataModel: PlayerDataModel, async: Boolean = true) {
        globalCache[playerDataModel.uuid] = playerDataModel
        if (async) {
            scope.launch {
                currentBackend?.savePlayerData(playerDataModel)
            }
        } else {
            runBlocking {
                currentBackend?.savePlayerData(playerDataModel)
            }
        }
    }

    fun saveAll(async: Boolean = false) {
        if (async) {
            globalCache.values.forEach { playerData ->
                scope.launch {
                    currentBackend?.savePlayerData(playerData)
                }
            }
        } else {
            runBlocking {
                globalCache.values.forEach { playerData ->
                    currentBackend?.savePlayerData(playerData)
                }
            }
        }
    }

    fun shutdown() {
        plugin.logger.info("Saving all cached player data...")

        runBlocking {
            withTimeoutOrNull(10.seconds) {
                globalCache.values.forEach { playerData ->
                    currentBackend?.savePlayerData(playerData)
                }
            } ?: plugin.logger.warning("Shutdown save timed out after 10 seconds")
        }

        scope.cancel()
        currentBackend?.close()
        globalCache.clear()
    }

    fun isHealthy(): Boolean {
        return currentBackend?.isHealthy() ?: false
    }

    fun getCurrentBackendName(): String {
        return currentBackend?.getName() ?: "None"
    }

    fun reload(): Boolean {
        shutdown()
        return initialize()
    }
}