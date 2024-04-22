package com.jn233.chatenc_mc;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.MinecraftClient;

import java.util.List;



public class Configuration extends MidnightConfig {
	
	@Entry(category = "general") public static boolean encrypted_echo = false;
	@Entry(category = "general") public static boolean progress_bar = true;
	@Entry(category = "general") public static boolean message_detail = true;
	@Entry(category = "message_sending",min=0) public static int message_delay = 1500;
	@Entry(category = "message_sending",min=1,max=242,isSlider=true) public static int cut_limit = 230;
	@Entry(category = "message_receiving") public static boolean silly_match = false;
	@Entry(category = "message_receiving") public static String chat_regex = "\\<(.*)\\>(.*)";
	
	
	public static String kem_alg = PQParser.param.getName();
	//@Comment(category = "text") 
	public static String sig_alg = PQParser.sig_param;
	//@Comment(category = "text")
	public static List<String> pkList = PKStorageGlass.getAllPlayerNames(MinecraftClient.getInstance());
	
}
