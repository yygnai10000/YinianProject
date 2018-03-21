package yinian.utils.test;
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
	public static void mkzip() {
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
			String url1=auth.privateDownloadUrl("http://7xpend.com1.z0.glb.clouddn.com/50cf5dcb-5cb8-4aa7-a8ff-f092e17add89.jpeg");
			String url2=auth.privateDownloadUrl("http://7xpend.com1.z0.glb.clouddn.com/34b7a2d7-2b6e-45fa-9843-bcb8858337e3.jpg");
			String url3=auth.privateDownloadUrl("http://7xpend.com1.z0.glb.clouddn.com/tmp_61cebdfda1a823ad5277215645d33246.jpg");
			String url4=auth.privateDownloadUrl("http://7xpend.com1.z0.glb.clouddn.com/tmp_1840006904o6zAJs7TrsuV9RMorlB_3dksq1YE0dc3d9fa7b52e5bf13c3d6a652f36070.png");
			String url5=auth.privateDownloadUrl("http://7xpend.com1.z0.glb.clouddn.com/tmp_2098bc70f3f25575034335d23b332890.mp4");
			//ѹ�������ļ�����
			String content = "";
			//for(FileInfo fileInfo : items){
				//ƴ��ԭʼ����
				//String url = "http://" + DOMAIN + QN_SEPARATOR + fileInfo.key;
				//���Ӽ��ܲ�����Base64���룬����ȥ��ǰ׺Ŀ¼��
			
				//System.out.println("url"+i+"  ="+UrlSafeBase64.encodeToString("url"+i));
				String safeUrl = "/url/" + UrlSafeBase64.encodeToString(url1);
				content += ((StringUtils.isBlank(content) ? "" : QN_NEWLINE) + safeUrl);
				safeUrl = "/url/" + UrlSafeBase64.encodeToString(url2);
				content += ((StringUtils.isBlank(content) ? "" : QN_NEWLINE) + safeUrl);
				safeUrl = "/url/" + UrlSafeBase64.encodeToString(url3);
				content += ((StringUtils.isBlank(content) ? "" : QN_NEWLINE) + safeUrl);
				safeUrl = "/url/" + UrlSafeBase64.encodeToString(url4);
				content += ((StringUtils.isBlank(content) ? "" : QN_NEWLINE) + safeUrl);
				safeUrl = "/url/" + UrlSafeBase64.encodeToString(url5);
				content += ((StringUtils.isBlank(content) ? "" : QN_NEWLINE) + safeUrl);
				
			//}
			System.out.println(content);

			//�����ļ�·��
			String txtKey = "yinian/" + TXT_NAME;
			//���������ļ�token�������ϴ���
			String uptoken = auth.uploadToken(BUCKET, txtKey, 3600, new StringMap().put("insertOnly", 0));
			//�ϴ������ļ�
			Response res = uploadManager.put(content.getBytes(), txtKey, uptoken);

			//Ĭ��utf-8������������ʾ���룬�޸�Ϊgbk
			String fops = "mkzip/4/encoding/" + UrlSafeBase64.encodeToString("utf-8") + "|saveas/" + UrlSafeBase64.encodeToString(BUCKET + ":"  + "4.zip");

			OperationManager operater = new OperationManager(auth);

			StringMap params = new StringMap();
			//ѹ����ɺ���ţ�ص�URL
			//params.put("notifyURL", "http://120.26.69.171/YinianProject/callBack.php");
  
			String id = operater.pfop(BUCKET, txtKey, fops, params);
			String purl = "http://api.qiniu.com/status/get/prefop?id="+id;
			System.out.println(purl);
		} catch (QiniuException e) {
			Response res = e.response;
			System.out.println(res);
			try {
				System.out.println(res.bodyString());
			} catch (QiniuException e1) {
				e1.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		String prefix = "public/download/";
		mkzip();
	}
}
