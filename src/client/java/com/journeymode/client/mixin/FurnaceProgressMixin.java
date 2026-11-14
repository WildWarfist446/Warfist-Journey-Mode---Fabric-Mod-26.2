package com.journeymode.client.mixin;

import com.journeymode.client.MenuTheme;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Draws the furnaces' flame and progress arrow in brass in a Journey Mode world, in place of the game's orange flame
 * and white arrow, which clash with the steampunk plate (see {@link MenuTheme}).
 */
@Mixin(AbstractFurnaceScreen.class)
public abstract class FurnaceProgressMixin {
	private static final String BLIT_SPRITE = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite"
			+ "(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIII)V";

	/** The flame: the game draws the bottom {@code height} rows of its 14 by 14 sprite, starting at row {@code v}. */
	@Redirect(method = "extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
			at = @At(value = "INVOKE", target = BLIT_SPRITE, ordinal = 0))
	private void journeymode$flame(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int textureWidth,
			int textureHeight, int u, int v, int x, int y, int width, int height) {
		if (MenuTheme.themes(this)) {
			MenuTheme.drawFlameFill(graphics, x, y, v, height);
		} else {
			graphics.blitSprite(pipeline, sprite, textureWidth, textureHeight, u, v, x, y, width, height);
		}
	}

	/** The arrow: the game draws the left {@code width} columns of its 24 by 16 sprite. */
	@Redirect(method = "extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
			at = @At(value = "INVOKE", target = BLIT_SPRITE, ordinal = 1))
	private void journeymode$arrow(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int textureWidth,
			int textureHeight, int u, int v, int x, int y, int width, int height) {
		if (MenuTheme.themes(this)) {
			MenuTheme.drawArrowFill(graphics, x, y, width);
		} else {
			graphics.blitSprite(pipeline, sprite, textureWidth, textureHeight, u, v, x, y, width, height);
		}
	}
}
