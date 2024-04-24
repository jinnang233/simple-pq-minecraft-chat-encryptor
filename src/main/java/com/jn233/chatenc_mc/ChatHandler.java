package com.jn233.chatenc_mc;


import net.minecraft.client.MinecraftClient;

import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ChatHandler {
	private PqEnc encryptor = new PqEnc();
	
	
	private String session_pattern = "\\[\\+\\=(.*)\\]\\:(.*)"; 
	private String session_end_pattern = "\\[\\$\\=(.*)\\]:(.*)"; 
	

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
		LOGGER.debug("Session match!"); 
		

		String sessionId = sessionMatcher.group(1); 
		String content = sessionMatcher.group(2);	
		
		sessionMap.put(sessionId, sessionMap.containsKey(sessionId)?sessionMap.get(sessionId)+content:content);
		
	}
	public void sessionEndProcess(Matcher sessionEndMatcher,String playerName) {
		if(sessionEndMatcher.groupCount()!=2) {return;}
		LOGGER.debug("Session end match!");
		
		String sessionId = sessionEndMatcher.group(1);
		String content = sessionEndMatcher.group(2);	
		//if(!sessionMap.containsKey(sessionId)) return;
		sessionMap.put(sessionId, sessionMap.containsKey(sessionId)?sessionMap.get(sessionId)+content:content);
		content = (String) sessionMap.get(sessionId);
		chatProcess(content,playerName); 

		// Delete processed session
		sessionMap.remove(sessionId);
	}
	public boolean chatProcessWithSession(String message,String playerName) {
		// TODO Try to match the message using regular expression, if the match is successful process the encrypted message

		Matcher session_end_pattern_matcher = session_end_pattern_patterner.matcher(message);
		
		Matcher session_pattern_matcher = session_pattern_patterner.matcher(message);
		
		
		if(session_pattern_matcher.find()) { 
			sessionProcess(session_pattern_matcher);
			//No echo
			if(!Configuration.encrypted_echo)return false;
		}else if(session_end_pattern_matcher.find()) { 
			sessionEndProcess(session_end_pattern_matcher,playerName);
			if(!Configuration.encrypted_echo)return false;
		
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
		String result="";
		String postfix="general.jn233_mcchat_enc.nosignature";
		
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
		
		
		switch(datapack.type) {
			case 1:
				// Determine the length of the signature and try to verify the signature if it is not empty
				if(datapack.signature.length!=0) {
					byte[] sig_pk = PKStorageGlass.get(instance, sender, true);
					sigdata = encryptor.decrypt_and_verify(message, sk, instance.player.getName().getString(), sig_pk);
					if(sigdata==null) {return;}
					chat=sigdata.data;
				}else {
					chat = encryptor.simple_KEM_decrypt(message, sk, instance.player.getName().getString());
				}
				
				
				// Try to decrypt using encryptor
				

				if(chat==null) {return;}
				
				result=new String(chat);
				// Show in game
				postfix="general.jn233_mcchat_enc.nosignature";
				if(sigdata!=null) {
					if(sigdata.is_valid) {
						postfix="general.jn233_mcchat_enc.valid";
					}else {
						postfix="general.jn233_mcchat_enc.invalid";
					}
				}
				
				
						
				break;
			case 2:
				if(encryptor.decaps(message,sk,instance.player.getName().getString(), sender)) {
					instance.inGameHud.getChatHud().addMessage(
							Text.literal(sender)
							.append(Text.literal(" "))
							.append(Text.translatable("general.jn233_mcchat_enc.exchange_secret"))
							.append(Text.literal("("))
							.append(Text.translatable("general.jn233_mcchat_enc.unidirectional"))
							.append(Text.translatable(")"))
							);
				}
				break;
				
			case 3:
				chat = encryptor.ss_decrypt(message,sk,instance.player.getName().getString(), sender);
				if(chat==null) {return;}
				result = new String(chat);
				postfix="general.jn233_mcchat_enc.nosignature";
				break;
		}
		
		if(datapack.type==1 || datapack.type == 3) {
			// Output message
			Text output_text = 
					Text.literal("[")
					.append(Text.literal(sender))
					.append(Text.literal("]"))
					.append(Text.literal("["))
					.append(Text.translatable(postfix))
					.append(Text.literal("]"))
					.append(Text.translatable("general.jn233_mcchat_enc.encrypt_tip"))
					.append(Text.literal(":" + result));
					instance.inGameHud.getChatHud().addMessage(output_text);
		}
	}
	
	public static void cutSend(String message,int length, int message_delay) {
		// Compute CRC32
		CRC32 crc32 = new CRC32();
		crc32.update(message.getBytes());
		crc32.update(Long.toHexString(System.currentTimeMillis()).getBytes());
		crc32.update(Long.toHexString(Random.create().nextLong()).getBytes());
		long checksum = crc32.getValue();
		String sessionId = Long.toHexString(checksum);
		
		MessageSendingProgress progress = new MessageSendingProgress();
		progress.setDelay(message_delay);
		int total = (int)Math.ceil(message.length()/length)+1;
		progress.setTotal(total);
		MinecraftClient instance = MinecraftClient.getInstance();
		for(int i=0;i<message.length();i+=length) {
			progress.setCurrent((i/length)+1);
			progress.setSender(instance.player.getName().toString());
			progress.setSessionId(sessionId);
			String message_cuted = sessionId + "]:" + message.substring(i,i+length>message.length()?message.length():i+length);
			if(i+length>=message.length()) {
				message_cuted="[$="+message_cuted;
			}else{
				message_cuted="[+="+message_cuted;
			}
			
			
			instance.getNetworkHandler().sendChatMessage(message_cuted);
			
			try {
				progress.call();
			} catch (Exception e) {}
			try {
				if(i+length<message.length()) 
					Thread.sleep(message_delay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		progress.finish();
	}
	
	public static void cutSend(String message,int length){
		cutSend(message,length,0);
	}
	public static boolean sendEncrypted(PqEnc encryptor,String message, String playerName,boolean withSignature) {
		return sendEncrypted(encryptor,message,playerName,withSignature,1);
	}
	
	public static boolean sendEncrypted(PqEnc encryptor,String message, String playerName,boolean withSignature,int type) {
			// TODO Encrypt the message and send it to the specified person
			boolean playerFound = false;
			MinecraftClient instance = MinecraftClient.getInstance();
			if(!PqEnc.privLoaded) { 
				instance.inGameHud.getChatHud().addMessage(Text.translatable("general.jn233_mcchat_enc.private_key_not_available_yet"));
				
				return false;
				}
			
			Text message_echo = Text.translatable("general.jn233_mcchat_enc.message_echo")
					.append(" -> ")
					.append(playerName)
					.append(": ")
					.append(message);
			
			try {
				byte[] pk = PKStorageGlass.get(instance, playerName);
				byte[] sig_sk = PKStorageGlass.fetch(instance, true);
				if(pk!=null) {
					switch(type) {
					case 1:
						if(withSignature) {
							message = encryptor.encrypt_and_sign(message.getBytes(), pk, playerName, sig_sk);
							
						}else {
							message = encryptor.simple_KEM_encrypt(message.getBytes(), pk, playerName);
						}
						break;
					case 2:
						message = encryptor.encaps(pk, playerName);
						break;
					case 3:
						message = encryptor.ss_encrypt(message.getBytes(), pk, playerName);
					}
					playerFound = true;
				}else {
					message="";
					
				}
				
			} catch (IOException e) {
				e.printStackTrace();
				if(Configuration.message_detail)instance.inGameHud.getChatHud().addMessage(Text.literal(e.toString()));
				return false;
				
			}


			if(playerFound) {
				if(Configuration.message_detail)instance.inGameHud.getChatHud().addMessage(Text.translatable("general.jn233_mcchat_enc.found").append(Text.literal(" "+playerName+" ")).append(Text.translatable("general.jn233_mcchat_enc.public_key")));
				CutSendImplements cutSend = new CutSendImplements();
				cutSend.setCutLimit(Configuration.cut_limit);
				cutSend.setMessage(message);
				cutSend.setDelay(Configuration.message_delay);
				
				cutSend.setCallback(
						()->{
							if(type!=2)
								instance.inGameHud.getChatHud().addMessage(message_echo);
							return;
						}
						);
				cutSend.onSending((send_message,cutLimit,delay)->{
					cutSend(send_message,cutLimit,delay);
					return;
				});
				Thread message_sending_thread =  new Thread(cutSend);
				message_sending_thread.start();
				
			}else {
				instance.inGameHud.getChatHud().addMessage(Text.translatable("general.jn233_mcchat_enc.notfound").append(Text.literal(" "+playerName+" ")).append(Text.translatable("general.jn233_mcchat_enc.public_key")));
			}

			
			return playerFound;
		
	}
}
	
