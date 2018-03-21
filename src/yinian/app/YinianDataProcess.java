package yinian.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import yinian.common.CommonParam;
import yinian.model.Event;
import yinian.model.Grab;
import yinian.model.Group;
import yinian.model.RedEnvelop;
import yinian.model.User;
import yinian.thread.VerifyPicture;
import yinian.thread.VerifyPictureNew;
import yinian.utils.DES;
import yinian.utils.JsonData;
import yinian.utils.QiniuOperate;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.log.Logger;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

public class YinianDataProcess {

	private static final Logger log = Logger.getLogger(YinianDataProcess.class);
	private static QiniuOperate operate = new QiniuOperate();

	/**
	 * 事件与评论结合类
	 * 
	 * @param event
	 * @param comment
	 * @return
	 */
	public List<Record> combieEventAndComment(List<Record> event, List<Record> comment) {
		for (Record EventRecord : event) {
			List<Record> AddList = new ArrayList<Record>();
			for (Record CommentRecord : comment) {
				if (EventRecord.get("eid").equals(CommentRecord.get("ceid"))) {
					AddList.add(CommentRecord);
				}
			}
			EventRecord.set("comments", AddList);
		}
		// 获取动态查看用户信息
		List<Record> view = Db
				.find("select userid,unickname,upic,veid from view,users where userid=vuserid and vstatus=0 ");
		for (Record EventRecord : event) {
			List<Record> AddList = new ArrayList<Record>();
			for (Record ViewRecord : view) {
				if (EventRecord.get("eid").equals(ViewRecord.get("veid"))) {
					AddList.add(ViewRecord);
				}
			}
			EventRecord.set("view", AddList);
		}
		return event;
	}

	/**
	 * 电话号码处理与分类
	 * 
	 * @param phonenumber
	 * @param phoneList
	 * @return
	 */
	public Record disposePhonenumber(String phonenumber, List<Record> phoneList) {
		String notifyPhone = ""; // 需要发送通知的电话
		String messagePhone = ""; // 需要发送短信的电话
		// 进行电话号码的筛选和处理
		String[] phoneArray = phonenumber.split(",");
		for (int i = 0; i < phoneArray.length; i++) {
			boolean flag = false;
			for (Record phone : phoneList) {
				if ((phoneArray[i]).equals(phone.get("uphone").toString())) {
					flag = true;
					break;
				}
			}
			if (flag) {
				notifyPhone += (phoneArray[i] + ",");
			} else {
				messagePhone += (phoneArray[i] + ",");
			}
		}
		// 字符串处理
		if (!notifyPhone.equals("")) {
			notifyPhone = notifyPhone.substring(0, notifyPhone.length() - 1);
		}
		if (!messagePhone.equals("")) {
			messagePhone = messagePhone.substring(0, messagePhone.length() - 1);
		}
		Record result = new Record().set("notifyPhone", notifyPhone).set("messagePhone", messagePhone);
		return result;
	}

	/**
	 * 根据是否在waits表中拆分要发送短信的号码
	 * 
	 * @param messagePhone
	 * @param waitPhoneList
	 * @return
	 */
	public Record disposeMessagePhone(String messagePhone, String userid, List<Record> waitPhoneList) {
		String inWaitsPhone = ""; // 已在waits表中的电话信息
		String notInWaitsPhone = ""; // 不在waits表中的电话信息
		// 进行电话号码的筛选和处理
		String[] phoneArray = messagePhone.split(",");
		for (int i = 0; i < phoneArray.length; i++) {
			boolean flag = false;
			for (Record phone : waitPhoneList) {
				String waitsPhone = phone.get("wphone").toString();
				String waitsUserid = phone.get("wsender").toString();
				if ((phoneArray[i]).equals(waitsPhone) && userid.equals(waitsUserid)) {
					flag = true;
					break;
				}
			}
			if (flag) {
				inWaitsPhone += (phoneArray[i] + ",");
			} else {
				notInWaitsPhone += (phoneArray[i] + ",");
			}
		}
		// 字符串处理
		if (!inWaitsPhone.equals("")) {
			inWaitsPhone = inWaitsPhone.substring(0, inWaitsPhone.length() - 1);
		}
		if (!notInWaitsPhone.equals("")) {
			notInWaitsPhone = notInWaitsPhone.substring(0, notInWaitsPhone.length() - 1);
		}
		Record result = new Record().set("inWaitsPhone", inWaitsPhone).set("notInWaitsPhone", notInWaitsPhone);
		return result;
	}

	/**
	 * 返回结果中url字段转成数组格式处理类，等比例缩略图版本
	 * 
	 * @param pic
	 * @return
	 */
	public List<Record> ChangePicAsArray(List<Record> list) {
		QiniuOperate operate = new QiniuOperate();
		String pic = "";
		for (Record record : list) {
			pic = record.get("url");
			String[] picArray = null;
			String[] thumbnailArray = null;
			if (pic != null) {
				picArray = pic.split(",");
				thumbnailArray = new String[picArray.length];
				// 获取图片访问权限
				for (int i = 0; i < picArray.length; i++) {
					thumbnailArray[i] = operate.getDownloadToken(picArray[i] + "?imageView2/2/w/300");
					picArray[i] = operate.getDownloadToken(picArray[i]);

				}
			}
			// 获取音频访问权限
			String audio = record.get("eaudio");
			if (audio != null && !audio.equals("")) {
				audio = operate.getDownloadToken(audio);
			}
			record.set("url", picArray).set("eaudio", audio).set("thumbnail", thumbnailArray);
		}
		return list;
	}

	/**
	 * 返回结果中url字段转成数组格式处理类，直接裁剪版本
	 * 
	 * @param pic
	 * @return
	 */
	public List<Record> ChangePicAsArrayDirectCutVersion(List<Record> list) {
		QiniuOperate operate = new QiniuOperate();
		String pic = "";
		for (Record record : list) {
			pic = record.get("url");
			String[] picArray = pic.split(",");
			String[] thumbnailArray = new String[picArray.length];
			// 获取图片访问权限
			if (picArray.length == 1) {
				// 只有一张图时返回大一点
				thumbnailArray[0] = operate.getDownloadToken(picArray[0] + "?imageView2/1/w/600");
				picArray[0] = operate.getDownloadToken(picArray[0]);
			} else {
				// 多张则控制大小
				for (int i = 0; i < picArray.length; i++) {
					thumbnailArray[i] = operate.getDownloadToken(picArray[i] + "?imageView2/1/w/200");
					picArray[i] = operate.getDownloadToken(picArray[i]);

				}
			}
			// 获取音频访问权限
			String audio = record.get("eaudio");
			if (audio != null && !audio.equals("")) {
				audio = operate.getDownloadToken(audio);
			}
			record.set("url", picArray).set("eaudio", audio).set("thumbnail", thumbnailArray);
		}
		return list;
	}
	/**
	 * 资源授权并获取图片缩略图 by lk 时刻
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> AuthorizeResourceAndGetThumbnailShowMoments(List<Record> list) {
		QiniuOperate operate = new QiniuOperate();

		for (Record record : list) {
			List<Record> picList = record.get("picList");
			String eMain=record.getLong("eMain").toString();
			
			// 获取图片访问权限
			if (picList.size() != 0) {
				if (picList.size() == 1) {
					String p=picList.get(0).get("poriginal").toString();
					//System.out.println("poriginal="+p);
					//视频封面
					String sourcePcover=picList.get(0).get("pcover");
					picList.get(0).set("pcover", null!=picList.get(0).get("pcover")&&!picList.get(0).get("pcover").equals("")
							?picList.get(0).get("pcover"):getVideoCover(picList.get(0).get("poriginal").toString(),eMain));
					// 只有一张图时返回大一点,缩略图授权
					picList.get(0).set("thumbnail", operate
							.getDownloadToken(p + "?imageView2/2/w/250"));
					// 中等缩略图授权
					picList.get(0).set("midThumbnail", operate
							.getDownloadToken(p + "?imageView2/2/w/1000"));
					// 原图授权
					picList.get(0).set("poriginal",
							operate.getDownloadToken(p));
					int width=345;
					int height=345;
					if(eMain.equals("4")){
						picList.get(0).set("width",width);
						picList.get(0).set("height",height);
//						String result="";
//						if(null!=sourcePcover&&!sourcePcover.equals("")){
//							 result = sentHttpRequestToGetImageInfo(operate.getDownloadToken(sourcePcover + "?imageInfo"));
//							 try{
//								 net.sf.json.JSONObject js=net.sf.json.JSONObject.fromObject(result);
//								//{"size":590849,"format":"png","width":750,"height":1334,"colorModel":"nrgba"}						
//								 width=Integer.parseInt(js.get("width").toString());
//								 height=Integer.parseInt(js.get("height").toString());
//							 }catch(Exception e){
//								 e.printStackTrace();
//							 }finally{
//								 picList.get(0).set("width",width);
//								 picList.get(0).set("height",height);
//							 }
//						}else{
//							result = sentHttpRequestToGetImageInfo(operate.getDownloadToken(p + "?avinfo"));
//							 net.sf.json.JSONObject js=net.sf.json.JSONObject.fromObject(result);
//								//{"size":590849,"format":"png","width":750,"height":1334,"colorModel":"nrgba"}	
//							 if(null!=js.get("streams")&&!js.get("streams").equals("")){
//								 try{
//									 net.sf.json.JSONArray sjs=net.sf.json.JSONArray.fromObject(js.get("streams"));
//									 net.sf.json.JSONObject sjsValue=net.sf.json.JSONObject.fromObject(sjs.get(0));
//									 width=Integer.parseInt(js.get("width").toString());
//									 height=Integer.parseInt(js.get("height").toString());
//								 }catch(Exception e){
//									 e.printStackTrace();
//								 }finally{
//									 picList.get(0).set("width",width);
//									 picList.get(0).set("height",height);
//								 }
//							 }
//						}
//										
//						
					}
					if(eMain.equals("0")){
						String result = sentHttpRequestToGetImageInfo(operate.getDownloadToken(p + "?imageInfo"));	
						 try{
							 net.sf.json.JSONObject js=net.sf.json.JSONObject.fromObject(result);
							 //{"size":590849,"format":"png","width":750,"height":1334,"colorModel":"nrgba"}
							 width=Integer.parseInt(js.get("width").toString());
							 height=Integer.parseInt(js.get("height").toString());
							 if(null!=js.get("orientation")&&
									 (js.get("orientation").toString().equals("Left-bottom")
											 ||js.get("orientation").toString().equals("Right-bottom")
											 ||js.get("orientation").toString().equals("Right-top")||js.get("orientation").toString().equals("Left-top"))){
								 width=Integer.parseInt(js.get("height").toString());
								 height=Integer.parseInt(js.get("width").toString());
							 }
							
						 }catch(Exception e){
							 e.printStackTrace();
						 }finally{
							 picList.get(0).set("width",width);
							 picList.get(0).set("height",height);
						 }
					}
					
				} 
			}

			// 获取音频访问权限
			String audio = record.get("eaudio");
			if (audio != null && !audio.equals("")) {
				audio = operate.getDownloadToken(audio);
			}
			// 地理位置图片授权
			String placePic = record.get("ePlacePic");
			if (placePic != null && !placePic.equals("")) {
				placePic = operate.getDownloadToken(placePic);
			}
			// 拍立得比对图片授权
			String verifyPic = record.get("eVerifyPic");
			if (verifyPic != null && !verifyPic.equals("")) {
				verifyPic = operate.getDownloadToken(verifyPic);
			}

			record.set("picList", picList).set("eaudio", audio).set("ePlacePic", placePic).set("eVerifyPic", verifyPic);
		}
		return list;
	}
	/**
	 * 资源授权并获取图片缩略图
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> AuthorizeResourceAndGetThumbnail(List<Record> list) {
		QiniuOperate operate = new QiniuOperate();

		for (Record record : list) {
			List<Record> picList = record.get("picList");
			String eMain=record.getLong("eMain").toString();
			
			// 获取图片访问权限
			if (picList.size() != 0) {
				if (picList.size() == 1) {
					//视频封面
					picList.get(0).set("pcover", null!=picList.get(0).get("pcover")&&!picList.get(0).get("pcover").equals("")
							?picList.get(0).get("pcover"):getVideoCover(picList.get(0).get("poriginal").toString(),eMain));
					// 只有一张图时返回大一点,缩略图授权
					picList.get(0).set("thumbnail", operate
							.getDownloadToken(picList.get(0).get("poriginal").toString() + "?imageView2/2/w/600"));
					// 中等缩略图授权
					picList.get(0).set("midThumbnail", operate
							.getDownloadToken(picList.get(0).get("poriginal").toString() + "?imageView2/2/w/1000"));
					// 原图授权
					picList.get(0).set("poriginal",
							operate.getDownloadToken(picList.get(0).get("poriginal").toString()));
					
				} else if (picList.size() == 2||picList.size() == 4) {
					for (Record picRecord : picList) {
						//视频封面
						picRecord.set("pcover", null!=picRecord.get("pcover")&&!picRecord.get("pcover").equals("")
								?picRecord.get("pcover"):getVideoCover(picRecord.get("poriginal").toString(),eMain));
						// 缩略图授权
						picRecord.set("thumbnail", operate
								.getDownloadToken(picRecord.get("poriginal").toString() + "?imageView2/2/w/300"));
						// 中等缩略图授权
						picRecord.set("midThumbnail", operate
								.getDownloadToken(picRecord.get("poriginal").toString() + "?imageView2/2/w/1000"));
						// 原图授权
						picRecord.set("poriginal", operate.getDownloadToken(picRecord.get("poriginal").toString()));
						
					}
				} else{
					// 多张则控制大小
					for (Record picRecord : picList) {
						//视频封面
						picRecord.set("pcover", null!=picRecord.get("pcover")&&!picRecord.get("pcover").equals("")
								?picRecord.get("pcover"):getVideoCover(picRecord.get("poriginal").toString(),eMain));
						// 缩略图授权
						picRecord.set("thumbnail", operate
								.getDownloadToken(picRecord.get("poriginal").toString() + "?imageView2/2/w/200"));
						// 中等缩略图授权
						picRecord.set("midThumbnail", operate
								.getDownloadToken(picRecord.get("poriginal").toString() + "?imageView2/2/w/1000"));
						// 原图授权
						picRecord.set("poriginal", operate.getDownloadToken(picRecord.get("poriginal").toString()));
						
					}
				}
			}

			// 获取音频访问权限
			String audio = record.get("eaudio");
			if (audio != null && !audio.equals("")) {
				audio = operate.getDownloadToken(audio);
			}
			// 地理位置图片授权
			String placePic = record.get("ePlacePic");
			if (placePic != null && !placePic.equals("")) {
				placePic = operate.getDownloadToken(placePic);
			}
			// 拍立得比对图片授权
			String verifyPic = record.get("eVerifyPic");
			if (verifyPic != null && !verifyPic.equals("")) {
				verifyPic = operate.getDownloadToken(verifyPic);
			}

			record.set("picList", picList).set("eaudio", audio).set("ePlacePic", placePic).set("eVerifyPic", verifyPic);
		}
		return list;
	}
/**
 * BY LK 20171108 获取段视频封面
 */
	public String getVideoCover(String url,String type){
		String cover="";
		if(type.equals("4")){
			QiniuOperate operate = new QiniuOperate();
			cover=operate
			.getDownloadToken(url + "?vframe/jpg/offset/1/w/750");
		}
		return cover;
	}
	/**
	 * 单个资源授权
	 * 
	 * @param list
	 * @param field
	 * @return
	 */
	public List<Record> AuthorizeSingleResource(List<Record> list, String field) {

		QiniuOperate operate = new QiniuOperate();

		for (Record record : list) {
			record.set(field, operate.getDownloadToken(record.get(field).toString()));

		}
		return list;
	}

	/**
	 * 获取消息中评论与邀请各自未读的数量
	 * 
	 * @param CommentNum
	 * @param InviteNum
	 * @return
	 */
	public Record getUnreadMessageNum(Record CommentNum, Record InviteNum) {
		Record numRecord = new Record();
		numRecord.set("commentNum", CommentNum.get("number"));
		numRecord.set("inviteNum", InviteNum.get("number"));
		return numRecord;
	}

	/**
	 * 将两个List合并成一个的处理类
	 * 
	 * @param list1
	 * @param list2
	 * @return
	 */
	public List<Record> combineTwoList(List<Record> list1, List<Record> list2) {
		for (Record record : list2) {
			list1.add(record);
		}
		return list1;
	}

	/**
	 * 将单一参数转成List<Record>
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public List<Record> makeSingleParamToList(String key, Object value) {
		Record record = new Record();
		record.set(key, value);
		List<Record> list = new ArrayList<Record>();
		list.add(record);
		return list;
	}

	/**
	 * 获取地址数组
	 * 
	 * @param picAddress
	 * @return
	 */
	public String[] getPicAddress(String picAddress, String mode) {
		String[] picArray = picAddress.split(",");
		for (int i = 0; i < picArray.length; i++) {
			if ((picArray[i].substring(0, 7)).equals("http://")) {
				picArray[i] = picArray[i];
			} else {
				if (mode == null || mode.equals("")) {
					picArray[i] = CommonParam.qiniuOpenAddress + picArray[i];
				} else {
					picArray[i] = CommonParam.qiniuPrivateAddress + picArray[i];
				}

			}
		}
		return picArray;
	}

	/**
	 * 封装评论列表中的用户类（即封装评论人与被评论人的信息）
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> encapsulationCommentList(List<Record> list) {
		for (Record record : list) {
			// 创建用户对象并赋值
			User commentUser = new User().set("userid", record.get("cuid"))
					.set("unickname", record.get("cunickname").toString()).set("upic", record.get("cpic").toString());
			User commentedUser = new User().set("userid", record.get("ruid"))
					.set("unickname", record.get("runickname").toString()).set("upic", record.get("rpic").toString());
			// 移除记录中多余的记录
			record.remove("cuid").remove("cunickname").remove("cpic").remove("ruid").remove("runickname")
					.remove("rpic");
			// 将用户对象添加到record中
			record.set("commentUser", commentUser).set("commentedUser", commentedUser);
		}
		return list;
	}

	/**
	 * 封装事件列表中的用户类（即封装发布者信息）
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> encapsulationEventList(List<Record> list) {
		for (Record record : list) {
			// 创建用户对象并赋值
			User user = new User().set("userid", record.get("userid"))
					.set("unickname", record.get("unickname").toString()).set("upic", record.get("upic").toString());
			// 移除记录中多余的记录
			record.remove("userid").remove("unickname").remove("upic");
			// 将用户对象添加到record中
			record.set("publishUser", user);
		}
		return list;
	}

	/**
	 * 封装历史记录中的用户类（即封装消息发布者的个人信息）
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> encapsulationChatMessagePublisher(List<Record> list) {
		for (Record record : list) {
			// 创建用户对象并赋值
			User user = new User().set("userid", record.get("chatFrom"))
					.set("unickname", record.get("unickname").toString()).set("upic", record.get("upic").toString());
			// 移除记录中多余的记录
			record.remove("unickname").remove("upic");
			// 将用户对象添加到record中
			record.set("chatFrom", user);
		}
		return list;
	}

	/**
	 * 封装用户信息
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> encapsulationUserInfo(List<Record> list) {
		for (Record record : list) {
			// 创建用户对象并赋值
			Record temp = new Record().set("userid", record.get("userid"))
					.set("unickname", record.get("unickname").toString()).set("upic", record.get("upic").toString())
					.set("noteName", record.get("noteName"));
			// 移除记录中多余的记录
			record.remove("userid").remove("unickname").remove("upic").remove("noteName");
			// 将用户对象添加到record中
			record.set("user", temp);
		}
		return list;
	}

	/**
	 * 组类型转换处理类
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> changeGroupTypeIntoWord(List<Record> list) {
		String type;
		for (Record record : list) {
			type = record.get("gtype").toString();
			switch (type) {
			case "0":
				record.set("gtype", "家人相册");
				break;
			case "1":
				record.set("gtype", "闺蜜相册");
				break;
			case "2":
				record.set("gtype", "死党相册");
				break;
			case "3":
				record.set("gtype", "情侣相册");
				break;
			case "4":
				record.set("gtype", "其他相册");
				break;
			case "5":
				record.set("gtype", "官方相册");
				break;
			default:
				break;
			}
		}
		return list;
	}
	
	public List<Record> changeGroupTypeIntoWordNew(List<Record> list) {
		String type;
		String groupid;
		for (Record record : list) {
			groupid = record.get("groupid").toString();
			Group group = new Group().findById(groupid);
			type = group.get("gtype").toString();
			String gname = group.get("gname").toString();
			switch (type) {
			case "0":
				record.set("gtype", "家人相册").set("gname", gname);
				break;
			case "1":
				record.set("gtype", "闺蜜相册").set("gname", gname);
				break;
			case "2":
				record.set("gtype", "死党相册").set("gname", gname);
				break;
			case "3":
				record.set("gtype", "情侣相册").set("gname", gname);
				break;
			case "4":
				record.set("gtype", "其他相册").set("gname", gname);
				break;
			case "5":
				record.set("gtype", "官方相册").set("gname", gname);
				break;
			default:
				break;
			}
		}
		return list;
	}

	/**
	 * 订单状态转换处理方法
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> changeOrderStatusIntoWord(List<Record> list) {
		String type;
		for (Record record : list) {
			type = record.get("ebOrderStatus").toString();
			switch (type) {
			case "0":
				record.set("ebOrderStatus", "未支付");
				break;
			case "1":
				record.set("ebOrderStatus", "已支付");
				break;
			case "2":
				record.set("ebOrderStatus", "制作中");
				break;
			case "3":
				record.set("ebOrderStatus", "已发货");
				break;
			case "4":
				record.set("ebOrderStatus", "已签收");
				break;
			case "5":
				record.set("ebOrderStatus", "已取消");
				break;
			case "6":
				record.set("ebOrderStatus", "已关闭");
				break;
			case "7":
				record.set("ebOrderStatus", "已删除");
			default:
				break;
			}
		}
		return list;
	}

	/**
	 * 将订单状态转成相应的数字
	 * 
	 * @param groupType
	 * @return
	 */
	public int changeOrderStatusIntoNumber(String status) {
		int num;
		switch (status) {
		case "未支付":
			num = 0;
			break;
		case "已支付":
			num = 1;
			break;
		case "制作中":
			num = 2;
			break;
		case "已发货":
			num = 3;
			break;
		case "已签收":
			num = 4;
			break;
		case "已取消":
			num = 5;
			break;
		case "已关闭":
			num = 6;
			break;
		case "已删除":
			num = 7;
			break;
		default:
			num = -1;
			break;
		}
		return num;
	}

	/**
	 * 获取中文名称的组类型
	 * 
	 * @param type
	 * @return
	 */
	public String getGroupType(String type) {
		String newType = "";
		switch (type) {
		case "0":
			newType += "家人相册";
			break;
		case "1":
			newType += "闺蜜相册";
			break;
		case "2":
			newType += "死党相册";
			break;
		case "3":
			newType += "情侣相册";
			break;
		case "4":
			newType += "其他相册";
			break;
		case "5":
			newType += "官方相册";
			break;
		default:
			break;
		}
		return newType;
	}

	/**
	 * 将组类型转成相应的ID
	 * 
	 * @param groupType
	 * @return
	 */
	public int changeGroupTypeWordIntoNumber(String groupType) {
		int gtype;
		switch (groupType) {
		case "家人相册":
			gtype = 0;
			break;
		case "闺蜜相册":
			gtype = 1;
			break;
		case "死党相册":
			gtype = 2;
			break;
		case "情侣相册":
			gtype = 3;
			break;
		case "其他相册":
			gtype = 4;
			break;
		case "官方相册":
			gtype = 5;
			break;
		default:
			gtype = -1;
			break;
		}
		return gtype;
	}

	/**
	 * 获取聊天消息类型的名称
	 * 
	 * @param type
	 * @return
	 */
	public String getChatType(String type) {
		String newType = "";
		switch (type) {
		case "0":
			newType += "文本";
			break;
		case "1":
			newType += "图片";
			break;
		case "2":
			newType += "语音";
			break;
		case "3":
			newType += "位置";
			break;
		case "4":
			newType += "记录卡片";
			break;
		case "5":
			newType += "时光明信片";
			break;
		case "6":
			newType += "时光印记";
			break;
		}
		return newType;
	}

	/**
	 * 聊天类型转换成相应中文
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> changeChatTypeIntoWord(List<Record> list) {
		String type;
		for (Record record : list) {
			type = record.get("chatType").toString();
			switch (type) {
			case "0":
				record.set("chatType", "文本");
				break;
			case "1":
				record.set("chatType", "图片");
				break;
			case "2":
				record.set("chatType", "语音");
				break;
			case "3":
				record.set("chatType", "位置");
				break;
			case "4":
				record.set("chatType", "记录卡片");
				break;
			case "5":
				record.set("chatType", "时光明信片");
				break;
			case "6":
				record.set("chatType", "时光印记");
				break;
			default:
				break;
			}
		}
		return list;
	}

	/**
	 * 将聊天消息类型转成相应的数字
	 * 
	 * @param chatType
	 * @return
	 */
	public int changeChatTypeIntoNumber(String chatType) {
		int num;
		switch (chatType) {
		case "文本":
			num = 0;
			break;
		case "图片":
			num = 1;
			break;
		case "语音":
			num = 2;
			break;
		case "位置":
			num = 3;
			break;
		case "记录卡片":
			num = 4;
			break;
		case "时光明信片":
			num = 5;
			break;
		case "时光印记":
			num = 6;
			break;
		default:
			num = -1;
			break;
		}
		return num;
	}

	/**
	 * 优惠券使用情况类型转换成相应中文
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> changeCouponStatusIntoWord(List<Record> list) {
		String type;
		for (Record record : list) {
			type = record.get("couponStatus").toString();
			switch (type) {
			case "0":
				record.set("couponStatus", "未使用");
				break;
			case "1":
				record.set("couponStatus", "已使用");
				break;
			case "2":
				record.set("couponStatus", "已过期");
				break;
			default:
				break;
			}
		}
		return list;
	}

	/**
	 * 获取通知内容
	 * 
	 * @param List
	 * @return
	 */
	public String getNotificationContent(Record record, String type) {

		String content = "";

		switch (type) {
		case "enter":
			content = "加入了圈子“" + record.getStr("gname") + "”";
			break;
		case "delete":
			content = "已经解散了圈子“" + record.getStr("gname") + "”";
			break;
		case "system":
			content = "系统通知~";
			break;
		case "quit":
			content = "退出了圈子“" + record.getStr("gname") + "”";
			break;
		case "success":
			content = "你已成功进入圈子“" + record.getStr("gname") + "”";
			break;
		case "kick":
			content = "你已被移出圈子“" + record.getStr("gname") + "”";
			break;
		case "contribute":
			content = "在官方空间“" + record.getStr("gname") + "”投稿啦~";
			break;
		case "refuseContribution":
			content = "您在官方空间“" + record.getStr("gname") + "”中的投稿没有通过审核";
			break;
		case "acceptContribution":
			content = "您在官方空间“" + record.getStr("gname") + "”中的投稿通过审核";
			break;
		default:
			break;
		}

		return content;
	}

	/**
	 * 根据插入flag返回插入结果
	 * 
	 * @param flag
	 * @return
	 */
	public String insertFlagResult(boolean flag) {
		String jsonString = "";
		JsonData jsonData = new JsonData();
		if (flag) {
			jsonString = jsonData.getJson(0, "success");
		} else {
			jsonString = jsonData.getJson(-50, "插入数据失败");
		}
		return jsonString;
	}

	/**
	 * 根据更新flag返回更新结果 1.1版本
	 * 
	 * @param flag
	 * @return
	 */
	public String updateFlagResult(boolean flag) {
		String jsonString = "";
		JsonData jsonData = new JsonData();
		if (flag) {
			jsonString = jsonData.getJson(0, "success");
		} else {
			jsonString = jsonData.getJson(-51, "更新数据失败");
		}
		return jsonString;
	}

	/**
	 * 根据删除flag返回更新结果
	 * 
	 * @param flag
	 * @return
	 */
	public String deleteFlagResult(boolean flag) {
		String jsonString = "";
		JsonData jsonData = new JsonData();
		if (flag) {
			jsonString = jsonData.getJson(0, "success");
		} else {
			jsonString = jsonData.getJson(-52, "删除数据失败");
		}
		return jsonString;
	}

	/**
	 * 将list中的内容拼接成string并返回
	 * 
	 * @param list
	 * @param field
	 * @return
	 */
	public String changeListToString(List<Record> list, String field) {
		String result = "";
		if (list.isEmpty())
			return result;

		for (Record record : list) {
			result += record.get(field).toString();
			result += ",";
		}
		result = result.substring(0, result.length() - 1);
		return result;
	}

	/**
	 * 判断两个时间哪个早，第一个参数的值比第二个的早则返回true
	 * 
	 * @param time1
	 * @param time2
	 * @return
	 */
	public boolean compareTwoTime(String time1, String time2) {
		// 如果想比较日期则写成"yyyy-MM-dd"就可以了
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		// 将字符串形式的时间转化为Date类型的时间
		Date a = new Date();
		Date b = new Date();
		try {
			a = sdf.parse(time1);
			b = sdf.parse(time2);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Date类的一个方法，如果a早于b返回true，否则返回false
		if (a.before(b))
			return true;
		else
			return false;
	}

	/**
	 * 计算两个日期的差值
	 */
	public int computeTheDifferenceOfTwoDate(String now, String plan) {
		Calendar ca = Calendar.getInstance();
		String year = String.valueOf(ca.get(Calendar.YEAR));
		now = (year + "-" + now);
		plan = (year + "-" + plan);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		int different = -1;
		try {
			long to = df.parse(plan).getTime();
			long from = df.parse(now).getTime();
			different = (int) ((to - from) / (1000 * 60 * 60 * 24));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return different;
	}

	/**
	 * 将音乐相册中的用户信息封装成对象
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> changeUserInfoIntoObeject(List<Record> list) {
		for (Record record : list) {
			Long userid = record.get("maCreatorID");
			String nickname = record.get("unickname").toString();
			String upic = record.get("upic").toString();
			Record newRecord = new Record().set("maCreatorID", userid).set("unickname", nickname).set("upic", upic);
			record.remove("maCreatorID").remove("unickname").remove("upic");
			record.set("userInfo", newRecord);
		}
		return list;
	}

	/**
	 * 关联动态与其标签
	 */
	public List<Record> combineEventWithTags(List<Record> eventList) {
		for (Record record : eventList) {
			String eid = record.get("eid").toString();
			List<Record> tagList = Db
					.find("select tagID,tagContent from tags where tagEventID=" + eid + " and tagStatus=0 ");
			record.set("tags", tagList);
		}
		return eventList;
	}

	/**
	 * 关联动态与其点赞
	 */
	public List<Record> combineEventWithLike(List<Record> eventList, String source) {
		for (Record record : eventList) {
			String eid = record.get("eid").toString();
			List<Record> likeList = Db.find(
					"select likeID,likeUserID,likeStatus,upic from `like`,users where userid=likeUserID and likeEventID="
							+ eid + " and likeStatus!=1 ");
			// app不转换成单词，小程序才转换
			if (source != null && !source.equals("app")) {
				likeList = changeLikeStatusToWord(likeList);
			}
			record.set("like", likeList);
		}
		return eventList;
	}/**
	 * 关联点赞数
	 */
	public List<Record> combineEventWithLikeNum(List<Record> eventList, String source,int userid,String ownUserid) {
		for (Record record : eventList) {
			String eid = record.get("eid").toString();
			List<Record> likeList = Db.find(
					"select count(*) as likenum from `like` where likeEventID="+ eid);
			record.set("like", likeList.get(0).getLong("likenum"));
			if(ownUserid != null && !ownUserid.equals("")) {
				List<Record> likeStatus = Db.find(
						"select * from `like` where likeUserID="+ownUserid+ " and likeEventID="+ eid);
				if(likeStatus.size()!=0) {
					record.set("likeStatus", 1);
				}else {
					record.set("likeStatus", 0);
				}
			}
			
			
		}
		return eventList;
	}
	
	
	

	/**
	 * 关联动态和卡片样式
	 */
	public List<Record> combineEventWithCardStyle(List<Record> eventList) {
		for (Record record : eventList) {
			String cardstyle = record.get("ecardstyle").toString();
			Record cardstyleRecord = Db
					.findFirst("select csid,cspic,csurl from cardstyle where csid=" + cardstyle + " ");
			record.set("cardstyle", cardstyleRecord);
			record.remove("ecardstyle");
		}
		return eventList;
	}

	/**
	 * 解析加密数据获得相应数据
	 */
	public String decryptData(String code, String type) {
		String data = "";
		// 处理因传输而丢失的数据。将空格换成加号
		code = code.replace(" ", "+");
		try {
			String result = DES.decryptDES(code, CommonParam.DESSecretKey);
			String[] array = result.split(",");
			for (int i = 0; i < array.length; i++) {
				if (((array[i].split("="))[0]).equals(type)) {
					data = (array[i].split("="))[1];
				}
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error("解密失败：" + e.toString());
			e.printStackTrace();
		}
		return data;
	}

	/**
	 * 获取图片访问权限
	 */
	public List<Record> getPicAccess(List<Record> list, String type) {
		QiniuOperate operate = new QiniuOperate();
		for (Record record : list) {
			String url = "";
			String[] urlArray = record.get("url").toString().split(",");
			for (int i = 0; i < urlArray.length; i++) {
				urlArray[i] = operate.getDownloadToken(urlArray[i]);
				url += (urlArray[i] + ",");
			}
			if (!url.equals("")) {
				url = url.substring(0, url.length() - 1);
			}
			record.set("url", url);
		}

		return list;
	}

	/**
	 * 单个公共资源前缀处理
	 */
	public String singleOpenResourcePrefix(String data) {
		if (data != null && !data.equals("")) {
			data = CommonParam.qiniuOpenAddress + data;
		}
		return data;
	}

	/**
	 * 电商图片地址字符串处理
	 */
	public String EBPicStringProcess(String picAddress) {
		String[] picArray = picAddress.split(",");
		String newUrl = "";
		for (int i = 0; i < picArray.length; i++) {
			if ((picArray[i].substring(0, 7)).equals("http://")) {
				newUrl += picArray[i] + ",";
			} else {
				newUrl += CommonParam.qiniuOpenAddress + picArray[i] + ",";
			}
		}
		newUrl = newUrl.substring(0, newUrl.length() - 1);
		return newUrl;
	}

	/**
	 * 获取列表中图片原图与缩略图的访问权限，等比例
	 */
	public List<Record> GetOriginAndThumbnailAccess(List<Record> list, String field) {
		QiniuOperate qiniu = new QiniuOperate();

		for (Record record : list) {
			String url = record.get(field).toString();
			String thumbnail = url + "?imageView2/2/w/300";
			String midThumbnail = url + "?imageView2/2/w/500";
			url = qiniu.getDownloadToken(url);
			thumbnail = qiniu.getDownloadToken(thumbnail);
			midThumbnail = qiniu.getDownloadToken(midThumbnail);

			record.set(field, url).set("thumbnail", thumbnail).set("midThumbnail", midThumbnail);
		}
		return list;
	}

	/**
	 * 获取列表中图片原图与缩略图的访问权限，直接裁剪
	 */
	public List<Record> GetOriginAndThumbnailAccessWithDirectCut(List<Record> list, String field) {
		QiniuOperate qiniu = new QiniuOperate();
		for (Record record : list) {
			//by lk 20171108 视频封面
			String pcover="";
			if(null!=record.get("pcover")&&!record.get("pcover").equals("")){
				pcover=record.get("pcover");
			}else{				
				pcover=getVideoCover(record.get("url").toString(),record.getLong("eMain").toString());
			}
					
			record.set("pcover", pcover);
			//by lk 20171108 视频封面 end
			String url = record.get(field).toString();
			String thumbnail = url + "?imageView2/1/w/200";
			String midThumbnail = url + "?imageView2/1/w/500";
			url = qiniu.getDownloadToken(url);
			thumbnail = qiniu.getDownloadToken(thumbnail);
			midThumbnail = qiniu.getDownloadToken(midThumbnail);
			record.set(field, url).set("thumbnail", thumbnail).set("midThumbnail", midThumbnail);
		}
		return list;
	}
	/**
	 * 按照前段要求将
	 */
	public List<Record> GetOriginAndThumbnailAccessWithDirectCutNew2(List<Record> list, String field) {
		QiniuOperate qiniu = new QiniuOperate();
		for (Record record : list) {
			//by lk 20171108 视频封面
			String pcover="";
			if(null!=record.get("pcover")&&!record.get("pcover").equals("")){
				pcover=record.get("pcover");
			}else{				
				pcover=getVideoCover(record.get("url").toString(),record.getLong("eMain").toString());
			}
			System.out.println(pcover);		
			record.set("pcover", pcover);
			//by lk 20171108 视频封面 end
			String url = record.get(field).toString();
			String thumbnail = url + "?imageView2/1/w/200";
			String midThumbnail = url + "?imageView2/1/w/500";
			url = qiniu.getDownloadToken(url);
			thumbnail = qiniu.getDownloadToken(thumbnail);
			midThumbnail = qiniu.getDownloadToken(midThumbnail);
			record.set("poriginal", url).set("thumbnail", thumbnail).set("midThumbnail", midThumbnail);
		}
		return list;
	}
	
	/**
	 * 获取列表中图片原图与缩略图的访问权限，直接裁剪
	 */
	public List<Record> GetOriginAndThumbnailAccessWithDirectCutNew(List<Record> list) {
		QiniuOperate qiniu = new QiniuOperate();
		for (Record record : list) {
			//by lk 20171108 视频封面
			String pcover="";
			if(null!=record.get("pcover")&&!record.get("pcover").equals("")){
				pcover=record.get("pcover");
			}else{				
				pcover=getVideoCover(record.get("url").toString(),record.get("pMain").toString());
			}
					
			record.set("pcover", pcover);
			//by lk 20171108 视频封面 end
			String url = record.get("url").toString();
			String thumbnail = url + "?imageView2/1/w/200";
			String midThumbnail = url + "?imageView2/1/w/500";
			url = qiniu.getDownloadToken(url);
			thumbnail = qiniu.getDownloadToken(thumbnail);
			midThumbnail = qiniu.getDownloadToken(midThumbnail);
			record.set("poriginal", url).set("thumbnail", thumbnail).set("midThumbnail", midThumbnail);
		}
		return list;
	}

	/**
	 * 获取列表中图片原图与缩略图的访问权限，并带有图片信息，等比例
	 */
	public List<Record> GetOriginAndThumbnailAccessWithImageInfo(List<Record> list, String field) {
		QiniuOperate qiniu = new QiniuOperate();
		for (Record record : list) {
			String url = record.get("url").toString();
			// 获取图片信息
			String imageInfo = sentHttpRequestToGetImageInfo(qiniu.getDownloadToken(url + "?imageInfo"));
			JSONObject jo = JSONObject.parseObject(imageInfo);
			int width = jo.getIntValue("width");
			int height = jo.getIntValue("height");
			// 获取图片授权后的地址和缩略图地址
			String thumbnail = url + "?imageView2/2/w/300";
			url = qiniu.getDownloadToken(url);
			thumbnail = qiniu.getDownloadToken(thumbnail);
			record.set("url", url).set("thumbnail", thumbnail).set("width", width).set("height", height);
		}
		return list;
	}

	/**
	 * 发送网络请求
	 * 
	 * @param url
	 * @param param
	 * @return
	 */
	public boolean sentNetworkRequest(String url, String value) {
		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("accept", "*/*");
			con.setDoOutput(true);
			con.setDoInput(true);
			PrintWriter out = new PrintWriter(con.getOutputStream());
			value = ("picaddress=" + value);
			out.print(value);
			out.flush();
			con.connect();

			InputStream input = con.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, "utf-8"));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line).append("\n");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;

	}

	/**
	 * 发送网络请求并返回结果
	 */
	public String sentNetworkRequest(String url) {
		String result = "";

		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("accept", "*/*");
			con.setDoOutput(true);
			con.setDoInput(true);
			con.connect();

			InputStream input = con.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, "utf-8"));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line).append("\n");
			}
			result = builder.toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	/**
	 * 发送网络请求并返回结果
	 */
	public String getNetworkRequest(String url) {
		String result = "";

		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("accept", "*/*");
			con.setDoOutput(true);
			con.setDoInput(true);
			con.connect();

			InputStream input = con.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, "utf-8"));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line).append("\n");
			}
			result = builder.toString();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 向指定 URL 发送POST方法的请求
	 * 
	 * @param url
	 *            发送请求的 URL
	 * @param param
	 *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
	 * @return 所代表远程资源的响应结果
	 */
	public String sendPost(String url, String param, String type) {
		PrintWriter out = null;
		BufferedReader in = null;
		String result = "";
		try {
			URL realUrl = new URL(url);
			// 打开和URL之间的连接
			URLConnection conn = realUrl.openConnection();
			// 设置通用的请求属性
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			if (type.equals("json")) {
				conn.setRequestProperty("Content-Type", "application/json");
			}
			// 发送POST请求必须设置如下两行
			conn.setDoOutput(true);
			conn.setDoInput(true);
			// 获取URLConnection对象对应的输出流
			out = new PrintWriter(conn.getOutputStream());
			// 发送请求参数
			out.print(param);
			// flush输出流的缓冲
			out.flush();
			// 定义BufferedReader输入流来读取URL的响应
			in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				result += line;

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
		return result;
	}

	/**
	 * 向七牛云发送请求获取图片信息
	 * 
	 * @param url
	 * @return
	 */
	public String sentHttpRequestToGetImageInfo(String url) {

		String result = "";
		BufferedReader in = null;
		try {
			while (true) {
				URL obj = new URL(url);
				HttpURLConnection con = (HttpURLConnection) obj.openConnection();
				con.setRequestMethod("GET");
				con.setRequestProperty("accept", "*/*");
				con.setDoOutput(true);
				con.setDoInput(true);
				int code = con.getResponseCode();
				if (!(code == 500)) {
					// 系统没有发生错误，则获取返回信息，并返回
					in = new BufferedReader(new InputStreamReader(con.getInputStream()));
					String line;
					while ((line = in.readLine()) != null) {
						result += line;
					}
					break;
				}
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 阿拉伯数字转中文数字
	 */
	public String numberToChineseNumber(String temp) {
		// 单位数组
		String[] units = new String[] { "十", "百", "千", "万", "十", "百", "千", "亿" };

		// 中文大写数字数组
		String[] numeric = new String[] { "零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖" };
		String res = "";
		// 遍历一行中所有数字
		for (int k = -1; temp.length() > 0; k++) {
			// 解析最后一位
			int j = Integer.parseInt(temp.substring(temp.length() - 1, temp.length()));
			String rtemp = numeric[j];

			// 数值不是0且不是个位 或者是万位或者是亿位 则去取单位
			if (j != 0 && k != -1 || k % 8 == 3 || k % 8 == 7) {
				rtemp += units[k % 8];
			}

			// 拼在之前的前面
			res = rtemp + res;

			// 去除最后一位
			temp = temp.substring(0, temp.length() - 1);
		}

		// 去除后面连续的零零..
		while (res.endsWith(numeric[0])) {
			res = res.substring(0, res.lastIndexOf(numeric[0]));
		}

		// 将零零替换成零
		while (res.indexOf(numeric[0] + numeric[0]) != -1) {
			res = res.replaceAll(numeric[0] + numeric[0], numeric[0]);
		}

		// 将 零+某个单位 这样的窜替换成 该单位 去掉单位前面的零
		for (int m = 1; m < units.length; m++) {
			res = res.replaceAll(numeric[0] + units[m], units[m]);
		}

		return res;
	}

	/**
	 * byte[]转String
	 */
	public String convertByteArrayToString(byte[] param) {
		StringBuffer sbuf = new StringBuffer();
		for (int i = 0; i < param.length; i++) {
			sbuf.append(param[i]);
		}
		return (sbuf.toString());
	}

	/**
	 * 改变点赞状态码为对应英文
	 */
	public List<Record> changeLikeStatusToWord(List<Record> list) {
		for (Record record : list) {
			String likeStatus = record.get("likeStatus").toString();
			String status = "";
			// 将type改成对应status
			switch (likeStatus) {
			case "0":
				status = "like";
				break;
			case "2":
				status = "happy";
				break;
			case "3":
				status = "sad";
				break;
			case "4":
				status = "mad";
				break;
			case "5":
				status = "surprise";
				break;
			case "1":
				status = "unlike";
				break;
			}
			record.set("likeStatus", status);
		}
		return list;
	}

	/**
	 * 空间信息与空间照片数拼接
	 * 
	 * @param spaceList
	 * @param photoList
	 * @return
	 */
	public List<Record> combineSpaceInfoWithPhotoNum(List<Record> spaceList, List<Record> photoList, int userid) {

		for (Record groupRecord : spaceList) {

			// 返回结果中增加加密后的groupid
			String groupid = groupRecord.get("groupid").toString();
			String encodeGroupid = "";
			try {
				encodeGroupid = DES.encryptDES("groupid=" + groupid + ",userid=" + userid, CommonParam.DESSecretKey);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			groupRecord.set("encodeGroupid", encodeGroupid);

			// 插入图片数量数据
			boolean flag = false;
			for (Record photoRecord : photoList) {
				if ((groupRecord.get("groupid").toString()).equals((photoRecord.get("groupid").toString()))) {
					groupRecord.set("gpicnum", photoRecord.get("gpicnum"));
					flag = true;
					break;
				}
			}
			if (!flag) {
				groupRecord.set("gpicnum", 0);
				if(groupRecord.get("groupid").toString().equals(CommonParam.pGroupId+"")
						||groupRecord.get("groupid").toString().equals(CommonParam.pGroupId2+"")
						||groupRecord.get("groupid").toString().equals(CommonParam.pGroupId3+"")
						||groupRecord.get("groupid").toString().equals(CommonParam.pGroupId4+"")){
					groupRecord.set("gpicnum", 500000);
				}
			}
		}
		return spaceList;
	}
	
	/**
	 * 空间信息与空间照片数拼接
	 * 
	 * @param spaceList
	 * @param photoList
	 * @return
	 */
	public List<Record> combineSpaceInfoWithPhotoNumNew(List<Record> spaceList, List<Record> photoList, int userid) {

		for (Record groupRecord : spaceList) {

			// 返回结果中增加加密后的groupid
			String groupid = groupRecord.get("groupid").toString();
			String encodeGroupid = "";
			try {
				encodeGroupid = DES.encryptDES("groupid=" + groupid + ",userid=" + userid, CommonParam.DESSecretKey);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			groupRecord.set("encodeGroupid", encodeGroupid);

			// 插入图片数量数据
			boolean flag = false;
			for (Record photoRecord : photoList) {
				if ((groupRecord.get("groupid").toString()).equals((photoRecord.get("groupid").toString()))) {
					groupRecord.set("gpicnum", photoRecord.get("gpicnum"));
					flag = true;
					break;
				}
			}
			if (!flag) {
				groupRecord.set("gpicnum", 0);
			}
		}
		return spaceList;
	}

	/**
	 * 空间信息与空间动态数拼接
	 * 
	 * @param spaceList
	 * @param eventList
	 * @return
	 */
	public List<Record> combineSpaceInfoWithEventNum(List<Record> spaceList, List<Record> eventList) {

		for (Record groupRecord : spaceList) {
			boolean flag = false;
			for (Record eventRecord : eventList) {
				if ((groupRecord.get("groupid").toString()).equals((eventRecord.get("groupid").toString()))) {
					groupRecord.set("geventnum", eventRecord.get("eventnum"));
					flag = true;
					break;
				}
			}
			if (!flag) {
				groupRecord.set("geventnum", 0);
			}
		}
		return spaceList;
	}
	
	/**
	 * 空间信息与空间动态数拼接
	 * 
	 * @param spaceList
	 * @param eventList
	 * @return
	 */
	public List<Record> combineSpaceInfoWithEventNumNew(List<Record> spaceList, List<Record> eventList) {

		for (Record groupRecord : spaceList) {
			boolean flag = false;
			for (Record eventRecord : eventList) {
				if ((groupRecord.get("groupid").toString()).equals((eventRecord.get("groupid").toString()))) {
					groupRecord.set("geventnum", eventRecord.get("eventnum"));
					flag = true;
					break;
				}
			}
			if (!flag) {
				groupRecord.set("geventnum", 0);
			}
		}
		return spaceList;
	}

	/**
	 * 获取动态红包信息
	 * 
	 * @param list
	 * @return
	 */

	public List<Record> getEventRedEnvelopInfo(List<Record> list) {
		RedEnvelop re = new RedEnvelop();
		Grab grab = new Grab();

		for (Record record : list) {
			if (record.get("eMain").toString().equals("5")) {
				String eid = record.get("eid").toString();

				// 插入红包信息
				List<Record> redEnvelopInfo = re.getEventEnvelopBasicInfo(eid);
				if (redEnvelopInfo.size() != 0) {
					String redEnvelopID = redEnvelopInfo.get(0).get("redEnvelopID").toString();
					List<Record> receiveInfo = grab.getRedEnvelopGrabInfo(redEnvelopID);

					// 插入数据
					record.set("redEnvelopInfo", redEnvelopInfo.get(0)).set("receiveInfo", receiveInfo);
				}

			}
		}

		return list;
	}
	/**
	 * 动态数据封装 by lk 时刻
	 * 
	 * @param eventList
	 * @param commentList
	 * @return
	 */
	public List<Record> eventDataEncapsulationShowMoments(List<Record> eventList, List<Record> commentList, String source) {

		Event event = new Event();
		// 关联动态和照片
		for (Record record : eventList) {
			record = event.CombineEventAndPictureShowMoments(record);
		}
		// 封装动态内的用户对象
		//eventList = encapsulationEventList(eventList);

		// 封装动态评论里面的用户对象
		//commentList = encapsulationCommentList(commentList);
		commentList = new ArrayList<Record>();

		// 拼接事件与评论
		List<Record> list = combieEventAndComment(eventList, commentList);

		// 封装事件和点赞
		//list = combineEventWithLike(list, source);

		// 获取动态红包信息
		//list = getEventRedEnvelopInfo(list);

		// 资源授权，并获取图片缩略图
		//list = AuthorizeResourceAndGetThumbnail(list);
		list = AuthorizeResourceAndGetThumbnailShowMoments(list);
		// 返回数据
		return list;
	}
	/**
	 * 动态数据封装
	 * 
	 * @param eventList
	 * @param commentList
	 * @return
	 */
	public List<Record> eventDataEncapsulation(List<Record> eventList, List<Record> commentList, String source) {

		Event event = new Event();
		// 关联动态和照片
		for (Record record : eventList) {
			record = event.CombineEventAndPicture(record);
		}
		// 封装动态内的用户对象
		eventList = encapsulationEventList(eventList);

		// 封装动态评论里面的用户对象
		commentList = encapsulationCommentList(commentList);

		// 拼接事件与评论
		List<Record> list = combieEventAndComment(eventList, commentList);

		// 封装事件和点赞
		list = combineEventWithLike(list, source);

		// 获取动态红包信息
		list = getEventRedEnvelopInfo(list);

		// 资源授权，并获取图片缩略图
		list = AuthorizeResourceAndGetThumbnail(list);

		// 返回数据
		return list;
	}
	
	/**
	 * 动态数据封装
	 * 
	 * @param eventList
	 * @param commentList
	 * @return
	 */
	public List<Record> eventDataEncapsulationNew(List<Record> eventList,String source,int userid,String ownUserid) {

		Event event = new Event();
		// 关联动态和照片
		for (Record record : eventList) {
			record = event.CombineEventAndPictureNew(record);
		}
		// 封装动态内的用户对象New
		//eventList = encapsulationEventList(eventList);
		// 封装事件和点赞
		eventList = combineEventWithLikeNum(eventList, source,userid,ownUserid);
		
		
		// 资源授权，并获取图片缩略图
		eventList = AuthorizeResourceAndGetThumbnail(eventList);

		// 返回数据
		return eventList;
	}


	/**
	 * 空间基本数据封装
	 * 
	 * @param list
	 * @param userid
	 * @return
	 */
	public List<Record> spaceDataEncapsulation(List<Record> list, int userid) {

		// 获取组照片数
		List<Record> photoList = Group.GetSpacePhotoNum(list);

		// 将组照片数插入到组信息中
		list = combineSpaceInfoWithPhotoNum(list, photoList, userid);

		// 获取相册动态数
		List<Record> eventList = Group.GetSpaceEventNum(list);

		// 将组动态数插入到组信息中
		list = combineSpaceInfoWithEventNum(list, eventList);

		// 将组类型转换成相应的文字
		list = changeGroupTypeIntoWord(list);

		return list;
	}
	
	/**
	 * 空间基本数据封装
	 * 
	 * @param list
	 * @param userid
	 * @return
	 */
	public List<Record> spaceDataEncapsulationNew(List<Record> list, int userid) {

		// 获取组照片数
		List<Record> photoList = Group.GetSpacePhotoNumNew(list);

		// 将组照片数插入到组信息中
		list = combineSpaceInfoWithPhotoNumNew(list, photoList, userid);

		// 获取相册动态数
		List<Record> eventList = Group.GetSpaceEventNumNew(list);

		// 将组动态数插入到组信息中
		list = combineSpaceInfoWithEventNumNew(list, eventList);

		// 将组类型转换成相应的文字
		list = changeGroupTypeIntoWordNew(list);

		return list;
	}
	/**
	 * 图片鉴黄
	 */
	public String[] PictureVerify(String[] picArray) {
		long start = System.currentTimeMillis();
		// 创建一个线程池
		ExecutorService exec = Executors.newCachedThreadPool();

		// 分配具体线程数量，每5张图的检验为一个线程
		int length = (int) Math.ceil(picArray.length / 5.0);
		// 执行鉴黄线程
		for (int i = 0; i < length; i++) {
			exec.execute(new VerifyPicture(picArray, i * 5, i * 5 + 4));
		}
		// 关闭线程池
		exec.shutdown();

		// 判断线程池执行完毕再继续执行
		try {
			// awaitTermination返回false即超时会继续循环，返回true即线程池中的线程执行完成主线程跳出循环往下执行，每隔1秒循环一次
			while (!exec.awaitTermination(1, TimeUnit.SECONDS))
				;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		long end = System.currentTimeMillis();
		System.out.println("RunTime:" + (end - start));

		// 筛选图片
		int count = 0;
		for (int i = 0; i < picArray.length; i++) {
			if (!picArray[i].equals(""))
				count++;
		}
		// 存储过滤后的图片数组
		String[] resultArray = new String[count];
		int index = 0;
		for (int i = 0; i < picArray.length; i++) {
			if (!picArray[i].equals("")) {
				resultArray[index] = picArray[i];
				index++;
			}
		}

		// String[] tempArray = new String[picArray.length];
		// int index = 0;
		// int count = 0;
		//
		// for (int i = 0; i < picArray.length; i++) {
		// String address = operate.getDownloadToken(picArray[i] + "?pulp");
		// // 发送请求并获取返回结果
		// String result = sentNetworkRequest(address);
		// if (!result.equals("")) {
		// JSONObject jo = JSONObject.parseObject(result);
		// int code = jo.getIntValue("code");
		// if (code == 0) {
		// JSONObject temp = JSONObject.parseObject(jo.get("pulp")
		// .toString());
		// int label = temp.getIntValue("label");
		// if (label == 2) {
		// // 不是色情或性感图片,添加到图片中
		// tempArray[index] = picArray[i];
		// index++;
		// count++;
		// }
		// }
		// }
		// }
		//
		// String[] resultArray = new String[count];
		// for (int i = 0; i < resultArray.length; i++) {
		// resultArray[i] = tempArray[i];
		// }

		return resultArray;
	}
	/**
	 * 图片鉴黄 by ylm
	 */
	public String[] PictureVerifyNew(String[] picArray) {
		long start = System.currentTimeMillis();
		// 创建一个线程池
		ExecutorService exec = Executors.newCachedThreadPool();
		
		// 分配具体线程数量，每5张图的检验为一个线程
		int length = (int) Math.ceil(picArray.length / 5.0);
		// 执行鉴黄线程
		for (int i = 0; i < length; i++) {
			exec.execute(new VerifyPicture(picArray, i * 5, i * 5 + 4));
		}
		// 关闭线程池
		exec.shutdown();
		
		// 判断线程池执行完毕再继续执行
		try {
			// awaitTermination返回false即超时会继续循环，返回true即线程池中的线程执行完成主线程跳出循环往下执行，每隔1秒循环一次
			while (!exec.awaitTermination(1, TimeUnit.SECONDS))
				;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		long end = System.currentTimeMillis();
		System.out.println("RunTime:" + (end - start));
		
		return picArray;
	}

	/**
	 * 单张图片鉴黄
	 */
	public boolean SinglePictureVerify(String address) {
		address = operate.getDownloadToken(address + "?nrop");
		// 发送请求并获取返回结果
		String result = sentNetworkRequest(address);
		if (!result.equals("")) {
			JSONObject jo = JSONObject.parseObject(result);
			int code = jo.getIntValue("code");
			if (code == 0) {
				JSONArray ja = jo.getJSONArray("fileList");
				JSONObject temp = ja.getJSONObject(0);

				// JSONObject temp = JSONObject.parseObject(jo.get("result").toString());
				int label = temp.getIntValue("label");
				if (label != 2) {
					// 色情图片
					return true;
				} else {
					return false;
				}
			}
		}
		return true;
	}
	/**
	 * 图片鉴黄 by ly YinianDataProcess
	 */
	public String[] PictureVerifyByLy(String[] picArray) {
		long start = System.currentTimeMillis();
		// 创建一个线程池
		ExecutorService exec = Executors.newCachedThreadPool();
		
		// 分配具体线程数量，每5张图的检验为一个线程
		int length = (int) Math.ceil(picArray.length / 5.0);
		// 执行鉴黄线程
		for (int i = 0; i < length; i++) {
			exec.execute(new VerifyPictureNew(picArray, i * 5, i * 5 + 4));
		}
		// 关闭线程池
		exec.shutdown();
		
		// 判断线程池执行完毕再继续执行
		try {
			// awaitTermination返回false即超时会继续循环，返回true即线程池中的线程执行完成主线程跳出循环往下执行，每隔1秒循环一次
			while (!exec.awaitTermination(1, TimeUnit.SECONDS))
				;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		long end = System.currentTimeMillis();
		System.out.println("RunTime:" + (end - start));
		
		return picArray;
	}
	/**
	 * 视频鉴黄
	 */
	public boolean VideoVerify(String address) {
		// 最多6张，10秒钟截图一张
		String url = operate.getDownloadToken(address + "?tupu-video/nrop/f/6/s/10");
		//String result = sentNetworkRequest(url);
		String result = getNetworkRequest(url);		
		if (result.equals("")) {
			return false;
		}
		try {
			JSONObject jo = JSONObject.parseObject(result);
			int code = jo.getIntValue("code");
			if (code == 0) {
				int lable = jo.getIntValue("label");
				// 0--色情 1--性感 2--正常
				return lable == 2 ? false : true;
			} else {
				return true;
			}
		} catch (Exception e) {
			return false;
		}

	}
	/**
	 * 动态数据封装 by lk
	 * 
	 * @param eventList
	 * @param commentList
	 * @return
	 */
	public List<Record> eventDataEncapsulationByLk(List<Record> eventList, List<Record> commentList, String source) {

		Event event = new Event();
		// 关联动态和照片
		System.out.println("时间轴 关联动态和照片 开始："+System.currentTimeMillis());
//		for (Record record : eventList) {
//			record = event.CombineEventAndPicture(record);
//		}
		//by lk 
		StringBuffer eids=new StringBuffer();
		for(Record record:eventList){
			eids.append(record.get("eid").toString()).append(",");
		}
		List<Record> picList=new ArrayList<Record>();
		if(eids.length()>0){
			picList=event.CombineEventAndPictureByLk(eids.substring(0, eids.length()-1).toString());
		}
		Map<String,List<Record>> map=new HashMap<>();
		for(Record pic:picList){
			if(null!=map.get(pic.get("eid").toString())&&!map.get(pic.get("eid").toString()).isEmpty()){
				map.get(pic.get("eid").toString()).add(pic);
			}else{
				List<Record> list=new ArrayList<>();
				list.add(pic);
				map.put(pic.get("eid").toString(), list);
			}
		}
		for(Record record:eventList){
			if(null!=map.get(record.get("eid").toString())){
				record.set("picList", map.get(record.get("eid").toString()));
			}else{
				record.set("picList", new ArrayList<Record>());
			}
		}
		System.out.println("时间轴 关联动态和照片 end："+System.currentTimeMillis());
		System.out.println("时间轴 封装动态内的用户对象 开始："+System.currentTimeMillis());
		// 封装动态内的用户对象
		eventList = encapsulationEventList(eventList);
		System.out.println("时间轴 封装动态内的用户对象 end："+System.currentTimeMillis());
		System.out.println("时间轴 封装动态评论里面的用户对象 开始："+System.currentTimeMillis());
		// 封装动态评论里面的用户对象
		commentList = encapsulationCommentList(commentList);
		System.out.println("时间轴 封装动态评论里面的用户对象 end："+System.currentTimeMillis());
		System.out.println("时间轴 拼接事件与评论 开始："+System.currentTimeMillis());
		// 拼接事件与评论
		List<Record> list = combieEventAndComment(eventList, commentList);
		System.out.println("时间轴 拼接事件与评论 end："+System.currentTimeMillis());
		System.out.println("时间轴 封装事件和点赞 开始："+System.currentTimeMillis());
		// 封装事件和点赞
		list = combineEventWithLikeByLk(list, source);
		System.out.println("时间轴 封装事件和点赞 end："+System.currentTimeMillis());
		System.out.println("时间轴 获取动态红包信息 开始："+System.currentTimeMillis());
		// 获取动态红包信息
		list = getEventRedEnvelopInfo(list);
		System.out.println("时间轴 获取动态红包信息 end："+System.currentTimeMillis());
		System.out.println("时间轴 资源授权，并获取图片缩略图 开始："+System.currentTimeMillis());
		// 资源授权，并获取图片缩略图
		list = AuthorizeResourceAndGetThumbnail(list);
		System.out.println("时间轴 资源授权，并获取图片缩略图 end："+System.currentTimeMillis());
		// 返回数据
		return list;
	}
	//by lk 返回10条点赞情况，并返回所有点赞用户id
		public List<Record> combineEventWithLikeByLk(List<Record> eventList, String source) {
			for (Record record : eventList) {
				String eid = record.get("eid").toString();
				List<Record> likeList = Db.find(
						"select likeID,likeUserID,likeStatus,upic from `like`,users where userid=likeUserID and likeEventID="
								+ eid + " and likeStatus!=1 limit 10");
				// app不转换成单词，小程序才转换
				if (source != null && !source.equals("app")) {
					likeList = changeLikeStatusToWord(likeList);
				}
				List<Record> likeUidList = Db.find(
						"select likeUserID from `like`,users where userid=likeUserID and likeEventID="
								+ eid + " and likeStatus!=1 ");
				record.set("like", likeList);
				record.set("likeCnt", likeUidList.size());
				record.set("likeUser", likeUidList);
			}
			return eventList;
		}	
}
