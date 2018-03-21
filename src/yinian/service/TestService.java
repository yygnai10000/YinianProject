package yinian.service;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
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
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Before;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.draw.ComposePicture;
import yinian.model.Points;
import yinian.model.PointsReceive;
import yinian.model.User;
import yinian.utils.QiniuOperate;

public class TestService {
	private static YinianDataProcess dataProcess = new YinianDataProcess();// ���ݴ�����
	/**
	 * �ж�access_token�Ƿ���Ч,true:��Ч��false:ʧЧ
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
	 * ��ȡС����AccessToken
	 * 
	 * @return
	 */
	public static String GetSmallAppAccessToken() {
		Record judge = Db.findFirst("select * from ynTemp where id=43");//��ȡaccessToken
		
		// ��ȡaccessToken
		String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + CommonParam.appID
				+ "&secret=" + CommonParam.secretID;
		String result="";
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if(null!=judge.get("remark")){
			//if(System.currentTimeMillis()-judge.getTimestamp("time").getTime()<1000*7200){
			//if(System.currentTimeMillis()-judge.getTimestamp("time").getTime()<1000*7200 && accesstokenIsAvailable(judge.get("remark"))){
			if(System.currentTimeMillis()-judge.getTimestamp("time").getTime()<1000*7200){
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
	 * ��ȡ������С�����ά�� by lk ����
	 */
	private static String GetLocalAndSaveQRCodePlan2(String accessToken, String type, Object value, String fileName) {

		String url = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + accessToken;
		
		JSONObject jo = new JSONObject();
		// ��������ƴ���������·��
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
		String QRCodeURL = SentLocalRequestToGetQRCodePlan2(url, param, fileName);
		return QRCodeURL;
	}
	
	/**
	 * ��ȡ������С�����ά��(Բ�� ��ţ��)
	 */
	private static String GetLocalAndSaveQRCodePlan3(String accessToken, String type, Object value, String fileName) {

		String url = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + accessToken;
		
		JSONObject jo = new JSONObject();
		// ��������ƴ���������·��
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
		String QRCodeURL = SentLocalRequestToGetQRCodePlan3(url, param, fileName);
		return QRCodeURL;
	}
	
	/**
	 * ����ֽ���ά��
	 */
	private static String GetShareAndSaveQRCode(String accessToken, String type, Object value,String a, String fileName) {

		String url = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + accessToken;
		
		JSONObject jo = new JSONObject();
		// ��������ƴ���������·��
		String path = "";
		String scene="";
		switch (type) {
		case "eventdetail2":
			path = "pages/eventdetail2/eventdetail2";
			scene="port=e&efrom=share&eid="+value;
			break;
		case "spaceQR":
			path = "pages/viewscoll/viewscoll";
			scene="groupid="+value+"&zj="+a;
			break;
		}
				
		jo.put("page", path);	
		jo.put("scene", scene);
		jo.put("width", 430);
		jo.put("auto_color", true);
		String param = jo.toJSONString();
		String QRCodeURL = SentLocalRequestToGetQRCodePlan3(url, param, fileName);
		return QRCodeURL;
	}
	/**
	 * ��ָ�� URL����POST����������΢��С����Ķ�ά�벢���� lk ������ ����
	 * 
	 * @param url
	 *            ��������� URL
	 * @param param
	 *            ����������������Ӧ���� name1=value1&name2=value2 ����ʽ��
	 * @return ������Զ����Դ����Ӧ���
	 */
	private static String SentLocalRequestToGetQRCodePlan2(String url, String param, String fileName) {
		PrintWriter out = null;
		InputStream in = null;
		try {
			URL realUrl = new URL(url);
			// �򿪺�URL֮�������
			URLConnection conn = realUrl.openConnection();
			// ����ͨ�õ���������
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			// ����POST�������������������
			conn.setDoOutput(true);
			conn.setDoInput(true);
			// ��ȡURLConnection�����Ӧ�������
			out = new PrintWriter(conn.getOutputStream());
			// �����������
			out.print(param);
			// flush������Ļ���
			out.flush();

			// ��ȡ����������ȡͼƬ
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
			// �ر���
			fileOutputStream.close();
			//JSONObject jo = JSONObject.parseObject(msg);
			if(msg.indexOf("errcode")!=-1) {
				return "http://oibl5dyji.bkt.clouddn.com/QRCodeError.png";//��ȡʧ��ʱ����"error"ͼƬ·��
			}else {
				QiniuOperate qiniu = new QiniuOperate();
				qiniu.uploadFileToOpenSpace(CommonParam.pictureSaveLocalPath + fileName, fileName);
				//qiniu.uploadFileToOpenSpace(CommonParam.tempPictureServerSavePath + fileName, fileName);
				return path;
			}
		} catch (Exception e) {
			System.out.println("���� POST ��������쳣��" + e);
			e.printStackTrace();
		}
		// ʹ��finally�����ر��������������
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
	 * ����ά�뱣�浽��ţ����--ly
	 */
	private static String SentLocalRequestToGetQRCodePlan3(String url, String param, String fileName) {
		PrintWriter out = null;
		InputStream in = null;
		try {
			URL realUrl = new URL(url);
			// �򿪺�URL֮�������
			URLConnection conn = realUrl.openConnection();
			// ����ͨ�õ���������
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			// ����POST�������������������
			conn.setDoOutput(true);
			conn.setDoInput(true);
			// ��ȡURLConnection�����Ӧ�������
			out = new PrintWriter(conn.getOutputStream());
			// �����������
			out.print(param);
			// flush������Ļ���
			out.flush();

			// ��ȡ����������ȡͼƬ
			in = conn.getInputStream();
			byte[] data = new byte[1024];
			int len = 0;
			FileOutputStream fileOutputStream = null;
			String path = CommonParam.tempPictureServerSavePath + fileName;
			
//			String path = CommonParam.pictureSaveLocalPath + fileName;
			fileOutputStream = new FileOutputStream(path);
			while ((len = in.read(data)) != -1) {
				fileOutputStream.write(data, 0, len);
			}
			String msg=new String(data);
			System.out.println(msg);
			// �ر���
			fileOutputStream.close();
			//JSONObject jo = JSONObject.parseObject(msg);
			if(msg.indexOf("errcode")!=-1) {
				return "http://oibl5dyji.bkt.clouddn.com/QRCodeError.png";//��ȡʧ��ʱ����"error"ͼƬ·��
			}else {
				QiniuOperate qiniu = new QiniuOperate();
				//qiniu.uploadFileToOpenSpace(CommonParam.pictureSaveLocalPath + fileName, fileName);
				qiniu.uploadFileToOpenSpace(CommonParam.tempPictureServerSavePath + fileName, fileName);
				return CommonParam.qiniuOpenAddress + fileName;
			}
		} catch (Exception e) {
			System.out.println("���� POST ��������쳣��" + e);
			e.printStackTrace();
		}
		// ʹ��finally�����ر��������������
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

	public String GetLocalSmallAppQRCodeURLPlan2(String type, Object value,String ename,String picUrl,String mpicUrl) {
		// ��ȡaccessToken
		String accessToken = GetSmallAppAccessToken();
		// ��ȡ��ά�벢���浽�ƶ�
		String fileName = (type + "_" + value + ".jpg");
		String QRCodeURL = GetLocalAndSaveQRCodePlan2(accessToken, type, value, fileName);
		// �ϳɶ�ά�벢���浽�ƶ�
		QRCodeURL = ComposeLocalShareQRCode(QRCodeURL, type, value.toString(),ename,picUrl);
		//QRCodeURL = ComposeLocalShareQRCode(QRCodeURL, type, value.toString(),"m-"+ename,mpicUrl);
		System.out.println(QRCodeURL);
		// ��ȡ����·��
		String[] temp = QRCodeURL.split("/");
		int length = temp.length;
		String url = CommonParam.qiniuOpenAddress + temp[length - 1];
		return url;
	}
	
	/**
	 * �ϳ�Բ�ζ�ά��  �����浽�ƶ�
	 */	
	public String GetLocalSmallAppQRCodeURLPlan3(String type, Object value,String ename,String picUrl,String mpicUrl) {
		// ��ȡaccessToken
		String accessToken = GetSmallAppAccessToken();
		// ��ȡ��ά�벢���浽�ƶ�
		UUID uuid = UUID.randomUUID();
		String fileName = (uuid + ".jpg");
		String QRCodeURL = GetLocalAndSaveQRCodePlan3(accessToken, type, value, fileName);
		System.out.println(QRCodeURL);
		if(QRCodeURL!=null&&QRCodeURL.indexOf("QRCodeError.png")!=-1) {
			return QRCodeURL;
		}
		// �ϳɶ�ά�벢���浽�ƶ�
		QRCodeURL = ComposeLocalShareQRCode(QRCodeURL, type, value.toString(),ename,picUrl,mpicUrl);
		System.out.println(QRCodeURL);
		// ��ȡ����·��
		String[] temp = QRCodeURL.split("/");
		int length = temp.length;
		String url = CommonParam.qiniuPrivateAddress  + temp[length - 1];
		QiniuOperate operate = new QiniuOperate();
		return operate.getDownloadToken(url);
	}
	
	/**
	 * ����ֽ���ά��
	 */	
	public String getSharePaperQRCodeURL(String type, Object value,String a) {
		// ��ȡaccessToken
		String accessToken = GetSmallAppAccessToken();
		// ��ȡ��ά�벢���浽�ƶ�
		UUID uuid = UUID.randomUUID();
		String fileName = (uuid + ".jpg");
		String QRCodeURL = GetShareAndSaveQRCode(accessToken, type, value,a, fileName);
		if(QRCodeURL!=null&&QRCodeURL.indexOf("QRCodeError.png")!=-1) {
			return QRCodeURL;
		}
		// ��ȡ����·��
		String[] temp = QRCodeURL.split("/");
		int length = temp.length;
		String url = CommonParam.qiniuOpenAddress  + temp[length - 1];	
		return url;
	}

	/**
	 * ��ȡС�����ά�� testController ʹ�ã����ɷ��εĶ�̬�����ά��
	 * 
	 * @return
	 */
	public String GetSmallAppQRCodeURLByLkTest2(String type, Object value,String ename,String picUrl,String mpicUrl) {
		// ��ȡaccessToken
		String accessToken = GetSmallAppAccessToken();
		// ��ȡ��ά�벢���浽�ƶ�
		String fileName = ( value + ".jpg");
		String QRCodeURL1 = GetAndSaveQRCodeByLktest(accessToken, type, value, fileName);
		// �ϳɶ�ά�벢���浽�ƶ�
		//ComposePicture compose = new ComposePicture();
		String QRCodeURL = ComposeShareQRCodeLkTest(QRCodeURL1, type, (String) value.toString(),ename,picUrl);
		//QRCodeURL = ComposeShareQRCodeLkTest(QRCodeURL1, type, (String) value.toString(),"m-"+ename,mpicUrl);
		
		//QRCodeURL = ComposeShareQRCodeLkTest2(QRCodeURL1, type, (String) value.toString(),"m-"+ename,mpicUrl);
		System.out.println(QRCodeURL);
		// ��ȡ����·��
		String[] temp = QRCodeURL.split("/");
		int length = temp.length;
		String url = CommonParam.qiniuOpenAddress + temp[length - 1];
		return url;
	}
	/**
	 * ��ȡС�����ά�� testController ʹ�ã����ɷ��εĶ�̬�����ά��
	 * 
	 * @return
	 */
	public String GetSmallAppQRCodeURLByLkTest(String type, Object value,String ename,String picUrl,String mpicUrl) {
		// ��ȡaccessToken
		String accessToken = GetSmallAppAccessToken();
		// ��ȡ��ά�벢���浽�ƶ�
		String fileName = ( value + ".jpg");
		String QRCodeURL1 = GetAndSaveQRCodeByLktest(accessToken, type, value, fileName);
		// �ϳɶ�ά�벢���浽�ƶ�
		//ComposePicture compose = new ComposePicture();
		String QRCodeURL = ComposeShareQRCodeLkTest(QRCodeURL1, type, (String) value.toString(),ename,picUrl);
		//QRCodeURL = ComposeShareQRCodeLkTest(QRCodeURL1, type, (String) value.toString(),"m-"+ename,mpicUrl);
		System.out.println(QRCodeURL);
		// ��ȡ����·��
		String[] temp = QRCodeURL.split("/");
		int length = temp.length;
		String url = CommonParam.qiniuOpenAddress + temp[length - 1];
		return url;
	}
	/**
	 * �����ά��ϳ� testcontrollerʹ��
	 */
	public String ComposeShareQRCodeLkTest(String QRCodeURL, String type, String data,String ename,String picUrl) {
		// ��ȡ����ͼ
		String bottomPictureName = "";
		int x = 10;
		int y = 10;
		int width = 230;
		int height = 261;
		switch (type) {
		case "spaceEvent":
			//bottomPictureName = "spaceEventBackground.jpg";
			bottomPictureName = "testEvent.jpg";
//			x = 552;
//			y = 1516;
//			width = 400;
//			height = 400;
//			x = 468;
//			y = 776;
//			width = 226;
//			height = 252;
			/*
			 * ����&���Ѻ��մ���
			 */
//			x = 562;
//			y = 1497;
//			width = 373;
//			height = 382;
			/*
			 * ���մ���ս
			 */
//			x = 2020;
//			y = 3442;
//			width = 812;
//			height = 886;
			/*
			 * ���Ѱ�������&���ѡ����մ���
			 */
//			x = 990;
//			y = 1747;
//			width = 421;
//			height = 416;
			/*
			 * ���εڶ����������մ���
			 */
//			x = 544;
//			y = 1410;
//			width = 413;
//			height = 435;
			/*
			 * ��������������������У԰���մ���  4931684
���ӱ���������������У԰���մ���   4931692
��������������������У԰���մ���   4931686
��������������������У԰���մ���   4931676
			 */
//			x = 443;
//			y = 787;
//			width = 234;
//			height = 249;
			/*
			 * �����մ���
			 */
//			x = 198;
//			y = 501;
//			width = 137;
//			height = 152;
			/*
			 * ����������ѡ����մ���
			 */
//			x = 646;
//			y = 1144;
//			width = 290;
//			height = 305;
			/*
			 * ���κ��մ���
			 */
//			x = 559;
//			y = 1451;
//			width = 381;
//			height = 412;
			/*
			 * �������˺��մ���
			 */
			x = 994;
			y = 1723;
			width = 428;
			height = 459;
			break;
		case "space":
			bottomPictureName = "spaceBackground.png";
			x = 160;
			y = 380;
			width = 430;
			height = 470;
			/*
			 * ʱ���ǡ���ʮ������������
			 */
//			x = 729;
//			y = 983;
//			width = 316;
//			height = 350;
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
		// File file = new File(JFinal.me().getServletContext().getRealPath("/")
		// + "\\image\\"+bottomPictureName);

		try {
			
			//URL file = new URL("http://localhost/~liukai/testEvent2.png");
			//File file = new File("/Users/liukai/Sites/jy2.jpg");
			//File file = new File("/Users/liukai/Sites/mz.png");
			/*
			 * ���Ѱ�������&���ѡ����մ���
			 */
			//File file = new File("/Users/liukai/Sites/jy2.jpeg");
			/*
			 * ���εڶ����������մ���
			 */
			//File file = new File("/Users/liukai/Sites/WechatIMG969.png");
			/*
			 * ��������������������У԰���մ���  4931684
���ӱ���������������У԰���մ���   4931692
��������������������У԰���մ���   4931686
��������������������У԰���մ���   4931676
			 */
			//File file = new File("/Users/liukai/Sites/kb.png");
			/*�����մ���*/
			//File file = new File("/Users/liukai/Sites/zcy.jpg");
			/*����������ѡ����մ���*/
			//File file = new File("/Users/liukai/Sites/zsq.jpg");
			/*���κ��մ���*/
			//File file = new File("/Users/liukai/Sites/zmhzds.png");
			/*�������˺��մ���*/
			File file = new File("/Users/liukai/Sites/zmjr.jpeg");
			File QRCode=new File(QRCodeURL);
			BufferedImage bi = ImageIO.read(file);
			Graphics2D graphics = bi.createGraphics();

			//BufferedImage qrBi = ImageIO.read(QRCode);
			Image qrBi = ImageIO.read(QRCode);
			// �ϳ�ͼƬ
			graphics.drawImage(qrBi, x, y, width, height, null);

			if (!picUrl.equals("")) {
				
				BufferedImage picBi = ImageIO.read(new URL(picUrl));
				
				// �ϳ�ͷ��
				//graphics.drawImage(picBi, 161, 583, 1179, 827, null);
				//graphics.drawImage(picBi, 126, 900, 2873, 2504, null);
				/*
				 * ���Ѱ�������&���ѡ����մ���
				 */
				//graphics.drawImage(picBi, 71, 661, 1341, 930, null);
				/*
				 * ���εڶ����������մ���
				 */
//				int w=1410;
//				int px=59;
//				if(picBi.getWidth()<w){
//					int oldw=w;
//					w=picBi.getWidth();
//					px+=(oldw-picBi.getWidth())/2;
//				}
//				graphics.drawImage(picBi, px, 438, w, 927, null);
				/*
				 * ��������������������У԰���մ���  4931684
���ӱ���������������У԰���մ���   4931692
��������������������У԰���մ���   4931686
��������������������У԰���մ���   4931676
				 */
				//graphics.drawImage(picBi, 72, 272, 589, 459, null);
				//�����մ���
				//graphics.drawImage(picBi, 7, 177, 520, 280, null);
				//����������ѡ����մ���
				//graphics.drawImage(picBi, 52, 374, 892, 725, null);
				//�������˺��մ���
				graphics.drawImage(picBi, 92, 494, 1316, 1125, null);
				//���κ��մ���
				//graphics.drawImage(picBi, 25, 502, 1451, 774, null);
//				// �ϳ��ǳ�
//				graphics.setColor(new Color(0, 0, 0));
//				// ��������
//				Font font = new Font("Microsoft YaHei", Font.BOLD, 35);
//				graphics.setFont(font);
//				graphics.drawString(nickname, 200, 125);
			}

			// ��������
			graphics.dispose();
			// ���浽���أ����ϴ����ƶ�
			//____lk test
			//ImageIO.write(bi, "png", new File(CommonParam.tempPictureServerSavePath + (type + "-" + data) + ".png"));
			ImageIO.write(bi, "png", new File(CommonParam.pictureSaveLocalPath + (data) +"-"+ (ename) +".png"));
			
			QiniuOperate qiniu = new QiniuOperate();
			//____lk test
//			qiniu.uploadFileToOpenSpace(CommonParam.tempPictureServerSavePath + (type + "-" + data) + ".png",
//					(type + "-" + data + "-result") + ".png");
//			qiniu.uploadFileToOpenSpace(CommonParam.pictureSaveLocalPath + (ename) + ".png",
//					(type + "-" + data + "-result") + ".png");
			QRCodeURL = CommonParam.qiniuOpenAddress + (data) + ".png";
			// DPIHandler dpi = new DPIHandler();
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
	 * �����ά��ϳ� testcontrollerʹ��
	 */
	public String ComposeShareQRCodeLkTest2(String QRCodeURL, String type, String data,String ename,String picUrl) {
		// ��ȡ����ͼ
		String bottomPictureName = "";
		int x = 10;
		int y = 10;
		int width = 230;
		int height = 261;
		switch (type) {
		case "spaceEvent":
			//bottomPictureName = "spaceEventBackground.jpg";
			bottomPictureName = "testEvent.jpg";
//			x = 552;
//			y = 1516;
//			width = 400;
//			height = 400;
//			x = 468;
//			y = 776;
//			width = 226;
//			height = 252;
			/*
			 * ����&���Ѻ��մ���
			 */
			x = 562;
			y = 1497;
			width = 373;
			height = 382;
			break;
		case "space":
			bottomPictureName = "spaceBackground.png";
			x = 160;
			y = 380;
			width = 430;
			height = 470;
			/*
			 * ʱ���ǡ���ʮ������������
			 */
//			x = 729;
//			y = 983;
//			width = 316;
//			height = 350;
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
		// File file = new File(JFinal.me().getServletContext().getRealPath("/")
		// + "\\image\\"+bottomPictureName);

		try {
			
			//URL file = new URL("http://localhost/~liukai/testEvent2.png");
			File file = new File("/Users/liukai/Sites/jy2.jpg");
			
			File QRCode=new File(QRCodeURL);
			BufferedImage bi = ImageIO.read(file);
			Graphics2D graphics = bi.createGraphics();

			//BufferedImage qrBi = ImageIO.read(QRCode);
			Image qrBi = ImageIO.read(QRCode);
			// �ϳ�ͼƬ
			graphics.drawImage(qrBi, x, y, width, height, null);

			if (!picUrl.equals("")) {
				
				BufferedImage picBi = ImageIO.read(new URL(picUrl));
				int pheight = picBi.getHeight();
		        int pwidth = picBi.getWidth();
		        int[][] martrix = new int[3][3];
		        int[] values = new int[9];
		        for (int i = 0; i < pwidth; i++)
		            for (int j = 0; j < pheight; j++) {
		                readPixel(picBi, i, j, values);
		                fillMatrix(martrix, values);
		                picBi.setRGB(i, j, avgMatrix(martrix));
		            }
				// �ϳ�ͷ��
				graphics.drawImage(picBi, 161, 583, 1179, 827, null);
//				// �ϳ��ǳ�
//				graphics.setColor(new Color(0, 0, 0));
//				// ��������
//				Font font = new Font("Microsoft YaHei", Font.BOLD, 35);
//				graphics.setFont(font);
//				graphics.drawString(nickname, 200, 125);
			}

			// ��������
			graphics.dispose();
			// ���浽���أ����ϴ����ƶ�
			//____lk test
			//ImageIO.write(bi, "png", new File(CommonParam.tempPictureServerSavePath + (type + "-" + data) + ".png"));
			ImageIO.write(bi, "png", new File(CommonParam.pictureSaveLocalPath + (data) +"-"+ (ename) +".png"));
			
			QiniuOperate qiniu = new QiniuOperate();
			//____lk test
//			qiniu.uploadFileToOpenSpace(CommonParam.tempPictureServerSavePath + (type + "-" + data) + ".png",
//					(type + "-" + data + "-result") + ".png");
//			qiniu.uploadFileToOpenSpace(CommonParam.pictureSaveLocalPath + (ename) + ".png",
//					(type + "-" + data + "-result") + ".png");
			QRCodeURL = CommonParam.qiniuOpenAddress + (data) + ".png";
			// DPIHandler dpi = new DPIHandler();
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
	 * ��ȡ������С�����ά��
	 */
	private static String GetAndSaveQRCodeByLktest(String accessToken, String type, Object value, String fileName) {

		String url = "https://api.weixin.qq.com/cgi-bin/wxaapp/createwxaqrcode?access_token=" + accessToken;
		
		JSONObject jo = new JSONObject();
		// ��������ƴ���������·��
		String path = "";
		switch (type) {
		case "space":
			path = "pages/viewscoll/viewscoll?groupid=" + value + "&port=�ռ��ά��&fromSpaceID=" + value + "&from=timeline";
			break;
		case "spaceEvent":
			path = "pages/eventdetail/eventdetail?eid=" + value + "&port=��̬�������&efrom=share";
			break;
		case "puzzle":
			path = "pages/pingtu/pingtu?id=" + value + "&port=С����ƴͼ";
			break;
		case "temp":
			path = "pages/commonpage/lookBurnAfterRead/look?tempid=" + value + "&port=�ĺ󼴷�";
			break;
		case "industry":
			path = "pages/smallUtilPages/hangyezhenxiang/index?port=��ҵ����";
			break;
		case "encourage":
			String[] array = value.toString().split("-");
			path = "pages/index/index?shareUserid=" + array[0] + "&shCode=" + array[1] + "&port=С�����ά��&fromUserID="
					+ array[0] + "";
			break;
		}
		jo.put("path", path);	
		String param = jo.toJSONString();
		String QRCodeURL = SentLocalRequestToGetQRCodePlan2(url, param, fileName);
		return QRCodeURL;
	}
	/**
	 * �����ά��ϳ� by lk ����
	 */
	public String ComposeLocalShareQRCode(String QRCodeURL, String type, String data,String ename,String picUrl) {
		// ��ȡ����ͼ
		String bottomPictureName = "";
		int x = 10;
		int y = 10;
		int width = 230;
		int height = 261;
		switch (type) {
		case "eventdetail2":
			//bottomPictureName = "spaceEventBackground.jpg";	
			bottomPictureName = "plan2QR.png";	
//			x = 99;
//			y = 1160;
//			width = 270;
//			height = 270;
			x = 183;
			y = 175;
			width = 390;
			height = 380;
			/*
			 * �������˺��մ���
			 */
			x = 994;
			y = 1723;
			width = 428;
			height = 459;
			break;
		case "spaceQR":
			bottomPictureName = "plan2QR.png";	
			x = 183;
			y = 175;
			width = 390;
			height = 380;
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
			/*�������˺��մ���*/
			File file = new File("/Users/liukai/Sites/zmjr.jpeg");
			File QRCode = new File(QRCodeURL);
			//URL QRCode = new URL(QRCodeURL);
			BufferedImage bi = ImageIO.read(file);
			Graphics2D graphics = bi.createGraphics();

			BufferedImage qrBi = ImageIO.read(QRCode);

			// �ϳ�ͼƬ
			graphics.drawImage(qrBi, x, y, width, height, null);

			// �����������棬������û�ͷ����ǳ�
			if (!picUrl.equals("")) {
				
				BufferedImage picBi = ImageIO.read(new URL(picUrl));
				// �ϳ�ͷ��
				//graphics.drawImage(picBi, 40, 234, 673, 534, null);
				//�������˺��մ���
				graphics.drawImage(picBi, 92, 494, 1316, 1125, null);
//				// �ϳ��ǳ�
//				graphics.setColor(new Color(0, 0, 0));
//				// ��������
//				Font font = new Font("Microsoft YaHei", Font.BOLD, 35);
//				graphics.setFont(font);
//				graphics.drawString(nickname, 200, 125);
			}

			// ��������
			graphics.dispose();
			// ���浽���أ����ϴ����ƶ�
			//____lk test
			//ImageIO.write(bi, "png", new File(CommonParam.tempPictureServerSavePath + (type + "-" + data) + "_1.png"));
			ImageIO.write(bi, "png", new File(CommonParam.pictureSaveLocalPath + (type + "-" + data+"-"+ename) + ".png"));
			
			QiniuOperate qiniu = new QiniuOperate();	
			QRCodeURL = CommonParam.qiniuOpenAddress + (type + "-" + data + "-result") + "_1.png";
			// DPIHandler dpi = new DPIHandler();
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
	 * �ϳ�Բ��ͷ��,�ͱ���ͼ�任
	 */
	public String ComposeLocalShareQRCode(String QRCodeURL, String type, String data,String ename,String picUrl,String mpicUrl) {
		// ��ȡ����ͼ
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
			URL file = new URL(CommonParam.materialPath + bottomPictureName);
			//File file = new File("/Users/liukai/Sites/photo.png");
			//File file = new File("D:/leiyu/list2/photo.png");
			//File QRCode = new File(QRCodeURL);
			URL QRCode = new URL(QRCodeURL);
			BufferedImage bi = ImageIO.read(file);
			Graphics2D graphics = bi.createGraphics();
			

			BufferedImage qrBi = ImageIO.read(QRCode);

			// �ϳ�ͼƬ
			graphics.drawImage(qrBi, x, y, width, height, null);

			// �ϳ�ͷ��ͱ���ͼƬ
			if (!mpicUrl.equals("")&&!picUrl.equals("")) {
				//�ϳɱ���ͼƬ
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
				// �ϳ�Բ��ͷ��
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
				
				// �ϳ��ǳ�
				graphics.setColor(new Color(255,255, 255));
				// ��������
				Font font = new Font("simsun", Font.CENTER_BASELINE, 40);
				graphics.setFont(font);
				graphics.drawString(ename, 100, 260);
				Font font2 = new Font("simsun", Font.CENTER_BASELINE, 24);
				graphics.setFont(font2);
				graphics.drawString("�������ǵ����", 182, 320);
				Font font3 = new Font("simsun", Font.CENTER_BASELINE, 24);
				graphics.setFont(font3);
				graphics.drawString("���Ƕ���", 218, 370);
				Font font4 = new Font("simsun", Font.CENTER_BASELINE, 30);
				graphics.setFont(font4);
				graphics.drawString("��Ҳ�����", 176, 420);
			}

			// ��������
			graphics.dispose();
			// ���浽���أ����ϴ����ƶ�
			//____lk test
			UUID uuid = UUID.randomUUID();
			ImageIO.write(bi, "png", new File(CommonParam.tempPictureServerSavePath + type + uuid+ ".png"));
			//ImageIO.write(bi, "png", new File(CommonParam.pictureSaveLocalPath + type + uuid+ ".png"));
			
			QiniuOperate qiniu = new QiniuOperate();
			qiniu.uploadFileToPrivateSpace(CommonParam.tempPictureServerSavePath +  type + uuid + ".png",type + uuid + ".png");
			//qiniu.uploadFileToPrivateSpace(CommonParam.pictureSaveLocalPath +  type + uuid + ".png",type + uuid + ".png");
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
    
    /**
     * ��ӻ���
     */
    @Before(Tx.class)
    public boolean addPoints(String userid,int point,String typeName) {
    	
    	Points points = new Points();
		PointsReceive pointsReceive = new PointsReceive();
		List<Record> pointsInfo = points.getPointsInfo(userid);
		boolean flag1 = false;
		boolean flag2 = false;
		if (pointsInfo.size()==0) {
			// �����ݣ�����������
			points.set("puserid", userid).set("totalPoints",point).set("useablePoints", point);
			flag1 = points.save();
			pointsReceive.set("puserid", userid).set("ptid", 9).set("ptname", "ϵͳ����")
			.set("receivePoints", point).set("preceivestatus", 2).set("beforepoints", 0)
			.set("laterpoints", point).set("premark", typeName + "��ȡ��" + point);
			pointsReceive.save();
		}else {
			String poid = pointsInfo.get(0).get("poid").toString();
			points = new Points().findById(poid);
			int totalPoints = Integer.parseInt(pointsInfo.get(0).get("totalPoints").toString());
			int useablePoints = Integer.parseInt(pointsInfo.get(0).get("useablePoints").toString());
			points.set("totalPoints", totalPoints+point).set("useablePoints", useablePoints+point);
			flag1 = points.update();
			pointsReceive.set("puserid", userid).set("ptid", 9).set("ptname", "ϵͳ����")
			.set("receivePoints", point).set("preceivestatus", 2).set("beforepoints", useablePoints)
			.set("laterpoints", useablePoints+point).set("premark", typeName + "��ȡ��" + point);
			flag2 = pointsReceive.save();
		}
		if(flag1 && flag2) {
			return true;
		} else {
			return false;
		}
    }
    
}
