package com.jn233.chatenc_mc.mixin;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jn233.chatenc_mc.ChatEnc;
import com.jn233.chatenc_mc.PqEnc;

import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

@Mixin(ChatHud.class)
public class DecMixin {
	private static final Logger LOGGER = LoggerFactory.getLogger("JNENC");
	private static com.jn233.chatenc_mc.ChatHandler chat_handler = new com.jn233.chatenc_mc.ChatHandler();
	private boolean selfAdded = false;
	// Mixin
	@Inject(method = "addMessage(Lnet/minecraft/text/Text;I)V", at = @At("HEAD"))
    public void addMessage(Text text, int messageId, CallbackInfo info) {
		String message = text.getString();
		LOGGER.info(message);
		if(!message.endsWith(".addMessage"))
			chat_handler.chatProcessWithSession(message,info);

		if(!selfAdded) {
			new Thread() {
				public void run() {
					try {
						MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.translatable("general.jn233_mcchat_enc.ensure_keypair"));
						(new PqEnc()).makesure();
						MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.translatable("general.jn233_mcchat_enc.keypair_generated"));
					} catch (Exception e) {
						ChatEnc.LOGGER.error(e.getMessage());
					}
				}
			
			}.start();
			selfAdded=true;
		}
		
	}
}
