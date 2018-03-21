package yinian.utils.test;

import java.util.UUID;

import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.util.Auth;


public class QiNiuCopy {
	public static final String ACCESS_KEY = "2caeYdL9QSwjhpJc2v05LLgaOrk_Mc_HterAtD28";
	public static final String SECRET_KEY = "XQYY6AE3rhhp-ep9-xwOOUc2noRyAvXu8uLjkTMT"; 
	public static void main(String[] a){
//		for(int i=0;i<10;i++){
//			String uuid = UUID.randomUUID().toString().replaceAll("-", "");
//			System.out.println("./qshell-darwin-x64 copy yinian 2015-12-25haha.avi yinian lk-"+uuid+".avi");
//		}
		String bucket = "yinian";
		Auth auth = Auth.create(ACCESS_KEY, SECRET_KEY);
//	
		BucketManager bucketManager = new BucketManager(auth);
		try {
//		    //单次批量请求的文件数量不得超过1000
////		    String[] keyList = new String[]{
////		            "qiniu.jpg",
////		            "qiniu.mp4",
////		            "qiniu.png",
////		    };
//		   
		    	BucketManager.Batch batchOperations = new BucketManager.Batch();
		   
		    	Response resp =bucketManager.batch(batchOperations.copy(bucket, " 2017-12-25-01.mp4", bucket, "lk0001.mp4"));
		    	 System.out.println(".mp4"+"   状态code: " + resp.statusCode);
//		    	//System.out.println(i);
//		    //}
////		    for(int i=0;i<100;i++){
////		    	Response resp =bucketManager.batch(batchOperations.copy(bucket, " 2017-12-25-01.mp4", bucket, "2017-12-25-lk"+i+".mp4"));
////		    	 System.out.println("2017-12-25-lk"+i+".mp4"+"   状态code: " + resp.statusCode);
////		    	System.out.println(i);
////		    }
//		    System.out.println("ok");
		} catch (Exception ex) {
		    System.err.println(ex.getStackTrace());
		}
	}
	public static void main_1(String[] a){
		//Configuration cfg = new Configuration(Zone.zone0());
		//...其他参数参考类注释
//		String accessKey = "your access key";
//		String secretKey = "your secret key";
		String bucket = "yinian";
		Auth auth = Auth.create(ACCESS_KEY, SECRET_KEY);
	
		BucketManager bucketManager = new BucketManager(auth);
		try {
		    //单次批量请求的文件数量不得超过1000
		    String[] keyList = new String[]{
		            "qiniu.jpg",
		            "qiniu.mp4",
		            "qiniu.png",
		    };
		   
		    BucketManager.Batch batchOperations = new BucketManager.Batch();
		    for(int i=0;i<100;i++){
		    	Response resp =bucketManager.batch(batchOperations.copy(bucket, " 2017-12-25-01.mp4", bucket, "2017-12-25-lk"+i+".mp4"));
		    	 System.out.println("2017-12-25-lk"+i+".mp4"+"   状态code: " + resp.statusCode);
		    	System.out.println(i);
		    }
		    System.out.println("ok");
		} catch (Exception ex) {
		    System.err.println(ex.getStackTrace());
		}
	}
}
