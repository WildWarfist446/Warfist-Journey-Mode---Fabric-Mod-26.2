package com.journeymode.client.mixin;

import com.journeymode.client.InventoryTheme;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Gives the recipe boxes in the recipe book the steampunk look in a Journey Mode world. */
@Mixin(RecipeButton.class)
public abstract class RecipeButtonMixin {
	@Redirect(method = "extractWidgetRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite"
					+ "(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private void journeymode$slot(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y,
			int width, int height) {
		if (InventoryTheme.themesRecipeBook()) {
			InventoryTheme.drawRecipeSlot(graphics, sprite, x, y, width, height);
		} else {
			graphics.blitSprite(pipeline, sprite, x, y, width, height);
		}
	}
}
