package com.jn233.chatenc_mc;

import java.io.Serializable;


public class encryptionDataPack implements Serializable {
	private static final long serialVersionUID = 1L;
	public int type;
	public String playerName;
	public String playerUUID;
	public String sender;
	public byte[] ciphertext= {};
	public byte[] data= {};
	public byte[] signature= {};
	public byte[] extend= {};
}
