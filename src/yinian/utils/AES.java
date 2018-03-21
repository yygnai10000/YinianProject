package yinian.utils;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.plugin.auth.SessionKit;

import java.security.AlgorithmParameters;
import java.security.Security;
import java.util.Arrays;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

public class AES {

	public static boolean initialized = false;
	private Key initKeyForAES(String key) throws NoSuchAlgorithmException {
        if (null == key || key.length() == 0) {
            throw new NullPointerException("key not is null");
        }
        SecretKeySpec key2 = null;
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed(key.getBytes());
        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            kgen.init(128, random);
            SecretKey secretKey = kgen.generateKey();
            byte[] enCodeFormat = secretKey.getEncoded();
            key2 = new SecretKeySpec(enCodeFormat, "AES");
        } catch (NoSuchAlgorithmException ex) {
            throw new NoSuchAlgorithmException();
        }
        return key2;

    }
	/**
	 * AES解密
	 * 
	 * @param content
	 *            密文
	 * @return
	 * @throws InvalidAlgorithmParameterException
	 * @throws NoSuchProviderException
	 */
	public byte[] decrypt_lk(byte[] content, byte[] keyByte, byte[] ivByte)
			throws InvalidAlgorithmParameterException {
		initialize();
		 try {
			 Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
				Key sKeySpec = new SecretKeySpec(keyByte, "AES");

				cipher.init(Cipher.DECRYPT_MODE, sKeySpec, generateIV(ivByte));// 初始化
				byte[] result = cipher.doFinal(content);
				return result;
			 
			 
//			 String key=new String(keyByte);
//				Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
//				SecretKeySpec skey = new SecretKeySpec(keyByte,0,Math.min(keyByte.length,Cipher.getMaxAllowedKeyLength("AES")/8),"AES");
//				
//				IvParameterSpec ivParam = new IvParameterSpec(ivByte,0,cipher.getBlockSize());
//		        //cipher.init(Cipher.DECRYPT_MODE,initKeyForAES(key),ivParam);
//		        cipher.init(Cipher.DECRYPT_MODE,skey,ivParam);
//				byte[] result = cipher.doFinal(content);
			 
//			 AlgorithmParameters iv = WechatAppDecryptor.generateIV(Base64.decode(ivStr));
//		        Key key = convertToKey(keyBytes);
//		        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
//		        //设置为解密模式
//		        cipher.init(Cipher.DECRYPT_MODE, key,iv);
//				return result;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * AES解密
	 * 
	 * @param content
	 *            密文
	 * @return
	 * @throws InvalidAlgorithmParameterException
	 * @throws NoSuchProviderException
	 */
	public byte[] decrypt(byte[] content, byte[] keyByte, byte[] ivByte)
			throws InvalidAlgorithmParameterException {
		initialize();
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			Key sKeySpec = new SecretKeySpec(keyByte, "AES");

			cipher.init(Cipher.DECRYPT_MODE, sKeySpec, generateIV(ivByte));// 初始化
			byte[] result = cipher.doFinal(content);
			return result;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static void initialize() {
		if (initialized)
			return;
		Security.addProvider(new BouncyCastleProvider());
		initialized = true;
	}

	// 生成iv
	public static AlgorithmParameters generateIV(byte[] iv) throws Exception {
		AlgorithmParameters params = AlgorithmParameters.getInstance("AES");
		params.init(new IvParameterSpec(iv));
		return params;
	}

	public static void main(String[] args)
			throws InvalidAlgorithmParameterException,
			UnsupportedEncodingException {

		String code = "031ygOUR12cfU815fhWR1ZxIUR1ygOUl";
		// 根据登录的code获取用户的session_key和openid
		YinianDataProcess data = new YinianDataProcess();
		String result = data
				.sentNetworkRequest(CommonParam.getWechatSessionKeyUrl + code);
		JSONObject jo = JSONObject.parseObject(result);
		String session_key = jo.getString("session_key");
		String openid = jo.getString("openid");
		//f
		session_key = "m3OOV3OzKDA6LkhcdaD6hg==";
		String encryptedData = "F3fjjrBD4QCarcp3GpDuIJmzTYhVwZ5dx30p+3bajnihnZn1EGH61MzVZXhinRS2mhW6dHqapuiZkALJbiW41PLwI7ysQJtwtCgZ3NcXy/fuuaWlzEAgUsahczxbsteH33InTfrOhL5tPYeVipbzTw==";
		String iv = "D5Z3b45f2umUyhEXWbRefw==";
		//t
//		session_key = "rCwwI6u13zda4Z7dpb3Lcw==";
//		       encryptedData = "QsELnGfsi/Y940betzZXrGxYrZfRY5/iNWM3dJal5sCvBTsf+N4eos0Jt35xvpHaCPE/FRCfMztEW2WIHwQXFyNcEOTXzlKuLrMbgfnKYArfkrQOTYracgGS9mRNXqRfxStKTOxQSNlqUwlPjmwLDQ==";
//		       iv = "rARf9HPfCqAMpxy+MX/oeQ==";
		AES aes = new AES();

		byte[] resultByte = aes.decrypt(Base64.decode(encryptedData),
				Base64.decode(session_key), Base64.decode(iv));
		if (null != resultByte && resultByte.length > 0) {
			String userInfo = new String(resultByte, "UTF-8");
			System.out.println(userInfo);
		}

	}

}
