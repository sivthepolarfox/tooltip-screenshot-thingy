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
        this.translation = "config.toolshot.saveFile"
    }

    var scale by int(4) {
        this.translation = "config.toolshot.scale"
    }
}