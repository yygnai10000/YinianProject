package yinian.utils.test;

import java.io.IOException;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;

public class QiNiuUpload {
	public static final String ACCESS_KEY = "2caeYdL9QSwjhpJc2v05LLgaOrk_Mc_HterAtD28";
	public static final String SECRET_KEY = "XQYY6AE3rhhp-ep9-xwOOUc2noRyAvXu8uLjkTMT"; 

	    //Ҫ�ϴ��Ŀռ�  
	    String bucketname = "yinian"; //��д�½����Ǹ��洢�ռ���������
	    //�ϴ�����ţ�󱣴���ļ���  
	    String key = "20171222.jpg";    
	    //�ϴ��ļ���·��  
	    String FilePath = "/Users/liukai/Sites/lkzhb.mp4";  //����Ҫ�ϴ��ļ�·��  

	    //��Կ����  
	    Auth auth = Auth.create(ACCESS_KEY, SECRET_KEY);  
	    //�����ϴ�����  
	    UploadManager uploadManager = new UploadManager();  

	    //���ϴ���ʹ��Ĭ�ϲ��ԣ�ֻ��Ҫ�����ϴ��Ŀռ����Ϳ�����  
	    public String getUpToken(){  
	        return auth.uploadToken(bucketname);  
	    }  
	    //��ͨ�ϴ�  
	    public void upload(String key) throws IOException{  
	      try {  
	    	  System.out.println("��ʼ");
	        //����put�����ϴ�  
	        Response res = uploadManager.put(FilePath, key, getUpToken());  
	        //��ӡ���ص���Ϣ  

	System.out.println(res.isOK());

	System.out.println(res.bodyString());   
	        } catch (QiniuException e) {  
	            Response r = e.response;  
	            // ����ʧ��ʱ��ӡ���쳣����Ϣ  
	            System.out.println(r.toString());  
	            try {  
	                //��Ӧ���ı���Ϣ  
	              System.out.println(r.bodyString());  
	            } catch (QiniuException e1) {  
	                //ignore  
	            }  
	        }
	      System.out.println("����");
	    }  
	    public static void main(String args[]) throws IOException{    
	    	//for(int i=0;i<10000;i++){
	    		new QiNiuUpload().upload("2017-12-25-01.mp4");  
	    		System.out.println("ok");
	    	//}
	    }  
}
