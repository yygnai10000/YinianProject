package yinian.utils;

import yinian.common.CommonParam;

import com.qiniu.common.QiniuException;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;

public class QiniuOperate {

	private Auth auth = Auth.create(CommonParam.accessKey,
			CommonParam.secretKey);
	private BucketManager bucketManager = new BucketManager(auth);

	// ���ϴ���ʹ��Ĭ�ϲ���
	public String getSimpleToken() {
		String token = auth.uploadToken(CommonParam.openBucket);
		return token;
	}

	// �����ϴ�
	public String getCoverToken(String key) {
		String token = auth.uploadToken(CommonParam.openBucket, key);
		return token;
	}

	// ��ȡ�����ռ��ϴ�token
	public String getUploadToken() {
		return auth
				.uploadToken(
						CommonParam.openBucket,
						null,
						3600 * 24,
						new StringMap()
								.putNotEmpty(
										"returnBody",
										"{\"key\": $(key), \"hash\": $(etag), \"width\": $(imageInfo.width), \"height\": $(imageInfo.height)}"));
	}

	// ��ȡ˽�пռ��ϴ�token
	public String getPrivateUploadToken() {
		return auth
				.uploadToken(
						CommonParam.privateBucket,
						null,
						3600 * 24,
						new StringMap()
								.putNotEmpty(
										"returnBody",
										"{\"key\": $(key), \"hash\": $(etag), \"width\": $(imageInfo.width), \"height\": $(imageInfo.height)}"));
	}

	// ��ȡ����token
	public String getDownloadToken(String url) {
		// Ĭ����Чʱ����3600��
		String urlSigned = auth.privateDownloadUrl(url, 3600 * 240);
		return urlSigned;
		// //ָ��ʱ��
		// String urlSigned2 = auth.privateDownloadUrl(url, 3600 * 24);
	}

	// �ƶ������ļ�
	public boolean CopySingleFile(String oldBucket, String newBucket,
			String oldKey, String newKey) {
		try {
			bucketManager.copy(oldBucket, oldKey, newBucket, newKey);
			return true;
		} catch (QiniuException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

	}

	/**
	 * �ϴ��ļ��������ռ�
	 * 
	 * @param filePath
	 * @param fileName
	 */
	public void uploadFileToOpenSpace(String filePath, String fileName) {
		QiniuOperate operate = new QiniuOperate();
		String token = operate.getUploadToken();
		UploadManager upload = new UploadManager();
		try {
			upload.put(filePath, fileName, token);
		} catch (QiniuException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	/**
	 * �ϴ��ļ���˽�пռ�
	 * 
	 * @param filePath
	 * @param fileName
	 */
	public void uploadFileToPrivateSpace(String filePath, String fileName) {
		QiniuOperate operate = new QiniuOperate();
		String token = operate.getPrivateUploadToken();
		UploadManager upload = new UploadManager();
		try {
			upload.put(filePath, fileName, token);
		} catch (QiniuException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
