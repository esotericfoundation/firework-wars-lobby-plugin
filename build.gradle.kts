import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml
import xyz.jpenilla.resourcefactory.bukkit.bukkitPluginYaml
import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml

plugins {
    kotlin("jvm") version "2.1.0"
    id("com.gradleup.shadow") version "8.3.5"
    id("xyz.jpenilla.resource-factory-paper-convention") version "1.2.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14"
}

// build output location configuration
// both false = build to build/libs
val buildToMain = true
val buildToCore = false

group = "xyz.fireworkwars"
version = "1.1.1"
description = "Lobby features for a Firework Wars server."

val paperApiVersion = "1.21.4"
val corePluginDependencyVersion = "1.4.1"
val targetJavaVersion = 21

// authors
val rolyPolyVole = "rolyPolyVole"
val esotericEnderman = "Esoteric Enderman"

// plugin yml
val pluginName = "FireworkWarsLobby"
val pluginAuthors = listOf(rolyPolyVole, esotericEnderman)
val pluginGithub = "https://github.com/fireworkwars/lobby-plugin"

val mainClassPath = "$group.lobby.FireworkWarsLobbyPlugin"

val corePluginName = "FireworkWarsCore"

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }

    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }

    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.fireworkwars:core-plugin:$corePluginDependencyVersion")

    paperweight.paperDevBundle("$paperApiVersion-R0.1-SNAPSHOT")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        @Suppress("UnstableApiUsage")
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }

    jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}

tasks {
    shadowJar {
        minimize {
            exclude(dependency("org.jetbrains.kotlin:kotlin-reflect"))
        }

        if (buildToMain) {
            destinationDirectory.set(file("../firework-wars-plugin/run/plugins"))
        } else if (buildToCore) {
            destinationDirectory.set(file("../firework-wars-core-plugin/run/plugins"))
        }
    }

    build {
        dependsOn(shadowJar)
    }

    assemble {
        dependsOn(reobfJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }

    compileKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }
}

paperPluginYaml {
    name = pluginName
    description = project.description
    authors = pluginAuthors
    website = pluginGithub

    apiVersion = paperApiVersion

    main = mainClassPath

    dependencies {
        server(corePluginName, PaperPluginYaml.Load.BEFORE, required = true, joinClasspath = true)
    }
}

bukkitPluginYaml {
    load = BukkitPluginYaml.PluginLoadOrder.POSTWORLD
}