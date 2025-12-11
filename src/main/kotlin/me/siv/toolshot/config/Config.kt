package me.siv.toolshot.config

import com.teamresourceful.resourcefulconfigkt.api.ConfigKt

object Config : ConfigKt("toolshot/config") {
    override val name = Literal("toolshot")

    init {
        separator {
            title = "Mrrow"
            description = "Mrrp"
        }
    }

    var saveFile by boolean(true) {
        translation = "config.toolshot.saveFile"
    }
}