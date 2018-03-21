package yinian.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import net.sf.json.JSONObject;
import sun.misc.BASE64Encoder;

public class YinianUtils {

	/**
	 * MD5�����㷨
	 * 
	 * @param str
	 * @return
	 */
	public static void main(String[] a){
		String b="123456";
		System.out.println(EncoderByMd5(b));
	}
	public static String EncoderByMd5(String str) {

		String newStr = ""; // ���ܺ������
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			BASE64Encoder base64en = new BASE64Encoder();
			newStr = base64en.encode(md5.digest(str.getBytes("utf-8")));
		} catch (NoSuchAlgorithmException e) {
			// �������ض��ļ����㷨�����ڸû����в�����ʱ�׳����쳣
			e.printStackTrace();
			System.out.println("�����ض��ļ����㷨�����ڸû����в�����");
		} catch (UnsupportedEncodingException e) {
			// ��֧���ַ������쳣
			e.printStackTrace();
			System.out.println("��֧���ַ������쳣");
		}

		return newStr;
	}
	
	/**
	 * md5����32λСд�ַ���
	 * @param str
	 * @return
	 */
	public static String EncodeByMd5With32Lowcase(String str) {
		String result = "";
		MessageDigest md5;
		try {
			md5 = MessageDigest.getInstance("MD5");
			md5.update((str).getBytes("UTF-8"));
			byte b[] = md5.digest();
			int i;
			StringBuffer buf = new StringBuffer("");

			for(int offset=0; offset<b.length; offset++){
				i = b[offset];
				if(i<0){
					i+=256;
				}
				if(i<16){
					buf.append("0");
				}
				buf.append(Integer.toHexString(i));
			}
			result = buf.toString();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * ��֤�����Ƿ���ͬ
	 * 
	 * @param orignalPass
	 * @param md5Pass
	 * @return
	 */
	public boolean checkPassword(String orignalPass, String md5Pass) {
		if (EncoderByMd5(orignalPass).equals(md5Pass)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * ��ȡ��ǰϵͳʱ��
	 * 
	 * @return
	 */
	public String getTimeNow() {
		Date nowTime = new Date();
		SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return (time.format(nowTime)).toString();
	}

	/**
	 * ����https����
	 * 
	 * @param requestUrl
	 *            �����ַ
	 * @param requestMethod
	 *            ����ʽ��GET��POST��
	 * @param outputStr
	 *            �ύ������
	 * @return JSONObject(ͨ��JSONObject.get(key)�ķ�ʽ��ȡjson���������ֵ)
	 */
	public static JSONObject httpsRequest(String requestUrl,
			String requestMethod, String outputStr) {
		JSONObject jsonObject = null;
		try {
			// ����SSLContext���󣬲�ʹ������ָ�������ι�������ʼ��
			TrustManager[] tm = { new MyX509TrustManager() };
			SSLContext sslContext = SSLContext.getInstance("SSL", "SunJSSE");
			sslContext.init(null, tm, new java.security.SecureRandom());
			// ������SSLContext�����еõ�SSLSocketFactory����
			SSLSocketFactory ssf = sslContext.getSocketFactory();

			URL url = new URL(requestUrl);
			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			conn.setSSLSocketFactory(ssf);

			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setUseCaches(false);
			// ��������ʽ��GET/POST��
			conn.setRequestMethod(requestMethod);

			// ��outputStr��Ϊnullʱ�������д����
			if (null != outputStr) {
				OutputStream outputStream = conn.getOutputStream();
				// ע������ʽ
				outputStream.write(outputStr.getBytes("UTF-8"));
				outputStream.close();
			}

			// ����������ȡ��������
			InputStream inputStream = conn.getInputStream();
			InputStreamReader inputStreamReader = new InputStreamReader(
					inputStream, "utf-8");
			BufferedReader bufferedReader = new BufferedReader(
					inputStreamReader);
			String str = null;
			StringBuffer buffer = new StringBuffer();
			while ((str = bufferedReader.readLine()) != null) {
				buffer.append(str);
			}

			// �ͷ���Դ
			bufferedReader.close();
			inputStreamReader.close();
			inputStream.close();
			inputStream = null;
			conn.disconnect();
			jsonObject = JSONObject.fromObject(buffer.toString());
		} catch (ConnectException ce) {

		} catch (Exception e) {

		}
		return jsonObject;
	}

	/**
	 * �����������󲢷��ؽ��
	 */
	public static String sentNetworkRequest(String url) {
		String result = "";

		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("accept", "*/*");
			con.setDoOutput(true);
			con.setDoInput(true);
			con.setConnectTimeout(3000);
			con.connect();

			InputStream input = con.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					input, "utf-8"));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line).append("\n");
			}
			result = builder.toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
		return result;
	}

}
