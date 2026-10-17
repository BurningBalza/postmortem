plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.1"

stonecutter parameters {
    val loader = node.branch.id

    // Lets stonecutter.properties.toml sections like [fabric."26.2"] apply to this node.
    properties {
        tags(current.project, loader)
    }

    // Plain class renames. Sources are stored in their 26.2 form, so each replacement maps the 26.x name
    // back to the older one.
    replacements {
        string(current.parsed < "26.1") {
            replace("net.minecraft.util.LightCoordsUtil", "net.minecraft.client.renderer.LightTexture")
            replace("LightCoordsUtil", "LightTexture")
            replace("GuiGraphicsExtractor", "GuiGraphics")
            replace(".centeredText(", ".drawCenteredString(")
        }
    }
}
