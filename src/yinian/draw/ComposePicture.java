package yinian.draw;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import yinian.common.CommonParam;
import yinian.model.User;
import yinian.utils.QiniuOperate;
import yinian.utils.TwoDimensionCode;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codec.TIFFDecodeParam;

public class ComposePicture {

	private static Log log = LogFactory.getLog(ComposePicture.class);

	/**
	 * 合成明信片封底
	 * 
	 * @param from
	 * @param to
	 * @param text
	 * @param ImgName
	 * @param orderNumber
	 * @param qrCode
	 */
	public static void MakePostcardBottom(String from, String to, String text, String ImgName, String qrCode,
			String orderNumber, String path) {

		try {
			// 判断图片是否偏移并翻转
			Image src = handlePictureReverse(ImgName);

			// 创建Graphics
			BufferedImage image = new BufferedImage(1772, 1181, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics = (Graphics2D) image.createGraphics();
			// 去锯齿
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			// 用白色填充整个图片
			graphics.fillRect(0, 0, 1772, 1181);

			// 添加图片
			graphics.drawImage(src, 43, 132, 900, 900, null);

			// 设置字体颜色
			graphics.setColor(new Color(45, 45, 45));

			// 设置字体
			FontManager fontm = new FontManager();
			graphics.setFont(fontm.fzjlFont());
			FontMetrics fm = graphics.getFontMetrics();

			// To，判断
			if (to.length() > 3) {
				if (!(to.substring(0, 3).equals("To "))) {
					to = "To " + to;
				}
			} else {
				to = "To " + to;
			}
			graphics.drawString(to, 1010, 200);

			// From，判断
			if (from.length() > 6) {
				if (!(from.substring(0, 6).equals("From: "))) {
					from = "From: " + from;
				}
			} else {
				from = "From: " + from;
			}
			int fromWidth = fm.stringWidth(from);
			graphics.drawString(from, 1697 - fromWidth, 960);

			// 正文，自动换行
			drawStringMultiLine(graphics, text, 637, 1060, 326);

			// 订单号
			graphics.drawString(orderNumber, 240, 1100);

			// 二维码
			TwoDimensionCode handler = new TwoDimensionCode();
			if (qrCode != null && !qrCode.equals("")) {
				String content = "http://wx.zhuiyinanian.com/shop/playaudio?src=" + qrCode + "&goodsid=1&from=yinian";
				BufferedImage bfImage = handler.qRCodeCommon(content, "png", 10);
				graphics.drawImage(bfImage, 1060, 680, 190, 190, null);
			}

			// 结束绘制
			graphics.dispose();

			// 设置图片dpi并保存,这是唯一文件名
			DPIHandler dpi = new DPIHandler();
			dpi.saveGridImage(image, new File(path));// 300dpi jpg 但是没有cmyk

			// 转变图片颜色模式
			// BufferedImage rgbImage = ImageIO.read(new File(path));
			// BufferedImage rgbImage = null;
			// ColorSpace cpace = new ICC_ColorSpace(
			// ICC_Profile.getInstance(ComposePicture.class
			// .getClassLoader().getResourceAsStream(
			// "ISOcoated_v2_300_eci.icc")));
			// ColorConvertOp op = new ColorConvertOp(cpace, null);
			// rgbImage = op.filter(image, null);
			// ImageIO.write(rgbImage, "JPEG", new
			// File("C:/Users/Zad/Desktop/CMYK_Sample_RGB_OUTPUT2.jpg"));
			//
			// /* tif转换到jpg格式 */
			// BufferedImage img = ImageIO.read(new
			// File("C:/Users/Zad/Desktop/results1.tif"));
			// ImageIO.write(img, "jpg", new
			// File("C:/Users/Zad/Desktop/results1.jpg"));

		} catch (Exception e) {
			log.error(ImgName + "   " + e);
			e.printStackTrace();
		}
	}
	/**
	 * 分享二维码合成 testcontroller使用
	 */
	public String ComposeShareQRCodeLkTest(String QRCodeURL, String type, String data,String ename) {
		// 获取背景图
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
			x = 503;
			y = 811;
			width = 208;
			height = 223;
			/*
			 * 最美“ 闺蜜＆基友 ”合照大赛
			 */
//			x = 500;//201
//			y = 1450;//534
//			width = 500;//129
//			height = 450;//133
			/*
			 * 宁夏人
			 */
//			x = 440;
//			y = 751;
//			width = 295;
//			height = 295;
			//花样宿舍校园拍照大赛
//			x = 467;
//			y = 775;
//			width = 233;
//			height = 251;
			/*
			 * 时遇记·第十七期恋爱体验
			 */
//			x = 729;
//			y = 983;
//			width = 316;
//			height = 350;
			break;
		case "space":
			bottomPictureName = "spaceBackground.png";
			x = 160;
			y = 380;
			width = 430;
			height = 470;
			/*
			 * 时遇记·第十七期恋爱体验
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
			//URL file = new URL(CommonParam.materialPath + bottomPictureName);
			//URL file = new URL("http://localhost/~liukai/nx.png");
			//URL file = new URL("http://localhost/~liukai/testEvent3.jpg");
			//URL file = new URL("http://localhost/~liukai/hyss.jpg");
			//URL QRCode = new URL(QRCodeURL);
			URL file = new URL("http://localhost/~liukai/testEvent2.png");
			//File file = new File("/Users/liukai/Sites/syj.jpg");
			File QRCode=new File(QRCodeURL);
			BufferedImage bi = ImageIO.read(file);
			Graphics2D graphics = bi.createGraphics();

			BufferedImage qrBi = ImageIO.read(QRCode);

			// 合成图片
			graphics.drawImage(qrBi, x, y, width, height, null);

			// 如果是邀请界面，则加上用户头像和昵称
			if (type.equals("encourage")) {
				String userid = data.split("-")[0];
				User user = new User().findById(userid);
				String nickname = user.get("unickname").toString();
				String pic = user.get("upic").toString();
				BufferedImage picBi = ImageIO.read(new URL(pic));
				// 合成头像
				graphics.drawImage(picBi, 60, 50, 120, 120, null);
				// 合成昵称
				graphics.setColor(new Color(0, 0, 0));
				// 设置字体
				Font font = new Font("Microsoft YaHei", Font.BOLD, 35);
				graphics.setFont(font);
				graphics.drawString(nickname, 200, 125);
			}

			// 结束绘制
			graphics.dispose();
			// 保存到本地，再上传到云端
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
	 * 分享二维码合成
	 */
	public String ComposeShareQRCode(String QRCodeURL, String type, String data) {
		// 获取背景图
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
			break;
//		case "plan2":
//			bottomPictureName = "plan2QR.jpg";			
//			x = 183;
//			y = 360;
//			width = 390;
//			height = 380;
//			break;
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
		// File file = new File(JFinal.me().getServletContext().getRealPath("/")
		// + "\\image\\"+bottomPictureName);

		try {
			URL file = new URL(CommonParam.materialPath + bottomPictureName);
			//File file = new File("/Users/liukai/Desktop/1.jpeg");
			URL QRCode = new URL(QRCodeURL);
			BufferedImage bi = ImageIO.read(file);
			Graphics2D graphics = bi.createGraphics();

			BufferedImage qrBi = ImageIO.read(QRCode);

			// 合成图片
			graphics.drawImage(qrBi, x, y, width, height, null);

			// 如果是邀请界面，则加上用户头像和昵称
			if (type.equals("encourage")) {
				String userid = data.split("-")[0];
				User user = new User().findById(userid);
				String nickname = user.get("unickname").toString();
				String pic = user.get("upic").toString();
				BufferedImage picBi = ImageIO.read(new URL(pic));
				// 合成头像
				graphics.drawImage(picBi, 60, 50, 120, 120, null);
				// 合成昵称
				graphics.setColor(new Color(0, 0, 0));
				// 设置字体
				Font font = new Font("Microsoft YaHei", Font.BOLD, 35);
				graphics.setFont(font);
				graphics.drawString(nickname, 200, 125);
			}

			// 结束绘制
			graphics.dispose();
			// 保存到本地，再上传到云端
			//____lk test
			ImageIO.write(bi, "png", new File(CommonParam.tempPictureServerSavePath + (type + "-" + data) + ".png"));
			//ImageIO.write(bi, "png", new File(CommonParam.pictureSaveLocalPath + (type + "-" + data) + ".png"));
			
			QiniuOperate qiniu = new QiniuOperate();
			//____lk test
			qiniu.uploadFileToOpenSpace(CommonParam.tempPictureServerSavePath + (type + "-" + data) + ".png",
					(type + "-" + data + "-result") + ".png");
			//qiniu.uploadFileToOpenSpace(CommonParam.pictureSaveLocalPath + (type + "-" + data) + ".png",
			//		(type + "-" + data + "-result") + ".png");
			QRCodeURL = CommonParam.qiniuOpenAddress + (type + "-" + data + "-result") + ".png";
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
	//5460577 使用统一底图
	public String ComposeShareQRCode_5460577(String QRCodeURL, String type, String data) {
		// 获取背景图
		String bottomPictureName = "";
		int x = 10;
		int y = 10;
		int width = 230;
		int height = 261;
		switch (type) {
		case "eventdetail2":
			//bottomPictureName = "spaceEventBackground.jpg";	
			bottomPictureName = "scw20180302.png";	

			x = 245;
			y = 521;
			width = 259;
			height = 271;
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
		// File file = new File(JFinal.me().getServletContext().getRealPath("/")
		// + "\\image\\"+bottomPictureName);

		try {
			URL file = new URL(CommonParam.materialPath + bottomPictureName);
			//File file = new File("/Users/liukai/Desktop/1.jpeg");
			URL QRCode = new URL(QRCodeURL);
			BufferedImage bi = ImageIO.read(file);
			Graphics2D graphics = bi.createGraphics();

			BufferedImage qrBi = ImageIO.read(QRCode);

			// 合成图片
			graphics.drawImage(qrBi, x, y, width, height, null);

		/*	// 如果是邀请界面，则加上用户头像和昵称
			if (type.equals("encourage")) {
				String userid = data.split("-")[0];
				User user = new User().findById(userid);
				String nickname = user.get("unickname").toString();
				String pic = user.get("upic").toString();
				BufferedImage picBi = ImageIO.read(new URL(pic));
				// 合成头像
				graphics.drawImage(picBi, 60, 50, 120, 120, null);
				// 合成昵称
				graphics.setColor(new Color(0, 0, 0));
				// 设置字体
				Font font = new Font("Microsoft YaHei", Font.BOLD, 35);
				graphics.setFont(font);
				graphics.drawString(nickname, 200, 125);
			}
*/
			// 结束绘制
			graphics.dispose();
			// 保存到本地，再上传到云端
			//____lk test
			ImageIO.write(bi, "png", new File(CommonParam.tempPictureServerSavePath + (type + "-" + data) + ".png"));
			//ImageIO.write(bi, "png", new File(CommonParam.pictureSaveLocalPath + (type + "-" + data) + ".png"));
			
			QiniuOperate qiniu = new QiniuOperate();
			//____lk test
			qiniu.uploadFileToPrivateSpace(CommonParam.tempPictureServerSavePath + (type + "-" + data) + ".png",
					(type + "-" + data + "-result") + ".png");
//			qiniu.uploadFileToOpenSpace(CommonParam.pictureSaveLocalPath + (type + "-" + data) + ".png",
//					(type + "-" + data + "-result") + ".png");
			QRCodeURL = CommonParam.qiniuPrivateAddress + (type + "-" + data + "-result") + ".png";
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
	 * 分享二维码合成 by lk 本地
	 */
	public String ComposeLocalShareQRCode(String QRCodeURL, String type, String data,String ename) {
		// 获取背景图
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
//			x = 183;
//			y = 175;
//			width = 390;
//			height = 380;
			/**青云杯**、
			 * 
			 */
//			x = 10;
//			y = 20;
//			width = 350;
//			height = 350;
			/**MQ武汉市十佳歌手大赛 **、
			 * 
			 */
			x = 503;
			y = 811;
			width = 208;
			height = 223;
			/**校园公益接力计划 **、
			 * 
			 */
//			x = 1070;
//			y = 1389;
//			width = 600;
//			height = 600;
//			x = 10;
//			y = 20;
//			width = 350;
//			height = 350;
			break;
//		case "plan2":
//			bottomPictureName = "plan2QR.jpg";			
//			x = 183;
//			y = 360;
//			width = 390;
//			height = 380;
//			break;
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
		// File file = new File(JFinal.me().getServletContext().getRealPath("/")
		// + "\\image\\"+bottomPictureName);

		try {
			//URL file = new URL(CommonParam.materialPath + bottomPictureName);
			File file = new File("/Users/liukai/Sites/mqwuhan.png");
			/*
			 * 校园公益接力计划
			 */
			//File file = new File("/Users/liukai/Sites/WechatIMG64.jpeg");
			URL QRCode = new URL(QRCodeURL);
			BufferedImage bi = ImageIO.read(file);
			Graphics2D graphics = bi.createGraphics();

			BufferedImage qrBi = ImageIO.read(QRCode);

			// 合成图片
			graphics.drawImage(qrBi, x, y, width, height, null);

			// 如果是邀请界面，则加上用户头像和昵称
			if (type.equals("encourage")) {
				String userid = data.split("-")[0];
				User user = new User().findById(userid);
				String nickname = user.get("unickname").toString();
				String pic = user.get("upic").toString();
				BufferedImage picBi = ImageIO.read(new URL(pic));
				// 合成头像
				graphics.drawImage(picBi, 60, 50, 120, 120, null);
				// 合成昵称
				graphics.setColor(new Color(0, 0, 0));
				// 设置字体
				Font font = new Font("Microsoft YaHei", Font.BOLD, 35);
				graphics.setFont(font);
				graphics.drawString(nickname, 200, 125);
			}

			// 结束绘制
			graphics.dispose();
			// 保存到本地，再上传到云端
			//____lk test
			//ImageIO.write(bi, "png", new File(CommonParam.tempPictureServerSavePath + (type + "-" + data) + "_1.png"));
			ImageIO.write(bi, "png", new File(CommonParam.pictureSaveLocalPath + (type + "-" + data+"-"+ename) + ".png"));
			
			QiniuOperate qiniu = new QiniuOperate();
			//____lk test
			//qiniu.uploadFileToOpenSpace(CommonParam.tempPictureServerSavePath + (type + "-" + data) + "_1.png",
			//		(type + "-" + data + "-result") + "_1.png");
			//qiniu.uploadFileToOpenSpace(CommonParam.pictureSaveLocalPath + (type + "-" + data) + ".png",
			//		(type + "-" + data + "-result") + ".png");
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
	 * 合成行业真相图片
	 */
	public String ComposeIndustryPicture(String type, String photo, String nickname, String userid) {

		try {
			// 获取背景图画板
			URL backgroundUrl = new URL(CommonParam.materialPath + "industryRealityBackground.png");
			BufferedImage bi = ImageIO.read(backgroundUrl);
			Graphics2D graphics = bi.createGraphics();

			// 获取中间图片文件
			URL picUrl = new URL(photo);
			BufferedImage picBi = ImageIO.read(picUrl);

			// 获取二维码图片文件
			URL qrCodeUrl = new URL(CommonParam.materialPath + "qrCode.png");
			BufferedImage qrBi = ImageIO.read(qrCodeUrl);

			// 合成图片
			graphics.drawImage(picBi, 50, 390, 640, 545, null);

			// 合成文字，设置字体颜色
			graphics.setColor(new Color(221, 33, 33));
			// 设置字体
			FontManager fontm = new FontManager();
			graphics.setFont(fontm.fzjlFont());

			// 绘制文字并设置字间距
			MyDrawString("你家" + type, 350, 1030, 0.8, graphics);
			MyDrawString(nickname, 400, 1110, 0.8, graphics);

			// 获取时间戳，用于存储
			long timestamp = System.currentTimeMillis();
			String fileName = userid + "_" + timestamp + "_";

			// 绘制二维码
			DPIHandler dpi = new DPIHandler();
			graphics.drawImage(qrBi, 200, 1120, 383, 142, null);
			// 输出有二维码的图片
			dpi.saveGridImage(bi, new File(CommonParam.tempPictureServerSavePath + fileName + "after.jpg"));
			// 结束绘制
			graphics.dispose();

			// 返回加上二维码后的文件名
			return fileName + "after.jpg";

		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}

	/**
	 * 合成行业真相图片第二版
	 */
	public String ComposeIndustryPicture2ndVersion(String photo, String nickname, String userid) {

		try {
			// 获取背景图画板
			URL picUrl = new URL(photo);
			BufferedImage bi = ImageIO.read(picUrl);
			Graphics2D graphics = bi.createGraphics();

			// 合成文字，设置字体颜色
			graphics.setColor(new Color(0, 0, 0));
			// 设置字体
			// FontManager fontm = new FontManager();
			// graphics.setFont(fontm.wryhFont());
			Font font = new Font("Microsoft YaHei", Font.BOLD, 40);
			graphics.setFont(font);
			// 绘制文字并截断多余字体
			FontMetrics m = graphics.getFontMetrics();
			String text = "@" + nickname;
			while (m.stringWidth(text) > 400) {
				text = text.substring(0, text.length() - 1);
			}
			graphics.drawString(text, 340, 860);

			// 获取时间戳，用于存储
			long timestamp = System.currentTimeMillis();
			String fileName = userid + "_" + timestamp + "_after.jpg";

			// 输出图片
			File file = new File(CommonParam.tempPictureServerSavePath + fileName);
			OutputStream os = new FileOutputStream(file);
			JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(os);
			encoder.encode(bi);
			// 结束绘制
			graphics.dispose();

			// 返回加上二维码后的文件名
			return fileName;

		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}

	/**
	 * 设置字间距
	 * 
	 * @param str
	 * @param x
	 * @param y
	 * @param rate
	 * @param g
	 */
	public static void MyDrawString(String str, int x, int y, double rate, Graphics g) {
		String tempStr = new String();
		int orgStringWight = g.getFontMetrics().stringWidth(str);
		int orgStringLength = str.length();
		int tempx = x;
		int tempy = y;
		while (str.length() > 0) {
			tempStr = str.substring(0, 1);
			str = str.substring(1, str.length());
			g.drawString(tempStr, tempx, tempy);
			tempx = (int) (tempx + (double) orgStringWight / (double) orgStringLength * rate);
		}
	}

	/**
	 * 自动换行
	 * 
	 * @param g
	 * @param text
	 * @param lineWidth
	 * @param x
	 * @param y
	 */
	public static void drawStringMultiLine(Graphics2D g, String text, int lineWidth, int x, int y) {
		FontMetrics m = g.getFontMetrics();
		if (m.stringWidth(text) < lineWidth) {
			g.drawString(text, x, y);
		} else {
			String[] words = text.split("");
			String currentLine = words[0];
			for (int i = 1; i < words.length; i++) {
				if (m.stringWidth(currentLine + words[i]) < lineWidth) {
					currentLine += "" + words[i];
				} else {
					g.drawString(currentLine, x, y);
					y += m.getHeight() + 30;
					currentLine = words[i];
				}
			}
			if (currentLine.trim().length() > 0) {
				g.drawString(currentLine, x, y);
			}
		}
	}

	/**
	 * 处理图片翻转
	 * 
	 * @throws IOException
	 * @throws ImageProcessingException
	 * @throws URISyntaxException
	 */
	public static BufferedImage handlePictureReverse(String src)
			throws IOException, ImageProcessingException, URISyntaxException {
		URL url = new URL(src);
		InputStream is = url.openStream();
		Metadata metadata = ImageMetadataReader.readMetadata(is);
		int orientation = 1;

		if (metadata.getDirectory(ExifIFD0Directory.class) != null) {
			// 判断图片是否有exif信息，有则获取偏转度
			Directory directory = metadata.getDirectory(ExifIFD0Directory.class);

			try {
				orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
			} catch (MetadataException e) {
				System.out.println("bbb   " + e);
			}
		}
		int angle = 0;
		switch (orientation) {
		case 8:
			angle = 270;
			break;
		case 6:
			angle = 90;
			break;
		case 3:
			angle = 180;
			break;

		}

		Image image = ImageIO.read(url);
		BufferedImage des = RotateImage.Rotate(image, angle);
		return des;

	}

	/**
	 * 把图片格式转换为JPEG格式
	 * 
	 * @param srcFile
	 *            File 要转换的原文件
	 * @param resFile
	 *            File 转换后的目标文件
	 * @param preType
	 *            int 原文件的类型，目前只支持TIFF格式-1
	 * @throws IOException
	 *             抛出IO异常
	 */
	@SuppressWarnings("unused")
	private static void convertImageToJPEG(File srcFile, File resFile, int preType) throws IOException {
		if (preType == 0) {
			System.out.println("Needn't to convert!");
			return;
		}

		InputStream fis = new FileInputStream(srcFile);
		OutputStream fos = new FileOutputStream(resFile);

		JPEGEncodeParam encodeParam = new JPEGEncodeParam();

		switch (preType) {
		case 1:
			TIFFDecodeParam decodeParam = new TIFFDecodeParam();
			decodeParam.setJPEGDecompressYCbCrToRGB(false);

			ImageDecoder decoder = ImageCodec.createImageDecoder("TIFF", fis, decodeParam);
			RenderedImage image = decoder.decodeAsRenderedImage();

			ImageEncoder encoder = ImageCodec.createImageEncoder("JPEG", fos, encodeParam);
			encoder.encode(image);
			break;
		default:
			break;
		}

		fos.close();
		fis.close();
	}

}
