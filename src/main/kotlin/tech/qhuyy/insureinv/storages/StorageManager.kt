package tech.qhuyy.insureinv.storages

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
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
    private var writerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    @Volatile
    private var saveQueue = Channel<SaveTask>(Channel.UNLIMITED)
    @Volatile
    private var writerJob: Job? = null
    @Volatile
    private var writerRunning = false

    private class SaveTask(
        val block: suspend () -> Unit,
        val completion: CompletableDeferred<Unit>? = null
    )

    private fun startWriter() {
        saveQueue = Channel(Channel.UNLIMITED)
        writerRunning = true
        writerJob = writerScope.launch {
            for (task in saveQueue) {
                try {
                    task.block()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    plugin.logger.severe("Failed to save player data: ${e.message}")
                    e.printStackTrace()
                } finally {
                    task.completion?.complete(Unit)
                }
            }
            writerRunning = false
        }
    }

    private fun enqueue(block: suspend () -> Unit) {
        if (!writerRunning) {
            plugin.logger.warning("Storage writer is not running, dropping save task")
            return
        }
        if (saveQueue.trySend(SaveTask(block)).isFailure) {
            plugin.logger.warning("Storage writer rejected a save task")
        }
    }

    private fun enqueueAndAwait(block: suspend () -> Unit) {
        if (!writerRunning) {
            plugin.logger.warning("Storage writer is not running, dropping save task")
            return
        }

        val completion = CompletableDeferred<Unit>()
        if (saveQueue.trySend(SaveTask(block, completion)).isFailure) {
            plugin.logger.warning("Storage writer rejected a save task")
            return
        }

        val completed = runBlocking {
            withTimeoutOrNull(SAVE_TIMEOUT) { completion.await() } != null
        }
        if (!completed) {
            plugin.logger.warning("Synchronous save timed out after $SAVE_TIMEOUT")
        }
    }

    fun initialize(): Boolean {
        scope.cancel()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        stopWriter()
        writerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        startWriter()

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
        globalCache[uuid]?.let { return it }

        val loaded = runBlocking {
            currentBackend?.loadPlayerData(uuid)
        } ?: PlayerDataModel(
            uuid,
            username,
            configManager.getDefaultCharge()
        )
        loaded.username = username

        return globalCache.putIfAbsent(uuid, loaded) ?: loaded
    }

    fun preloadPlayerData(player: Player) {
        val uuid = player.uniqueId
        val existing = globalCache[uuid]
        if (existing != null) {
            if (existing.username != player.name) {
                existing.username = player.name
                savePlayerData(existing)
            }
            return
        }

        scope.launch {
            val loaded = currentBackend?.loadPlayerData(uuid)
                ?: PlayerDataModel(uuid, player.name, configManager.getDefaultCharge())

            loaded.username = player.name
            globalCache.putIfAbsent(uuid, loaded)
        }
    }

    fun unloadPlayerData(player: Player, async: Boolean = true) {
        val uuid = player.uniqueId
        val playerData = globalCache[uuid] ?: return

        if (async) {
            enqueue {
                currentBackend?.savePlayerData(playerData)
                globalCache.remove(uuid, playerData)
            }
        } else {
            enqueueAndAwait {
                currentBackend?.savePlayerData(playerData)
            }
            globalCache.remove(uuid, playerData)
        }
    }

    fun savePlayerData(playerDataModel: PlayerDataModel, async: Boolean = true) {
        globalCache[playerDataModel.uuid] = playerDataModel
        if (async) {
            enqueue {
                currentBackend?.savePlayerData(playerDataModel)
            }
        } else {
            enqueueAndAwait {
                currentBackend?.savePlayerData(playerDataModel)
            }
        }
    }

    fun saveAll(async: Boolean = false) {
        if (async) {
            enqueue {
                saveCacheSnapshot()
            }
        } else {
            enqueueAndAwait {
                saveCacheSnapshot()
            }
        }
    }

    private suspend fun saveCacheSnapshot() {
        globalCache.values.forEach { playerData ->
            currentBackend?.savePlayerData(playerData)
        }
    }

    private fun stopWriter() {
        if (!writerRunning) {
            saveQueue.close()
            writerRunning = false
            return
        }

        saveQueue.close()

        val drained = runBlocking {
            withTimeoutOrNull(DRAIN_TIMEOUT) {
                writerJob?.join()
                true
            } ?: false
        }

        if (!drained) {
            plugin.logger.warning("Storage writer drain timed out after $DRAIN_TIMEOUT")
        }

        writerRunning = false
        writerScope.cancel()
    }

    fun shutdown() {
        plugin.logger.info("Saving all cached player data...")

        enqueue {
            saveCacheSnapshot()
        }
        stopWriter()

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

    companion object {
        private val SAVE_TIMEOUT = 10.seconds
        private val DRAIN_TIMEOUT = 10.seconds
    }
}
