package com.jn233.chatenc_mc;

import java.io.IOException;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.api.ModInitializer;


import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;



import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.server.command.CommandManager;


public class ChatEnc implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("JNENC");
	private PqEnc encryptor;
	public static EncryptorConfigurationScreen configurationScreen = new EncryptorConfigurationScreen();
	
	@Override
	public void onInitialize() {
		// Message handler listener initialization
		// EncReceiverHandler.init();
		// LOGGER.info("Registering command events");
		
		KeyBinding keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.jn233_mcchat_enc.settings",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_J,
				"category.jn233_mcchat_enc.chatkeybind"
				));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while(keyBinding.wasPressed()) {
				MinecraftClient instance = MinecraftClient.getInstance();
				MinecraftClient.getInstance().setScreen(ChatEnc.configurationScreen.makeScreen(instance.currentScreen));;
				
			}
		});
	}
}

