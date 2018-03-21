package yinian.controller;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;

import org.bouncycastle.util.encoders.Base64;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.interceptor.CrossDomain;
import yinian.model.BackupEvent;
import yinian.model.BackupPhoto;
import yinian.model.Encourage;
import yinian.model.Group;
import yinian.model.GroupCanPublish;
import yinian.model.GroupMember;
import yinian.model.LoverTimeMachine;
import yinian.model.Order;
import yinian.model.SmallAppLog;
import yinian.model.TodayMemory;
import yinian.model.User;
import yinian.service.H5Service;
import yinian.service.IMService;
import yinian.service.SimplificationH5Service;
import yinian.service.UserService;
import yinian.service.YinianService;
import yinian.utils.AES;
import yinian.utils.DES;
import yinian.utils.JsonData;
import yinian.utils.QiniuOperate;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;

@Before(CrossDomain.class)
public class H5Controller extends Controller {

	private String jsonString; // 返回的json字符串
	private JsonData jsonData = new JsonData(); // json操作类
	private YinianService service = new YinianService(); // 业务层对象
	private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类
	private H5Service h5Service = new H5Service();
	private IMService im = new IMService();
	// enhance方法对业务层目标进行AOP增强
	YinianService TxService = enhance(YinianService.class);

	/**
	 * 获取系统时间
	 */
	public void GetSystemTime() {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
		String time = df.format(new Date());
		Record record = new Record().set("systemTime", time);
		List<Record> list = new ArrayList<Record>();
		list.add(record);
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * 获取秒杀信息
	 */
	public void ShowGoodsInfo() {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
		String time = df.format(new Date());
		// 判断当前是否有正在进行的秒杀
		List<Record> nowList = Db.find(
				"select goodsID,goodsName,goodsPrice,goodsPicture,goodsBeginTime,goodsEndTime from goods where goodsBeginTime<='"
						+ time + "' and goodsEndTime>='" + time + "' ");
		if (nowList.size() == 0) {
			// 没有秒杀，返回即将进行的秒杀
			List<Record> nextList = Db.find(
					"select goodsID,goodsName,goodsPrice,goodsPicture,goodsBeginTime,goodsEndTime from goods where goodsBeginTime>'"
							+ time + "' limit 2 ");
			if (nextList.size() == 0) {
				// 没有活动了
				jsonString = jsonData.getJson(1000, "无秒杀商品");
			} else {
				// 返回下一个秒杀信息
				jsonString = jsonData.getJson(0, "success", nextList);
			}
		} else {
			// 正在进行秒杀，返回该秒杀信息
			jsonString = jsonData.getJson(0, "success", nowList);
		}
		renderText(jsonString);
	}

	/**
	 * 秒杀商品
	 */
	@Before(Tx.class)
	public void GetGoods() {
		String code = this.getPara("code");
		String goodsID = this.getPara("goodsID");
		try {
			// 解密
			String result = DES.decryptDES(code, "YZadZjYx");
			String[] array = result.split(",");
			// 获取用户id
			String userID = "";
			for (int i = 0; i < array.length; i++) {
				if (((array[i].split("="))[0]).equals("userid")) {
					userID = (array[i].split("="))[1];
				}
			}
			if (userID.equals("") || goodsID.equals("")) {
				jsonString = jsonData.getJson(2, "请求参数错误");
			} else {
				// 获取要秒杀的商品相关信息
				Record goodsRecord = Db
						.findFirst("select goodsNum,goodsLimit,goodsBeginTime,goodsEndTime from goods where goodsID="
								+ goodsID + " ");
				// 判断商品秒杀活动是否未开始或已经结束
				String beginTime = goodsRecord.get("goodsBeginTime").toString();
				String endTime = goodsRecord.get("goodsEndTime").toString();
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm");
				Date now = new Date();
				Date begin = df.parse(beginTime);
				Date end = df.parse(endTime);
				if (now.getTime() < begin.getTime() && now.getTime() > end.getTime()) {
					// 秒杀结束
					jsonString = jsonData.getJson(1001, "该商品秒杀结束");
				} else {
					// 秒杀未结束，判断商品是否已经抢完
					int num = Integer.parseInt(goodsRecord.get("goodsNum").toString());
					int limit = Integer.parseInt(goodsRecord.get("goodsLimit").toString());
					if (num >= limit) {
						// 已抢完
						jsonString = jsonData.getJson(1002, "该商品已被抢完");
					} else {
						// 商品未抢完
						// 判断用户是否已经抢到该商品
						List<Record> judge1 = Db.find("select * from orders where orderUserID=" + userID
								+ " and orderGoodsID=" + goodsID + " ");
						if (judge1.size() != 0) {
							// 用户已抢过该商品
							jsonString = jsonData.getJson(1003, "用户已抢到过该商品");
						} else {
							// 判断用户是否在这一天已经抢到过商品
							String time = df.format(new Date());
							time = time.substring(0, 10);
							List<Record> judge2 = Db.find("select * from orders where orderUserID=" + userID
									+ " and orderTime LIKE '" + time + "%'  ");
							if (judge2.size() != 0) {
								// 这一天已经抢过商品
								jsonString = jsonData.getJson(1004, "用户这天已抢到过商品");
							} else {
								// 秒杀!
								int count = 0;
								count = Db.update(
										"update goods set goodsNum = goodsNum+1 where goodsID=" + goodsID + " ");
								// 生成12位数字识别码
								String base = "0123456789";
								Random random = new Random();
								StringBuffer sb = new StringBuffer();
								for (int i = 0; i < 12; i++) {
									int number = random.nextInt(base.length());
									sb.append(base.charAt(number));
								}
								String verifyCode = sb.toString();
								// 生成订单
								Order order = new Order().set("orderUserID", userID).set("orderGoodsID", goodsID)
										.set("orderVerifyCode", verifyCode);
								if (order.save() && count == 1) {
									// 返回相应数据
									String orderID = order.get("orderID").toString();
									orderID = DES.encryptDES(orderID, "YZadZjYx");
									Record record = new Record().set("orderID", orderID);
									List<Record> list = new ArrayList<Record>();
									list.add(record);
									jsonString = jsonData.getJson(0, "success", list);
								} else {
									jsonString = jsonData.getJson(-50, "数据插入失败");
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			jsonString = jsonData.getJson(2001, "解密失败");
		}
		renderText(jsonString);
	}

	/**
	 * 获取验证信息
	 */
	public void GetVerifyInfo() {
		String code = this.getPara("orderID");
		try {
			String orderID = DES.decryptDES(code, "YZadZjYx");
			List<Record> list = Db.find(
					"select orderTime,orderVerifyCode,unickname,upic,goodsName,goodsPicture,goodsPrice from users,goods,orders where orderUserID=userid and orderGoodsID=goodsID and orderID="
							+ orderID + "  ");
			jsonString = jsonData.getJson(0, "success", list);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			jsonString = jsonData.getJson(2001, "解密失败");
		}
		renderText(jsonString);
	}

	/**
	 * 显示一周内的今日忆
	 */
	public void ShowTodayMemoryInOneWeek() {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
		// 获取今天的日期
		String tomorrow = df.format(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24));
		List<Record> list = Db
				.find("select * from todaymemory where TMtime < '" + tomorrow + "' order by TMtime desc limit 7");
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * 显示三天的今日忆
	 */
	public void ShowTodayMemoryInThreeDay() {
		Date date = new Date();// 取时间
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		calendar.add(Calendar.DATE, 1);// 把日期往后增加一天.整数往后推,负数往前移动
		date = calendar.getTime(); // 这个时间就是日期往后推一天的结果
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		String tomorrow = formatter.format(date);
		List<Record> list = Db.find("select TMpic,TMtext from todaymemory where date(TMtime) <= '" + tomorrow
				+ "' order by TMtime desc limit 3");
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * 上传今日忆
	 */
	public void UploadTodayMemory() {
		String picUrl = this.getPara("picUrl");
		String audioUrl = this.getPara("audioUrl");
		String text = this.getPara("text");
		String date = this.getPara("date") + " 20:00:00";

		TodayMemory tm = new TodayMemory().set("TMpic", picUrl).set("TMaudio", audioUrl).set("TMtext", text)
				.set("TMtime", date);
		boolean flag = tm.save();
		jsonString = dataProcess.insertFlagResult(flag);
		renderText(jsonString);
	}

	/**
	 * 删除今日忆
	 */
	public void DeleteTodayMemory() {
		String TMid = this.getPara("TMid");
		TodayMemory tm = new TodayMemory().findById(TMid);
		boolean flag = tm.delete();
		jsonString = dataProcess.deleteFlagResult(flag);
		renderText(jsonString);
	}

	/**
	 * 显示网页内相册信息
	 */
	public void ShowWebAlbumInformation() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		// 此处的groupid是移动端传到web端的加密数据，需对其进行解密获取明文的groupid
		groupid = dataProcess.decryptData(groupid, "groupid");
		System.out.println(groupid);
		List<Record> list = service.getSingleOfficialAlbumInfo(userid, groupid);
		if (list.size() == 0) {
			jsonString = jsonData.getJson(1012, "相册已被删除");
		} else {
			jsonString = jsonData.getJson(0, "success", list);
		}
		renderText(jsonString);
	}

	/**
	 * 显示网页内相册动态内容
	 */
	public void ShowWebAlbumContents() {
		String userid = this.getPara("userid");
		String sign = this.getPara("sign");
		String gtype = this.getPara("gtype");
		String eventid = this.getPara("eventid");
		String groupid = this.getPara("groupid");
		// 此处的groupid是移动端传到web端的加密数据，需对其进行解密获取明文的groupid
		groupid = dataProcess.decryptData(groupid, "groupid");
		if (gtype.equals("官方相册")) {
			jsonString = service.getOfficialAlbumEvents(userid, groupid, eventid, sign);
		} else {
			jsonString = service.getPrivateAlbumEvents(userid, groupid, eventid, sign);
		}
		renderText(jsonString);
	}

	/**
	 * 申请进入相册，网页端接口
	 */
	@Before(Tx.class)
	public void EnterAlbum() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		// 此处的groupid是移动端传到web端的加密数据，需对其进行解密获取明文的groupid
		groupid = dataProcess.decryptData(groupid, "groupid");
		System.out.println(groupid);
		// 判断用户是否在组内
		List<Record> list = Db
				.find("select * from groupmembers where gmgroupid=" + groupid + " and gmuserid=" + userid + " ");
		// 获取进入组之前改组所有成员ID，留作通知
		List<Record> groupmemberList = service.getGroupMemberID(groupid);
		// 获取相册类型
		List<Record> groupTypeList = Db.find("select gtype from groups where groupid=" + groupid + " ");
		String gtype = groupTypeList.get(0).get("gtype").toString();
		if (list.size() == 0) {
			// 通过判断组类型来决定要调用哪个通知方法
			boolean insertFlag = false;
			boolean NotificationFlag = false;
			if (gtype.equals("5")) {
				TxService.enterOfficialAlbum(groupid, userid);// 进组并返回组信息用于用户添加到他的组列表中
				// 插入数据到likes表中
				insertFlag = TxService.newUserJoinInsertLikes(userid, groupid);
				NotificationFlag = TxService.enterOfficialAlbumNotification(groupid, userid);
			} else {
				TxService.enterGroup(groupid, userid);
				NotificationFlag = TxService.insertEnterNotification(groupid, userid, groupmemberList);
				insertFlag = true;
			}
			if (NotificationFlag && insertFlag) {
				jsonString = jsonData.getJson(0, "success");
			} else {
				jsonString = jsonData.getJson(-51, "更新数据失败");
			}
		} else {
			jsonString = jsonData.getJson(1010, "用户已在组内");
		}
		renderText(jsonString);
	}

	/**
	 * 获取邀请人信息
	 */
	public void GetInviteInfo() {
		String userid = this.getPara("userid");
		String code = this.getPara("code");
		// 对code进行解密
		String inviteUserid = dataProcess.decryptData(code, "userid");
		String groupid = dataProcess.decryptData(code, "groupid");
		System.out.println(inviteUserid);
		System.out.println(groupid);
		// 获取相关数据
		Record userRecord = Db.findFirst("select unickname,upic from users where userid=" + inviteUserid + " ");
		Record groupRecord = Db.findFirst("select gtype,gname from groups where groupid=" + groupid + " ");
		// 判断用户是否已在组内
		List<Record> judge = Db.find("select * from groupmembers where gmuserid=" + userid + " and gmgroupid=" + groupid
				+ " and gmstatus=0");
		if (judge.size() == 0) {
			userRecord.set("isInAlbum", 0);
		} else {
			userRecord.set("isInAlbum", 1);
		}
		// 整合数据
		userRecord.set("gtype", groupRecord.get("gtype").toString()).set("gname", groupRecord.get("gname").toString());
		List<Record> list = new ArrayList<Record>();
		list.add(userRecord);
		// 将组类型转换成相应的文字
		list = dataProcess.changeGroupTypeIntoWord(list);
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * 获取用户加入的所有情侣相册
	 */
	public void GetUserAllCoupleAlbums() {
		String userid = this.getPara("userid");
		userid = dataProcess.decryptData(userid, "userid");
		List<Record> list = Db.find(
				"select groupid,gname,gpic from groups,groupmembers where groupid=gmgroupid and gtype=3 and gmuserid="
						+ userid + " and gstatus=0 and gmstatus=0 ");
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * 生成情侣时光机
	 */
	public void CreateLoverTimeMachine() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String name = this.getPara("name");
		String memoryTime = this.getPara("memoryTime");
		userid = dataProcess.decryptData(userid, "userid");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String time = sdf.format(new Date());
		List<Record> list = Db
				.find("select ltmID,ltmLoverName,ltmMemoryTime,ltmGroupID from lovertimemachine where ltmUserid="
						+ userid + " and ltmStatus=0 ");

		if (list.size() == 0) {
			LoverTimeMachine ltm = new LoverTimeMachine().set("ltmUserID", userid).set("ltmGroupID", groupid)
					.set("ltmLoverName", name).set("ltmMemoryTime", memoryTime);
			// 判断相册中是否已有倒计时，没有则进行添加
			boolean groupFlag = true;
			Record record = Db.findFirst("select grecordtime from groups where groupid=" + groupid + " ");
			if (record.get("grecordtime") == null) {
				Group group = new Group().findById(groupid).set("grecordtime", time);
				groupFlag = group.update();
			}
			boolean ltmFlag = false;
			ltmFlag = ltm.save();
			if (ltmFlag && groupFlag) {
				jsonString = jsonData.getJson(0, "success");
			} else {
				jsonString = jsonData.getJson(-51, "更新数据失败");
			}
		} else {
			jsonString = jsonData.getJson(2005, "用户已创建情侣时光机");
		}
		renderText(jsonString);
	}

	/**
	 * 显示情侣时光机
	 */
	public void ShowLoverTimeMachine() {
		String userid = this.getPara("userid");
		userid = dataProcess.decryptData(userid, "userid");
		System.out.println(userid);
		List<Record> list = Db.find(
				"select ltmID,ltmLoverName,ltmMemoryTime,ltmGroupID,unickname from lovertimemachine,users where userid=ltmUserid and ltmUserid="
						+ userid + " and ltmStatus=0 ");
		if (list.size() == 0) {
			jsonString = jsonData.getJson(2004, "用户未创建情侣时光机");
		} else {
			String groupid = list.get(0).get("ltmGroupID").toString();
			Record record = Db.findFirst("select grecordtime,gtime from groups where groupid=" + groupid + " ");
			List<Record> picList = Db.find(
					"select count(*) as picNum from groups,events,pictures where groupid=egroupid and eid=peid and groupid="
							+ groupid + " group by groupid ");
			if (picList.size() == 0) {
				list.get(0).set("pictureNum", 0);
			} else {
				list.get(0).set("pictureNum", picList.get(0).get("picNum"));
			}
			list.get(0).set("grecordtime", record.get("grecordtime").toString()).set("gtime",
					record.get("gtime").toString());
			jsonString = jsonData.getJson(0, "success", list);
		}
		renderText(jsonString);
	}

	/**
	 * 显示单条动态,加密版
	 */
	public void ShowSingleEvent() {
		// 获取参数
		String code = this.getPara("code");
		String eventid = dataProcess.decryptData(code, "eventid");
		jsonString = service.getSingleEvent(eventid);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 删除单张备份照片，web版本
	 */
	public void DeleteSingleBackupPhoto() {
		String backupPhotoID = this.getPara("backupPhotoID");
		int count = Db.update("update backupphoto set backupPStatus=1 where backupPhotoID in(" + backupPhotoID + ") ");
		// 判断是否为最后一张
		BackupPhoto bup = new BackupPhoto().findById(backupPhotoID);
		String bupEid = bup.get("backupPEid").toString();
		List<Record> judge = Db.find("select * from backupphoto where backupPEid=" + bupEid + " and backupPStatus=0 ");
		if (judge.size() == 0) {
			BackupEvent bue = new BackupEvent().findById(bupEid);
			bue.set("backupStatus", 1);
			bue.update();
		}

		jsonString = dataProcess.updateFlagResult(1 == count);
		renderText(jsonString);
	}

	/**
	 * 展示照片墙
	 */
	public void ShowPhotoWall() {
		String type = this.getPara("type");
		String code = this.getPara("code");

		String groupid = "";
		try {
			code = DES.decryptDES(code, CommonParam.DESSecretKey);
			String[] array = code.split(",");
			// 获取groupid
			for (int i = 0; i < array.length; i++) {
				if (((array[i].split("="))[0]).equals("groupid")) {
					groupid = (array[i].split("="))[1];
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int id = Integer.parseInt(this.getPara("id"));
		List<Record> list = h5Service.getPhotoWall(type, groupid, id);
		jsonString = jsonData.getJson(0, "success", list);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * PC端注册登录
	 */
	public void PCLoginAndRegister() {
		// 接收参数
		String code = this.getPara("code");
		// 用户来源客户端及对应版本
		String source = this.getPara("source");
		String version = this.getPara("version");
		// 记录用户来源端口参数
		String port = this.getPara("port");
		port = (port == null ? "自然渠道" : port);
		String fromUserID = this.getPara("fromUserID");
		String fromSpaceID = this.getPara("fromSpaceID");
		String fromEventID = this.getPara("fromEventID");

		// 根据登录的code向微信服务器发送请求，获取用户的access_token和openid
		String result = dataProcess.sentNetworkRequest(CommonParam.getPCAccessToken + code);
		if (result.equals("")) {
			jsonString = jsonData.getJson(1047, "连接微信服务器失败");
		} else {
			JSONObject jo = JSONObject.parseObject(result);
			String access_token = jo.getString("access_token");
			String openid = jo.getString("openid");

			// 获取用户微信信息与unionid
			result = dataProcess.sentNetworkRequest(
					CommonParam.getWechatUserInfoUrl + "?access_token=" + access_token + "&openid=" + openid);
			if (result.equals("")) {
				jsonString = jsonData.getJson(1047, "连接微信服务器失败");
			} else {
				jo = JSONObject.parseObject(result);
				String nickname = jo.getString("nickname");
				String unionid = jo.getString("unionid");
				String headimgurl = jo.getString("headimgurl");
				String sex = String.valueOf(jo.getIntValue("sex"));
				String province = jo.getString("province");
				String city = jo.getString("city");

				// 进行登录或注册判断
				List<Record> list = service.judgeUserQQorWechatID(unionid, "wechat");
				if (list.size() == 0) {
					// 该微信用户未注册，注册后返回userid
					String userid = service.wechatUserRegister(unionid, nickname, headimgurl, sex, "PC", province, city,
							version, port, fromUserID, fromSpaceID, fromEventID, null);
					if (userid.equals("")) {
						jsonString = jsonData.getJson(1016, "微信用户注册失败");
					} else {
						// 创建三个系统引导相册
						boolean createFlag1 = TxService.creatDefaultAlbum(userid, "6", "小程序");
						boolean createFlag2 = TxService.creatDefaultAlbum(userid, "7", "小程序");
						boolean createFlag3 = TxService.creatDefaultAlbum(userid, "8", "小程序");
						if (createFlag1 && createFlag2 && createFlag3) {
							// 注册环信用户
							im.AddSingleIMUser(userid, nickname);

							Record record = new Record();
							record.set("userid", userid).set("unickname", nickname);
							list.add(record);
							jsonString = jsonData.getJson(0, "success", list);
						} else {
							jsonString = jsonData.getJson(-50, "插入数据失败");
						}
					}
				} else {
					// 该微信用户已注册，添加用户历史访问记录，添加成功后登录并且返回userid
					String userid = list.get(0).get("userid").toString();
					UserService userService = new UserService();
					if (userService.AddHistoryAccessInfo(userid, source, version, port, null)) {
						// 判断是否有APP端登录官网激励机制奖励
						List<Record> encourage = Encourage.getAppEncourageInfo(userid);
						int appLoginWeb = Integer.parseInt(encourage.get(0).get("appLoginWeb").toString());
						if (appLoginWeb == 0)
							Encourage.SetOneFieldCanBeGet(userid, "appLoginWeb");
						// 返回结果
						list.get(0).set("unickname", nickname);
						jsonString = jsonData.getJson(0, "success", list);
					} else {
						jsonString = jsonData.getJson(-51, "更新数据库数据失败");
					}
				}

			}

		}
		renderText(jsonString);
	}
	/**
	 * 小程序登录注册 by lk 发布默认动态
	 */
	public void SmallAppLoginAndRegisterByLk() {
		String code = this.getPara("code");
		String encodeData = this.getPara("encodeData");
		String iv = this.getPara("iv");
		// 用户来源客户端及对应版本
		String source = this.getPara("source");
		source = (source == null ? "小程序" : source);
		String version = this.getPara("version");
		// 记录用户来源端口参数
		String port = this.getPara("port");
		port = ((port == null || port.equals("")) ? "自然渠道" : port);
		String fromUserID = this.getPara("fromUserID");
		String fromSpaceID = this.getPara("fromSpaceID");
		String fromEventID = this.getPara("fromEventID");

		// 根据登录的code像微信服务器发送请求，获取用户的session_key和openid
		String result = "";

		switch (source) {
		case "小程序":
			result = dataProcess.sentNetworkRequest(CommonParam.getWechatSessionKeyUrl + code);
			break;
		case "玩图小程序":
			result = dataProcess.sentNetworkRequest(CommonParam.playImageGetWechatSessionKeyUrl + code);
			break;
		case "测试小程序":
			result = dataProcess.sentNetworkRequest(CommonParam.testGetWechatSessionKeyUrl + code);
			break;
		case "精简版小程序":
			result = dataProcess.sentNetworkRequest(CommonParam.jjGetWechatSessionKeyUrl + code);
			break;
		default:
			result = dataProcess.sentNetworkRequest(CommonParam.getWechatSessionKeyUrl + code);
			break;
		}

		JSONObject jo = JSONObject.parseObject(result);
		String session_key = jo.getString("session_key");
		String openid = jo.getString("openid");

		// 数据解密
		AES aes = new AES();
		byte[] resultByte;
		String userInfo = "";
		if(null!=encodeData&&null!=session_key&&null!=iv){
			try {
				resultByte = aes.decrypt(Base64.decode(encodeData), Base64.decode(session_key), Base64.decode(iv));
				if (null != resultByte && resultByte.length > 0) {
					userInfo = new String(resultByte, "UTF-8");
				}
			} catch (InvalidAlgorithmParameterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (userInfo.equals("")) {
			jsonString = jsonData.getJson(1014, "解密失败");
		} else {
			// 解析数据
			jo = JSONObject.parseObject(userInfo);
			String wechatNickname = jo.getString("nickName");
			String wechatPic = jo.getString("avatarUrl");
			String wechatSex = String.valueOf(jo.getIntValue("gender"));
			String province = jo.getString("province");
			String city = jo.getString("city");
			String unionID = jo.getString("unionId");

			// 进行登录或注册判断
			List<Record> list = service.judgeUserQQorWechatID(unionID, "wechat");
			if (list.size() == 0) {
				// 该微信用户未注册，注册后返回userid
				String userid = service.wechatUserRegister(unionID, wechatNickname, wechatPic, wechatSex, source,
						province, city, version, port, fromUserID, fromSpaceID, fromEventID, openid);
				if (userid.equals("")) {
					jsonString = jsonData.getJson(1016, "微信用户注册失败");
				} else {
					/**
					 * 1000w照片活动
					 */
					if(CommonParam.pOpenJoinGroup){
						GroupMember gm = new GroupMember();
						boolean isInFlag = gm.judgeUserIsInTheAlbum(Integer.parseInt(userid), CommonParam.pGroupId6); // true时用户不在空间内
						if(isInFlag){
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							String time = sdf.format(new Date());
							gm = new GroupMember().set("gmgroupid", CommonParam.pGroupId6).set("gmuserid", userid).set("gmPort", port)
									.set("gmFromUserID", fromUserID).set("isTop", 1).set("topTime", time);
							gm.save();
						}
						service.SetGroupIsTop(userid, CommonParam.pGroupId6+"", "yes");
					}
					/**
					 * 1000w照片活动 end
					 */
					// 创建两个系统引导相册
					//boolean createFlag1 = TxService.creatDefaultAlbumAndPulishMsg(userid, "6", "小程序");
					//boolean createFlag2 = TxService.creatDefaultAlbumAndPulishMsg(userid, "8", "小程序");
					boolean createFlag1 = true;
					boolean createFlag2 = true;
//					if(source.equals("精简版小程序")){
//						createFlag1 = TxService.creatDefaultAlbum(userid, "9", "精简版小程序");
//						createFlag1 = TxService.creatDefaultAlbum(userid, "10", "精简版小程序");
//					}else{
//						createFlag1 = TxService.creatDefaultAlbumAndPulishMsg(userid, "6", "小程序");
//						createFlag2 = TxService.creatDefaultAlbumAndPulishMsg(userid, "8", "小程序");
//					}
					// boolean createFlag3 = TxService.creatDefaultAlbum(userid, "8", "小程序");
					if (createFlag1 && createFlag2) {
						// 注册环信用户
						im.AddSingleIMUser(userid, wechatNickname);

						Record record = new Record();
						record.set("userid", userid).set("isNewUser", "yes").set("unickname", wechatNickname)
								.set("uLockPass", null).set("openIdFlag", "true");
						list.add(record);
						jsonString = jsonData.getJson(0, "success", list);
					} else {
						jsonString = jsonData.getJson(-50, "插入数据失败");
					}
				}
			} else {
				// 该微信用户已注册，添加用户历史访问记录，添加成功后登录并且返回userid
				String userid = list.get(0).get("userid").toString();
				//by lk 修改用户头像
				if(list.get(0).get("upic").toString().indexOf("https://wx.qlogo.cn/mmopen/")!=-1){
					User u=new User();
					u.set("userid", userid);
					u.set("unickname", wechatNickname);
					u.set("upic", wechatPic);
					u.update();
				}
				// lk end 
				UserService userService = new UserService();
				if (userService.AddHistoryAccessInfo(userid, source, version, port, openid)) {
					list.get(0).set("isNewUser", "no").set("unickname", wechatNickname)
							.set("uLockPass", list.get(0).get("uLockPass")).set("openIdFlag", "true");
					/**
					 * 1000w照片活动
					 */
					if(CommonParam.pOpenJoinGroup){
						GroupMember gm = new GroupMember();
						boolean isInFlag = gm.judgeUserIsInTheAlbum(Integer.parseInt(userid), CommonParam.pGroupId6); // true时用户不在空间内
						if(isInFlag){
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
							String time = sdf.format(new Date());
							gm = new GroupMember().set("gmgroupid", CommonParam.pGroupId6).set("gmuserid", userid).set("gmPort", port)
									.set("gmFromUserID", fromUserID).set("isTop", 1).set("topTime", time);
							gm.save();
						}
						service.SetGroupIsTop(userid, CommonParam.pGroupId6+"", "yes");
					}
					/**
					 * 1000w照片活动 end
					 */
					jsonString = jsonData.getJson(0, "success", list);
				} else {
					jsonString = jsonData.getJson(-51, "更新数据库数据失败");
				}
			}
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 小程序登录注册
	 */
	public void SmallAppLoginAndRegister() {
		String code = this.getPara("code");
		String encodeData = this.getPara("encodeData");
		String iv = this.getPara("iv");
		// 用户来源客户端及对应版本
		String source = this.getPara("source");
		source = (source == null ? "小程序" : source);
		String version = this.getPara("version");
		// 记录用户来源端口参数
		String port = this.getPara("port");
		port = ((port == null || port.equals("")) ? "自然渠道" : port);
		String fromUserID = this.getPara("fromUserID");
		String fromSpaceID = this.getPara("fromSpaceID");
		String fromEventID = this.getPara("fromEventID");

		// 根据登录的code像微信服务器发送请求，获取用户的session_key和openid
		String result = "";

		switch (source) {
		case "小程序":
			result = dataProcess.sentNetworkRequest(CommonParam.getWechatSessionKeyUrl + code);
			break;
		case "玩图小程序":
			result = dataProcess.sentNetworkRequest(CommonParam.playImageGetWechatSessionKeyUrl + code);
			break;
		case "测试小程序":
			result = dataProcess.sentNetworkRequest(CommonParam.testGetWechatSessionKeyUrl + code);
			break;
		case "精简版小程序":
			result = dataProcess.sentNetworkRequest(CommonParam.jjGetWechatSessionKeyUrl + code);
			break;
		default:
			result = dataProcess.sentNetworkRequest(CommonParam.getWechatSessionKeyUrl + code);
			break;
		}

		JSONObject jo = JSONObject.parseObject(result);
		String session_key = jo.getString("session_key");
		String openid = jo.getString("openid");

		// 数据解密
		AES aes = new AES();
		byte[] resultByte;
		String userInfo = "";
		try {
			resultByte = aes.decrypt(Base64.decode(encodeData), Base64.decode(session_key), Base64.decode(iv));
			if (null != resultByte && resultByte.length > 0) {
				userInfo = new String(resultByte, "UTF-8");
			}
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (userInfo.equals("")) {
			jsonString = jsonData.getJson(1014, "解密失败");
		} else {
			// 解析数据
			jo = JSONObject.parseObject(userInfo);
			String wechatNickname = jo.getString("nickName");
			String wechatPic = jo.getString("avatarUrl");
			String wechatSex = String.valueOf(jo.getIntValue("gender"));
			String province = jo.getString("province");
			String city = jo.getString("city");
			String unionID = jo.getString("unionId");

			// 进行登录或注册判断
			List<Record> list = service.judgeUserQQorWechatID(unionID, "wechat");
			if (list.size() == 0) {
				// 该微信用户未注册，注册后返回userid
				String userid = service.wechatUserRegister(unionID, wechatNickname, wechatPic, wechatSex, source,
						province, city, version, port, fromUserID, fromSpaceID, fromEventID, openid);
				if (userid.equals("")) {
					jsonString = jsonData.getJson(1016, "微信用户注册失败");
				} else {
					// 创建两个系统引导相册
					boolean createFlag1 = false;
					boolean createFlag2 = false;
					if(source.equals("精简版小程序")){
						createFlag1 = TxService.creatDefaultAlbum(userid, "9", "精简版小程序");
						createFlag1 = TxService.creatDefaultAlbum(userid, "11", "精简版小程序");
					}else{
						createFlag1 = TxService.creatDefaultAlbum(userid, "6", "小程序");
						createFlag2 = TxService.creatDefaultAlbum(userid, "8", "小程序");
					}
//					boolean createFlag1 = TxService.creatDefaultAlbum(userid, "6", "小程序");
//					boolean createFlag2 = TxService.creatDefaultAlbum(userid, "7", "小程序");
					// boolean createFlag3 = TxService.creatDefaultAlbum(userid, "8", "小程序");
					if (createFlag1 && createFlag2) {
						// 注册环信用户
						im.AddSingleIMUser(userid, wechatNickname);

						Record record = new Record();
						record.set("userid", userid).set("isNewUser", "yes").set("unickname", wechatNickname)
								.set("uLockPass", null).set("openIdFlag", "true");
						list.add(record);
						jsonString = jsonData.getJson(0, "success", list);
					} else {
						jsonString = jsonData.getJson(-50, "插入数据失败");
					}
				}
			} else {
				// 该微信用户已注册，添加用户历史访问记录，添加成功后登录并且返回userid
				String userid = list.get(0).get("userid").toString();
				User u=new User();
				u.set("userid", userid);
				u.set("upic", wechatPic);
				u.update();
				UserService userService = new UserService();
				if (userService.AddHistoryAccessInfo(userid, source, version, port, openid)) {
					list.get(0).set("isNewUser", "no").set("unickname", wechatNickname)
							.set("uLockPass", list.get(0).get("uLockPass")).set("openIdFlag", "true");
					jsonString = jsonData.getJson(0, "success", list);
				} else {
					jsonString = jsonData.getJson(-51, "更新数据库数据失败");
				}
			}
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 记录小程序错误信息
	 */
	public void RecordSmallAppFaultMsg() {
		String msg = this.getPara("msg");
		String device = this.getPara("device");
		SmallAppLog sal = new SmallAppLog().set("msg", msg).set("device", device);
		jsonString = dataProcess.insertFlagResult(sal.save());
		renderText(jsonString);
	}

	/**
	 * 设置或取消锁屏密码
	 */
	public void SetOrCancelLockPass() {
		String userid = this.getPara("userid");
		String password = this.getPara("password");
		String type = this.getPara("type");

		User user = new User().findById(userid);

		switch (type) {
		case "set":
			user.set("uLockPass", password);
			break;
		case "cancel":
			user.set("uLockPass", null);
			break;
		}

		jsonString = dataProcess.updateFlagResult(user.update());
		renderText(jsonString);

	}
	/*
	 * by lk 活动提示
	 */
	public void GetActivityMsg(){
		String userid=this.getPara("userid")==null?"":this.getPara("userid");
		H5Service service=new H5Service();
		if(userid.equals("")){		
			jsonString = jsonData.getJson(2, "请求参数错误");
		}else{
			jsonString = jsonData.getJson(0, "success", service.GetActivityMsg(userid));
		}
		renderText(jsonString);
	}
	/**
	 * by lk 记录活动提示显示状态
	 */
	public void SetUserJoinActivity(){
		String userid=this.getPara("userid")==null?"":this.getPara("userid");
		String activityId=this.getPara("activityId")==null?"":this.getPara("activityId");
		H5Service service=new H5Service();
		if(userid.equals("")||activityId.equals("")||activityId.equals("0")){		
			jsonString = jsonData.getJson(2, "请求参数错误");
		}else{
			service.SetActivityMsg(userid,activityId);
			jsonString = jsonData.getJson(0, "success");
		}
		renderText(jsonString);
	}
	/**
	 * 获取相册最分享图片
	 */
	public void GetGroupPic(){
//		System.out.println("123");
		QiniuOperate operate = new QiniuOperate();
		String groupid=this.getPara("groupid")==null?"":this.getPara("groupid");
		String userid=this.getPara("userid")==null?"":this.getPara("userid");
		List<Record> returnList=new ArrayList<>();
		List<Record> picList=new ArrayList<>();
		Record r=new Record();
		Record pr=new Record();
		if(!groupid.equals("")&&!userid.equals("")){	
			Group group=new Group().findById(groupid);
			r.set("groupname",group.get("gname"));
			User user=new User().findById(userid);
			r.set("unickname", user.get("unickname"));
			r.set("upic", user.get("upic"));
			pr.set("poriginal", "http://7xlmtr.com1.z0.glb.clouddn.com/20180313_3.png");
			pr.set("thumbnail", "http://7xlmtr.com1.z0.glb.clouddn.com/20180313_3.png");
			pr.set("midThumbnail", "http://7xlmtr.com1.z0.glb.clouddn.com/20180313_3.png");
			picList.add(pr);		
			r.set("picList", picList);			
			returnList.add(r);
			jsonString = jsonData.getSuccessJson(returnList);		
		}else{
			jsonString = jsonData.getJson(2, "请求参数错误");
		}
		renderText(jsonString);
	}
	/**
	 * 获取相册最后一个动态的1张图片
	 */
	public void GetGroupLastPic(){
//		System.out.println("123");
		QiniuOperate operate = new QiniuOperate();
		String groupid=this.getPara("groupid")==null?"":this.getPara("groupid");
		String userid=this.getPara("userid")==null?"":this.getPara("userid");
		List<Record> returnList=new ArrayList<>();
		List<Record> picList=new ArrayList<>();
		Record r=new Record();
		if(!groupid.equals("")&&!userid.equals("")){	
			Group group=new Group().findById(groupid);
			r.set("groupname",group.get("gname"));
			User user=new User().findById(userid);
			r.set("unickname", user.get("unickname"));
			r.set("upic", user.get("upic"));
			List<Record> eventList=Db.find("select eid from events where eMain=0 and estatus=0 and egroupid="+groupid+" order by eid desc limit 0,1");
			//System.out.println("eid="+eventList.get(0).getLong("eid").intValue());
			if(!eventList.isEmpty()){
				List<Record> pList=Db.find("select poriginal from pictures where pstatus = 0 and peid ="+eventList.get(0).getLong("eid").intValue()+" order by puploadtime desc limit 0,1");
				if(!pList.isEmpty()){
					for(Record pic:pList ){
						//System.out.println("poriginal="+pic.getStr("poriginal"));
						Record p=new Record();
						p.set("poriginal",operate.getDownloadToken(pic.getStr("poriginal")));
						p.set("thumbnail", operate
								.getDownloadToken(pic.getStr("poriginal") + "?imageView2/2/w/250"));
						// 中等缩略图授权
						p.set("midThumbnail", operate
								.getDownloadToken(pic.getStr("poriginal") + "?imageView2/2/w/1000"));
						picList.add(p);	
					}								
				}
				r.set("picList", picList);
				returnList.add(r);
				jsonString = jsonData.getSuccessJson(returnList);
			}else{
				Record p=new Record();
				p.set("poriginal",group.get("gpic"));
				p.set("thumbnail", group.get("gpic"));
				// 中等缩略图授权
				p.set("midThumbnail", group.get("gpic"));
				picList.add(p);
				r.set("picList", picList);			
				returnList.add(r);
				jsonString = jsonData.getSuccessJson(returnList);
			}
		}else{
			jsonString = jsonData.getJson(2, "请求参数错误");
		}
		renderText(jsonString);
	}
//	/**
//	 * 活动相册 帮助中心内容
//	 */
//	public void getActivityDialog(){
//		String groupid=this.getPara("groupid");
//		if(null!=groupid&&!groupid.equals("")){
//			//String
//			List<Record> list=new GroupCanPublish().getDialogByGroupid(groupid, "2");
//			if(null!=list&&!list.isEmpty()){
//				Record r=list.get(0);
//				r.remove("id");
//				r.remove("pGroupType");
//				r.remove("pStatus");
//				r.remove("createTime");
//			}
//			jsonString = jsonData.getSuccessJson(list);
//		}else{
//			jsonString = jsonData.getJson(2, "请求参数错误");
//		}
//		renderText(jsonString);
//	}
	/*
	 * 判断用户是否自主创建相册
	 */
	public void getUserCreateGroupCnt(){
		jsonString = jsonData.getJson(2, "请求参数错误");
		String userid=this.getPara("userid");
		if(null!=userid&&!userid.equals("")){
			jsonString = jsonData.getSuccessJson(h5Service.getUserCreateGroupCnt(userid));
		}
		renderText(jsonString);
	}
	/**
	 * 根据图片ID删除图片，若用户删除了一个动态内所有的图片则删除动态
	 */
	public void deletePic(){
		jsonString = jsonData.getJson(2, "请求参数错误");
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String pid = this.getPara("pid")==null?"":this.getPara("pid");
		String source = this.getPara("source");
		if(!pid.equals("")){			
			List<Record> list=h5Service.deletePic(userid,pid);
				jsonString = jsonData.getSuccessJson(list);			
		}
		System.out.println("jsonString:"+jsonString);
		renderText(jsonString);
	}
}
