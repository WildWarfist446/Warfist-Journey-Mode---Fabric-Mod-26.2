package com.journeymode.client.mixin;

import com.journeymode.client.InventoryTheme;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Gives the recipe book's category tabs the steampunk look in a Journey Mode world. */
@Mixin(RecipeBookTabButton.class)
public abstract class RecipeBookTabButtonMixin {
	@Redirect(method = "extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite"
					+ "(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private void journeymode$tab(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y,
			int width, int height) {
		if (InventoryTheme.themesRecipeBook()) {
			InventoryTheme.drawRecipeTab(graphics, sprite, x, y, width, height);
		} else {
			graphics.blitSprite(pipeline, sprite, x, y, width, height);
		}
	}
}
