package yinian.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.interceptor.CrossDomain;
import yinian.model.Comment;
import yinian.model.Event;
import yinian.model.FormID;
import yinian.model.Group;
import yinian.model.GroupCanPublish;
import yinian.model.GroupMember;
import yinian.model.Like;
import yinian.model.Picture;
import yinian.model.TextLibrary;
import yinian.model.User;
import yinian.service.EventService;
import yinian.service.TestService;
import yinian.thread.EventPushPicNumThread;
import yinian.thread.PictureVerifyThread;
import yinian.thread.PictureVerifyThreadNew;
import yinian.thread.VerifyPicture;
import yinian.utils.JsonData;
import yinian.utils.PHash;
import yinian.utils.QiniuOperate;
import yinian.utils.SmallAppQRCode;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.log.Logger;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.ehcache.CacheKit;
import com.jfinal.upload.UploadFile;

public class EventController extends Controller {
	

	private String jsonString; // 返回的json字符串
	private JsonData jsonData = new JsonData(); // json操作类
	private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类
	private EventService service = new EventService();// 业务层对象
	private QiniuOperate qiniu = new QiniuOperate();
	// enhance方法对目标进行AOP增强
	EventService TxService = enhance(EventService.class);
	private static final Logger log = Logger.getLogger(EventController.class);

	/**
	 * 获取文字库类型
	 */
	public void GetTextLibraryType() {
		TextLibrary tl = new TextLibrary();
		List<Record> list = tl.GetAllTextType();
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 获取文字库内容
	 */
	public void GetTextOfOneTextType() {
		String textType = this.getPara("textType");
		int textID = Integer.parseInt(this.getPara("textID"));
		TextLibrary tl = new TextLibrary();
		List<Record> list = tl.GetTextOfOneTextType(textType, textID);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 上传动态
	 */
	@Before(CrossDomain.class)
	public void UploadEvent() {
		// 谁发到哪个空间内
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String groupid = this.getPara("groupid")==null?"":this.getPara("groupid");
		// 图片
		String picAddress = this.getPara("picAddress")==null?"":this.getPara("picAddress");
		// 文字
		String content = this.getPara("content")==null?"":this.getPara("content");
		// 语音
		String audio = this.getPara("audio")==null?"":this.getPara("audio");
		// 地点
		String place = this.getPara("place")==null?"":this.getPara("place");
		String placePic = this.getPara("placePic")==null?"":this.getPara("placePic");// 位置生成的图片地址
		String placeLongitude = this.getPara("placeLongitude")==null?"":this.getPara("placeLongitude");// 经度
		String placeLatitude = this.getPara("placeLatitude")==null?"":this.getPara("placeLatitude");// 纬度
		// 和谁
		String peopleName = this.getPara("peopleName")==null?"":this.getPara("peopleName");
		// 动态以哪个要素为主
		String main = this.getPara("main")==null?"":this.getPara("main"); // 0--照片 1--文字 2--语音 3--地点
		// 其他元素
		String storage = this.getPara("storage")==null?"":this.getPara("storage");// 存储空间
		String source = this.getPara("source")==null?"":this.getPara("source");// 判断接口来源
		String isPush = this.getPara("isPush")==null?"":this.getPara("isPush"); // 推送判断 app:yes/no 小程序:true/false
		String formID = this.getPara("formID")==null?"":this.getPara("formID"); // 小程序推送表单ID

		// 插入formID
		//FormID.insertFormID(userid, formID);
		if(!userid.equals("")&&!formID.equals("")){
			FormID.insert(userid, formID);
		}
		// 判断存储空间是否有传
		double storagePlace;
		if (storage == null || storage.equals("")) {
			storagePlace = 0.00;
		} else {
			storagePlace = Double.parseDouble(storage);
		}

		// 接口来源为web，需要解密
		if (source != null && source.equals("web")) {
			groupid = dataProcess.decryptData(groupid, "groupid");
		}

		// 地址字段数据处理
		String firstPic = null;
		String[] picArray = new String[0];
		if (picAddress != null && !picAddress.equals("")) {
			picArray = dataProcess.getPicAddress(picAddress, "private");
			// 图片鉴黄
		//	picArray = dataProcess.PictureVerify(picArray);
			// 获取动态第一张图片地址,可能没有上传图片
			firstPic = (picArray.length == 0 ? null : picArray[0]);
		}

		// 图片都被过滤掉，不插入数据
		int eid = 0;
		if (main.equals("0") && picArray.length == 0) {
			List<Record> errorList = new ArrayList<Record>();
			Record r = new Record();
			r.set("picList", new ArrayList<String>());
			errorList.add(r);
			jsonString = jsonData.getSuccessJson(errorList);
		} else {
			// 支持同时上传到多个空间
			String[] IDs = groupid.split(",");
			// 逐个空间上传
			for (int i = 0; i < IDs.length; i++) {
				// 同步标记,0--非同步 1--同步 ,第一个空间为原创，其他为同步
				int isSynchronize = (i == 0 ? 0 : 1);
				eid = TxService.upload(userid, IDs[i], picArray, content, audio, place, placePic, placeLongitude,
						placeLatitude, peopleName, main, storagePlace, firstPic, isPush, source, isSynchronize, formID);
				if (eid != 0) {
					// 说明上传成功
					List<Record> result = service.getSingleEvent(eid, source);// 获取动态的信息
					jsonString = jsonData.getSuccessJson(result);
					//开启推送 by lk
//					if(CommonParam.canPublish){
//						Group group = new Group().findById(IDs[i]);
//						String gOrigin = String.valueOf(group.getLong("gOrigin"));
//						if (gOrigin.equals("0")) {
//							ExecutorService exec = Executors.newCachedThreadPool();
//							exec.execute(new EventPushPicNumThread(IDs[i], picArray.length,userid));
////							// 关闭线程池
//							exec.shutdown();
//						}
//					}
				} else {
					jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
					break;
				}
			}
		}

		// 返回结果
		renderText(jsonString);
		//雷雨推送？？？？？？
//		if (eid != 0) {
//			String[] IDs = groupid.split(",");
//			for (int i = 0; i < IDs.length; i++) {
//				// 即时上传动态推送开关
//				Boolean eventIsPush = false;
//				// 判断是否是普通相册 在普通相册点赞才推送
//				Group group = new Group().findById(IDs[i]);
//				String gOrigin = String.valueOf(group.getLong("gOrigin"));
//				if (gOrigin.equals("1")) {
//					eventIsPush = false;
//				}
//				if (eventIsPush) {
//					// 创建一个线程池
//					ExecutorService exec = Executors.newCachedThreadPool();
//					System.out.println(picArray.length);
//					exec.execute(new EventPushPicNumThread(IDs[i], picArray.length));
//					// 关闭线程池
//					exec.shutdown();
//				}
//			}
//		}
		//雷雨推送？？？？？？
		//鉴黄线程
		if(eid != 0) {
			// 创建一个线程池
			ExecutorService exec = Executors.newCachedThreadPool();
			// 执行鉴黄线程
			//exec.execute(new PictureVerifyThread(userid,eid, main));
			exec.execute(new PictureVerifyThreadNew(userid,eid, main));
			// 关闭线程池
			exec.shutdown();
		}
	}
	
	/**
	 * 显示时间轴
	 */
	@Before(CrossDomain.class)
	public void ShowTimeAxis() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String type = this.getPara("type");
		int eid = this.getParaToInt("eid");
		String source = this.getPara("source");

		// // 设置该空间对该用户无新动态
		// GroupMember gm = new GroupMember();
		// gm.UpdateSingleGroupMenberNoNewDynamic(groupid,
		// String.valueOf(userid));

		// 初始化时间轴缓存
		List<Record> result;
		if (type.equals("initialize")) {
			result = CacheKit.get("ConcurrencyCache", groupid + "InitializeEvent");
			if (result == null) {
				result = service.getSpaceTimeAxisContent(userid, groupid, type, eid, source);
				CacheKit.put("ConcurrencyCache", groupid + "InitializeEvent", result);
			}
		} else {
			result = service.getSpaceTimeAxisContent(userid, groupid, type, eid, source);
		}

		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);

	}
	/**
	 * 显示时间轴 by lk test
	 */
	@Before(CrossDomain.class)
	public void ShowTimeAxisNew() {
		System.out.println("进入时间轴 "+System.currentTimeMillis());
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String type = this.getPara("type");
		int eid = this.getParaToInt("eid");
		String source = this.getPara("source");//h5 小程序特殊修改
		// 初始化时间轴缓存
		List<Record> result;
		if (type.equals("initialize")) {
			System.out.println("时间轴 读取缓存开始："+System.currentTimeMillis());
			result = CacheKit.get("ConcurrencyCache", groupid + "InitializeEvent_lk_new");
			System.out.println("时间轴 读取缓存结束："+System.currentTimeMillis());
			if (result == null) {
				System.out.println("时间轴 读取DB开始："+System.currentTimeMillis());
				//result = service.getSpaceTimeAxisContent(userid, groupid, type, eid, source);
				result = service.getSpaceTimeAxisContentByLk(userid, groupid, type, eid, source);
				System.out.println("时间轴 读取DB结束："+System.currentTimeMillis());
				System.out.println("时间轴 写入缓存开始："+System.currentTimeMillis());
				CacheKit.put("ConcurrencyCache", groupid + "InitializeEvent_lk_new", result);
				System.out.println("时间轴 写入缓存结束："+System.currentTimeMillis());
			}
		} else {
			//result = service.getSpaceTimeAxisContent(userid, groupid, type, eid, source);
			result = service.getSpaceTimeAxisContentByLk(userid, groupid, type, eid, source);
		}

		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
		System.out.println("退出时间轴 "+System.currentTimeMillis());
	}

	/**
	 * 获取单条动态
	 */
	@Before(CrossDomain.class)
	public void GetSingleEventContent() {
		String eid = this.getPara("eid");
		String userid = this.getPara("userid");
		if(eid==null||eid.equals("")||eid.equals("undefined")||eid.equals("NaN")){
			jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		String source = this.getPara("source");
		if (source != null && source.equals("web")) {
			eid = dataProcess.decryptData(eid, "eid");
		}
		List<Record> result=new ArrayList<Record>();
		result = CacheKit.get("ConcurrencyCache", eid + "GetSingleEventContent");
		if(result==null){
			result = service.getSingleEvent(Integer.parseInt(eid), source);
			CacheKit.put("ConcurrencyCache", eid + "GetSingleEventContent", result);
		}
		
		if (result == null) {
			// 动态被删除
			jsonString = jsonData.getJson(1027, "动态已被删除");
		} else {
//			//用户加入相册 by lk 20171031
//			if(null!=result.get(0)&&userid!=null&&!userid.equals("")||!userid.equals("undefined")||!userid.equals("NaN")){
//				try{
//					int groupid=result.get(0).getLong("egroupid").intValue();
//					GroupMember gm=new GroupMember();
//					if(gm.judgeUserIsInTheAlbum(Integer.parseInt(userid), groupid)){
//						gm.AddGroupMember(Integer.parseInt(userid), groupid);
//					}
//				}catch(Exception e){
//					e.printStackTrace();
//				}
//			}
//			//用户加入相册 by lk 20171031 end 
			jsonString = jsonData.getSuccessJson(result);
		}

		renderText(jsonString);
	}

	/**
	 * 获取活动空间单条动态
	 */
	@Before(CrossDomain.class)
	public void GetActivitySpaceSingleEventContent() {
		String eid = this.getPara("eid");
		String source = this.getPara("source");

		if (source != null && source.equals("web")) {
			eid = dataProcess.decryptData(eid, "eid");
		}

		List<Record> result = service.getSingleEvent(Integer.parseInt(eid), source);
		if (result == null) {
			// 动态被删除
			jsonString = jsonData.getJson(1027, "动态已被删除");
		} else {
			// 获取其他信息
			Event event = new Event().findById(eid);
			Group group = new Group().findById(event.get("egroupid").toString());
			result.get(0).set("gname", group.get("gname")).set("gintroducePic", group.get("gintroducePic"))
					.set("gbanner", group.get("gbanner"));

			jsonString = jsonData.getSuccessJson(result);
		}

		renderText(jsonString);
	}

	/**
	 * 显示我的，第二版
	 */
	public void ShowMe2ndVersion() {
		String userid = this.getPara("userid");
		String minID = this.getPara("minID");

		if (minID == null) {
			// 刷到底没有数据了，直接返回
			jsonString = jsonData.getSuccessJson();
		} else {
			int eid = Integer.parseInt(minID);
			String source = this.getPara("source");
			List<Record> result = service.getMyEvents2ndVersion(userid, eid, source);
			jsonString = jsonData.getSuccessJson(result);
		}

		renderText(jsonString);
	}

	/**
	 * 显示空间成员的动态
	 */
	public void ShowSpaceMemberEvents() {
		// 获取参数
		int userid = this.getParaToInt("userid");
		//int groupid = this.getParaToInt("groupid");
		String minID = this.getPara("minID");
		String source = this.getPara("source");

		if (minID == null||null== this.getPara("groupid")||this.getPara("groupid").equals("")) {
			// 刷到底没有数据了，直接返回
			jsonString = jsonData.getSuccessJson();
		} else {
			int groupid = this.getParaToInt("groupid");
			int eid = Integer.parseInt(minID);
			List<Record> result = service.getSpaceMemberEvents(groupid, userid, eid, source);
			jsonString = jsonData.getSuccessJson(result);
		}
		renderText(jsonString);
	}
	
	/**
	 * 显示空间成员的动态
	 */
	public void ShowSpaceMemberEventsNew() {
		// 获取参数
		int userid = this.getParaToInt("userid");//进入个人中心的个人id
		String ownUserid = this.getPara("ownUserid");//进入他人的个人中心时的自己的id
		//int groupid = this.getParaToInt("groupid");
		int pagenum = Integer.parseInt(this.getPara("pagenum"));
		String source = this.getPara("source");

		int groupid = this.getParaToInt("groupid");
		List<Record> list = service.getSpaceMemberEventsNew(groupid, userid,ownUserid,pagenum,source);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}
	
	/**
	 * 获取空间成员照片数和动态数
	 */
	public void GetSpaceMemberPhotoNum() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");

		List<Record> list = Db.find("select count(*) as num from events,pictures where eid=peid and euserid=" + userid
				+ " and egroupid=" + groupid + " and estatus=0 and pstatus=0 ");
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}
	
	/**
	 * 通过相册id，用户id获取空间成员照片数和动态数
	 */
	public void GetSpaceMemberPhotoEventNum() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		Record record = new Record();
		//查询还相册的相片数
//		List<Record> list = Db.find("select count(*) as num from events,pictures where eid=peid and euserid=" + userid
//				+ " and egroupid=" + groupid + " and estatus=0 and pstatus=0 ");
		List<Record> list = CacheKit.get("ConcurrencyCache", groupid + "_GetSpaceMemberPhotoEventNum_num");
		if(null==list){
			list = Db.find("select count(*) as num from pictures where puserid=" + userid
				+ "  and pstatus=0 and pGroupid=" + groupid);
			CacheKit.put("ConcurrencyCache", groupid + "_GetSpaceMemberPhotoEventNum_num", list);
		}
		record.set("num", 0);
		if(null!=list&&!list.isEmpty()){
			Long num = list.get(0).getLong("num");
			record.set("num", num);
		}
		//查询该相册的动态数
		List<Record> list2 = CacheKit.get("ConcurrencyCache", groupid + "_GetSpaceMemberPhotoEventNum_eventNum");
		if(null==list2){
			list2 = Db.find("select count(*) as eventNum from events where euserid=" + userid
				+ " and egroupid=" + groupid + " and estatus=0");
			CacheKit.put("ConcurrencyCache", groupid + "_GetSpaceMemberPhotoEventNum_eventNum", list2);
		}
		record.set("eventNum", 0);
		if(null!=list2&&!list2.isEmpty()){
			Long eventNum = list2.get(0).getLong("eventNum");
			record.set("eventNum", eventNum);
		}
		//查询相册的创建者和是否是活动相册
		List<Record> list3 = Db.find("select gcreator,gOrigin from groups where groupid=" + groupid);
		Long gcreator = list3.get(0).getLong("gcreator");
		Long gOrigin = list3.get(0).getLong("gOrigin");
		record.set("gcreator", gcreator);//相册的创建者
		record.set("gOrigin", gOrigin);//是否是活动相册
		//分享动态的时候是否显示二维码的开关
		int eventQRCodeCanPublish=new GroupCanPublish().getGroupPublishByType(groupid+"", "1");
		record.set("eventQRCodeCanPublish", eventQRCodeCanPublish);
		List<Record> newReturnList = new ArrayList<Record>();
		newReturnList.add(record);
		jsonString = jsonData.getSuccessJson(newReturnList);
		renderText(jsonString);
	}

	/**
	 * 显示动态点赞列表
	 */
	public void ShowEventLikeList() {
		String eid = this.getPara("eid");
		String type = this.getPara("type");
		String likeID = this.getPara("likeID");

		List<Record> list = new ArrayList<Record>();
		Like like = new Like();
		switch (type) {
		case "initialize":
			list = CacheKit.get("ConcurrencyCache", eid + "ShowEventLikeList");
			if(null==list||list.isEmpty()){
				list = like.InitializeEventLikeLike(eid);
				CacheKit.put("ConcurrencyCache", eid + "ShowEventLikeList", list);
			}
			break;
		case "loading":
			list = like.LoadingEventLikeLike(eid, likeID);
			break;
		}
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/*******************************
	 * <短视频>相关接口 Start
	 *******************************/

	/**
	 * 上传短视频
	 */
	@Before(CrossDomain.class)
	public void UploadShortVideo() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String address = this.getPara("address");
		String content = this.getPara("content");
		String storage = this.getPara("storage");
		String place = this.getPara("place");
		String cover = this.getPara("cover");
		String time = this.getPara("time");
		String source = this.getPara("source");

		// 判断存储空间是否有传
		double storagePlace = ((storage == null || storage.equals("")) ? 0.00 : Double.parseDouble(storage));
		cover = (cover == null ? "" : cover);
		time = (time == null ? "0" : time);

		// 资源地址加前缀
		address = CommonParam.qiniuPrivateAddress + address;
		// 视频鉴黄,视频封面图片鉴黄true为色情视频
		boolean videoJudge = dataProcess.VideoVerify(address);
		boolean coverJudge = false;
		if(!cover.equals(""))
		 coverJudge = dataProcess.SinglePictureVerify(cover);
		
		if (videoJudge || coverJudge) {
			jsonString = jsonData.getJson(1039, "资源违规");
		} else {
			// 支持同时上传到多个空间
			String[] IDs = groupid.split(",");
			boolean flag = true;
			int eventID = 0;
			// 逐个空间上传
			for (int i = 0; i < IDs.length; i++) {
				// 同步标记,0--非同步 1--同步 ,第一个空间为原创，其他为同步
				int isSynchronize = (i == 0 ? 0 : 1);
				// 上传短视频
				int eid = TxService.uploadShortVedio(userid, IDs[i], address, content, storagePlace, place, cover, time,
						isSynchronize, source);
				eventID = eid;
				if (eid == 0) {
					flag = false;
					break;
				}
			}
			if (flag) {
				// 说明上传成功
				List<Record> result = service.getSingleEvent(eventID, source);// 获取动态的信息
				jsonString = jsonData.getSuccessJson(result);
			} else {
				jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
			}
		}

		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 显示短视频墙
	 */
	@Before(CrossDomain.class)
	public void ShowShortVideoWall() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String pid = this.getPara("pid");
		String source = this.getPara("source");

		// 来源为web则需要对groupid解密
		if (source != null && source.equals("web")) {
			groupid = dataProcess.decryptData(groupid, "groupid");
		}

		List<Record> videoList = service.getShortVideoWallContent(userid, groupid, pid);
		jsonString = jsonData.getSuccessJson(videoList);
		renderText(jsonString);

	}

	/******************************* <短视频>相关接口 End *******************************/

	/*******************************
	 * <拍立得>相关接口 Start
	 *******************************/

	/**
	 * 上传拍立得
	 */
	public void UploadScanEvent() {
		// 谁发到哪个空间内
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		// 图片
		String picAddress = this.getPara("picAddress");
		// 比对图片
		String verifyPicAddress = this.getPara("verifyPicAddress");
		// 文字
		String content = this.getPara("content");
		// 语音
		String audio = this.getPara("audio");
		// 红包元素
		String totalNum = this.getPara("totalNum");
		String totalMoney = this.getPara("totalMoney");
		// 其他元素
		String storage = this.getPara("storage");
		String isPush = this.getPara("isPush"); // 推送判断
		// 判断接口来源
		String source = this.getPara("source");

		// 判断存储空间是否有传
		double storagePlace;
		if (storage == null || storage.equals("")) {
			storagePlace = 0.00;
		} else {
			storagePlace = Double.parseDouble(storage);
		}

		// 判断红包是否有包含
		double money;
		int num;
		if (totalNum == null || totalNum.equals("") || totalMoney == null || totalMoney.equals("")) {
			money = 0.00;
			num = 0;
		} else {
			money = Double.parseDouble(totalMoney);
			num = Integer.parseInt(totalNum);
		}

		// 判断余额是否足够
		User user = new User();
		boolean judge = user.JudgeUserBalanceIsEnough(userid, new BigDecimal(String.valueOf(money)));

		if (judge) {
			// 上传动态
			int eid = TxService.uploadScanEvent(userid, groupid, picAddress, verifyPicAddress, content, audio,
					storagePlace, isPush, num, money, source);
			if (eid != 0) {
				// 说明上传成功
				List<Record> result = service.getSingleEvent(eid, source);// 获取动态的信息
				jsonString = jsonData.getSuccessJson(result);
			} else {
				jsonString = jsonData.getJson(1042, "账户余额不足");

			}
		} else {
			jsonString = jsonData.getJson(1042, "账户余额不足");
		}

		// 返回结果
		renderText(jsonString);

	}

	/**
	 * 扫描获取动态
	 */
	public void ScanForEvent() {

		UploadFile uploadFile = this.getFile("uploadFile");
		String groupid = this.getPara("groupid");

		List<Record> resultList = new ArrayList<Record>();

		// 获取空间内的所有拍立得
		Group group = new Group();
		List<Record> scanEventsList = group.getAllScanEventsInSpace(groupid);

		if (scanEventsList.size() == 0) {
			jsonString = jsonData.getJson(1039, "空间内没有拍立得");
		} else {
			File comparePic = uploadFile.getFile();
			if (comparePic == null) {
				jsonString = jsonData.getJson(1, "请求参数缺失");
			} else {
				// 逐个比对，相似度大于86%则返回
				for (Record record : scanEventsList) {
					// 获取比对图片地址并授权
					String verifyPic = qiniu.getDownloadToken(record.getStr("eVerifyPic"));
					// 下载图片并转成文件
					try {
						URL picUrl = new URL(verifyPic);
						BufferedImage compareImg = ImageIO.read(comparePic);
						BufferedImage sourceImg = ImageIO.read(picUrl);

						// 执行对比
						Double compareResult = PHash.calculateSimilarity(PHash.getFeatureValue(compareImg),
								PHash.getFeatureValue(sourceImg));

						// 保存相关动态
						if (compareResult >= 0.86) {
							int eid = Integer.parseInt(record.get("eid").toString());
							List<Record> temp = service.getSingleEvent(eid, "app");
							if (temp != null)
								resultList.add(temp.get(0));
						}

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						continue;
					}
				}
				jsonString = jsonData.getSuccessJson(resultList);
			}

		}
		renderText(jsonString);

	}

	/******************************* <拍立得>相关接口 End *******************************/

	/******************************* <时刻>相关接口 Start *****************************/

	/**
	 * 显示时刻
	 */
	public void ShowMoments() {
		System.out.println("ShowMoments开始："+System.currentTimeMillis());
		String userid = this.getPara("userid");
		String type = this.getPara("type");
		String eid = this.getPara("eid");
		if(userid==null||userid.equals("")||userid.equals("undefined")||userid.equals("NaN")||eid==null||eid.equals("")||eid.equals("undefined")||eid.equals("NaN")){
			jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		List<Record> result = service.GetMoments(userid, type, eid);
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
		System.out.println("ShowMoments结束："+System.currentTimeMillis());
	}

	/******************************* <时刻>相关接口 End *******************************/
	/******************************* <时刻>相关接口 Start *****************************/

	/**
	 * 显示时刻 by lk 简化版本
	 */
	public void ShowMoments_sim() {
		System.out.println("ShowMoments开始："+System.currentTimeMillis());
		String userid = this.getPara("userid");
		String type = this.getPara("type");
		String eid = this.getPara("eid");
		if(userid==null||userid.equals("")||userid.equals("undefined")||userid.equals("NaN")||eid==null||eid.equals("")||eid.equals("undefined")||eid.equals("NaN")){
			jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		List<Record> result = service.GetMoments_sim(userid, type, eid);
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
		System.out.println("ShowMoments结束："+System.currentTimeMillis());
	}
	/**
	 * 显示时刻 by lk 简化版本-不显示活动相册内容
	 */
	public void ShowMoments_sim_noGtype() {
		System.out.println("ShowMoments开始："+System.currentTimeMillis());
		String userid = this.getPara("userid");
		String type = this.getPara("type");
		String eid = this.getPara("eid");
		if(userid==null||userid.equals("")||userid.equals("undefined")||userid.equals("NaN")||eid==null||eid.equals("")||eid.equals("undefined")||eid.equals("NaN")){
			jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		//List<Record> result = service.GetMoments_sim(userid, type, eid);
		List<Record> result = service.GetMoments_sim_noGtype(userid, type, eid);
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
		System.out.println("ShowMoments结束："+System.currentTimeMillis());
	}

	/******************************* <时刻>相关接口 End *******************************/
	
	/**
	 * 分享动态二维码--ly
	 */
	public void shareEventQRCode(){
		QiniuOperate operate=new QiniuOperate();
		SmallAppQRCode small = new SmallAppQRCode();
		String groupid = this.getPara("groupid");
		String userid = this.getPara("userid");
		String eventid = this.getPara("eventid");
		if(eventid!=null&&!eventid.equals("")){
			Event e=new Event().findById(eventid);
			User u = new User().findById(userid);
			Group g = new Group().findById(groupid);
			String gname = g.get("gname");
			String eQRCode = e.get("eQRCode");
			String url = "";
			if(eQRCode == null||eQRCode .equals("")) {
				int numberCharacter = 0;
			    int enCharacter = 0;
			    if(gname.length()>6) {
					gname = gname.substring(0, 6);
					gname = gname + "..";
					for (int i = 0; i < gname.length(); i++) {
			            char tmp = gname.charAt(i);
			            if ((tmp >= 'A' && tmp <= 'Z') || (tmp >= 'a' && tmp <= 'z')) {
			                enCharacter ++;
			            } else if ((tmp >= '0') && (tmp <= '9')) {
			                numberCharacter ++;
			            } 
			        }
					int count = numberCharacter + enCharacter;
					int mm = count/2;
					for(int x=0;x<mm;x++) {
						gname =" "+gname;
					}
				}else {
					
					int len = (6-gname.length())/2;
					int le = (6-gname.length())%2;
					for (int i = 0; i < gname.length(); i++) {
			            char tmp = gname.charAt(i);
			            if ((tmp >= 'A' && tmp <= 'Z') || (tmp >= 'a' && tmp <= 'z')) {
			                enCharacter ++;
			            } else if ((tmp >= '0') && (tmp <= '9')) {
			                numberCharacter ++;
			            } 
			        }
					int count = numberCharacter + enCharacter;
					int mm = count/2;
					if(le==0) {
						for(int x=0;x<=len;x++) {
							gname = "  "+gname;
						}
					}
					if(le==1) {
						for(int x=0;x<=len;x++) {
							gname = "  "+gname;
						}
						gname = " " + gname;
					}
						for(int x=0;x<mm;x++) {
							gname =" "+gname;
						}
				}
				System.out.println(gname);
				String mpicUrl="";
				String picUrl="";
				if(null!=e.get("efirstpic")&&!e.get("efirstpic").equals("")&&
						null!=u.get("upic")&&!u.get("upic").equals("")){
					picUrl=operate.getDownloadToken(u.get("upic")+"?imageView2/1/w/126/h/126/");
					//mpicUrl=operate.getDownloadToken(e.get("efirstpic")+"?imageView2/1/w/500/h/500/");
					mpicUrl =operate.getDownloadToken(e.get("efirstpic")+"?imageMogr2/auto-orient/blur/1x1/thumbnail/1179x");
//						System.out.println(mpicUrl);
				}
				
				url = small.GetShareSmallAppQRCodeURL("eventdetail2", eventid,gname,picUrl,mpicUrl);
				if(url!=null&&url.indexOf("QRCodeError.png")!=-1) {
					
				}else{
					e.set("eQRCode", url);
					e.update();
				}
			}else {
				url = eQRCode;
			}
			Record resultRecord = new Record();
			resultRecord.set("url", url);
			List<Record> resultList = new ArrayList<Record>();
			resultList.add(resultRecord);
			jsonString = jsonData.getSuccessJson(resultList);
			renderText(jsonString);
		 }
	}
	/**
	 * 评论分页加载  EventController

	 */
	public void GetCommentByPaged() {
		String eid = this.getPara("eid");
		String type = this.getPara("type");
		String cid = this.getPara("cid");
		
		List<Record> result = new ArrayList<>();
		if(eid != null && !eid.equals("") && null!=cid && !cid.equals("")) {
			result = new Comment().GetEventComment(eid,cid,type);
		}
		
		//封装动态评论里面的用户对象
		result = dataProcess.encapsulationCommentList(result);
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
	}
}
