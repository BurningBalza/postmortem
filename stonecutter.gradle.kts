plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21"

stonecutter parameters {
    val loader = node.branch.id

    // Lets stonecutter.properties.toml sections like [fabric."26.2"] apply to this node.
    properties {
        tags(current.project, loader)
    }

    // Plain renames, applied in the range given and in reverse outside it. Sources are stored in their vcsVersion
    // (newest) form, so the text a rule produces for the other side must never already appear in the stored sources;
    // GameProfile -> NameAndId, for one, would have rewritten GameProfileArgument.
    replacements {
        string(current.parsed >= "26.3") {
            replace(".mulPose(", ".rotate(")
        }
        string(current.parsed < "26.1") {
            replace("net.minecraft.util.LightCoordsUtil", "net.minecraft.client.renderer.LightTexture")
            replace("LightCoordsUtil", "LightTexture")
            replace("GuiGraphicsExtractor", "GuiGraphics")
            replace(".centeredText(", ".drawCenteredString(")
        }
        string(current.parsed < "1.21.11") {
            replace("net.minecraft.resources.Identifier", "net.minecraft.resources.ResourceLocation")
            replace("Identifier", "ResourceLocation")
            // Only ResourceKey's accessor; a bare ".location()" also names the interact packet's hit position.
            replace(").identifier()", ").location()")
            replace("key.identifier()", "key.location()")
            replace("net.minecraft.world.level.gamerules.GameRules", "net.minecraft.world.level.GameRules")
            replace(".get(GameRules.KEEP_INVENTORY)", ".getBoolean(GameRules.RULE_KEEPINVENTORY)")
            replace("net.minecraft.client.renderer.rendertype.RenderType;", "net.minecraft.client.renderer.RenderType;")
            replace("RenderTypes.", "RenderType.")
            replace("net.minecraft.client.model.player.PlayerModel", "net.minecraft.client.model.PlayerModel")
        }
        string(current.parsed < "1.21.4") {
            replace("headEquipment", "headItem")
            replace("chestEquipment", "chestItem")
            replace("legsEquipment", "legsItem")
            replace("feetEquipment", "feetItem")
        }
        string(current.parsed < "1.21.9") {
            replace("AvatarRenderState", "PlayerRenderState")
        }

    }
}
