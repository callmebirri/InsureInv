import java.time.Instant

plugins {
    kotlin("jvm") version "2.4.10"
    id("com.gradleup.shadow") version "9.6.1"
}

val generateGitProperties by tasks.registering {
    group = "build"
    description = "Generates git.properties file with commit information"

    val outputFile = layout.buildDirectory.file("generated/resources/git/git.properties")

    inputs.property("git.head", providers.exec {
        commandLine("git", "rev-parse", "HEAD")
        isIgnoreExitValue = true
    }.standardOutput.asText.map { it.trim() }.orElse("unknown"))

    outputs.file(outputFile)

    doLast {
        fun runGit(command: String): String = providers.exec {
            commandLine("git", *command.split(" ").toTypedArray())
            isIgnoreExitValue = true
        }.standardOutput.asText.map { it.trim() }.getOrElse("unknown")

        val props = mapOf(
            "git.commit.id" to runGit("rev-parse HEAD"),
            "git.commit.id.abbrev" to runGit("rev-parse --short HEAD"),
            "git.commit.message.short" to runGit("log -1 --pretty=%s"),
            "git.commit.time" to runGit("log -1 --pretty=%cI"),
            "git.branch" to runGit("rev-parse --abbrev-ref HEAD"),
            "git.build.time" to Instant.now().toString(),
            "git.dirty" to (runGit("status --porcelain").isNotBlank()).toString()
        )

        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(props.entries.joinToString("\n") { "${it.key}=${it.value}" })
        }
    }
}

val gitCommitShort = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim() }.getOrElse("unknown")

sourceSets {
    main {
        resources {
            srcDir(layout.buildDirectory.dir("generated/resources/git"))
        }
    }
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.helpch.at/releases")
    maven("https://repo.tcoded.com/releases")
    maven("https://repo.rosewooddev.io/repository/public/")
    maven("https://repo.codemc.io/repository/creatorfromhell/")
    mavenCentral()
}

val coroutinesVersion: String by project

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("me.clip:placeholderapi:2.12.2")
    compileOnly("org.black_ixx:playerpoints:3.3.3")
    compileOnly("com.zaxxer:HikariCP:5.1.0")
    compileOnly("net.milkbowl.vault:VaultUnlockedAPI:2.16")
    compileOnly("com.mysql:mysql-connector-j:9.7.0")
    compileOnly("org.xerial:sqlite-jdbc:3.53.2.0")
    compileOnly("com.google.code.gson:gson:2.10.1")

    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    implementation("org.bstats:bstats-bukkit:3.1.0")
    implementation("com.tcoded:FoliaLib:0.5.1")
}

kotlin {
    jvmToolchain(21)
}

tasks.processResources {
    dependsOn(generateGitProperties)

    val props = mapOf(
        "name" to project.name,
        "version" to project.version.toString(),
        "gitCommit" to gitCommitShort,
    )
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("${project.name}-${project.version}+${gitCommitShort}.jar")

    dependencies {
        include(dependency("com.tcoded:FoliaLib"))
        include(dependency("org.bstats:bstats-bukkit"))
        include(dependency("org.bstats:bstats-base"))
    }

    relocate("com.tcoded.folialib", "dev.hqng.insureinv.libs.folialib")
    relocate("org.bstats", "dev.hqng.insureinv.libs.bstats")

    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/MANIFEST.MF")
    exclude("META-INF/NOTICE*")
    exclude("META-INF/versions/**")
    exclude("META-INF/maven/**")
    exclude("META-INF/proguard/**")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.register("printGitInfo") {
    group = "help"
    description = "Prints current git information"
    doLast {
        file(layout.buildDirectory.file("generated/resources/git/git.properties").get().asFile)
            .takeIf { it.exists() }
            ?.readLines()
            ?.forEach { println(it) }
            ?: println("Git properties not generated yet. Run 'build' first.")
    }
}
