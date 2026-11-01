package com.journeymode.client.mixin;

import com.journeymode.client.InventoryTheme;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Gives the recipe book beside the survival inventory the steampunk look in a Journey Mode world. */
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {
	/** Draws our background where the game blits the recipe book's picture. */
	@Redirect(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit"
					+ "(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
	private void journeymode$background(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture, int x, int y,
			float u, float v, int width, int height, int textureWidth, int textureHeight) {
		if (InventoryTheme.themesRecipeBook()) {
			InventoryTheme.drawRecipeBook(graphics, x, y, width, height);
		} else {
			graphics.blit(pipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight);
		}
	}
}
