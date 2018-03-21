package yinian.utils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
public class Pkcs7Encoder {
	// �㷨����
    static  final String KEY_ALGORITHM = "AES";
    // �ӽ����㷨/ģʽ/��䷽ʽ
    static  final String algorithmStr = "AES/CBC/PKCS7Padding";
    private static Key key;
    private static Cipher cipher;
    boolean isInited = false;

    //Ĭ�϶Գƽ����㷨��ʼ���� iv
    static byte[] iv = { 0x30, 0x31, 0x30, 0x32, 0x30, 0x33, 0x30, 0x34, 0x30, 0x35, 0x30, 0x36, 0x30, 0x37, 0x30, 0x38 };

    public static void init(byte[] keyBytes) {

        // �����Կ����16λ����ô�Ͳ���.  ���if �е����ݺ���Ҫ
        int base = 16;
        if (keyBytes.length % base != 0) {
            int groups = keyBytes.length / base + (keyBytes.length % base != 0 ? 1 : 0);
            byte[] temp = new byte[groups * base];
            Arrays.fill(temp, (byte) 0);
            System.arraycopy(keyBytes, 0, temp, 0, keyBytes.length);
            keyBytes = temp;
        }
        // ��ʼ��
        Security.addProvider(new BouncyCastleProvider());
        // ת����JAVA����Կ��ʽ
        key = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        try {
            // ��ʼ��cipher
            cipher = Cipher.getInstance(algorithmStr, "BC");
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    /**
     * ���ܷ���
     *      --ʹ��Ĭ��ivʱ
     * @param content
     *            Ҫ���ܵ��ַ���
     * @param keyBytes
     *            ������Կ
     * @return
     */
    public static byte[] encrypt(byte[] content, byte[] keyBytes) {
        byte[] encryptedText =  encryptOfDiyIV(content,keyBytes,iv);
        return encryptedText;
    }


    /**
     * ���ܷ���
     *      --ʹ��Ĭ��ivʱ
     * @param encryptedData
     *            Ҫ���ܵ��ַ���
     * @param keyBytes
     *            ������Կ
     * @return
     */
    public static byte[] decrypt(byte[] encryptedData, byte[] keyBytes) {
        byte[] encryptedText = decryptOfDiyIV(encryptedData,keyBytes,iv);
        return encryptedText;
    }
    /**
     * ���ܷ���
     *      ---�Զ���Գƽ����㷨��ʼ���� iv
     * @param content
     *              Ҫ���ܵ��ַ���
     * @param keyBytes
     *              ������Կ
     * @param ivs
     *         �Զ���Գƽ����㷨��ʼ���� iv
     * @return ���ܵĽ��
     */
    public static byte[] encryptOfDiyIV(byte[] content, byte[] keyBytes, byte[] ivs) {
        byte[] encryptedText = null;
        init(keyBytes);
        System.out.println("IV��" + new String(ivs));
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(ivs));
            encryptedText = cipher.doFinal(content);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return encryptedText;
    }
    /**
     * ���ܷ���
     *
     * @param encryptedData
     *            Ҫ���ܵ��ַ���
     * @param keyBytes
     *            ������Կ
     * @param ivs
     *         �Զ���Գƽ����㷨��ʼ���� iv
     * @return
     */
    public static byte[] decryptOfDiyIV(byte[] encryptedData, byte[] keyBytes,byte[] ivs) {
        byte[] encryptedText = null;
        init(keyBytes);
        System.out.println("IV��" + new String(ivs));
        try {
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ivs));
            encryptedText = cipher.doFinal(encryptedData);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return encryptedText;
    }
}
