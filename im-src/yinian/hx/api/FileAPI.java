package yinian.hx.api;

/**
 * This interface is created for RestAPI of File Upload and Download, it should
 * be synchronized with the API list.
 * 
 * @author Eric23 2016-01-05
 * @see http://docs.easemob.com/doku.php?id=start:100serverintegration:
 *      40fileoperation
 */
public interface FileAPI {

	/**
	 * 上传文件 <br>
	 * POST
	 * 
	 * @param file
	 *            上传的文件对象，可以是地�?、流等，以实现类为准
	 * @return
	 */
	Object uploadFile(Object file);

	/**
	 * 下载文件 <br>
	 * GET
	 * 
	 * @param fileUUID
	 *            文件唯一标识，从上传Response-entities-uuid中获�?
	 * @param shareSecret
	 *            文件访问秘钥，从上传Response-entities-share-secret中获�?
	 * @param isThumbnail
	 *            ，如果下载图片，是否为缩略图
	 * @return
	 */
	Object downloadFile(String fileUUID, String shareSecret, Boolean isThumbnail);
}
