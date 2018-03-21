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
	 * 七牛回调URL
	 */
	public static final String NOTIFY_URL = "*******";
	/**
	 * 七牛间隔符
	 */
	public static final String QN_SEPARATOR = "/";
	/**
	 * txt换行符
	 */
	public static final String QN_NEWLINE = "\n";
	/**
	 * 索引文件名称
	 */
	public static final String TXT_NAME = "index.txt";    

	/**
	 * @Description: 大量文件压缩
	 * @author ljwang
	 * @date 2017年9月5日
	 */
	public static void mkzip() {
		//密钥配置
		Auth auth = Auth.create(ACCESS_KEY, SECRET_KEY);

		//自动识别要上传的空间(bucket)的存储区域是华东、华北、华南。
//		Zone z = Zone.zone0();
//		Configuration c = new Configuration(z);

		//实例化一个BucketManager对象
		BucketManager bucketManager = new BucketManager(auth);
		
		//创建上传对象
		UploadManager uploadManager = new UploadManager();

		try {
			//调用listFiles方法列举指定空间的指定文件
			//参数一：bucket    空间名
			//参数二：prefix    文件名前缀
			//参数三：marker    上一次获取文件列表时返回的 marker
			//参数四：limit     每次迭代的长度限制，最大1000，推荐值 100
			//参数五：delimiter 指定目录分隔符，列出所有公共前缀（模拟列出目录效果）。缺省值为空字符串
			//FileListing fileListing = bucketManager.listFiles(BUCKET, prefix, null, 100, null);
			//FileInfo[] items = fileListing.items;
			String url1=auth.privateDownloadUrl("http://7xpend.com1.z0.glb.clouddn.com/50cf5dcb-5cb8-4aa7-a8ff-f092e17add89.jpeg");
			String url2=auth.privateDownloadUrl("http://7xpend.com1.z0.glb.clouddn.com/34b7a2d7-2b6e-45fa-9843-bcb8858337e3.jpg");
			String url3=auth.privateDownloadUrl("http://7xpend.com1.z0.glb.clouddn.com/tmp_61cebdfda1a823ad5277215645d33246.jpg");
			String url4=auth.privateDownloadUrl("http://7xpend.com1.z0.glb.clouddn.com/tmp_1840006904o6zAJs7TrsuV9RMorlB_3dksq1YE0dc3d9fa7b52e5bf13c3d6a652f36070.png");
			String url5=auth.privateDownloadUrl("http://7xpend.com1.z0.glb.clouddn.com/tmp_2098bc70f3f25575034335d23b332890.mp4");
			//压缩索引文件内容
			String content = "";
			//for(FileInfo fileInfo : items){
				//拼接原始链接
				//String url = "http://" + DOMAIN + QN_SEPARATOR + fileInfo.key;
				//链接加密并进行Base64编码，别名去除前缀目录。
			
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

			//索引文件路径
			String txtKey = "yinian/" + TXT_NAME;
			//生成索引文件token（覆盖上传）
			String uptoken = auth.uploadToken(BUCKET, txtKey, 3600, new StringMap().put("insertOnly", 0));
			//上传索引文件
			Response res = uploadManager.put(content.getBytes(), txtKey, uptoken);

			//默认utf-8，但是中文显示乱码，修改为gbk
			String fops = "mkzip/4/encoding/" + UrlSafeBase64.encodeToString("utf-8") + "|saveas/" + UrlSafeBase64.encodeToString(BUCKET + ":"  + "4.zip");

			OperationManager operater = new OperationManager(auth);

			StringMap params = new StringMap();
			//压缩完成后，七牛回调URL
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
