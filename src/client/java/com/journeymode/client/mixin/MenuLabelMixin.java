package com.journeymode.client.mixin;

import com.journeymode.client.InventoryTheme;
import com.journeymode.client.MenuTheme;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Menu titles are dark grey, which would vanish on the dark plate, so the themed menus draw them in the panels' dim text colour. */
@Mixin(AbstractContainerScreen.class)
public abstract class MenuLabelMixin {
	@ModifyArg(method = "extractLabels(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text"
					+ "(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"),
			index = 4)
	private int journeymode$labelColor(int color) {
		return MenuTheme.themes(this) ? InventoryTheme.titleColor() : color;
	}
}
