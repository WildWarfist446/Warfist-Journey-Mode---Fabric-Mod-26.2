package com.journeymode.client;

import com.journeymode.JourneyMode;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The Journey Mode on/off switch on the world creation screen. It looks like the other buttons
 * there ("Allow Cheats: OFF"), with the research icon at its left. It only sets
 * {@link JourneyMode#pendingNewWorld}; the world takes the choice when it is created and it
 * cannot be changed afterwards.
 */
public final class JourneyToggleButton extends AbstractButton {
	private static final Identifier ICON = JourneyMode.id("textures/gui/research_icon.png");

	public JourneyToggleButton(int width, int height) {
		super(0, 0, width, height, label(JourneyMode.pendingNewWorld));
		setTooltip(Tooltip.create(Component.translatable("gui.journeymode.create.journey_mode.tooltip")));
	}

	private static Component label(boolean on) {
		return Component.translatable("gui.journeymode.create.journey_mode",
				Component.translatable(on ? "options.on" : "options.off"));
	}

	@Override
	public void onPress(InputWithModifiers input) {
		JourneyMode.pendingNewWorld = !JourneyMode.pendingNewWorld;
		setMessage(label(JourneyMode.pendingNewWorld));
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		extractDefaultSprite(graphics);
		// The research icon: a 10 x 12 picture at (11, 8) on a 32 x 32 canvas, near the button's left edge.
		graphics.blit(RenderPipelines.GUI_TEXTURED, ICON, getX() + 6, getY() + (getHeight() - 12) / 2, 11, 8, 10, 12, 10, 12, 32, 32);
		extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}
}
