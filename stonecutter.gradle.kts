plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.2-fabric"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    // Lets stonecutter.properties.toml sections like [fabric."26.2"] apply to this node.
    properties {
        tags(version, loader)
    }

    // Enables `//? if fabric {` / `//? if neoforge {` / `//? if forge {` in sources.
    constants {
        match(loader, "fabric", "neoforge", "forge")
    }
}
