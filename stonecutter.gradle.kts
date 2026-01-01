plugins {
    id("dev.kikugie.stonecutter")
}
stonecutter active "1.21.10"

stonecutter parameters {
    swaps["mod_version"] = "\"" + property("version") + "\";"
    swaps["minecraft"] = "\"" + node.metadata.version + "\";"
}