package com.jn233.chatenc_mc;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Pair;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PKStorageGlass {

	public static PqEnc encryptor = new PqEnc();
	public static final String pkDirectoryName="keys";
	private final static String pkRegex = "(.*)\\-(.*)\\.pk";
	private final static String spkRegex = "(.*)\\-(.*)\\.spk";
	public PKStorageGlass() {
		
	}
	
	// store public keys
	public static boolean makeDir(MinecraftClient instance) {
		File dir = instance.runDirectory;
		File pkdir = new File(dir.getAbsolutePath(),pkDirectoryName);
		byte[] pubkey = {};
		try {
			return pkdir.mkdir();
			
		}catch (SecurityException se) {
			return false;
		}
	}
	// Get File list
	private static File[] getFiles(MinecraftClient instance) {
		File dir = instance.runDirectory;
		File pkdir = new File(dir.getAbsolutePath(),pkDirectoryName);
		try {
			if(pkdir.exists()) {
				File[] pks = pkdir.listFiles(new pubKeyFilter());
				return pks;
			}
			else {
				PKStorageGlass.makeDir(instance);
				return null;
			}
		}catch(SecurityException se) {
			return null;
		}
	}
	public static List<String> getAllPlayerNames(MinecraftClient instance) {
		File[] pks = getFiles(instance);
		ArrayList<String> playerNames = new ArrayList<String>();

		
		for(File pk:pks) {
			String pkFileName = pk.getName();
			Matcher matcher = Pattern.compile(pkRegex).matcher(pkFileName);
			if(matcher.find()) {
				playerNames.add(matcher.group(1));
			}
		}
		return playerNames;
	}
	// Get public key
	public static byte[] get(MinecraftClient instance,int groupId,String value,boolean is_signature) {
		for(File pk:PKStorageGlass.getFiles(instance)){
			String pkName = pk.getName();
			String keyRegex = is_signature?spkRegex:pkRegex;
			Matcher matcher = Pattern.compile(keyRegex).matcher(pkName);
			if (matcher.find()) {
				if(matcher.group(groupId).equalsIgnoreCase(value)) {
					try {
						byte[] keyData = Files.readAllBytes(Paths.get(pk.getAbsolutePath()));
						return keyData;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return null;
					}
				}
				}
				
			}
		return null;
	}
	public static byte[] get(MinecraftClient instance,int groupId,String value) {
		return get(instance,groupId,value,false);
	}
	public static byte[] get(MinecraftClient instance,String name) {
		return PKStorageGlass.get(instance, 1, name);
	}
	public static byte[] get(MinecraftClient instance,String name,boolean is_signature) {
		return PKStorageGlass.get(instance, 1, name,is_signature);
	}
	
	public static boolean putFile(MinecraftClient instance,String fileName,byte[] data) {
		return putFile(instance,fileName,data,false);
		
	}
	public static boolean putFile(MinecraftClient instance,String fileName,byte[] data,boolean ignore_exists) {
		File dir = instance.runDirectory;
		File keydir = new File(dir.getAbsolutePath(),pkDirectoryName);
		File pkfile = new File(keydir.getAbsolutePath(),fileName);
		if(keydir.exists() && ((!pkfile.exists())||ignore_exists)) {
			try {
				if(pkfile.createNewFile()) {
					Files.write(Paths.get(pkfile.getAbsolutePath()), data);
					return true;
				}
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		return false;
	}
	
	public static File getFileObject(MinecraftClient instance,String fileName) {
		File dir = instance.runDirectory;
		File keydir = new File(dir.getAbsolutePath(),pkDirectoryName);
		return new File(keydir.getAbsolutePath(),fileName);
	}
	
	public static boolean put(MinecraftClient instance,String name,byte[] publicKey,String uuid) {
		return putFile(instance,String.format("%s-%s.pk",name,uuid),publicKey);
	}
	public static boolean put(MinecraftClient instance,String name,byte[] publicKey) {
		return PKStorageGlass.put(instance, name, publicKey,"nouuid");
	}
	public static boolean putSig(MinecraftClient instance,String name,String uuid,byte[] publicKey) {
		return putFile(instance, String.format("%s-%s.spk",name,uuid), publicKey);
	}
	public static boolean putSig(MinecraftClient instance,String name,byte[] publicKey) {
		return PKStorageGlass.putSig(instance, name,"nouuid", publicKey);
	}
	public static boolean store(MinecraftClient instance,byte[] privateKey) {
		return store(instance,privateKey,false);
	}

	public static boolean store(MinecraftClient instance,byte[] privateKey,boolean is_signature) {
		File dir = instance.runDirectory;
		File pkdir = new File(dir.getAbsolutePath(),pkDirectoryName);
		String postfix;
		if(is_signature) {
			postfix=".ssk";
		}else {
			postfix=".sk";
		}
		
		File skfile = new File(pkdir.getAbsolutePath(),instance.player.getName().asString()+postfix);
		boolean flag=false;
		if(pkdir.exists()) {
			if(!skfile.exists())
				try {
					if(skfile.createNewFile()) flag=true;
				} catch (IOException e) {
					e.printStackTrace();
					flag=false;
				}
			if(flag) {
				try {
					Files.write(Paths.get(skfile.getAbsolutePath()), privateKey);
					return true;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
		
	}
	// Fetch private key
	public static byte[] fetch(MinecraftClient instance) {
		return fetch(instance,false);
	}
	public static byte[] fetch(MinecraftClient instance,boolean is_signature) {
		File dir = instance.runDirectory;
		File pkdir = new File(dir.getAbsolutePath(),pkDirectoryName);
		String postfix;
		if(is_signature) {
			postfix=".ssk";
		}else {
			postfix=".sk";
		}
		
		File skfile = new File(pkdir.getAbsolutePath(),instance.player.getName().asString()+postfix);
		if(pkdir.exists() && skfile.exists() && skfile.canRead()) {
			try {
				byte[] keyData = Files.readAllBytes(Paths.get(skfile.getAbsolutePath()));
				return keyData;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}

