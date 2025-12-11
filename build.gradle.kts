import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
    idea
    kotlin("jvm") version "2.2.21"
    alias(libs.plugins.loom)
}

repositories {
    maven("https://maven.teamresourceful.com/repository/maven-public/")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://maven.terraformersmc.com/releases/")
}

dependencies {
    minecraft(libs.minecraft)
    mappings(loom.officialMojangMappings())

    modImplementation(libs.fabricLoader)
    modImplementation(libs.fabricApi)
    modImplementation(libs.fabricKt)

    modImplementation(libs.devauth)

    modImplementation(libs.resourcefulconfig)
    modImplementation(libs.resourcefulconfigkt)
    include(libs.resourcefulconfigkt)

    modImplementation(libs.modmenu)

    compileOnly(libs.objc)
}

loom {
    accessWidenerPath = rootProject.file("src/main/resources/tooltipscreenshot.accesswidener")

    runs {
        getByName("client") {
            property("devauth.configDir", rootProject.file(".devauth").absolutePath)
        }
    }
}

tasks {
    processResources {
        inputs.property("version", project.version)
        inputs.property("minecraft_version", libs.versions.minecraft.get())
        inputs.property("loader_version", libs.versions.fabricLoader.get())

        filesMatching("fabric.mod.json") {
            expand(
                "version" to project.version,
                "loader_version" to libs.versions.fabricLoader.get(),
                "minecraft_version" to libs.versions.minecraft.get(),
            )
        }
    }

    jar {
        from("LICENSE")
    }

    compileKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }

}

java {
    withSourcesJar()
}

idea {
    module {
        excludeDirs.add(file("run"))
    }
}