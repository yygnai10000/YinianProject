package yinian.utils.test;

import com.qiniu.common.QiniuException;

import com.qiniu.common.Zone;

import com.qiniu.http.Response;

import com.qiniu.processing.OperationManager;


import com.qiniu.util.Auth;

import com.qiniu.util.StringMap;

import com.qiniu.util.UrlSafeBase64;

import yinian.common.CommonParam;
import yinian.utils.QiniuOperate;


public class DownTest {
	//private static final String bucket = "yinian";
	//private static final String bucket = "https://photo.zhuiyinanian.com/";
//	public static void main_32(String[] a){
//		Auth auth = Auth.create(CommonParam.accessKey, CommonParam.secretKey);
//
//		OperationManager operater = new OperationManager(auth);
////		String a1="http://78re52.com1.z0.glb.clouddn.com/resource/gogopher.jpg";
////		System.out.println("encode="+encode(a1.getBytes()));
//		String url1="https://photo.zhuiyinanian.com/50cf5dcb-5cb8-4aa7-a8ff-f092e17add89.jpeg";
//		String url2="https://photo.zhuiyinanian.com/34b7a2d7-2b6e-45fa-9843-bcb8858337e3.jpg";
//		String url3="https://photo.zhuiyinanian.com/tmp_61cebdfda1a823ad5277215645d33246.jpg";
//		String name1="50cf5dcb-5cb8-4aa7-a8ff-f092e17add89.jpeg";
//		String name2="34b7a2d7-2b6e-45fa-9843-bcb8858337e3.jpg";
//		String name3="tmp_61cebdfda1a823ad5277215645d33246.jpg";
//		String key="50cf5dcb-5cb8-4aa7-a8ff-f092e17add89.jpeg";
//		String zipName = "yasuobao.zip";
//		String encodedEntryURI = UrlSafeBase64.encodeToString(bucket + ":" + zipName);
//		 String fops = "";//fops参数
//	        try {
//	            fops = "mkzip/4"
//	                    + "/url/" + UrlSafeBase64.encodeToString(url1) + "/alias/" + UrlSafeBase64.encodeToString(name1)
//	                    + "/url/" + UrlSafeBase64.encodeToString(url2) + "/alias/" + UrlSafeBase64.encodeToString(name2)
//	                    + "/url/" + UrlSafeBase64.encodeToString(url3) + "/alias/" + UrlSafeBase64.encodeToString(name3)
//	                    + "|saveas/" + encodedEntryURI;
//
//	            System.out.println(fops);
//	            String id = operater.pfop(bucket, key, fops);
//	            String purl = "http://api.qiniu.com/status/get/prefop?id=" + id;
//	            //
//	            System.out.println(purl);
//	        } catch (Exception e) {
//	        	e.printStackTrace();
////	        	 Response r = e.response;
////
////		            // 请求失败时简单状态信息
////
////		            System.out.println(r.toString());
////
////		            try {
////
////		                // 响应的文本信息
////
////		                System.out.println(r.bodyString());
////
////		            } catch (QiniuException e1) {
////
////		                //ignore
////
////		            }
//	        }
////			Auth auth = Auth.create(CommonParam.accessKey, CommonParam.secretKey);
////
////		    OperationManager operater = new OperationManager(auth);
////
////		    String bucket = "testres";
////
////		    String key = "sintel_trailer.mp4";
////
////		    // CHECKSTYLE:OFF
////		    Base64EncodedURL
////		    String fops = "mkzip/2/url/aHR0cDovL3Rlc3RyZXMucWluaXVkbi5jb20vZ29nb3BoZXIuanBn/alias/Z29nb3BoZXIuanBn/url/aHR0cDovL3Rlc3RyZXMucWluaXVkbi5jb20vZ29nb3BoZXIuanBn";
////
////		    fops += "|saveas/" + UrlSafeBase64.encodeToString("javasdk" + ":" + key + "_" + UUID.randomUUID());
////
////		    // CHECKSTYLE:ON
////
////		    try {
////
////		        String id = operater.pfop(bucket, key, fops);
////
////		       // assertNotNull(id);
////
////		       // assertNotEquals("", id);
////
////		        String purl = "http://api.qiniu.com/status/get/prefop?id=" + id;
////
////		        System.out.println(purl);
////
////		    } catch (QiniuException e) {
////
////		        Response res = e.response;
////
////		        System.out.println(res);
////
////		        try {
////
////		            System.out.println(res.bodyString());
////
////		        } catch (QiniuException e1) {
////
////		            e1.printStackTrace();
////
////		        }
////
////		    }
//
//	}
	public static void main(String[] a){
		QiniuOperate operate = new QiniuOperate();
		String bucket = "yinian";
		String accessKey = "2caeYdL9QSwjhpJc2v05LLgaOrk_Mc_HterAtD28";
		// 七牛云密钥
		String secretKey = "XQYY6AE3rhhp-ep9-xwOOUc2noRyAvXu8uLjkTMT";
		Auth auth = Auth.create(accessKey, secretKey);
		//Auth auth = Auth.create(CommonParam.accessKey, CommonParam.secretKey);

		OperationManager operater = new OperationManager(auth);
//		String a1="http://78re52.com1.z0.glb.clouddn.com/resource/gogopher.jpg";
//		System.out.println("encode="+encode(a1.getBytes()));
		
		String url1=auth.privateDownloadUrl("http://7xpend.com1.z0.glb.clouddn.com/50cf5dcb-5cb8-4aa7-a8ff-f092e17add89.jpeg");
		String url2=auth.privateDownloadUrl("http://7xpend.com1.z0.glb.clouddn.com/34b7a2d7-2b6e-45fa-9843-bcb8858337e3.jpg");
		String url3=auth.privateDownloadUrl("http://7xpend.com1.z0.glb.clouddn.com/tmp_61cebdfda1a823ad5277215645d33246.jpg");
		//String url3=auth.privateDownloadUrl("https://photo.zhuiyinanian.com/tmp_61cebdfda1a823ad5277215645d33246.jpg");
		String name1="50cf5dcb-5cb8-4aa7-a8ff-f092e17add89.jpeg";
		String name2="34b7a2d7-2b6e-45fa-9843-bcb8858337e3.jpg";
		String name3="tmp_61cebdfda1a823ad5277215645d33246.jpg";
		String key="50cf5dcb-5cb8-4aa7-a8ff-f092e17add89.jpeg";
		String zipName = "yasuobao03.zip";
		String encodedEntryURI = UrlSafeBase64.encodeToString(bucket + ":" + zipName);
		 String fops = "";//fops参数
	        try {
	            fops = "mkzip/2"
	                    + "/url/" + UrlSafeBase64.encodeToString(url1) 
	                    //+ "/alias/" + UrlSafeBase64.encodeToString(name1)
	                    + "/url/" + UrlSafeBase64.encodeToString(url2) 
	                    //+ "/alias/" + UrlSafeBase64.encodeToString(name2)
	                    + "/url/" + UrlSafeBase64.encodeToString(url3) 
	                    //+ "/alias/" + UrlSafeBase64.encodeToString(name3)
	                    + "|saveas/" + encodedEntryURI;

	            System.out.println(fops);
	            String id = operater.pfop(bucket, key, fops);
	            String purl = "http://api.qiniu.com/status/get/prefop?id=" + id;
	            //
	            System.out.println(purl);
	        } catch (Exception e) {
	        	e.printStackTrace();
	        }


	}
	 public static String encode(byte[] bstr){    
		   return new sun.misc.BASE64Encoder().encode(bstr);    
	 } 
}
