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

	// 简单上传，使用默认策略
	public String getSimpleToken() {
		String token = auth.uploadToken(CommonParam.openBucket);
		return token;
	}

	// 覆盖上传
	public String getCoverToken(String key) {
		String token = auth.uploadToken(CommonParam.openBucket, key);
		return token;
	}

	// 获取公共空间上传token
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

	// 获取私有空间上传token
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

	// 获取下载token
	public String getDownloadToken(String url) {
		// 默认有效时长：3600秒
		String urlSigned = auth.privateDownloadUrl(url, 3600 * 240);
		return urlSigned;
		// //指定时长
		// String urlSigned2 = auth.privateDownloadUrl(url, 3600 * 24);
	}

	// 移动单个文件
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
	 * 上传文件到公开空间
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
	 * 上传文件到私有空间
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
