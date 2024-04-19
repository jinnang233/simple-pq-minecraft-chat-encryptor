package com.jn233.chatenc_mc;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;

import java.io.IOException; 
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


public class ChatHandler {
	private PqEnc encryptor = new PqEnc();
	
	
	private String session_pattern = "\\[\\+\\=(.*)\\]\\:(.*)"; 
	private String session_end_pattern = "\\[\\$\\=(.*)\\]"; 
	

	private Pattern session_end_pattern_patterner = Pattern.compile(session_end_pattern);
	
	private Pattern session_pattern_patterner = Pattern.compile(session_pattern);
	

	private HashMap<String,String> sessionMap = new HashMap<String,String>();

	private static final Logger LOGGER = LoggerFactory.getLogger("JNChatHandler");
	

	public void printFailure() {
		MinecraftClient instance = MinecraftClient.getInstance();
		instance.inGameHud.getChatHud().addMessage(Text.translatable("general.jn233_mcchat_enc.message_processing_failed"));
	}
	public void sessionProcess(Matcher sessionMatcher) {
		// TODO Process message.
		if(sessionMatcher.groupCount()!=2) {return;}
		LOGGER.info("Session match!"); 
		

		String sessionId = sessionMatcher.group(1); 
		String content = sessionMatcher.group(2);	
		
		sessionMap.put(sessionId, sessionMap.containsKey(sessionId)?sessionMap.get(sessionId)+content:content);
		
	}
	public void sessionEndProcess(Matcher sessionEndMatcher,String playerName) {
		if(sessionEndMatcher.groupCount()!=1) {return;}
		LOGGER.info("Session end match!");
		
		String sessionId = sessionEndMatcher.group(1);
		
		if(!sessionMap.containsKey(sessionId)) return;
		String content = (String) sessionMap.get(sessionId);
		chatProcess(content,playerName); 

		// Delete processed session
		sessionMap.remove(sessionId);
	}
	public boolean chatProcessWithSession(String message,String playerName) {
		// TODO Try to match the message using regular expression, if the match is successful process the encrypted message

		MinecraftClient instance = MinecraftClient.getInstance();		
		
		Matcher session_end_pattern_matcher = session_end_pattern_patterner.matcher(message);
		
		Matcher session_pattern_matcher = session_pattern_patterner.matcher(message);
		
		// LOGGER.info("player:" + player_message_matcher.group(2).strip());
		
		if(session_pattern_matcher.find()) { 
			sessionProcess(session_pattern_matcher);
			//No echo
			if(!ChatEnc.configurationScreen.encrypted_echo)return false;
		}else if(session_end_pattern_matcher.find()) { 
			sessionEndProcess(session_end_pattern_matcher,playerName);
			if(!ChatEnc.configurationScreen.encrypted_echo)return false;
		
		}
		return true;
	}

	public void chatProcess(String message) {
		
		// TODO This function is used to decode the base64-encoded content and pass it to the encryptor for decryption and display.
		chatProcess(message,(Text.translatable("general.jn233_mcchat_enc.stranger")).toString());
	}
	public void chatProcess(String message,String sender) {
		
		// Get the game instance and initialize variables
		MinecraftClient instance = MinecraftClient.getInstance();		
		byte[] data;
		byte[] chat;
		SigData sigdata = null;
		
		
		// Try to Base64 decode the data, exit the method if decryption fails
		try {
			data =Base64.getDecoder().decode(message);
		}catch(IllegalArgumentException e) {
			e.printStackTrace();
			return;
		}
		
		
		if(data==null) return;
		
		// Get the private key, if the private key is empty (failed to obtain) exit method
		byte[] sk = PKStorageGlass.fetch(instance);
		if(sk == null){return;}
		
		// Try to decrypt the packet
		encryptionDataPack datapack = PQParser.unpack(Base64.getDecoder().decode(message));
		
		// Determine the length of the signature and try to verify the signature if it is not empty
		if(datapack.signature.length!=0) {
			byte[] sig_pk = PKStorageGlass.get(instance, sender, true);
			sigdata = encryptor.decrypt_and_verify(message, sk, instance.player.getName().getString(), sig_pk);
			chat=sigdata.data;
		}else {
			chat = encryptor.simple_KEM_decrypt(message, sk, instance.player.getName().getString());
		}
		
		
		// Try to decrypt using encryptor
		

		if(chat==null && (!instance.player.getName().getString().equals(sender))) {printFailure();return;}
		
		String result=new String(chat);
		// Show in game
		String postfix="general.jn233_mcchat_enc.nosignature";
		if(sigdata!=null) {
			if(sigdata.is_valid) {
				postfix="general.jn233_mcchat_enc.valid";
			}else {
				postfix="general.jn233_mcchat_enc.invalid";
			}
		}
		
		// Output message
		Text output_text = Text.literal(sender)
				.append(Text.literal("["))
				.append(Text.translatable(postfix))
				.append(Text.literal("]"))
				.append(Text.literal(" "))
				.append(Text.translatable("general.jn233_mcchat_enc.encrypt_tip"))
				.append(Text.literal(":" + result));
				instance.inGameHud.getChatHud().addMessage(output_text);
	}
	
	public static void cutSend(String message,int length, int message_delay) {
		UUID sessionUid = UUID.randomUUID();
		MessageSendingProgress progress = new MessageSendingProgress();
		progress.setDelay(message_delay);
		int total = (int)Math.ceil(message.length()/length)+1;
		progress.setTotal(total);
		for(int i=0;i<message.length();i+=length) {
			progress.setCurrent(i/length);
			progress.setSender(MinecraftClient.getInstance().player.getName().toString());
			progress.setSessionId(sessionUid.toString());
			String message_cuted = "[+=" + sessionUid.toString() + "]:" + message.substring(i,i+length>message.length()?message.length():i+length);

			MinecraftClient.getInstance().getNetworkHandler().sendChatMessage(message_cuted);
			//MinecraftClient.getInstance().getNetworkHandler().sendPacket(new ChatMessageC2SPacket(message_cuted));
			try {
				Thread.sleep(message_delay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try {
				progress.call();
			} catch (Exception e) {}
		}
		String message_ending = "[$=" + sessionUid.toString()  + "]";
		MinecraftClient.getInstance().getNetworkHandler().sendChatMessage(message_ending);
		//MinecraftClient.getInstance().getNetworkHandler().sendPacket(new ChatMessageC2SPacket(message_ending));
		progress.setCurrent(total);
		try {
			progress.call();
		} catch (Exception e) {}
	}
	
	public static void cutSend(String message,int length){
		cutSend(message,length,0);
	}
	
	public static boolean sendEncrypted(PqEnc encryptor,String message, String playerName,boolean withSignature) {
			// TODO Encrypt the message and send it to the specified person
			boolean playerFound = false;
			MinecraftClient instance = MinecraftClient.getInstance();
			instance.inGameHud.getChatHud().addToMessageHistory(message);
			if(!PqEnc.privLoaded) { 
				instance.inGameHud.getChatHud().addMessage(Text.translatable("general.jn233_mcchat_enc.private_key_not_available_yet"));
				
				return false;
				}
			
			try {
				byte[] pk = PKStorageGlass.get(instance, playerName);
				byte[] sig_sk = PKStorageGlass.fetch(instance, true);
				if(pk!=null) {
					if(withSignature) {
						message = encryptor.encrypt_and_sign(message.getBytes(), pk, playerName, sig_sk);
						
					}else {
						message = encryptor.simple_KEM_encrypt(message.getBytes(), pk, playerName);
					}
					playerFound = true;
					instance.inGameHud.getChatHud().addMessage(Text.translatable("general.jn233_mcchat_enc.found").append(Text.literal(" "+playerName+" ")).append(Text.translatable("general.jn233_mcchat_enc.public_key")));
				}else {
					message="";
					instance.inGameHud.getChatHud().addMessage(Text.translatable("general.jn233_mcchat_enc.notfound").append(Text.literal(" "+playerName+" ")).append(Text.translatable("general.jn233_mcchat_enc.public_key")));
				}
				
			} catch (IOException e) {
				e.printStackTrace();
				instance.inGameHud.getChatHud().addMessage(Text.literal(e.toString()));
				return false;
				
			}

			// Send on batch
			if(playerFound) {
				CutSendImplements cutSend = new CutSendImplements();
				cutSend.setCutLimit(ChatEnc.configurationScreen.cut_limit);
				cutSend.setMessage(message);
				cutSend.setDelay(ChatEnc.configurationScreen.message_delay);
				(new Thread(cutSend)).start();
			}

			
			return playerFound;
		
	}
}
	
