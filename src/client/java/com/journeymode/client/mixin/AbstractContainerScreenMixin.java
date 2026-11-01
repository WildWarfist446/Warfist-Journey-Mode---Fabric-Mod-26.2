package com.journeymode.client.mixin;

import com.journeymode.client.UnresearchedMarker;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks items in the inventory that have not been researched yet with a small cog. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
	@Inject(method = "extractSlot", at = @At("TAIL"))
	private void journeymode$cog(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		if (UnresearchedMarker.applies(slot.getItem())) {
			UnresearchedMarker.draw(graphics, slot.x, slot.y);
		}
	}
}
