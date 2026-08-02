package com.avie29.healthindicator.mixin;

import com.avie29.healthindicator.HealthIndicatorClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class PlayerNameTagMixin<T extends Entity> {

	// Textures for 1.20 / 1.20.1 (Legacy texture atlas)
	private static final Identifier ICONS_TEXTURE = new Identifier("textures/gui/icons.png");

	// Textures for 1.20.2+ (GUI atlas sprites)
	private static final Identifier GUI_ATLAS = new Identifier("textures/atlas/gui.png");
	private static final Identifier HEART_CONTAINER = new Identifier("hud/heart/container");
	private static final Identifier HEART_FULL = new Identifier("hud/heart/full");
	private static final Identifier HEART_HALF = new Identifier("hud/heart/half");
	private static final Identifier HEART_ABSORB_FULL = new Identifier("hud/heart/absorbing_full");
	private static final Identifier HEART_ABSORB_HALF = new Identifier("hud/heart/absorbing_half");

	@Inject(
			method = "renderLabelIfPresent",
			at = @At("RETURN")
	)
	private void renderHealthHearts(T entity, Text text, MatrixStack matrices,
	                                VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {

		// Cancel rendering if mod is disabled via hotkey
		if (!HealthIndicatorClient.enabled) return;

		if (!(entity instanceof AbstractClientPlayerEntity player)) return;
		if (player == MinecraftClient.getInstance().player) return;

		MinecraftClient client = MinecraftClient.getInstance();

		int health = MathHelper.ceil(player.getHealth());
		int maxHealth = MathHelper.ceil(player.getMaxHealth());
		int absorption = MathHelper.ceil(player.getAbsorptionAmount());

		int maxBaseHearts = MathHelper.ceil(maxHealth / 2.0F);
		int absorptionHearts = MathHelper.ceil(absorption / 2.0F);
		int totalHeartsToDisplay = Math.min(maxBaseHearts + absorptionHearts, 10);

		boolean isSneaking = player.isSneaking() || player.isInSneakingPose();

		matrices.push();

		double yOffset = player.getHeight() + 0.85D;
		if (isSneaking) {
			yOffset -= 0.25D;
		}

		matrices.translate(0.0D, yOffset, 0.0D);
		matrices.multiply(client.getEntityRenderDispatcher().getRotation());

		float scale = 0.02F;
		matrices.scale(-scale, -scale, scale);

		Matrix4f matrix = matrices.peek().getPositionMatrix();

		int heartSpacing = 8;
		int heartWidth = 9;

		int totalWidth = ((totalHeartsToDisplay - 1) * heartSpacing) + heartWidth;
		int startX = -totalWidth / 2;

		// Check whether we are running 1.20.2+ (GuiAtlasManager exists)
		boolean isModernVersion = isModernAtlasSupported(client);

		if (isModernVersion) {
			// --- LOGIC FOR 1.20.2+ ---
			Sprite containerSprite = client.getGuiAtlasManager().getSprite(HEART_CONTAINER);
			Sprite fullSprite = client.getGuiAtlasManager().getSprite(HEART_FULL);
			Sprite halfSprite = client.getGuiAtlasManager().getSprite(HEART_HALF);
			Sprite absorbFullSprite = client.getGuiAtlasManager().getSprite(HEART_ABSORB_FULL);
			Sprite absorbHalfSprite = client.getGuiAtlasManager().getSprite(HEART_ABSORB_HALF);

			if (!isSneaking) {
				VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getTextSeeThrough(GUI_ATLAS));
				renderModernHearts(matrix, buffer, startX, heartSpacing, totalHeartsToDisplay, maxBaseHearts, health, absorption, containerSprite, fullSprite, halfSprite, absorbFullSprite, absorbHalfSprite, 64);
			}
			VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getText(GUI_ATLAS));
			renderModernHearts(matrix, buffer, startX, heartSpacing, totalHeartsToDisplay, maxBaseHearts, health, absorption, containerSprite, fullSprite, halfSprite, absorbFullSprite, absorbHalfSprite, 255);

		} else {
			// --- LEGACY LOGIC FOR 1.20 & 1.20.1 (sampled from icons.png) ---
			if (!isSneaking) {
				VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getTextSeeThrough(ICONS_TEXTURE));
				renderLegacyHearts(matrix, buffer, startX, heartSpacing, totalHeartsToDisplay, maxBaseHearts, health, absorption, 64);
			}
			VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getText(ICONS_TEXTURE));
			renderLegacyHearts(matrix, buffer, startX, heartSpacing, totalHeartsToDisplay, maxBaseHearts, health, absorption, 255);
		}

		matrices.pop();
	}

	private boolean isModernAtlasSupported(MinecraftClient client) {
		try {
			return client.getGuiAtlasManager() != null;
		} catch (NoSuchMethodError | Exception e) {
			return false;
		}
	}

	// Rendering for MC 1.20.2+ (Sprites)
	private void renderModernHearts(Matrix4f matrix, VertexConsumer buffer, int startX, int heartSpacing,
	                                int totalHeartsToDisplay, int maxBaseHearts, int health, int absorption,
	                                Sprite containerSprite, Sprite fullSprite, Sprite halfSprite,
	                                Sprite absorbFullSprite, Sprite absorbHalfSprite, int alpha) {
		for (int i = 0; i < totalHeartsToDisplay; i++) {
			int x = startX + (i * heartSpacing);
			int heartValue = (i + 1) * 2;

			drawSprite(matrix, buffer, containerSprite.getMinU(), containerSprite.getMaxU(), containerSprite.getMinV(), containerSprite.getMaxV(), x, 0, alpha);

			if (i < maxBaseHearts) {
				if (health >= heartValue) {
					drawSprite(matrix, buffer, fullSprite.getMinU(), fullSprite.getMaxU(), fullSprite.getMinV(), fullSprite.getMaxV(), x, 0, alpha);
				} else if (health == heartValue - 1) {
					drawSprite(matrix, buffer, halfSprite.getMinU(), halfSprite.getMaxU(), halfSprite.getMinV(), halfSprite.getMaxV(), x, 0, alpha);
				}
			} else {
				int absorbValue = (i - maxBaseHearts + 1) * 2;
				if (absorption >= absorbValue) {
					drawSprite(matrix, buffer, absorbFullSprite.getMinU(), absorbFullSprite.getMaxU(), absorbFullSprite.getMinV(), absorbFullSprite.getMaxV(), x, 0, alpha);
				} else if (absorption == absorbValue - 1) {
					drawSprite(matrix, buffer, absorbHalfSprite.getMinU(), absorbHalfSprite.getMaxU(), absorbHalfSprite.getMinV(), absorbHalfSprite.getMaxV(), x, 0, alpha);
				}
			}
		}
	}

	// Rendering for MC 1.20 & 1.20.1 (Exact UV mapping from icons.png)
	private void renderLegacyHearts(Matrix4f matrix, VertexConsumer buffer, int startX, int heartSpacing,
	                                int totalHeartsToDisplay, int maxBaseHearts, int health, int absorption, int alpha) {

		// UV scale factor for icons.png (256x256 texture sheet)
		float uTex = 1.0F / 256.0F;
		float vTex = 1.0F / 256.0F;

		for (int i = 0; i < totalHeartsToDisplay; i++) {
			int x = startX + (i * heartSpacing);
			int heartValue = (i + 1) * 2;

			// Heart container UV (16, 0)
			drawSprite(matrix, buffer, 16 * uTex, 25 * uTex, 0 * vTex, 9 * vTex, x, 0, alpha);

			if (i < maxBaseHearts) {
				if (health >= heartValue) {
					// Full heart UV (52, 0)
					drawSprite(matrix, buffer, 52 * uTex, 61 * uTex, 0 * vTex, 9 * vTex, x, 0, alpha);
				} else if (health == heartValue - 1) {
					// Half heart UV (61, 0)
					drawSprite(matrix, buffer, 61 * uTex, 70 * uTex, 0 * vTex, 9 * vTex, x, 0, alpha);
				}
			} else {
				int absorbValue = (i - maxBaseHearts + 1) * 2;
				if (absorption >= absorbValue) {
					// Full absorption heart UV (160, 0)
					drawSprite(matrix, buffer, 160 * uTex, 169 * uTex, 0 * vTex, 9 * vTex, x, 0, alpha);
				} else if (absorption == absorbValue - 1) {
					// Half absorption heart UV (169, 0)
					drawSprite(matrix, buffer, 169 * uTex, 178 * uTex, 0 * vTex, 9 * vTex, x, 0, alpha);
				}
			}
		}
	}

	private void drawSprite(Matrix4f matrix, VertexConsumer buffer, float uMin, float uMax, float vMin, float vMax, float x, float y, int alpha) {
		float size = 9.0F;
		int fullLight = 0xF000F0;

		buffer.vertex(matrix, x, y + size, 0.0F).color(255, 255, 255, alpha).texture(uMin, vMax).light(fullLight).next();
		buffer.vertex(matrix, x + size, y + size, 0.0F).color(255, 255, 255, alpha).texture(uMax, vMax).light(fullLight).next();
		buffer.vertex(matrix, x + size, y, 0.0F).color(255, 255, 255, alpha).texture(uMax, vMin).light(fullLight).next();
		buffer.vertex(matrix, x, y, 0.0F).color(255, 255, 255, alpha).texture(uMin, vMin).light(fullLight).next();
	}
}