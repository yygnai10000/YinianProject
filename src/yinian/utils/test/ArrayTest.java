package yinian.utils.test;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;

import yinian.utils.HttpUtils;
import yinian.utils.MD5;

public class ArrayTest {
	public static void main(String[] a){
		Calendar c = Calendar.getInstance();
	      int year = c.get(Calendar.YEAR);//��ȡ���
	      int month=c.get(Calendar.MONTH)+1;//��ȡ�·�
	      int day=c.get(Calendar.DATE);//��ȡ��
	      System.out.println(year+":"+month+":"+day+" 00:00:00");
	      System.out.println(year+":"+month+":"+day+" 23:59:59");
	/*	String url="https://java.zk2013.com/kedie2017/AdvApi/WxSmallProg/FreeUpCoin.do";
		String params="scene=180117044076&thirdopenid=oIrcI0Sohvl-oCVD8_gsyOummPOQ";
		String sign=MD5.getMD5(params+"&3ecb837979debb4b2b92a0a250ed8f8a").toUpperCase();
		
				//&Sign=9B9959C1C28DA87112A520B5588B6986";
		HttpUtils u=new HttpUtils();
		System.out.println("sign="+sign);
		String str=u.sendGet(url,params+"&Sign="+sign);
		try{
		String strGBK = URLEncoder.encode(str, "UTF-8");  
        System.out.println("1=="+strGBK);  
        String strUTF8 = URLDecoder.decode(str, "UTF-8");  
        System.out.println("2=="+strUTF8);
		}catch(Exception e){
			
		}*/
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//		try{
//		long f=sdf.parse("2017-12-07 17:44:53").getTime();
//		if(f>sdf.parse("2017-12-14 23:59:59").getTime()){
//			System.out.println("����"+System.currentTimeMillis());
//		}else{
//			System.out.println("С��"+System.currentTimeMillis());
//		}
//		}catch(Exception e){
//			e.printStackTrace();
//		}
		//String b="101,201,301";
//		String b="";
//		for(int i=0;i<20000;i++){
//			b+=i+""+",";
//		}
//		String[] ar=b.split(",");
//		System.out.println("begin"+System.currentTimeMillis());
//		System.out.println(useArraysBinarySearch(ar,"1"));
//		System.out.println("end"+System.currentTimeMillis());
	}
	 /** 
     * ����md5 
     *  
     * @param message 
     * @return 
     */  
    public static String getMD5(String message) {  
    String md5str = "";  
    try {  
        // 1 ����һ���ṩ��ϢժҪ�㷨�Ķ��󣬳�ʼ��Ϊmd5�㷨����  
        MessageDigest md = MessageDigest.getInstance("MD5");  
  
        // 2 ����Ϣ���byte����  
        byte[] input = message.getBytes();  
  
        // 3 ��������ֽ�����,�������128λ��  
        byte[] buff = md.digest(input);  
  
        // 4 ������ÿһ�ֽڣ�һ���ֽ�ռ��λ������16��������md5�ַ���  
        md5str = bytesToHex(buff);
        md5str = md5str.toLowerCase();
        System.out.println(md5str);
  
    } catch (Exception e) {  
        e.printStackTrace();  
    }  
    return md5str;  
    }  
    /** 
     * ������תʮ������ 
     *  
     * @param bytes 
     * @return 
     */  
    public static String bytesToHex(byte[] bytes) {  
    StringBuffer md5str = new StringBuffer();  
    // ������ÿһ�ֽڻ���16��������md5�ַ���  
    int digital;  
    for (int i = 0; i < bytes.length; i++) {  
        digital = bytes[i];  
  
        if (digital < 0) {  
        digital += 256;  
        }  
        if (digital < 16) {  
        md5str.append("0");  
        }  
        md5str.append(Integer.toHexString(digital));  
    }  
    return md5str.toString().toUpperCase();  
    } 
	public static boolean useArraysBinarySearch(String[] arr,String targetValue){
		 for(String s:arr){
		        if(s.equals(targetValue))
		            return true;
		        }  
		        return false;
	}
}
