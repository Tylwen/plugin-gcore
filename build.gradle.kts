@file:Suppress("SpellCheckingInspection")

plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.guillaumevdn"
version = "8.49.0"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://libraries.minecraft.net/")
    maven("https://maven.citizensnpcs.co/repo")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.rosewooddev.io/repository/public/")
}

dependencies {
    compileOnly("com.googlecode.json-simple:json-simple:1.1.1")
    compileOnly("org.spigotmc:spigot-api:1.20.5-R0.1-SNAPSHOT")
    compileOnly("com.mojang:authlib:1.5.26")
    compileOnly("net.citizensnpcs:citizens-main:2.0.30-SNAPSHOT") { exclude(group = "*", module = "*") }
    compileOnly("com.comphenix.protocol:ProtocolLib:5.1.0")
    compileOnly("org.black_ixx:playerpoints:3.2.6")
    compileOnly("com.arcaniax:HeadDatabase-API:1.3.1")
    compileOnly("me.clip:placeholderapi:2.11.3")
    compileOnly(files("lib/ChatControl-8.8.0-BETA.jar"))
    compileOnly(files("lib/ChatControl-Free-5.8.9.jar"))
    compileOnly(files("lib/DeluxeChat-1.12.3.jar"))
    compileOnly(files("lib/DeluxeChat-1.15.0.jar"))
    compileOnly(files("lib/MythicMobs-4.11.0-BETA.jar"))
    compileOnly(files("lib/MythicMobs-5.0.2.jar"))
    compileOnly(files("lib/TokenEnchant-18.29.35"))
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    archiveFileName.set("GCore.jar")
}

tasks.register("copyFiles") {
    dependsOn("shadowJar")

    doLast {
        copy {
            from("$buildDir/libs/GCore.jar")
            into("/home/guillaumevdn/Projects/plugins-servers/spigot-1.20.5/plugins")
        }
    }
}
