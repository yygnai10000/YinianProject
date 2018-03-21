package yinian.utils;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.draw.ComposePicture;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

public class SmallAppQRCode {

	private static YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类

	/**
	 * 获取小程序二维码
	 * 
	 * @return
	 */
	public String GetSmallAppQRCodeURL(String type, Object value) {
		// 获取accessToken
		String accessToken = GetSmallAppAccessToken();
		// 获取二维码并保存到云端
		String fileName = (type + "_" + value + ".jpg");
		String QRCodeURL = GetAndSaveQRCode(accessToken, type, value, fileName);
		// 合成二维码并保存到云端
		ComposePicture compose = new ComposePicture();
		QRCodeURL = compose.ComposeShareQRCode(QRCodeURL, type, (String) value);
		System.out.println(QRCodeURL);
		// 获取网络路径
		String[] temp = QRCodeURL.split("/");
		int length = temp.length;
		String url = CommonParam.qiniuOpenAddress + temp[length - 1];
		return url;
	}
	/**
	 * 获取小程序二维码 testController 使用，生成方形的动态分享二维码
	 * 
	 * @return
	 */
	public String GetSmallAppQRCodeURLByLkTest(String type, Object value,String ename) {
		// 获取accessToken
		String accessToken = GetSmallAppAccessToken();
		// 获取二维码并保存到云端
		String fileName = ( value + ".jpg");
		String QRCodeURL = GetAndSaveQRCodeByLktest(accessToken, type, value, fileName);
		// 合成二维码并保存到云端
		ComposePicture compose = new ComposePicture();
		QRCodeURL = compose.ComposeShareQRCodeLkTest(QRCodeURL, type, (String) value.toString(),ename);
		System.out.println(QRCodeURL);
		// 获取网络路径
		String[] temp = QRCodeURL.split("/");
		int length = temp.length;
		String url = CommonParam.qiniuOpenAddress + temp[length - 1];
		return url;
	}
	/**
	 * 获取小程序二维码(圆形) by lk 
	 * 
	 * @return
	 */
	public String GetSmallAppRoundQRCodeURL(String type, Object value) {
		//System.out.println("----------------------lk ----------------------");
		// 获取accessToken
		String accessToken = GetSmallAppAccessToken();
		// 获取二维码并保存到云端
		String fileName = (type + "_" + value + ".jpg");
		String QRCodeURL = GetAndSaveRoundQRCode(accessToken, type, value, fileName);
		// 合成二维码并保存到云端
		ComposePicture compose = new ComposePicture();
		QRCodeURL = compose.ComposeShareQRCode(QRCodeURL, type, (String) value);
		System.out.println(QRCodeURL);
		// 获取网络路径
		String[] temp = QRCodeURL.split("/");
		int length = temp.length;
		String url = CommonParam.qiniuOpenAddress + temp[length - 1];
		return url;
	}
	/**
	 * 获取小程序二维码plan2 by lk 
	 * 
	 * @return
	 */
	public String GetSmallAppQRCodeURLPlan2(String type, Object value) {
		// 获取accessToken
		String accessToken = GetSmallAppAccessToken();
		// 获取二维码并保存到云端
		String fileName = (type + "_" + value + ".jpg");
		String QRCodeURL = GetAndSaveQRCodePlan2(accessToken, type, value, fileName);
		if(QRCodeURL!=null&&QRCodeURL.indexOf("QRCodeError.png")!=-1) {
			return QRCodeURL;
		}else {
			// 合成二维码并保存到云端
			ComposePicture compose = new ComposePicture();
			QRCodeURL = compose.ComposeShareQRCode(QRCodeURL, type, value.toString());
			System.out.println(QRCodeURL);
			// 获取网络路径
			String[] temp = QRCodeURL.split("/");
			int length = temp.length;
			String url = CommonParam.qiniuOpenAddress + temp[length - 1];
			return url;
		}
	}
	//5460577相册，使用统一底图
	public String GetSmallAppQRCodeURLPlan2_5460577(String type, Object value) {
		// 获取accessToken
		String accessToken = GetSmallAppAccessToken();
		// 获取二维码并保存到云端
		String fileName = (type + "_" + value + ".jpg");
		String QRCodeURL = GetAndSaveQRCodePlan2(accessToken, type, value, fileName);
		if(QRCodeURL!=null&&QRCodeURL.indexOf("QRCodeError.png")!=-1) {
			return QRCodeURL;
		}else {
			// 合成二维码并保存到云端
			ComposePicture compose = new ComposePicture();
			QRCodeURL = compose.ComposeShareQRCode_5460577(QRCodeURL, type, value.toString());
			System.out.println(QRCodeURL);
			// 获取网络路径
			String[] temp = QRCodeURL.split("/");
			int length = temp.length;
			String url = CommonParam.qiniuPrivateAddress + temp[length - 1];
			QiniuOperate operate = new QiniuOperate();
			return operate.getDownloadToken(url);
		}
	}
	/**
	 * 合成分享圆形二维码  并保存到云端---ly
	 */	
	public String GetShareSmallAppQRCodeURL(String type, Object value,String ename,String picUrl,String mpicUrl) {
		// 获取accessToken
		String accessToken = GetSmallAppAccessToken();
		// 获取二维码并保存到云端
		UUID uuid = UUID.randomUUID();
		String fileName = (uuid + ".jpg");
		String QRCodeURL = GetShareAndSaveQRCode(accessToken, type, value, fileName);
		if(QRCodeURL!=null&&QRCodeURL.indexOf("QRCodeError.png")!=-1) {
			return QRCodeURL;
		}
		// 合成二维码并保存到云端
		QRCodeURL = ComposeLocalShareQRCode(QRCodeURL, type, value.toString(),ename,picUrl,mpicUrl);
		System.out.println(QRCodeURL);
		// 获取网络路径
		String[] temp = QRCodeURL.split("/");
		int length = temp.length;
		String url = CommonParam.qiniuPrivateAddress  + temp[length - 1];
		QiniuOperate operate = new QiniuOperate();
		return operate.getDownloadToken(url);
	}
//	/**
//	 * 获取小程序二维码plan1 by lk 本地生成
//	 * 
//	 * @return
//	 */
//	public String GetLocalSmallAppQRCodeURLPlan1(String type, Object value) {
//		// 获取accessToken
//		String accessToken = GetSmallAppAccessToken();
//		// 获取二维码并保存到云端
//		String fileName = (type + "_" + value + ".jpg");
//		String QRCodeURL = GetLocalAndSaveQRCodePlan1(accessToken, type, value, fileName);
//		// 合成二维码并保存到云端
//		ComposePicture compose = new ComposePicture();
//		QRCodeURL = compose.ComposeShareQRCodeLkTest(QRCodeURL, type, value.toString());
//		System.out.println(QRCodeURL);
//		// 获取网络路径
//		String[] temp = QRCodeURL.split("/");
//		int length = temp.length;
//		String url = CommonParam.qiniuOpenAddress + temp[length - 1];
//		return url;
//	}
	/**
	 * 获取小程序二维码plan2 by lk 本地生成
	 * 
	 * @return
	 */
	public String GetLocalSmallAppQRCodeURLPlan2(String type, Object value,String ename) {
		// 获取accessToken
		String accessToken = GetSmallAppAccessToken();
		// 获取二维码并保存到云端
		String fileName = (type + "_" + value + ".jpg");
		String QRCodeURL = GetLocalAndSaveQRCodePlan2(accessToken, type, value, fileName);
		// 合成二维码并保存到云端
		ComposePicture compose = new ComposePicture();
		QRCodeURL = compose.ComposeLocalShareQRCode(QRCodeURL, type, value.toString(),ename);
		System.out.println(QRCodeURL);
		// 获取网络路径
		String[] temp = QRCodeURL.split("/");
		int length = temp.length;
		String url = CommonParam.qiniuOpenAddress + temp[length - 1];
		return url;
	}
	/**
	 * 获取小程序AccessToken
	 * 
	 * @return
	 */
	public static String GetSmallAppAccessToken() {
		Record judge = Db.findFirst("select * from ynTemp where id=43");//获取accessToken
		
		// 获取accessToken
		String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + CommonParam.appID
				+ "&secret=" + CommonParam.secretID;
		String result="";
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if(null!=judge.get("remark")){
			if(System.currentTimeMillis()-judge.getTimestamp("time").getTime()<1000*7200){
			//if(System.currentTimeMillis()-judge.getTimestamp("time").getTime()<1000*7200 && accesstokenIsAvailable(judge.get("remark"))){
				result=judge.get("remark");
			}else{
				result = dataProcess.sentNetworkRequest(url);
				Db.update("update ynTemp set `remark`='"+result+"',time='"+df.format(new Date())+"' where id=43  ");
			}
		}else{
			result = dataProcess.sentNetworkRequest(url);
			Db.update("update ynTemp set `remark`='"+result+"',time='"+df.format(new Date())+"' where id=43  ");
		}
		//String result = dataProcess.sentNetworkRequest(url);
		JSONObject jo = JSONObject.parseObject(result);
		String accessToken = jo.getString("access_token");
		return accessToken;
	}
	
	/**
	 * 判断access_token是否有效,true:有效，false:失效
	 */
	public static boolean accesstokenIsAvailable(String remark) {
		String url = "https://api.weixin.qq.com/cgi-bin/getcallbackip?access_token=";
		JSONObject jo = JSONObject.parseObject(remark);
		String result = dataProcess.sentNetworkRequest(url+jo.getString("access_token"));
		JSONObject jo2 = JSONObject.parseObject(result);
		if(jo2.getString("errcode")!=null) {
			return false;
		}else {
			return true;
		}
	}

	/**
	 * 获取并保存小程序二维码
	 */
	private static String GetAndSaveQRCodeByLktest(String accessToken, String type, Object value, String fileName) {

		String url = "https://api.weixin.qq.com/cgi-bin/wxaapp/createwxaqrcode?access_token=" + accessToken;
		
		JSONObject jo = new JSONObject();
		// 根据类型拼接请求参数路径
		String path = "";
		switch (type) {
		case "space":
			path = "pages/viewscoll/viewscoll?groupid=" + value + "&port=空间二维码&fromSpaceID=" + value + "&from=timeline";
			break;
		case "spaceEvent":
			path = "pages/eventdetail/eventdetail?eid=" + value + "&port=动态详情分享&efrom=share";
			break;
		case "puzzle":
			path = "pages/pingtu/pingtu?id=" + value + "&port=小程序拼图";
			break;
		case "temp":
			path = "pages/commonpage/lookBurnAfterRead/look?tempid=" + value + "&port=阅后即焚";
			break;
		case "industry":
			path = "pages/smallUtilPages/hangyezhenxiang/index?port=行业真相";
			break;
		case "encourage":
			String[] array = value.toString().split("-");
			path = "pages/index/index?shareUserid=" + array[0] + "&shCode=" + array[1] + "&port=小程序二维码&fromUserID="
					+ array[0] + "";
			break;
		}
		jo.put("path", path);	
		String param = jo.toJSONString();
		String QRCodeURL = SentRequestToGetQRCodeByLkTest(url, param, fileName);
		return QRCodeURL;
	}
	/**
	 * 获取并保存小程序二维码
	 */
	private static String GetAndSaveQRCode(String accessToken, String type, Object value, String fileName) {

		String url = "https://api.weixin.qq.com/cgi-bin/wxaapp/createwxaqrcode?access_token=" + accessToken;
		
		JSONObject jo = new JSONObject();
		// 根据类型拼接请求参数路径
		String path = "";
		switch (type) {
		case "space":
			path = "pages/viewscoll/viewscoll?groupid=" + value + "&port=空间二维码&fromSpaceID=" + value + "&from=timeline";
			break;
		case "spaceEvent":
			path = "pages/eventdetail/eventdetail?eid=" + value + "&port=动态详情分享&efrom=share";
			break;
		case "puzzle":
			path = "pages/pingtu/pingtu?id=" + value + "&port=小程序拼图";
			break;
		case "temp":
			path = "pages/commonpage/lookBurnAfterRead/look?tempid=" + value + "&port=阅后即焚";
			break;
		case "industry":
			path = "pages/smallUtilPages/hangyezhenxiang/index?port=行业真相";
			break;
		case "encourage":
			String[] array = value.toString().split("-");
			path = "pages/index/index?shareUserid=" + array[0] + "&shCode=" + array[1] + "&port=小程序二维码&fromUserID="
					+ array[0] + "";
			break;
		}
		jo.put("path", path);		
		String param = jo.toJSONString();
		String QRCodeURL = SentRequestToGetQRCode(url, param, fileName);
		return QRCodeURL;
	}
	/**
	 * 获取并保存小程序二维码(圆)
	 */
	private static String GetAndSaveRoundQRCode(String accessToken, String type, Object value, String fileName) {
		
		String url = "https://api.weixin.qq.com/wxa/getwxacode?access_token=" + accessToken;
		JSONObject jo = new JSONObject();
		// 根据类型拼接请求参数路径
		String path = "";
		switch (type) {
		
		case "spaceEvent":
			path = "pages/eventdetail/eventdetail?eid=" + value + "&port=动态详情分享&efrom=share";
			break;		
		}
		jo.put("path", path);
		//by lk test 
		jo.put("width", 300);
		jo.put("auto_color", true);
		String param = jo.toJSONString();
		String QRCodeURL = SentRequestToGetRoundQRCode(url, param, fileName);
		return QRCodeURL;
	}
	/**
	 * 获取并保存小程序二维码
	 */
	private static String GetAndSaveQRCodePlan2(String accessToken, String type, Object value, String fileName) {

		String url = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + accessToken;
		
		JSONObject jo = new JSONObject();
		// 根据类型拼接请求参数路径
		String path = "";
		String scene="";
		switch (type) {
		case "eventdetail2":
			path = "pages/eventdetail2/eventdetail2";
			scene="port=e&efrom=share&eid="+value;
			break;
		case "spaceQR":
			path = "pages/viewscoll/viewscoll";
			scene="port=spaceQR&groupid="+value;
			break;
		}
				
		jo.put("page", path);	
		jo.put("scene", scene);
		jo.put("width", 430);
		jo.put("auto_color", true);
		String param = jo.toJSONString();
		String QRCodeURL = SentRequestToGetQRCodePlan2(url, param, fileName);
		return QRCodeURL;
	}
	/**
	 * 获取并保存小程序二维码 by lk 本地
	 */
	private static String GetLocalAndSaveQRCodePlan2(String accessToken, String type, Object value, String fileName) {

		String url = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + accessToken;
		
		JSONObject jo = new JSONObject();
		// 根据类型拼接请求参数路径
		String path = "";
		String scene="";
		switch (type) {
		case "eventdetail2":
			path = "pages/eventdetail2/eventdetail2";
			scene="port=e&efrom=share&eid="+value;
			break;
		case "spaceQR":
			path = "pages/viewscoll/viewscoll";
			scene="port=spaceQR&groupid="+value;
			break;
//		case "index":
//			path = "pages/index/index";
//			scene="port="+value;
//			break;
		}
				
		jo.put("page", path);	
		jo.put("scene", scene);
		jo.put("width", 430);
		jo.put("auto_color", true);
		String param = jo.toJSONString();
		String QRCodeURL = SentLocalRequestToGetQRCodePlan2(url, param, fileName);
		return QRCodeURL;
	}
	/**
	 * 获取并保存小程序二维码 by lk 本地 方形
	 */
	private static String GetLocalAndSaveQRCodePlan1(String accessToken, String type, Object value, String fileName) {

		String url = "https://api.weixin.qq.com/cgi-bin/wxaapp/createwxaqrcode?access_token=" + accessToken;
		
		JSONObject jo = new JSONObject();
		// 根据类型拼接请求参数路径
		String path = "";
		switch (type) {
		case "space":
			path = "pages/viewscoll/viewscoll?groupid=" + value + "&port=空间二维码&fromSpaceID=" + value + "&from=timeline";
			break;
		case "spaceEvent":
			path = "pages/eventdetail/eventdetail?eid=" + value + "&port=动态详情分享&efrom=share";
			break;
		case "puzzle":
			path = "pages/pingtu/pingtu?id=" + value + "&port=小程序拼图";
			break;
		case "temp":
			path = "pages/commonpage/lookBurnAfterRead/look?tempid=" + value + "&port=阅后即焚";
			break;
		case "industry":
			path = "pages/smallUtilPages/hangyezhenxiang/index?port=行业真相";
			break;
		case "encourage":
			String[] array = value.toString().split("-");
			path = "pages/index/index?shareUserid=" + array[0] + "&shCode=" + array[1] + "&port=小程序二维码&fromUserID="
					+ array[0] + "";
			break;
		}
		jo.put("path", path);		
		String param = jo.toJSONString();
		String QRCodeURL = SentLocalRequestToGetQRCodePlan2(url, param, fileName);
		return QRCodeURL;
	}
	/**
	 * 向指定 URL发送POST方法的请求到微信小程序的二维码并保存 lk 测试用
	 * 
	 * @param url
	 *            发送请求的 URL
	 * @param param
	 *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
	 * @return 所代表远程资源的响应结果
	 */
	private static String SentRequestToGetQRCodePlan2(String url, String param, String fileName) {
		PrintWriter out = null;
		InputStream in = null;
		try {
			URL realUrl = new URL(url);
			// 打开和URL之间的连接
			URLConnection conn = realUrl.openConnection();
			// 设置通用的请求属性
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			// 发送POST请求必须设置如下两行
			conn.setDoOutput(true);
			conn.setDoInput(true);
			// 获取URLConnection对象对应的输出流
			out = new PrintWriter(conn.getOutputStream());
			// 发送请求参数
			out.print(param);
			// flush输出流的缓冲
			out.flush();

			// 获取输入流并获取图片
			in = conn.getInputStream();
			byte[] data = new byte[1024];
			int len = 0;
			FileOutputStream fileOutputStream = null;
			String path = CommonParam.tempPictureServerSavePath + fileName;
			
			//String path = CommonParam.pictureSaveLocalPath + fileName;
			fileOutputStream = new FileOutputStream(path);
			while ((len = in.read(data)) != -1) {
				fileOutputStream.write(data, 0, len);
			}
			String msg=new String(data);
			System.out.println(msg);
			
			// 关闭流
			fileOutputStream.close();
		//	JSONObject jo = JSONObject.parseObject(msg);
			if(msg.indexOf("errcode")!=-1) {
				return "http://oibl5dyji.bkt.clouddn.com/QRCodeError.png";//获取失败时返回"error"图片路径
			}else {
				QiniuOperate qiniu = new QiniuOperate();
				//qiniu.uploadFileToOpenSpace(CommonParam.pictureSaveLocalPath + fileName, fileName);
				qiniu.uploadFileToOpenSpace(CommonParam.tempPictureServerSavePath + fileName, fileName);
				return CommonParam.qiniuOpenAddress + fileName;
			}
		} catch (Exception e) {
			System.out.println("发送 POST 请求出现异常！" + e);
			e.printStackTrace();
		}
		// 使用finally块来关闭输出流、输入流
		finally {
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return null;
	}
	/**
	 * 向指定 URL发送POST方法的请求到微信小程序的二维码并保存 lk 测试用 本地
	 * 
	 * @param url
	 *            发送请求的 URL
	 * @param param
	 *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
	 * @return 所代表远程资源的响应结果
	 */
	private static String SentLocalRequestToGetQRCodePlan2(String url, String param, String fileName) {
		PrintWriter out = null;
		InputStream in = null;
		try {
			URL realUrl = new URL(url);
			// 打开和URL之间的连接
			URLConnection conn = realUrl.openConnection();
			// 设置通用的请求属性
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			// 发送POST请求必须设置如下两行
			conn.setDoOutput(true);
			conn.setDoInput(true);
			// 获取URLConnection对象对应的输出流
			out = new PrintWriter(conn.getOutputStream());
			// 发送请求参数
			out.print(param);
			// flush输出流的缓冲
			out.flush();

			// 获取输入流并获取图片
			in = conn.getInputStream();
			byte[] data = new byte[1024];
			int len = 0;
			FileOutputStream fileOutputStream = null;
			//String path = CommonParam.tempPictureServerSavePath + fileName;
			
			String path = CommonParam.pictureSaveLocalPath + fileName;
			fileOutputStream = new FileOutputStream(path);
			while ((len = in.read(data)) != -1) {
				fileOutputStream.write(data, 0, len);
			}
			String msg=new String(data);
			System.out.println(msg);
			// 关闭流
			fileOutputStream.close();
			//JSONObject jo = JSONObject.parseObject(msg);
			if(msg.indexOf("errcode")!=-1) {
				return "http://oibl5dyji.bkt.clouddn.com/QRCodeError.png";//获取失败时返回"error"图片路径
			}else {
				QiniuOperate qiniu = new QiniuOperate();
				qiniu.uploadFileToOpenSpace(CommonParam.pictureSaveLocalPath + fileName, fileName);
				//qiniu.uploadFileToOpenSpace(CommonParam.tempPictureServerSavePath + fileName, fileName);
				return CommonParam.qiniuOpenAddress + fileName;
			}
		} catch (Exception e) {
			System.out.println("发送 POST 请求出现异常！" + e);
			e.printStackTrace();
		}
		// 使用finally块来关闭输出流、输入流
		finally {
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return null;
	}
	/**
	 * 向指定 URL发送POST方法的请求到微信小程序的二维码并保存
	 * 
	 * @param url
	 *            发送请求的 URL
	 * @param param
	 *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
	 * @return 所代表远程资源的响应结果
	 */
	private static String SentRequestToGetQRCodeByLkTest(String url, String param, String fileName) {
		PrintWriter out = null;
		InputStream in = null;
		try {
			URL realUrl = new URL(url);
			// 打开和URL之间的连接
			URLConnection conn = realUrl.openConnection();
			// 设置通用的请求属性
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			// 发送POST请求必须设置如下两行
			conn.setDoOutput(true);
			conn.setDoInput(true);
			// 获取URLConnection对象对应的输出流
			out = new PrintWriter(conn.getOutputStream());
			// 发送请求参数
			out.print(param);
			// flush输出流的缓冲
			out.flush();

			// 获取输入流并获取图片
			in = conn.getInputStream();
			byte[] data = new byte[1024];
			int len = 0;
			FileOutputStream fileOutputStream = null;
			//String path = CommonParam.tempPictureServerSavePath + fileName;
			
			String path = CommonParam.pictureSaveLocalPath + fileName;
			fileOutputStream = new FileOutputStream(path);
			while ((len = in.read(data)) != -1) {
				fileOutputStream.write(data, 0, len);
			}
			// 关闭流
			fileOutputStream.close();
			QiniuOperate qiniu = new QiniuOperate();
			//qiniu.uploadFileToOpenSpace(CommonParam.pictureSaveLocalPath + fileName, fileName);
			//qiniu.uploadFileToOpenSpace(CommonParam.tempPictureServerSavePath + fileName, fileName);
			//return CommonParam.qiniuOpenAddress + fileName;
			return path;
		} catch (Exception e) {
			System.out.println("发送 POST 请求出现异常！" + e);
			e.printStackTrace();
		}
		// 使用finally块来关闭输出流、输入流
		finally {
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return null;
	}
	/**
	 * 向指定 URL发送POST方法的请求到微信小程序的二维码并保存
	 * 
	 * @param url
	 *            发送请求的 URL
	 * @param param
	 *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
	 * @return 所代表远程资源的响应结果
	 */
	private static String SentRequestToGetQRCode(String url, String param, String fileName) {
		PrintWriter out = null;
		InputStream in = null;
		try {
			URL realUrl = new URL(url);
			// 打开和URL之间的连接
			URLConnection conn = realUrl.openConnection();
			// 设置通用的请求属性
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			// 发送POST请求必须设置如下两行
			conn.setDoOutput(true);
			conn.setDoInput(true);
			// 获取URLConnection对象对应的输出流
			out = new PrintWriter(conn.getOutputStream());
			// 发送请求参数
			out.print(param);
			// flush输出流的缓冲
			out.flush();

			// 获取输入流并获取图片
			in = conn.getInputStream();
			byte[] data = new byte[1024];
			int len = 0;
			FileOutputStream fileOutputStream = null;
			String path = CommonParam.tempPictureServerSavePath + fileName;
			
			//String path = CommonParam.pictureSaveLocalPath + fileName;
			fileOutputStream = new FileOutputStream(path);
			while ((len = in.read(data)) != -1) {
				fileOutputStream.write(data, 0, len);
			}
			// 关闭流
			fileOutputStream.close();
			QiniuOperate qiniu = new QiniuOperate();
			//qiniu.uploadFileToOpenSpace(CommonParam.pictureSaveLocalPath + fileName, fileName);
			qiniu.uploadFileToOpenSpace(CommonParam.tempPictureServerSavePath + fileName, fileName);
			return CommonParam.qiniuOpenAddress + fileName;
		} catch (Exception e) {
			System.out.println("发送 POST 请求出现异常！" + e);
			e.printStackTrace();
		}
		// 使用finally块来关闭输出流、输入流
		finally {
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return null;
	}
	private static String SentRequestToGetRoundQRCode(String url, String param, String fileName) {
		PrintWriter out = null;
		InputStream in = null;
		try {
			URL realUrl = new URL(url);
			// 打开和URL之间的连接
			URLConnection conn = realUrl.openConnection();
			// 设置通用的请求属性
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			// 发送POST请求必须设置如下两行
			conn.setDoOutput(true);
			conn.setDoInput(true);
			// 获取URLConnection对象对应的输出流
			out = new PrintWriter(conn.getOutputStream());
			// 发送请求参数
			out.print(param);
			// flush输出流的缓冲
			out.flush();

			// 获取输入流并获取图片
			in = conn.getInputStream();
			byte[] data = new byte[1024];
			int len = 0;
			FileOutputStream fileOutputStream = null;
			//___lk test
			String path = CommonParam.tempPictureServerSavePath + fileName;
			
			//String path = CommonParam.pictureSaveLocalPath + fileName;
			fileOutputStream = new FileOutputStream(path);
			while ((len = in.read(data)) != -1) {
				fileOutputStream.write(data, 0, len);
			}
			// 关闭流
			fileOutputStream.close();
			QiniuOperate qiniu = new QiniuOperate();
			
			//qiniu.uploadFileToOpenSpace(CommonParam.pictureSaveLocalPath + fileName, fileName);
			//___lk test
			qiniu.uploadFileToOpenSpace(CommonParam.tempPictureServerSavePath + fileName, fileName);
			return CommonParam.qiniuOpenAddress + fileName;
		} catch (Exception e) {
			System.out.println("发送 POST 请求出现异常！" + e);
			e.printStackTrace();
		}
		// 使用finally块来关闭输出流、输入流
		finally {
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return null;
	}
	
	/**
	 * 获取并保存分享二维码(圆形 七牛云)---ly
	 */
	private static String GetShareAndSaveQRCode(String accessToken, String type, Object value, String fileName) {

		String url = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + accessToken;
		
		JSONObject jo = new JSONObject();
		// 根据类型拼接请求参数路径
		String path = "";
		String scene="";
		switch (type) {
		case "eventdetail2":
			path = "pages/eventdetail2/eventdetail2";
			scene="port=e&efrom=share&eid="+value;
			break;
		case "spaceQR":
			path = "pages/viewscoll/viewscoll";
			scene="port=spaceQR&groupid="+value;
			break;
		}
				
		jo.put("page", path);	
		jo.put("scene", scene);
		jo.put("width", 430);
		jo.put("auto_color", true);
		String param = jo.toJSONString();
		String QRCodeURL = SentShareRequestToGetQRCode(url, param, fileName);
		return QRCodeURL;
	}
	
	/**
	 * 合成圆形头像,和背景图变换---ly
	 */
	public String ComposeLocalShareQRCode(String QRCodeURL, String type, String data,String ename,String picUrl,String mpicUrl) {
		// 获取背景图
		String bottomPictureName = "";
		int x = 10;
		int y = 10;
		int width = 230;
		int height = 261;
		switch (type) {
		case "eventdetail2":
			//bottomPictureName = "spaceEventBackground.jpg";	
			bottomPictureName = "share.png";	
//			x = 99;
//			y = 1160;
//			width = 270;
//			height = 270;
			x = 156;
			y = 589;
			width = 220;
			height = 220;
			break;
		case "spaceQR":
			bottomPictureName = "share.png";	
			x = 156;
			y = 589;
			width = 220;
			height = 220;
			break;
		case "space":
			bottomPictureName = "spaceBackground.png";
			x = 160;
			y = 380;
			width = 430;
			height = 470;
			break;
		case "puzzle":
			bottomPictureName = "puzzleBackground.png";
			x = 85;
			y = 185;
			break;
		case "temp":
			bottomPictureName = "tempBackground.jpg";
			x = 70;
			y = 160;
			break;
		case "encourage":
			bottomPictureName = "encourageBackground.jpg";
			x = 200;
			y = 340;
			width = 350;
			height = 390;
			break;
		}
		try {
			//URL file = new URL(CommonParam.materialPath + bottomPictureName);
			//File file = new File("/Users/liukai/Sites/photo.png");
			File file = new File("D:/leiyu/list2/photo.png");
			//File QRCode = new File(QRCodeURL);
			URL QRCode = new URL(QRCodeURL);
			BufferedImage bi = ImageIO.read(file);
			Graphics2D graphics = bi.createGraphics();
			

			BufferedImage qrBi = ImageIO.read(QRCode);

			// 合成图片
			graphics.drawImage(qrBi, x, y, width, height, null);

			// 合成头像和背景图片
			if (!mpicUrl.equals("")&&!picUrl.equals("")) {
				//合成背景图片
				BufferedImage mpicBi = ImageIO.read(new URL(mpicUrl));
				int pheight = mpicBi.getHeight();
		        int pwidth = mpicBi.getWidth();
		        int[][] martrix = new int[3][3];
		        int[] values = new int[9];
		        for (int i = 0; i < pwidth; i++) {
		            for (int j = 0; j < pheight; j++) {
		                readPixel(mpicBi, i, j, values);
		                fillMatrix(martrix, values);
		                mpicBi.setRGB(i, j, avgMatrix(martrix));
		            }
		        }
				graphics.drawImage(mpicBi, 16, 16, 500, 500, null);				
				// 合成圆形头像
				BufferedImage picBi = ImageIO.read(new URL(picUrl));
				BufferedImage img = new BufferedImage(picBi.getWidth(), picBi.getHeight(), 
						BufferedImage.TYPE_INT_ARGB);
				Ellipse2D.Double shape = new Ellipse2D.Double(0, 0, picBi.getWidth(), picBi.getHeight());
				Graphics2D g2 = img.createGraphics();
				img=g2.getDeviceConfiguration().createCompatibleImage(picBi.getWidth(), picBi.getHeight());
				g2=img.createGraphics();
				g2.setComposite(AlphaComposite.Clear);
				g2.fill(new Rectangle(img.getWidth(), img.getHeight()));
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC,1.0f));
				g2.setClip(shape);
				g2.drawImage(picBi,0,0,null);
				g2.dispose();
				graphics.drawOval(195, 71, 142, 142);
				graphics.drawImage(img, 203, 79, 126, 126, null);
				
				// 合成昵称
				graphics.setColor(new Color(255,255, 255));
				// 设置字体
				Font font = new Font("simsun", Font.CENTER_BASELINE, 40);
				graphics.setFont(font);
				graphics.drawString(ename, 100, 260);
				Font font2 = new Font("simsun", Font.CENTER_BASELINE, 24);
				graphics.setFont(font2);
				graphics.drawString("这是我们的相册", 182, 320);
				Font font3 = new Font("simsun", Font.CENTER_BASELINE, 24);
				graphics.setFont(font3);
				graphics.drawString("我们都在", 218, 370);
				Font font4 = new Font("simsun", Font.CENTER_BASELINE, 30);
				graphics.setFont(font4);
				graphics.drawString("你也加入吧", 176, 420);
			}

			// 结束绘制
			graphics.dispose();
			// 保存到本地，再上传到云端
			//____lk test
			UUID uuid = UUID.randomUUID();
			//ImageIO.write(bi, "png", new File(CommonParam.tempPictureServerSavePath + type + uuid+ ".png"));
			ImageIO.write(bi, "png", new File(CommonParam.pictureSaveLocalPath + type + uuid+ ".png"));
			
			QiniuOperate qiniu = new QiniuOperate();
			//qiniu.uploadFileToPrivateSpace(CommonParam.tempPictureServerSavePath +  type + uuid + ".png",type + uuid + ".png");
			qiniu.uploadFileToPrivateSpace(CommonParam.pictureSaveLocalPath +  type + uuid + ".png",type + uuid + ".png");
			QRCodeURL = CommonParam.qiniuPrivateAddress + type  + uuid  + ".png";
			//DPIHandler dpi = new DPIHandler();
			// dpi.saveGridImage(bi, new File(QRCodeURL));

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return QRCodeURL;
	}
	
	/**
	 * 将分享圆形二维码保存到七牛云上--ly
	 */
	private static String SentShareRequestToGetQRCode(String url, String param, String fileName) {
		PrintWriter out = null;
		InputStream in = null;
		try {
			URL realUrl = new URL(url);
			// 打开和URL之间的连接
			URLConnection conn = realUrl.openConnection();
			// 设置通用的请求属性
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			// 发送POST请求必须设置如下两行
			conn.setDoOutput(true);
			conn.setDoInput(true);
			// 获取URLConnection对象对应的输出流
			out = new PrintWriter(conn.getOutputStream());
			// 发送请求参数
			out.print(param);
			// flush输出流的缓冲
			out.flush();

			// 获取输入流并获取图片
			in = conn.getInputStream();
			byte[] data = new byte[1024];
			int len = 0;
			FileOutputStream fileOutputStream = null;
			//String path = CommonParam.tempPictureServerSavePath + fileName;
			
			String path = CommonParam.pictureSaveLocalPath + fileName;
			fileOutputStream = new FileOutputStream(path);
			while ((len = in.read(data)) != -1) {
				fileOutputStream.write(data, 0, len);
			}
			String msg=new String(data);
			System.out.println(msg);
			// 关闭流
			fileOutputStream.close();
			//JSONObject jo = JSONObject.parseObject(msg);
			if(msg.indexOf("errcode")!=-1) {
				return "http://oibl5dyji.bkt.clouddn.com/QRCodeError.png";//获取失败时返回"error"图片路径
			}else {
				QiniuOperate qiniu = new QiniuOperate();
				qiniu.uploadFileToOpenSpace(CommonParam.pictureSaveLocalPath + fileName, fileName);
				//qiniu.uploadFileToOpenSpace(CommonParam.tempPictureServerSavePath + fileName, fileName);
				return CommonParam.qiniuOpenAddress + fileName;
			}
		} catch (Exception e) {
			System.out.println("发送 POST 请求出现异常！" + e);
			e.printStackTrace();
		}
		// 使用finally块来关闭输出流、输入流
		finally {
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return null;
	}
	
	private static void readPixel(BufferedImage img, int x, int y, int[] pixels) {
        int xStart = x - 1;
        int yStart = y - 1;
        int current = 0;
        for (int i = xStart; i < 3 + xStart; i++)
            for (int j = yStart; j < 3 + yStart; j++) {
                int tx = i;
                if (tx < 0) {
                    tx = -tx;

                } else if (tx >= img.getWidth()) {
                    tx = x;
                }
                int ty = j;
                if (ty < 0) {
                    ty = -ty;
                } else if (ty >= img.getHeight()) {
                    ty = y;
                }
                pixels[current++] = img.getRGB(tx, ty);

            }
    }

    private static void fillMatrix(int[][] matrix, int[] values) {
        int filled = 0;
        for (int i = 0; i < matrix.length; i++) {
            int[] x = matrix[i];
            for (int j = 0; j < x.length; j++) {
                x[j] = values[filled++];
            }
        }
    }

    private static int avgMatrix(int[][] matrix) {
        int r = 0;
        int g = 0;
        int b = 0;
        for (int i = 0; i < matrix.length; i++) {
            int[] x = matrix[i];
            for (int j = 0; j < x.length; j++) {
                if (j == 1) {
                    continue;
                }
                Color c = new Color(x[j]);
                r += c.getRed();
                g += c.getGreen();
                b += c.getBlue();
            }
        }
        return new Color(r / 8, g / 8, b / 8).getRGB();

    }
}
