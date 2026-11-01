package com.journeymode.client.mixin.compat;

import com.journeymode.client.InventoryTheme;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Trinkets draws a grey square behind each of its slots in the inventory, on top of our sunken slots.
 * In a Journey Mode world that square is left out, so those slots look like the rest.
 * Only applies when Trinkets is installed ({@link Pseudo}); if its code differs from the version this
 * was written against (4.1.x), the patch is skipped and the grey squares simply stay.
 */
@Pseudo
@Mixin(targets = "eu.pb4.trinkets.impl.client.TrinketScreenManager")
public abstract class TrinketScreenManagerMixin {
	@Redirect(method = "drawSlotExtrasCommonHead(Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;"
			+ "Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/world/inventory/Slot;II)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite"
					+ "(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"),
			require = 0)
	private static void journeymode$skipSlotSquare(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite,
			int x, int y, int width, int height, int color) {
		if (!InventoryTheme.themesInventory()) {
			graphics.blitSprite(pipeline, sprite, x, y, width, height, color);
		}
	}
}
