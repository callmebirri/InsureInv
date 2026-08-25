package tech.qhuyy.insureinv.economy

import tech.qhuyy.insureinv.InsureInv
import tech.qhuyy.insureinv.economy.providers.NoneProvider
import tech.qhuyy.insureinv.economy.providers.PlayerPointsProvider
import tech.qhuyy.insureinv.economy.providers.VaultProvider
import tech.qhuyy.insureinv.economy.providers.VaultUnlockedProvider
import tech.qhuyy.insureinv.utils.ServerSoftware
import java.util.logging.Logger

class EconomyRegistry(
    plugin: InsureInv,
    private val platform: ServerSoftware
) {
    private val logger: Logger = plugin.logger
    private val config = plugin.configManager

    private val priorityList: List<EconomyType>
        get() = when (platform) {
            ServerSoftware.FOLIA -> listOf(
                EconomyType.VAULT_UNLOCKED,
                EconomyType.PLAYER_POINTS,
                EconomyType.NONE
            )

            else -> listOf(
                EconomyType.VAULT,
                EconomyType.PLAYER_POINTS,
                EconomyType.VAULT_UNLOCKED,
                EconomyType.NONE
            )
        }

    fun resolve(): EconomyProvider {
        val preferred = config.getEconomyProviderType()

        if (preferred == EconomyType.NONE) {
            logger.info("Economy provider set to NONE - economy features disabled")
            return NoneProvider
        }

        if (isCompatible(preferred)) {
            createProvider(preferred)?.let {
                logger.info("Using economy provider: $preferred")
                return it
            }
            logger.warning("Preferred economy $preferred not available, trying fallback...")
        } else {
            logger.warning("Preferred economy $preferred is not compatible with $platform, trying fallback...")
        }

        for (type in priorityList) {
            if (type == EconomyType.NONE) continue
            createProvider(type)?.let {
                logger.info("Using economy provider: $type")
                return it
            }
        }

        logger.warning("No economy provider available. Using NONE provider.")
        return NoneProvider
    }

    private fun isCompatible(type: EconomyType): Boolean {
        return !(platform == ServerSoftware.FOLIA && type == EconomyType.VAULT)
    }

    private fun createProvider(type: EconomyType): EconomyProvider? {
        return when (type) {
            EconomyType.VAULT -> VaultProvider.create()
            EconomyType.VAULT_UNLOCKED -> VaultUnlockedProvider.create()
            EconomyType.PLAYER_POINTS -> PlayerPointsProvider.create()
            EconomyType.NONE -> NoneProvider
        }
    }
}