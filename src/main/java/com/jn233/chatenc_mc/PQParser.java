package com.jn233.chatenc_mc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.pqc.crypto.cmce.CMCEKEMExtractor;
import org.bouncycastle.pqc.crypto.cmce.CMCEKEMGenerator;
import org.bouncycastle.pqc.crypto.cmce.CMCEKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.cmce.CMCEKeyPairGenerator;
import org.bouncycastle.pqc.crypto.cmce.CMCEParameters;
import org.bouncycastle.pqc.crypto.cmce.CMCEPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.cmce.CMCEPublicKeyParameters;
import org.bouncycastle.pqc.crypto.falcon.FalconParameters;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;



public class PQParser {
	public static CMCEParameters param = null;
	public static String sig_param = null;
	
	
	
	private final SecureRandom sr = new SecureRandom();
	
	private CMCEKEMGenerator kem_generator ;
	private CMCEKeyPairGenerator keypair_generator ;
	@SuppressWarnings("unused")
	private KeyFactory sigs_factory ;
	private KeyPairGenerator sigs_kpgen;
	public static HashMap<String,CMCEParameters> getKEMParams() {
		HashMap<String,CMCEParameters> params = new HashMap<String,CMCEParameters>();
		Class<CMCEParameters> param_class = CMCEParameters.class;
		Field[] fields = param_class.getFields();
		for(Field f: fields) {
			if(f.getName().startsWith("mceliece")) {
				try {
					params.put(f.getName(), (CMCEParameters) f.get(null));
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return params;
	}
	public static ArrayList<String> getSIGParams() {
		ArrayList<String> params = new ArrayList<String>();
		Class<FalconParameters> param_class = FalconParameters.class;
		Field[] fields = param_class.getFields();
		for(Field f: fields) {
			if(f.getName().startsWith("falcon_")) {
				params.add(f.getName().replace("_", "-").replace("falcon", "Falcon"));
			}
		}
		return params;
	}
	public PQParser() {
		if(param==null) {
			HashMap<String,CMCEParameters> param_map = PQParser.getKEMParams();
			PQParser.param=param_map.get(Configuration.kem_param.name());
		}
		if(sig_param==null) {
			PQParser.sig_param = Configuration.sig_param.name().replace("_","-");
		}
		
		
		this.kem_generator = new CMCEKEMGenerator(sr);
		this.keypair_generator = new CMCEKeyPairGenerator();
		this.keypair_generator.init(new CMCEKeyGenerationParameters(sr, param));
		try {
			this.sigs_factory = KeyFactory.getInstance(sig_param,new BouncyCastlePQCProvider());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		try {
			this.sigs_kpgen = KeyPairGenerator.getInstance(sig_param,new BouncyCastlePQCProvider());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		
	}
	


	public AsymmetricCipherKeyPair generateKeyPair() {
		return this.keypair_generator.generateKeyPair();
	}
	
	public KeyPair generateSigKeyPair() {
		return sigs_kpgen.generateKeyPair();
	}
	
	public static byte[] getSigPubKey(KeyPair keyPair) {
		return keyPair.getPublic().getEncoded();
	}
	public static byte[] getSigPrivKey(KeyPair keyPair) {
		return keyPair.getPrivate().getEncoded();
	}
	public static byte[] sign(byte[] message, byte[] privateKey) throws  InvalidKeySpecException, InvalidKeyException, SignatureException {
		KeyFactory sig_factory;
		PrivateKey secret_key;
		Signature signer;
		PKCS8EncodedKeySpec sk_spec = new PKCS8EncodedKeySpec(privateKey);
		try {
			sig_factory = KeyFactory.getInstance(sig_param,new BouncyCastlePQCProvider());
			secret_key = sig_factory.generatePrivate(sk_spec);
			signer = Signature.getInstance(sig_param,new BouncyCastlePQCProvider());
			signer.initSign(secret_key);
			signer.update(message);
			byte[] signature = signer.sign();
			return signature;
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		
	}
	public static boolean verify(byte[] message,byte[] signature, byte[] publicKey) throws InvalidKeySpecException, InvalidKeyException, SignatureException {
		KeyFactory sig_factory;
		PublicKey public_key;
		Signature verifier;
		X509EncodedKeySpec pk_spec = new X509EncodedKeySpec(publicKey);
		try {
			sig_factory = KeyFactory.getInstance(sig_param,new BouncyCastlePQCProvider());
			public_key = sig_factory.generatePublic(pk_spec);
			verifier = Signature.getInstance(sig_param,new BouncyCastlePQCProvider());
			verifier.initVerify(public_key);
			verifier.update(message);
			return verifier.verify(signature);
		} catch (NoSuchAlgorithmException e) {
			return false;
		}
	}


	public static byte[] getPubKey(AsymmetricCipherKeyPair keyPair) {
		return ((CMCEPublicKeyParameters)keyPair.getPublic()).getPublicKey();
	}



	public static byte[] getPrivKey(AsymmetricCipherKeyPair keyPair) {
		return ((CMCEPrivateKeyParameters)keyPair.getPrivate()).getPrivateKey();
	}


	public SecretWithEncapsulation encaps(byte[] pk){
		CMCEPublicKeyParameters cmcePk = new CMCEPublicKeyParameters(param, pk);
		
		SecretWithEncapsulation encapsulation = this.kem_generator.generateEncapsulated(cmcePk);
		return encapsulation;
	}
	
	
	public byte[] decaps(byte[] ciphertext,byte[] sk) {
		CMCEPrivateKeyParameters cmceSk = new CMCEPrivateKeyParameters(param, sk);
		
		CMCEKEMExtractor extractor = new CMCEKEMExtractor(cmceSk);
		return extractor.extractSecret(ciphertext);
		
	}
	
	
	public static byte[] pack(encryptionDataPack dataPack) throws IOException {
		ByteArrayOutputStream bBuffer = new ByteArrayOutputStream();
		try(ObjectOutputStream output = new ObjectOutputStream(bBuffer)){
			//output.writeObject(dataPack);
			if(dataPack.playerName==null)dataPack.playerName=new String();
			if(dataPack.playerUUID==null)dataPack.playerUUID=new String();
			byte[] playerName = dataPack.playerName.getBytes();
			byte[] playerUUID = dataPack.playerUUID.getBytes();
			output.writeInt(dataPack.type);
			output.writeInt(playerName.length);
			output.writeInt(playerUUID.length);
			output.writeInt(dataPack.ciphertext.length);
			output.writeInt(dataPack.data.length);
			output.writeInt(dataPack.signature.length);
			output.writeInt(dataPack.extend.length);
			output.write(playerName);
			output.write(playerUUID);
			output.write(dataPack.ciphertext);
			output.write(dataPack.data);
			output.write(dataPack.signature);
			output.write(dataPack.extend);
			
			output.flush();
		}
		return bBuffer.toByteArray();
	}
	
	public static encryptionDataPack unpack(byte[] data)  {
		ByteArrayInputStream bBuffer = new ByteArrayInputStream(data);
		try(ObjectInputStream input = new ObjectInputStream(bBuffer)){
			try {
				encryptionDataPack dataPack = new encryptionDataPack();
				dataPack.type=input.readInt();
				int len_playername = input.readInt();
				int len_playeruuid = input.readInt();
				int len_ciphertext = input.readInt();
				int len_data = input.readInt();
				int len_signature = input.readInt();
				int len_extend = input.readInt();
				dataPack.playerName = new String(input.readNBytes(len_playername));
				dataPack.playerUUID = new String(input.readNBytes(len_playeruuid));
				dataPack.ciphertext = input.readNBytes(len_ciphertext);
				dataPack.data=input.readNBytes(len_data);
				dataPack.signature = input.readNBytes(len_signature);
				dataPack.extend = input.readNBytes(len_extend);
				
				return dataPack;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
	}
	
}
