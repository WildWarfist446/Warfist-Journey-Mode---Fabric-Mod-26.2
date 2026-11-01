package com.journeymode.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Where an inventory-style screen sits, which moves when the recipe book opens. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int journeymode$leftPos();

	@Accessor("topPos")
	int journeymode$topPos();

	@Accessor("imageWidth")
	int journeymode$imageWidth();

	@Accessor("imageHeight")
	int journeymode$imageHeight();

	@Accessor("hoveredSlot")
	Slot journeymode$hoveredSlot();
}