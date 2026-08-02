package com.avie29.healthindicator;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class HealthIndicatorClient implements ClientModInitializer {

	public static boolean enabled = true;
	private static KeyBinding toggleKeyBinding;

	@Override
	public void onInitializeClient() {
		toggleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.healthindicator.toggle",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				"category.healthindicator"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKeyBinding.wasPressed()) {
				enabled = !enabled;

				if (client.player != null) {
					if (enabled) {
						client.player.sendMessage(
								Text.translatable("message.healthindicator.enabled").formatted(Formatting.GREEN),
								true
						);
					} else {
						client.player.sendMessage(
								Text.translatable("message.healthindicator.disabled").formatted(Formatting.RED),
								true
						);
					}
				}
			}
		});
	}
}