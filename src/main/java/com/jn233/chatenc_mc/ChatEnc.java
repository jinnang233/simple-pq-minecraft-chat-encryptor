package com.jn233.chatenc_mc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.StringArgumentType;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;


public class ChatEnc implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("JNENC");
	private PqEnc encryptor;
	private boolean keypair_initialized = false;
	private static com.jn233.chatenc_mc.ChatHandler chat_handler = new com.jn233.chatenc_mc.ChatHandler();
	public static Configuration configurationScreen = new Configuration();
	public static final String rootCommand = "enc";
	
	
	public void keypair_initialize() {
		if(!keypair_initialized) {
			new Thread() {
				public void run() {
						try {
							MinecraftClient instance = MinecraftClient.getInstance();
							if(Configuration.message_detail)instance.inGameHud.getChatHud().addMessage(Text.translatable("general.jn233_mcchat_enc.ensure_keypair"));
							(new PqEnc()).makesure();
							keypair_initialized=true;
							if(Configuration.message_detail)instance.inGameHud.getChatHud().addMessage(Text.translatable("general.jn233_mcchat_enc.keypair_generated"));
						} catch (Exception e) {
							ChatEnc.LOGGER.error(e.getMessage());
						}
					
				}
			}.start();
		}
}
	
	@Override
	public void onInitialize() {
		MidnightConfig.init("jn233_mcchat_enc", Configuration.class);
		
		encryptor = new PqEnc();
		ArgumentTypeRegistry.registerArgumentType(
				  new Identifier("jn233_mcchat_enc", "enc_receiver"),
				  ReceiverArgumentType.class, ConstantArgumentSerializer.of(ReceiverArgumentType::receiver));
		
		ClientCommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess)->dispatcher.register(ClientCommandManager.literal(rootCommand)
				.then(ClientCommandManager.literal("tell")
				.then(ClientCommandManager.argument("receiver", ReceiverArgumentType.receiver())
				.then(ClientCommandManager.argument("message",StringArgumentType.string())
				.executes(context -> {
					
					final String receiver = ReceiverArgumentType.getReceiver(context,"receiver");
					final String message = StringArgumentType.getString(context,"message");
					ChatHandler.sendEncrypted(encryptor, message, receiver, false);
					return 1;
				}))))));
		ClientCommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess)->dispatcher.register(ClientCommandManager.literal(rootCommand)
				.then(ClientCommandManager.literal("stell")
				.then(ClientCommandManager.argument("receiver", ReceiverArgumentType.receiver())
				.then(ClientCommandManager.argument("message",StringArgumentType.string())
				.executes(context -> {
					final String receiver = ReceiverArgumentType.getReceiver(context,"receiver");
					final String message = StringArgumentType.getString(context,"message");
					if(keypair_initialized) ChatHandler.sendEncrypted(encryptor, message, receiver, true);
					return 1;
				}))))));
		
		ClientCommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess)->dispatcher.register(ClientCommandManager.literal(rootCommand)
				.then(ClientCommandManager.literal("exchange")
				.then(ClientCommandManager.argument("receiver", ReceiverArgumentType.receiver())
				.executes(context -> {
					final String receiver = ReceiverArgumentType.getReceiver(context,"receiver");
					ChatHandler.sendEncrypted(encryptor, "", receiver, false,2);
					return 1;
				})))));
		ClientCommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess)->dispatcher.register(ClientCommandManager.literal(rootCommand)
				.then(ClientCommandManager.literal("etell")
				.then(ClientCommandManager.argument("receiver", ReceiverArgumentType.receiver())
				.then(ClientCommandManager.argument("message",StringArgumentType.string())
				.executes(context -> {
					final String receiver = ReceiverArgumentType.getReceiver(context,"receiver");
					final String message = StringArgumentType.getString(context,"message");
					if(keypair_initialized) ChatHandler.sendEncrypted(encryptor, message, receiver, true,3);
					return 1;
				}))))));
		ClientCommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess)->dispatcher.register(ClientCommandManager.literal(rootCommand)
				.then(ClientCommandManager.literal("showalgs")
				.executes(context -> {
					MinecraftClient instance = MinecraftClient.getInstance();
					instance.inGameHud.getChatHud().addMessage(
							Text.translatable("jn233_mcchat_enc.midnightconfig.kem_alg").append(":")
							.append(PQParser.param.getName()));
					instance.inGameHud.getChatHud().addMessage(
							Text.translatable("jn233_mcchat_enc.midnightconfig.sig_alg").append(":")
							.append(PQParser.sig_param));
					return 1;
				}))));
		
		ClientReceiveMessageEvents.ALLOW_CHAT.register(
				(message, signedMessage, sender, params, receptionTimestamp)->{
					if(keypair_initialized && (!Configuration.silly_match)) return chat_handler.chatProcessWithSession(message.getString(), sender.getName());
					return true;
				}
				);
		ClientReceiveMessageEvents.ALLOW_GAME.register(
				(message,overlay)->{
					if(keypair_initialized && Configuration.silly_match) {
						String content_pattern = Configuration.chat_regex;
						Pattern content_patterner = Pattern.compile(content_pattern);
						Matcher player_message_matcher = content_patterner.matcher(message.getString());
						if(!player_message_matcher.find()) return true;
						return chat_handler.chatProcessWithSession(player_message_matcher.group(2), player_message_matcher.group(1));
					}
					return true;
				}
				);
		ClientPlayConnectionEvents.JOIN.register(
				(handler,sender,client)->{
					keypair_initialize();
					return;
				}
				);
		PKStorageGlass.makeDir(MinecraftClient.getInstance());

	}
}

