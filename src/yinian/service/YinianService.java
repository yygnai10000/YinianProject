package yinian.service;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import yinian.app.CacheData;
import yinian.app.YinianDAO;
import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.model.BackupEvent;
import yinian.model.BackupPhoto;
import yinian.model.Comment;
import yinian.model.Event;
import yinian.model.Feedback;
import yinian.model.Group;
import yinian.model.GroupMember;
import yinian.model.GroupsIsTop;
import yinian.model.HistoryCover;
import yinian.model.HistoryTag;
import yinian.model.Inform;
import yinian.model.InviteGroup;
import yinian.model.Likes;
import yinian.model.MAPicture;
import yinian.model.Message;
import yinian.model.MusicAlbum;
import yinian.model.Notification;
import yinian.model.Picture;
import yinian.model.Tag;
import yinian.model.User;
import yinian.model.Wait;
import yinian.push.PushMessage;
import yinian.push.YinianGetuiPush;
import yinian.utils.DES;
import yinian.utils.JsonData;
import yinian.utils.QiniuOperate;
import yinian.utils.YinianUtils;

import com.jfinal.aop.Before;
import com.jfinal.plugin.activerecord.ActiveRecordException;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.IAtom;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.jfinal.plugin.ehcache.CacheKit;

import yinian.utils.SendMessage;

public class YinianService {

	private String jsonString;// 返回结果
	private JsonData jsonData = new JsonData(); // json操作类
	private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类
	private YinianGetuiPush push = new YinianGetuiPush(); // 个推推送类
	private YinianDAO dao = new YinianDAO();
	// private IMService im = new IMService();
	private QiniuOperate qiniu = new QiniuOperate();
	private PushMessage pushMessage = new PushMessage();
	private EventService eventService = new EventService();

	@Before(Tx.class)
	public void Test(String userid) {
		// 创建空间
		String inviteCode = Group.CreateSpaceInviteCode();
		String groupid = createAlbum("私人空间", userid, "http://7xlmtr.com1.z0.glb.clouddn.com/defaultCoverOfSpace.png",
				"4", inviteCode, "app");
		System.out.println("相册创建成功");
		// 获取所有动态
		List<Record> eventList = Db.find("select backupEventID,backupDate from backupevent where backupUserID=" + userid
				+ " and backupStatus=0 order by backupDate asc ");
		int size = eventList.size();
		System.out.println("开始备份" + userid + "动态，共有" + size + "条");
		for (int i = 0; i < size; i++) {
			System.out.println("正在备份第" + i + "条动态");
			String eventID = eventList.get(i).get("backupEventID").toString();
			String backupDate = eventList.get(i).get("backupDate").toString() + " 00:00:00";
			// 获取动态内的所有图片
			List<Record> list = Db.find(
					"select backupPhotoURL from backupphoto where backupPEid=" + eventID + " and backupPStatus=0    ");

			if (list.size() != 0) {
				Event event = new Event().set("egroupid", groupid).set("euserid", userid)
						.set("efirstpic", list.get(0).get("backupPhotoURL").toString()).set("eMain", 0).set("etype", 0)
						.set("euploadtime", backupDate).set("ememorytime", backupDate);
				if (event.save()) {
					String eid = event.get("eid").toString();

					for (Record record : list) {
						String url = record.get("backupPhotoURL").toString();
						Picture pic = new Picture().set("poriginal", url).set("peid", eid)
								.set("puploadtime", backupDate).set("pmemorytime", backupDate);
						pic.save();
					}
					System.out.println("第" + i + "条动态生成成功");
				}
			} else {
				System.out.println("第" + i + "条动态生成失败");
			}

			System.out.println("结束备份第" + i + "条动态");
		}

	}

	/**
	 * 登陆
	 * 
	 * @param username
	 * @param password
	 * @return String
	 */
	public String login(String username, String password, String source) {
		// 通过用户名查找密码和其他信息
		List<Record> list = User.QueryUserLoginBasicInfo(username, "uphone");
		if (list.size() == 0) {
			// 查到的数据为空
			jsonString = jsonData.getJson(1000, "登录信息错误");
		} else {
			if (YinianUtils.EncoderByMd5(password).equals((list.get(0)).get("upass"))) {
				List<Record> userInfo = new ArrayList<Record>();
				Record record = new Record();
				String userid = list.get(0).get("userid").toString();
				// 添加环信登录密码
				String hxPassword = userid + (CommonParam.APP_USER_PASSWORD_SUFFIX);
				try {
					// 密码进行DES加密
					hxPassword = DES.encryptDES(hxPassword, "YZadZjYx");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				record.set("userid", userid).set("password", hxPassword);
				// 判断用户是否绑定微信
				if (list.get(0).get("uwechatid") == null) {
					record.set("isBindWechat", 0);
				} else {
					record.set("isBindWechat", 1);
				}
				userInfo.add(record);
				if (updateLastLoginInfo(list.get(0).get("userid").toString(), null)) {
					// 修改用户最后登录时间
					jsonString = jsonData.getJson(0, "success", userInfo);
				} else {
					jsonString = jsonData.getJson(-51, "更新数据库数据失败");
				}
			} else {
				jsonString = jsonData.getJson(1001, "登录信息错误");
			}
		}
		return jsonString;
	}

	/**
	 * 验证手机号
	 * 
	 * @param phonenumber
	 * @return boolean
	 */
	public boolean verifyPhone(String phonenumber) {
		List<Record> list = Db.find("select * from users where uphone='" + phonenumber + "'");// 查找手机号码
		if (list.size() == 0) {
			// 不存在为false
			return false;
		} else {
			// 存在为true
			return true;
		}
	}

	/**
	 * 发送验证短信
	 * 
	 * @param phonenumber
	 * @return
	 */
	public String verifyMessage(String phonenumber) {
		String feedback = "";// 短信网关返回结果
		String content = "【忆年】注册验证码："; // 短信内容
		List<Record> list = new ArrayList<Record>();
		// 随机生成四位整数作为验证码
		int verifyCode = (int) (Math.random() * 9000 + 1000);
		SendMessage sm = new SendMessage();
		content += verifyCode + "，此验证码只用于登录“忆年”APP，请勿转发他人。";
		// 发送验证码并获取返回结果
		try {
			feedback = sm.send(phonenumber, content);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if ((feedback.substring(0, 5)).equals("error")) {
			jsonString = jsonData.getJson(1003, "短信验证码发送失败");
		} else {
			Record record = new Record();
			record.set("verifyCode", verifyCode);
			list.add(record);
			jsonString = jsonData.getJson(0, "success", list);
		}
		return jsonString;
	}

	/**
	 * 注册(跳过)
	 * 
	 * @param username
	 * @param password
	 * @return
	 */
	public String register(String username, String password) {
		String encoder = YinianUtils.EncoderByMd5(password);// 密码加密
		SimpleDateFormat birthDf = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
		String birth = birthDf.format(new Date()); // new
													// Date()为获取当前系统时间，设置当前日期为生日

		int sex = 0;// 默认性别为女
		// 生成8位随机码当做用户昵称
		String base = "abcdefghijklmnopqrstuvwxyz0123456789";
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < 8; i++) {
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		String nickname = sb.toString();// nickname为随机生成的昵称
		String defaultHead = CommonParam.qiniuOpenAddress + CommonParam.userDefaultHeadPic;// 默认头像的地址
		String defaultBackground = CommonParam.qiniuOpenAddress + CommonParam.userDefaultBackground;// 默认背景的地址

		User user = new User().set("uphone", username).set("upass", encoder).set("usex", sex).set("unickname", nickname)
				.set("ubirth", birth).set("upic", defaultHead).set("ubackground", defaultBackground);

		// 用户ID，注册成功则有相应的值，注册失败为空字符串
		String userid = "";
		if (user.save()) {
			userid = user.get("userid").toString();
		}
		return userid;
	}

	/**
	 * 注册（填写个人信息）
	 * 
	 * @param username
	 * @param password
	 * @param nickname
	 * @param sex
	 * @param birthday
	 * @return
	 */
	public String register(String username, String password, String nickname, String sex, String birthday) {
		// 加密后的密码
		String encoder = YinianUtils.EncoderByMd5(password);
		// 性别
		int newSex;
		if (sex.equals("女")) {
			newSex = 0;
		} else {
			newSex = 1;
		}
		String defaultHead = CommonParam.qiniuOpenAddress + CommonParam.userDefaultHeadPic;// 默认头像的地址
		String defaultBackground = CommonParam.qiniuOpenAddress + CommonParam.userDefaultBackground;// 默认背景的地址

		User user = new User().set("uphone", username).set("upass", encoder).set("unickname", nickname)
				.set("usex", newSex).set("ubirth", birthday).set("upic", defaultHead)
				.set("ubackground", defaultBackground);

		String userid = "";
		if (user.save()) {
			userid = user.get("userid").toString();
		}
		return userid;
	}

	/**
	 * 重置密码
	 * 
	 * @param phonenumber
	 * @param password
	 * @return
	 */
	public String resetPassword(String phonenumber, String password) {
		// 新密码加密
		String encoder = YinianUtils.EncoderByMd5(password);
		int result = Db.update("update users set upass=? where uphone=?", encoder, phonenumber);
		if (result == 1) {
			String Idsql = "select userid from users where uphone='" + phonenumber + "'";
			List<Record> list = Db.find(Idsql);
			jsonString = jsonData.getJson(0, "success", list);
		} else {
			jsonString = jsonData.getJson(-51, "更新数据库数据失败");
		}
		return jsonString;
	}

	/**
	 * 显示组列表
	 * 
	 * @param userid 
	 * @return
	 */
	public String showGroup(int userid) {
		// 判断相册列表是否排序过，未排序则按时间顺序排列
		// List<Record> judgeList = Db.find("select gmorder from groups,groupmembers
		// where groupid=gmgroupid and gmuserid="
		// + userid + " and gstatus=0 and gmstatus=0 and gtype not in(5,12)");//
		// 排除官方相册和小相册和活动相册
		// if (judgeList.size() != 0) {
		// Record judgeRecord = judgeList.get(0);
		// boolean judgeFlag = true;
		// if ((judgeRecord.get("gmorder").toString()).equals("0")) {
		// judgeFlag = false;
		// }
		// // 根据判断结果获取组信息
		// List<Record> list = new ArrayList<Record>();
		// if (judgeFlag) {
		//
		// list = Group.GetAlreadyOrderSpaceBasicInfo(userid);
		// } else {
		//
		// list = Group.GetUnOrderSpaceBasicInfo(userid);
		//
		// // 将最新加入的相册置顶
		// int size = list.size();
		// if (size != 0 && size != 1) {
		// // 找出最新加入时间
		// String gmtime = list.get(0).get("gmtime").toString();
		// int index = 0;
		// for (int i = 1; i < size; i++) {
		// String temp = list.get(i).get("gmtime").toString();
		// if (dataProcess.compareTwoTime(gmtime, temp)) {
		// gmtime = temp;
		// index = i;
		// }
		// }
		// Record tempRecord = list.get(index);
		// list.remove(index);
		// list.add(0, tempRecord);
		// }
		//
		// }
		//
		// // 封装空间数据
		// list = dataProcess.spaceDataEncapsulation(list, userid);
		//
		// jsonString = jsonData.getJson(0, "success", list);
		// } else {
		// jsonString = jsonData.getJson(0, "success", judgeList);
		// }

		List<Record> list = Group.GetUnOrderSpaceBasicInfo(userid);
		// 封装空间数据
		list = dataProcess.spaceDataEncapsulation(list, userid);
		jsonString = jsonData.getJson(0, "success", list);
		return jsonString;
	}

	/**
	 * 搜索空间
	 * 
	 * @param spaceName
	 * @return
	 */
	public List<Record> searchSpace(String spaceName) {
		List<Record> list = Group.GetSearchSpaceBasicInfo(spaceName);

		// 封装空间数据
		list = dataProcess.spaceDataEncapsulation(list, 0);
		return list;
	}

	/**
	 * 获取所有电话号码，加缓存!!!!!!!!!!!!!!!
	 * 
	 * @return
	 */
	public List<Record> getAllPhone() {
		List<Record> list = Db.find("select uphone from users where uphone is not null");
		return list;
	}

	/**
	 * 获取所有等待者的电话号码，加缓存!!!!!!!!!!!!!!!
	 * 
	 * @return
	 */
	public List<Record> getAllWaitPhone() {
		List<Record> list = Db.find("select wsender,wphone from waits");
		return list;
	}

	/**
	 * 获取短信发送内容的相关信息
	 * 
	 * @param groupid
	 * @param userid
	 * @return
	 */
	public Record getMessageInfo(String groupid, String userid) {
		List<Record> list = Db.find("select gname,gtype,ginvite from groups where groupid='" + groupid + "'");
		List<Record> list2 = Db.find("select unickname from users where userid='" + userid + "' ");
		String groupName = list.get(0).get("gname").toString();// 获取相册名，发送信息时使用
		String userName = list2.get(0).get("unickname").toString();// 获取邀请人昵称，发送信息时使用
		String groupType = list.get(0).get("gtype").toString(); // 获取相册性质
		String ginvite = list.get(0).get("ginvite").toString();
		String newType = "";
		switch (groupType) {
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
		}
		Record messageInfo = new Record();
		messageInfo.set("userName", userName).set("type", newType).set("groupName", groupName).set("ginvite", ginvite);
		// String content = "你的朋友" + userName + "邀请你加入" + newType + "“"
		// + groupName + "”，一起在【忆年】里面构建你们线上的家"; // 短信内容
		return messageInfo;
	}

	/**
	 * 发送邀请短信
	 * 
	 * @param content
	 * @param phonenumber
	 * @return
	 */
	public boolean sendInviteMessage(String content, String messagePhone) {
		String feedback = "";// 短信网关返回结果
		// 发送验证码并获取返回结果
		SendMessage sm = new SendMessage();
		boolean flag;
		try {
			feedback = sm.send(messagePhone, content);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// jsonString = jsonData.getJson(1007, "短信邀请码发送失败");
			flag = false;
		}

		flag = (feedback.substring(0, 5)).equals("error") ? false : true;

		return flag;
	}

	/**
	 * 创建相册
	 * 
	 * @param groupName
	 * @param userid
	 * @param address
	 * @param groupType
	 * @return
	 */
	@Before(Tx.class)
	public String createAlbum(String groupName, String userid, String address, String groupType, String inviteCode,
			String source) {
		
		Group group = new Group().set("gname", groupName).set("gcreator", userid).set("gpic", address)
				.set("gtype", groupType).set("gnum", 1).set("ginvite", inviteCode);
		if (source != null) {
			group.set("gsource", source);
		}

		if (group.save()) {
			String groupid = group.get("groupid").toString();			
			// 插入数据到groupmembers表中
			boolean insertFlag = insertIntoGroupMembers(userid, groupid);
			if (insertFlag) {
				if (!(groupType.equals("5"))) {
					// 创建环信聊天群组
					// String gimid = im
					// .CreatChatGroup(groupid, groupName, userid);
					// groupid加密，用于web端
					String encodeGroupid = "";
					try {
						encodeGroupid = DES.encryptDES("groupid=" + groupid, CommonParam.DESSecretKey);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// group.set("gimid", gimid);
					// boolean updateFlag = group.update();
					List<Record> list = getSingleAlbum(groupid);// 获取创建的组的信息
					list.get(0).set("encodeGroupid", encodeGroupid);
					list = dataProcess.changeGroupTypeIntoWord(list);// 转换组类型为文字

					jsonString = jsonData.getSuccessJson(list);

				} else {
					List<Record> list = getSingleAlbum(groupid);// 获取创建的组的信息
					list = dataProcess.changeGroupTypeIntoWord(list);// 转换组类型为文字
					jsonString = jsonData.getJson(0, "success", list);
				}

			} else {
				jsonString = jsonData.getJson(-50, "插入数据失败");
			}
		} else {
			jsonString = jsonData.getJson(-50, "插入数据失败");
		}
		return jsonString;
	}
	

	/**
	 * 创建默认相册
	 * 
	 * @param userid
	 * @param gtype
	 * @return
	 */
	@Before(Tx.class)
	public boolean creatDefaultAlbum(String userid, String gtype, String source) {
		String url = "";
		String gname = "";
		switch (gtype) {
		case "0":// 家人组
			url += CommonParam.qiniuOpenAddress + CommonParam.familyGroup;
			gname += "家人相册";
			break;
		case "1":// 闺蜜组
			url += CommonParam.qiniuOpenAddress + CommonParam.bestieGroup;
			gname += "闺蜜相册";
			break;
		case "2":// 死党组
			url += CommonParam.qiniuOpenAddress + CommonParam.friendGrop;
			gname += "死党相册";
			break;
		case "3":// 情侣组
			url += CommonParam.qiniuOpenAddress + CommonParam.coupleGroup;
			gname += "情侣相册";
			break;
		case "4":// 其他
			url += CommonParam.qiniuOpenAddress + CommonParam.otherGroup;
			gname += "其他相册";
			break;
		case "6":// 系统引导，家人
			url += CommonParam.qiniuOpenAddress + CommonParam.familySpaceCover;
			gname += "欢乐一家人";
			break;
		case "7":// 系统引导，情侣
			url += CommonParam.qiniuOpenAddress + CommonParam.loverSpaceCover;
			gname += "情侣空间";
			break;
		case "8":// 系统引导，好友
			url += CommonParam.qiniuOpenAddress + CommonParam.friendSpaceCover;
			gname += "朋友聚会";
			break;
		case "9":// 精简小程序系统引导，家人
			url += CommonParam.qiniuOpenAddress + CommonParam.simH5familySpaceCover;
			gname += "欢乐一家人";
			break;
		case "10":// 精简小程序系统引导，情侣
			url += CommonParam.qiniuOpenAddress + CommonParam.simH5loverSpaceCover;
			gname += "情侣空间";
			break;
		case "11":// 精简小程序系统引导，情侣
			url += CommonParam.qiniuOpenAddress + CommonParam.simH5friendSpaceCover;
			gname += "朋友聚会";
			break;
		}

		// 获取邀请码
		String inviteCode = Group.CreateSpaceInviteCode();
		Group group =new Group();
		if(gtype.equals("9")||gtype.equals("10")){
			if(gtype.equals("9")){
				group = new Group().set("gname", gname).set("gcreator", userid).set("simAppPic", url).set("gpic", CommonParam.qiniuOpenAddress + CommonParam.familySpaceCover).set("gtype", gtype)
					.set("gnum", 1).set("ginvite", inviteCode);
			}else{
				group = new Group().set("gname", gname).set("gcreator", userid).set("simAppPic", url).set("gpic", CommonParam.qiniuOpenAddress + CommonParam.loverSpaceCover).set("gtype", gtype).set("gtype", gtype)
						.set("gnum", 1).set("ginvite", inviteCode);
			}
		}else{
			group = new Group().set("gname", gname).set("gcreator", userid).set("gpic", url).set("gtype", gtype)
				.set("gnum", 1).set("ginvite", inviteCode);
		}
		if (source != null) {
			group.set("gsource", source);
		}
		if (group.save()) {
			String groupid = group.get("groupid").toString();						
			// // 创建环信组并将创建用户插入到环信组中
			// String gimid = im.CreatChatGroup(groupid, gname, userid);
			// group.set("gimid", gimid);
			// boolean updateFlag = group.update();

			// 插入数据到groupmembers表中
			boolean insertFlag = insertIntoGroupMembers(userid, groupid);
			return insertFlag;
		} else {
			return false;
		}
	}
	/**
	 * 创建默认相册并发布默认动态
	 * 
	 * @param userid
	 * @param gtype
	 * @return
	 */
	@Before(Tx.class)
	public boolean creatDefaultAlbumAndPulishMsg(String userid, String gtype, String source) {
		String url = "";
		String gname = "";
		switch (gtype) {
		case "0":// 家人组
			url += CommonParam.qiniuOpenAddress + CommonParam.familyGroup;
			gname += "家人相册";
			break;
		case "1":// 闺蜜组
			url += CommonParam.qiniuOpenAddress + CommonParam.bestieGroup;
			gname += "闺蜜相册";
			break;
		case "2":// 死党组
			url += CommonParam.qiniuOpenAddress + CommonParam.friendGrop;
			gname += "死党相册";
			break;
		case "3":// 情侣组
			url += CommonParam.qiniuOpenAddress + CommonParam.coupleGroup;
			gname += "情侣相册";
			break;
		case "4":// 其他
			url += CommonParam.qiniuOpenAddress + CommonParam.otherGroup;
			gname += "其他相册";
			break;
		case "6":// 系统引导，家人
			url += CommonParam.qiniuOpenAddress + CommonParam.familySpaceCover;
			gname += "欢乐一家人";
			break;
		case "7":// 系统引导，情侣
			url += CommonParam.qiniuOpenAddress + CommonParam.loverSpaceCover;
			gname += "情侣空间";
			break;
		case "8":// 系统引导，好友
			url += CommonParam.qiniuOpenAddress + CommonParam.friendSpaceCover;
			gname += "朋友聚会";
			break;
		case "9":// 精简小程序系统引导，家人
			url += CommonParam.qiniuOpenAddress + CommonParam.simH5familySpaceCover;
			gname += "欢乐一家人";
			break;
		case "10":// 精简小程序系统引导，情侣
			url += CommonParam.qiniuOpenAddress + CommonParam.simH5loverSpaceCover;
			gname += "情侣空间";
			break;
		}

		// 获取邀请码
		String inviteCode = Group.CreateSpaceInviteCode();
		Group group =new Group();
		if(gtype.equals("9")||gtype.equals("10")){
			group = new Group().set("gname", gname).set("gcreator", userid).set("simAppPic", url).set("gtype", gtype)
					.set("gnum", 1).set("ginvite", inviteCode);
		}else{
			group = new Group().set("gname", gname).set("gcreator", userid).set("gpic", url).set("gtype", gtype)
				.set("gnum", 1).set("ginvite", inviteCode);
		}
		if (source != null) {
			group.set("gsource", source);
		}
		if (group.save()) {
			String groupid = group.get("groupid").toString();
			
			//by lk 
			//发布默认动态
			//YinianService TxService = enhance(YinianService.class);
			Calendar c = Calendar.getInstance();//可以对每个时间域单独修改
			int year = c.get(Calendar.YEAR); 
			int month = c.get(Calendar.MONTH)+1; 
			int date = c.get(Calendar.DATE); 
			//by lk 先不发布动态 20171108
			String picAddress="http://7xpend.com1.z0.glb.clouddn.com/tmp_1840006904o6zAJs7TrsuV9RMorlB_3dksq1YE2e0ff03bac96b62f6df9266a5504314b.png,http://7xpend.com1.z0.glb.clouddn.com/tmp_1840006904o6zAJs7TrsuV9RMorlB_3dksq1YE0dc3d9fa7b52e5bf13c3d6a652f36070.png";
			String eventID = uploadDefault(userid, groupid, picAddress, "忆年共享相册，两步即可共享美好瞬间...", "600", year+"-"+month+"-"+date, "", "",
					"小程序");// 上传动态并获取动态的ID
			//lk end 
			// // 创建环信组并将创建用户插入到环信组中
			// String gimid = im.CreatChatGroup(groupid, gname, userid);
			// group.set("gimid", gimid);
			// boolean updateFlag = group.update();

			// 插入数据到groupmembers表中
			boolean insertFlag = insertIntoGroupMembers(userid, groupid);
			return insertFlag;
		} else {
			return false;
		}
	}
	/**
	 * 创建一些相册
	 * 
	 * @param userid
	 * @param gtype
	 * @return
	 */
	public boolean creatSomeAlbum(String userid, String gtype) {
		String url = "";
		String gname = "";
		switch (gtype) {
		case "0":// 家人组
			url += CommonParam.qiniuOpenAddress + CommonParam.familyGroup;
			gname += "家人相册";
			break;
		case "1":// 闺蜜组
			url += CommonParam.qiniuOpenAddress + CommonParam.bestieGroup;
			gname += "闺蜜相册";
			break;
		case "2":// 死党组
			url += CommonParam.qiniuOpenAddress + CommonParam.friendGrop;
			gname += "死党相册";
			break;
		case "3":// 情侣组
			url += CommonParam.qiniuOpenAddress + CommonParam.coupleGroup;
			gname += "情侣相册";
			break;
		case "4":// 其他
			url += CommonParam.qiniuOpenAddress + CommonParam.otherGroup;
			gname += "其他相册";
			break;
		}
		// 生成8位随机码当做相册邀请码
		String base = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < 6; i++) {
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		String inviteCode = sb.toString();// inviteCode为随机生成的邀请码
		Group group = new Group().set("gname", gname).set("gcreator", userid).set("gpic", url).set("gtype", gtype)
				.set("gnum", 1).set("ginvite", inviteCode).set("gOrigin", 1);
		if (group.save()) {
			String groupid = group.get("groupid").toString();
			// 插入数据到groupmembers表中
			boolean insertFlag = insertIntoGroupMembers(userid, groupid);
			return insertFlag;
		} else {
			return false;
		}
	}

	/**
	 * 获取单个相册信息
	 * 
	 * @param groupid
	 * @return
	 */
	public List<Record> getSingleAlbum(String groupid) {
		String sql = "select groupid,gimid,gname,gpic,gnum,gtime,gtype,gcreator,ginvite from groups where groupid ='"
				+ groupid + "' ";
		List<Record> list = Db.find(sql);
		// 刚创建相册时，直接把相册照片数设置为0
		list.get(0).set("gpicnum", 0);
		return list;
	}

	/**
	 * 获得组内信息
	 * 
	 * @param groupid
	 * @return
	 */
	public String getGroupContent(String userid, String groupid, String minID) {
		// 评论查询语句
		String sqlForComment = CommonParam.selectForComment
				+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and egroupid="
				+ groupid + " and cstatus=0 ORDER BY ceid,ctime asc";
		List<Record> event;
		if (minID.equals("0")) {
			// minID==0代表获取组内信息的初始化信息
			// 获取置顶的所有动态
			List<Record> topEvents = Db.find(CommonParam.selectForEvent
					+ " from users,events,pictures where userid=euserid and eid=peid and egroupid=" + groupid
					+ " and estatus=0 and pstatus=0 and elevel=1 and eMain!=5 group by peid DESC order by eTopTime asc ");
			int size = topEvents.size();

			event = Db.find(CommonParam.selectForEvent
					+ " from users,events,pictures where userid=euserid and eid=peid and egroupid=" + groupid
					+ " and estatus=0 and pstatus=0 and elevel=0 and eMain!=5 group by peid DESC  limit " + (10 - size)
					+ "");
			// 初始化的时候更新相册状态为无新动态
			Db.update(
					"update groupmembers set gmnotify=0 where gmgroupid=" + groupid + " and gmuserid=" + userid + " ");
			topEvents.addAll(event);
			event = topEvents;
		} else {
			// minID不等于0则查询小于该ID的前十条数据
			event = Db.find(CommonParam.selectForEvent
					+ " from users,events,pictures where userid=euserid and eid=peid and egroupid=" + groupid
					+ " and eid < " + minID
					+ " and estatus=0 and pstatus=0 and elevel=0 and eMain!=5 group by peid DESC limit 10");
		}
		event = dataProcess.encapsulationEventList(event);// 获取事件的相关信息并封装里面的用户对象
		List<Record> commentList = new ArrayList<Record>();
		if (!groupid.equals("1065266")) {
			commentList = Db.find(sqlForComment);
		}

		List<Record> comment = dataProcess.encapsulationCommentList(commentList);// 获取事件的评论信息并封装里面的用户对象
		List<Record> list = dataProcess.combieEventAndComment(event, comment);// 拼接事件与评论
		// 封装事件和点赞
		list = dataProcess.combineEventWithLike(list, "smallApp");
		// 封装事件和标签
		list = dataProcess.combineEventWithTags(list);
		// 封装事件和卡片样式
		list = dataProcess.combineEventWithCardStyle(list);
		// 修改upic字段格式
		list = dataProcess.ChangePicAsArrayDirectCutVersion(list);

		jsonString = jsonData.getSuccessJson(list);
		return jsonString;
	}

	/**
	 * 获取组员列表
	 * 
	 * @param groupid
	 * @return
	 */
	public String getMemberList(String userid, String groupid) {

		String sql = "select userid,unickname,upic,gmtime,gname from users,groups,groupmembers where groupid=gmgroupid and users.userid=groupmembers.gmuserid and gmgroupid='"
				+ groupid + "' limit 100 ";
		List<Record> list = Db.find(sql);

		// 区分新旧版本，增加备注名字段
		if (userid != null && !(userid.equals(""))) {
			String sqlForNote = "select noteName,noteTo from note where noteGroupID=" + groupid + " and noteFrom="
					+ userid + " ";
			List<Record> noteList = Db.find(sqlForNote);
			for (Record record : list) {
				record.set("noteName", null);
				boolean flag = true;
				for (Record noteRecord : noteList) {
					if (noteRecord.get("noteTo").equals(record.get("userid"))) {
						record.set("noteName", noteRecord.get("noteName"));
						flag = false;
						break;
					}
				}
				if (flag) {
					String tempUserid = record.get("userid").toString();
					List<Record> temp = Db.find("select noteName from note where noteGroupID=" + groupid
							+ " and noteFrom=" + tempUserid + " and noteTo=" + tempUserid + "  ");
					if (temp.size() != 0) {
						record.set("noteName", temp.get(0).get("noteName"));
					}
				}
			}
		}

		list = dataProcess.encapsulationUserInfo(list);
		jsonString = jsonData.getJson(0, "success", list);
		return jsonString;
	}

	/**
	 * 获取用户头信息
	 * 
	 * @param userid
	 * @return
	 */
	public String getUserHead(String userid) {
		String sql = "select unickname,upic,ubackground,uwechatid,uBalance from users where userid='" + userid + "' ";
		List<Record> list = Db.find(sql);
		jsonString = jsonData.getJson(0, "success", list);
		return jsonString;
	}

	/**
	 * 发表评论
	 * 
	 * @param commentUserId
	 * @param commentedUserId
	 * @param eventId
	 * @param content
	 * @return
	 */
	@Before(Tx.class)
	public boolean sendComment(String commentUserId, String commentedUserId, String eventId, String content) {

		// 推送list
		List<Record> pushList = new ArrayList<Record>();

		// 插入评论是否成功判断变量
		boolean commentFlag;
		// 发送消息是否成功判断变量
		boolean messageFlag = true;

		// 创建评论对象并存储评论信息
		Comment comment = new Comment().set("ceid", eventId).set("cuserid", commentUserId)
				.set("ceduserid", commentedUserId).set("ccontent", content);
		// 插入评论
		if (comment.save()) {
			commentFlag = true;
		} else {
			commentFlag = false;
		}
		// 获取动态发布者的ID
		Record PublisherID = Db.findFirst("select euserid from events where eid=" + eventId + " ");
		// 存储需要发送消息的用户ID的集合
		Set<Integer> useridSet = new HashSet<Integer>();
		// 填入发布者的信息
		useridSet.add(Integer.parseInt(PublisherID.get("euserid").toString()));
		// 判断被评论人的ID，如果为系统ID，则发送消息给所有相关人员
		if (commentedUserId.equals(CommonParam.systemUserID)) {
			// 获取所有用户ID
			List<Record> list = Db.find("select distinct cuserid from comments where ceid=" + eventId + " ");
			// 将数据填入Set中
			for (Record record : list) {
				useridSet.add(Integer.parseInt(record.get("cuserid").toString()));
			}
			// 去除评论人的ID
			useridSet.remove(Integer.parseInt(commentUserId));
			// 插入评论消息
			Iterator<Integer> it = useridSet.iterator();
			while (it.hasNext()) {
				int mreceiver = it.next();
				// 构造消息对象
				Message message = new Message().set("msender", Integer.parseInt(commentUserId))
						.set("mreceiver", mreceiver).set("meid", Integer.parseInt(eventId)).set("mcontent", content);
				// 插入数据
				if (message.save()) {
					messageFlag = true;
					// 评论消息存储成功的同时,构造透传内容并进行推送
					String mid = message.get("mid").toString();
					// 获取动态的第一张图
					Record eventRecord = Db.findFirst("select eid,efirstpic from events where eid=" + eventId + " ");
					Record pushRecord = pushMessage.getPushRecord(commentUserId, commentedUserId, "", content, mid,
							"comment", eventRecord);
					if (!((pushRecord.toJson()).equals("{}"))) {
						// Record不为空时，加入到list中
						pushList.add(pushRecord);
					}
				} else {
					messageFlag = false;
					break;
				}
			}
		} else {
			// 添加被评论人的ID
			useridSet.add(Integer.parseInt(commentedUserId));
			// 除去评论人的ID
			useridSet.remove(Integer.parseInt(commentUserId));
			// 插入评论消息
			Iterator<Integer> it = useridSet.iterator();
			while (it.hasNext()) {
				int mreceiver = it.next();
				// 构造消息对象
				Message message = new Message().set("msender", Integer.parseInt(commentUserId))
						.set("mreceiver", mreceiver).set("meid", Integer.parseInt(eventId)).set("mcontent", content);
				if (message.save()) {
					messageFlag = true;
					// 评论消息存储成功的同时,构造透传内容并进行推送
					String mid = message.get("mid").toString();
					// 获取动态的第一张图
					Record eventRecord = Db.findFirst("select eid,efirstpic from events where eid=" + eventId + " ");
					Record pushRecord = pushMessage.getPushRecord(commentUserId, commentedUserId, "", content, mid,
							"comment", eventRecord);
					if (!((pushRecord.toJson()).equals("{}"))) {
						// Record不为空时，加入到list中
						pushList.add(pushRecord);
					}
				} else {
					messageFlag = false;
					break;
				}
			}
		}

		if (commentFlag && messageFlag) {
			// 进行推送并返回true
			push.yinianPushToSingle(pushList);
			return true;
		} else {
			return false;
		}

	}

	/**
	 * 发表评论1 1.1版本 新增返回cid字段
	 * 
	 * @param commentUserId
	 * @param commentedUserId
	 * @param eventId
	 * @param content
	 * @return
	 */
	@Before(Tx.class)
	public String sendComment1(String commentUserId, String commentedUserId, String eventId, String content,
			String place) {

		// 推送list
		List<Record> pushList = new ArrayList<Record>();

		// 插入评论是否成功判断变量
		boolean commentFlag;
		// 发送消息是否成功判断变量
		boolean messageFlag = true;

		// 创建评论对象并存储评论信息
		Comment comment = new Comment().set("ceid", eventId).set("cuserid", commentUserId)
				.set("ceduserid", commentedUserId).set("ccontent", content).set("cplace", place);
		// 插入评论
		if (comment.save()) {
			commentFlag = true;
		} else {
			commentFlag = false;
		}
		// 获取插入的评论的ID
		String cid = comment.get("cid").toString();
		// 获取动态发布者的ID
		Record PublisherID = Db.findFirst("select euserid from events where eid=" + eventId + " ");
		// 存储需要发送消息的用户ID的集合
		Set<Integer> useridSet = new HashSet<Integer>();
		// 填入发布者的信息
		useridSet.add(Integer.parseInt(PublisherID.get("euserid").toString()));
		// 判断被评论人的ID，如果为系统ID，则发送消息给所有相关人员
		if (commentedUserId.equals(CommonParam.systemUserID)) {
			// 获取所有用户ID
			List<Record> list = Db.find("select distinct cuserid from comments where ceid=" + eventId + " ");
			// 将数据填入Set中
			for (Record record : list) {
				useridSet.add(Integer.parseInt(record.get("cuserid").toString()));
			}
			// 去除评论人的ID
			useridSet.remove(Integer.parseInt(commentUserId));
			// 插入评论消息
			Iterator<Integer> it = useridSet.iterator();
			while (it.hasNext()) {
				int mreceiver = it.next();
				// 构造消息对象
				Message message = new Message().set("msender", Integer.parseInt(commentUserId))
						.set("mreceiver", mreceiver).set("meid", Integer.parseInt(eventId)).set("mcontent", content);
				// 插入数据
				if (message.save()) {
					messageFlag = true;
					// 评论消息存储成功的同时,构造透传内容并进行推送
					String mid = message.get("mid").toString();
					// 获取动态的第一张图
					Record eventRecord = Db.findFirst("select eid,efirstpic from events where eid=" + eventId + " ");
					Record pushRecord = pushMessage.getPushRecord(commentUserId, String.valueOf(mreceiver), "", content,
							mid, "comment", eventRecord);
					if (!((pushRecord.toJson()).equals("{}"))) {
						// Record不为空时，加入到list中
						pushList.add(pushRecord);
					}
				} else {
					messageFlag = false;
					break;
				}
			}
		} else {
			// 添加被评论人的ID
			useridSet.add(Integer.parseInt(commentedUserId));
			// 除去评论人的ID
			useridSet.remove(Integer.parseInt(commentUserId));
			// 插入评论消息
			Iterator<Integer> it = useridSet.iterator();
			while (it.hasNext()) {
				int mreceiver = it.next();
				// 构造消息对象
				Message message = new Message().set("msender", Integer.parseInt(commentUserId))
						.set("mreceiver", mreceiver).set("meid", Integer.parseInt(eventId)).set("mcontent", content);
				if (message.save()) {
					messageFlag = true;
					// 评论消息存储成功的同时,构造透传内容并进行推送
					String mid = message.get("mid").toString();
					// 获取动态的第一张图
					Record eventRecord = Db.findFirst("select eid,efirstpic from events where eid=" + eventId + " ");
					Record pushRecord = pushMessage.getPushRecord(commentUserId, commentedUserId, "", content, mid,
							"comment", eventRecord);
					if (!((pushRecord.toJson()).equals("{}"))) {
						// Record不为空时，加入到list中
						pushList.add(pushRecord);
					}
				} else {
					messageFlag = false;
					break;
				}
			}
		}
		if (commentFlag && messageFlag) {
			// 进行推送并返回cid
			push.yinianPushToSingle(pushList);
			return cid;
		} else {
			return "";
		}
	}

	/**
	 * 在官方相册内发表评论 1.2版本 2016.1.30 1、管理员收到他人评论动态与对他的回复时进行推送 2、普通成员收到对他的回复时进行推送
	 * 
	 * @param commentUserId
	 * @param commentedUserId
	 * @param eventId
	 * @param content
	 * @return
	 */
	@Before(Tx.class)
	public String sendCommentInOfficialAlbum(String commentUserId, String commentedUserId, String eventId,
			String content) {
		// 推送list
		List<Record> pushList = new ArrayList<Record>();

		// 插入评论是否成功判断变量
		boolean commentFlag;
		// 发送消息是否成功判断变量
		boolean messageFlag = true;

		// 创建评论对象并存储评论信息
		Comment comment = new Comment().set("ceid", eventId).set("cuserid", commentUserId)
				.set("ceduserid", commentedUserId).set("ccontent", content);
		// 插入评论
		if (comment.save()) {
			commentFlag = true;
		} else {
			commentFlag = false;
		}
		// 获取插入的评论的ID
		String cid = comment.get("cid").toString();
		if (commentedUserId.equals(CommonParam.systemUserID)) {
			// 代表评论，只有其他人评论时，管理员收到消息和推送
			if (commentUserId.equals(CommonParam.superUserID)) {
				// 管理员自己评论，无情况

			} else {
				// 普通成员评论，管理员收到消息和推送
				// 构造消息对象
				Message message = new Message().set("msender", Integer.parseInt(commentUserId))
						.set("mreceiver", CommonParam.systemUserID).set("meid", Integer.parseInt(eventId))
						.set("mcontent", content);
				// 插入数据
				if (message.save()) {
					messageFlag = true;
					// 评论消息存储成功的同时,构造透传内容并进行推送
					String mid = message.get("mid").toString();
					// 获取动态的第一张图
					Record eventRecord = Db.findFirst("select eid,efirstpic from events where eid=" + eventId + " ");
					Record pushRecord = pushMessage.getPushRecord(commentUserId,
							String.valueOf(CommonParam.superUserID), "", content, mid, "comment", eventRecord);
					if (!((pushRecord.toJson()).equals("{}"))) {
						// Record不为空时，加入到list中
						pushList.add(pushRecord);
					}
				} else {
					messageFlag = false;
				}
			}
		} else {
			// 代表回复，只有被回复人收到消息和推送
			// 构造消息对象
			Message message = new Message().set("msender", Integer.parseInt(commentUserId))
					.set("mreceiver", Integer.parseInt(commentedUserId)).set("meid", Integer.parseInt(eventId))
					.set("mcontent", content);
			if (message.save()) {
				messageFlag = true;
				// 评论消息存储成功的同时,构造透传内容并进行推送
				String mid = message.get("mid").toString();
				// 获取动态的第一张图
				Record eventRecord = Db.findFirst("select eid,efirstpic from events where eid=" + eventId + " ");
				Record pushRecord = pushMessage.getPushRecord(commentUserId, commentedUserId, "", content, mid,
						"comment", eventRecord);
				if (!((pushRecord.toJson()).equals("{}"))) {
					// Record不为空时，加入到list中
					pushList.add(pushRecord);
				}
			} else {
				messageFlag = false;
			}
		}

		if (commentFlag && messageFlag) {
			// 进行推送并返回cid
			push.yinianPushToSingle(pushList);
			return cid;
		} else {
			return "";
		}
	}

	/**
	 * 删除动态
	 * 
	 * @param eventId
	 * @return
	 */
	@Before(Tx.class)
	public boolean deleteEvent(String eventId) {

		// 将事件、照片、评论的状态值改为1
		Db.update("update pictures set pstatus=1 where peid=" + eventId + " ");
		Db.update("update comments set cstatus=1 where ceid=" + eventId + " ");
		// 将与该动态相关的评论消息删除
		Db.update("update messages set mstatus=2 where meid=" + eventId + " ");
		int eventCount = Db.update("update events set estatus=1 where eid=" + eventId + " ");

		// 扣除用户存储空间
		Event event = new Event().findById(eventId);
		double storage = Double.parseDouble(event.get("eStoragePlace").toString());
		String userid = event.get("euserid").toString();
		if (storage != 0.0)
			updateUserStoragePlace(userid, storage, "reduce");

		// 如果是拍立得，则退还红包
		String redEnvelopID = eventService.JudgeEventIsContainRedEnvelop(eventId);
		if (!(redEnvelopID.equals("0")))
			eventService.returnRedEnvelop(redEnvelopID);

		return (eventCount == 1);
	}

	/**
	 * 获取组成员所在组发布的动态消息
	 * 
	 * @param groupid
	 * @param userid
	 * @return
	 */
	public String getMemberInfo(String groupid, String userid, String minID) {
		String SqlForEvent = "";
		String commentSql = CommonParam.selectForComment
				+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and egroupid= '"
				+ groupid + "' and euserid = '" + userid + "' and cstatus=0 ORDER BY ceid,ctime asc ";
		if (minID.equals("0") || minID.equals("") || minID == null) {
			SqlForEvent = "SELECT eid,egroupid,etext,eaudio,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\") AS url FROM events,pictures WHERE eid = peid AND egroupid = '"
					+ groupid + "' AND euserid = '" + userid
					+ "' and estatus=0 and pstatus=0 GROUP BY peid DESC limit 10";
		} else {
			SqlForEvent = "SELECT eid,egroupid,etext,eaudio,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\") AS url FROM events,pictures WHERE eid = peid AND egroupid = '"
					+ groupid + "' AND euserid = '" + userid + "' and eid < " + minID
					+ " and estatus=0 and pstatus=0 GROUP BY peid DESC limit 10";
		}
		// 获取事件的相关信息
		List<Record> event = Db.find(SqlForEvent);
		// 获取事件的评论信息
		List<Record> comment = Db.find(commentSql);
		// 封装评论内的用户类
		comment = dataProcess.encapsulationCommentList(comment);
		List<Record> list = dataProcess.combieEventAndComment(event, comment);
		// 封装事件和标签
		list = dataProcess.combineEventWithTags(list);
		// 封装事件和卡片样式
		list = dataProcess.combineEventWithCardStyle(list);
		// 修改upic字段格式
		list = dataProcess.ChangePicAsArray(list);

		jsonString = jsonData.getJson(0, "success", list);
		return jsonString;
	}

	/**
	 * 刷新成员个人动态
	 * 
	 * @param userid
	 * @param groupid
	 * @return
	 */
	public String refreshMemberEvents(String userid, String groupid, String maxID) {
		String SqlForEvent = "SELECT eid,egroupid,etext,eaudio,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\") AS url FROM events,pictures WHERE eid = peid AND egroupid = '"
				+ groupid + "' AND euserid = '" + userid + "' and eid > " + maxID
				+ " and estatus=0 and pstatus=0 GROUP BY peid DESC";
		String commentSql = CommonParam.selectForComment
				+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and egroupid= '"
				+ groupid + "' and euserid = '" + userid + "' and cstatus=0 and eid> " + maxID
				+ " ORDER BY ceid,ctime asc ";
		List<Record> event = Db.find(SqlForEvent);// 获取事件的相关信息
		List<Record> comment = Db.find(commentSql);// 获取事件的评论信息
		comment = dataProcess.encapsulationCommentList(comment);// 封装评论内的用户类
		List<Record> list = dataProcess.combieEventAndComment(event, comment);
		// 封装事件和标签
		list = dataProcess.combineEventWithTags(list);
		// 封装事件和卡片样式
		list = dataProcess.combineEventWithCardStyle(list);
		list = dataProcess.ChangePicAsArray(list);// 修改upic字段格式
		jsonString = jsonData.getJson(0, "success", list);
		return jsonString;
	}

	/**
	 * 删除空间
	 * 
	 * @param groupid
	 * @return
	 */
	@Before(Tx.class)
	public String deleteGroup(String groupid, String source) {

		// 获取需要发送通知的所有用户的ID
		List<Record> UserID = getGroupMemberID(groupid);
		// 修改分组表、组员表、动态表相应数据的状态值为1
		Db.update("update groups set gstatus=1 where groupid=" + groupid + " ");
		Db.update("update groupmembers set gmstatus=1 where gmgroupid=" + groupid + " ");
		Db.update("update events set estatus=1 where egroupid=" + groupid + " ");

		// 获取改组内所有动态的ID
		List<Record> list = Db.find("select eid from events where egroupid=" + groupid + " ");

		// 将每条动态对应的状态、图片状态、评论状态改为1
		for (Record record : list) {
			deleteEvent(record.get("eid").toString());
		}

		// 将改组对应的所有进组邀请信息和进组通知的状态改为2表示已删除
		Db.update(" update invitegroup set igstatus=2 where iggroupid=" + groupid + " ");
		Db.update(" update notifications set nstatus=2 where ntype=1 and ngroupid=" + groupid + " ");

		// 推送list
		List<Record> pushList = new ArrayList<Record>();
		// 发送通知成功判断标志
		boolean flag = true;
		if (source == null || !source.equals("smallApp")) {

			// 获取通知的相关内容
			Record contentRecord = Db
					.findFirst("select gcreator,gtype,gname from groups where groupid=" + groupid + " ");
			// 获取创建者的ID
			int creator = Integer.parseInt(contentRecord.get("gcreator").toString());
			// 获取通知内容
			String content = dataProcess.getNotificationContent(contentRecord, "delete");

			// 逐条通知插入
			for (Record record : UserID) {
				int userid = Integer.parseInt(record.get("gmuserid").toString());
				if (creator != userid) {
					// 发送通知
					Notification notification = new Notification()
							.set("nsender", Integer.parseInt(contentRecord.get("gcreator").toString()))
							.set("nreceiver", Integer.parseInt(record.get("gmuserid").toString()))
							.set("ncontent", content).set("ntype", 2).set("ngroupid", groupid);
					if (notification.save()) {
						flag = true;
						// 通知存储成功的同时,构造透传内容并进行推送
						String nid = notification.get("nid").toString();
						Record pushRecord = pushMessage.getPushRecord(contentRecord.get("gcreator").toString(),
								record.get("gmuserid").toString(), groupid, content, nid, "notification", null);
						if (!((pushRecord.toJson()).equals("{}"))) {
							// Record不为空时，加入到list中
							pushList.add(pushRecord);
						}
					} else {
						flag = false;
						break;
					}
				}
			}
		}
		// 删除情侣时光机中的数据
		Db.update("update lovertimemachine set ltmStatus=1 where ltmGroupID=" + groupid + " ");
		// 删除环信中的聊天组
		// im.DismissChatGroup(groupid);

		if (flag) {
			push.yinianPushToSingle(pushList);
			jsonString = jsonData.getJson(0, "success");
		} else {
			jsonString = jsonData.getJson(-52, "删除数据库数据失败");
		}
		return jsonString;
	}

	/**
	 * 新成员进入组
	 * 
	 * @param groupid
	 * @param userid
	 * @return
	 */
	@Before(Tx.class)
	public List<Record> enterGroup(String groupid, String userid) {
		String updateSql = "update groups set gnum = gnum+1 where groupid='" + groupid + "' "; // 更新分组表中组成员数量字段
		// 插入数据到groupmembers表中
		boolean insertFlag = insertIntoGroupMembers(userid, groupid);
		int count = Db.update(updateSql);
		// 获取组信息
		List<Record> list = Db.find(
				"select groupid,gname,gpic,gnum,gtime,gmnotify,gtype,gcreator,ginvite from groups,groupmembers where groups.groupid=groupmembers.gmgroupid and gstatus=0 and groupid="
						+ groupid + " and gmuserid=" + userid + " ");
		// 获取相册照片数
		String sqlForPhotos = "select groupid,count(*) as gpicnum from groups,events,pictures where peid=eid and groupid=egroupid and gstatus=0 and estatus in(0,3) and pstatus=0 and groupid="
				+ groupid + " group by groupid";
		List<Record> photoList = Db.find(sqlForPhotos);
		// 将照片数字段放入相册信息中
		if (photoList.size() == 0) {
			list.get(0).set("gpicnum", "0");
		} else {
			list.get(0).set("gpicnum", photoList.get(0).get("gpicnum").toString());
		}
		// 用户加入IM群组中
		// im.AddChatGroupMember(userid, groupid);
		// 将相册性质换成对应中文
		list = dataProcess.changeGroupTypeIntoWord(list);
		if (insertFlag && count == 1) {
			return list;
		} else {
			return null;
		}
	}

	/**
	 * 新成员进入官方相册
	 * 
	 * @param groupid
	 * @param userid
	 * @return
	 */
	@Before(Tx.class)
	public List<Record> enterOfficialAlbum(String groupid, String userid) {
		String updateSql = "update groups set gnum = gnum+1 where groupid='" + groupid + "' "; // 更新分组表中组成员数量字段
		// 插入数据到groupmembers表中
		boolean insertFlag = insertIntoGroupMembers(userid, groupid);
		int count = Db.update(updateSql);
		List<Record> list = Db.find(
				"select groupid,gname,gpic,gnum,gtime,gmnotify,gtype,gcreator,ginvite from groups,groupmembers where groups.groupid=groupmembers.gmgroupid and gstatus=0 and groupid="
						+ groupid + " and gmuserid=" + userid + " ");
		list.get(0).remove("gtype");
		list.get(0).set("gtype", "5");
		// 获取相册照片数
		String sqlForPhotos = "select groupid,count(*) as gpicnum from groups,events,pictures where peid=eid and groupid=egroupid and gstatus=0 and gtype=5 and estatus in(0,3) and pstatus=0 and groupid="
				+ groupid + " group by groupid";
		List<Record> photoList = Db.find(sqlForPhotos);
		// 将照片数字段放入相册信息中
		if (photoList.size() == 0) {
			list.get(0).set("gpicnum", "0");
		} else {
			list.get(0).set("gpicnum", photoList.get(0).get("gpicnum").toString());
		}
		if (insertFlag && count == 1) {
			return list;
		} else {
			return null;
		}

	}

	/**
	 * 插入groupmembers表，需判断用户是否在相册里面
	 * 
	 * @param userid
	 * @param groupid
	 * @return
	 */
	public boolean insertIntoGroupMembers(String userid, String groupid) {
		boolean flag;
		int count = 0; // 排序值
		// // 获取相册排序的最大值
		// List<Record> judgeList = Db
		// .find("select max(gmorder) as gmorder from groupmembers where gmuserid="
		// + userid + " and gmstatus=0");
		// // 判断相册列表是否排序过，未排序则按时间顺序排列
		// if ((judgeList.get(0).get("gmorder")) != null) {
		// int nowCount = Integer.parseInt(judgeList.get(0).get("gmorder")
		// .toString());
		// if (nowCount != 0) {
		// count = nowCount + 1;
		// }
		// }
		GroupMember groupMember = new GroupMember().set("gmuserid", userid).set("gmgroupid", groupid).set("gmorder",
				count).set("isAdmin", 1);
		flag = groupMember.save();
		return flag;
	}

	/**
	 * 获取“我”发的所有动态
	 * 
	 * @param userid
	 * @return
	 */
	public String getMyEvents(String userid, String minID) {
		String SqlForEvent = "";
		String commentSql = CommonParam.selectForComment
				+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and euserid ='"
				+ userid + "' and cstatus=0 ORDER BY ceid,ctime asc ";
		if (minID.equals("0") || minID == null || minID.equals("")) {
			SqlForEvent = "SELECT eid,egroupid,gname,userid,upic,unickname,etext,eMain,eaudio,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\") AS url FROM users,groups,events,pictures WHERE userid=euserid and groupid = egroupid AND eid = peid AND euserid = '"
					+ userid + "' and egroupid!=104851 and estatus in(0,3) and pstatus=0 GROUP BY peid DESC limit 10";
		} else {
			SqlForEvent = "SELECT eid,egroupid,gname,userid,upic,unickname,etext,eMain,eaudio,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\") AS url FROM users,groups,events,pictures WHERE userid=euserid and groupid = egroupid AND eid = peid AND euserid = '"
					+ userid + "' and eid<" + minID
					+ " and egroupid!=104851 and estatus in(0,3) and pstatus=0 GROUP BY peid DESC limit 10";
		}
		List<Record> event = Db.find(SqlForEvent);// 获取事件的相关信息
		List<Record> comment = Db.find(commentSql);// 获取事件的评论信息
		event = dataProcess.encapsulationEventList(event);// 获取事件的相关信息并封装里面的用户对象
		comment = dataProcess.encapsulationCommentList(comment);// 封装评论内的用户类
		List<Record> list = dataProcess.combieEventAndComment(event, comment);
		// 封装事件和标签
		list = dataProcess.combineEventWithTags(list);
		// 封装事件和卡片样式
		list = dataProcess.combineEventWithCardStyle(list);
		list = dataProcess.ChangePicAsArray(list);// 修改upic字段格式
		// 封装事件和点赞
		list = dataProcess.combineEventWithLike(list, "smallApp");
		jsonString = jsonData.getJson(0, "success", list);
		return jsonString;
	}

	/**
	 * 修改个人信息
	 * 
	 * @param userid
	 * @param pic
	 * @param sex
	 * @param nickname
	 * @param birthday
	 * @return
	 */
	public boolean modifyPersonalInfo(String userid, String pic, String sex, String nickname, String birthday) {
		int newSex;
		if (sex.equals("女")) {
			newSex = 0;
		} else {
			newSex = 1;
		}
		User user = new User().set("userid", userid).set("upic", pic).set("usex", newSex).set("unickname", nickname)
				.set("ubirth", birthday);
		return user.update();
		// if (user.update()) {
		// // 修改IM中用户的昵称
		// im.ModifyIMUserNickname(userid, nickname);
		// return true;
		// } else {
		// return false;
		// }
	}

	/**
	 * 修改个人单项资料
	 * 
	 * @param userid
	 * @param data
	 * @param type
	 * @return
	 */
	public boolean modifyPersonalSingleInfo(String userid, String data, String type) {
		User user = new User().set("userid", userid);
		switch (type) {
		case "pic":
			if (!data.substring(0, 4).equals("http")) {
				data = CommonParam.qiniuOpenAddress + data;
			}
			user.set("upic", data);
			break;
		case "sex":
			int newSex;
			if (data.equals("女")) {
				newSex = 0;
			} else {
				newSex = 1;
			}
			user.set("usex", newSex);
			break;
		case "birthday":
			user.set("ubirth", data);
			break;
		case "nickname":
			user.set("unickname", data);
			// 修改用户IM中的昵称
			// im.ModifyIMUserNickname(userid, data);
			break;
		}
		if (user.update()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 获得个人信息
	 * 
	 * @param userid
	 * @return
	 */
	public String getPersonalInfo(String userid) {
		List<Record> list = Db.find(
				"select unickname,ubirth,usex,upic,ubackground,uBalance from users where userid='" + userid + "'");
		jsonString = jsonData.getJson(0, "success", list);
		return jsonString;
	}

	/**
	 * 修改组名
	 * 
	 * @param groupid
	 * @param groupName
	 * @return
	 */
	public String modifyGroupName(String groupid, String groupName) {
		if (Group.dao.findById(groupid).set("gname", groupName).update()) {
			jsonString = jsonData.getJson(0, "success");
		} else {
			jsonString = jsonData.getJson(-51, "更新数据库数据失败");
		}
		return jsonString;
	}

	/**
	 * 添加举报
	 * 
	 * @param userid
	 * @param eduserid
	 * @param eventId
	 * @return
	 */
	public String addReport(String userid, String eduserid, String eventId) {
		Inform inform = new Inform().set("iuserid", userid).set("ieduserid", eduserid).set("iedeid", eventId);
		if (inform.save()) {
			jsonString = jsonData.getJson(0, "success");
		} else {
			jsonString = jsonData.getJson(-50, "插入数据失败");
		}
		return jsonString;
	}

	/**
	 * 获取单条动态信息
	 * 
	 * @param eventId
	 * @return
	 */
	public String getSingleEvent(String eventId) {
		String eventSql = "SELECT eid,egroupid,estatus,gimid,efirstpic,userid,unickname,upic,eaudio,eaudiotime,ememorytime,ecover,ecardstyle,eplace,etype,eStoragePlace,etext,gname,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\") AS url,GROUP_CONCAT(pid SEPARATOR \",\") AS pid from users,`events`,pictures,groups where groupid=egroupid and eid=peid and euserid=userid and pstatus=0 and eid ="
				+ eventId + "  ";
		String commentSql = CommonParam.selectForComment
				+ " from users A,users B,comments where ceduserid=B.userid and A.userid=cuserid and ceid='" + eventId
				+ "' and cstatus=0 ORDER BY ctime asc";
		List<Record> event = Db.find(eventSql);// 获取事件的相关信息
		String status = event.get(0).get("estatus").toString();
		if (status.equals("1")) {
			jsonString = jsonData.getJson(1027, "动态已被删除");
		} else {
			// 第一张图片图片授权
			if (event.get(0).get("efirstpic") != null) {
				String auth = qiniu.getDownloadToken(event.get(0).get("efirstpic").toString());
				event.get(0).set("efirstpic", auth);
			}

			List<Record> comment = Db.find(commentSql);// 获取事件的评论信息
			// 封装事件和评论内的用户类
			event = dataProcess.encapsulationEventList(event);
			comment = dataProcess.encapsulationCommentList(comment);
			List<Record> list = dataProcess.combieEventAndComment(event, comment);
			// 封装事件和标签
			list = dataProcess.combineEventWithTags(list);
			// 资源授权
			list = dataProcess.ChangePicAsArray(list);
			jsonString = jsonData.getJson(0, "success", list);
		}
		return jsonString;
	}

	/**
	 * 获取单条动态信息
	 * 
	 * @param eventId
	 * @return
	 */
	public List<Record> getSingleEventWithList(String eventId) {
		String eventSql = "SELECT eid,egroupid,estatus,gimid,efirstpic,userid,unickname,upic,eaudio,eaudiotime,ememorytime,ecover,ecardstyle,eplace,etype,eStoragePlace,etext,gname,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\") AS url from users,events,pictures,groups where groupid=egroupid and eid=peid and euserid=userid and pstatus=0 and eid ="
				+ eventId + "  ";
		String commentSql = CommonParam.selectForComment
				+ " from users A,users B,comments where ceduserid=B.userid and A.userid=cuserid and ceid='" + eventId
				+ "' and cstatus=0 ORDER BY ctime asc";
		List<Record> event = Db.find(eventSql);// 获取事件的相关信息
		List<Record> list = new ArrayList<Record>();
		String status = event.get(0).get("estatus").toString();
		if (status.equals("1")) {
			jsonString = jsonData.getJson(1027, "动态已被删除");
		} else {
			// 第一张图片图片授权
			String auth = qiniu.getDownloadToken(event.get(0).get("efirstpic").toString());
			event.get(0).set("efirstpic", auth);
			List<Record> comment = Db.find(commentSql);// 获取事件的评论信息
			// 封装事件和评论内的用户类
			event = dataProcess.encapsulationEventList(event);
			comment = dataProcess.encapsulationCommentList(comment);
			list = dataProcess.combieEventAndComment(event, comment);
			// 封装事件和标签
			list = dataProcess.combineEventWithTags(list);
			// 封装事件和卡片样式
			list = dataProcess.combineEventWithCardStyle(list);
			list = dataProcess.ChangePicAsArray(list);// 修改upic字段格式
		}
		return list;
	}

	/**
	 * 获取官方相册内的单条动态信息，根据用户需要返回包含点赞的动态
	 * 
	 * @param eventId
	 * @return
	 */
	public String getSingleEventInOfficialAlbum(String userid, String eventId) {
		String eventSql = "SELECT eid,egroupid,userid,unickname,upic,etext,elike,eaudio,eaudiotime,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,lislike,gname,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\") AS url from users,events,pictures,groups,likes where eid=leid and groupid=egroupid and eid=peid and euserid=userid and pstatus=0 and eid ="
				+ eventId + " and luserid = " + userid + " ";
		String commentSql = CommonParam.selectForComment
				+ " from users A,users B,comments where ceduserid=B.userid and A.userid=cuserid and ceid='" + eventId
				+ "' and cstatus=0 ORDER BY ctime asc";
		List<Record> event = Db.find(eventSql);// 获取事件的相关信息
		List<Record> comment = Db.find(commentSql);// 获取事件的评论信息
		// 封装事件和评论内的用户类
		event = dataProcess.encapsulationEventList(event);
		comment = dataProcess.encapsulationCommentList(comment);
		List<Record> list = dataProcess.combieEventAndComment(event, comment);
		// 封装事件和标签
		list = dataProcess.combineEventWithTags(list);
		// 封装事件和卡片样式
		list = dataProcess.combineEventWithCardStyle(list);
		list = dataProcess.ChangePicAsArray(list);// 修改upic字段格式
		jsonString = jsonData.getJson(0, "success", list);
		return jsonString;
	}

	/**
	 * 获取组内所有成员的ID
	 * 
	 * @param groupid
	 * @return
	 */
	public List<Record> getGroupMemberID(String groupid) {
		List<Record> list = Db
				.find("select gmuserid,gmstatus from groupmembers where gmstatus=0 and gmgroupid = '" + groupid + "'");
		return list;
	}

	/**
	 * 修改邀请进组信息状态为已删除
	 * 
	 * @param messageID
	 * @return
	 */
	public boolean deleteMessage(String messageID) {
		boolean flag = InviteGroup.dao.findById(messageID).set("igstatus", 2).update();
		return flag;
	}

	/**
	 * 修改评论信息状态为已删除
	 * 
	 * @param messageID
	 * @return
	 */
	public boolean deleteCommentMessage(String messageID) {
		boolean flag = Message.dao.findById(messageID).set("mstatus", 2).update();
		return flag;
	}

	/**
	 * 修改通知状态为已删除
	 * 
	 * @param notificationID
	 * @return
	 */
	public boolean deleteNotification(String notificationID) {
		boolean flag = Notification.dao.findById(notificationID).set("nstatus", 2).update();
		return flag;
	}

	/**
	 * 获取消息列表
	 * 
	 * @param userid
	 * @param page
	 * @param numRecord
	 * @return
	 */
	@Before(Tx.class)
	public String getMessageList(String userid, int minID, String sign) {
		String sqlForInvite = "select unickname,upic,igcontent,igid,iggroupid,type,igtime,igstatus from invitegroup,users where userid=igsender and igstatus in(0,1) and igreceiver = '"
				+ userid + "' group by igid asc";
		String SqlForMessage = "";
		if (minID == 0) {
			// minID==0代表初始化的数据
			SqlForMessage = "select mid,unickname,upic,efirstpic,meid,mcontent,type,mtime,mstatus from users,messages,events where msender = userid and mstatus in(0,1) and meid = eid and mreceiver='"
					+ userid + "' group by mid desc limit 10";
		} else {
			SqlForMessage = "select mid,unickname,upic,efirstpic,meid,mcontent,type,mtime,mstatus from users,messages,events where msender = userid and mstatus in(0,1) and meid = eid and mreceiver='"
					+ userid + "' and mid < " + minID + " group by mid desc limit 10";
		}
		// 获取应该显示的评论消息列表
		List<Record> messageList = Db.find(SqlForMessage);
		// 获取该用户所有的消息的ID
		List<Record> messageID = Db
				.find("select mid from messages where mreceiver=" + userid + " and mstatus in(0,1) ");
		// 将评论消息列表中的每个记录状态标记为已读
		for (Record record : messageID) {
			Db.update(" update messages set mstatus=1 where mid=" + record.get("mid").toString() + " ");
		}
		if (sign.equals("initialization")) {
			// 标志为“初始化”，获取邀请进组消息列表
			List<Record> inviteList = Db.find(sqlForInvite);
			List<Record> list = dataProcess.combineTwoList(inviteList, messageList);
			jsonString = jsonData.getJson(0, "success", list);
		} else {
			if (sign.equals("loading")) {
				// 标志位“加载”，只获取上面获取的评论消息列表
				jsonString = jsonData.getJson(0, "success", messageList);
			} else {
				jsonString = jsonData.getJson(2, "参数错误");
			}
		}
		return jsonString;
	}

	/**
	 * 获取评论消息的信息
	 * 
	 * @param userid
	 * @param id
	 * @param type
	 * @return
	 */
	@Before(Tx.class)
	public String getCommentMessageInformation(String userid, String id, String type) {
		String sqlForCommentMessage = "";
		// 根据type构造相应的数据
		switch (type) {
		case "initialize":
			sqlForCommentMessage = "select mid,unickname,upic,efirstpic,eMain,meid,mcontent,type,mtime,mstatus from users,messages,events where msender = userid and mstatus in(0,1) and meid = eid and mreceiver='"
					+ userid + "' group by mid desc limit 10";
			break;
		case "loading":
			sqlForCommentMessage = "select mid,unickname,upic,efirstpic,eMain,meid,mcontent,type,mtime,mstatus from users,messages,events where msender = userid and mstatus in(0,1) and meid = eid and mreceiver='"
					+ userid + "' and mid < " + id + " group by mid desc limit 10";
			break;
		case "refresh":
			sqlForCommentMessage = "select mid,unickname,upic,efirstpic,eMain,meid,mcontent,type,mtime,mstatus from users,messages,events where msender = userid and mstatus in(0,1) and meid = eid and mreceiver='"
					+ userid + "' and mid > " + id + " group by mid desc ";
			break;
		default:
			jsonString = jsonData.getJson(2, "参数错误");
			return jsonString;
		}
		// 获取应该显示的评论消息列表
		List<Record> messageList = Db.find(sqlForCommentMessage);
		// 图片授权
		for (Record record : messageList) {
			String efirstpic = record.get("efirstpic");
			if (efirstpic == null) {
				int eMain = Integer.parseInt(record.get("eMain").toString());
				switch (eMain) {
				case 1:
					efirstpic = CommonParam.textBitmap;
					break;
				case 2:
					efirstpic = CommonParam.audioBitmap;
					break;
				case 3:
					efirstpic = CommonParam.placeBitmap;
					break;
				}
			} else {
				efirstpic = record.get("efirstpic").toString();
			}

			record.set("efirstpic", qiniu.getDownloadToken(efirstpic));
		}

		// 刷新、初始化时将相应的数据设置为已读
		switch (type) {
		case "initialize":
			// 获取该用户所有的消息的ID
			List<Record> messageID = Db
					.find("select mid from messages where mreceiver=" + userid + " and mstatus in(0,1) ");
			// 将评论消息列表中的每个记录状态标记为已读
			for (Record record : messageID) {
				Db.update(" update messages set mstatus=1 where mid=" + record.get("mid").toString() + " ");
			}
			break;
		case "refresh":
			// 将刷新后获得的评论消息列表中的元素的状态改为已读
			for (Record record : messageList) {
				Db.update(" update messages set mstatus=1 where mid=" + record.get("mid").toString() + " ");
			}
			break;
		}
		jsonString = jsonData.getJson(0, "success", messageList);
		return jsonString;
	}

	/**
	 * 获取通知消息的信息
	 * 
	 * @param userid
	 * @param id
	 * @param type
	 * @return
	 */
	@Before(Tx.class)
	public String getNotificationMessageInformation(String userid, String id, String type) {
		String SqlForNotification = "";
		// 根据type构造相应的数据
		switch (type) {
		case "initialize":
			SqlForNotification = "select nid,ncontent,ntype,ntime,nstatus,unickname,gpic as upic from notifications,users,groups where nsender=userid and ngroupid=groupid and nreceiver='"
					+ userid + "' and nstatus in(0,1) GROUP BY nid DESC limit 10";
			break;
		case "loading":
			SqlForNotification = "select nid,ncontent,ntype,ntime,nstatus,unickname,gpic as upic from notifications,users,groups where nsender=userid and ngroupid=groupid and nreceiver='"
					+ userid + "' and nid<" + id + " and nstatus in(0,1) GROUP BY nid DESC limit 10";
			break;
		case "refresh":
			SqlForNotification = "select nid,ncontent,ntype,ntime,nstatus,unickname,gpic as upic from notifications,users,groups where nsender=userid and ngroupid=groupid and nreceiver='"
					+ userid + "' and nid>" + id + " and nstatus in(0,1) GROUP BY nid DESC ";
			break;
		default:
			jsonString = jsonData.getJson(2, "参数错误");
			return jsonString;
		}
		// 获取应该显示的评论消息列表
		List<Record> notificationList = Db.find(SqlForNotification);
		// 刷新、初始化时将相应的数据设置为已读
		switch (type) {
		case "initialize":
			// 获取该用户所有通知的ID
			List<Record> notificationID = Db
					.find("select nid from notifications where nreceiver = " + userid + " and nstatus in(0,1) ");
			// 将通知列表中的每个元素的状态修改为已读
			for (Record record : notificationID) {
				Db.update(" update notifications set nstatus=1 where nid=" + record.get("nid").toString() + " ");
			}
			break;
		case "refresh":
			// 将通知列表中的每个元素的状态修改为已读
			for (Record record : notificationList) {
				Db.update(" update notifications set nstatus=1 where nid=" + record.get("nid").toString() + " ");
			}
			break;
		}
		jsonString = jsonData.getJson(0, "success", notificationList);
		return jsonString;
	}

	/**
	 * 获取邀请消息的信息
	 * 
	 * @param userid
	 * @param id
	 * @param type
	 * @return
	 */
	public String getInviteMessageInformation(String userid, String id, String type) {
		String sqlForInviteMessage = "";
		switch (type) {
		case "initialize":
			sqlForInviteMessage = "select unickname,upic,igcontent,igid,iggroupid,type,igtime,igstatus from invitegroup,users where userid=igsender and igstatus in(0,1) and igreceiver = '"
					+ userid + "'  group by igid desc limit 10";
			break;
		case "loading":
			sqlForInviteMessage = "select unickname,upic,igcontent,igid,iggroupid,type,igtime,igstatus from invitegroup,users where userid=igsender and igstatus in(0,1) and igreceiver = '"
					+ userid + "' and igid<" + id + "  group by igid desc limit 10";
			break;
		case "refresh":
			sqlForInviteMessage = "select unickname,upic,igcontent,igid,iggroupid,type,igtime,igstatus from invitegroup,users where userid=igsender and igstatus in(0,1) and igreceiver = '"
					+ userid + "' and igid>" + id + " group by igid desc";
			break;
		default:
			jsonString = jsonData.getJson(2, "参数错误");
			return jsonString;
		}
		List<Record> inviteList = Db.find(sqlForInviteMessage);
		jsonString = jsonData.getJson(0, "success", inviteList);
		return jsonString;
	}

	/**
	 * 获取通知列表
	 * 
	 * @param userid
	 * @param page
	 * @return
	 */
	@Before(Tx.class)
	public String getNotificationList(String userid, int minID) {
		String SqlForNotification = "";
		if (minID == 0) {
			// minID==0代表是初始化的情况
			// tip:组头像gpic用字段用户头像upic代替，前端不用修改，下同，谨以此防止忘记
			SqlForNotification = "select nid,ncontent,ntype,ntime,nstatus,unickname,gpic as upic from notifications,users,groups where nsender=userid and ngroupid=groupid and nreceiver='"
					+ userid + "' and nstatus in(0,1) GROUP BY nid DESC limit 10";
			// 获取该用户所有通知的ID
			List<Record> notificationID = Db
					.find("select nid from notifications where nreceiver = " + userid + " and nstatus in(0,1) ");
			// 将通知列表中的每个元素的状态修改为已读
			for (Record record : notificationID) {
				Db.update(" update notifications set nstatus=1 where nid=" + record.get("nid").toString() + " ");
			}
		} else {
			SqlForNotification = "select nid,ncontent,ntype,ntime,nstatus,unickname,gpic as upic from notifications,users,groups where nsender=userid and ngroupid=groupid and nreceiver='"
					+ userid + "' and nid<" + minID + " and nstatus in(0,1) GROUP BY nid DESC limit 10";
		}
		List<Record> notification = Db.find(SqlForNotification);// 获取事件的相关信息

		jsonString = jsonData.getJson(0, "success", notification);
		return jsonString;
	}

	/**
	 * 获取未读消息或通知的数量
	 * 
	 * @param receiverID
	 * @param type
	 * @return
	 */
	public Record getCount(String receiverID, String type) {
		Record record = new Record();
		List<Record> list = new ArrayList<Record>();
		switch (type) {
		case "comment":
			list = Db.find(
					"select count(*) as number from messages where mstatus=0 and mreceiver='" + receiverID + "' ");
			break;
		case "invitegroup":
			list = Db.find(
					"select count(*) as number from invitegroup where igstatus=0 and igreceiver='" + receiverID + "' ");
			break;
		case "notification":
			list = Db.find(
					"select count(*) as number from notifications where nstatus=0 and nreceiver='" + receiverID + "' ");
			break;
		}
		record = list.get(0);
		return record;
	}

	/**
	 * 获取推送当中badge的数字
	 */
	public int getNumbersInBadge(String userid) {
		// 获取各个部分的未读消息数量
		Record commentRecord = getCount(userid, "comment");
		Record invitegroupRecord = getCount(userid, "invitegroup");
		Record notificationRecord = getCount(userid, "notification");
		int comment = Integer.parseInt(commentRecord.get("number").toString());
		int invite = Integer.parseInt(invitegroupRecord.get("number").toString());
		int notification = Integer.parseInt(notificationRecord.get("number").toString());
		int number = comment + invite + notification;
		return number;
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
	public String upload(String userid, String groupid, String picAddress, String content, String storage,
			String memorytime, String mode, String location, String source) {

		String eventID = "";

		// 将地址字符串转换成数组
		String[] picArray = dataProcess.getPicAddress(picAddress, mode);

		// 图片鉴黄
		picArray = dataProcess.PictureVerify(picArray);

		// 获取动态第一张图片地址
		String firstPic = picArray[0];

		// 将占用空间的类型转为double
		Double place = Double.parseDouble(storage);

		String newTime = "";
		// 保存事件
		Event event = new Event();

		// 判断memorytime字段是否有传过来
		if (memorytime == null || memorytime.equals("")) {
			event.set("egroupid", groupid).set("euserid", userid).set("etext", content).set("efirstpic", firstPic)
					.set("eStoragePlace", place).set("eSource", source).set("isSynchronize", 1);
		} else {
			// 拼接字符串
			newTime = memorytime + " 00:00:00";
			event.set("egroupid", groupid).set("euserid", userid).set("etext", content).set("efirstpic", firstPic)
					.set("ememorytime", newTime).set("eStoragePlace", place).set("eSource", source)
					.set("isSynchronize", 1);
		}

		// 判断mode是不是等于dayMark 即日签，是则加入etype为3
		if (mode.equals("dayMark")) {
			event.set("etype", 3);
		}

		// 判断位置信息
		if (location != null && !location.equals("")) {
			event.set("eplace", location);
		}

		if (event.save()) {
			eventID = event.get("eid").toString();

			// 将事件所属组的状态改为有新状态
			// Db.update("update groupmembers set gmnotify=1 where gmgroupid ="
			// + groupid + " ");
			// // 发布者的状态改为无新状态，因为就算别人发了新动态，发布者发布完后还在组内动态界面
			// Db.update("update groupmembers set gmnotify=0 where gmgroupid ="
			// + groupid + " and gmuserid = " + userid + " ");

			// 判断memorytime字段是否有传过来
			if (memorytime == null || memorytime.equals("")) {
				// 保存图片
				for (int i = 0; i < picArray.length; i++) {
					Picture pic = new Picture();
					pic.set("peid", eventID).set("poriginal", picArray[i]);
					pic.save();
				}
			} else {
				// newTime不需要再拼接
				// 保存图片
				for (int i = 0; i < picArray.length; i++) {
					Picture pic = new Picture();
					pic.set("peid", eventID).set("poriginal", picArray[i]).set("pmemorytime", newTime);
					pic.save();
				}
			}

		}

		// 增加用户已使用空间
		if (place != 0) {
			updateUserStoragePlace(userid, place, "add");
		}

		// // 进行推送准备
		// // 获取组内所有用户的ID
		// List<Record> useridList = Db
		// .find("select gmuserid from groupmembers where gmgroupid = "
		// + groupid + " ");
		// // 去掉发送者的ID
		// for (Record record : useridList) {
		// if ((record.get("gmuserid").toString()).equals(userid)) {
		// useridList.remove(record);
		// break;
		// }
		// }
		// // useridList不为null时才进行推送
		// if (!(useridList.isEmpty())) {
		// // 将list中的ID拼接成字符串
		// String userids = dataProcess.changeListToString(useridList,
		// "gmuserid");
		// // userids不为空字符串时才进行推送
		// if (!(userids.equals(""))) {
		// // 获取要进行推送的用户的cid
		// Record cid = pushMessage.getUsersCids(userids, "userid");
		// // 获取动态发布者的昵称
		// String nickname = dao.getUserSingleInfo(userid, "nickname");
		// // 获取发布动态所在组的信息
		// List<Record> groupList = Db
		// .find("select gname,gtype from groups where groupid = "
		// + groupid + " ");
		// groupList = dataProcess.changeGroupTypeIntoWord(groupList);
		// // 拼接推送内容
		// String pushContent = nickname + "在私密空间“"
		// + groupList.get(0).getStr("gname") + "”发布了新动态";
		// Record data = new Record().set("content", pushContent);
		// // 设置透传内容
		// int gid = Integer.parseInt(groupid);
		// Record transmissionRecord = new Record().set("groupid", gid)
		// .set("pushContent", pushContent)
		// .set("gname", groupList.get(0).getStr("gname"));
		// List<Record> list = new ArrayList<Record>();
		// list.add(transmissionRecord);
		// String transmissionContent = jsonData.getJson(3, "发表动态", list);
		// data.set("transmissionContent", transmissionContent);
		// // 推送
		// push.yinianPushToList(cid, data);
		// }
		// }
		return eventID;

	}

	/**
	 * 上传一些动态
	 * 
	 */
	@Before(Tx.class)
	public boolean uploadSomeEvent(String userid, String groupid, String picAddress, String content, String storage) {
		String eventID = "";
		// 将地址字符串转换成数组
		String[] picArray = dataProcess.getPicAddress(picAddress, null);
		// 获取动态第一张图片地址
		String firstPic = picArray[0];
		// 将占用空间的类型转为double
		Double place = Double.parseDouble(storage);

		// 保存事件
		Event event = new Event();
		// 判断memorytime字段是否有传过来

		event.set("egroupid", groupid).set("euserid", userid).set("etext", content).set("efirstpic", firstPic)
				.set("eStoragePlace", place).set("eOrigin", 1);

		if (event.save()) {
			eventID = event.get("eid").toString();
			// 保存图片
			for (int i = 0; i < picArray.length; i++) {
				Picture pic = new Picture();
				pic.set("peid", eventID).set("poriginal", picArray[i]).set("pOrigin", 1)
					.set("pGroupid", groupid).set("puserid", userid);
				pic.save();
			}

		}
		// 增加用户已使用空间
		boolean flag = updateUserStoragePlace(userid, place, "add");

		return flag;

	}

	/**
	 * 上传记忆卡片
	 * 
	 * @param userid
	 * @param groupid
	 * @param picAddress
	 * @param tags
	 * @param audio
	 * @param content
	 * @param storage
	 * @param memorytime
	 * @param cardstyle
	 * @param place
	 * @return
	 */
	@Before(Tx.class)
	public String uploadMemoryCard(String userid, String groupid, String picAddress, String tags, String audio,
			String content, String storage, String memorytime, String cardstyle, String place, String audiotime,
			String mode) {
		String eventID = "";
		// 将地址字符串转换成数组
		String[] picArray = dataProcess.getPicAddress(picAddress, mode);
		// 获取动态第一张图片地址
		String firstPic = picArray[0];
		// 判断占用空间的值并将占用空间的类型转为double
		if (storage == null || storage.equals("")) {
			storage = "0";
		}
		Double storagePlace = Double.parseDouble(storage);

		String newTime = "";
		if ((audiotime == null || audiotime.equals(""))) {
			audiotime = "0";
		}
		// 保存事件,先存好一定会存进去的参数，其他的再根据情况判断
		Event event = new Event().set("egroupid", groupid).set("euserid", userid).set("etext", content)
				.set("efirstpic", firstPic).set("eStoragePlace", storagePlace).set("ecardstyle", cardstyle)
				.set("eplace", place).set("etype", 1).set("eaudiotime", audiotime);
		// 判断memorytime字段是否有传过来
		if (!(memorytime.equals("")) && memorytime != null) {
			// 拼接字符串
			newTime = memorytime + " 00:00:00";
			event.set("ememorytime", newTime);
		}
		// 判断audio是否有前缀，以此是上传、分享、同步三个功能可以兼容
		if (audio != null && !audio.equals("")) {
			if ((audio.substring(0, 7)).equals("http://")) {
				;
			} else {
				audio = CommonParam.qiniuOpenAddress + audio;
			}
			event.set("eaudio", audio);
		}
		if (event.save()) {
			eventID = event.get("eid").toString();
			// 将事件所属组的状态改为有新状态
			Db.update("update groupmembers set gmnotify=1 where gmgroupid =" + groupid + " ");
			// 发布者的状态改为无新状态，因为就算别人发了新动态，发布者发布完后还在组内动态界面
			Db.update("update groupmembers set gmnotify=0 where gmgroupid =" + groupid + " and gmuserid = " + userid
					+ " ");

			// 判断memorytime字段是否有传过来
			if (memorytime == null || memorytime.equals("")) {
				// 保存图片
				for (int i = 0; i < picArray.length; i++) {
					Picture pic = new Picture();
					pic.set("peid", eventID).set("poriginal", picArray[i]).set("pGroupid", groupid).set("puserid", userid);
					pic.save();
				}
			} else {
				// newTime不需要再拼接
				// 保存图片
				for (int i = 0; i < picArray.length; i++) {
					Picture pic = new Picture();
					pic.set("peid", eventID).set("poriginal", picArray[i]).set("pmemorytime", newTime)
					.set("pGroupid", groupid).set("puserid", userid);
					pic.save();
				}
			}

			// 判断标签tags是否有传值，有则进行插入
			if (tags != null) {
				// 通过 &nbsp 来隔开各个tag
				String[] eventTag = tags.split("&nbsp");
				for (int i = 0; i < eventTag.length; i++) {
					Tag tag = new Tag().set("tagEventID", eventID).set("tagContent", eventTag[i]);
					tag.save();
					// 对每个标签进行历史标签处理
					handleTagsInHistoryTags(userid, eventTag[i]);
				}
			}

		}
		// 增加用户已使用空间
		updateUserStoragePlace(userid, storagePlace, "add");

		// 进行推送准备
		// 获取组内所有用户的ID
		List<Record> useridList = Db.find("select gmuserid from groupmembers where gmgroupid = " + groupid + " ");
		// 去掉发送者的ID
		for (Record record : useridList) {
			if ((record.get("gmuserid").toString()).equals(userid)) {
				useridList.remove(record);
				break;
			}
		}
		// useridList不为null时才进行推送
		if (!(useridList.isEmpty())) {
			// 将list中的ID拼接成字符串
			String userids = dataProcess.changeListToString(useridList, "gmuserid");
			// userids不为空字符串时才进行推送
			if (!(userids.equals(""))) {
				// 获取要进行推送的用户的cid
				Record cid = pushMessage.getUsersCids(userids, "userid");
				// 获取动态发布者的昵称
				String nickname = dao.getUserSingleInfo(userid, "nickname");
				// 获取发布动态所在组的信息
				List<Record> groupList = Db.find("select gname,gtype from groups where groupid = " + groupid + " ");
				groupList = dataProcess.changeGroupTypeIntoWord(groupList);
				// 拼接推送内容
				String pushContent = nickname + "在私密空间“" + groupList.get(0).getStr("gname") + "”上传了一张记忆卡片";
				Record data = new Record().set("content", pushContent);
				// 设置透传内容
				int gid = Integer.parseInt(groupid);
				Record transmissionRecord = new Record().set("groupid", gid).set("pushContent", pushContent)
						.set("gname", groupList.get(0).getStr("gname"));
				List<Record> list = new ArrayList<Record>();
				list.add(transmissionRecord);
				String transmissionContent = jsonData.getJson(3, "发表动态", list);
				data.set("transmissionContent", transmissionContent);
				// 推送
				push.yinianPushToList(cid, data);
			}
		}
		return eventID;
	}

	/**
	 * 上传时光明信片
	 * 
	 * @param userid
	 * @param groupid
	 * @param picAddress
	 * @param audiotime
	 * @param audio
	 * @param content
	 * @param storage
	 * @param memorytime
	 * @param coverUrl
	 * @param place
	 * @return
	 */
	@Before(Tx.class)
	public String uploadPostcard(String userid, String groupid, String picAddress, String audiotime, String audio,
			String content, String storage, String memorytime, String coverUrl, String place, String mode) {
		String eventID = "";
		// 将地址字符串转换成数组
		String[] picArray = dataProcess.getPicAddress(picAddress, mode);
		// 获取动态第一张图片地址
		String firstPic = picArray[0];
		// 判断占用空间的值并将占用空间的类型转为double
		if (storage == null || storage.equals("")) {
			storage = "0";
		}
		Double storagePlace = Double.parseDouble(storage);

		String newTime = "";
		// 保存事件,先存好一定会存进去的参数，其他的再根据情况判断
		Event event = new Event().set("egroupid", groupid).set("euserid", userid).set("etext", content)
				.set("efirstpic", firstPic).set("eStoragePlace", storagePlace).set("ecover", coverUrl)
				.set("eplace", place).set("etype", 2).set("eaudiotime", audiotime);
		// 判断memorytime字段是否有传过来
		if (!(memorytime.equals("")) && memorytime != null) {
			// 拼接字符串
			newTime = memorytime + " 00:00:00";
			event.set("ememorytime", newTime);
		}
		// 判断audio是否有前缀，以此是上传、分享、同步三个功能可以兼容
		if (audio != null && !(audio.equals(""))) {
			if ((audio.substring(0, 7)).equals("http://")) {
				;
			} else {
				audio = CommonParam.qiniuOpenAddress + audio;
			}
			event.set("eaudio", audio);
		}
		if (event.save()) {
			eventID = event.get("eid").toString();
			// 将事件所属组的状态改为有新状态
			Db.update("update groupmembers set gmnotify=1 where gmgroupid =" + groupid + " ");
			// 发布者的状态改为无新状态，因为就算别人发了新动态，发布者发布完后还在组内动态界面
			Db.update("update groupmembers set gmnotify=0 where gmgroupid =" + groupid + " and gmuserid = " + userid
					+ " ");

			// 判断memorytime字段是否有传过来
			if (memorytime == null || memorytime.equals("")) {
				// 保存图片
				for (int i = 0; i < picArray.length; i++) {
					Picture pic = new Picture();
					pic.set("peid", eventID).set("poriginal", picArray[i])
					.set("pGroupid", groupid).set("puserid", userid);
					pic.save();
				}
			} else {
				// newTime不需要再拼接
				// 保存图片
				for (int i = 0; i < picArray.length; i++) {
					Picture pic = new Picture();
					pic.set("peid", eventID).set("poriginal", picArray[i]).set("pmemorytime", newTime)
					.set("pGroupid", groupid).set("puserid", userid);
					pic.save();
				}
			}

			// 判断封面是否为历史封面，不是则插入到历史记录当中，通过URL进行判断
			List<Record> list = Db.find("select * from historycover where historyCoverPicture='" + coverUrl
					+ "' and historyCoverUserID=" + userid + " and historyCoverStatus=0 ");
			if (list.size() == 0) {
				HistoryCover hc = new HistoryCover().set("historyCoverPicture", coverUrl).set("historyCoverUserID",
						userid);
				hc.save();
			}

		}
		// 增加用户已使用空间
		updateUserStoragePlace(userid, storagePlace, "add");

		// 进行推送准备
		// 获取组内所有用户的ID
		List<Record> useridList = Db.find("select gmuserid from groupmembers where gmgroupid = " + groupid + " ");
		// 去掉发送者的ID
		for (Record record : useridList) {
			if ((record.get("gmuserid").toString()).equals(userid)) {
				useridList.remove(record);
				break;
			}
		}
		// useridList不为null时才进行推送
		if (!(useridList.isEmpty())) {
			// 将list中的ID拼接成字符串
			String userids = dataProcess.changeListToString(useridList, "gmuserid");
			// userids不为空字符串时才进行推送
			if (!(userids.equals(""))) {
				// 获取要进行推送的用户的cid
				Record cid = pushMessage.getUsersCids(userids, "userid");
				// 获取动态发布者的昵称
				String nickname = dao.getUserSingleInfo(userid, "nickname");
				// 获取发布动态所在组的信息
				List<Record> groupList = Db.find("select gname,gtype from groups where groupid = " + groupid + " ");
				groupList = dataProcess.changeGroupTypeIntoWord(groupList);
				// 拼接推送内容
				String pushContent = nickname + "在私密空间“" + groupList.get(0).getStr("gname") + "”中上传了一张时光明信片";
				Record data = new Record().set("content", pushContent);
				// 设置透传内容
				int gid = Integer.parseInt(groupid);
				Record transmissionRecord = new Record().set("groupid", gid).set("pushContent", pushContent)
						.set("gname", groupList.get(0).getStr("gname"));
				List<Record> list = new ArrayList<Record>();
				list.add(transmissionRecord);
				String transmissionContent = jsonData.getJson(3, "发表动态", list);
				data.set("transmissionContent", transmissionContent);
				// 推送
				push.yinianPushToList(cid, data);
			}
		}
		return eventID;
	}

	/**
	 * 无推送的上传动态 1.2版本 2016.1.29新增
	 * 
	 * @param userid
	 * @param groupid
	 * @param picAddress
	 * @param content
	 * @return
	 */
	@Before(Tx.class)
	public String uploadWithoutPush(String userid, String groupid, String picAddress, String content, String shottime,
			String shotplace, String mode) {
		String eventID = "";
		// 将地址字符串转换成数组
		String[] picArray = dataProcess.getPicAddress(picAddress, mode);
		// 获取动态第一张图片地址
		String firstPic = picArray[0];

		// 保存事件
		Event event = new Event();
		event.set("egroupid", groupid).set("euserid", userid).set("etext", content).set("efirstpic", firstPic);
		if (event.save()) {
			eventID = event.get("eid").toString();
			// 将事件所属组的状态改为有新状态
			Db.update("update groupmembers set gmnotify=1 where gmgroupid =" + groupid + " ");
			// 发布者的状态改为无新状态，因为就算别人发了新动态，发布者发布完后还在组内动态界面
			Db.update("update groupmembers set gmnotify=0 where gmgroupid =" + groupid + " and gmuserid = " + userid
					+ " ");
			// 保存图片 \
			for (int i = 0; i < picArray.length; i++) {
				Picture pic = new Picture();
				pic.set("peid", eventID).set("poriginal", picArray[i]).set("pshottime", shottime).set("pshotplace",
						shotplace).set("pGroupid", groupid).set("puserid", userid);
				pic.save();

			}
		}
		return eventID;

	}

	/**
	 * 投稿至官方相册 2016.3.1新增
	 * 
	 * @param userid
	 * @param groupid
	 * @param picAddress
	 * @param content
	 * @return
	 */
	@Before(Tx.class)
	public String contributeToOfficialAlbum(String userid, String groupid, String picAddress, String content,
			String shottime, String shotplace, String mode) {
		String eventID = "";
		// 将地址字符串转换成数组
		String[] picArray = dataProcess.getPicAddress(picAddress, mode);
		// 获取动态第一张图片地址
		String firstPic = picArray[0];

		// 保存事件,同时设置该条动态的状态为2 （待审核）
		Event event = new Event();
		event.set("egroupid", groupid).set("euserid", userid).set("etext", content).set("efirstpic", firstPic)
				.set("estatus", 2);
		if (event.save()) {
			eventID = event.get("eid").toString();
			// 将事件所属组的状态改为有新状态
			Db.update("update groupmembers set gmnotify=1 where gmgroupid =" + groupid + " ");
			// 发布者的状态改为无新状态，因为就算别人发了新动态，发布者发布完后还在组内动态界面
			Db.update("update groupmembers set gmnotify=0 where gmgroupid =" + groupid + " and gmuserid = " + userid
					+ " ");
			// 保存图片
			for (int i = 0; i < picArray.length; i++) {
				Picture pic = new Picture();
				pic.set("peid", eventID).set("poriginal", picArray[i]).set("pshottime", shottime).set("pshotplace",
						shotplace).set("pGroupid", groupid).set("puserid", userid);
				pic.save();

			}
		}
		return eventID;

	}

	/**
	 * 投稿记忆卡片至官方相册
	 * 
	 * @param userid
	 * @param groupid
	 * @param picAddress
	 * @param content
	 * @return
	 */
	@Before(Tx.class)
	public String contributeMemoryCardToOfficialAlbum(String userid, String groupid, String picAddress, String tags,
			String audio, String content, String storage, String memorytime, String cardstyle, String place,
			String audiotime, int estatus, String mode) {
		String eventID = "";
		// 将地址字符串转换成数组
		String[] picArray = dataProcess.getPicAddress(picAddress, mode);
		// 获取动态第一张图片地址
		String firstPic = picArray[0];
		// 判断占用空间的值并将占用空间的类型转为double
		if (storage == null || storage.equals("")) {
			storage = "0";
		}
		Double storagePlace = Double.parseDouble(storage);

		String newTime = "";
		// 保存事件,先存好一定会存进去的参数，其他的再根据情况判断
		Event event = new Event().set("egroupid", groupid).set("euserid", userid).set("etext", content)
				.set("efirstpic", firstPic).set("eStoragePlace", storagePlace).set("ecardstyle", cardstyle)
				.set("eplace", place).set("etype", 1).set("eaudiotime", audiotime).set("estatus", estatus);
		// 判断memorytime字段是否有传过来
		if (!(memorytime.equals("")) && memorytime != null) {
			// 拼接字符串
			newTime = memorytime + " 00:00:00";
			event.set("ememorytime", newTime);
		}
		// 判断audio是否有前缀，以此是上传、分享、同步三个功能可以兼容
		if (audio != null) {
			if ((audio.substring(0, 7)).equals("http://")) {
				;
			} else {
				audio = CommonParam.qiniuOpenAddress + audio;
			}
			event.set("eaudio", audio);
		}
		if (event.save()) {
			eventID = event.get("eid").toString();

			// 判断memorytime字段是否有传过来
			if (memorytime == null || memorytime.equals("")) {
				// 保存图片
				for (int i = 0; i < picArray.length; i++) {
					Picture pic = new Picture();
					pic.set("peid", eventID).set("poriginal", picArray[i]);
					pic.save();
				}
			} else {
				// newTime不需要再拼接
				// 保存图片
				for (int i = 0; i < picArray.length; i++) {
					Picture pic = new Picture();
					pic.set("peid", eventID).set("poriginal", picArray[i]).set("pmemorytime", newTime);
					pic.save();
				}
			}

			// 判断标签tags是否有传值，有则进行插入
			if (tags != null) {
				// 通过 &nbsp 来隔开各个tag
				String[] eventTag = tags.split("&nbsp");
				for (int i = 0; i < eventTag.length; i++) {
					Tag tag = new Tag().set("tagEventID", eventID).set("tagContent", eventTag[i]);
					tag.save();
					// 对每个标签进行历史标签处理
					handleTagsInHistoryTags(userid, eventTag[i]);
				}
			}

		}
		// 增加用户已使用空间
		updateUserStoragePlace(userid, storagePlace, "add");
		return eventID;
	}

	/**
	 * 刷新动态
	 * 
	 * @param maxEventID
	 * @param groupid
	 * @return
	 */
	public String refreshEvent(String maxEventID, String groupid) {
		// 事件查询语句
		String sqlForEvent = "select eid,egroupid,userid,unickname,upic,etext,eaudio,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\" ) as url from users,events,pictures where userid=euserid and eid=peid and egroupid="
				+ groupid + " and eid > " + maxEventID + " and estatus=0 and pstatus=0 group by peid DESC";
		// 评论查询语句
		String sqlForComment = CommonParam.selectForComment
				+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and egroupid="
				+ groupid + " and cstatus=0 ORDER BY ceid,ctime asc";

		List<Record> event = dataProcess.encapsulationEventList(Db.find(sqlForEvent));// 获取事件的相关信息并封装里面的用户对象
		List<Record> comment = dataProcess.encapsulationCommentList(Db.find(sqlForComment));// 获取事件的评论信息并封装里面的用户对象
		List<Record> list = dataProcess.combieEventAndComment(event, comment);// 拼接事件与评论
		// 封装事件和标签
		list = dataProcess.combineEventWithTags(list);
		// 封装事件和卡片样式
		list = dataProcess.combineEventWithCardStyle(list);
		list = dataProcess.ChangePicAsArray(list);// 修改upic字段格式
		jsonString = jsonData.getJson(0, "success", list);
		return jsonString;
	}

	/**
	 * 初始化、刷新、加载官方相册内的动态
	 * 
	 * @param userid
	 * @param groupid
	 * @param eventid
	 * @param sign
	 * @return
	 */
	public String getOfficialAlbumEvents(String userid, String groupid, String eventid, String sign) {
		int count = 1;
		// 构造相应的SQL语句
		String sqlForEvent = "";
		String sqlForComment = "";
		switch (sign) {
		case "initialize":
			sqlForEvent = "select eid,egroupid,userid,unickname,upic,etext,lislike,elike,eaudio,eaudiotime,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\" ) as url from users,events,pictures,likes where userid=euserid and eid=leid and eid=peid and luserid="
					+ userid + " and egroupid=" + groupid
					+ " and estatus in(0,3) and pstatus=0  group by peid DESC limit 10";
			sqlForComment = CommonParam.selectForComment
					+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and egroupid="
					+ groupid + " and cstatus=0 ORDER BY ceid,ctime asc";
			// 初始化的时候更新相册状态为无新动态
			count = Db.update(
					"update groupmembers set gmnotify=0 where gmgroupid=" + groupid + " and gmuserid=" + userid + " ");
			break;
		case "refresh":
			sqlForEvent = "select eid,egroupid,userid,unickname,upic,etext,lislike,elike,eaudio,eaudiotime,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\" ) as url from users,events,pictures,likes where userid=euserid and eid=leid and eid=peid and luserid="
					+ userid + " and egroupid=" + groupid + " and eid > " + eventid
					+ " and estatus in(0,3) and pstatus=0 group by peid DESC";
			sqlForComment = CommonParam.selectForComment
					+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and eid>"
					+ eventid + " and egroupid=" + groupid + " and cstatus=0 ORDER BY ceid,ctime asc";
			break;
		case "loading":
			sqlForEvent = "select eid,egroupid,userid,unickname,upic,etext,lislike,elike,eaudio,eaudiotime,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\" ) as url from users,events,pictures,likes where userid=euserid and eid=leid and eid=peid and luserid="
					+ userid + " and egroupid=" + groupid + " and eid < " + eventid
					+ " and estatus in(0,3) and pstatus=0 group by peid DESC limit 10";
			sqlForComment = CommonParam.selectForComment
					+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and eid<"
					+ eventid + " and egroupid=" + groupid + " and cstatus=0 ORDER BY ceid,ctime asc";
			break;
		}
		List<Record> event = dataProcess.encapsulationEventList(Db.find(sqlForEvent));// 获取事件的相关信息并封装里面的用户对象
		List<Record> comment = dataProcess.encapsulationCommentList(Db.find(sqlForComment));// 获取事件的评论信息并封装里面的用户对象
		List<Record> list = dataProcess.combieEventAndComment(event, comment);// 拼接事件与评论
		// 封装事件和标签
		list = dataProcess.combineEventWithTags(list);
		// 封装事件和卡片样式
		list = dataProcess.combineEventWithCardStyle(list);
		list = dataProcess.ChangePicAsArray(list);// 修改upic字段格式
		jsonString = jsonData.getJson(0, "success", list);

		return jsonString;
	}

	/**
	 * 刷新评论
	 * 
	 * @param maxCommentID
	 * @param eventID
	 * @return
	 */
	public String loadComment(String maxCommentID, String eventID) {
		// 评论查询语句
		String sqlForComment = CommonParam.selectForComment
				+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and eid ="
				+ eventID + " and cid>" + maxCommentID + " ORDER BY cid,ctime asc";
		List<Record> list = Db.find(sqlForComment);
		List<Record> comment = dataProcess.encapsulationCommentList(list);
		jsonString = jsonData.getJson(0, "success", comment);
		return jsonString;
	}

	/**
	 * 发送邀请进组的消息
	 * 
	 * @param userid
	 * @param groupid
	 * @param phonenumbers
	 * @return
	 */
	public List<Record> sendInviteNotification(String userid, String groupid, Record messageInfo, String phonenumbers) {

		// 存储推送相关信息的列表
		List<Record> pushList = new ArrayList<Record>();
		// 需要通知的电话号码拆分成数组
		String[] phoneArray = phonenumbers.split(",");
		// 构造短信内容
		String content = "邀请你加入" + messageInfo.getStr("type").toString() + "“"
				+ messageInfo.getStr("groupName").toString() + "”";
		// 开启事务，多条消息插入成功后提交
		boolean succeed = Db.tx(new IAtom() {
			public boolean run() throws SQLException {
				boolean flag = true;
				Record UserRecord;
				for (int i = 0; i < phoneArray.length; i++) {
					// 查询接收者的用户ID
					UserRecord = Db.findFirst("select userid from users where uphone='" + phoneArray[i] + "' ");
					// 插入数据
					InviteGroup invite = new InviteGroup().set("igsender", userid)
							.set("igreceiver", UserRecord.get("userid")).set("igcontent", content)
							.set("iggroupid", groupid);
					if (invite.save()) {
						flag = true;
						// 获取ID
						String igid = invite.get("igid").toString();
						// 获取推送Record
						Record pushRecord = pushMessage.getPushRecord(userid, UserRecord.get("userid").toString(),
								groupid, content, igid, "inviteGroup", null);
						if (!((pushRecord.toJson()).equals("{}"))) {
							// Record不为空时，加入到list中
							pushList.add(pushRecord);
						}
					} else {
						flag = false;
						break;
					}
				}
				return flag;
			}
		});
		if (succeed) {
			// 插入成功返回pushList
			return pushList;
		} else {
			// 插入失败返回空的list
			List<Record> list = new ArrayList<Record>();
			return list;
		}
	}

	/**
	 * 插入成员进组通知
	 * 
	 * @param groupid
	 * @param userid
	 * @param useridList
	 * @return
	 */
	@Before(Tx.class)
	public boolean insertEnterNotification(String groupid, String userid, List<Record> useridList) {

		// 推送list
		List<Record> pushList = new ArrayList<Record>();
		boolean flag = true;
		// 获取组类型
		Record record = Db.findFirst("select gtype,gname from groups where groupid = " + groupid + " ");
		record.set("groupid", groupid);
		// 将组类型转换成中文
		List<Record> list = new ArrayList<Record>();
		list.add(record);
		list = dataProcess.changeGroupTypeIntoWord(list);
		record = list.get(0);
		// 构成通知内容
		String content = dataProcess.getNotificationContent(record, "enter");

		// 插入数据
		for (Record user : useridList) {
			int userID = Integer.parseInt(user.get("gmuserid").toString());
			Notification notification = new Notification().set("nsender", Integer.parseInt(userid))
					.set("nreceiver", userID).set("ncontent", content).set("ntype", 1).set("ngroupid", groupid);
			if (notification.save()) {
				flag = true;
				// 通知存储成功的同时,构造透传内容并加入到推送list中
				String nid = notification.get("nid").toString();
				Record pushRecord = pushMessage.getPushRecord(userid, user.get("gmuserid").toString(), groupid, content,
						nid, "notification", null);
				if (!((pushRecord.toJson()).equals("{}"))) {
					// Record不为空时，加入到list中
					pushList.add(pushRecord);
				}
			} else {
				flag = false;
			}
		}
		// 进行推送
		if (flag) {
			push.yinianPushToSingle(pushList);
		}
		return flag;
	}

	/**
	 * 成员进入官方相册后插入通知并推送
	 * 
	 * @param groupid
	 * @param userid
	 * @return
	 */
	public boolean enterOfficialAlbumNotification(String groupid, String userid) {
		// 推送list
		List<Record> pushList = new ArrayList<Record>();
		boolean flag = true;
		// 获取组类型
		Record record = Db.findFirst("select gtype,gname from groups where groupid = " + groupid + " ");
		record.set("groupid", groupid);
		// 将组类型转换成中文
		List<Record> list = new ArrayList<Record>();
		list.add(record);
		list = dataProcess.changeGroupTypeIntoWord(list);
		record = list.get(0);
		// 构成通知内容
		String content = dataProcess.getNotificationContent(record, "enter");
		// 插入通知数据
		Notification notification = new Notification().set("nsender", Integer.parseInt(userid))
				.set("nreceiver", CommonParam.superUserID).set("ncontent", content).set("ntype", 1)
				.set("ngroupid", groupid);
		if (notification.save()) {
			flag = true;
			// 通知存储成功的同时,构造透传内容并加入到推送list中
			String nid = notification.get("nid").toString();
			Record pushRecord = pushMessage.getPushRecord(userid, CommonParam.superUserID, groupid, content, nid,
					"notification", null);
			if (!((pushRecord.toJson()).equals("{}"))) {
				// Record不为空时，加入到list中
				pushList.add(pushRecord);
			}
		} else {
			flag = false;
		}
		// 进行推送
		if (flag) {
			push.yinianPushToSingle(pushList);
		}
		return flag;
	}

	/**
	 * 创建者同意申请人进入组
	 * 
	 * @param groupid
	 * @param applyUserid
	 * @param useridList
	 * @return
	 */
	@Before(Tx.class)
	public boolean agreeJoinGroup(String groupid, String applyUserid, List<Record> useridList) {
		// 推送list
		List<Record> pushList = new ArrayList<Record>();
		boolean flag1 = true;
		boolean flag2 = true;
		// 获取组信息
		Record record = Db.findFirst("select gtype,gname from groups where groupid = " + groupid + " ");
		record.set("groupid", groupid);
		// 将组类型转换成中文
		List<Record> list = new ArrayList<Record>();
		list.add(record);
		list = dataProcess.changeGroupTypeIntoWord(list);
		record = list.get(0);
		// 构成通知内容
		String successContent = dataProcess.getNotificationContent(record, "success");// 给申请人
		String enterContent = dataProcess.getNotificationContent(record, "enter");// 给组内其他成员
		// 向组内其他成员插入该申请者人进组的通知数据
		for (Record user : useridList) {
			int userID = Integer.parseInt(user.get("gmuserid").toString());
			Notification notification = new Notification().set("nsender", Integer.parseInt(applyUserid))
					.set("nreceiver", userID).set("ncontent", enterContent).set("ntype", 1).set("ngroupid", groupid);
			if (notification.save()) {
				flag1 = true;
				// 通知存储成功的同时,构造透传内容并加入到推送list中
				String nid = notification.get("nid").toString();
				Record pushRecord = pushMessage.getPushRecord(applyUserid, user.get("gmuserid").toString(), groupid,
						enterContent, nid, "notification", null);
				if (!((pushRecord.toJson()).equals("{}"))) {
					// Record不为空时，加入到list中
					pushList.add(pushRecord);
				}
			} else {
				flag1 = false;
				break;
			}
		}
		// 向申请人插入已成功进组的系统通知
		Notification notification = new Notification().set("nsender", CommonParam.systemUserID)
				.set("nreceiver", Integer.parseInt(applyUserid)).set("ncontent", successContent).set("ntype", 0)
				.set("ngroupid", groupid);
		if (notification.save()) {
			flag2 = true;
			// 通知存储成功的同时,构造透传内容并加入到推送list中
			String nid = notification.get("nid").toString();
			Record pushRecord = pushMessage.getPushRecord(CommonParam.systemUserID, applyUserid, groupid,
					successContent, nid, "agree", null);
			if (!((pushRecord.toJson()).equals("{}"))) {
				// Record不为空时，加入到list中
				pushList.add(pushRecord);
			}
		} else {
			flag2 = false;
		}
		if (flag1 && flag2) {
			push.yinianPushToSingle(pushList);
		}
		return flag1 && flag2;
	}

	/**
	 * 获取新的通知
	 * 
	 * @param userid
	 * @param maxNid
	 * @return
	 */
	@Before(Tx.class)
	public List<Record> getNewNotification(String userid, String maxNid) {
		String sql = "select upic,unickname,ncontent,nid,nstatus,ntype,ntime from notifications,users where userid=nsender and nreceiver="
				+ userid + " and nid>" + maxNid + " and nstatus in(0,1) order by nid DESC ";
		List<Record> list = Db.find(sql);
		// 将通知列表中的每个元素的状态修改为已读
		for (Record record : list) {
			Db.update(" update notifications set nstatus=1 where nid=" + record.get("nid").toString() + " ");
		}
		return list;
	}

	/**
	 * 获取新的“我的”
	 * 
	 * @param userid
	 * @param maxEventID
	 * @return
	 */
	public List<Record> getNewMe(String userid, String maxEventID) {
		String commentSql = CommonParam.selectForComment
				+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and euserid ='"
				+ userid + "' and eid>" + maxEventID + " and cstatus = 0 ORDER BY ceid,ctime asc ";
		String SqlForEvent = "SELECT eid,egroupid,gname,etext,eaudio,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\") AS url FROM groups,EVENTS,pictures WHERE groupid = egroupid AND eid = peid AND euserid = '"
				+ userid + "' and eid>" + maxEventID + " and estatus in(0,3) and pstatus=0 GROUP BY peid DESC ";
		List<Record> event = Db.find(SqlForEvent);// 获取事件的相关信息
		List<Record> comment = Db.find(commentSql);// 获取事件的评论信息
		comment = dataProcess.encapsulationCommentList(comment);// 封装评论内的用户类
		List<Record> list = dataProcess.combieEventAndComment(event, comment);
		// 封装事件和标签
		list = dataProcess.combineEventWithTags(list);
		// 封装事件和卡片样式
		list = dataProcess.combineEventWithCardStyle(list);
		list = dataProcess.ChangePicAsArray(list);// 修改upic字段格式
		return list;
	}

	/**
	 * 获取新的消息列表
	 * 
	 * @param userid
	 * @param maxMid
	 * @param maxIGid
	 * @return
	 */
	@Before(Tx.class)
	public List<Record> getNewMessage(String userid, String maxMid, String maxIGid) {
		String sqlForInvite = "select unickname,upic,igcontent,igid,iggroupid,type,igtime,igstatus from invitegroup,users where userid=igsender and igstatus in(0,1) and igreceiver = '"
				+ userid + "' and igid>" + maxIGid + " and igstatus in (0,1)  group by igid asc";
		String SqlForMessage = "select mid,unickname,upic,efirstpic,meid,mcontent,type,mtime,mstatus from users,messages,events where msender = userid and mstatus in(0,1) and meid = eid and mreceiver='"
				+ userid + "' and mid > " + maxMid + " and mstatus in(0,1) group by mid desc ";
		// 获取评论消息列表
		List<Record> messageList = Db.find(SqlForMessage);
		// 将评论消息列表中的元素的状态改为已读
		for (Record record : messageList) {
			Db.update(" update messages set mstatus=1 where mid=" + record.get("mid").toString() + " ");
		}
		// 获取邀请进组消息列表
		List<Record> inviteList = Db.find(sqlForInvite);
		List<Record> list = dataProcess.combineTwoList(inviteList, messageList);
		return list;
	}

	/**
	 * 插入信息到等待表中
	 * 
	 * @param userid
	 * @param groupid
	 * @param messageInfo
	 * @param messagePhone
	 * @return
	 */
	@Before(Tx.class)
	public boolean insertInfoIntoWaits(String userid, String groupid, Record messageInfo, String messagePhone) {
		boolean flag = true;
		// 需要通知的电话号码拆分成数组
		String[] phoneArray = messagePhone.split(",");
		// 构造短信内容
		String content = "邀请你进入" + messageInfo.getStr("type").toString() + "“"
				+ messageInfo.getStr("groupName").toString() + "”";
		// 逐条插入wait表中
		for (int i = 0; i < phoneArray.length; i++) {
			Wait wait = new Wait().set("wsender", userid).set("wphone", phoneArray[i]).set("wcontent", content)
					.set("wgroupid", groupid);
			// 任意一条失败，则结果失败
			if (!wait.save()) {
				flag = false;
			}
		}
		return flag;
	}

	/**
	 * 发送通知给被邀请者
	 * 
	 * @param phonenumber
	 * @param userid
	 * @return
	 */
	public boolean sendInviteToWaitPerson(String phonenumber, String userid) {
		boolean flag = true;
		List<Record> list = Db
				.find(" select wid,wsender,wcontent,wgroupid from waits where wphone =" + phonenumber + " ");
		for (Record record : list) {
			Db.update("update waits set wstatus = 1 where wid=" + record.get("wid").toString() + " ");
			InviteGroup inviteRecord = new InviteGroup().set("igsender", record.get("wsender").toString())
					.set("igreceiver", userid).set("igcontent", record.get("wcontent").toString())
					.set("iggroupid", record.get("wgroupid").toString());
			if (!inviteRecord.save()) {
				flag = false;
			}
		}
		return flag;
	}

	/**
	 * 发送系统消息
	 * 
	 * @param content
	 * @return
	 */
	@Before(Tx.class)
	public boolean sendSystemNotification(String content) {
		boolean flag = true;
		List<Record> list = Db.find("select userid from users");
		for (Record record : list) {
			Notification notification = new Notification().set("nsender", 10)
					.set("nreceiver", Integer.parseInt(record.get("userid").toString())).set("ncontent", content)
					.set("ngroupid", 0).set("ntype", 0);
			if (!notification.save()) {
				flag = false;
			}
		}
		return flag;
	}

	/**
	 * 修改组头像
	 * 
	 * @param url
	 * @param groupID
	 * @return
	 */
	public boolean modifyGroupPic(String url, String groupID) {
		int count = Db.update("update groups set gpic='" + url + "' where groupid=" + groupID + " ");
		return count == 1;
	}

	/**
	 * 判断电话号码的用户是否已在组内
	 * 
	 * @param notifyPhone
	 * @return
	 */
	public String judgeUserInGroup(String groupid, String notifyPhone) {
		// 拆分字符串
		String[] phoneArray = notifyPhone.split(",");
		String notInGroupPhone = ""; // 不在组内的成员的电话
		// 获取组内成员的ID
		List<Record> list = Db
				.find("select uphone from users,groupmembers where gmuserid = userid and gmgroupid = " + groupid + " ");
		for (int i = 0; i < phoneArray.length; i++) {
			boolean flag = true;
			for (Record record : list) {
				String phone = record.get("uphone").toString();
				if ((phoneArray[i]).equals(phone)) {
					flag = false;
					break;
				}
			}
			if (flag) {
				notInGroupPhone += (phoneArray[i] + ",");
			}
		}
		// 字符串处理
		if (!notInGroupPhone.equals("")) {
			notInGroupPhone = notInGroupPhone.substring(0, notInGroupPhone.length() - 1);
		}
		return notInGroupPhone;
	}

	/**
	 * 删除评论
	 * 
	 * @param commentID
	 * @param userid
	 * @return
	 */
	public boolean deleteComment(String commentID) {
		int count = Db.update("update comments set cstatus=1 where cid=" + commentID + " ");
		return count == 1;
	}

	/**
	 * 退出相册
	 * 
	 * @param userid
	 * @param groupid
	 * @return
	 */
	@Before(Tx.class)
	public boolean quitAlbum(String userid, String groupid, String source) {

		// 推送列表
		List<Record> pushList = new ArrayList<Record>();

		// 1.直接将组员信息删除
		// 获取该组员在成员表中的编号
		List<Record> userRecord = Db
				.find("select gmid from groupmembers where gmuserid=" + userid + " and gmgroupid=" + groupid + " ");

		int gmid = userRecord.size() == 0 ? 0 : Integer.parseInt(userRecord.get(0).get("gmid").toString());
		if (gmid == 0)
			return true;

		GroupMember gm = new GroupMember().set("gmid", gmid).set("gmuserid", Integer.parseInt(userid)).set("gmgroupid",
				Integer.parseInt(groupid));

		boolean deleteFlag = gm.delete();

		// 2.修改组成员数量，减一
		int count = Db.update("update groups set gnum=gnum-1 where groupid=" + groupid + " ");

		// 3.将改组员发布的动态的相关数据隐藏
		boolean eventFlag = true;
		List<Record> eventList = Db.find(
				"select eid from events where egroupid=" + groupid + " and euserid=" + userid + " and estatus=0 ");
		for (Record record : eventList) {
			if (deleteEvent((record.get("eid").toString()))) {
				eventFlag = true;
			} else {
				eventFlag = false;
				break;
			}
		}

		boolean notificationFlag = true;
		if (source == null || !source.equals("smallApp")) {
			// 4.给组内其他成员发送通知,type为10.11时不需要
			Group group = new Group().findById(groupid);
			int gtype = Integer.parseInt(group.get("gtype").toString());
			if (gtype != 10 && gtype != 11) {
				// 获取需要发送通知的所有用户的ID
				List<Record> UserID = getGroupMemberID(groupid);
				// 获取通知的相关内容
				Record contentRecord = Db.findFirst("select gtype,gname from groups where groupid=" + groupid + " ");
				// 获取通知内容
				String content = dataProcess.getNotificationContent(contentRecord, "quit");

				// 逐条通知插入
				for (Record record : UserID) {
					int receiverID = Integer.parseInt(record.get("gmuserid").toString());
					Notification notification = new Notification().set("nsender", userid).set("nreceiver", receiverID)
							.set("ncontent", content).set("ntype", 3).set("ngroupid", groupid);
					if (notification.save()) {
						notificationFlag = true;
						// 通知存储成功的同时,构造透传内容并进行推送
						String nid = notification.get("nid").toString();
						Record pushRecord = pushMessage.getPushRecord(userid, record.get("gmuserid").toString(),
								groupid, content, nid, "notification", null);
						if (!((pushRecord.toJson()).equals("{}"))) {
							// Record不为空时，加入到list中
							pushList.add(pushRecord);
						}
					} else {
						notificationFlag = false;
						break;
					}
				}
			}
		}
		// 5.删除情侣时光机中的数据
		Db.update("update lovertimemachine set ltmStatus=1 where ltmGroupID=" + groupid + " and ltmUserID=" + userid
				+ " ");
		// // 6.用户退出IM聊天群组
		// im.RemoveChatGroupMember(userid, groupid);

		if (deleteFlag && eventFlag && notificationFlag && count == 1) {
			// 成功进行推送
			push.yinianPushToSingle(pushList);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 退出官方相册
	 * 
	 * @param userid
	 * @param groupid
	 */
	@Before(Tx.class)
	public boolean quitOfficialAlbum(String userid, String groupid) {

		// 推送列表
		List<Record> pushList = new ArrayList<Record>();

		// 1.直接将组员信息删除
		// 获取该组员在成员表中的编号
		Record userRecord = Db.findFirst(
				"select gmid from groupmembers where gmuserid=" + userid + " and gmgroupid=" + groupid + " ");
		int gmid = Integer.parseInt(userRecord.get("gmid").toString());
		GroupMember gm = new GroupMember().set("gmid", gmid).set("gmuserid", Integer.parseInt(userid)).set("gmgroupid",
				Integer.parseInt(groupid));
		boolean deleteFlag = gm.delete();
		// 2.修改组成员数量，减一
		int count = Db.update("update groups set gnum=gnum-1 where groupid=" + groupid + " ");
		// 3.将改组员发布的动态的相关数据隐藏
		boolean eventFlag = true;
		List<Record> eventList = Db.find(
				"select eid from events where egroupid=" + groupid + " and euserid=" + userid + " and estatus=0 ");
		for (Record record : eventList) {
			if (deleteEvent((record.get("eid").toString()))) {
				eventFlag = true;
			} else {
				eventFlag = false;
				break;
			}
		}
		// 4.删除likes表中的数据
		boolean deleteLikes = true;
		// 获取组内改组内所有的动态ID
		List<Record> eventsList = Db.find("select eid from events where egroupid=" + groupid + " and estatus in (0,3)");
		for (Record eventRecord : eventsList) {
			Record record = Db.findFirst("select lid from likes where leid=" + eventRecord.get("eid").toString()
					+ " and luserid=" + userid + " ");
			Likes like = new Likes().set("lid", record.get("lid").toString());
			deleteLikes = like.delete();
		}

		// 5.给管理员发送通知并进行推送
		boolean notificationFlag = true;
		// 获取通知的相关内容
		Record contentRecord = Db.findFirst("select gtype,gname from groups where groupid=" + groupid + " ");
		// 获取通知内容
		String content = dataProcess.getNotificationContent(contentRecord, "kick");

		// 通知插入
		Notification notification = new Notification().set("nsender", userid).set("nreceiver", CommonParam.superUserID)
				.set("ncontent", content).set("ntype", 3).set("ngroupid", groupid);
		if (notification.save()) {
			notificationFlag = true;
			// 通知存储成功的同时,构造透传内容并进行推送
			String nid = notification.get("nid").toString();
			Record pushRecord = pushMessage.getPushRecord(userid, CommonParam.superUserID, groupid, content, nid,
					"notification", null);
			if (!((pushRecord.toJson()).equals("{}"))) {
				// Record不为空时，加入到list中
				pushList.add(pushRecord);
			}
		} else {
			notificationFlag = false;
		}
		if (deleteFlag && eventFlag && notificationFlag && count == 1 && deleteLikes) {
			// 成功进行推送
			push.yinianPushToSingle(pushList);
			return true;
		} else {
			return false;
		}

	}

	/**
	 * 踢出相册
	 * 
	 * @param userid
	 * @param groupid
	 */
	@Before(Tx.class)
	public boolean kickOutAlbum(String userid, String groupid) {

		// 推送列表
		List<Record> pushList = new ArrayList<Record>();

		// 1.直接将组员信息删除
		// 获取该组员在成员表中的编号
		Record userRecord = Db.findFirst(
				"select gmid from groupmembers where gmuserid=" + userid + " and gmgroupid=" + groupid + " ");
		int gmid = Integer.parseInt(userRecord.get("gmid").toString());
		GroupMember gm = new GroupMember().set("gmid", gmid).set("gmuserid", Integer.parseInt(userid)).set("gmgroupid",
				Integer.parseInt(groupid));

		boolean deleteFlag = gm.delete();

		// 2.修改组成员数量，减一
		int count = Db.update("update groups set gnum=gnum-1 where groupid=" + groupid + " ");

		// 3.将改组员发布的动态的相关数据隐藏
		boolean eventFlag = true;
		List<Record> eventList = Db.find(
				"select eid from events where egroupid=" + groupid + " and euserid=" + userid + " and estatus=0 ");
		for (Record record : eventList) {
			if (deleteEvent((record.get("eid").toString()))) {
				eventFlag = true;
			} else {
				eventFlag = false;
				break;
			}
		}

		// 4.给被踢出者发送通知并进行推送
		boolean notificationFlag = true;
		// 获取通知的相关内容
		Record contentRecord = Db.findFirst("select gtype,gname from groups where groupid=" + groupid + " ");
		// 获取通知内容
		String content = dataProcess.getNotificationContent(contentRecord, "kick");

		// 通知插入
		Notification notification = new Notification().set("nsender", CommonParam.systemUserID).set("nreceiver", userid)
				.set("ncontent", content).set("ntype", 4).set("ngroupid", groupid);
		if (notification.save()) {
			notificationFlag = true;
			// 通知存储成功的同时,构造透传内容并进行推送
			String nid = notification.get("nid").toString();
			Record pushRecord = pushMessage.getPushRecord(CommonParam.systemUserID, userid, groupid, content, nid,
					"notification", null);
			if (!((pushRecord.toJson()).equals("{}"))) {
				// Record不为空时，加入到list中
				pushList.add(pushRecord);
			}
		} else {
			notificationFlag = false;
		}
		if (deleteFlag && eventFlag && notificationFlag && count == 1) {
			// 成功进行推送
			push.yinianPushToSingle(pushList);
			return true;
		} else {
			return false;
		}

	}

	/**
	 * 修改用户背景图
	 * 
	 * @param userid
	 * @param url
	 * @return
	 */
	public boolean modifyUserBackground(String userid, String url) {
		int count = Db.update("update users set ubackground='" + url + "' where userid=" + userid + " ");
		return count == 1;
	}

	/**
	 * 获取有新动态的组的数量
	 * 
	 * @param userid
	 * @return
	 */
	public List<Record> getTheNumberOfGroupsWithNewEvent(String userid) {
		List<Record> list = Db.find(
				"select count(*) as number,GROUP_CONCAT(gmgroupid SEPARATOR \",\" ) as groupid from groupmembers,groups where groupid=gmgroupid and gmuserid = "
						+ userid + " and gmnotify =1 and gmstatus = 0 and gtype !=5 ");
		return list;
	}

	/**
	 * 更新用户的cid
	 * 
	 * @param userid
	 * @param cid
	 * @param type
	 * @return
	 */
	public boolean updateUcid(String userid, String cid, String type, String device) {
		String sql = "";
		switch (type) {
		case "enter":
			sql = "update users set ucid='" + cid + "' , udevice='" + device + "' where userid=" + userid + " ";
			break;
		case "quit":
			sql = "update users set ucid='' where userid=" + userid + " ";
			break;
		}
		if (Db.update(sql) == 1) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 验证密码是否正确
	 * 
	 * @param userid
	 * @param oldPassword
	 * @return
	 */
	public boolean checkPassword(String userid, String oldPassword) {
		String password = dao.getUserSingleInfo(userid, "password");
		String encodePassword = YinianUtils.EncoderByMd5(oldPassword);
		if (password.equals(encodePassword)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 获取组内的所有照片
	 * 
	 * @param groupid
	 * @return
	 */
	public List<Record> getGroupPhotos(String groupid, String type, int id) {
		String sql = "";
		switch (type) {
		case "initialize":
			sql = "select eid,euserid,pid,poriginal as url from events,pictures where peid=eid and egroupid=" + groupid
					+ " and estatus in(0,3) and pstatus=0 ORDER BY pid DESC limit 30";
			break;
		case "loading":
			sql = "select eid,euserid,pid,poriginal as url from events,pictures where peid=eid and egroupid=" + groupid
					+ " and pid<" + id + " and estatus in(0,3) and pstatus=0 ORDER BY pid DESC limit 30";
			break;
		case "refresh":
			sql = "select eid,euserid,pid,poriginal as url from events,pictures where peid=eid and egroupid=" + groupid
					+ " and pid>" + id + " and estatus in(0,3) and pstatus=0 ORDER BY pid DESC";
			break;
		}
		List<Record> list = Db.find(sql);
		list = dataProcess.GetOriginAndThumbnailAccess(list, "url");
		return list;
	}

	/**
	 * 按日、月获取照片墙数据
	 * 
	 * @param groupid
	 * @param type
	 * @param date
	 * @param mode
	 * @param source
	 * @return
	 */
	public List<Record> getPhotoWallByDayOrMonth(String groupid, String type, String date, String mode, String source) {

		if (mode.equals("day")) {
			mode = "\"%Y-%m-%d\"";
		} else {
			mode = "\"%Y-%m\"";
		}

		List<Record> list = new ArrayList<Record>();
		String sql = "";

		switch (type) {
		case "initialize":
			sql = "select GROUP_CONCAT(eid SEPARATOR \",\") as eid,DATE_FORMAT( euploadtime, " + mode
					+ " ) as euploadtime from events where eMain not in (4,5) and estatus =0 and egroupid="
					+ groupid + " group by DATE_FORMAT( euploadtime, " + mode
					+ " ) desc limit 10";
			break;
		case "loading":
			sql = "select GROUP_CONCAT(eid SEPARATOR \",\") as eid,DATE_FORMAT( euploadtime, " + mode
					+ " ) as euploadtime from events where eMain not in (4,5) and estatus =0 and DATE_FORMAT( euploadtime, "
					+ mode + " )<'" + date + "' and egroupid=" + groupid + " group by DATE_FORMAT( euploadtime, " + mode + " ) desc limit 10";
			break;
		case "refresh":
			sql = "select GROUP_CONCAT(eid SEPARATOR \",\") as eid,DATE_FORMAT( euploadtime, " + mode
					+ " ) as euploadtime from events where eMain not in (4,5) and estatus =0 and DATE_FORMAT( euploadtime, "
					+ mode + " )>'" + date + "' and egroupid=" + groupid + " group by DATE_FORMAT( euploadtime, " + mode + " ) desc";
			break;
		}
		list = Db.find(sql);
		// 按日期获取相应数据
		for (int i = 0; i < list.size(); i++) {
			String eids = list.get(i).get("eid").toString();
			List<Record> picList = Db.find(
					"select eid,euserid,pid,poriginal as url,eMain from events,pictures where peid=eid and eMain not in (4,5) and eid in("
							+ eids + ") and pstatus=0 ORDER BY pid DESC ");
			if (picList.size() != 0) {
				// 图片授权
				picList = dataProcess.GetOriginAndThumbnailAccessWithDirectCut(picList, "url");

				// 插入数据
				list.get(i).remove("eid");
				list.get(i).set("picture", picList);
			} else {

				list.remove(list.get(i));
				i--;
			}

		}

		return list;
	}

	/**
	 * 获取照片短视频墙
	 * 
	 * @param groupid
	 * @param type
	 * @param date
	 * @return
	 */
	public List<Record> getPhotoAndVideoWall(String groupid, String type, String date) {

		String mode = "\"%Y-%m-%d\"";
		List<Record> list = new ArrayList<Record>();
		String sql = "";

		switch (type) {
		case "initialize":
			sql = "select GROUP_CONCAT(eid SEPARATOR \",\") as eid,DATE_FORMAT( euploadtime, " + mode
					+ " ) as euploadtime from events where eMain in (0,4) and estatus in (0,3) and (egroupid=" + groupid
					+ " or eRecommendGroupID=" + groupid + " ) group by DATE_FORMAT( euploadtime, " + mode
					+ " ) desc limit 10";
			break;
		case "loading":
			sql = "select GROUP_CONCAT(eid SEPARATOR \",\") as eid,DATE_FORMAT( euploadtime, " + mode
					+ " ) as euploadtime from events where eMain in (0,4) and estatus in (0,3) and DATE_FORMAT( euploadtime, "
					+ mode + " )<'" + date + "' and (egroupid=" + groupid + " or eRecommendGroupID=" + groupid
					+ " ) group by DATE_FORMAT( euploadtime, " + mode + " ) desc limit 10";
			break;
		case "refresh":
			sql = "select GROUP_CONCAT(eid SEPARATOR \",\") as eid,DATE_FORMAT( euploadtime, " + mode
					+ " ) as euploadtime from events where eMain in (0,4) and estatus in (0,3) and DATE_FORMAT( euploadtime, "
					+ mode + " )>'" + date + "' and (egroupid=" + groupid + " or eRecommendGroupID=" + groupid
					+ " ) group by DATE_FORMAT( euploadtime, " + mode + " ) desc";
			break;
		}
		list = Db.find(sql);
		// 按日期获取相应数据
		for (int i = 0; i < list.size(); i++) {
			//byte[] eidsByte=list.get(i).getBytes("eid");
			String eids = list.get(i).get("eid").toString();
			if(eids.substring(eids.length()-1, eids.length()).equals(",")){
				eids=eids.substring(0, eids.length()-1);
			}
			System.out.println("sql="+"select eid,euserid,pid,pcover,eMain,poriginal as url from events,pictures where peid=eid and eMain in (0,4) and eid in("
							+ eids + ") and pstatus=0 ORDER BY pid DESC ");
			List<Record> picList = Db.find(
					"select eid,euserid,pid,pcover,eMain,poriginal as url from events,pictures where peid=eid and eMain in (0,4) and eid in("
							+ eids + ") and pstatus=0 ORDER BY pid DESC ");
			System.out.println(picList.size());
			if (picList.size() != 0) {
				// 图片授权
				picList = dataProcess.GetOriginAndThumbnailAccessWithDirectCut(picList, "url");
				
				// 插入数据
				list.get(i).remove("eid");
				list.get(i).set("picture", picList);
			} else {

				list.remove(list.get(i));
				i--;
			}

		}

		return list;
	}
	
	/**
	 * 获取照片短视频墙
	 * 
	 * @param groupid
	 * @param type
	 * @param date
	 * @return
	 */
	public List<Record> getPhotoAndVideoWallNew(String groupid, String type, String date) {

		String mode = "\"%Y-%m-%d\"";
		List<Record> list = new ArrayList<Record>();
		String sql = "";

		switch (type) {
		case "initialize":
			sql = "select GROUP_CONCAT(eid SEPARATOR \",\") as eid,DATE_FORMAT( euploadtime, " + mode
					+ " ) as euploadtime from events where eMain in (0,4) and estatus in (0,3) and (egroupid=" + groupid
					+ " or eRecommendGroupID=" + groupid + " ) group by DATE_FORMAT( euploadtime, " + mode
					+ " ) desc limit 10";
			break;
		case "loading":
			sql = "select GROUP_CONCAT(eid SEPARATOR \",\") as eid,DATE_FORMAT( euploadtime, " + mode
					+ " ) as euploadtime from events where eMain in (0,4) and estatus in (0,3) and DATE_FORMAT( euploadtime, "
					+ mode + " )<'" + date + "' and (egroupid=" + groupid + " or eRecommendGroupID=" + groupid
					+ " ) group by DATE_FORMAT( euploadtime, " + mode + " ) desc limit 10";
			break;
		case "refresh":
			sql = "select GROUP_CONCAT(eid SEPARATOR \",\") as eid,DATE_FORMAT( euploadtime, " + mode
					+ " ) as euploadtime from events where eMain in (0,4) and estatus in (0,3) and DATE_FORMAT( euploadtime, "
					+ mode + " )>'" + date + "' and (egroupid=" + groupid + " or eRecommendGroupID=" + groupid
					+ " ) group by DATE_FORMAT( euploadtime, " + mode + " ) desc";
			break;
		}
		list = Db.find(sql);
		if(list.size()!=0) {
			
		}
		// 按日期获取相应数据
		for (int i = 0; i < list.size(); i++) {
			//byte[] eidsByte=list.get(i).getBytes("eid");
			String eids = list.get(i).get("eid").toString();
			if(eids.substring(eids.length()-1, eids.length()).equals(",")){
				eids=eids.substring(0, eids.length()-1);
			}
			System.out.println("sql="+"select eid,euserid,pid,pcover,eMain,poriginal as url from events,pictures where peid=eid and eMain in (0,4) and eid in("
							+ eids + ") and pstatus=0 ORDER BY pid DESC ");
			List<Record> picList = Db.find(
					"select eid,euserid,pid,pcover,eMain,poriginal as url from events,pictures where peid=eid and eMain in (0,4) and eid in("
							+ eids + ") and pstatus=0 ORDER BY pid DESC ");
			System.out.println(picList.size());
			list.get(i).set("num", picList.size());
			// 资源授权
			if(picList.size()!=0) {
				if(picList.size()>=99) {
					for(int j=99;j<picList.size();j++) {
						picList.remove(j);
						j--;
					}
				}
			picList = dataProcess.GetOriginAndThumbnailAccessWithDirectCutNew2(picList, "url");
			list.get(i).set("picture", picList);
			list.get(i).remove("eid");
				
			}else {
				list.remove(list.get(i));
				i--;
			}
			
			
		}
		return list;
	}
	
	/**
	 * 获取照片短视频墙
	 * 
	 * @param groupid
	 * @param type
	 * @param date
	 * @return
	 */
	public List<Record> getPhotoAndVideoWallByTime(String groupid, String date , int pagenum) {
		int page = (pagenum-1)*10;
		String mode = "\"%Y-%m-%d\"";
		List<Record> list = new ArrayList<Record>();
		String sql = "";
		sql = "select GROUP_CONCAT(eid SEPARATOR \",\") as eid,DATE_FORMAT( euploadtime, " + mode
				+ " ) as euploadtime from events where eMain in (0,4) and estatus=0"+" and euploadtime like "+"'"+date+"%'"+ " and (egroupid=" + groupid
				+ " or eRecommendGroupID=" + groupid + " ) group by DATE_FORMAT( euploadtime, " + mode
				+ " ) desc limit "+page+",10";

		list = Db.find(sql);
		if(list.size()!=0) {
			// 按日期获取相应数据
			for (int i = 0; i < list.size(); i++) {
				//byte[] eidsByte=list.get(i).getBytes("eid");
				String eids = list.get(i).get("eid").toString();
				if(eids.substring(eids.length()-1, eids.length()).equals(",")){
					eids=eids.substring(0, eids.length()-1);
				}
				System.out.println("sql="+"select eid,euserid,pid,pcover,eMain,poriginal as url from events,pictures where peid=eid and eMain in (0,4) and eid in("
								+ eids + ") and pstatus=0 ORDER BY pid DESC ");
				List<Record> picList = Db.find(
						"select eid,euserid,pid,pcover,eMain,poriginal as url from events,pictures where peid=eid and eMain in (0,4) and eid in("
								+ eids + ") and pstatus=0 ORDER BY pid DESC ");
				/*List<Record> picList = Db.find(
						"select peid as eid,puserid as euserid,pid,pcover,pMain as eMain,poriginal as url from pictures where peid in("
								+ eids + ") and pstatus=0 ORDER BY pid DESC ");*/
				list.get(i).set("num", picList.size());
				// 资源授权
				if(picList.size()!=0) {
					if(picList.size()>=99) {
						for(int j=99;j<picList.size();j++) {
							picList.remove(j);
							j--;
						}
					}
					picList = dataProcess.GetOriginAndThumbnailAccessWithDirectCutNew2(picList, "url");
					list.get(i).set("picture", picList);
					list.get(i).remove("eid");
				}else {
					list.remove(list.get(i));
					i--;
				}
			}
		}
		

		return list;
	}
	
	/**
	 * 获取全部照片短视频墙
	 * 
	 * @param groupid
	 * @param type
	 * @param date
	 * @return
	 */
	public List<Record> getPhotoAndVideoWallShowMore(String groupid,String uploadtime , int pagenum) {
		int page = (pagenum-1)*30;
		List<Record> list = new ArrayList<Record>();
		String mode = "\"%Y-%m-%d\"";
		String sql = "select pid,pcover,pMain,poriginal as url from pictures where pGroupid="+groupid+" and pstatus=0"+ " and puploadtime like "+"'"+uploadtime+"%'"+" order by puploadtime"+" limit "+page+",30";
		System.out.println(sql);
		list = Db.find(sql);
		if(list.size()!=0) {
			// 图片授权
			list = dataProcess.GetOriginAndThumbnailAccessWithDirectCutNew(list);
		}

		return list;
	}


	/**
	 * 申请进入组
	 * 
	 * @param userid
	 * @param inviteCode
	 * @return
	 */
	public int applyJoinGroup(String userid, String inviteCode) {

		// 返回标志
		int type; // 0--成功 1--邀请码不存在 2--组已删除 3--用户已经在组内 4--插入信息失败 5--官方相册
		// 推送列表
		List<Record> pushList = new ArrayList<Record>();

		// 通过邀请码获取相册的相关信息
		List<Record> list = Db
				.find("select groupid,gcreator,gtype,gname,gstatus from groups where ginvite='" + inviteCode + "'  ");
		// 判断邀请码是否存在
		if (list.size() == 0) {
			type = 1;
			return type;
		}
		Record groupRecord = list.get(0);
		// 判断组是否被删除
		if ((groupRecord.get("gstatus").toString()).equals("1")) {
			type = 2;
			return type;
		}
		// 判断申请者是否已经在组内
		boolean inGroupFlag = judgeUserInGroupByUserid(userid, groupRecord.get("groupid").toString());
		if (inGroupFlag) {
			type = 3;
			return type;
		}
		// 判断进入的相册类型， 如果是官方相册(gtype为5)，返回5
		if ((groupRecord.get("gtype").toString()).equals("5")) {
			type = 5;
			return type;
		}
		// 构造invitegroup内容
		String content = "申请加入私密空间“" + groupRecord.getStr("gname") + "”";
		// 插入通知消息
		InviteGroup invite = new InviteGroup().set("igsender", userid).set("igreceiver", groupRecord.get("gcreator"))
				.set("igcontent", content).set("iggroupid", groupRecord.get("groupid")).set("type", 2);
		if (invite.save()) {
			type = 0;
			// 获取ID
			String igid = invite.get("igid").toString();
			// 获取推送Record
			Record pushRecord = pushMessage.getPushRecord(userid, groupRecord.get("gcreator").toString(),
					groupRecord.get("groupid").toString(), content, igid, "applyIntoGroup", null);
			if (!((pushRecord.toJson()).equals("{}"))) {
				// Record不为空时，加入到list中
				pushList.add(pushRecord);
			}
		} else {
			type = 4;
			return type;
		}
		// 进行推送
		push.yinianPushToSingle(pushList);
		return type;
	}

	/**
	 * 判断用户是否在组内
	 * 
	 * @param userid
	 * @return
	 */
	public boolean judgeUserInGroupByUserid(String userid, String groupid) {
		List<Record> list = Db
				.find("select * from groupmembers where gmuserid = " + userid + " and gmgroupid =" + groupid + " ");
		if (list.size() == 0) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * 获取邀请码网站内容
	 * 
	 * @param value
	 * @return
	 */
	public List<Record> getWebsiteContent(String value) {
		List<Record> list = new ArrayList<Record>();
		Record record = new Record();
		String[] array = value.split(",");
		String groupid = "";
		String userid = "";
		for (int i = 0; i < array.length; i++) {
			if (((array[i].split("="))[0]).equals("groupid")) {
				groupid = (array[i].split("="))[1];
			}
			if (((array[i].split("="))[0]).equals("userid")) {
				userid = (array[i].split("="))[1];
			}
		}
		Record userRecord = Db.findFirst("select upic,unickname from users where userid=" + userid + "  ");
		List<Record> groupList = Db
				.find("select gname,gtype,ginvite,gpic from groups where gstatus=0 and groupid=" + groupid + " ");
		if (groupList.size() == 0) {
			return list;
		} else {
			List<Record> picList = Db.find("select pid,poriginal from pictures,events where eid=peid and egroupid="
					+ groupid + " and estatus=0 ORDER BY pid desc limit 1");
			if (picList.size() == 0) {
				userRecord.set("gfirstpic", CommonParam.defaultFirstPicOfGroup);
			} else {
				userRecord.set("gfirstpic", picList.get(0).get("poriginal").toString());
			}
			userRecord.set("gname", groupList.get(0).getStr("gname")).set("gtype", groupList.get(0).get("gtype"))
					.set("ginvite", groupList.get(0).getStr("ginvite"));
			list.add(userRecord);
			return list;
		}
	}

	/**
	 * 点赞
	 * 
	 * @param userid
	 * @param groupid
	 * @return
	 */
	@Before(Tx.class)
	public boolean likeEvent(String userid, String eventid) {
		int updateLikesFlag;
		int updateEventFlag;
		updateLikesFlag = Db
				.update("update likes set lislike=1 where leid=" + eventid + " and luserid=" + userid + " ");
		updateEventFlag = Db.update("update events set elike=elike+1 where eid=" + eventid + " ");
		return updateLikesFlag == 1 && updateEventFlag == 1;
	}

	/**
	 * 取消点赞
	 * 
	 * @param userid
	 * @param groupid
	 * @return
	 */
	@Before(Tx.class)
	public boolean unlikeEvent(String userid, String eventid) {
		int updateLikesFlag;
		int updateEventFlag;
		updateLikesFlag = Db
				.update("update likes set lislike=0 where leid=" + eventid + " and luserid=" + userid + " ");
		updateEventFlag = Db.update("update events set elike=elike-1 where eid=" + eventid + " ");
		return updateLikesFlag == 1 && updateEventFlag == 1;
	}

	/**
	 * 上传动态后插入数据到点赞表中
	 * 
	 * @param groupid
	 * @param list
	 * @return
	 */
	@Before(Tx.class)
	public boolean insertDataIntoLikes(String eventid, List<Record> list) {
		boolean flag = true;
		for (Record record : list) {
			Likes like = new Likes().set("leid", eventid).set("luserid", record.get("gmuserid").toString());
			if (!like.save()) {
				flag = false;
				break;
			}
		}
		return flag;
	}

	/**
	 * 用户进入官方相册时插入数据到点赞表中
	 * 
	 * @param userid
	 * @param groupid
	 * @return
	 */
	@Before(Tx.class)
	public boolean newUserJoinInsertLikes(String userid, String groupid) {
		boolean flag = true;
		// 获取组内所有的动态
		List<Record> list = Db.find("select eid from events where egroupid=" + groupid + " and estatus in(0,3) ");
		for (Record record : list) {
			Likes like = new Likes().set("leid", record.get("eid").toString()).set("luserid", userid);
			if (!like.save()) {
				flag = false;
				break;
			}
		}
		return flag;
	}

	/**
	 * 获取个人回忆信息
	 * 
	 * @throws ParseException
	 */
	public Record getPersonalMemory(String userid) throws ParseException {
		Record userRecord = Db.findFirst(
				"select unickname as uname,utime,count(*) as gnum from users,groupmembers where userid=gmuserid and userid="
						+ userid + " and gmstatus=0 ");
		// 获取当前的日期
		Date now = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		// 用户注册时间
		String registerDate = (userRecord.get("utime").toString()).substring(0, 10);
		Date register = sdf.parse(registerDate);
		long day = (now.getTime() - register.getTime()) / (24 * 60 * 60 * 1000) > 0
				? (now.getTime() - register.getTime()) / (24 * 60 * 60 * 1000)
				: (register.getTime() - now.getTime()) / (24 * 60 * 60 * 1000);
		userRecord.set("ucount", day);

		// 获取年月日
		Calendar ca = Calendar.getInstance();
		ca.setTime(register);
		int uyear = ca.get(Calendar.YEAR);
		int umonth = ca.get(Calendar.MONTH) + 1;
		int uday = ca.get(Calendar.DAY_OF_MONTH);
		userRecord.remove("utime");
		userRecord.set("uyear", uyear).set("umonth", umonth).set("uday", uday);
		return userRecord;
	}

	/**
	 * 插入反馈
	 * 
	 * @param userid
	 * @param content
	 * @param type
	 * @return
	 */
	public boolean insertFeedback(String userid, String content, String type, String machine, String system) {
		boolean flag;
		Feedback feedback = new Feedback().set("fuserid", userid).set("fcontent", content).set("ftype", type)
				.set("fmachine", machine).set("fsystem", system);
		if (feedback.save()) {
			flag = true;
		} else {
			flag = false;
		}
		return flag;
	}

	/**
	 * 清除所有消息（按评论、邀请、通知划分）
	 * 
	 * @param userid
	 * @param type
	 * @return
	 */
	@Before(Tx.class)
	public boolean clearAllMessage(String userid, String type) {
		boolean flag = false;
		switch (type) {
		case "invite":
			Db.update("update invitegroup set igstatus=2 where igreceiver=" + userid + " and igstatus in(0,1) ");
			flag = true;
			break;
		case "comment":
			Db.update("update messages set mstatus=2 where mreceiver=" + userid + " and mstatus in(0,1) ");
			flag = true;
			break;
		case "notification":
			Db.update("update notifications set nstatus=2 where nreceiver=" + userid + " and nstatus in(0,1)  ");
			flag = true;
			break;
		}
		return flag;
	}

	/**
	 * 删除单张照片
	 * 
	 * @param pid
	 * @return
	 */
	public boolean deleteSinglePhoto(String address, String eid) {
		int count = 0;
		// 判断是否为动态的最后一张照片，是的话把相应的动态删除
		List<Record> list = Db.find("select * from pictures where peid=" + eid + " and pstatus=0 ");

		if (eid == null || eid.equals("")) {
			count = Db.update("update pictures set pstatus=1 where poriginal='" + address + "'  ");

		} else {
			count = Db.update("update pictures set pstatus=1 where poriginal='" + address + "' and peid=" + eid + " ");
		}

		if (list.size() == 1) {
			deleteEvent(eid);
		}

		return (count == 1);
	}

	/**
	 * 通知并推送给单个人
	 * 
	 * @param sender
	 * @param receiver
	 * @param type
	 * @param groupid
	 * @param isPush
	 *            yes--推送 no--不推送
	 * @return
	 */
	@Before(Tx.class)
	public boolean notifyAndPushToSingle(String sender, String receiver, String type, String groupid, String isPush) {
		boolean notificationFlag = false;
		// 获取通知的相关内容
		Record contentRecord = Db.findFirst("select gtype,gname from groups where groupid=" + groupid + " ");
		// 推送列表
		List<Record> pushList = new ArrayList<Record>();
		// 通知内容
		String content = "";
		// 通知对象
		Notification notification = new Notification();
		switch (type) {
		case "contribute":
			// 获取通知内容
			content = dataProcess.getNotificationContent(contentRecord, "contribute");
			notification.set("nsender", sender).set("nreceiver", receiver).set("ncontent", content).set("ntype", 5)
					.set("ngroupid", groupid);

			break;
		case "acceptContribution":
			// 获取通知内容
			content = dataProcess.getNotificationContent(contentRecord, "acceptContribution");
			notification.set("nsender", sender).set("nreceiver", receiver).set("ncontent", content).set("ntype", 6)
					.set("ngroupid", groupid);
			break;
		case "refuseContribution":
			// 获取通知内容
			content = dataProcess.getNotificationContent(contentRecord, "refuseContribution");
			notification.set("nsender", sender).set("nreceiver", receiver).set("ncontent", content).set("ntype", 7)
					.set("ngroupid", groupid);
			break;
		default:
			return false;
		}
		if (notification.save()) {
			notificationFlag = true;
			// 判断是否进行推送
			if (isPush.equals("yes")) {
				String nid = notification.get("nid").toString();
				Record pushRecord = pushMessage.getPushRecord(sender, receiver, groupid, content, nid, "notification",
						null);
				if (!((pushRecord.toJson()).equals("{}"))) {
					// Record不为空时，加入到list中
					pushList.add(pushRecord);
				}
			}
		} else {
			notificationFlag = false;
			return false;
		}
		if (notificationFlag) {
			push.yinianPushToSingle(pushList);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 显示投稿界面
	 * 
	 * @param groupid
	 * @param type
	 * @param eventID
	 * @return
	 */
	public List<Record> getContributeContent(String groupid, String type, String eventID) {
		String sqlForContributeEvents = "";
		switch (type) {
		case "initialize":
			sqlForContributeEvents = "select eid,userid,unickname,upic,etext,eaudio,eaudiotime,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\" ) as url from users,events,pictures where userid=euserid and eid=peid and egroupid="
					+ groupid + " and estatus=2 and pstatus=0 group by peid DESC limit 10";
			break;
		case "loading":
			sqlForContributeEvents = "select eid,userid,unickname,upic,etext,eaudio,eaudiotime,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\" ) as url from users,events,pictures where userid=euserid and eid=peid and egroupid="
					+ groupid + " and eid < " + eventID + " and estatus=2 and pstatus=0 group by peid DESC limit 10";
			break;
		case "refresh":
			sqlForContributeEvents = "select eid,userid,unickname,upic,etext,eaudio,eaudiotime,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\" ) as url from users,events,pictures where userid=euserid and eid=peid and egroupid="
					+ groupid + " and eid > " + eventID + " and estatus=2 and pstatus=0 group by peid DESC";
			break;
		default:
			break;
		}
		List<Record> event = dataProcess.encapsulationEventList(Db.find(sqlForContributeEvents));// 获取事件的相关信息并封装里面的用户对象
		// 封装事件和标签
		event = dataProcess.combineEventWithTags(event);
		// 封装事件和卡片样式
		event = dataProcess.combineEventWithCardStyle(event);

		// 图片授权
		QiniuOperate qiniu = new QiniuOperate();
		for (Record record : event) {
			String[] array = record.get("url").toString().split(",");
			String newURL = "";
			for (int i = 0; i < array.length; i++) {
				newURL += qiniu.getDownloadToken(array[i]) + ",";
			}
			newURL = newURL.substring(0, newURL.length() - 1);
			record.set("url", newURL);
		}

		return event;
	}

	/**
	 * 审核投稿
	 * 
	 * @param eventID
	 * @param publishUserID
	 * @param type
	 * @return
	 */
	@Before(Tx.class)
	public String examineContributes(String groupid, String eventID, String publishUserID, String type) {
		int count;
		switch (type) {
		case "accept":
			// 动态的状态改为审核通过
			count = Db.update("update events set estatus=3 where eid=" + eventID + " ");
			// 给投稿者发送推送
			boolean flag1 = notifyAndPushToSingle(CommonParam.superUserID, publishUserID, "acceptContribution", groupid,
					"yes");
			// 审核通过后插入数据到likes表中
			// 获取组内所有成员的ID
			List<Record> memberList = getGroupMemberID(groupid);
			// 插入数据到likes表中
			boolean flag = insertDataIntoLikes(eventID, memberList);
			if (flag && flag1) {
				jsonString = getSingleEventInOfficialAlbum(CommonParam.superUserID, eventID);// 获取动态的信息
			} else {
				jsonString = jsonData.getJson(-50, "插入数据失败");
			}

			break;
		case "refuse":
			// 修改动态状态
			count = Db.update("update events set estatus=4 where eid=" + eventID + " ");
			// 给投稿者发送推送
			boolean flag2 = notifyAndPushToSingle(CommonParam.superUserID, publishUserID, "refuseContribution", groupid,
					"yes");
			if (flag2 && count == 1) {
				jsonString = jsonData.getJson(2001, "投稿审核未通过");
			} else {
				jsonString = jsonData.getJson(-50, "插入数据失败");
			}
			break;
		default:
			break;
		}
		return jsonString;
	}

	/**
	 * 获取首页banner信息
	 * 
	 * @param userid
	 * @return
	 */
	public List<Record> getMainBanner(String userid) {
		List<Record> bannerList = Db.find("select bid,btitle,bpic,bdata,btype from banner where bstatus=0  ");
		for (Record banner : bannerList) {
			String type = banner.get("btype").toString();
			if (type.equals("2")) {
				// 判断用户是否在该官方相册内，通过isInAlbum字段返回 0--不在 1--在
				String groupid = banner.get("bdata").toString();
				List<Record> user = Db.find(
						"select * from groupmembers where gmuserid=" + userid + " and gmgroupid=" + groupid + " ");
				if (user.size() == 0) {
					banner.set("isInAlbum", 0);
				} else {
					banner.set("isInAlbum", 1);
				}
			}
		}
		return bannerList;
	}

	/**
	 * 获取与用户相关的官方相册列表
	 * 
	 * @param userid
	 * @return
	 */
	public List<Record> getOfficialAlbumList(String userid) {

		// 获取官方相册信息
		List<Record> list = Db.find(
				"select groupid,gname,gpic,gnum,gtime,gtype,gcreator,ginvite,gintroduceText,gintroducePic from groups where gstatus=0 and gtype=5 and gname not in ("
						+ CommonParam.SchoolAlbumsName + ") order by gorder  ");
		// 获取组照片数
		List<Record> photoList = Db.find(
				"select groupid,count(*) as gpicnum from groups,events,pictures where peid=eid and groupid=egroupid and gstatus=0 and gtype=5 and estatus in(0,3) and pstatus=0 group by groupid");
		// 将组照片数插入到组信息中
		for (Record groupRecord : list) {
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
		// 将组类型转换成相应的文字
		list = dataProcess.changeGroupTypeIntoWord(list);
		// 判断用户是否在官方相册中 并返回isInAlbum字段 0--不在 1--在
		for (Record record : list) {
			String groupid = record.get("groupid").toString();
			List<Record> user = Db.find("select * from groupmembers where gmuserid=" + userid + " and gmgroupid="
					+ groupid + " and gmstatus=0 ");
			if (user.size() == 0) {
				record.set("isInAlbum", 0);
			} else {
				record.set("isInAlbum", 1);
			}
		}
		return list;
	}

	/**
	 * 获取与用户相关的官方相册列表
	 * 
	 * @param userid
	 * @return
	 */
	public List<Record> getSchoolAlbumList(String userid) {
		// 获取校园相册信息
		List<Record> list = Db.find(
				"select groupid,gname,gpic,gnum,gtime,gtype,gcreator,ginvite,gintroduceText,gintroducePic from groups where gstatus=0 and gtype=5 and gname in ("
						+ CommonParam.SchoolAlbumsName + ")  ");
		// 获取各个校园相册照片数
		List<Record> photoList = Db.find(
				"select groupid,count(*) as gpicnum from groups,events,pictures where peid=eid and groupid=egroupid and gstatus=0 and gtype=5 and estatus in(0,3) and pstatus=0 group by groupid");
		// 将组照片数插入到组信息中
		for (Record groupRecord : list) {
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
		// 将组类型转换成相应的文字
		list = dataProcess.changeGroupTypeIntoWord(list);
		// 判断用户是否在官方相册中 并返回isInAlbum字段 0--不在 1--在
		for (Record record : list) {
			String groupid = record.get("groupid").toString();
			List<Record> user = Db.find("select * from groupmembers where gmuserid=" + userid + " and gmgroupid="
					+ groupid + " and gmstatus=0 ");
			if (user.size() == 0) {
				record.set("isInAlbum", 0);
			} else {
				record.set("isInAlbum", 1);
			}
		}
		return list;
	}

	/**
	 * 获取单个官方相册的相册信息
	 * 
	 * @param groupid
	 * @return
	 */
	public List<Record> getSingleOfficialAlbumInfo(String userid, String groupid) {
		String sqlForGroupInfo = "select groupid,gimid,gname,gpic,gnum,gtime,gtype,gcreator,ginvite,gintroduceText,gintroducePic from groups where gstatus=0 and groupid="
				+ groupid + " ";
		String sqlForPhotos = "select groupid,count(*) as gpicnum from groups,events,pictures where peid=eid and groupid=egroupid and gstatus=0 and estatus=0 and pstatus=0 and groupid="
				+ groupid + " group by groupid";
		String sqlForJudge = "select * from groupmembers where gmuserid=" + userid + " and gmgroupid=" + groupid
				+ " and gmstatus=0 ";
		List<Record> list = Db.find(sqlForGroupInfo);
		if (list.size() == 0) {
			List<Record> temp = new ArrayList<Record>();
			return temp;
		} else {
			// 获取组照片数
			List<Record> photoList = Db.find(sqlForPhotos);
			// 判断用户是否在官方相册内
			List<Record> user = Db.find(sqlForJudge);
			if (user.size() == 0) {
				list.get(0).set("isInAlbum", 0);
			} else {
				list.get(0).set("isInAlbum", 1);
			}
			// 将组照片数插入到组信息中
			for (Record groupRecord : list) {
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
			// 将组类型转换成相应的文字
			list = dataProcess.changeGroupTypeIntoWord(list);
		}
		return list;
	}

	/**
	 * 初始化、刷新、加载用户未进入时的官方相册内的动态
	 * 
	 * @param userid
	 * @param groupid
	 * @param eventid
	 * @param sign
	 * @return
	 */
	public String getOfficialAlbumEventsWhenUserNotIn(String groupid, String eventid, String sign) {
		// 构造相应的SQL语句
		String sqlForEvent = "";
		String sqlForComment = "";
		switch (sign) {
		case "initialize":
			sqlForEvent = "select eid,egroupid,userid,unickname,upic,etext,elike,eaudio,eaudiotime,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\" ) as url from users,events,pictures where userid=euserid and eid=peid and egroupid="
					+ groupid + " and estatus in(0,3) and pstatus=0 group by peid DESC limit 10";
			sqlForComment = CommonParam.selectForComment
					+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and egroupid="
					+ groupid + " and cstatus=0 ORDER BY ceid,ctime asc";
			break;
		case "refresh":
			sqlForEvent = "select eid,egroupid,userid,unickname,upic,elike,etext,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\" ) as url from users,events,pictures where userid=euserid and eid=peid and egroupid="
					+ groupid + " and estatus in(0,3) and pstatus=0 and eid>" + eventid + " group by peid DESC";
			sqlForComment = CommonParam.selectForComment
					+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and eid>"
					+ eventid + " and egroupid=" + groupid + " and cstatus=0 ORDER BY ceid,ctime asc";
			break;
		case "loading":
			sqlForEvent = "select eid,egroupid,userid,unickname,upic,elike,etext,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\" ) as url from users,events,pictures where userid=euserid and eid=peid and egroupid="
					+ groupid + " and estatus in(0,3) and pstatus=0 and eid<" + eventid
					+ " group by peid DESC limit 10";
			sqlForComment = CommonParam.selectForComment
					+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and eid<"
					+ eventid + " and egroupid=" + groupid + " and cstatus=0 ORDER BY ceid,ctime asc";
			break;
		}
		List<Record> event = dataProcess.encapsulationEventList(Db.find(sqlForEvent));// 获取事件的相关信息并封装里面的用户对象
		if (event.size() != 0) {
			for (Record record : event) {
				record.set("lislike", 0);
			}
		}
		List<Record> comment = dataProcess.encapsulationCommentList(Db.find(sqlForComment));// 获取事件的评论信息并封装里面的用户对象
		List<Record> list = dataProcess.combieEventAndComment(event, comment);// 拼接事件与评论
		// 封装事件和标签
		list = dataProcess.combineEventWithTags(list);
		// 封装事件和卡片样式
		list = dataProcess.combineEventWithCardStyle(list);
		list = dataProcess.ChangePicAsArray(list);// 修改upic字段格式
		jsonString = jsonData.getJson(0, "success", list);
		return jsonString;
	}

	/**
	 * 判断用户微信或qq的ID
	 * 
	 * @param id
	 * @param type
	 * @return
	 */
	public List<Record> judgeUserQQorWechatID(String id, String type) {
		List<Record> list = new ArrayList<Record>();
		switch (type) {
		case "wechat":
			list = Db.find(
					"select userid,unickname,upic,ubackground,uLockPass from users where uwechatid='" + id + "' ");
			break;
		case "qq":
			list = Db.find("select userid,unickname,uLockPass from users where uqqid='" + id + "' ");
			break;
		}

		return list;
	}

	/**
	 * 绑定微信与手机
	 * 
	 * @param userid
	 * @param openid
	 * @return
	 */
	public boolean bindWechatAndPhonenumber(String userid, String data, String type) {
		boolean flag = false;
		int id = Integer.parseInt(userid);
		switch (type) {
		case "wechat":
			User user = User.dao.findById(id).set("uwechatid", data);
			if (user.update()) {
				flag = true;
			} else {
				flag = false;
			}
			break;
		case "phone":
			User user1 = User.dao.findById(id).set("uphone", data);
			if (user1.update()) {
				flag = true;
			} else {
				flag = false;
			}
			break;
		default:
			return false;
		}
		return flag;
	}

	/**
	 * 微信注册
	 * 
	 * @param id
	 * @param nickname
	 * @param pic
	 * @param sex
	 * @return
	 */
	public String wechatUserRegister(String id, String nickname, String pic, String sex, String source, String province,
			String city, String version, String port, String fromUserID, String fromSpaceID, String fromEventID,
			String openID) {
		SimpleDateFormat birthDf = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
		String birth = birthDf.format(new Date()); // 获取当前系统时间，设置当前日期为生日
		// 性别判断
		int newSex;
		if (sex.equals("1")) {
			newSex = 1;
		} else {
			newSex = 0;
		}
		// 头像判断
		if (pic == null || pic.equals("")) {
			//pic = CommonParam.yinianLogo;
			pic="http://7xlmtr.com1.z0.glb.clouddn.com/20180313_1.png";
		}
		// 默认背景的地址
		String defaultBackground = CommonParam.qiniuOpenAddress + CommonParam.userDefaultBackground;
		User user = new User().set("uwechatid", id).set("usex", newSex).set("unickname", nickname).set("ubirth", birth)
				.set("upic", pic).set("ubackground", defaultBackground).set("usource", source)
				.set("uprovince", province).set("ucity", city).set("uversion", version).set("uport", port)
				.set("uloginSource", source).set("uFromUserID", fromUserID).set("uFromSpaceID", fromSpaceID)
				.set("uFromEventID", fromEventID).set("uopenid", openID);

		String userid;
		// 用户可能已经注册成功，捕获数据库插入异常，返回userid
		try {
			user.save();
			userid = user.get("userid").toString();
			return userid;
		} catch (ActiveRecordException e) {
			// 搜索用户ID并返回
			e.printStackTrace();
			userid = User.QueryUserLoginBasicInfo(id, "uwechatid").get(0).get("userid").toString();
			return userid;
		}

	}

	/**
	 * QQ用户注册
	 * 
	 * @param id
	 * @param nickname
	 * @param pic
	 * @param sex
	 * @return
	 */
	public String qqUserRegister(String id, String nickname, String pic, String sex) {
		String userid = "";
		SimpleDateFormat birthDf = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
		String birth = birthDf.format(new Date()); // 获取当前系统时间，设置当前日期为生日
		int newSex;
		if (sex.equals("男")) {
			newSex = 1;
		} else {
			newSex = 0;
		}
		String defaultBackground = CommonParam.qiniuOpenAddress + CommonParam.userDefaultBackground;// 默认背景的地址
		User user = new User().set("uqqid", id).set("usex", newSex).set("unickname", nickname).set("ubirth", birth)
				.set("upic", pic).set("ubackground", defaultBackground);
		// 用户ID，注册成功则有相应的值，注册失败为空字符串
		if (user.save()) {
			userid = user.get("userid").toString();
			return userid;
		} else {
			return "";
		}

	}

	/**
	 * 获取网页相册内容
	 * 
	 * @param groupid
	 * @param gtype
	 * @return
	 */
	public List<Record> getWebAlbumContent(String groupid) {
		String sqlForEvent = "";
		String sqlForComment = "";
		String sqlForCommentNum = "select ceid,count(*) as num from events,comments where eid=ceid and egroupid="
				+ groupid + " and cstatus=0 and estatus=0 GROUP BY ceid";
		String sqlForGtype = "select gtype from groups where groupid=" + groupid + " ";
		Record record = Db.findFirst(sqlForGtype);
		String gtype = record.get("gtype").toString();
		List<Record> comment = new ArrayList<Record>();
		List<Record> event = new ArrayList<Record>();
		if (gtype.equals("5")) {
			sqlForEvent = "select eid,egroupid,userid,unickname,upic,etext,elike,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\" ) as url from users,events,pictures where userid=euserid and eid=peid and egroupid="
					+ groupid + " and estatus in(0,3) and pstatus=0 group by peid DESC limit 5";
			sqlForComment = CommonParam.selectForComment
					+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and egroupid="
					+ groupid + " and cstatus=0 ORDER BY ceid,ctime asc";
			comment = dataProcess.encapsulationCommentList(Db.find(sqlForComment));// 获取事件的评论信息并封装里面的用户对象
		} else {
			sqlForEvent = "select eid,egroupid,userid,unickname,upic,etext,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\" ) as url from users,events,pictures where userid=euserid and eid=peid and egroupid="
					+ groupid + " and estatus in(0,3) and pstatus=0 group by peid DESC limit 2";
		}
		event = dataProcess.encapsulationEventList(Db.find(sqlForEvent));// 获取事件的相关信息并封装里面的用户对象
		List<Record> commentNum = Db.find(sqlForCommentNum);
		for (Record eventRecord : event) {
			boolean flag = true;
			for (Record commentRecord : commentNum) {
				if ((eventRecord.get("eid").toString()).equals(commentRecord.get("ceid").toString())) {
					eventRecord.set("commentNum", Integer.parseInt(commentRecord.get("num").toString()));
					flag = false;
					break;
				}
			}
			if (flag) {
				eventRecord.set("commentNum", 0);
			}
		}
		List<Record> list = new ArrayList<Record>();
		if (gtype.equals("5")) {
			list = dataProcess.combieEventAndComment(event, comment);// 拼接事件与评论
		} else {
			list = event;
		}
		list = dataProcess.ChangePicAsArray(list);// 修改upic字段格式
		jsonString = jsonData.getJson(0, "success", list);
		return list;
	}

	// /**
	// * 获取网页相册内容
	// *
	// * @param groupid
	// * @param gtype
	// * @return
	// */
	// public List<Record> getWebAlbumContent(String groupid) {
	// String sqlForEvent = "";
	// String sqlForComment = "";
	// String sqlForCommentNum =
	// "select ceid,count(*) as num from events,comments where eid=ceid and
	// egroupid="
	// + groupid + " and cstatus=0 and estatus=0 GROUP BY ceid";
	// String sqlForGtype = "select gtype from groups where groupid="
	// + groupid + " ";
	// Record record = Db.findFirst(sqlForGtype);
	// String gtype = record.get("gtype").toString();
	// List<Record> comment = new ArrayList<Record>();
	// List<Record> event = new ArrayList<Record>();
	// if (gtype.equals("5")) {
	// sqlForEvent =
	// "select
	// eid,userid,unickname,upic,etext,elike,euploadtime,GROUP_CONCAT(poriginal
	// SEPARATOR \",\" ) as url from users,events,pictures where userid=euserid and
	// eid=peid and egroupid="
	// + groupid
	// + " and estatus in(0,3) and pstatus=0 group by peid DESC limit 5";
	// sqlForComment = CommonParam.selectForComment
	// +
	// " from users A,users B,comments,events where ceduserid=B.userid and
	// A.userid=cuserid and eid=ceid and egroupid="
	// + groupid + " and cstatus=0 ORDER BY ceid,ctime asc";
	// comment = dataProcess.encapsulationCommentList(Db
	// .find(sqlForComment));// 获取事件的评论信息并封装里面的用户对象
	// } else {
	// sqlForEvent =
	// "select eid,userid,unickname,upic,etext,euploadtime,GROUP_CONCAT(poriginal
	// SEPARATOR \",\" ) as url from users,events,pictures where userid=euserid and
	// eid=peid and egroupid="
	// + groupid
	// + " and estatus in(0,3) and pstatus=0 group by peid DESC limit 2";
	// }
	// event = dataProcess.encapsulationEventList(Db.find(sqlForEvent));//
	// 获取事件的相关信息并封装里面的用户对象
	// List<Record> commentNum = Db.find(sqlForCommentNum);
	// for (Record eventRecord : event) {
	// boolean flag = true;
	// for (Record commentRecord : commentNum) {
	// if ((eventRecord.get("eid").toString()).equals(commentRecord
	// .get("ceid").toString())) {
	// eventRecord.set("commentNum", Integer
	// .parseInt(commentRecord.get("num").toString()));
	// flag = false;
	// break;
	// }
	// }
	// if (flag) {
	// eventRecord.set("commentNum", 0);
	// }
	// }
	// List<Record> list = new ArrayList<Record>();
	// if (gtype.equals("5")) {
	// list = dataProcess.combieEventAndComment(event, comment);// 拼接事件与评论
	// } else {
	// list = event;
	// }
	// list = dataProcess.ChangePicAsArray(list);// 修改upic字段格式
	// jsonString = jsonData.getJson(0, "success", list);
	// return list;
	// }

	/**
	 * 插入统计数据
	 * 
	 * @param userid
	 * @param type
	 * @param data
	 * @return
	 */
	public boolean insertStatisticsInfo(String userid, String type, String data) {
		boolean flag = false;
		switch (type) {
		case "":
			break;
		default:
			return false;
		}
		return flag;
	}

	/**
	 * 获取项目中的公共信息
	 * 
	 * @param type
	 * @return
	 */
	public List<Record> getProjectPublicInfo(String type, String data) {
		List<Record> info = new ArrayList<Record>();
		switch (type) {
		case "theme":
			if (data.equals("Android")) {
				info = Db.find("select tid,tname,turl,tpic from themes where tstatus=0 and tsystem='Android' ");
			} else {
				if (data.equals("iOS")) {
					info = Db.find("select tid,tname,turl,tpic from themes where tstatus=0 and tsystem='iOS'  ");
				}
			}

			break;
		case "cardStyle":
			info = Db.find("select csid,csname,cspic,csurl from cardstyle where csstatus=0");
		case "albumCover":
			// int gtype = dataProcess.changeGroupTypeWordIntoNumber(data);
			info = Db.find("select acid,acurl from albumcover where acstatus=0 ");
			break;
		default:
			info = null;
			break;
		}

		return info;
	}

	/**
	 * 相册排序
	 */
	@Before(Tx.class)
	public boolean sortAlbumSequence(String userid, List<String> list) {
		boolean flag = true;
		int count = 1;
		for (String sort : list) {
			GroupMember gm = new GroupMember().findFirst("select * from groupmembers where gmuserid=" + userid
					+ " and gmgroupid=" + sort + " and gmstatus=0 ").set("gmorder", count);
			if (gm.update()) {
				count++;
			} else {
				flag = false;
				break;
			}
		}
		return flag;
	}

	/**
	 * 获取音乐相册
	 * 
	 * @param groupid
	 */
	public List<Record> getMusicAlbums(String groupid, String maID, String type) {
		String sqlForMusicAlbums = "";
		switch (type) {
		case "initialize":
			sqlForMusicAlbums = "select maID,maTitle,maName,maCreatorID,maContent,maCover,maTime,musicUrl,templetUrl,unickname,upic,GROUP_CONCAT(mapOriginal SEPARATOR \",\") as pictureUrl from users,music,templet,mapicture,musicalbum where userid=maCreatorID and musicID=maMusicID and templetID=maTempletID and mapMusicAlbumID=maID and maGroupID="
					+ groupid + " and maStatus=0 group by maID desc limit 10";
			break;
		case "refresh":
			sqlForMusicAlbums = "select maID,maTitle,maName,maCreatorID,maContent,maCover,maTime,musicUrl,templetUrl,unickname,upic,GROUP_CONCAT(mapOriginal SEPARATOR \",\") as pictureUrl from users,music,templet,mapicture,musicalbum where userid=maCreatorID and musicID=maMusicID and templetID=maTempletID and mapMusicAlbumID=maID and maGroupID="
					+ groupid + " and maID>" + maID + " and maStatus=0 group by maID desc";
			break;
		case "loading":
			sqlForMusicAlbums = "select maID,maTitle,maName,maCreatorID,maContent,maCover,maTime,musicUrl,templetUrl,unickname,upic,GROUP_CONCAT(mapOriginal SEPARATOR \",\") as pictureUrl from users,music,templet,mapicture,musicalbum where userid=maCreatorID and musicID=maMusicID and templetID=maTempletID and mapMusicAlbumID=maID and maGroupID="
					+ groupid + " and maID<" + maID + " and maStatus=0 group by maID desc limit 10";
			break;
		}
		List<Record> list = Db.find(sqlForMusicAlbums);
		list = dataProcess.changeUserInfoIntoObeject(list);
		return list;
	}

	/**
	 * 获取模板信息
	 * 
	 * @return
	 */
	public List<Record> getTempletInfo() {
		List<Record> list = Db.find(
				"select templetID,templetName,templetPic,templetUrl,templetProportion,templetDefaultMusicID,musicUrl as templetDefaultMusicUrl from templet,music where templetDefaultMusicID=musicID and templetStatus=0");
		return list;
	}

	/**
	 * 获取音乐信息
	 * 
	 * @return
	 */
	public List<Record> getMusicInfo() {
		List<Record> list = Db.find("select musicID,musicName,musicTempletID,musicUrl from music where musicStatus=0 ");
		return list;
	}

	/**
	 * 创建音乐相册
	 * 
	 * @param userid
	 * @param groupid
	 * @param albumName
	 * @param musicid
	 * @param templetid
	 * @param picAddress
	 * @return
	 */
	@Before(Tx.class)
	public String createMusicAlbum(String userid, String groupid, String albumName, String musicid, String templetid,
			String content, String picAddress) {

		String maID = "";

		// 将地址字符串转换成数组
		String[] picArray = dataProcess.getPicAddress(picAddress, "private");

		// 获取动态第一张图片地址
		String firstPic = picArray[0];

		// 保存音乐相册
		MusicAlbum musicAlbum = new MusicAlbum();
		musicAlbum.set("maName", albumName).set("maCreatorID", userid).set("maContent", content)
				.set("maGroupID", groupid).set("maMusicID", musicid).set("maTempletID", templetid)
				.set("maCover", firstPic);
		if (musicAlbum.save()) {
			maID = musicAlbum.get("maID").toString();
			// 将事件所属组的状态改为有新状态
			Db.update("update groupmembers set gmnotify=1 where gmgroupid =" + groupid + " ");
			// 发布者的状态改为无新状态，因为就算别人发了新动态，发布者发布完后还在组内动态界面
			Db.update("update groupmembers set gmnotify=0 where gmgroupid =" + groupid + " and gmuserid = " + userid
					+ " ");

			// 保存图片
			for (int i = 0; i < picArray.length; i++) {
				MAPicture maPic = new MAPicture();
				maPic.set("mapOriginal", picArray[i]).set("mapMusicAlbumID", maID);
				maPic.save();
			}
		}

		// 进行推送准备
		// 获取组内所有用户的ID
		List<Record> useridList = Db.find("select gmuserid from groupmembers where gmgroupid = " + groupid + " ");
		// 去掉发送者的ID
		for (Record record : useridList) {
			if ((record.get("gmuserid").toString()).equals(userid)) {
				useridList.remove(record);
				break;
			}
		}
		// useridList不为null时才进行推送
		if (!(useridList.isEmpty())) {
			// 将list中的ID拼接成字符串
			String userids = dataProcess.changeListToString(useridList, "gmuserid");
			// userids不为空字符串时才进行推送
			if (!(userids.equals(""))) {
				// 获取要进行推送的用户的cid
				Record cid = pushMessage.getUsersCids(userids, "userid");
				// 获取动态发布者的昵称
				String nickname = dao.getUserSingleInfo(userid, "nickname");
				// 获取发布动态所在组的信息
				List<Record> groupList = Db.find("select gname,gtype from groups where groupid = " + groupid + " ");
				groupList = dataProcess.changeGroupTypeIntoWord(groupList);
				// 拼接推送内容
				String pushContent = nickname + "在" + groupList.get(0).getStr("gtype") + "“"
						+ groupList.get(0).getStr("gname") + "”发布了新的音乐相册";
				Record data = new Record().set("content", pushContent);
				// 设置透传内容
				int gid = Integer.parseInt(groupid);
				Record transmissionRecord = new Record().set("groupid", gid);
				List<Record> list = new ArrayList<Record>();
				list.add(transmissionRecord);
				String transmissionContent = jsonData.getJson(3, "发表动态", list);
				data.set("transmissionContent", transmissionContent);
				// 推送
				push.yinianPushToListWithAndroidNotification(cid, data);
			}
		}

		return maID;
	}

	/**
	 * 获取单个音乐相册
	 * 
	 * @param maID
	 * @return
	 */
	public List<Record> getSingleMusicAlbum(String maID) {
		List<Record> list = Db.find(
				"select maID,maName,maCreatorID,maContent,maCover,maTime,musicUrl,templetUrl,templetProportion,unickname,upic,GROUP_CONCAT(mapOriginal SEPARATOR \",\") as pictureUrl "
						+ "from users,music,templet,mapicture,musicalbum where userid=maCreatorID and musicID=maMusicID and templetID=maTempletID and mapMusicAlbumID=maID and maID="
						+ maID + " and maStatus=0 group by maID desc  ");
		list = dataProcess.changeUserInfoIntoObeject(list);
		return list;
	}

	/**
	 * 获取时光罗盘数据
	 * 
	 * @param userid
	 * @return
	 */
	public List<Record> getTimeCompass(String userid) {
		// 获取用户在的所有非官方相册的相册信息
		List<Record> albumList = Db
				.find("select groupid,gname,gtype from groups,groupmembers where groupid=gmgroupid and gmuserid="
						+ userid + " and gstatus=0 and gtype not in(5)  ");
		// 将组性质转成文字
		albumList = dataProcess.changeGroupTypeIntoWord(albumList);
		// 获取用户在的所有组中的所有动态信息
		List<Record> dateList = Db.find(
				"select groupid,ememorytime,efirstpic from groups,groupmembers,events where groupid=gmgroupid and gmgroupid=egroupid and gtype not in (5) and gmuserid="
						+ userid + " and estatus=0 and gmstatus=0");
		for (Record albumRecord : albumList) {
			String albumGroupID = albumRecord.get("groupid").toString();
			String lastYear = "";// 用于存储上一个dateRecord中的年份
			String lastMonth = "";// 用于存储上一个dateRecord中的年份
			ArrayList<Record> list = new ArrayList<Record>();// list用于存储相应的date记录
			ArrayList<Record> temp = new ArrayList<Record>();// 临时list用于存储跟albumRecord相同组的date记录

			// 挑出和albumRecord中组id相同的记录
			for (Record dateRecord : dateList) {
				String dateGroupID = dateRecord.get("groupid").toString();
				if (albumGroupID.equals(dateGroupID)) {
					temp.add(dateRecord);
				}
			}

			Record tempRecord;
			String minium;
			int index;
			// 将列表中的record按回忆时间排序
			for (int i = 0; i < temp.size(); i++) {
				minium = temp.get(i).get("ememorytime").toString();
				index = i;
				for (int j = i + 1; j < temp.size(); j++) {
					if (dataProcess.compareTwoTime(temp.get(j).get("ememorytime").toString(), minium)) {
						minium = temp.get(j).get("ememorytime").toString();
						index = j;
					}
				}
				tempRecord = temp.get(i);
				temp.set(i, temp.get(index));
				temp.set(index, tempRecord);
			}

			String url = "";// 存储同一个年月的图片的url
			// 将同一个年月的数据归类在一起
			for (int i = 0; i < temp.size(); i++) {
				// 获取年、月
				String time = temp.get(i).get("ememorytime").toString();
				String year = time.substring(0, 4);
				String month = time.substring(5, 7);
				if (i == 0) {
					// 赋初始值
					lastYear = year;
					lastMonth = month;
				}
				if (lastYear.equals(year) && lastMonth.equals(month)) {
					url += temp.get(i).get("efirstpic").toString() + ",";
					// 判断是不是已经是最后一个元素
					if (i == temp.size() - 1) {
						url = url.substring(0, url.length() - 1);
						Record record = new Record().set("year", year).set("month", month).set("picAddress", url);
						list.add(record);
						// 跳出循环
						break;
					}
				} else {
					// 将值存入list中
					url = url.substring(0, url.length() - 1);
					Record record = new Record().set("year", lastYear).set("month", lastMonth).set("picAddress", url);
					list.add(record);
					// 赋予新值
					lastYear = year;
					lastMonth = month;
					url = temp.get(i).get("efirstpic").toString() + ",";
					// 判断是不是 已经是最后一个元素
					if (i == temp.size() - 1) {
						url = url.substring(0, url.length() - 1);
						Record record1 = new Record().set("year", year).set("month", month).set("picAddress", url);
						list.add(record1);
						// 跳出循环
						break;
					}
				}
			}
			// 整理list中的picAddress字段，并对图片授权，同时设置缩略图
			for (Record record : list) {
				String picAddress = record.get("picAddress").toString();
				String thumbnail = "";
				String[] array = picAddress.split(",");
				if (array.length == 1) {
					// 只有一张图，加上两张默认的
					thumbnail = qiniu.getDownloadToken((array[0] + "?imageView2/2/w/300")) + ","
							+ CommonParam.timeCompassDefaultPicOne + "," + CommonParam.timeCompassDefaultPicTwo;
					array[0] = qiniu.getDownloadToken(array[0]);
					picAddress += "," + CommonParam.timeCompassDefaultPicOne + ","
							+ CommonParam.timeCompassDefaultPicTwo;

				}
				if (array.length == 2) {
					// 有两张图，再加一张默认的
					thumbnail = qiniu.getDownloadToken((array[0] + "?imageView2/2/w/300")) + ","
							+ qiniu.getDownloadToken((array[1] + "?imageView2/2/w/300")) + ","
							+ CommonParam.timeCompassDefaultPicOne;
					array[0] = qiniu.getDownloadToken(array[0]);
					array[1] = qiniu.getDownloadToken(array[1]);
					picAddress += "," + CommonParam.timeCompassDefaultPicOne;
				}
				if (array.length == 3) {
					thumbnail = qiniu.getDownloadToken((array[0] + "?imageView2/2/w/300")) + ","
							+ qiniu.getDownloadToken((array[1] + "?imageView2/2/w/300")) + ","
							+ qiniu.getDownloadToken((array[2] + "?imageView2/2/w/300"));
					array[0] = qiniu.getDownloadToken(array[0]);
					array[1] = qiniu.getDownloadToken(array[1]);
					array[2] = qiniu.getDownloadToken(array[2]);
					break;
				}
				if (array.length > 3) {
					thumbnail = qiniu.getDownloadToken((array[0] + "?imageView2/2/w/300")) + ","
							+ qiniu.getDownloadToken((array[1] + "?imageView2/2/w/300")) + ","
							+ qiniu.getDownloadToken((array[2] + "?imageView2/2/w/300"));
					array[0] = qiniu.getDownloadToken(array[0]);
					array[1] = qiniu.getDownloadToken(array[1]);
					array[2] = qiniu.getDownloadToken(array[2]);
					picAddress = array[0] + "," + array[1] + "," + array[2];
				}
				// 赋予新值
				record.set("picAddress", picAddress).set("thumbnail", thumbnail);
			}

			albumRecord.set("date", list);
		}

		return albumList;
	}

	/**
	 * 获取时光罗盘内的照片
	 * 
	 * @param groupid
	 * @param pid
	 * @param year
	 * @param month
	 * @return
	 */
	public List<Record> getPhotosInTimeCompass(String groupid, String pid, String year, String month) {
		if (month.length() == 1) {
			month = "0" + month;
		}
		String date = year + "-" + month;
		List<Record> list = Db.find("select pid,pmemorytime,poriginal from pictures,events where peid=eid and egroupid="
				+ groupid + " and estatus=0 and pmemorytime like '" + date + "%' order by pmemorytime asc ");
		// 获取图片访问权限
		list = dataProcess.GetOriginAndThumbnailAccess(list, "poriginal");
		if (pid.equals("0")) {
			// 初始化，判断是否有三十张，有则返回30张，无则全部返回
			if (list.size() <= 30) {
				return list;
			} else {
				list = list.subList(0, 30);
			}
		} else {
			if (pid.equals("-1")) {
				// pid为1，返回所有照片
				return list;
			} else {
				int index = 0;
				// 找到pid所对应的下标
				for (int i = 0; i < list.size(); i++) {
					if ((list.get(i).get("pid").toString()).equals(pid)) {
						index = i;
						break;
					}
				}
				// 判断下标
				if (index == list.size() - 1) {
					// 表示是最后一个了
					List<Record> temp = new ArrayList<Record>();
					return temp;
				} else {
					if (index + 30 >= list.size()) {
						// 表示能把剩余的全部返回
						list = list.subList(index + 1, list.size());
					} else {
						// 表示只能在返回30个
						list = list.subList(index + 1, index + 31);
					}
				}
			}
		}
		return list;
	}

	/**
	 * 删除音乐相册
	 * 
	 * @param maID
	 * @return
	 */
	@Before(Tx.class)
	public boolean deleteMusicAlbum(String maID) {
		// 删除图片
		Db.update("update mapicture set mapStatus=1 where mapMusicAlbumID=" + maID + " ");
		// 删除音乐相册
		int count = Db.update("update musicalbum set maStatus=1 where maID=" + maID + " ");
		return count == 1;
	}

	/**
	 * 更新用户最后一次登录的信息，目前有登录时间和用户来源两个
	 * 
	 * @param userid
	 * @return
	 */
	public boolean updateLastLoginInfo(String userid, String headPicture) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date();
		String nowTime = sdf.format(date);
		User user = new User().findById(userid).set("ulogintime", nowTime);
		// if (headPicture != null && !headPicture.equals("")) {
		// user.set("upic", headPicture);
		// }
		return user.update();
	}

	/**
	 * 修改动态中的单项信息
	 * 
	 * @param eventid
	 * @param type
	 * @param data
	 * @return
	 */
	@Before(Tx.class)
	public boolean modifyEventInfo(String eventid, String type, String data, String linkedData) {
		boolean flag = false;
		Event event = new Event().findById(eventid);
		switch (type) {
		case "cardstyle":
			// 传卡片样式的ID
			event.set("ecardstyle", data);
			break;
		case "text":
			event.set("etext", data);
			break;
		case "audio":
			data = (CommonParam.qiniuOpenAddress + data);
			event.set("eaudio", data);
			// 更新用户使用的存储空间
			Double storage = Double.parseDouble(linkedData);
			String userid = event.get("euserid").toString();
			updateUserStoragePlace(userid, storage, "add");
			break;
		case "memorytime":
			// 传年日月 格式为yyyy-MM-dd
			data += " 00:00:00";
			event.set("ememorytime", data);
			// 获取该动态目前所有的照片，修改所有照片的回忆时间
			List<Record> pictureList = Db.find("select * from pictures where peid=" + eventid + " and pstatus=0 ");
			for (Record record : pictureList) {
				String pid = record.get("pid").toString();
				Picture picture = new Picture().findById(pid);
				picture.set("pmemorytime", data);
				picture.update();
			}
			break;
		case "place":
			event.set("place", data);
			break;
		}
		flag = event.update();
		return flag;
	}

	/**
	 * 增加动态中的单项信息
	 * 
	 * @param eventid
	 * @param type
	 * @param data
	 * @return
	 */
	@Before(Tx.class)
	public boolean addEventInfo(String eventid, String type, String data, String linkedData) {
		boolean flag = false;
		Event event = new Event().findById(eventid);
		String userid = event.get("euserid").toString();
		String groupid = event.get("egroupid").toString();
		switch (type) {
		case "picture":
			// 获取动态的回忆时间，则新增图片的回忆时间与其一致
			String memorytime = event.get("ememorytime").toString();
			// 增加多张图片则用英文标点的逗号隔开
			String[] picAddress = data.split(",");
			for (int i = 0; i < picAddress.length; i++) {
				String url = CommonParam.qiniuOpenAddress + picAddress[i];
				Picture picture = new Picture().set("poriginal", url).set("peid", eventid).set("pmemorytime",
						memorytime).set("pGroupid", groupid).set("puserid", userid).set("pMain", event.get("eMain"));
				picture.save();
			}
			// 更新用户使用的存储空间
			Double storage = Double.parseDouble(linkedData);
			updateUserStoragePlace(userid, storage, "add");
			break;
		case "tag":
			// 一次只能增加一个tag
			Tag tag = new Tag().set("tagEventID", eventid).set("tagContent", data);
			boolean flag1 = tag.save();
			boolean flag2 = handleTagsInHistoryTags(userid, data);
			if (flag1 && flag2) {
				break;
			} else {
				return false;
			}
		}
		flag = event.update();
		return flag;
	}

	/**
	 * 删除动态内的单项信息
	 * 
	 * @param eventid
	 * @param type
	 * @param data
	 * @return
	 */
	public boolean removeEventInfo(String eventid, String type, String data, String linkedData) {
		boolean flag = false;
		Event event = new Event().findById(eventid);
		switch (type) {
		case "text":
			event.set("etext", null);
			break;
		case "audio":
			event.set("eaudio", null);
			break;
		case "place":
			event.set("eplace", null);
			break;
		case "tag":
			// data为该tag的ID
			Tag tag = new Tag().findById(data).set("tagStatus", 1);
			if (tag.update()) {
				break;
			} else {
				return false;
			}
		}
		flag = event.update();
		return flag;
	}

	/**
	 * 更新用户已用存储空间
	 */
	public boolean updateUserStoragePlace(String userid, Double number, String type) {
		boolean flag = false;
		User user = new User().findById(userid);
		Double useSpace = user.getDouble("uusespace");
		switch (type) {
		case "add":
			useSpace += number;
			break;
		case "reduce":
			useSpace -= number;
			break;
		}
		user.set("uusespace", useSpace);
		flag = user.update();
		return flag;
	}

	/**
	 * 在历史标签表中处理新加入的标签
	 */
	public boolean handleTagsInHistoryTags(String userid, String tagContent) {
		boolean flag = false;
		// 判断表中该用户是否发过同样内容的标签且该标签的历史记录没有被删除
		List<Record> list = Db.find("select historyTagContent from historytag where historyTagUserID=" + userid
				+ " and historyTagContent='" + tagContent + "' and historyTagType=1 ");
		if (list.size() > 0) {
			// 说明该标签是历史标签且未删除
			flag = true;
		} else {
			HistoryTag tag = new HistoryTag().set("historyTagUserID", userid).set("historyTagContent", tagContent);
			flag = tag.save();
		}
		return flag;
	}

	/**
	 * 初始化、加载、刷新私密相册内动态
	 */
	public String getPrivateAlbumEvents(String userid, String groupid, String eventid, String sign) {

		int count = 1;
		// 构造相应的SQL语句
		String sqlForEvent = "";
		String sqlForComment = "";
		switch (sign) {
		case "initialize":
			sqlForEvent = "select eid,egroupid,userid,unickname,upic,etext,eaudio,eaudiotime,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\" ) as url from users,events,pictures where userid=euserid and eid=peid and egroupid="
					+ groupid + " and estatus=0 and pstatus=0 group by peid DESC limit 10";
			sqlForComment = CommonParam.selectForComment
					+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and egroupid="
					+ groupid + " and cstatus=0 ORDER BY ceid,ctime asc";
			// 初始化的时候更新相册状态为无新动态
			count = Db.update(
					"update groupmembers set gmnotify=0 where gmgroupid=" + groupid + " and gmuserid=" + userid + " ");
			break;
		case "refresh":
			sqlForEvent = "select eid,egroupid,userid,unickname,upic,etext,eaudio,eaudiotime,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\" ) as url from users,events,pictures where userid=euserid and eid=peid and egroupid="
					+ groupid + " and eid > " + eventid + " and estatus=0 and pstatus=0 group by peid DESC ";
			sqlForComment = CommonParam.selectForComment
					+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and eid>"
					+ eventid + " and egroupid=" + groupid + " and cstatus=0 ORDER BY ceid,ctime asc";
			break;
		case "loading":
			sqlForEvent = "select eid,egroupid,userid,unickname,upic,etext,eaudio,eaudiotime,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\" ) as url from users,events,pictures where userid=euserid and eid=peid and egroupid="
					+ groupid + " and eid < " + eventid + " and estatus=0 and pstatus=0 group by peid DESC limit 10";
			sqlForComment = CommonParam.selectForComment
					+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and eid<"
					+ eventid + " and egroupid=" + groupid + " and cstatus=0 ORDER BY ceid,ctime asc";
			break;
		}
		// 整合数据
		List<Record> event = dataProcess.encapsulationEventList(Db.find(sqlForEvent));// 获取事件的相关信息并封装里面的用户对象
		List<Record> comment = dataProcess.encapsulationCommentList(Db.find(sqlForComment));// 获取事件的评论信息并封装里面的用户对象
		List<Record> list = dataProcess.combieEventAndComment(event, comment);// 拼接事件与评论
		// 封装事件和标签
		list = dataProcess.combineEventWithTags(list);
		// 封装事件和卡片样式
		list = dataProcess.combineEventWithCardStyle(list);
		// 修改upic字段格式
		list = dataProcess.ChangePicAsArray(list);

		jsonString = jsonData.getJson(0, "success", list);

		return jsonString;

	}

	/**
	 * 显示备份照片界面 ?imageView2/w/300
	 */
	public List<Record> showBackupPhotos(String userid, String date, String sign) {
		String sqlForEvent = "";
		List<Record> photoList = new ArrayList<Record>();
		switch (sign) {
		case "initialize":
			sqlForEvent = "select backupEventID,backupDate from backupevent where backupUserID=" + userid
					+ " and backupStatus=0 order by backupDate desc ";
			break;
		case "loading":
			sqlForEvent = "select backupEventID,backupDate from backupevent where backupUserID=" + userid
					+ " and backupStatus=0 and backupDate<'" + date + "' order by backupDate desc limit 10 ";
			break;
		}
		List<Record> eventList = Db.find(sqlForEvent);
		for (Record record : eventList) {
			String backupEventID = record.get("backupEventID").toString();
			photoList = Db.find("select backupPhotoID,backupPhotoURL,backupPHash from backupphoto where backupPEid="
					+ backupEventID + " and backupPStatus=0 ");
			// 获取图片访问权限
			photoList = dataProcess.GetOriginAndThumbnailAccessWithDirectCut(photoList, "backupPhotoURL");
			record.set("backupPhoto", photoList);
		}

		return eventList;
	}

	/**
	 * 备份单日照片
	 */
	@Before(Tx.class)
	public boolean backupSingleDayPhoto(String userid, String date, String photo, double storage, String hash) {
		boolean flag = false;
		// 图片地址处理
		String[] url = dataProcess.getPicAddress(photo, "secret");

		// 判断该用户该日期内是否已经上传过动态，是的话直接插入到该动态当中
		List<Record> judge = Db.find("select backupEventID from backupevent where backupUserID=" + userid
				+ " and backupDate='" + date + "' and backupStatus=0 ");
		if (judge.size() == 0) {
			BackupEvent event = new BackupEvent().set("backupUserID", userid).set("backupDate", date)
					.set("backupStoragePlace", storage);
			if (event.save()) {
				String eid = event.get("backupEventID").toString();
				// 存储图片
				for (int i = 0; i < url.length; i++) {
					BackupPhoto backupPhoto = new BackupPhoto().set("backupPhotoURL", url[i]).set("backupPEid", eid);
					flag = backupPhoto.save();
					if (!flag) {
						return flag;
					}
				}
			} else {
				return flag;
			}
		} else {
			String eventID = judge.get(0).get("backupEventID").toString();
			int count = Db.update("update backupevent set backupStoragePlace = backupStoragePlace+" + storage
					+ " where backupEventID=" + eventID + " ");
			if (count == 1) {
				// 存储图片
				for (int i = 0; i < url.length; i++) {
					BackupPhoto backupPhoto = new BackupPhoto().set("backupPhotoURL", url[i]).set("backupPEid",
							eventID);
					flag = backupPhoto.save();
					if (!flag) {
						return flag;
					}
				}
			} else {
				return flag;
			}
		}

		return flag;
	}

	/**
	 * 修改普通动态
	 */
	public boolean ModifyEvent(Event event, String eid, String picture, String content, String place, String storage) {

		boolean picProcess = true;
		if (picture != null) {
			picProcess = resetPicture(eid, picture);
			event.set("efirstpic", CommonParam.qiniuPrivateAddress + (picture.split(",")[0]));
		}

		if (content != null) {
			event.set("etext", content);
		}

		if (place != null) {
			event.set("eplace", place);
		}

		if (storage != null) {
			event.set("eStoragePlace", storage);
		}

		if (picture == null && content == null && place == null && storage == null) {
			return true;
		} else {
			return picProcess && (event.update());
		}

	}

	/**
	 * 修改记录卡片
	 */
	public boolean ModifyRecordCard(String userid, Event event, String eid, String picture, String content,
			String place, String memorytime, String audio, String tag, String storage) {

		boolean picProcess = true;
		if (picture != null) {
			picProcess = resetPicture(eid, picture);
			event.set("efirstpic", CommonParam.qiniuPrivateAddress + (picture.split(",")[0]));
		}

		boolean tagProcess = true;
		if (tag != null) {
			tagProcess = resetTag(userid, eid, tag);
		}

		if (content != null) {
			event.set("etext", content);
		}

		if (place != null) {
			event.set("eplace", place);
		}

		if (storage != null) {
			event.set("eStoragePlace", storage);
		}

		if (audio != null) {
			if (audio.equals("")) {
				event.set("eaudio", null);
			} else {
				event.set("eaudio", CommonParam.qiniuOpenAddress + audio);
			}
		}

		if (memorytime != null) {
			event.set("ememorytime", memorytime);
		}

		if (picture == null && tag == null && content == null && place == null && storage == null && audio == null
				&& memorytime == null) {
			return true;
		} else {
			return picProcess && tagProcess && (event.update());
		}

	}

	/**
	 * 修改语音图集
	 */
	public boolean ModifyPostCard(Event event, String eid, String picture, String place, String cover,
			String memorytime, String audio, String storage) {

		boolean picProcess = true;
		if (picture != null) {
			picProcess = resetPicture(eid, picture);
			event.set("efirstpic", CommonParam.qiniuPrivateAddress + (picture.split(",")[0]));
		}

		if (place != null) {
			event.set("eplace", place);
		}

		if (storage != null) {
			event.set("eStoragePlace", storage);
		}

		if (audio != null) {
			if (audio.equals("")) {
				event.set("eaudio", null);
			} else {
				event.set("eaudio", CommonParam.qiniuOpenAddress + audio);
			}
		}

		if (memorytime != null) {
			event.set("ememorytime", memorytime);
		}

		if (cover != null) {
			event.set("ecover", cover);
		}

		if (picture == null && place == null && storage == null && audio == null && memorytime == null
				&& cover == null) {
			return true;
		} else {
			return picProcess && (event.update());
		}

	}

	/**
	 * 重置图片
	 */
	@Before(Tx.class)
	public boolean resetPicture(String eid, String picAddress) {
		String[] picArray = picAddress.split(",");
		List<String> newList = new ArrayList<String>();
		for (int i = 0; i < picArray.length; i++) {
			newList.add(picArray[i]);
		}

		boolean inserFlag = true;
		boolean deleteFlag = false;

		// 处理字符串用于数据库查询
		String newPicAddress = "";
		for (int i = 0; i < picArray.length; i++) {
			newPicAddress += ("'" + picArray[i] + "',");
		}
		newPicAddress = newPicAddress.substring(0, newPicAddress.length() - 1);

		/**
		 * 删除图片
		 */
		// 查询要删除的图片数
		List<Record> list = Db.find("select substring_index(poriginal, '/', -1) as poriginal from pictures where peid="
				+ eid + " and substring_index(poriginal, '/', -1) not in (" + newPicAddress + ")  and pstatus=0");
		int size = list.size();
		// 修改要删除的图片的状态
		int count = Db.update("update pictures set pstatus=1 where peid=" + eid
				+ " and substring_index(poriginal, '/', -1) not in (" + newPicAddress + ")  and pstatus=0");
		// 比对size和count是否一致，一致则表示操作成功
		deleteFlag = (size == count);

		/**
		 * 增加图片
		 */
		// 查询不删除的图片
		List<Record> existList = Db
				.find("select substring_index(poriginal, '/', -1) as poriginal from pictures where peid=" + eid
						+ " and pstatus=0");
		// 转换成两个list获取需要增加的图片地址list
		List<String> oldList = new ArrayList<String>();
		for (Record record : existList) {
			oldList.add(record.get("poriginal").toString());
			
		}
		// 去除已有元素
		Iterator<String> it = newList.iterator();
		while (it.hasNext()) {
			if (oldList.contains(it.next())) {
				it.remove();
			}
		}

		//查询动态所在相册和用户
		List<Record> eventList = Db.find("SELECT euserid,egroupid FROM `events` where eid="+eid);
		String groupid = eventList.get(0).get("egroupid").toString();
		String userid = eventList.get(0).get("euserid").toString();
		// 插入数据库
		for (String pic : newList) {
			Picture picture = new Picture().set("poriginal", CommonParam.qiniuPrivateAddress + pic).set("peid", eid)
					.set("pGroupid",groupid ).set("puserid",userid);
			if (!picture.save()) {
				inserFlag = false;
				break;
			}
		}

		return deleteFlag && inserFlag;
	}

	/**
	 * 重置标签
	 */
	@Before(Tx.class)
	public boolean resetTag(String userid, String eid, String tag) {
		// 判断标签tags是否有传值，有则进行插入
		if (tag != null) {
			// 通过 &nbsp 来隔开各个tag
			String[] eventTag = tag.split("&nbsp");
			// 清空原来的标签
			Db.update("update tags set tagStatus=1 where tagEventID=" + eid + " ");
			// 加入标签，并对每个标签进行历史标签处理
			for (int i = 0; i < eventTag.length; i++) {
				Tag tagObject = new Tag();
				tagObject.set("tagEventID", eid).set("tagContent", eventTag[i]);
				tagObject.save();
				handleTagsInHistoryTags(userid, eventTag[i]);
			}
		}

		return true;
	}

	/**
	 * 转移动态
	 */
	@Before(Tx.class)
	public List<Record> transferEvent(String userid, String eid, String groupid) {
		/** 黑名单**/
//		User u=new User().findById(userid);
//		if(null!=u&&null!=u.get("ustate")&&u.get("ustate").toString().equals("1")){
//			Db.update("update pictures set poriginal='http://oibl5dyji.bkt.clouddn.com/Resource_violation_pic.jpg' where peid=" + eid);
//		}
		/** 黑名单**/
		// 转移动态
		Event event = new Event().findById(eid);
		// 新动态发布时间
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String time = sdf.format(new Date());
		event.set("euserid", userid).set("egroupid", groupid).set("euploadtime", time).set("isSynchronize", 1);
		event.remove("eid");
		if (event.save()) {
			// 获得新的eid
			String newEid = event.get("eid").toString();
			// 转移照片
			List<Record> pictureList = Db.find("select * from pictures where peid=" + eid + " and pstatus=0 ");
			for (Record record : pictureList) {
				String address = record.get("poriginal").toString();
				Picture picture = new Picture().set("poriginal", address).set("peid", newEid)
						.set("pGroupid", groupid).set("puserid", userid).set("pMain", event.get("eMain"));
				boolean flag = picture.save();
				if (!flag) {
					return null;
				}
			}
			EventService eService = new EventService();
			return eService.getSingleEvent(Integer.parseInt(newEid), "app");
		} else {
			return null;
		}
	}

	/**
	 * 获取相册信息
	 */
	public String getSpaceInfo(String groupid) {
		Group group = new Group().findById(groupid);

		int status = Integer.parseInt(group.get("gstatus").toString());
		int gtype = Integer.parseInt(group.get("gtype").toString());
		// 判断相册是否删除
		if (status == 0) {
			// 获取返回数据
			Record record = new Record().set("gname", group.get("gname").toString())
					.set("gcreator", group.get("gcreator").toString()).set("gtype", gtype)
					.set("gnum", group.get("gnum").toString());
			// 获取成员列表
			List<Record> groupMember = Db.find(
					"select userid,unickname,upic,gmtime from users,groups,groupmembers where groupid=gmgroupid and users.userid=groupmembers.gmuserid and gmgroupid='"
							+ groupid + "' and gmstatus=0 limit 10 ");
			// 获取照片数
			List<Record> photo = Db.find(
					"select count(*) as gpicNum from groups,events,pictures where peid=eid and groupid=egroupid and groupid="
							+ groupid + " and estatus in(0,3) and pstatus=0 ");
			record.set("picNum", photo.get(0).get("gpicNum").toString()).set("memberList", groupMember);

			List<Record> result = new ArrayList<Record>();
			result.add(record);
			jsonString = jsonData.getSuccessJson(result);
		} else if (status == 1) {
			jsonString = jsonData.getJson(1012, "相册已被删除");
		} else {
			jsonString = jsonData.getJson(1037, "相册已被封");
		}
		return jsonString;
	}
	/**
	 * 显示组列表
	 * 
	 * @param userid model by lk mode=create 为自己创建的相册，join为自己加入的相册
	 * @return
	 */
	public String showGroupByCreateOrJoin(int userid ,String mode) {
		List<Record> list = Group.GetUnOrderSpaceBasicInfoByCreateOrJoin(userid,mode);
		// 封装空间数据
		list = dataProcess.spaceDataEncapsulation(list, userid);
		jsonString = jsonData.getJson(0, "success", list);
		return jsonString;
	}
	public String SetGroupIsTop(String userid,String groupid,String isTop){
		GroupsIsTop top=new GroupsIsTop();
		List<Record> list=top.findByUseridAndGroupid(userid,groupid);
		if(isTop.equals("yes")){			
			if(list.isEmpty()){
				if(!top.saveByUseridAndGroupid(userid, groupid)){
					jsonString = jsonData.getJson(2, "参数错误");
				}
			}
		}else{
			if(!list.isEmpty()){
				Record r=list.get(0);
				top.deleteById(r.getLong("id").intValue());				
			}
		}
		jsonString = jsonData.getJson(0, "success");
		return jsonString;
	}
	
	/**
	 * 相册置顶和取消置顶
	 * @param userid
	 * @param groupid
	 * @param isTop
	 * @return
	 */
	public String SetGroupIsTopNew(String userid,String groupid,String isTop){
		GroupMember member = new GroupMember();
		List<Record> list=Db.find("select gmid,IFNULL(isTop,0) as isTop from groupmembers where gmuserid=" + userid + 
				" and gmgroupid=" + groupid);
		Long top;
		Long gmid;
		//int top = list.get(0).getInt("isTop");
		if(null != list && list.size()!=0) {
			top = list.get(0).getLong("isTop");
			//top = String.valueOf(list.get(0).get("isTop"));
			gmid = list.get(0).getLong("gmid");
		}else {
			jsonString = jsonData.getJson(-50, "参数错误");
			return jsonString;
		}
		
		//System.out.println(top);
		member = new GroupMember().findById(gmid);
		if(isTop.equals("yes")){			
			if(top==0){
				Timestamp timestamp = new Timestamp(System.currentTimeMillis());
				member.set("isTop", 1).set("topTime", timestamp);
				if(!member.update()) {
					jsonString = jsonData.getJson(-50, "参数错误");
				}
			}
		}else{
			if(top==1){
				member.set("isTop", 0).set("topTime", null);
				//System.out.println("进来了");
				if(!member.update()) {
					jsonString = jsonData.getJson(-50, "参数错误");
				}
			}
		}
		jsonString = jsonData.getJson(0, "success");
		return jsonString;
	}
/*	public String SetGroupIsTopNew(String userid,String groupid,String isTop){
		GroupMember member = new GroupMember();
		List<Record> list=Db.find("select * from groupmembers where gmuserid=" + userid + 
				" and gmgroupid=" + groupid);
		int top = list.get(0).getInt("isTop");
		//System.out.println(top);
		Long gmid = list.get(0).getLong("gmid");
		member = new GroupMember().findById(gmid);
		if(isTop.equals("yes")){			
			if(top==0){
				Timestamp timestamp = new Timestamp(System.currentTimeMillis());
				member.set("isTop", 1).set("topTime", timestamp);
				if(!member.update()) {
					jsonString = jsonData.getJson(-50, "参数错误");
				}
			}
		}else{
			if(top==1){
				member.set("isTop", 0).set("topTime", null);
				//System.out.println("进来了");
				if(!member.update()) {
					jsonString = jsonData.getJson(-50, "参数错误");
				}
			}
		}
		jsonString = jsonData.getJson(0, "success");
		return jsonString;
	}*/
	
	/**
	 * 显示组列表 显示是否置顶 by lk  精简小程序专用
	 * 
	 * @param userid 
	 * @return
	 */
	public String simH5ShowGroupWithTop(int userid) {
		QiniuOperate qiniu = new QiniuOperate();
		List<Record> list = Group.GetUnOrderSpaceBasicInfo(userid);
		List<Record> topList=new GroupsIsTop().findByUseridAndGroupid(userid+"",null);
		List<Record> topIsFirst=new LinkedList<Record>();
		for(Record topR:topList){
			Iterator<Record> it = list.iterator();
			while(it.hasNext()){
				Record r = it.next();
				r.set("isTop", false);
				if(r.get("groupid").toString().equals(topR.get("tGroupId").toString())){
					r.set("isTop", true);
					topIsFirst.add(r);
					it.remove();
				}
			}
		}
		topIsFirst.addAll(list);
		for(Record r:topIsFirst){
			//获取视频封面，如果没有封面，则获取最后一张上传的图片
			if(null==r.get("simAppPic")||r.get("simAppPic").equals("")){
				List<Record> eventList=Db.find("select eid from events where eMain=0 and estatus=0 and egroupid="+r.get("groupid").toString()+" order by euploadtime desc limit 0,1");
				if(!eventList.isEmpty()){
					List<Record> topPicList=Db.find("select poriginal from pictures where pstatus = 0 and peid ="+eventList.get(0).get("eid")+" order by puploadtime desc limit 0,1");
					if(!topPicList.isEmpty()){
						r.set("simAppPic",qiniu.getDownloadToken(topPicList.get(0).getStr("poriginal")+"?imageView2/2/w/600"));
					}else{
						r.set("simAppPic","http://oibl5dyji.bkt.clouddn.com/simAppNoPhoto.png");
					}
				}else{
					r.set("simAppPic","http://oibl5dyji.bkt.clouddn.com/simAppNoPhoto.png");
				}
			}
		}		
			// 封装空间数据
			list = dataProcess.spaceDataEncapsulation(topIsFirst, userid);
			jsonString = jsonData.getJson(0, "success", list);
			return jsonString;

	}
	
	/**
	 * 显示组列表 显示是否置顶   精简小程序专用
	 * 
	 * @param userid 
	 * @return
	 */
	public String simH5ShowGroupWithTopNew(int userid,int pagenum) {
		int page = (pagenum-1)*10;
		String sql = "select Istop,topTime,isDefault,openGId,groupid,gimid,gname,gpic,gnum,gtime,gmnotify,gtype,gcreator,ginvite,gintroduceText,gintroducePic,gmorder,gmtime,gOrigin,simAppPic";
		List<Record> toplist = Db.find(sql + " from groupmembers,groups WHERE groupid=gmgroupid and gmstatus=0 and gmuserid="+userid+
				" ORDER BY IsTop desc,topTime desc,gmtime DESC LIMIT "+ page+",10");
		toplist = dataProcess.spaceDataEncapsulationNew(toplist, userid);
		jsonString = jsonData.getJson(0, "success", toplist);
		
		return jsonString;
	}
	/**
	 * 显示组列表 显示是否置顶 by lk 
	 * 
	 * @param userid 
	 * @return
	 */
	public String showGroupWithTop(int userid) {
		QiniuOperate qiniu = new QiniuOperate();
		List<Record> list = Group.GetUnOrderSpaceBasicInfo(userid);
		List<Record> topList=new GroupsIsTop().findByUseridAndGroupid(userid+"",null);
		List<Record> topIsFirst=new LinkedList<Record>();
		for(Record topR:topList){
			Iterator<Record> it = list.iterator();
			while(it.hasNext()){
				Record r = it.next();
				r.set("isTop", false);
				if(r.get("groupid").toString().equals(topR.get("tGroupId").toString())){
					r.set("isTop", true);
					topIsFirst.add(r);
					it.remove();
				}
			}
		}
		topIsFirst.addAll(list);	
			// 封装空间数据
			list = dataProcess.spaceDataEncapsulation(topIsFirst, userid);
			jsonString = jsonData.getJson(0, "success", list);
			return jsonString;
		
//		
//		Iterator<Record> it = list.iterator();
//	//	System.out.println("list.size:"+list.size());
//		while(it.hasNext()){
//			Record r = it.next();
//			r.set("isTop", false);
//			for(Record topR:topList){
//				
//				if(r.get("groupid").toString().equals(topR.get("tGroupId").toString())){
//					System.out.println(r.get("groupid")+"===="+topR.get("tGroupId"));
//					r.set("isTop", true);
//					topIsFirst.add(r);
//					it.remove();
//				}
//			}
//		List<Record> list = Group.GetUnOrderSpaceBasicInfo(userid);
//		List<Record> topList=new GroupsIsTop().findByUseridAndGroupid(userid+"",null);
//		List<Record> topIsFirst=new LinkedList<Record>();
//		Iterator<Record> it = list.iterator();
//	//	System.out.println("list.size:"+list.size());
//		while(it.hasNext()){
//			Record r = it.next();
//			r.set("isTop", false);
//			for(Record topR:topList){
//				
//				if(r.get("groupid").toString().equals(topR.get("tGroupId").toString())){
//					System.out.println(r.get("groupid")+"===="+topR.get("tGroupId"));
//					r.set("isTop", true);
//					topIsFirst.add(r);
//					it.remove();
//				}
					
	//	}
	//	System.out.println("list.size2222:"+list.size());
	//	System.out.println("topIsFirst.size:"+topIsFirst.size());
//		for(Record r:list){
//			
//			}
//			if(null!=r.get("isTop")){
//				r.set("isTop", true);
//			}else{
//				r.set("isTop", false);
//			}
//		}
//		topIsFirst.addAll(list);
//	//	System.out.println("topIsFirst2222.size:"+topIsFirst.size());
//		// 封装空间数据
//		list = dataProcess.spaceDataEncapsulation(topIsFirst, userid);
//		jsonString = jsonData.getJson(0, "success", list);
//		return jsonString;
	}
	/**
	 * 显示组列表 显示是否置顶 分页 不显示图片数，用户数 by ly  
	 * 
	 * @param userid 
	 * @return
	 */
	public String showNoPicGroupWithTopNew(int userid,int pagenum,String type) {
		List<Record> toplist = new ArrayList<>();
		String actGroupids = CacheKit.get("DataSystem", "showGroupWithTopNew_actGroupids");
		if(actGroupids == null) {
			//缓存为空  从数据库查询
			List<Record> actGroupList = Db.find("select activitiGroupid from activitigroups");
			if(null!=actGroupList&&!actGroupList.isEmpty()){
				StringBuffer ids=new StringBuffer();
				for(int i=0;i<actGroupList.size();i++){
					ids.append(actGroupList.get(i).get("activitiGroupid").toString()).append(",");
				}
				if(ids.length()>0){
					actGroupids=ids.substring(0, ids.length()-1);
					CacheKit.put("DataSystem", "showGroupWithTopNew_actGroupids",actGroupids);			
				}		
			}
		}
		String conds="";
		if(actGroupids!=null){
			conds+=" and groupid not in ("+actGroupids+")";
		}
		if(type.equals("refresh")) {
			String sql = "select isTop,topTime,isDefault,openGId,groupid,gimid,gname,gpic,gnum,gtime,gmnotify,gtype,gcreator,ginvite,gintroduceText,gintroducePic,gmorder,gmtime,gOrigin,simAppPic";
			toplist = Db.find(sql + " from groupmembers,groups WHERE groupid=gmgroupid and gmstatus=0 and gstatus=0 and gmuserid="+userid+conds+
					" ORDER BY isTop desc,topTime desc,gmtime DESC LIMIT "+ pagenum);
		}else {
			int page = (pagenum-1)*10;
			String sql = "select isTop,topTime,gmtime,isDefault,openGId,groupid,gimid,gname,gpic,gnum,gtime,gmnotify,gtype,gcreator,ginvite,gintroduceText,gintroducePic,gmorder,gmtime,gOrigin,simAppPic";
			toplist = Db.find(sql + " from groupmembers,groups WHERE groupid=gmgroupid and gmstatus=0 and gstatus=0 and gmuserid="+userid+conds+
					" ORDER BY isTop desc,topTime desc,gmtime DESC LIMIT "+ page+",10");
		}
		//toplist = dataProcess.spaceDataEncapsulationNew(toplist, userid);
		jsonString = jsonData.getJson(0, "success", toplist);
		
		return jsonString;
	}
	/**
	 * 显示组列表 显示是否置顶 分页 by ly  
	 * 
	 * @param userid 
	 * @return
	 */
	public String showGroupWithTopNew(int userid,int pagenum,String type) {
		List<Record> toplist = new ArrayList<>();
		String actGroupids = CacheKit.get("DataSystem", "showGroupWithTopNew_actGroupids");
		if(actGroupids == null) {
			//缓存为空  从数据库查询
			List<Record> actGroupList = Db.find("select activitiGroupid from activitigroups");
			if(null!=actGroupList&&!actGroupList.isEmpty()){
				StringBuffer ids=new StringBuffer();
				for(int i=0;i<actGroupList.size();i++){
					ids.append(actGroupList.get(i).get("activitiGroupid").toString()).append(",");
				}
				if(ids.length()>0){
					actGroupids=ids.substring(0, ids.length()-1);
					CacheKit.put("DataSystem", "showGroupWithTopNew_actGroupids",actGroupids);			
				}		
			}
		}
		String conds="";
		if(actGroupids!=null){
			conds+=" and groupid not in ("+actGroupids+")";
		}
		if(type.equals("refresh")) {
			String sql = "select isTop,topTime,isDefault,openGId,groupid,gimid,gname,gpic,gnum,gtime,gmnotify,gtype,gcreator,ginvite,gintroduceText,gintroducePic,gmorder,gmtime,gOrigin,simAppPic";
			toplist = Db.find(sql + " from groupmembers,groups WHERE groupid=gmgroupid and gmstatus=0 and gstatus=0 and gmuserid="+userid+conds+
					" ORDER BY isTop desc,topTime desc,gmtime DESC LIMIT "+ pagenum);
		}else {
			int page = (pagenum-1)*10;
			String sql = "select isTop,topTime,gmtime,isDefault,openGId,groupid,gimid,gname,gpic,gnum,gtime,gmnotify,gtype,gcreator,ginvite,gintroduceText,gintroducePic,gmorder,gmtime,gOrigin,simAppPic";
			toplist = Db.find(sql + " from groupmembers,groups WHERE groupid=gmgroupid and gmstatus=0 and gstatus=0 and gmuserid="+userid+conds+
					" ORDER BY isTop desc,topTime desc,gmtime DESC LIMIT "+ page+",10");
		}
		toplist = dataProcess.spaceDataEncapsulationNew(toplist, userid);
		jsonString = jsonData.getJson(0, "success", toplist);
		
		return jsonString;
	}
	/**
	 * 上传默认动态，成功后返回事件的ID eOrigin=1 by lk
	 * 
	 * @param userid
	 * @param groupid
	 * @param picAddress
	 * @param content
	 * @return
	 */
	@Before(Tx.class)
	public String uploadDefault(String userid, String groupid, String picAddress, String content, String storage,
			String memorytime, String mode, String location, String source) {

		String eventID = "";

		// 将地址字符串转换成数组
		String[] picArray = dataProcess.getPicAddress(picAddress, mode);

		// 图片鉴黄
		picArray = dataProcess.PictureVerify(picArray);

		// 获取动态第一张图片地址
		String firstPic = picArray[0];

		// 将占用空间的类型转为double
		Double place = Double.parseDouble(storage);

		String newTime = "";
		// 保存事件
		Event event = new Event();
		event.set("eOrigin", 1);
		// 判断memorytime字段是否有传过来
		if (memorytime == null || memorytime.equals("")) {
			event.set("egroupid", groupid).set("euserid", userid).set("etext", content).set("efirstpic", firstPic)
					.set("eStoragePlace", place).set("eSource", source).set("isSynchronize", 1);
		} else {
			// 拼接字符串
			newTime = memorytime + " 00:00:00";
			event.set("egroupid", groupid).set("euserid", userid).set("etext", content).set("efirstpic", firstPic)
					.set("ememorytime", newTime).set("eStoragePlace", place).set("eSource", source)
					.set("isSynchronize", 1);
		}

		// 判断mode是不是等于dayMark 即日签，是则加入etype为3
		if (mode.equals("dayMark")) {
			event.set("etype", 3);
		}

		// 判断位置信息
		if (location != null && !location.equals("")) {
			event.set("eplace", location);
		}

		if (event.save()) {
			eventID = event.get("eid").toString();

			// 将事件所属组的状态改为有新状态
			// Db.update("update groupmembers set gmnotify=1 where gmgroupid ="
			// + groupid + " ");
			// // 发布者的状态改为无新状态，因为就算别人发了新动态，发布者发布完后还在组内动态界面
			// Db.update("update groupmembers set gmnotify=0 where gmgroupid ="
			// + groupid + " and gmuserid = " + userid + " ");

			// 判断memorytime字段是否有传过来
			if (memorytime == null || memorytime.equals("")) {
				// 保存图片
				for (int i = 0; i < picArray.length; i++) {
					Picture pic = new Picture();
					pic.set("peid", eventID).set("poriginal", picArray[i])
						.set("pGroupid", groupid).set("puserid", userid);
					pic.save();
				}
			} else {
				// newTime不需要再拼接
				// 保存图片
				for (int i = 0; i < picArray.length; i++) {
					Picture pic = new Picture();
					pic.set("peid", eventID).set("poriginal", picArray[i]).set("pmemorytime", newTime).set("pGroupid", groupid).set("puserid", userid);
					pic.save();
				}
			}

		}

		// 增加用户已使用空间
		if (place != 0) {
			updateUserStoragePlace(userid, place, "add");
		}

		// // 进行推送准备
		// // 获取组内所有用户的ID
		// List<Record> useridList = Db
		// .find("select gmuserid from groupmembers where gmgroupid = "
		// + groupid + " ");
		// // 去掉发送者的ID
		// for (Record record : useridList) {
		// if ((record.get("gmuserid").toString()).equals(userid)) {
		// useridList.remove(record);
		// break;
		// }
		// }
		// // useridList不为null时才进行推送
		// if (!(useridList.isEmpty())) {
		// // 将list中的ID拼接成字符串
		// String userids = dataProcess.changeListToString(useridList,
		// "gmuserid");
		// // userids不为空字符串时才进行推送
		// if (!(userids.equals(""))) {
		// // 获取要进行推送的用户的cid
		// Record cid = pushMessage.getUsersCids(userids, "userid");
		// // 获取动态发布者的昵称
		// String nickname = dao.getUserSingleInfo(userid, "nickname");
		// // 获取发布动态所在组的信息
		// List<Record> groupList = Db
		// .find("select gname,gtype from groups where groupid = "
		// + groupid + " ");
		// groupList = dataProcess.changeGroupTypeIntoWord(groupList);
		// // 拼接推送内容
		// String pushContent = nickname + "在私密空间“"
		// + groupList.get(0).getStr("gname") + "”发布了新动态";
		// Record data = new Record().set("content", pushContent);
		// // 设置透传内容
		// int gid = Integer.parseInt(groupid);
		// Record transmissionRecord = new Record().set("groupid", gid)
		// .set("pushContent", pushContent)
		// .set("gname", groupList.get(0).getStr("gname"));
		// List<Record> list = new ArrayList<Record>();
		// list.add(transmissionRecord);
		// String transmissionContent = jsonData.getJson(3, "发表动态", list);
		// data.set("transmissionContent", transmissionContent);
		// // 推送
		// push.yinianPushToList(cid, data);
		// }
		// }
		return eventID;

	}	

}