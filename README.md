<div align="center">

# InsureInv

[![Modrinth](https://img.shields.io/badge/available-Modrinth-00AF5C?logo=modrinth)](https://modrinth.com/plugin/insureinv)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

**A flexible, charge-based inventory protection plugin for modern Minecraft servers.**

</div>

---

## 🌟 Overview

**InsureInv** gives players peace of mind by protecting their items and experience upon death using a robust **charge
system**. Instead of enabling a global `keepInventory` rule that trivializes survival gameplay, InsureInv brings balance
by requiring players to purchase "charges" using in-game currency. Each death automatically consumes one charge to keep
their inventory intact.

Designed with performance in mind, it fully supports **Paper 1.20.1+** and **Folia**.

## ✨ Key Features

- **🛡️ Charge-based Saves:** Move away from binary `keepInventory` rules. Define custom prices and maximum limits for
  inventory protection charges.
- **⚡ Next-Gen Compatibility:** Built natively for modern server environments, with full **Folia** support.
- **💰 Seamless Economy Integration:** Hook into **Vault**, **VaultUnlocked**, or **PlayerPoints** to let players
  seamlessly buy charges. Economy features can also be completely disabled for custom use cases.
- **💾 Flexible Database Options:** Use what works for your network—scales from simple **SQLite** and **JSON** to
  dedicated **MySQL** connection pools (via HikariCP).

---

## 🚀 Commands & Permissions

The main command is `/insureinv`. You can also use the aliases `/inv` or `/hinv`.

### Player Commands

| Command                   | Description                                | Permission      |
|---------------------------|--------------------------------------------|-----------------|
| `/insureinv buy <amount>` | Purchase inventory protection charges.     | `insureinv.use` |
| `/insureinv toggle`       | Enable or disable active protection.       | `insureinv.use` |
| `/insureinv info`         | Check remaining charges and active status. | `insureinv.use` |
| `/insureinv help`         | Show the user help menu.                   | `insureinv.use` |

### Admin — Usage Commands

**Note:** `insureinv.admin` inherits all permissions from `insureinv.use`.

| Command                                   | Description                           | Permission        |
|-------------------------------------------|---------------------------------------|-------------------|
| `/insureinv usage set <player> <amount>`  | Directly set a player's charge count. | `insureinv.admin` |
| `/insureinv usage give <player> <amount>` | Give additional charges to a player.  | `insureinv.admin` |
| `/insureinv usage reset <player>`         | Reset a player's charges to 0.        | `insureinv.admin` |

### Admin — Config Commands

| Command                              | Description                                            | Permission        |
|--------------------------------------|--------------------------------------------------------|-------------------|
| `/insureinv config setprice <price>` | Globally change the economy price per charge.          | `insureinv.admin` |
| `/insureinv config setmax <amount>`  | Globally change the maximum charges a player can hold. | `insureinv.admin` |
| `/insureinv config reload`           | Reload the plugin configuration.                       | `insureinv.admin` |
| `/insureinv version`                 | View detailed version and build info.                  | `insureinv.admin` |

---

## ⚙️ Configuration Configuration

The `config.yml` file is straightforward to set up out of the box.

### `storage` Options

- **`sqlite` (Default)**: Saves data in a local `data.db` file. Clean and simple.
- **`mysql`**: Highly recommended for multiserver networks. Uses HikariCP for asynchronous connection pooling.
- **`json`**: Saves data locally in a `data.json` file. Only use for testing or very low-traffic servers.

### `economy` Options

Controls how players acquire charges:

- `VAULT` (Default): Hooks into your Vault-compatible economy plugin (EssentialsX, etc.). **Not available on Folia.**
- `VAULT_UNLOCKED`: Hooks into [VaultUnlocked](https://www.spigotmc.org/resources/vaultunlocked.117277/), a modern fork
  of Vault with **Folia support**.
- `PLAYER_POINTS`: Deducts from [PlayerPoints](https://www.spigotmc.org/resources/playerpoints.80745/). Works on all
  platforms.
- `NONE`: No economy features. Commands like `/insureinv buy` are disabled. Useful if admins want to hand out charges
  directly via scripts or crates.

---

## 📊 bStats Metrics

To help me understand how the plugin is being used and to plan future updates, InsureInv
uses [bStats](https://bstats.org/plugin/bukkit/InsureInv/29775) to collect anonymous, basic server data (like the Java
version, server software, and player counts). It does not collect any identifying information about your server or your
players.

### How to Disable bStats

If you wish to opt out of data collection, you can safely disable metrics in the `config.yml`:

```yaml
metrics:
  enabled: false
```

You can also opt out globally for your whole server by modifying the bStats config located at
`plugins/bStats/config.yml`.

---

## 🛠️ Building From Source

InsureInv is built using **Gradle** (Kotlin DSL).

1. Ensure you have **Java 21** installed (required by Paper 1.21+).
2. Clone the repository: `git clone https://github.com/callmebirri/InsureInv.git`
3. Enter the specific directory: `cd InsureInv`
4. Run the Gradle build task:
    - On Linux/macOS: `./gradlew build`
    - On Windows: `gradlew.bat build`
5. The compiled plugin jar will be generated inside the `build/libs` folder with a name like
   `insureinv-[version]+[git-commit-hash].jar`.

---

## Contributing

Contributions, issues, and feature requests are welcome!
Feel free to check the [issues page](https://github.com/callmebirri/InsureInv/issues).

---
<div align="center">

Developed with ❤️ by **birri**

</div>
