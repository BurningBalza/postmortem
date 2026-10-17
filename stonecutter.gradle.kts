plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.2"

stonecutter parameters {
    val loader = node.branch.id

    // Lets stonecutter.properties.toml sections like [fabric."26.2"] apply to this node.
    properties {
        tags(current.project, loader)
    }
}
