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

    // Plain renames. Sources are stored in their 26.2 form, so each replacement maps the 26.x text back to the
    // older one. Stonecutter runs them in reverse on the versions outside the range, so the older text must never
    // appear in the 26.2 sources (which rules out names like GameProfile).
    replacements {
        string(current.parsed < "26.1") {
            replace("net.minecraft.util.LightCoordsUtil", "net.minecraft.client.renderer.LightTexture")
            replace("LightCoordsUtil", "LightTexture")
            replace("GuiGraphicsExtractor", "GuiGraphics")
            replace(".centeredText(", ".drawCenteredString(")
        }
        string(current.parsed < "1.21.11") {
            replace("net.minecraft.resources.Identifier", "net.minecraft.resources.ResourceLocation")
            replace("Identifier", "ResourceLocation")
            replace(".identifier()", ".location()")
            replace("net.minecraft.world.level.gamerules.GameRules", "net.minecraft.world.level.GameRules")
            replace(".get(GameRules.KEEP_INVENTORY)", ".getBoolean(GameRules.RULE_KEEPINVENTORY)")
            replace("net.minecraft.client.renderer.rendertype.RenderType;", "net.minecraft.client.renderer.RenderType;")
            replace("RenderTypes.", "RenderType.")
        }
        string(current.parsed < "1.21.9") {
            replace("net.minecraft.client.model.player.PlayerModel", "net.minecraft.client.model.PlayerModel")
            replace("AvatarRenderState", "PlayerRenderState")
        }

    }
}
