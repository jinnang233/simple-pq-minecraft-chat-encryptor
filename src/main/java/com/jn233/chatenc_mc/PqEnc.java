package com.jn233.chatenc_mc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Pair;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.SecureRandom;
import java.security.SignatureException;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.jcajce.provider.keystore.bc.BcKeyStoreSpi.BouncyCastleStore;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.asn1.CMCEPublicKey;
import org.bouncycastle.pqc.crypto.cmce.CMCEKEMExtractor;
import org.bouncycastle.pqc.crypto.cmce.CMCEKEMGenerator;
import org.bouncycastle.pqc.crypto.cmce.CMCEKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.cmce.CMCEKeyPairGenerator;
import org.bouncycastle.pqc.crypto.cmce.CMCEKeyParameters;
import org.bouncycastle.pqc.crypto.cmce.CMCEParameters;
import org.bouncycastle.pqc.crypto.cmce.CMCEPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.cmce.CMCEPublicKeyParameters;
import org.bouncycastle.pqc.jcajce.interfaces.CMCEKey;
import org.bouncycastle.pqc.jcajce.provider.cmce.BCCMCEPublicKey;


public class PqEnc {
	
	public static boolean privLoaded=false;
	public void makesure() {
		
		MinecraftClient instance = MinecraftClient.getInstance();
		if(PKStorageGlass.fetch(instance)==null) {
			AsymmetricCipherKeyPair keypair = (new PQParser()).generateKeyPair();
			KeyPair sig_keypair = (new PQParser().generateSigKeyPair());
			
			byte[] sig_pubkey = PQParser.getSigPubKey(sig_keypair);
			byte[] sig_privkey = PQParser.getSigPrivKey(sig_keypair);
			
			byte[] pubkey = PQParser.getPubKey(keypair);
			byte[] privkey = PQParser.getPrivKey(keypair);
			
			PKStorageGlass.store(instance, privkey);
			PKStorageGlass.put(instance,instance.player.getName().asString(),pubkey);
			
			PKStorageGlass.store(instance, sig_privkey, true);
			PKStorageGlass.putSig(instance, instance.player.getName().asString(),sig_pubkey);
		}
		PqEnc.privLoaded=true;
	}

	private encryptionDataPack kem_encrypt(byte[] data,byte[] pk,String playerName) throws IOException {
		MinecraftClient instance = MinecraftClient.getInstance();
		PQParser parser = new PQParser();
		SecretWithEncapsulation enc = parser.encaps(pk);
		byte[] encapsulation = enc.getEncapsulation();
		byte[] ss = enc.getSecret();
		
		byte[] ciphertext =  encrypt(data, ss);
		
		
		encryptionDataPack datapack = new encryptionDataPack();
		datapack.type=1;
		datapack.data=ciphertext;
		datapack.ciphertext=encapsulation;
		datapack.playerName=playerName;
		datapack.sender=instance.player.getName().asString();
		return datapack;
	}
	
	// KEM(Only) encrypt
	public String simple_KEM_encrypt(byte[] data,byte[] pk,String playerName) throws IOException {
		encryptionDataPack dataPack = kem_encrypt(data,pk,playerName);
		byte[] result = PQParser.pack(dataPack);
		return Base64.getEncoder().encodeToString(result);
	
	}
	public String encrypt_and_sign(byte[] data, byte[] pk, String playerName, byte[] sig_sk)throws IOException {
		byte[] signature = null;
		try {
			signature = PQParser.sign(data, sig_sk);
			
		}catch(InvalidKeySpecException e1) {
			ChatEnc.LOGGER.error(e1.toString());
		}catch(InvalidKeyException e2) {
			ChatEnc.LOGGER.error(e2.toString());
		}catch(SignatureException e3) {
			ChatEnc.LOGGER.error(e3.toString());
		}
		encryptionDataPack dataPack = kem_encrypt(data,pk,playerName);
		dataPack.signature = signature;
		
		byte[] result = PQParser.pack(dataPack);
		return Base64.getEncoder().encodeToString(result);
	}
	
	public SigData decrypt_and_verify(String cipher,byte[] sk,String playerName,byte[] sig_pk)  {
		SigData sig_data = new SigData();
		PQParser parser = new PQParser();
		encryptionDataPack datapack = PQParser.unpack(Base64.getDecoder().decode(cipher));
		if(datapack == null) {ChatEnc.LOGGER.info("Datapack is null!"); return null;};
		if(datapack.type!=1 || (!datapack.playerName.equalsIgnoreCase(playerName))) {
			ChatEnc.LOGGER.info("Wrong type or playername!");
			return null;
		}
		
		byte[] ss = parser.decaps(datapack.ciphertext, sk);
		byte[] data = decrypt(datapack.data,ss);
		boolean is_valid = false;
		try {
			is_valid = PQParser.verify(data, datapack.signature, sig_pk);
		} catch (InvalidKeyException e) {
			ChatEnc.LOGGER.error(e.toString());
		} catch (InvalidKeySpecException e) {
			ChatEnc.LOGGER.error(e.toString());
		} catch (SignatureException e) {
			ChatEnc.LOGGER.error(e.toString());
		}
		sig_data.is_valid=is_valid;
		sig_data.data=data;
		return sig_data;
	}
	// KEM(only) decrypt
	public byte[] simple_KEM_decrypt(String cipher,byte[] sk,String playerName) {
		PQParser parser = new PQParser();
		encryptionDataPack datapack = PQParser.unpack(Base64.getDecoder().decode(cipher));
		if(datapack == null) {ChatEnc.LOGGER.info("Datapack is null!"); return null;};
		
		if(datapack.type!=1 || (!datapack.playerName.equalsIgnoreCase(playerName))) {
			ChatEnc.LOGGER.info("Wrong type or playername!");
			return null;
		}
		
		byte[] ss = parser.decaps(datapack.ciphertext, sk);
		byte[] data = decrypt(datapack.data,ss);
		return data;
	}
	public static Object getObjfromB64(String b64str) {
		byte[] data = Base64.getDecoder().decode(b64str);
		ByteArrayInputStream istream = new ByteArrayInputStream(data);
		ObjectInputStream objp;
		Object obj;
		try {
			objp = new ObjectInputStream(istream);
			obj = objp.readObject();
		}catch(IOException | ClassNotFoundException e) {
			return null;
		}
		return obj;
	}
	public static String getB64FromObj(Object obj) throws IOException {
		if(obj==null) {
			return null;
		}
		byte[] data;
		ByteArrayOutputStream ostream = new ByteArrayOutputStream();
		ObjectOutputStream objp = new ObjectOutputStream(ostream);
		objp.writeObject(obj);
		objp.flush();
		data = ostream.toByteArray();
		objp.close();
		ostream.close();

		return Base64.getEncoder().encodeToString(data);
	}
	private String base64encode(byte[] data) {
		return Base64.getEncoder().encodeToString(data);
	}
	// AES encrypt
	private byte[] encrypt(byte[] data, byte[] key) {
		try {
			SecretKey secretkey = new SecretKeySpec(key, "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secretkey);
			
			byte[] result = cipher.doFinal(data);
			return result;
		}catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	// AES decrypt
	private byte[] decrypt(byte[] data, byte[] key) {
		try {
			SecretKey secretkey = new SecretKeySpec(key,"AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, secretkey);
			byte[] result = cipher.doFinal(data);
			return result;
		}catch (Exception e) {
			ChatEnc.LOGGER.error(e.toString());
			return null;
		}
		
	}
}
