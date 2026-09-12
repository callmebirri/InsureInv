package tech.qhuyy.insureinv.utils

import tech.qhuyy.insureinv.InsureInv
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class PluginBuildInfo(private val plugin: InsureInv) {

    data class GitInfo(
        val commitIdAbbrev: String,
        val commitMessage: String,
        val commitTime: String,
        val branch: String,
        val buildTime: String,
        val buildVersion: String,
        val isDirty: Boolean
    )

    private val gitInfo: GitInfo by lazy { load() }

    /** Loads packaged Git metadata and combines it with the plugin descriptor version. */
    private fun load(): GitInfo {
        val props = Properties()
        val stream = javaClass.getResourceAsStream("/git.properties")
            ?: Thread.currentThread().contextClassLoader.getResourceAsStream("git.properties")
            ?: return default()

        return stream.use {
            props.load(it)

            GitInfo(
                commitIdAbbrev = props.getProperty("git.commit.id.abbrev", "Unknown"),
                commitMessage = props.getProperty("git.commit.message.short", "Unknown"),
                commitTime = props.getProperty("git.commit.time")?.let { raw -> format(raw) } ?: "Unknown",
                branch = props.getProperty("git.branch", "Unknown"),
                buildTime = props.getProperty("git.build.time")?.let { raw -> format(raw) } ?: "Unknown",
                buildVersion = plugin.pluginVersion,
                isDirty = props.getProperty("git.dirty", "false")
                    .toBooleanStrictOrNull() ?: false
            )
        }
    }

    private fun format(raw: String?): String {
        if (raw.isNullOrBlank()) return "Unknown"

        val instant = runCatching { Instant.parse(raw) }
            .recoverCatching { Instant.ofEpochSecond(raw.toLong()) }
            .getOrNull() ?: return "Invalid timestamp"

        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
            .format(instant)
    }

    /** Returns fallback build information with unknown Git fields and the plugin descriptor version. */
    private fun default() = GitInfo(
        commitIdAbbrev = "Unknown",
        commitMessage = "Unknown",
        commitTime = "Unknown",
        branch = "Unknown",
        buildTime = "Unknown",
        buildVersion = plugin.pluginVersion,
        isDirty = false
    )

    fun info(): GitInfo = gitInfo

    val commitIdAbbrev get() = gitInfo.commitIdAbbrev
    val commitMessage get() = gitInfo.commitMessage
    val commitTime get() = gitInfo.commitTime
    val branch get() = gitInfo.branch
    val buildTime get() = gitInfo.buildTime
    val buildVersion get() = gitInfo.buildVersion
    val isDirty get() = gitInfo.isDirty

    fun toMap(): Map<String, Any> = mapOf(
        "version" to buildVersion,
        "branch" to branch,
        "commit" to commitIdAbbrev,
        "commitMessage" to commitMessage,
        "commitTime" to commitTime,
        "buildTime" to buildTime,
        "isDirty" to isDirty
    )

    /** Returns the descriptor name, or the fixed stylized display name when [fancy] is true. */
    @Suppress("DEPRECATION")
    fun getPluginName(fancy: Boolean): String {
        return if (!fancy) plugin.description.name else "ɪɴѕᴜʀᴇɪɴᴠ"
    }
}