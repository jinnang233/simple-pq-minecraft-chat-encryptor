package com.jn233.chatenc_mc;
import eu.midnightdust.lib.config.MidnightConfig;
public class Configuration extends MidnightConfig {
	
	@Entry(category = "general") public static boolean encrypted_echo = false;
	@Entry(category = "general") public static boolean progress_bar = true;
	@Entry(category = "general") public static boolean message_detail = true;
	@Entry(category = "message_sending",min=0) public static int message_delay = 1500;
	@Entry(category = "message_sending",min=1,max=242,isSlider=true) public static int cut_limit = 230;
	@Entry(category = "message_receiving") public static boolean silly_match = false;
	@Entry(category = "message_receiving") public static String chat_regex = "\\<(.*)\\>(.*)";
	public enum CMCEParamEnum{
		mceliece348864r3,
		mceliece348864fr3,
		mceliece460896r3,
		mceliece460896fr3,
		mceliece6688128r3,
		mceliece6688128fr3,
		mceliece6960119r3,
		mceliece6960119fr3,
		mceliece8192128r3,
		mceliece8192128fr3
	}
	public enum FalconParamEnum {
		Falcon_512,
		Falcon_1024
	}
	@Entry(category = "general",isSlider = true) public static CMCEParamEnum kem_param = CMCEParamEnum.mceliece8192128r3;
	@Entry(category = "general",isSlider = true) public static FalconParamEnum sig_param = FalconParamEnum.Falcon_512;
	
	//@Comment(category = "text")
	//public static List<String> pkList = PKStorageGlass.getAllPlayerNames(MinecraftClient.getInstance());
	
}
