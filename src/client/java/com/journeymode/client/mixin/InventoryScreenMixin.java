package com.journeymode.client.mixin;

import com.journeymode.client.InventoryTheme;
import com.journeymode.client.ResearchClientState;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Gives the survival inventory the steampunk look in a Journey Mode world. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
	/** Draws our background where the game blits its inventory picture. */
	@Redirect(method = "extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit"
					+ "(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
	private void journeymode$background(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture, int x, int y,
			float u, float v, int width, int height, int textureWidth, int textureHeight) {
		if (ResearchClientState.journeyEnabled()) {
			InventoryTheme.draw(graphics, (InventoryScreen) (Object) this, x, y, width, height);
		} else {
			graphics.blit(pipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight);
		}
	}

	/** The "Crafting" title is dark grey, which would vanish on the dark plate, so it is drawn in the panels' dim text colour. */
	@ModifyArg(method = "extractLabels(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text"
					+ "(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"),
			index = 4)
	private int journeymode$titleColor(int color) {
		return ResearchClientState.journeyEnabled() ? InventoryTheme.titleColor() : color;
	}
}
