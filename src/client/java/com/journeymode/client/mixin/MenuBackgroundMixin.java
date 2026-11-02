package com.journeymode.client.mixin;

import com.journeymode.client.MenuTheme;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.CrafterScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Gives the game's crafting menus the steampunk look in a Journey Mode world, by drawing our plate where each
 * of them draws its background picture. (The anvil and smithing table draw theirs through {@link ItemCombinerScreen}.)
 */
@Mixin({CraftingScreen.class, AbstractFurnaceScreen.class, ItemCombinerScreen.class, StonecutterScreen.class, LoomScreen.class,
		CartographyTableScreen.class, GrindstoneScreen.class, EnchantmentScreen.class, BrewingStandScreen.class, CrafterScreen.class})
public abstract class MenuBackgroundMixin {
	@Redirect(method = "extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit"
					+ "(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
	private void journeymode$background(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture, int x, int y,
			float u, float v, int width, int height, int textureWidth, int textureHeight) {
		if (MenuTheme.themes(this) && texture.getPath().startsWith("textures/gui/container/")) {
			MenuTheme.draw(graphics, (AbstractContainerScreen<?>) (Object) this, x, y, width, height);
		} else {
			graphics.blit(pipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight);
		}
	}
}
