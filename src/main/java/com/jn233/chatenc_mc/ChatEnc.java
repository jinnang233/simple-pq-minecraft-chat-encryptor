package com.jn233.chatenc_mc;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.Chat;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.impl.networking.PacketCallbackListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.ClientCommonPacketListener;
import net.minecraft.network.listener.ClientPacketListener;
import net.minecraft.text.Text;


public class ChatEnc implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("JNENC");
	private PqEnc encryptor;
	private static com.jn233.chatenc_mc.ChatHandler chat_handler = new com.jn233.chatenc_mc.ChatHandler();
	public static EncryptorConfigurationScreen configurationScreen = new EncryptorConfigurationScreen();
	
	@Override
	public void onInitialize() {
		
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
		encryptor = new PqEnc();
		ClientCommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess)->dispatcher.register(ClientCommandManager.literal("einit")
				.executes(context -> {
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
					return 1;
				})));
		ClientCommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess)->dispatcher.register(ClientCommandManager.literal("enc")
				.then(ClientCommandManager.literal("tell")
				.then(ClientCommandManager.argument("playername", StringArgumentType.string())
				.then(ClientCommandManager.argument("message",StringArgumentType.string())
				.executes(context -> {
					final String playername = StringArgumentType.getString(context,"playername");
					final String message = StringArgumentType.getString(context,"message");
					ChatHandler.sendEncrypted(encryptor, message, playername, false);
					return 1;
				}))))));
		ClientCommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess)->dispatcher.register(ClientCommandManager.literal("enc")
				.then(ClientCommandManager.literal("stell")
				.then(ClientCommandManager.argument("playername", StringArgumentType.string())
				.then(ClientCommandManager.argument("message",StringArgumentType.string())
				.executes(context -> {
					final String playername = StringArgumentType.getString(context,"playername");
					final String message = StringArgumentType.getString(context,"message");
					ChatHandler.sendEncrypted(encryptor, message, playername, true);
					return 1;
				}))))));
		
		ClientReceiveMessageEvents.ALLOW_CHAT.register(
				(message, signedMessage, sender, params, receptionTimestamp)->{
					return chat_handler.chatProcessWithSession(message.getString(), sender.getName());
					
				}
				);
	}
}

