package com.jn233.chatenc_mc.mixin;

import java.io.IOException;

import org.spongepowered.asm.mixin.Mixin;

import com.jn233.chatenc_mc.ChantConstants;
import com.jn233.chatenc_mc.ChatEnc;
import com.jn233.chatenc_mc.ChatHandler;
import com.jn233.chatenc_mc.CutSendImplements;
import com.jn233.chatenc_mc.PKStorageGlass;
import com.jn233.chatenc_mc.PqEnc;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.StatHandler;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.util.Pair;
// Reference: https://www.mcbbs.net/thread-1205392-1-1.html
@Mixin(ClientPlayerEntity.class)
public class EncMixin{

	private PqEnc encryptor;
	private ChatHandler chat = new ChatHandler();
	private String etell_pattern = "etell\\s+(\\S*)\\s+(.*)";
	private String estell_pattern = "estell\\s+(\\S*)\\s+(.*)";
	private Pattern r = Pattern.compile(etell_pattern);
	private Pattern estell_r = Pattern.compile(estell_pattern);
	@Inject(method="sendChatMessage", at=@At("HEAD"), cancellable=true)
	public void addToMessageHistory(String message, CallbackInfo ci) {
		
		// Get Instance
		MinecraftClient instance = MinecraftClient.getInstance();
		if(this.encryptor ==null) {
			this.encryptor = PKStorageGlass.encryptor;
		}
		
		
		Matcher m = r.matcher(message);
		Matcher estell_m = estell_r.matcher(message);
		
		boolean estell_found = estell_m.find();
		boolean m_found = m.find();
		
		
		
		if(estell_found||m_found) {
			String playername = estell_found?estell_m.group(1):m.group(1);
			message = estell_found?estell_m.group(2):m.group(2);
			ChatHandler.sendEncrypted(encryptor, message, playername, estell_found, ci);
		}
		
		
	}
	public boolean updatePqEnc(String sigSK, String kemSK) {
		this.encryptor = new PqEnc();
		return true;
	}

}
