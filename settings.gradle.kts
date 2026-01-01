
rootProject.name = "Toolshot"

pluginManagement {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/snapshots")
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.8-beta.2"
}

val versions = listOf("1.21.10", "1.21.8")

stonecutter {
    create(rootProject) {
        versions(versions)
        vcsVersion = versions.first()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        versions.forEach { version ->
            val versionName = version.replace('.', '_')
            create("libs${versionName.replace("_", "")}") {
                from(
                    files(
                        rootProject.projectDir.resolve("gradle/$versionName.versions.toml")
                    )
                )
            }
        }
    }
}
