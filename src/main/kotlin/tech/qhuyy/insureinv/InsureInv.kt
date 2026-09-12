package tech.qhuyy.insureinv

import com.tcoded.folialib.FoliaLib
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import tech.qhuyy.insureinv.command.InsureInvCommand
import tech.qhuyy.insureinv.economy.EconomyManager
import tech.qhuyy.insureinv.listeners.PlayerDeathListener
import tech.qhuyy.insureinv.managers.ConfigManager
import tech.qhuyy.insureinv.managers.MessageManager
import tech.qhuyy.insureinv.metrics.MetricsManager
import tech.qhuyy.insureinv.storages.StorageManager
import tech.qhuyy.insureinv.utils.PluginBuildInfo
import tech.qhuyy.insureinv.utils.ServerSoftware

private const val PLUGIN_ID: Int = 29775

open class InsureInv : JavaPlugin() {

    lateinit var audienceBukkit: BukkitAudiences
        private set
    lateinit var serverSoftware: ServerSoftware
        private set
    lateinit var pluginBuildInfo: PluginBuildInfo
        private set
    lateinit var foliaLib: FoliaLib
        private set
    lateinit var configManager: ConfigManager
        private set
    lateinit var metricsManager: MetricsManager
        private set
    lateinit var messageManager: MessageManager
        private set
    lateinit var economyManager: EconomyManager
        private set
    lateinit var storageManager: StorageManager
        private set
    @Suppress("DEPRECATION")
    val pluginVersion: String get() = description.version

    override fun onEnable() {
        foliaLib = FoliaLib(this)
        serverSoftware = ServerSoftware.detectServerSoftware(foliaLib)
        if (serverSoftware in setOf(
                ServerSoftware.UNKNOWN
            )
        ) {
            logger.severe("═══════════════════════════════════════════════════════════════")
            logger.severe("InsureInv requires Paper, Spigot or Folia to run ( including forks ).")
            logger.severe("Non-bukkit and other server software are not supported.")
            logger.severe("Please upgrade to Paper: https://papermc.io/downloads/paper")
            logger.severe("═══════════════════════════════════════════════════════════════")
            server.pluginManager.disablePlugin(this)
            return
        }
        audienceBukkit = BukkitAudiences.create(this)
        pluginBuildInfo = PluginBuildInfo(this)

        if (foliaLib.isFolia) {
            logger.info("Running on Folia - region-safe scheduling enabled")
        } else {
            logger.info("Running on Spigot/Paper - standard scheduling enabled")
        }

        configManager = ConfigManager(this)

        metricsManager = MetricsManager(
            this,
            PLUGIN_ID
        )
        metricsManager.start()

        messageManager = MessageManager(this, configManager)

        economyManager = EconomyManager(this, serverSoftware)
        economyManager.initialize()

        storageManager = StorageManager(this, configManager)
        if (!storageManager.initialize()) {
            logger.severe("Failed to initialize storage system! Disabling plugin...")
            server.pluginManager.disablePlugin(this)
            return
        }

        registerCommands()
        registerEvents()

        logger.info("InsureInv v$pluginVersion enabled successfully! Have Fun :D")
        sendStartupLog()
    }

    override fun onDisable() {
        if (::storageManager.isInitialized) {
            storageManager.shutdown()
        }

        if(::audienceBukkit.isInitialized) {
            audienceBukkit.close()
        }

        logger.info("InsureInv disabled.")
    }

    private fun registerCommands() {
        val commandHandler = InsureInvCommand(
            this,
            configManager,
            storageManager,
            economyManager,
            messageManager,
            audienceBukkit
        )

        getCommand("insureinv")?.apply {
            setExecutor(commandHandler)
            tabCompleter = commandHandler
        }
    }

    private fun registerEvents() {
        val playerDeathListener = PlayerDeathListener(
            configManager,
            storageManager,
            messageManager
        )

        server.pluginManager.registerEvents(playerDeathListener, this)
    }

    private fun sendStartupLog() {
        listOf(
            "",
            " <aqua>${pluginBuildInfo.getPluginName(true)}</aqua> <gray>ᴠ${pluginBuildInfo.buildVersion}</gray>",
            " <dark_gray>--------------------------------------</dark_gray>",
            " <red>ɪɴꜰᴏʀᴍᴀᴛɪᴏɴ</red>",
            "<gray>   • </gray><white>ɴᴀᴍᴇ: </white><aqua>${pluginBuildInfo.getPluginName(true)}</aqua>",
            "<gray>   • </gray><white>ᴀᴜᴛʜᴏʀ: </white><aqua>birri</aqua>",
            " <dark_gray>--------------------------------------</dark_gray>",
            ""
        ).forEach {
            audienceBukkit.console().sendMessage(
                MiniMessage.miniMessage().deserialize(it)
            )
        }
    }
}
