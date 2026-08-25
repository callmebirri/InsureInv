pluginManagement {
    val kotlinVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        id("com.gradleup.shadow") version "9.6.1"
    }
}

rootProject.name = "InsureInv"
