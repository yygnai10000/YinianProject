package yinian.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.ObjectUtils.Null;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.model.Comment;
import yinian.model.Event;
import yinian.model.Group;
import yinian.model.GroupCanPublish;
import yinian.model.GroupMember;
import yinian.model.Picture;
import yinian.model.RedEnvelop;
import yinian.model.User;
import yinian.push.PushMessage;
import yinian.push.SmallAppPush;
import yinian.utils.JsonData;
import yinian.utils.QiniuOperate;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Before;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;

public class EventService {

	private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类
	private QiniuOperate qiniu = new QiniuOperate();
	private UserService userService = new UserService();// 用户业务逻辑类
	/**
	 * 上传动态，成功后返回事件的ID by lk 添加图片pGroupid字段
	 * 
	 * @param userid
	 * @param groupid
	 * @param picAddress
	 * @param content
	 * @return
	 */
	@Before(Tx.class)
	public int uploadBySimApp(String userid, String groupid, String[] picArray, String content, String audio, String place,
			String placePic, String placeLongitude, String placeLatitude, String peopleName, String main,
			double storage, String firstPic, String isPush, String source, int isSynchronize, String formID) {

		int eventID = 0;
		GroupMember gm = new GroupMember();

		// 判断地图图片和语音是否有上传
		if (audio != null && !audio.equals("")) {
			audio = CommonParam.qiniuPrivateAddress + audio;
		}
		if (placePic != null && !placePic.equals("")) {
			placePic = CommonParam.qiniuPrivateAddress + placePic;
		}

		// 保存事件
		Event event = new Event().set("egroupid", groupid).set("euserid", userid).set("efirstpic", firstPic)
				.set("etext", content).set("eaudio", audio).set("eplace", place).set("ePlacePic", placePic)
				.set("eMain", main).set("ePeopleName", peopleName).set("eStoragePlace", storage).set("eSource", source)
				.set("isSynchronize", isSynchronize);

		// 参数传值判断
		if (placeLongitude != null && placeLatitude != null && !placeLongitude.equals("")
				&& !placeLatitude.equals("")) {
			event.set("ePlaceLongitude", placeLongitude).set("ePlaceLatitude", placeLatitude);
		}

		if (event.save()) {
			eventID = Integer.parseInt(event.get("eid").toString());

			// app才更新状态 //lk 添加精简版小程序判断 like 小程序
			if (source != null && !source.equals("小程序") &&!source.equals("精简版小程序")&& !source.equals("官网")) {
				// 将事件所属组的状态改为有新状态
				gm.UpdateAllGroupMembersNewDynamic(groupid);
				// 发布者的状态改为无新状态，因为就算别人发了新动态，发布者发布完后还在组内动态界面
				gm.UpdateSingleGroupMenberNoNewDynamic(groupid, userid);
			}

			// 有图片则保存图片
			for (int i = 0; i < picArray.length; i++) {
				Picture pic = new Picture();
				//by lk test
				pic.set("peid", eventID).set("poriginal", picArray[i]).set("pGroupid", groupid).set("pUserid", userid).set("pMain", main);
				pic.save();
			}

		}

		// 增加用户已使用空间
		User user = new User();
		user.updateUserStoragePlace(userid, storage, "add");

		// APP推送
		if (isPush != null && isPush.equals("yes")) {
			PushMessage push = new PushMessage();
			push.pushToSpaceMember(groupid, userid, push.PUSH_TYPE_OF_EVENT);
		}
		// 小程序推送
		if (source != null && source.equals("小程序") && isPush != null && isPush.equals("true")) {
			SmallAppPush.UploadPush(groupid, userid, formID, picArray.length);
		}
		// 精简版小程序推送 by lk 
				if (source != null && source.equals("精简版小程序") && isPush != null && isPush.equals("true")) {
					SmallAppPush.UploadPush(groupid, userid, formID, picArray.length);
				}

		return eventID;

	}
	/**
	 * 上传动态，成功后返回事件的ID
	 * 
	 * @param userid
	 * @param groupid
	 * @param picAddress
	 * @param content
	 * @return
	 */
	@Before(Tx.class)
	public int upload(String userid, String groupid, String[] picArray, String content, String audio, String place,
			String placePic, String placeLongitude, String placeLatitude, String peopleName, String main,
			double storage, String firstPic, String isPush, String source, int isSynchronize, String formID) {

		int eventID = 0;
		GroupMember gm = new GroupMember();

		// 判断地图图片和语音是否有上传
		if (audio != null && !audio.equals("")) {
			audio = CommonParam.qiniuPrivateAddress + audio;
		}
		if (placePic != null && !placePic.equals("")) {
			placePic = CommonParam.qiniuPrivateAddress + placePic;
		}

		// 保存事件
		Event event = new Event().set("egroupid", groupid).set("euserid", userid).set("efirstpic", firstPic)
				.set("etext", content).set("eaudio", audio).set("eplace", place).set("ePlacePic", placePic)
				.set("eMain", main).set("ePeopleName", peopleName).set("eStoragePlace", storage).set("eSource", source)
				.set("isSynchronize", isSynchronize);

		// 参数传值判断
		if (placeLongitude != null && placeLatitude != null && !placeLongitude.equals("")
				&& !placeLatitude.equals("")) {
			event.set("ePlaceLongitude", placeLongitude).set("ePlaceLatitude", placeLatitude);
		}

		if (event.save()) {
			eventID = Integer.parseInt(event.get("eid").toString());

			// app才更新状态 //lk 添加精简版小程序判断 like 小程序
			if (source != null && !source.equals("小程序") &&!source.equals("精简版小程序")&& !source.equals("官网")) {
				// 将事件所属组的状态改为有新状态
				gm.UpdateAllGroupMembersNewDynamic(groupid);
				// 发布者的状态改为无新状态，因为就算别人发了新动态，发布者发布完后还在组内动态界面
				gm.UpdateSingleGroupMenberNoNewDynamic(groupid, userid);
			}

			// 有图片则保存图片
			for (int i = 0; i < picArray.length; i++) {
				Picture pic = new Picture();
				pic.set("peid", eventID).set("poriginal", picArray[i])
				.set("pGroupid", groupid).set("puserid", userid).set("pMain", main);
				pic.save();
			}

		}

		// 增加用户已使用空间
		User user = new User();
		user.updateUserStoragePlace(userid, storage, "add");

		// APP推送
		if (isPush != null && isPush.equals("yes")) {
			PushMessage push = new PushMessage();
			push.pushToSpaceMember(groupid, userid, push.PUSH_TYPE_OF_EVENT);
		}
		// 小程序推送
		
		if (source != null && source.equals("小程序") && isPush != null && isPush.equals("true")) {
			SmallAppPush.UploadPush(groupid, userid, formID, picArray.length);
		}
		// 精简版小程序推送 by lk 
		if (source != null && source.equals("精简版小程序") && isPush != null && isPush.equals("true")) {
			SmallAppPush.UploadPush(groupid, userid, formID, picArray.length);
		}

		return eventID;

	}

	/**
	 * 上传拍立得，成功后返回事件的ID
	 * 
	 * @param userid
	 * @param groupid
	 * @param picAddress
	 * @param content
	 * @return
	 */
	@Before(Tx.class)
	public int uploadScanEvent(String userid, String groupid, String picAddress, String verifyPicAddress,
			String content, String audio, double storage, String isPush, int totalNum, double totalMoney,
			String source) {

		int eventID = 0;
		GroupMember gm = new GroupMember();

		// 将地址字符串转换成数组
		String firstPic = null;
		String[] picArray = new String[0];
		if (picAddress != null && !picAddress.equals("")) {
			picArray = dataProcess.getPicAddress(picAddress, "private");
			// 获取动态第一张图片地址,可能没有上传图片
			firstPic = (picArray.length == 0 ? null : picArray[0]);
		}

		// 判断语音是否有上传
		if (audio != null && !audio.equals("")) {
			audio = CommonParam.qiniuPrivateAddress + audio;
		}

		// 比对图片加上前缀
		verifyPicAddress = CommonParam.qiniuPrivateAddress + verifyPicAddress;

		// 保存事件
		Event event = new Event().set("egroupid", groupid).set("euserid", userid).set("efirstpic", firstPic)
				.set("etext", content).set("eaudio", audio).set("eMain", 5).set("eVerifyPic", verifyPicAddress)
				.set("eStoragePlace", storage).set("eSource", source);

		if (event.save()) {
			eventID = Integer.parseInt(event.get("eid").toString());

			// 将事件所属组的状态改为有新状态
			gm.UpdateAllGroupMembersNewDynamic(groupid);
			// 发布者的状态改为无新状态，因为就算别人发了新动态，发布者发布完后还在组内动态界面
			gm.UpdateSingleGroupMenberNoNewDynamic(groupid, userid);

			// 有图片则保存图片
			for (int i = 0; i < picArray.length; i++) {
				Picture pic = new Picture();
				pic.set("peid", eventID).set("poriginal", picArray[i])
				.set("pGroupid", groupid).set("puserid", userid).set("pMain", 5);
				pic.save();
			}

			// 插入红包信息
			if (totalMoney != 0.00 && totalNum != 0) {
				RedEnvelop re = new RedEnvelop();
				re.set("redEnvelopUserID", userid).set("redEnvelopEventID", eventID).set("redEnvelopTotalNum", totalNum)
						.set("redEnvelopTotalMoney", totalMoney).set("redEnvelopRemainNum", totalNum)
						.set("redEnvelopRemainMoney", totalMoney);
				if (re.save()) {
					String redEnvelopID = re.get("redEnvelopID").toString();
					// 扣除用户钱包总金额
					boolean judge = userService.ExpenseMoney(userid, new BigDecimal(String.valueOf(totalMoney)),
							CommonParam.keyOfSendRedEnvelop, redEnvelopID);
					if (!judge)
						return 0;
				}
			}

		}

		// 增加用户已使用空间
		User user = new User();
		user.updateUserStoragePlace(userid, storage, "add");

		if (isPush == null || isPush.equals("yes")) {
			// 推送消息，默认为推送
			PushMessage push = new PushMessage();
			push.pushToSpaceMember(groupid, userid, push.PUSH_TYPE_OF_EVENT);
		}

		return eventID;

	}

	/**
	 * 上传短视频
	 * 
	 * @param userid
	 * @param groupid
	 * @param address
	 * @param content
	 * @param storagePlace
	 * @return
	 */
	public int uploadShortVedio(String userid, String groupid, String address, String content, double storage,
			String place, String cover, String time, int isSynchronize, String source) {

		int eventID = 0;

		// 保存事件
		Event event = new Event().set("egroupid", groupid).set("euserid", userid).set("eMain", 4).set("etext", content)
				.set("eStoragePlace", storage).set("eplace", place).set("eSource", source)
				.set("isSynchronize", isSynchronize);

		if (event.save()) {
			eventID = Integer.parseInt(event.get("eid").toString());

			/**
			 * 小程序不用更新新动态，app不显示<br>
			 * // 将事件所属组的状态改为有新状态<br>
			 * gm.UpdateAllGroupMembersNewDynamic(groupid);<br>
			 * // 发布者的状态改为无新状态，因为就算别人发了新动态，发布者发布完后还在组内动态界面<br>
			 * gm.UpdateSingleGroupMenberNoNewDynamic(groupid, userid);<br>
			 **/

			// 保存短视频路径
			Picture pic = new Picture();
			pic.set("peid", eventID).set("poriginal", address).set("pcover", cover).set("ptime", time)
			.set("pGroupid", groupid).set("puserid", userid).set("pMain", 4);
			pic.save();

		}

		// 增加用户已使用空间
		User user = new User();
		user.updateUserStoragePlace(userid, storage, "add");

		return eventID;

	}

	/**
	 * 获取单条动态信息
	 * 
	 * @param eventId
	 * @return
	 */
	public List<Record> getSingleEvent(int eid, String source) {

		// 获取单条动态内容
		Event event = new Event();
		List<Record> eventList = event.GetSingleEventContent(eid);
		
		// 动态被删除则返回空
		if (eventList.size() == 0)
			return null;
		int eventQRCodeCanPublish=new GroupCanPublish().getGroupPublishByType(eventList.get(0).getLong("egroupid").intValue()+"", "1");
		// 关联动态和其照片等资源
		for (Record record : eventList) {
			record = event.CombineEventAndPicture(record);
			record.set("eventQRCodeCanPublish", eventQRCodeCanPublish);
		}
		// 获取单条动态评论
		Comment comment = new Comment();
		List<Record> commentList = comment.GetSingleEventComments(eid);

		// 封装事件和评论内的用户类
		eventList = dataProcess.encapsulationEventList(eventList);
		commentList = dataProcess.encapsulationCommentList(commentList);
		List<Record> list = dataProcess.combieEventAndComment(eventList, commentList);
		
		// 第一张图片图片授权
		if (list.get(0).get("efirstpic") != null) {
			String auth = qiniu.getDownloadToken(list.get(0).get("efirstpic").toString());
			list.get(0).set("efirstpic", auth);
		}

		// 判断是否是拍立得动态
		if (list.get(0).get("eMain").toString().equals("5")) {
			// 对比图片授权
			list.get(0).set("eVerifyPic", qiniu.getDownloadToken(list.get(0).get("eVerifyPic").toString()));

			// 插入红包信息
			List<Record> redEnvelopInfo = Db.find(
					"select redEnvelopID,redEnvelopTotalNum,redEnvelopTotalMoney,redEnvelopRemainNum,redEnvelopRemainMoney from redEnvelop where redEnvelopEventID="
							+ eid + " ");
			if (redEnvelopInfo.size() != 0) {
				String redEnvelopID = redEnvelopInfo.get(0).get("redEnvelopID").toString();
				List<Record> receiveInfo = Db.find(
						"select userid,unickname,upic,GrabMoney,GrabTime from grab,users where userid=GrabUserID and GrabRedEnvelopID="
								+ redEnvelopID + "  ");
				list.get(0).set("redEnvelopInfo", redEnvelopInfo.get(0)).set("receiveInfo", receiveInfo);
			}

		}

		// 封装事件和点赞
		list = dataProcess.combineEventWithLike(list, source);
		// 资源授权，并获取图片缩略图
		list = dataProcess.AuthorizeResourceAndGetThumbnail(list);

		return list;
	}

	/**
	 * 获取时间轴内容
	 * 
	 * @param userid
	 * @param groupid
	 * @param type
	 * @param eid
	 * @return
	 */
	public List<Record> getSpaceTimeAxisContent(String userid, String groupid, String type, int eid, String source) {

		// 获取空间所有评论
		List<Record> commentList = new Comment().GetAllCommentsOfOneSpace(groupid);
		// 获取动态内容
		List<Record> eventList = new ArrayList<Record>();
		// 置顶动态列表
		List<Record> topEventList = new ArrayList<Record>();

		// 根据请求类型获取相应数据
		Event event = new Event();
		switch (type) {
		case "initialize":
			// 获取置顶动态
			topEventList = event.GetAllTopEvent(groupid, source);
			// 初始化非置顶动态
			eventList = event.InitializeEventContent(groupid, source);
			eventList = dataProcess.combineTwoList(topEventList, eventList);
			break;
		case "refresh":
			eventList = event.RefreshEventContent(groupid, eid, source);
			break;
		case "loading":
			eventList = event.LoadingEventContent(groupid, eid, source);
			break;
		}

		// 动态数据封装
		List<Record> list = dataProcess.eventDataEncapsulation(eventList, commentList, source);

		// 判断是否为活动相册， 是则随机增加浏览量
		// Group group = new Group().findById(groupid);
		// if (group.get("gtype").toString().equals("12")) {
		// Event.AddRandomView(eventList);
		// }

		// 返回数据
		return list;
	}

	/**
	 * 获取“我”发的所有动态,第二版
	 * 
	 * @param userid
	 * @return
	 */
	public List<Record> getMyEvents2ndVersion(String userid, int minID, String source) {

		// 获取评论列表
		List<Record> commentList = Comment.GetCommentsOfUserUploadEvents(userid);
		// 获取动态列表
		List<Record> eventList = new ArrayList<Record>();

		if (minID <= 0) {
			// 初始化
			eventList = Event.InitializeUserUploadEvents(userid, source);
		} else {
			// 加载
			eventList = Event.LoadingUserUploadEvents(userid, minID, source);
		}

		// 动态数据封装
		List<Record> list = dataProcess.eventDataEncapsulation(eventList, commentList, source);

		// 返回数据
		return list;
	}

	/**
	 * 获取空间成员的动态
	 * 
	 * @param groupid
	 * @param userid
	 * @param minID
	 * @return
	 */
	public List<Record> getSpaceMemberEvents(int groupid, int userid, int minID, String source) {

		// 获取评论列表
		List<Record> commentList = Comment.GetCommentsOfSpaceMemberUploadEvents(userid, groupid);
		// 获取动态列表
		List<Record> eventList = new ArrayList<Record>();

		if (minID <= 0) {
			// 初始化
			eventList = Event.InitializeSpaceMemberUploadEvents(userid, groupid, source);
		} else {
			// 加载
			eventList = Event.LoadingSpaceMemberUploadEvents(userid, groupid, minID, source);
		}

		// 动态数据封装
		List<Record> list = dataProcess.eventDataEncapsulation(eventList, commentList, source);

		return list;
	}
	
	/**
	 * 获取空间成员的动态
	 * 
	 * @param groupid
	 * @param userid
	 * @param minID
	 * @return
	 */
	public List<Record> getSpaceMemberEventsNew(int groupid, int userid,String ownUserid, int pagenum ,String source) {
		String mode = "\"%Y-%m-%d\"";
		int page = (pagenum-1)*10;
		// 获取动态列表
		List<Record> eventList = Db.find("select eid,etext,eMain,elevel,DATE_FORMAT( euploadtime, " + mode+ " ) as euploadtime from `events` where euserid="+userid
				+" and egroupid="+groupid +" and estatus=0"+ " order by euploadtime desc" +" limit "+page + ",10");
		// 动态数据封装
		List<Record> list = dataProcess.eventDataEncapsulationNew(eventList,source,userid,ownUserid);

		return list;
	}
	

	/**
	 * 获取短视频墙内容
	 * 
	 * @param userid
	 * @param groupid
	 * @param pid
	 * @return
	 */
	public List<Record> getShortVideoWallContent(String userid, String groupid, String pid) {
		Event event = new Event();
		List<Record> videoList = new ArrayList<Record>();
		if (pid.equals("0")) {
			videoList = event.initializeShortVideoWall(groupid);
		} else {
			videoList = event.loadingShortVideoWall(groupid, pid);
		}

		// 资源授权
		videoList = dataProcess.AuthorizeSingleResource(videoList, "poriginal");

		// 返回结果
		return videoList;
	}

	/**
	 * 退还红包
	 * 
	 * @param redEnvelopID
	 * @param userid
	 * @return
	 */
	@Before(Tx.class)
	public boolean returnRedEnvelop(String redEnvelopID) {
		// 获取退回时间
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String now = sdf.format(new Date());

		// 将红包设置为已退回
		RedEnvelop re = new RedEnvelop().findById(redEnvelopID);
		String owner = re.get("redEnvelopUserID").toString();

		// 获取退还金额
		BigDecimal leftMoney = new BigDecimal(re.get("redEnvelopRemainMoney").toString());
		// 修改红包状态
		re.set("redEnvelopStatus", 2).set("redEnvelopReturnTime", now);

		// 增加用户余额
		boolean returnFlag = userService.incomeMoney(owner, leftMoney, CommonParam.keyOfRefund, redEnvelopID);

		return re.update() && returnFlag;

	}

	/**
	 * 判断动态是否包含红包
	 * 
	 * @return 0--不包含<br>
	 *         正整数 --红包ID
	 */
	public String JudgeEventIsContainRedEnvelop(String eventID) {
		Event event = new Event().findById(eventID);
		// 先判断是否为拍立得
		if ((event.get("eMain").toString()).equals("5")) {
			List<Record> list = Db.find("select redEnvelopID from redEnvelop where redEnvelopEventID=" + eventID
					+ " and redEnvelopStatus!=2 ");

			return list.size() == 0 ? "0" : list.get(0).get("redEnvelopID").toString();
		} else {
			return "0";
		}
	}

	/**
	 * 获取时刻
	 * 
	 * @param userid
	 * @param type
	 * @param eid
	 * @return
	 */
	public List<Record> GetMoments(String userid, String type, String eid) {
		List<Record> momentsList = new ArrayList<Record>();
		switch (type) {
		case "initialize":
			momentsList = Event.initializeMoments(userid);
			break;
		case "loading":
			momentsList = Event.loadingMoments(userid, eid);
			break;
		case "refresh":
			momentsList = Event.refreshMoments(userid, eid);
			break;
		}
		//获取评论列表
		List<Record> commentList = Comment.GetCommentsInEventList(momentsList);
		//List<Record> commentList =new ArrayList<Record>();
		// 动态数据封装
		//List<Record> list = dataProcess.eventDataEncapsulationShowMoments(momentsList, commentList, "小程序");
		List<Record> list = dataProcess.eventDataEncapsulation(momentsList, commentList, "小程序");

		return list;
	}
	/**
	 * 获取时刻 by lk 简化版
	 * 
	 * @param userid
	 * @param type
	 * @param eid
	 * @return
	 */
	public List<Record> GetMoments_sim(String userid, String type, String eid) {
		List<Record> momentsList = new ArrayList<Record>();
		switch (type) {
		case "initialize":
			//momentsList = Event.initializeMomentsBySim(userid);
			momentsList = Event.initializeMomentsBySim_new(userid);
			break;
		case "loading":
			//momentsList = Event.loadingMoments(userid, eid);
			momentsList = Event.loadingMoments_new(userid, eid);
			break;
		case "refresh":
			//momentsList = Event.refreshMoments(userid, eid);
			momentsList = Event.refreshMoments_new(userid, eid);
			break;
		}
		//获取评论列表
		//List<Record> commentList = Comment.GetCommentsInEventList(momentsList);
		List<Record> commentList =new ArrayList<Record>();
		// 动态数据封装
		List<Record> list = dataProcess.eventDataEncapsulationShowMoments(momentsList, commentList, "小程序");
		//List<Record> list = dataProcess.eventDataEncapsulation(momentsList, commentList, "小程序");

		return list;
	}
	/**
	 * 获取时刻 by lk 简化版-不显示活动相册内容
	 * 
	 * @param userid
	 * @param type
	 * @param eid
	 * @return
	 */
	public List<Record> GetMoments_sim_noGtype(String userid, String type, String eid) {
		List<Record> momentsList = new ArrayList<Record>();
		switch (type) {
		case "initialize":
			momentsList = Event.initializeMomentsBySim_noGtype(userid);
			break;
		case "loading":
			momentsList = Event.loadingMoments_noGtype(userid, eid);
			break;
		case "refresh":
			momentsList = Event.refreshMoments_noGtype(userid, eid);
			break;
		}
		//获取评论列表
		//List<Record> commentList = Comment.GetCommentsInEventList(momentsList);
		List<Record> commentList =new ArrayList<Record>();
		// 动态数据封装
		List<Record> list = dataProcess.eventDataEncapsulationShowMoments(momentsList, commentList, "小程序");
		//List<Record> list = dataProcess.eventDataEncapsulation(momentsList, commentList, "小程序");

		return list;
	}
	/**
	 * 获取时间轴内容 by lk
	 * 
	 * @param userid
	 * @param groupid
	 * @param type
	 * @param eid
	 * @return
	 */
	public List<Record> getSpaceTimeAxisContentByLk(String userid, String groupid, String type, int eid, String source) {

		
		// 获取动态内容
		List<Record> eventList = new ArrayList<Record>();
		// 置顶动态列表
		List<Record> topEventList = new ArrayList<Record>();
		// 根据请求类型获取相应数据
		Event event = new Event();
		switch (type) {
		case "initialize":
			// 获取置顶动态
			System.out.println("时间轴 获取置顶动态 开始："+System.currentTimeMillis());
			topEventList = event.GetAllTopEvent(groupid, source);
			System.out.println("时间轴 获取置顶动态 结束："+System.currentTimeMillis());
			// 初始化非置顶动态
			System.out.println("时间轴 获取非置顶动态 开始："+System.currentTimeMillis());
			eventList = event.InitializeEventContent(groupid, source);
			System.out.println("时间轴 获取非置顶动态 结束："+System.currentTimeMillis());
			System.out.println("时间轴 动态list合并 开始："+System.currentTimeMillis());
			eventList = dataProcess.combineTwoList(topEventList, eventList);
			System.out.println("时间轴 动态list合并 结束："+System.currentTimeMillis());
			break;
		case "refresh":
			eventList = event.RefreshEventContent(groupid, eid, source);
			break;
		case "loading":
			eventList = event.LoadingEventContent(groupid, eid, source);
			break;
		}
		// 获取空间所有评论
		System.out.println("时间轴 获取空间所有评论 开始："+System.currentTimeMillis());
		//by lk 修改评论获取条件
		//List<Record> commentList = new Comment().GetAllCommentsOfOneSpace(groupid);
		List<Record> commentList = new ArrayList<Record>();
		StringBuffer eids=new StringBuffer();
		for(Record r:eventList){
			eids.append(r.get("eid").toString()+",");
		}
		if(eids.length()>0){
			commentList = new Comment().GetAllCommentsOfOneSpaceByLk(groupid,eids.substring(0, eids.length()-1).toString());
			//eids=eids.substring(0, eids.length()-1);
		}
		// lk end 
		
		System.out.println("时间轴 获取空间所有评论 结束："+System.currentTimeMillis());
				
		System.out.println("时间轴 动态数据封装 开始："+System.currentTimeMillis());
		// 动态数据封装
		List<Record> list = dataProcess.eventDataEncapsulationByLk(eventList, commentList, source);
		System.out.println("时间轴 动态数据封装 end："+System.currentTimeMillis());

		// 判断是否为活动相册， 是则随机增加浏览量
		// Group group = new Group().findById(groupid);
		// if (group.get("gtype").toString().equals("12")) {
		// Event.AddRandomView(eventList);
		// }

		// 返回数据
		return list;
	}
//	public List<Record> getSpaceMemberEventsNew(int groupid, int userid,String ownUserid, int pagenum ,String source) {
//		String mode = "\"%Y-%m-%d\"";
//		int page = (pagenum-1)*10;
//		List<Record> eventList = Db.find("select eid,etext,eMain,elevel,DATE_FORMAT( euploadtime, " + mode+ " ) as euploadtime from `events` where euserid="+userid
//				+" and egroupid="+groupid +" and estatus=0"+ " order by euploadtime desc" +" limit "+page + ",10");
//		List<Record> list = dataProcess.eventDataEncapsulationNew(eventList,source,userid,ownUserid);
//
//		return list;
//	}
}
