package com.journeymode.client.mixin;

import com.journeymode.JourneyMode;
import com.journeymode.client.JourneyToggleButton;

import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the Journey Mode switch under the other options on the world creation screen's Game tab. */
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$GameTab")
public abstract class GameTabMixin {
	@Inject(method = "<init>", at = @At("RETURN"))
	private void journeymode$addToggle(CreateWorldScreen screen, CallbackInfo ci) {
		// A new creation screen starts with Journey Mode off.
		JourneyMode.pendingNewWorld = false;
		GridLayout layout = (GridLayout) ((GridLayoutTab) (Object) this).getLayout();
		// Every option in this tab is a row of its own, so the new one goes in the next row.
		int[] rows = {0};
		layout.visitChildren(child -> rows[0]++);
		layout.addChild(new JourneyToggleButton(210, 20), rows[0], 0, layout.newCellSettings().alignHorizontallyCenter());
	}
}
