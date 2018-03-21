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
	      int year = c.get(Calendar.YEAR);//获取年份
	      int month=c.get(Calendar.MONTH)+1;//获取月份
	      int day=c.get(Calendar.DATE);//获取日
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
//			System.out.println("大于"+System.currentTimeMillis());
//		}else{
//			System.out.println("小于"+System.currentTimeMillis());
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
     * 生成md5 
     *  
     * @param message 
     * @return 
     */  
    public static String getMD5(String message) {  
    String md5str = "";  
    try {  
        // 1 创建一个提供信息摘要算法的对象，初始化为md5算法对象  
        MessageDigest md = MessageDigest.getInstance("MD5");  
  
        // 2 将消息变成byte数组  
        byte[] input = message.getBytes();  
  
        // 3 计算后获得字节数组,这就是那128位了  
        byte[] buff = md.digest(input);  
  
        // 4 把数组每一字节（一个字节占八位）换成16进制连成md5字符串  
        md5str = bytesToHex(buff);
        md5str = md5str.toLowerCase();
        System.out.println(md5str);
  
    } catch (Exception e) {  
        e.printStackTrace();  
    }  
    return md5str;  
    }  
    /** 
     * 二进制转十六进制 
     *  
     * @param bytes 
     * @return 
     */  
    public static String bytesToHex(byte[] bytes) {  
    StringBuffer md5str = new StringBuffer();  
    // 把数组每一字节换成16进制连成md5字符串  
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
