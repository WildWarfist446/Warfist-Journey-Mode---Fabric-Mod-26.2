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
 * Both of Trinkets' slot layouts (the legacy one and the sidebar) draw a grey square behind each of
 * their slots in {@code drawBackground}. In a Journey Mode world that square is drawn as a sunken slot instead.
 * Only applies when Trinkets is installed ({@link Pseudo}); if its code differs from the version this was
 * written against (4.1.x) the patch is skipped.
 */
@Pseudo
@Mixin(targets = {
		"eu.pb4.trinkets.impl.client.slot.legacy.LegacyTrinketScreenManager",
		"eu.pb4.trinkets.impl.client.slot.sidebar.SidebarTrinketScreenManager"})
public abstract class TrinketsBackendMixin {
	@Redirect(method = "drawBackground(Leu/pb4/trinkets/impl/client/TrinketScreen;Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite"
					+ "(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"),
			require = 0)
	private void journeymode$sunkenSlot(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y,
			int width, int height) {
		if (InventoryTheme.themesInventory() && sprite.getPath().equals("container/slot")) {
			InventoryTheme.drawSunkenSlot(graphics, x, y, width, height);
		} else {
			graphics.blitSprite(pipeline, sprite, x, y, width, height);
		}
	}
}
