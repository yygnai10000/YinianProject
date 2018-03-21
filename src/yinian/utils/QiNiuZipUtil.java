package yinian.utils;
import com.jfinal.plugin.activerecord.Record;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.processing.OperationManager;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.storage.model.FileListing;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.qiniu.util.UrlSafeBase64;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
public class QiNiuZipUtil {
		
	public static final String ACCESS_KEY = "2caeYdL9QSwjhpJc2v05LLgaOrk_Mc_HterAtD28";
	public static final String SECRET_KEY = "XQYY6AE3rhhp-ep9-xwOOUc2noRyAvXu8uLjkTMT";
	public static final String BUCKET = "yinian";
	public static final String DOMAIN = "*******";
	/**
	 * ��ţ�ص�URL
	 */
	public static final String NOTIFY_URL = "*******";
	/**
	 * ��ţ�����
	 */
	public static final String QN_SEPARATOR = "/";
	/**
	 * txt���з�
	 */
	public static final String QN_NEWLINE = "\n";
	/**
	 * �����ļ�����
	 */
	public static final String TXT_NAME = "index.txt";    

	/**
	 * @Description: �����ļ�ѹ��
	 * @author ljwang
	 * @date 2017��9��5��
	 */
	public static List<Record> mkzip(String[] picArray) {
		List<Record> list=new ArrayList<Record>();
		String downUrl="";
		String zipid ="";
		String zipName=UUID.randomUUID().toString().replaceAll("-", "")+".zip";
		QiniuOperate operate = new QiniuOperate();
		//��Կ����
		Auth auth = Auth.create(ACCESS_KEY, SECRET_KEY);

		//�Զ�ʶ��Ҫ�ϴ��Ŀռ�(bucket)�Ĵ洢�����ǻ��������������ϡ�
//		Zone z = Zone.zone0();
//		Configuration c = new Configuration(z);

		//ʵ����һ��BucketManager����
		BucketManager bucketManager = new BucketManager(auth);
		
		//�����ϴ�����
		UploadManager uploadManager = new UploadManager();

		try {
			//����listFiles�����о�ָ���ռ��ָ���ļ�
			//����һ��bucket    �ռ���
			//��������prefix    �ļ���ǰ׺
			//��������marker    ��һ�λ�ȡ�ļ��б�ʱ���ص� marker
			//�����ģ�limit     ÿ�ε����ĳ������ƣ����1000���Ƽ�ֵ 100
			//�����壺delimiter ָ��Ŀ¼�ָ������г����й���ǰ׺��ģ���г�Ŀ¼Ч������ȱʡֵΪ���ַ���
			//FileListing fileListing = bucketManager.listFiles(BUCKET, prefix, null, 100, null);
			//FileInfo[] items = fileListing.items;
			String content = "";
			for(int i=0;i<picArray.length;i++){
				String safeUrl = "/url/" + UrlSafeBase64.encodeToString(operate.getDownloadToken("http://7xpend.com1.z0.glb.clouddn.com/"+picArray[i]));
				content += ((StringUtils.isBlank(content) ? "" : QN_NEWLINE) + safeUrl);
			}			
			//System.out.println(content);

			//�����ļ�·��
			String txtKey = "yinian/" + TXT_NAME;
			//���������ļ�token�������ϴ���
			String uptoken = auth.uploadToken(BUCKET, txtKey, 3600, new StringMap().put("insertOnly", 0));
			//�ϴ������ļ�
			Response res = uploadManager.put(content.getBytes(), txtKey, uptoken);
			//Ĭ��utf-8������������ʾ���룬�޸�Ϊgbk
			String fops = "mkzip/4/encoding/" + UrlSafeBase64.encodeToString("utf-8") + "|saveas/" + UrlSafeBase64.encodeToString(BUCKET + ":"  + zipName);

			OperationManager operater = new OperationManager(auth);

			StringMap params = new StringMap();
			//ѹ����ɺ���ţ�ص�URL
			//params.put("notifyURL", NOTIFY_URL);
  
			String id = operater.pfop(BUCKET, txtKey, fops, params);
			if(null!=id&&!id.equals("")){
				downUrl=operate.getDownloadToken("http://7xpend.com1.z0.glb.clouddn.com/"+zipName);
				zipid =id;
			}
			
			//System.out.println(purl);
		} catch (QiniuException e) {
			Response res = e.response;
			System.out.println(res);
			try {
				System.out.println(res.bodyString());
			} catch (QiniuException e1) {
				e1.printStackTrace();
			}
		}finally{
			Record r=new Record();
			r.set("downUrl", downUrl);
			r.set("zipId", zipid);
			list.add(r);
			return list;
		}
	}
}
