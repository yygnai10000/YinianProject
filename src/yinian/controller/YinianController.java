package yinian.controller;

import java.io.IOException;
import java.net.URLDecoder;
import java.text.Collator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import yinian.app.YinianDAO;
import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.interceptor.CrossDomain;
import yinian.model.Encourage;
import yinian.model.Event;
import yinian.model.FormID;
import yinian.model.Gift;
import yinian.model.Group;
import yinian.model.GroupCanPublish;
import yinian.model.GroupMember;
import yinian.model.InviteGroup;
import yinian.model.Like;
import yinian.model.Mark;
import yinian.model.Note;
import yinian.model.Notification;
import yinian.model.Push;
import yinian.model.Puzzle;
import yinian.model.Receiver;
import yinian.model.SNSUserInfo;
import yinian.model.Temp;
import yinian.model.TodayMemory;
import yinian.model.User;
import yinian.model.View;
import yinian.model.WeixinOauth2Token;
import yinian.push.PushMessage;
import yinian.push.SmallAppPush;
import yinian.push.YinianGetuiPush;
import yinian.service.IMService;
import yinian.service.PointsService;
import yinian.service.UserService;
import yinian.service.YinianService;
import yinian.thread.Yellow;
import yinian.utils.DES;
import yinian.utils.JsonData;
import yinian.utils.QiniuOperate;
import yinian.utils.RedisUtils;
import yinian.utils.SmallAppQRCode;
import yinian.utils.WechatOauth;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.log.Logger;
import com.jfinal.plugin.activerecord.ActiveRecordException;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.jfinal.plugin.ehcache.CacheKit;

import redis.clients.jedis.Jedis;

public class YinianController extends Controller {

	private String jsonString; // 返回的json字符串
	private JsonData jsonData = new JsonData(); // json操作类
	private YinianService service = new YinianService();// 业务层对象
	private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类
	private QiniuOperate operate = new QiniuOperate(); // 七牛云处理类
	// enhance方法对目标进行AOP增强
	private YinianService TxService = enhance(YinianService.class);
	private YinianGetuiPush push = new YinianGetuiPush(); // 个推推送类
	private YinianDAO dao = new YinianDAO();
	private static final Logger log = Logger.getLogger(YinianController.class);
	private IMService im = new IMService();

	public void version() {
		renderText("3.4.77");
	}

	/**
	 * 测试接口
	 * 
	 * @throws Exception
	 */
	public void test() throws IOException {

		/**
		 * 模拟生成openID
		 */
		// String base =
		// "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		// Random random = new Random();
		// String openID = "TestRegister";
		// StringBuffer sb = new StringBuffer();
		//
		// for (int i = 0; i < 10; i++) {
		// int number = random.nextInt(base.length());
		// sb.append(base.charAt(number));
		// }
		// openID += sb.toString();// inviteCode为随机生成的邀请码
		// List<Record> list = dataProcess.makeSingleParamToList("openID", openID);
		// jsonString = jsonData.getSuccessJson(list);
		// renderText(jsonString);

		/**
		 * 照片数小于1000张空间鉴黄
		 */
		// System.out.println("开始获取所有空间信息");
		// List<Record> spaceList = Db.find(
		// "select groupid,count(*) from groups,`events`,pictures where groupid=egroupid
		// and eid=peid and gstatus=0 and estatus=0 and pstatus=0 GROUP BY groupid
		// HAVING count(*)<=1000");
		// System.out.println("已获取到所有空间信息，共有" + spaceList.size() + "个空间满足条件");
		// System.out.println("开始鉴黄");
		//
		// Yellow y1 = new Yellow(spaceList, 0);
		// Yellow y2 = new Yellow(spaceList, 3200);
		// Yellow y3 = new Yellow(spaceList, 6400);
		// Yellow y4 = new Yellow(spaceList, 9600);
		// Yellow y5 = new Yellow(spaceList, 12800);
		// Yellow y6 = new Yellow(spaceList, 16000);
		// Yellow y7 = new Yellow(spaceList, 19200);
		// Yellow y8 = new Yellow(spaceList, 22400);
		// Yellow y9 = new Yellow(spaceList, 25600);
		// Yellow y10 = new Yellow(spaceList, 28800);
		// Yellow y11 = new Yellow(spaceList, 32000);
		// Yellow y12 = new Yellow(spaceList, 35200);
		// Yellow y13 = new Yellow(spaceList, 38400);
		// Yellow y14 = new Yellow(spaceList, 41600);
		// Yellow y15 = new Yellow(spaceList, 44800);
		// Yellow y16 = new Yellow(spaceList, 48000);
		// Yellow y17 = new Yellow(spaceList, 51200);
		// Yellow y18 = new Yellow(spaceList, 54400);
		// Yellow y19 = new Yellow(spaceList, 57600);
		// Yellow y20 = new Yellow(spaceList, 60800);
		//
		// y1.start();
		// y2.start();
		// y3.start();
		// y4.start();
		// y5.start();
		// y6.start();
		// y7.start();
		// y8.start();
		// y9.start();
		// y10.start();
		// y11.start();
		// y12.start();
		// y13.start();
		// y14.start();
		// y15.start();
		// y16.start();
		// y17.start();
		// y18.start();
		// y19.start();
		// y20.start();
		//
		// System.out.println("结束鉴黄");

		/**
		 * 获取链接访问地址
		 */
		// String address =
		// "http://photo.zhuiyinanian.com/tmp_ac42b8c65edb099c92eb13ba703eda06434dad01229b91c6.png?imageView2/2/w/300";
		// address = operate.getDownloadToken(address);
		// System.out.println(address);

		/**
		 * 私人相册转移
		 */
		// 获取备份者列表
		// List<Record> userList = Db
		// .find("select DISTINCT backupUserID from backupevent where backupStatus=0
		// limit 670,1000 ");
		// int size = userList.size();
		// System.out.println("一共有" + size + "个用户需要转移");
		// // 逐个转移
		// for (int i = 0; i < size; i++) {
		// String userid = userList.get(i).get("backupUserID").toString();
		// System.out.println("开始转移第" + (i + 1) + "个用户照片");
		// TxService.Test(userid);
		// System.out.println("结束转移第" + (i + 1) + "个用户照片");
		// }
		// System.out.println("转移结束");

		/**
		 * 清除黄色小视频
		 */
		// File file = new File("C:\\Users\\Zad\\Desktop\\result.txt");
		// InputStreamReader reader = new InputStreamReader(
		// new FileInputStream(file)); // 建立一个输入流对象reader
		// BufferedReader br = new BufferedReader(reader); //
		// 建立一个对象，它把文件内容转成计算机能读懂的语言
		// String line = "";
		// line = br.readLine();
		// while (line != null) {
		// line = br.readLine(); // 一次读入一行数据
		// }

		/**
		 * 视频鉴黄
		 */
		// List<Record> list = Db
		// .find("select eid,egroupid,poriginal from events,pictures where eid=peid and
		// eMain=4 and estatus=0 and euserid not in(136298) ");
		// int count = 0;
		// File file = new File("C:\\Users\\Zad\\Desktop\\result.txt");
		// BufferedWriter out = new BufferedWriter(new FileWriter(file));
		//
		// int size = list.size();
		// for (int i = 0; i < size; i++) {
		//
		// String url = list.get(i).get("poriginal").toString();
		// boolean judge = dataProcess.VideoVerify(url);
		// System.out.println("当前进度:" + (i + 1) + "/" + size + " 该视频是否为黄色视频:"
		// + judge);
		// if (judge) {
		// count++;
		// out.write("序号:"
		// + count
		// + " 动态id:"
		// + list.get(i).get("eid").toString()
		// + " 空间id:"
		// + list.get(i).get("egroupid").toString()
		// + "\r\n图片地址:"
		// + operate.getDownloadToken(list.get(i).get("poriginal")
		// .toString()) + "\r\n");
		//
		// }
		// }
		// out.flush();
		// out.close();
		//
		// System.out.println(count);

		/**
		 * 小号进空间
		 */
		// List<Record> list = Db
		// .find("select userid,uphone from users where uphone like '10000000%' order by
		// uphone desc limit 900");
		// for (Record record : list) {
		// String userid = record.get("userid").toString();
		// GroupMember gm = new GroupMember().set("gmgroupid", 1655533).set("gmuserid",
		// userid);
		// try {
		// gm.save();
		// System.out.println("成功");
		// } catch (Exception e) {
		// System.out.println("已进入");
		// }
		// }

		/**
		 * 刷量空间导人
		 */
		// List<Record> list = Db.find("select gmuserid from groupmembers where
		// gmgroupid=833623 order by gmuserid desc limit 1312 ");
		// for (Record record : list) {
		// String userid = record.get("gmuserid").toString();
		// GroupMember gm = new GroupMember().set("gmgroupid", 1655533).set("gmuserid",
		// userid);
		// try {
		// gm.save();
		// System.out.println("成功");
		// } catch (Exception e) {
		// System.out.println("已进入");
		// }
		// }

		/**
		 * 成员数大于等于50的空间内图片鉴黄
		 */
		// List<Record> spaceList = Db
		// .find("select groupid,gname,gnum from groups where gnum>=15 and gnum<20 and
		// gstatus=0 and gtype not in (5,9) and gcreator not in (52426,98214)");
		// VerifyPicture v1 = new VerifyPicture(spaceList, 0);
		// VerifyPicture v2 = new VerifyPicture(spaceList, 10);
		// VerifyPicture v3 = new VerifyPicture(spaceList, 20);
		// VerifyPicture v4 = new VerifyPicture(spaceList, 30);
		// VerifyPicture v5 = new VerifyPicture(spaceList, 40);
		// VerifyPicture v6 = new VerifyPicture(spaceList, 50);
		// VerifyPicture v7 = new VerifyPicture(spaceList, 60);
		// VerifyPicture v8 = new VerifyPicture(spaceList, 70);
		// VerifyPicture v9 = new VerifyPicture(spaceList, 80);
		// VerifyPicture v10 = new VerifyPicture(spaceList, 90);
		// VerifyPicture v11 = new VerifyPicture(spaceList, 100);
		// VerifyPicture v12 = new VerifyPicture(spaceList, 110);
		// VerifyPicture v13 = new VerifyPicture(spaceList, 120);
		// VerifyPicture v14 = new VerifyPicture(spaceList, 130);
		// VerifyPicture v15 = new VerifyPicture(spaceList, 140);
		//
		// v1.start();
		// v2.start();
		// v3.start();
		// v4.start();
		// v5.start();
		// v6.start();
		// v7.start();
		// v8.start();
		// v9.start();
		// v10.start();
		// v11.start();
		// v12.start();
		// v13.start();
		// v14.start();
		// v15.start();

		/**
		 * 图片鉴黄
		 */
		// List<Record> list = Db
		// .find("select pid,poriginal from pictures where pstatus=0 and pOrigin=0 and
		// pid<=10000");
		// for (int i = 0; i < list.size(); i++) {
		// String address = operate.getDownloadToken(list.get(i).get(
		// "poriginal")
		// + "?pulp");
		// // 发送请求并获取返回结果
		// try {
		// String result = dataProcess.sentNetworkRequest(address);
		// JSONObject jo = JSONObject.parseObject(result);
		// int code = jo.getIntValue("code");
		// System.out.print(i + " " + code + " ");
		// if (code == 0) {
		// JSONObject temp = JSONObject.parseObject(jo.get("pulp")
		// .toString());
		// int label = temp.getIntValue("label");
		// System.out.println(label);
		// if (label == 0) {
		// String pid = list.get(i).get("pid").toString();
		// Picture pic = new Picture().findById(pid);
		// pic.set("pstatus", 1);
		// System.out.println(pid + " " + pic.update());
		// }
		// }
		// } catch (Exception e) {
		// System.out.println("第" + i + "个数据获取失败，地址为" + address);
		// continue;
		// }
		// }

		/**
		 * 重置今日忆
		 */
		// int TMid = 158;
		// SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		//
		// for (int i = 1; i <= 150; i++) {
		// Calendar calendar = new GregorianCalendar();
		// Date date = new Date();
		// calendar.setTime(date);
		// calendar.add(calendar.DATE, i);
		// date = calendar.getTime();
		// String dateString = sdf.format(date);
		// System.out.println(dateString);
		// String address = "http://7xlmtr.com1.z0.glb.clouddn.com/jinriyi"
		// + dateString + ".jpg";
		// TodayMemory tm = new TodayMemory().findById(TMid);
		// tm.set("TMpic", address);
		// System.out.println(i + " " + tm.update());
		//
		// TMid++;
		// }

		/**
		 * 生成小号
		 */		
		 List<Record> list = Db.find("select unickname,upic from users order by userid desc limit 0,10000");
		 int size = list.size();
		 System.out.println(size);
		 Random random = new Random();
		 for (int i = 1; i <= 10000; i++) {
		 String phone = "999999" + String.valueOf(i);//3001已生成
		 int index1 = random.nextInt(size);
		 int index2 = random.nextInt(size);
		 String nickname = list.get(index1).get("unickname").toString();
		 String pic = list.get(index2).get("upic").toString();
		 // 4QrcOUm6Wau+VuBX8g+IPg==        为123456
		 User user = new User()
		 .set("uphone", phone)
		 .set("upass", "4QrcOUm6Wau+VuBX8g+IPg==")
		 .set("unickname", nickname)
		 .set("usex", 0)
		 .set("ubirth", "2015-08-01")
		 .set("upic", pic)
		 .set("ubackground",
		 CommonParam.qiniuOpenAddress
		 + CommonParam.userDefaultBackground);
		 if (user.save()) {
		 String userid = user.get("userid").toString();
		 // 注册环信用户
		 im.AddSingleIMUser(userid,nickname);
		 System.out.println(i);
		 }
		 }

		/**
		 * 除去重复点赞
		 */
		// int i = 0;
		// List<Record> list = Db
		// .find("select likeID from `like` GROUP BY likeEventID,likeUserID HAVING
		// count(*)>1");
		// for (Record record : list) {
		// String likeID = record.get("likeID").toString();
		// Like like = new Like().findById(likeID);
		// if (like.delete()) {
		// i++;
		// System.out.println(i);
		// }
		// }

		/**
		 * 动态点赞
		 */
		// 218711 221596
		// String num = this.getPara("num");
		//第二名 776685
		//714654//第一名
		
		//山东最人气  嘻嘻哈哈 725947
//		 String eid =this.getPara("eid");
//		 String likeCnt=this.getPara("likeCnt");
//		 List<Record> list = Db
//		 .find("select userid,uphone from users where uphone like '9999999%' order by uphone desc");
//		 int i = 0;
//		 for (Record record : list) {
//		 Like like = new Like().set("likeEventID", eid)
//		 .set("likeUserID", record.get("userid").toString())
//		 .set("likeStatus", 0);
//		 try {
//		 like.save();
//		 i++;
//		 System.out.println(i);
//		 } catch (Exception e) {
//		 System.out.println("已点赞");
//		 }
//		 if (i > Integer.parseInt(likeCnt))
//		 break;
//		 }

		/**
		 * 网页查看图片
		 */
		// String result = "<html><body>";
		// List<Record> list =
		// Db.find("select address from userPhoto order by id desc limit 100");
		// for(Record record : list){
		// String address = operate.getDownloadToken(record.getStr("address"));
		// result +=
		// "<img src=\""+address+"\" width=\"300\" height=\"300\" />";
		// }
		// result +="</body></html>";
		// renderHtml(result);

		/**
		 * 复制空间
		 */
		// TestThread t1 = new TestThread(new int[] { 1065236 });
		// TestThread t2 = new TestThread(new int[] { 1065243 });
		// TestThread t3 = new TestThread(new int[] { 1065265 });
		// TestThread t4 = new TestThread(new int[] { 1065273 });
		// TestThread t5 = new TestThread(new int[] { 833662 });

		// t1.start();
		// t2.start();
		// t3.start();
		// t4.start();
		// t5.start();

		/**
		 * 捕获数据库异常
		 */
		// GroupMember gm = new GroupMember().set("gmgroupid", 2515).set(
		// "gmuserid", 2);
		// try {
		// gm.save();
		// } catch (ActiveRecordException e) {
		// System.out.println("用户已在该空间内");
		// }

		/**
		 * 全app推送
		 */
		// 获取推送内容和标题
		// String title = "周四×声音记忆器";
		// String content =
		// "我们每天说那么多话，没有一句说给自己。丨打开忆年，为认真一天的自己留下一句语音。嘿，亲爱的，你今天好吗？>>";
		//
		// // 向整个app用户推送
		// YinianGetuiPush push = new YinianGetuiPush();
		// List<Record> list = new ArrayList<Record>();
		//
		// Record data = new Record().set("content", content).set("title",
		// title);
		// list.add(data);
		// String transmissContent = jsonData.getJson(5, "日常推送", list);
		//
		// // 推送
		// push.pushMessageToApp(transmissContent, data);

		/**
		 * 空间成员复制
		 */
		// int i = 0;
		// List<Record> list =
		// Db.find("select gmuserid from groupmembers where gmgroupid=540537 order by
		// gmuserid desc limit 2010 ");
		// for(Record record : list){
		// String userid = record.get("gmuserid").toString();
		// GroupMember gm = new GroupMember().set("gmgroupid",
		// 555933).set("gmuserid", userid);
		// gm.save();
		// System.out.println(i);
		// i++;
		// }

		/**
		 * 查看某些数据
		 */
		// String result = "<html><body>";
		//
		// List<Record> list = Db
		// .find("select userid,unickname,count(*) as num from users,`events`,pictures
		// where userid=euserid and eid=peid and euploadtime>'2017-07-08 00:00:00' group
		// by userid HAVING count(*)>20000");
		// for (Record record : list) {
		// String userid = record.get("userid").toString();
		// String nickname = record.get("unickname").toString();
		// String num = record.get("num").toString();
		// result += "<p>" + userid + "---" + nickname + "---" + num + "张</p>";
		// List<Record> temp = Db
		// .find("select gname,gnum,count(*) as num from
		// groups,groupmembers,`events`,pictures where groupid=gmgroupid and
		// groupid=egroupid and eid=peid and gmuserid="
		// + userid + " GROUP BY groupid");
		// for (Record tempR : temp) {
		// String gname = tempR.getStr("gname");
		// String gnum = tempR.get("gnum").toString();
		// String pnum = tempR.get("num").toString();
		// result += "<p>" + gname + "---" + gnum + "---" + pnum + "张</p>";
		// }
		// }
		// result += "</body></html>";
		// renderHtml(result);

	}

	/**
	 * 登陆功能
	 */
	public void Login() {
		// 获取参数
		String username = this.getPara("username");
		String password = this.getPara("password");
		String source = this.getPara("source");
		// 获取登陆结果
		jsonString = service.login(username, password, source);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 其他平台微信登录注册，用于H5移动端登录
	 * 
	 * @throws Exception
	 */
	@Before(CrossDomain.class)
	public void OtherPlatformWechatLoginAndRegister() throws Exception {
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

		if (code == null || code.equals("")) {
			jsonString = jsonData.getJson(1045, "用户拒绝授权");
		} else {
			// 获取accessToken和openid
			WeixinOauth2Token wot = WechatOauth.getOauth2AccessToken(code);
			String openid = wot.getOpenId();
			String accessToken = wot.getAccessToken();

			// 通过accessToken和openid获取用户信息
			SNSUserInfo wxUserInfo = WechatOauth.getSNSUserInfo(accessToken, openid);
			String unionId = wxUserInfo.getUnionId();

			// 判断该用户是注册还是登录
			List<Record> userInfo = User.QueryUserLoginBasicInfo(unionId, "uwechatid");
			if (userInfo.size() == 0) {
				// 新用户，注册
				User user = new User().set("uwechatid", unionId).set("unickname", wxUserInfo.getNickname())
						.set("upic", wxUserInfo.getHeadImgUrl()).set("usex", wxUserInfo.getSex())
						.set("uprovince", wxUserInfo.getProvince()).set("ucity", wxUserInfo.getCity())
						.set("ubirth", "2017-01-01").set("ubackground", wxUserInfo.getHeadImgUrl()).set("usource", "h5")
						.set("uversion", version).set("uport", port).set("uloginSource", "h5")
						.set("uFromUserID", fromUserID).set("uFromSpaceID", fromSpaceID)
						.set("uFromEventID", fromEventID);
				if (user.save()) {
					String userid = user.get("userid").toString();
					// userid加密
					String encodeUserid = DES.encryptDES("userid=" + userid, CommonParam.DESSecretKey);
					// 构造返回对象
					Record resultRecord = new Record().set("userid", userid).set("encodeUserid", encodeUserid);
					List<Record> resultList = new ArrayList<>();
					resultList.add(resultRecord);
					// 注册环信用户
					im.AddSingleIMUser(userid, wxUserInfo.getNickname());
					// 返回结果
					jsonString = jsonData.getSuccessJson(resultList);
				} else {
					jsonString = dataProcess.insertFlagResult(false);
				}

			} else {
				// 老用户，登录
				String userid = userInfo.get(0).get("userid").toString();
				// userid加密
				String encodeUserid = DES.encryptDES("userid=" + userid, CommonParam.DESSecretKey);
				// 构造返回对象
				Record resultRecord = new Record().set("userid", userid).set("encodeUserid", encodeUserid);
				List<Record> resultList = new ArrayList<>();
				resultList.add(resultRecord);
				// 插入历史访问信息
				UserService userService = new UserService();
				if (userService.AddHistoryAccessInfo(userid, "h5", version, port, null)) {
					jsonString = jsonData.getSuccessJson(resultList);
				} else {
					jsonString = dataProcess.insertFlagResult(false);
				}
			}
		}
		renderText(jsonString);
	}
	/**
	 * 微信登录注册,并发布默认动态
	 */
	public void WechatLoginAndRegisterByLk() {
		// 获取基本参数
		String openid = this.getPara("openid");
		String wechatNickname = this.getPara("wechatNickname");
		String wechatPic = this.getPara("wechatPic");
		String wechatSex = this.getPara("wechatSex");
		String province = this.getPara("province");
		String city = this.getPara("city");
		// 用户来源客户端及对应版本
		String source = this.getPara("source");
		String version = this.getPara("version");
		// 记录用户来源端口参数
		String port = this.getPara("port");
		port = (port == null ? "自然渠道" : port);
		String fromUserID = this.getPara("fromUserID");
		String fromSpaceID = this.getPara("fromSpaceID");
		String fromEventID = this.getPara("fromEventID");

		// 判断新老用户
		List<Record> list = service.judgeUserQQorWechatID(openid, "wechat");
		String userid = "";
		if (list.size() == 0) {
			// 该微信用户未注册，注册后返回userid
			userid = service.wechatUserRegister(openid, wechatNickname, wechatPic, wechatSex, source, province, city,
					version, port, fromUserID, fromSpaceID, fromEventID, null);
			if (userid.equals("")) {
				jsonString = jsonData.getJson(1016, "微信用户注册失败");
			} else {
				// 创建三个系统引导相册
//				boolean createFlag1 = TxService.creatDefaultAlbumAndPulishMsg(userid, "6", source);
//				boolean createFlag2 = TxService.creatDefaultAlbumAndPulishMsg(userid, "7", source);
//				boolean createFlag3 = TxService.creatDefaultAlbumAndPulishMsg(userid, "8", source);
				boolean createFlag1 = true;
				boolean createFlag2 = true;
				boolean createFlag3 = true;
				if (createFlag1 && createFlag2 && createFlag3) {
					// 获取环信登录密码
					String hxPassword = im.getHXLoginPassword(userid);
					// 注册环信用户
					im.AddSingleIMUser(userid, wechatNickname);

					// 添加返回结果
					Record record = new Record();
					record.set("userid", userid).set("password", hxPassword).set("isNewUser", "yes");
					list.add(record);
					jsonString = jsonData.getJson(0, "success", list);
				} else {
					jsonString = jsonData.getJson(-50, "插入数据失败");
				}
			}
		} else {
			// 该微信用户已注册，添加用户历史访问记录，添加成功后登录并且返回userid
			userid = list.get(0).get("userid").toString();
			UserService userService = new UserService();
			if (userService.AddHistoryAccessInfo(userid, source, version, port, null)) {
				// 添加环信登录密码
				String hxPassword = im.getHXLoginPassword(userid);

				list.get(0).set("password", hxPassword).set("isNewUser", "no");
				jsonString = jsonData.getJson(0, "success", list);
			} else {
				jsonString = jsonData.getJson(-51, "更新数据库数据失败");
			}
		}

		// 返回结果
		renderText(jsonString);
	}
	/**
	 * 微信登录注册
	 */
	public void WechatLoginAndRegister() {
		// 获取基本参数
		String openid = this.getPara("openid");
		String wechatNickname = this.getPara("wechatNickname");
		String wechatPic = this.getPara("wechatPic");
		String wechatSex = this.getPara("wechatSex");
		String province = this.getPara("province");
		String city = this.getPara("city");
		// 用户来源客户端及对应版本
		String source = this.getPara("source");
		String version = this.getPara("version");
		// 记录用户来源端口参数
		String port = this.getPara("port");
		port = (port == null ? "自然渠道" : port);
		String fromUserID = this.getPara("fromUserID");
		String fromSpaceID = this.getPara("fromSpaceID");
		String fromEventID = this.getPara("fromEventID");

		// 判断新老用户
		List<Record> list = service.judgeUserQQorWechatID(openid, "wechat");
		String userid = "";
		if (list.size() == 0) {
			// 该微信用户未注册，注册后返回userid
			userid = service.wechatUserRegister(openid, wechatNickname, wechatPic, wechatSex, source, province, city,
					version, port, fromUserID, fromSpaceID, fromEventID, null);
			if (userid.equals("")) {
				jsonString = jsonData.getJson(1016, "微信用户注册失败");
			} else {
				// 创建三个系统引导相册
				boolean createFlag1 = TxService.creatDefaultAlbum(userid, "6", source);
				boolean createFlag2 = TxService.creatDefaultAlbum(userid, "7", source);
				boolean createFlag3 = TxService.creatDefaultAlbum(userid, "8", source);
				if (createFlag1 && createFlag2 && createFlag3) {
					// 获取环信登录密码
					String hxPassword = im.getHXLoginPassword(userid);
					// 注册环信用户
					im.AddSingleIMUser(userid, wechatNickname);

					// 添加返回结果
					Record record = new Record();
					record.set("userid", userid).set("password", hxPassword).set("isNewUser", "yes");
					list.add(record);
					jsonString = jsonData.getJson(0, "success", list);
				} else {
					jsonString = jsonData.getJson(-50, "插入数据失败");
				}
			}
		} else {
			// 该微信用户已注册，添加用户历史访问记录，添加成功后登录并且返回userid
			userid = list.get(0).get("userid").toString();
			UserService userService = new UserService();
			if (userService.AddHistoryAccessInfo(userid, source, version, port, null)) {
				// 添加环信登录密码
				String hxPassword = im.getHXLoginPassword(userid);

				list.get(0).set("password", hxPassword).set("isNewUser", "no");
				jsonString = jsonData.getJson(0, "success", list);
			} else {
				jsonString = jsonData.getJson(-51, "更新数据库数据失败");
			}
		}

		// 返回结果
		renderText(jsonString);
	}

	/**
	 * QQ登录注册
	 * 
	 * @废弃接口
	 */
	public void QQLoginAndRegister() {
		String qqOpenID = this.getPara("qqOpenID");
		String nickname = this.getPara("nickname");
		String picture = this.getPara("picture");
		String sex = this.getPara("sex");
		String source = this.getPara("source");
		List<Record> list = service.judgeUserQQorWechatID(qqOpenID, "qq");
		if (list.size() == 0) {
			// 该微信用户未注册，注册后返回userid
			String userid = service.qqUserRegister(qqOpenID, nickname, picture, sex);
			if (userid.equals("")) {
				jsonString = jsonData.getJson(1024, "QQ用户注册失败");
			} else {
				list = dataProcess.makeSingleParamToList("userid", userid);
				jsonString = jsonData.getJson(0, "success", list);
			}
		} else {
			// 该QQ用户已注册，修改用户最后一次登录时间，修改成功后登录并且返回userid
			if (service.updateLastLoginInfo(list.get(0).get("userid").toString(), picture)) {
				// 修改用户最后登录时间
				jsonString = jsonData.getJson(0, "success", list);
			} else {
				jsonString = jsonData.getJson(-51, "更新数据库数据失败");
			}
		}
		// 返回结果
		renderText(jsonString);

	}

	/**
	 * QQ与微信绑定
	 * 
	 * @废弃接口
	 */
	public void BindWechatAndQQ() {
		String userid = this.getPara("userid");
		String type = this.getPara("type");
		String id = this.getPara("id");

		User user = new User().findById(userid);
		List<Record> list = new ArrayList<Record>();
		boolean flag = false;

		switch (type) {
		case "wechat":
			list = service.judgeUserQQorWechatID(id, type);
			if (list.size() == 0) {
				user.set("uwechatid", id);
				flag = user.update();
				if (flag) {
					jsonString = jsonData.getJson(0, "success");
				} else {
					jsonString = jsonData.getJson(1018, "微信号绑定失败");
				}
			} else {
				jsonString = jsonData.getJson(1017, "微信号已被绑定");
			}
			break;
		case "qq":
			list = service.judgeUserQQorWechatID(id, type);
			if (list.size() == 0) {
				user.set("uqqid", id);
				flag = user.update();
				if (flag) {
					jsonString = jsonData.getJson(0, "success");
				} else {
					jsonString = jsonData.getJson(1026, "QQ号绑定失败");
				}
			} else {
				jsonString = jsonData.getJson(1025, "QQ号已被绑定");
			}
			break;
		}
		renderText(jsonString);
	}

	/**
	 * 绑定微信
	 */
	public void BindWechat() {
		String userid = this.getPara("userid");
		String openid = this.getPara("openid");
		List<Record> list = service.judgeUserQQorWechatID(openid, "wechat");
		if (list.size() == 0) {
			boolean flag = service.bindWechatAndPhonenumber(userid, openid, "wechat");
			if (flag) {
				jsonString = jsonData.getJson(0, "success");
			} else {
				jsonString = jsonData.getJson(1018, "微信号绑定失败");
			}
		} else {
			jsonString = jsonData.getJson(1017, "微信号已被绑定");
		}
		renderText(jsonString);
	}

	/**
	 * 注册——验证手机号码
	 */
	public void RegisterVerifyPhone() {
		// 获取参数
		String phonenumber = this.getPara("phonenumber");
		boolean flag = service.verifyPhone(phonenumber); // 获取验证结果
		if (flag) {
			jsonString = jsonData.getJson(1002, "手机号码已存在");
		} else {
			jsonString = jsonData.getJson(0, "success");
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 发送验证码
	 */
	public void SendVerifyMessage() {
		// 获取参数
		String phonenumber = this.getPara("phonenumber");
		// 获取短信发送结果
		jsonString = service.verifyMessage(phonenumber);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 注册（跳过）
	 */
	public void SkipRegister() {
		// 获取参数
		String username = this.getPara("username");
		String password = this.getPara("password");
		// 获取注册结果返回的用户ID
		String userid = service.register(username, password);
		if (userid.equals("")) {
			jsonString = jsonData.getJson(-50, "插入数据失败");
		} else {
			boolean flag = TxService.sendInviteToWaitPerson(username, userid);
			// 创建默认相册
			// boolean createFlag1 = service.creatDefaultAlbum(userid, "0",
			// null);
			// boolean createFlag2 = service.creatDefaultAlbum(userid, "1",
			// null);
			if (flag) {
				List<Record> list = dataProcess.makeSingleParamToList("userid", userid);
				jsonString = jsonData.getJson(0, "success", list);
			} else {
				jsonString = jsonData.getJson(-50, "插入数据失败");
			}
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 注册（填写个人信息）
	 */
	public void Register() {
		// 获取参数
		String username = this.getPara("username");
		String password = this.getPara("password");
		String nickname = this.getPara("nickname");
		String sex = this.getPara("sex");
		String birthday = this.getPara("birthday");
		// 获取注册结果返回的用户ID
		String userid = service.register(username, password, nickname, sex, birthday);
		if (userid.equals("")) {
			jsonString = jsonData.getJson(-50, "插入数据失败");
		} else {
			boolean flag = TxService.sendInviteToWaitPerson(username, userid);
			if (flag) {
				List<Record> list = dataProcess.makeSingleParamToList("userid", userid);
				jsonString = jsonData.getJson(0, "success", list);
			} else {
				jsonString = jsonData.getJson(-50, "插入数据失败");
			}
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 忘记密码——验证手机
	 */
	public void ForgetVerifyPhone() {
		// 获取参数
		String phonenumber = this.getPara("phonenumber");
		boolean flag = service.verifyPhone(phonenumber); // 获取验证结果
		if (flag) {
			jsonString = jsonData.getJson(0, "success");
		} else {
			jsonString = jsonData.getJson(1004, "该手机号尚未注册");
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 忘记密码——重置密码
	 */
	public void ResetPassword() {
		// 获取参数
		String phonenumber = this.getPara("phonenumber");
		String password = this.getPara("password");
		jsonString = service.resetPassword(phonenumber, password);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 显示相册
	 */
	@Before(CrossDomain.class)
	public void ShowGroup() {
		// 获取参数
		int userid = Integer.parseInt(this.getPara("userid"));
		jsonString = service.showGroup(userid);
		// 返回结果
		renderText(jsonString);
	}
	/**
	 * 显示相册 带置顶
	 */
	@Before(CrossDomain.class)
	public void ShowGroupWithTop() {
		// 获取参数
		int userid = Integer.parseInt(this.getPara("userid"));
		jsonString = service.showGroupWithTop(userid);
		// 返回结果
		renderText(jsonString);
	}
	
	/**
	 * 显示相册 带置顶(带分页) by ly
	 */
	@Before(CrossDomain.class)
	public void ShowGroupWithTopNew() {
		// 获取参数
		int userid = Integer.parseInt(this.getPara("userid"));
		int pagenum = Integer.parseInt(this.getPara("pagenum"));
		String type = this.getPara("type");
		jsonString = service.showGroupWithTopNew(userid,pagenum,type);
		// 返回结果
		renderText(jsonString);
	}
	/**
	 * 显示相册 带置顶(带分页) 不显示用户数，图片数 by ly
	 */
	@Before(CrossDomain.class)
	public void ShowNoPicCntGroupWithTopNew() {
		// 获取参数
		int userid = Integer.parseInt(this.getPara("userid"));
		int pagenum = Integer.parseInt(this.getPara("pagenum"));
		String type = this.getPara("type");
		jsonString = service.showNoPicGroupWithTopNew(userid,pagenum,type);
		// 返回结果
		renderText(jsonString);
	}
	/**
	 * 搜索空间
	 */
	public void SearchSpace() {
		String spaceName = this.getPara("spaceName");
		List<Record> result = service.searchSpace(spaceName);
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
	}
	
	/**
	 * 首页根据相册名搜索
	 */
	public void SearchGroupByName() {
		String spaceName = this.getPara("spaceName");
		String userid = this.getPara("userid");
		List<Record> result = Db.find("select groupid,gname,openGId,isDefault,gcreator,isTop from groups,groupmembers where gmstatus=0 and  gmgroupid=groupid and gmuserid="+userid+" and gname like "+"'%"+spaceName+"%'"+" and isDefault=0");
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
	}
	
	/**
	 * 发送邀请功能
	 */
	public void SendInvite() {
		// 获取参数
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String phonenumber = this.getPara("phonenumber");

		// 获得所有电话的列表
		List<Record> phoneList = service.getAllPhone();
		// 获取所有等待列表中的电话及发送者
		List<Record> waitPhoneList = service.getAllWaitPhone();

		// 获取分类后的电话
		Record result = dataProcess.disposePhonenumber(phonenumber, phoneList);
		String notifyPhone = result.getStr("notifyPhone"); // 需要发送通知的电话
		String messagePhone = result.getStr("messagePhone"); // 需要发送短信的电话

		// 获取短信内容的相关信息
		Record messageInfo = service.getMessageInfo(groupid, userid);
		// 你的小伙伴XXX在忆年和你建立了一个死党相册XXX（复制邀请码：XXXX），
		// 点击可在相册查看ta给你的小惊喜哦~（忆年，小圈子多人图片共享相册。下载链接：www.pgyer.com/fRNE

		// 构造短信内容
		String content = "你的小伙伴" + messageInfo.getStr("userName").toString() + "在忆年和你建立了一个" + messageInfo.getStr("type")
				+ messageInfo.getStr("groupName") + "（复制邀请码：" + messageInfo.getStr("ginvite")
				+ "），点击查看照片~忆年，小圈子多人图片共享相册。下载链接：www.pgyer.com/fRNE";

		// 判断flag
		boolean messageFlag = true;
		boolean waitFlag = true;
		// 推送列表
		List<Record> pushList = new ArrayList<Record>();
		// 判断要发送短信的号码是否为空，不为空的话进行短信发送处理
		if (!(messagePhone.equals(""))) {
			// 判断用户是否向那些电话发送过短信邀请，是：发短信，但不插入数据到waits表；否：发短信并插入数据到waits表
			Record waitResult = dataProcess.disposeMessagePhone(messagePhone, userid, waitPhoneList);
			String notInWaitsPhone = waitResult.getStr("notInWaitsPhone"); // 不在waits表中的电话信息
			// 所有号码进行短信发送并获取结果
			messageFlag = service.sendInviteMessage(content, messagePhone);
			// 不在waits表中的用户信息要插入到里面
			waitFlag = TxService.insertInfoIntoWaits(userid, groupid, messageInfo, notInWaitsPhone);
		}
		// 判断要发送邀请的号码是否为空，不为空的话进行邀请发送处理
		if (!(notifyPhone.equals(""))) {
			// 判断是否在组内，返回不在组内的用户的电话号码
			String notInGroupPhone = service.judgeUserInGroup(groupid, notifyPhone);
			if (!(notInGroupPhone.equals(""))) {
				// 不在组内的话，发送通知并返回推送列表
				pushList = service.sendInviteNotification(userid, groupid, messageInfo, notInGroupPhone);
			}
		}

		// 结果判断
		if (messageFlag && waitFlag) {
			// 上述操作均成功，pushList不为空时进行推送
			if ((pushList.size()) != 0) {
				push.yinianPushToSingle(pushList);
			}
			jsonString = jsonData.getJson(0, "success");
		}
		if (!messageFlag) {
			jsonString = jsonData.getJson(1007, "短信邀请码发送失败");
		}
		if (!waitFlag) {
			jsonString = jsonData.getJson(-50, "插入数据失败");
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 创建相册
	 */
	@Before(CrossDomain.class)
	public void CreateAlbum() {
		// 获取参数
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String groupName = this.getPara("groupName");
		String groupType = this.getPara("groupType");
		String url = this.getPara("url");
		String source = this.getPara("source");
		String formID = this.getPara("formID")==null?"":this.getPara("formID"); // 小程序推送表单ID

		if(!userid.equals("")&&!formID.equals("")){
			FormID.insert(userid, formID);
		}
		if (url == null || url.equals("")) {
			// 根据组类别获取相应的群头像图片
			switch (groupType) {
			case "0":// 家人组
				url = CommonParam.qiniuOpenAddress + CommonParam.familyGroup;
				break;
			case "1":// 闺蜜组
				url = CommonParam.qiniuOpenAddress + CommonParam.bestieGroup;
				break;
			case "2":// 死党组
				url = CommonParam.qiniuOpenAddress + CommonParam.friendGrop;
				break;
			case "3":// 情侣组
				url = CommonParam.qiniuOpenAddress + CommonParam.coupleGroup;
				break;
			case "4":// 其他
				// 从缓存中获取随机封面
				List<Record> coverList = CacheKit.get("EternalCache", "spaceCover");
				// 缓存为空，重新查询
				if (coverList == null) {
					coverList = Group.GetSpaceDefaultCoverList();
				}
				int size = coverList.size();
				url = coverList.get(new Random().nextInt(size - 1)).getStr("acurl");
				break;
			default:
				url = CommonParam.qiniuOpenAddress + CommonParam.otherGroup;
				break;
			}
		} else {
			String temp = url.substring(0, 7);
			if (!(temp.equals("http://"))) {
				url = CommonParam.qiniuOpenAddress + url;
			}

		}
		String inviteCode = Group.CreateSpaceInviteCode();

		jsonString = TxService.createAlbum(groupName, userid, url, groupType, inviteCode, source);

		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 显示组内信息
	 */
	public void ShowGroupContent() {
		// 获取参数
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String minID = this.getPara("minID");
		jsonString = service.getGroupContent(userid, groupid, minID);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 显示成员列表
	 */
	@Before(CrossDomain.class)
	public void ShowGroupMember() {
		// 获取参数
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		if(userid==null||userid.equals("")||userid.equals("undefined")||userid.equals("NaN")||groupid==null||groupid.equals("")||groupid.equals("undefined")||groupid.equals("NaN")){
			jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		String source = this.getPara("source");
		// by lk 
		if(userid!=null&&groupid!=null&&!groupid.equals("undefined")&&!groupid.equals("NaN")){
			if (source != null && !source.equals("")) {
				groupid = dataProcess.decryptData(groupid, "groupid");
			}
			jsonString = service.getMemberList(userid, groupid);
		}else{
			jsonString=jsonData.getJson(2, "参数错误");
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 显示用户头信息
	 */
	public void ShowUserHead() {
		// 获取参数
		String userid = this.getPara("userid");
		jsonString = service.getUserHead(userid);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 发表评论
	 */
	public void SendComment() {
		String commentUserId = this.getPara("commentUserId");// 评论人ID
		String commentedUserId = this.getPara("commentedUserId");// 被评论人ID
		String eventId = this.getPara("eventId");// 事件ID
		String content = this.getPara("content");// 评论内容

		boolean flag = TxService.sendComment(commentUserId, commentedUserId, eventId, content);
		if (flag) {
			jsonString = jsonData.getJson(0, "success");

		} else {
			jsonString = jsonData.getJson(-50, "插入数据失败");
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 发表评论1 1.1版本 新增返回字段 cid
	 * 
	 */
	public void SendComment1() {
		String commentUserId = this.getPara("commentUserId")==null?"":this.getPara("commentUserId");// 评论人ID
		String commentedUserId = this.getPara("commentedUserId");// 被评论人ID
		String eventId = this.getPara("eventId");// 事件ID
		String content = this.getPara("content");// 评论内容
		String place = this.getPara("place");
		String formID = this.getPara("formID")==null?"":this.getPara("formID"); // 小程序推送表单ID

		if(!commentUserId.equals("")&&!formID.equals("")){
			FormID.insert(commentUserId, formID);
		}
		String cid = TxService.sendComment1(commentUserId, commentedUserId, eventId, content, place);
		if (cid.equals("")) {
			jsonString = jsonData.getJson(-50, "插入数据失败");
		} else {
			List<Record> list = dataProcess.makeSingleParamToList("cid", cid);
			jsonString = jsonData.getJson(0, "success", list);
			//评论推送 begin
			if(CommonParam.canPublish){
				Event event = new Event().findById(eventId);
				String egroupid = event.get("egroupid").toString();
//				//判断是否是普通相册 在普通相册才推送
				String gOrigin =String.valueOf(new Group().findById(egroupid).getLong("gOrigin"));
				if(gOrigin.equals("0")) {
					User user = new User().findById(commentUserId);
					//评论人的姓名
					String username = user.get("unickname").toString();
					//被评论人的uopenid
					String uid = event.get("euserid").toString();
					String uopenid = new User().findById(uid).get("uopenid");
					if(null!=uopenid&&!uopenid.equals("")&&!uid.equals(commentUserId)){
						//提取发布人的formid
						List<Record> formidList = Db.find("select formID from formid where 1 "
								+ "and status=0 and userID="+uid+ " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
								+ "order by time asc limit 1");
						
						if(null!=formidList&&!formidList.isEmpty()) {
							String formid = formidList.get(0).get("formID");
							//评论成功发送推送
							SmallAppPush smallAppPush = new SmallAppPush();
							smallAppPush.commentIsPush(formid, uopenid, eventId, username);
							Db.update("delete from formid where userID="+uid+" and formID='"+formid+"'");
						}
					}
				}
			}
			//评论推送 end
			//雷雨评论推送开关
//			Boolean commentPushFlag = true;
//			Event event = new Event().findById(eventId);
//			String egroupid = event.get("egroupid").toString();
//			//判断是否是普通相册 在普通相册才推送
//			String gOrigin =String.valueOf(new Group().findById(egroupid).getLong("gOrigin"));
//			if(gOrigin.equals("1")) {
//				commentPushFlag = false;
//			}
//			if(commentPushFlag) {
//				User user = new User().findById(commentUserId);
//				//评论人的姓名
//				String username = user.get("unickname").toString();
//				//被评论人的uopenid
//				String uid = event.get("euserid").toString();
//				String uopenid = new User().findById(uid).get("uopenid");
//				//提取发布人的formid
//				List<Record> formidList = Db.find("select formID from formid where 1 "
//						+ "and status=0 and userID="+uid+ " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
//						+ "order by time asc limit 1");
//				
//				if(null!=formidList&&!formidList.isEmpty()) {
//					String formid = formidList.get(0).get("formID");
//					//评论成功发送推送
//					SmallAppPush smallAppPush = new SmallAppPush();
//					smallAppPush.commentIsPush(formid, uopenid, eventId, username);
//					Db.update("delete from formid where userID="+uid+" and formID='"+formid+"'");
//				}
//			}
			//雷雨推送？？？？
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 在官方相册内的评论
	 */
	public void SendCommentInOfficialAlbum() {
		String commentUserId = this.getPara("commentUserId");// 评论人ID
		String commentedUserId = this.getPara("commentedUserId");// 被评论人ID
		String eventId = this.getPara("eventId");// 事件ID
		String content = this.getPara("content");// 评论内容
		if (commentedUserId == null) {
			commentedUserId = CommonParam.systemUserID;
		}
		String cid = TxService.sendCommentInOfficialAlbum(commentUserId, commentedUserId, eventId, content);
		if (cid.equals("")) {
			jsonString = jsonData.getJson(-50, "插入数据失败");
		} else {
			List<Record> list = dataProcess.makeSingleParamToList("cid", cid);
			jsonString = jsonData.getJson(0, "success", list);
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 删除动态
	 */
	@Before(CrossDomain.class)
	public void DeleteEvent() {
		// 获取参数
		String eventId = this.getPara("eventId");

		boolean flag = TxService.deleteEvent(eventId);
		if (flag) {
			jsonString = jsonData.getJson(0, "success");
		} else {
			jsonString = jsonData.getJson(-52, "删除数据库数据失败");
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 删除动态，带有身份验证
	 */
	public void DeleteEventWithUserVerify() {
		String userid = this.getPara("userid");
		String eventID = this.getPara("eventID");

		Event event = new Event().findById(eventID);
		String publisher = event.get("euserid").toString();

		if (userid.equals(publisher)) {
			boolean flag = TxService.deleteEvent(eventID);
			if (flag) {
				jsonString = jsonData.getJson(0, "success");
			} else {
				jsonString = jsonData.getJson(-52, "删除数据库数据失败");
			}
		} else {
			jsonString = jsonData.getJson(6, "没有权限执行操作");
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 显示组员个人信息
	 */
	public void ShowMemberInfo() {
		// 获取参数
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String minID = this.getPara("minID");
		jsonString = service.getMemberInfo(groupid, userid, minID);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 删除组
	 */
	public void DeleteGroup() {
		// 获取参数
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String source = this.getPara("source");
		if (userid == null || userid.equals("")) {
			jsonString = TxService.deleteGroup(groupid, source);
			// jsonString = jsonData.getJson(1, "请求参数缺失");
		} else {
			Group group = new Group().findById(groupid);
			String gcreator = group.get("gcreator").toString();
			if (gcreator.equals(userid)) {
				jsonString = TxService.deleteGroup(groupid, source);
			} else {
				jsonString = jsonData.getJson(6, "没有权限执行操作");
			}
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 新成员进组
	 */
	@Before(Tx.class)
	public void EnterGroup() {
		// 获取参数
		String groupid = this.getPara("groupid");
		String userid = this.getPara("userid");
		String messageID = this.getPara("messageID");
		List<Record> list = service.getGroupMemberID(groupid);// 获取进入组之前改组所有成员ID，留作通知
		// 获取messageID对应的invitegroup数据的类型（type） type为1表示是邀请进组type为2表示是申请进组
		String type = InviteGroup.dao.findById(messageID).get("type").toString();
		// 获取组类型，如果是官方相册（5），则成员进入相册后，需要在likes表中加入数据
		List<Record> typeList = dao.query("gtype", "groups", "groupid=" + groupid + " ");
		String gtype = typeList.get(0).get("gtype").toString();
		// 判断标志
		boolean NotInGroupFlag = true; // 是否在组内
		boolean DeleteFlag = true; // 删除用户消息列表上的invitegroup
		boolean NotificationFlag = true; // 插入通知
		boolean likeFlag = true; // 插入到点赞表中的标志
		switch (type) {
		case "1":
			// 邀请进组
			// 判断用户是否已经在组内
			for (Record record : list) {
				if ((record.get("gmuserid").toString()).equals(userid)) {
					NotInGroupFlag = false;
					break;
				}
			}
			if (NotInGroupFlag) {
				// 不在组内，进组并返回组信息，删除邀请，发送通知
				List<Record> groupInfo = TxService.enterGroup(groupid, userid);// 进组并返回组信息用于用户添加到他的组列表中
				DeleteFlag = service.deleteMessage(messageID);// 判断该信息是否从客户端删除，即信息状态为：已删除
				NotificationFlag = TxService.insertEnterNotification(groupid, userid, list);
				if (type.equals("5")) {
					likeFlag = service.newUserJoinInsertLikes(userid, groupid);
				}
				if (DeleteFlag && NotificationFlag && likeFlag) {
					jsonString = jsonData.getJson(0, "success", groupInfo);
				} else {
					jsonString = jsonData.getJson(-51, "更新数据失败");
				}

			} else {
				// 在组内，把该条邀请标记为已删除
				DeleteFlag = service.deleteMessage(messageID);// 判断该信息是否从客户端删除，即信息状态为：已删除
				if (DeleteFlag) {
					jsonString = jsonData.getJson(0, "该用户已在组内");
				} else {
					jsonString = jsonData.getJson(-51, "更新数据失败");
				}
			}
			break;

		case "2":
			// 申请进组
			// 通过MessageID获取igsender即申请者的userid
			String senderid = InviteGroup.dao.findById(messageID).get("igsender").toString();
			// 判断用户是否已经在组内
			for (Record record : list) {
				if ((record.get("gmuserid").toString()).equals(senderid)) {
					NotInGroupFlag = false;
					break;
				}
			}
			if (NotInGroupFlag) {
				// 不在组内，进组并返回组信息，删除邀请，发送通知
				List<Record> groupInfo = TxService.enterGroup(groupid, senderid);// 进组并返回组信息用于用户添加到他的组列表中
				DeleteFlag = service.deleteMessage(messageID);// 判断该信息是否从客户端删除，即信息状态为：已删除
				NotificationFlag = TxService.agreeJoinGroup(groupid, senderid, list);
				if (type.equals("5")) {
					likeFlag = service.newUserJoinInsertLikes(userid, groupid);
				}
				if (DeleteFlag && NotificationFlag && likeFlag) {
					jsonString = jsonData.getJson(0, "success", groupInfo);
				} else {
					jsonString = jsonData.getJson(-51, "更新数据失败");
				}

			} else {
				// 在组内，把该条邀请标记为已删除
				DeleteFlag = service.deleteMessage(messageID);// 判断该信息是否从客户端删除，即信息状态为：已删除
				if (DeleteFlag) {
					jsonString = jsonData.getJson(0, "该用户已在组内");
				} else {
					jsonString = jsonData.getJson(-51, "更新数据失败");
				}
			}
			break;
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 显示我的
	 */
	public void ShowMe() {
		String userid = this.getPara("userid");
		String minID = this.getPara("minID");
		jsonString = service.getMyEvents(userid, minID);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 修改个人信息
	 */
	public void ModifyInfo() {
		// 获取参数
		String userid = this.getPara("userid");
		String pic = this.getPara("pic");
		if (!pic.substring(0, 4).equals("http")) {
			pic = CommonParam.qiniuOpenAddress + pic;
		}
		String sex = this.getPara("sex");
		String birthday = this.getPara("birthday");
		String nickname = this.getPara("nickname");
		boolean flag = service.modifyPersonalInfo(userid, pic, sex, nickname, birthday);
		if (flag) {
			jsonString = jsonData.getJson(0, "success");
		} else {
			jsonString = jsonData.getJson(-51, "修改数据库数据失败");
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 修改个人单项资料
	 */
	public void ModifySingleInfo() {
		// 获取参数
		String userid = this.getPara("userid");
		String data = this.getPara("data");
		String type = this.getPara("type");
		boolean flag = service.modifyPersonalSingleInfo(userid, data, type);
		jsonString = dataProcess.updateFlagResult(flag);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 显示个人信息
	 */
	public void ShowInfo() {
		// 获取参数
		String userid = this.getPara("userid");
		jsonString = service.getPersonalInfo(userid);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 修改组名
	 */
	@Before( CrossDomain.class )
	public void ModifyGroupName() {
		// 获取参数
		String groupid = this.getPara("groupid");
		String groupName = this.getPara("groupName");
		// 获取上传所在组的类型，若是官方相册（5），则不不能修改
		List<Record> list = dao.query("gtype", "groups", "groupid=" + groupid + " ");
		String type = list.get(0).get("gtype").toString();
		if (type.equals("5")) {
			jsonString = jsonData.getJson(0, "success");
		} else {
			jsonString = service.modifyGroupName(groupid, groupName);
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 举报功能
	 */
	public void Report() {
		// 获取参数
		String userid = this.getPara("userid");
		String eduserid = this.getPara("eduserid");
		String eventId = this.getPara("eventId");
		jsonString = service.addReport(userid, eduserid, eventId);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 显示单条动态
	 */
	public void ShowSingleEvent() {
		// 获取参数
		String eventId = this.getPara("eventId");
		jsonString = service.getSingleEvent(eventId);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 删除邀请消息
	 */
	public void DeleteMessage() {
		// 获取参数
		String messageID = this.getPara("messageID");
		boolean flag = service.deleteMessage(messageID);
		if (flag) {
			jsonString = jsonData.getJson(0, "success");
		} else {
			jsonString = jsonData.getJson(-51, "更新数据库数据失败");
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 删除评论消息 1.1版本
	 */
	public void DeleteCommentMessage() {
		// 获取参数
		String messageID = this.getPara("messageID");
		boolean flag = service.deleteCommentMessage(messageID);
		jsonString = dataProcess.updateFlagResult(flag);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 删除通知
	 */
	public void DeleteNotification() {
		// 获取参数
		String notificationID = this.getPara("notificationID");
		boolean flag = service.deleteNotification(notificationID);
		if (flag) {
			jsonString = jsonData.getJson(0, "success");
		} else {
			jsonString = jsonData.getJson(-51, "更新数据库数据失败");
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 显示消息列表
	 */
	public void ShowMessageList() {
		// 获取参数
		String userid = this.getPara("userid");
		String sign = this.getPara("sign");
		int minID = Integer.parseInt(this.getPara("minID"));
		// Record CommentNum = service.getCount(userid, "comment");
		// Record InviteNum = service.getCount(userid, "invitegroup");
		// Record numRecord = dataProcess.getUnreadMessageNum(CommentNum,
		// InviteNum); // 获取未读消息数量
		jsonString = TxService.getMessageList(userid, minID, sign);
		// 返回结果
		renderText(jsonString);

	}

	/**
	 * 显示通知列表
	 */
	public void ShowNotificationList() {
		// 获取参数
		String userid = this.getPara("userid");
		int minID = Integer.parseInt(this.getPara("minID"));
		// 获取未读通知的数量
		jsonString = TxService.getNotificationList(userid, minID);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 上传动态
	 * 
	 * @废弃接口
	 */
	@Before({ Tx.class, CrossDomain.class })
	public void UploadEvent() {

		// 获取参数
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String picAddress = this.getPara("picAddress");
		String content = this.getPara("content");
		String storage = this.getPara("storage");
		String memorytime = this.getPara("memorytime");
		String mode = this.getPara("mode");
		String source = this.getPara("source");// 判断接口来源
		String place = this.getPara("place");

		if (storage == null || storage.equals("")) {
			storage = "0";
		}

		// 接口来源为web，需要解密
		if (source != null && source.equals("web")) {
			groupid = dataProcess.decryptData(groupid, "groupid");
		}

		String[] IDs = groupid.split(",");
		// 上传动态
	//	User u=new User().findById(userid);
		for (int i = 0; i < IDs.length; i++) {
			String eventID = TxService.upload(userid, IDs[i], picAddress, content, storage, memorytime, mode, place,
					source);// 上传动态并获取动态的ID
			if (eventID.equals("")) {
				jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
				break;
			}
//			if(null!=u&&null!=u.get("ustate")&&u.get("ustate").toString().equals("1")){
//				Db.update("update pictures set poriginal='http://oibl5dyji.bkt.clouddn.com/Resource_violation_pic.jpg' where peid=" + eventID);
//			}
			if (i + 1 == IDs.length) {								
				jsonString = service.getSingleEvent(eventID);// 获取动态的信息
			}
		}

		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 上传日签，也可用于日签的同步和分享
	 */
	public void uploadDayMark() {
		// 获取参数
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String picAddress = this.getPara("picAddress");
		String storage = this.getPara("storage");
		if (storage == null || storage.equals("")) {
			storage = "0";
		}
		String[] IDs = groupid.split(",");
		// 返回eid和euploadtime
		for (int i = 0; i < IDs.length; i++) {
			String eventID = TxService.upload(userid, IDs[i], picAddress, null, storage, null, "dayMark", null, "app");// 上传动态并获取动态的ID
			if (!eventID.equals("")) {
				jsonString = service.getSingleEvent(eventID);// 获取动态的信息
			} else {
				jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
				break;
			}
		}
		// 返回结果
		renderText(jsonString);

	}

	/**
	 * 发布记忆卡片 也可用于记忆卡片的同步或分享
	 */
	public void UploadMemoryCard() {
		// 获取参数
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String picAddress = this.getPara("picAddress");
		String tags = this.getPara("tags");
		String audio = this.getPara("audio");
		String content = this.getPara("content");
		String storage = this.getPara("storage");
		String memorytime = this.getPara("memorytime");
		String cardstyle = this.getPara("cardstyle");
		String place = this.getPara("place");
		String audiotime = this.getPara("audiotime");
		String mode = this.getPara("mode");

		String[] groupidArray = groupid.split(",");
		String eventID = "";
		for (int i = 0; i < groupidArray.length; i++) {
			eventID = TxService.uploadMemoryCard(userid, groupidArray[i], picAddress, tags, audio, content, storage,
					memorytime, cardstyle, place, audiotime, mode);// 上传记忆卡片并获取动态的ID
		}

		if (!eventID.equals("")) {
			jsonString = service.getSingleEvent(eventID);// 获取动态的信息
		} else {
			jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 发布时光明信片 也可用于时光明信片的同步或分享
	 */
	public void UploadPostcard() {
		// 获取参数
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String picAddress = this.getPara("picAddress");
		String coverUrl = this.getPara("coverUrl");
		String audio = this.getPara("audio");
		String audiotime = this.getPara("audiotime");
		String content = this.getPara("content");
		String storage = this.getPara("storage");
		String memorytime = this.getPara("memorytime");
		String place = this.getPara("place");
		String mode = this.getPara("mode");

		String[] groupidArray = groupid.split(",");
		String eventID = "";
		for (int i = 0; i < groupidArray.length; i++) {
			eventID = TxService.uploadPostcard(userid, groupidArray[i], picAddress, audiotime, audio, content, storage,
					memorytime, coverUrl, place, mode);// 上传记忆卡片并获取动态的ID
		}

		if (!eventID.equals("")) {
			jsonString = service.getSingleEvent(eventID);// 获取动态的信息
		} else {
			jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 在官方相册内上传动态，可选择是否进行推送
	 */
	@Before(Tx.class)
	public void UploadEventInOfficialAlbum() {
		// 获取参数
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String picAddress = this.getPara("picAddress");
		String content = this.getPara("content");
		String shottime = this.getPara("shottime");
		String shotplace = this.getPara("shotplace");
		String isPush = this.getPara("isPush"); // 0--不推送 1--推送
		String mode = this.getPara("mode");
		String eventID = "";
		switch (isPush) {
		case "0":
			// 无推送
			eventID = TxService.uploadWithoutPush(userid, groupid, picAddress, content, shottime, shotplace, mode);// 上传动态并获取动态的ID
			break;
		case "1":
			// 有推送
			eventID = TxService.upload(userid, groupid, picAddress, content, "0", null, mode, null, "app");// 上传动态并获取动态的ID
			break;
		}
		if (!eventID.equals("")) {
			// 获取组内所有成员的ID
			List<Record> list = service.getGroupMemberID(groupid);
			// 插入数据到likes表中
			boolean flag = service.insertDataIntoLikes(eventID, list);
			if (flag) {
				jsonString = service.getSingleEventInOfficialAlbum(userid, eventID);// 获取动态的信息
			} else {
				jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
			}
		} else {
			jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 在官方相册内上传记忆卡片，可选择是否进行推送
	 */
	@Before(Tx.class)
	public void UploadMemoryCardInOfficialAlbum() {
		// 获取参数
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String picAddress = this.getPara("picAddress");
		String tags = this.getPara("tags");
		String audio = this.getPara("audio");
		String content = this.getPara("content");
		String storage = this.getPara("storage");
		String memorytime = this.getPara("memorytime");
		String cardstyle = this.getPara("cardstyle");
		String place = this.getPara("place");
		String audiotime = this.getPara("audiotime");
		String isPush = this.getPara("isPush"); // 0--不推送 1--推送
		String mode = this.getPara("mode");

		String eventID = "";
		switch (isPush) {
		case "0":
			// 无推送
			eventID = TxService.contributeMemoryCardToOfficialAlbum(userid, groupid, picAddress, tags, audio, content,
					storage, memorytime, cardstyle, place, audiotime, 0, mode);// 上传动态并获取动态的ID
			break;
		case "1":
			// 有推送
			eventID = TxService.uploadMemoryCard(userid, groupid, picAddress, tags, audio, content, storage, memorytime,
					cardstyle, place, audiotime, mode);// 上传动态并获取动态的ID
			break;
		}
		if (!eventID.equals("")) {
			// 获取组内所有成员的ID
			List<Record> list = service.getGroupMemberID(groupid);
			// 插入数据到likes表中
			boolean flag = service.insertDataIntoLikes(eventID, list);
			if (flag) {
				jsonString = service.getSingleEventInOfficialAlbum(userid, eventID);// 获取动态的信息
			} else {
				jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
			}
		} else {
			jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 获取官方相册内的单条动态
	 */
	public void GetSingleEventInOfficialAlbum() {
		// 获取参数
		String userid = this.getPara("userid");
		String eventid = this.getPara("eventid");
		jsonString = service.getSingleEventInOfficialAlbum(userid, eventid);// 获取动态的信息
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 获取公共空间上传 token
	 */
	@Before(CrossDomain.class)
	public void GetUploadToken() {
		String token = operate.getUploadToken();
		if (token == "") {
			jsonString = jsonData.getJson(1008, "上传token获取失败");
		} else {
			List<Record> list = dataProcess.makeSingleParamToList("token", token);
			jsonString = jsonData.getJson(0, "success", list);
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 获取私有空间上传 token
	 */
	@Before(CrossDomain.class)
	public void GetPrivateSpaceUploadToken() {
		String token = operate.getPrivateUploadToken();
		if (token == "") {
			jsonString = jsonData.getJson(1008, "上传token获取失败");
		} else {
			List<Record> list = dataProcess.makeSingleParamToList("token", token);
			jsonString = jsonData.getJson(0, "success", list);
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 获取下载 token
	 */
	public void GetDownloadURL() {
		// 获取参数
		String url = this.getPara("url");
		String newURL = operate.getDownloadToken(url);
		List<Record> list = dataProcess.makeSingleParamToList("url", newURL);
		jsonString = jsonData.getJson(0, "success", list);
		// 返回结果
		renderText(jsonString);

	}

	/**
	 * 刷新动态
	 */
	public void EventRefresh() {
		// 获取参数
		String maxEventID = this.getPara("maxEventID");
		String groupid = this.getPara("groupid");
		jsonString = service.refreshEvent(maxEventID, groupid);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 加载评论
	 */
	public void CommentLoading() {
		// 获取参数
		String eventID = this.getPara("eventID");
		String maxCommentID = this.getPara("maxCommentID");
		jsonString = service.loadComment(maxCommentID, eventID);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 刷新消息
	 */
	public void RefreshMessage() {
		// 获取参数
		String userid = this.getPara("userid");
		String maxIGid = this.getPara("maxIGid");
		String maxMid = this.getPara("maxMid");
		// 获取刷新的消息内容
		List<Record> list = TxService.getNewMessage(userid, maxMid, maxIGid);
		jsonString = jsonData.getJson(0, "success", list);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 刷新通知
	 */
	public void RefreshNotification() {
		// 获取参数
		String userid = this.getPara("userid");
		String maxNid = this.getPara("maxNid");
		// 获取刷新的通知内容
		List<Record> list = TxService.getNewNotification(userid, maxNid);
		jsonString = jsonData.getJson(0, "success", list);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 刷新我的
	 */
	public void RefreshMe() {
		// 获取参数
		String userid = this.getPara("userid");
		String maxEventID = this.getPara("maxEventID");
		// 获取刷新的“我的”中的内容
		List<Record> list = service.getNewMe(userid, maxEventID);
		jsonString = jsonData.getJson(0, "success", list);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 获取消息模块的各个新消息数量
	 */
	public void GetUnreadInformationNumber() {
		// 获取参数
		String userid = this.getPara("userid");
		// 获取各个部分的未读消息数量
		Record commentRecord = service.getCount(userid, "comment");
		Record invitegroupRecord = service.getCount(userid, "invitegroup");
		Record notificationRecord = service.getCount(userid, "notification");
		// 将上面获取到的数据集合到一个record当中
		Record record = new Record().set("comment", Integer.parseInt(commentRecord.get("number").toString()))
				.set("invitegroup", Integer.parseInt(invitegroupRecord.get("number").toString()))
				.set("notification", Integer.parseInt(notificationRecord.get("number").toString()));
		List<Record> list = new ArrayList<Record>();
		list.add(record);
		jsonString = jsonData.getJson(0, "success", list);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 刷新组员个人动态
	 */
	public void RefreshGroupMemberEvents() {
		// 获取参数
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String maxID = this.getPara("maxID");
		jsonString = service.refreshMemberEvents(userid, groupid, maxID);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 修改组头像
	 */
	@Before(CrossDomain.class)
	public void ModifyGroupPic() {
		String url = this.getPara("url");
		String groupID = this.getPara("groupID");
		// 判断URL是否已经拼接好
		String temp = url.substring(0, 7);
		if (!(temp.equals("http://"))) {
			url = CommonParam.qiniuOpenAddress + url;
		}
		//鉴黄 by lk 
		String address = operate.getDownloadToken(url + "?nrop");
		// 发送请求并获取返回结果
		String result =new YinianDataProcess().sentNetworkRequest(address);
		if (!result.equals("")) {
			JSONObject jo = JSONObject.parseObject(result);
			int code = jo.getIntValue("code");
			if (code == 0) {
				JSONArray ja = jo.getJSONArray("fileList");
				JSONObject temp1 = ja.getJSONObject(0);

				// JSONObject temp = JSONObject.parseObject(jo.get("result").toString());
				int label = temp1.getIntValue("label");
				if (label ==0) {
					url=null;
				}
			}
		}
		//end lk
		// 获取上传所在组的类型，若是官方相册（5），则不不能修改
		List<Record> list = dao.query("gtype", "groups", "groupid=" + groupID + " ");
		String type = list.get(0).get("gtype").toString();
		if (type.equals("5")) {
			jsonString = jsonData.getJson(0, "success");
		} else {
			if(url==null){
				jsonString = dataProcess.updateFlagResult(false);
			}else{
				boolean flag = service.modifyGroupPic(url, groupID);
				jsonString = dataProcess.updateFlagResult(flag);
			}
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 删除评论
	 */
	public void DeleteComment() {
		String commentID = this.getPara("commentID");
		boolean flag = service.deleteComment(commentID);
		jsonString = dataProcess.updateFlagResult(flag);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 退出相册
	 */
	public void LeaveAlbum() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		if(userid==null||userid.equals("")||userid.equals("undefined")||userid.equals("NaN")||groupid==null||groupid.equals("")||groupid.equals("undefined")||groupid.equals("NaN")){
			jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		//不让用户退出
		List<Record> ynlist = CacheKit.get("DataSystem", groupid + "ynlist");
		if (ynlist == null) {
			ynlist = Db.find("select * from yntemp where id=48");
			CacheKit.put("DataSystem", groupid + "ynlist", ynlist);
		}
		if (ynlist != null && ynlist.size() != 0) {
			String yndata = ynlist.get(0).get("remark");
			List<String> result = Arrays.asList(yndata.split(","));
			System.out.println(result);
			if (result.contains(groupid)) {
				jsonString = jsonData.getJson(2, "参数错误", new ArrayList<Record>());
				renderText(jsonString);
				return;
			}
		}
		 //不让用户退出 end
		String source = this.getPara("source");
		// 获取组性质，若退出的是私密相册，则使用原方法；若退出的是官方相册，则使用退出官方相册方法
		List<Record> list = dao.query("gtype", "groups", "groupid=" + groupid + "");
		String gtype = list.get(0).get("gtype").toString();
		boolean quitFlag = false;
		if (gtype.equals("5")) {
			// 退出官方相册，给管理员发送通知与推送
			quitFlag = TxService.quitOfficialAlbum(userid, groupid);
		} else {
			// 退出相册，删除组成员数据，将该成员发送的动态等数据隐藏，成功返回true失败返回false
			quitFlag = TxService.quitAlbum(userid, groupid, source);
		}
		jsonString = dataProcess.deleteFlagResult(quitFlag);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 修改用户头像背景
	 */
	public void ModifyUserBackground() {
		String userid = this.getPara("userid");
		String url = this.getPara("url");
		// 判断，传过来的值没有http开头代表不是一个完整的链接
		if (!url.substring(0, 4).equals("http")) {
			url = CommonParam.qiniuOpenAddress + url;
		}
		boolean flag = service.modifyUserBackground(userid, url);
		jsonString = dataProcess.updateFlagResult(flag);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 获取有新动态的相册数量
	 */
	public void GetNewEventGroupNum() {
		String userid = this.getPara("userid");
		List<Record> list = service.getTheNumberOfGroupsWithNewEvent(userid);
		jsonString = jsonData.getJson(0, "success", list);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 更新用户cid, cid为用户登录的设备的机器码, 用于定位用户并推送消息
	 */
	public void updateUserCid() {
		String userid = this.getPara("userid");
		String cid = this.getPara("cid");
		String type = this.getPara("type");
		String device = this.getPara("device");
		boolean flag = service.updateUcid(userid, cid, type, device);
		jsonString = dataProcess.updateFlagResult(flag);
		// 返回结果
		renderText(jsonString);

	}

	/**
	 * 验证密码是否正确
	 */
	public void CheckPassword() {
		String userid = this.getPara("userid");
		String oldPassword = this.getPara("oldPassword");
		boolean flag = service.checkPassword(userid, oldPassword);
		if (flag) {
			jsonString = jsonData.getJson(0, "success");
		} else {
			jsonString = jsonData.getJson(1001, "密码错误");
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 通过userid修改密码
	 */
	public void ModifyPasswordByUserid() {
		String userid = this.getPara("userid");
		String newPassword = this.getPara("newPassword");
		boolean flag = dao.modifyUserSingleInfo(userid, newPassword, "password");
		jsonString = dataProcess.updateFlagResult(flag);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 通过邀请码申请进组
	 */
	@Before(Tx.class)
	public void ApplyIntoGroup() {
		// 获取参数
		String userid = this.getPara("userid");
		String inviteCode = this.getPara("inviteCode");
		int type = TxService.applyJoinGroup(userid, inviteCode);
		switch (type) {
		case 0:
			// 0--成功
			jsonString = jsonData.getJson(0, "success");
			break;
		case 1:
			// 1--邀请码不存在
			jsonString = jsonData.getJson(1011, "邀请码不存在");
			break;
		case 2:
			// 2--组已删除
			jsonString = jsonData.getJson(1012, "组已被删除");
			break;
		case 3:
			// 3--用户已经在组内
			jsonString = jsonData.getJson(1010, "用户已在组内");
			break;
		case 4:
			// 4--插入信息失败
			jsonString = jsonData.getJson(-50, "插入数据到数据库中失败");
			break;
		case 5:
			// 5--官方相册，直接进入相册，并只给管理员发推送
			// 通过inviteCode获取groupid
			List<Record> gidList = dao.query("groupid", "groups", "ginvite='" + inviteCode + "' ");
			String groupid = gidList.get(0).get("groupid").toString();
			// 不在组内，进组并返回组信息，删除邀请，发送通知
			List<Record> groupInfo = TxService.enterOfficialAlbum(groupid, userid);// 进组并返回组信息用于用户添加到他的组列表中
			// 将组类型转换成相应的文字
			groupInfo = dataProcess.changeGroupTypeIntoWord(groupInfo);
			// 插入数据到likes表中
			boolean insertFlag = TxService.newUserJoinInsertLikes(userid, groupid);
			boolean NotificationFlag = TxService.enterOfficialAlbumNotification(groupid, userid);
			if (NotificationFlag && insertFlag) {
				jsonString = jsonData.getJson(2000, "success", groupInfo);
			} else {
				jsonString = jsonData.getJson(-51, "更新数据失败");
			}
			break;
		default:
			jsonString = jsonData.getJson(0, "success");
			break;
		}
		// 返回结果
		renderText(jsonString);

	}

	/**
	 * 获取组内所有照片,照片墙
	 */
	public void GetAllPhotosInOneGroup() {
		// 获取参数
		String groupid = this.getPara("groupid");
		String type = this.getPara("type");
		int id = Integer.parseInt(this.getPara("id"));
		List<Record> list = service.getGroupPhotos(groupid, type, id);
		jsonString = jsonData.getJson(0, "success", list);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 按日、月显示照片墙
	 */
	@Before(CrossDomain.class)
	public void ShowPhotoWallByDayOrMonth() {
		String groupid = this.getPara("groupid");
		String type = this.getPara("type");
		String mode = this.getPara("mode");
		String date = this.getPara("date");
		String source = this.getPara("source");// 区分web和app

		// 来源为web则需要对groupid解密
		if (source != null && source.equals("web")) {
			groupid = dataProcess.decryptData(groupid, "groupid");
		}
		List<Record> result = CacheKit.get("ConcurrencyCache", groupid + "ShowPhotoWallByDayOrMonth");
		if (result == null) {
			result = service.getPhotoWallByDayOrMonth(groupid, type, date, mode, source);
			CacheKit.put("ConcurrencyCache", groupid + "ShowPhotoWallByDayOrMonth", result);
		}
		//List<Record> result = service.getPhotoWallByDayOrMonth(groupid, type, date, mode, source);
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);

	}

	/**
	 * 显示照片短视频墙
	 */
	public void ShowPhotoAndVideoWall() {
		String groupid = this.getPara("groupid");
		Group g=new Group().findById(groupid);
		if(null!=g&&null!=g.get("gtype")&&g.get("gtype").toString().equals("11")){
			jsonString = jsonData.getSuccessJson(new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		String type = this.getPara("type");
		String date = this.getPara("date");
		List<Record> result = CacheKit.get("ConcurrencyCache", groupid +"_"+type+"_"+date+ "ShowPhotoAndVideoWall");
		if (result == null) {
			result = service.getPhotoAndVideoWall(groupid, type, date);
			CacheKit.put("ConcurrencyCache", groupid +"_"+type+"_"+date+ "ShowPhotoAndVideoWall", result);
		}
		//List<Record> result = service.getPhotoAndVideoWall(groupid, type, date);
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
	}
	
	/**
	 * 显示照片短视频墙(new) 
	 */
	public void ShowPhotoAndVideoWallNew() {
		String groupid = this.getPara("groupid");
		Group g=new Group().findById(groupid);
		/*if(null!=g&&null!=g.get("gtype")&&g.get("gtype").toString().equals("11")){
			jsonString = jsonData.getSuccessJson(new ArrayList<Record>());
			renderText(jsonString);
			return;
		}*/
		String type = this.getPara("type");
		String date = this.getPara("date");
		List<Record> result = CacheKit.get("ConcurrencyCache", groupid +"_"+type+"_"+date+ "ShowPhotoAndVideoWall");
		if (result == null) {
			result = service.getPhotoAndVideoWallNew(groupid, type, date);
			CacheKit.put("ConcurrencyCache", groupid +"_"+type+"_"+date+ "ShowPhotoAndVideoWall", result);
		}
		
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
	}
	
	/**
	 * 显示照片短视频墙(按时间查询分页)
	 */
	public void ShowPhotoAndVideoWallByTime() {
		String groupid = this.getPara("groupid");
		int pagenum = Integer.parseInt(this.getPara("pagenum"));
		//Group g=new Group().findById(groupid);
	/*	if(null!=g&&null!=g.get("gtype")&&g.get("gtype").toString().equals("11")){
			jsonString = jsonData.getSuccessJson(new ArrayList<Record>());
			renderText(jsonString);
			return;
		}*/
		String date = this.getPara("date");
		List<Record> result = service.getPhotoAndVideoWallByTime(groupid, date,pagenum);
		//List<Record> result = service.getPhotoAndVideoWall(groupid, type, date);
		//查询本月的总照片数
		Record record = new Record();
			if(pagenum==1) {
				if(result.size()!=0) {
					// 获取照片数，缓存
					List<Record> photo = CacheKit.get("ConcurrencyCache", groupid + "Photo");
					if (photo == null) {
						photo = Db.find(
								"select count(*) as gpicNum from pictures where pGroupid="
										+ groupid + " and pstatus=0 and date_format(puploadtime,'%Y-%m')='"+date+"'");
						CacheKit.put("ConcurrencyCache", groupid + "Photo", photo);
						Long picnum = photo.get(0).getLong("gpicNum");
						record.set("picnum", picnum);
						result.add(record);
					}
				}else {
					record.set("picnum",0);
					result.add(record);
				}
			}
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
	}
	
	/**
	 * 显示查询日期的总照片数
	 */
	public void picNumByTime() {
		String groupid = this.getPara("groupid");
		int pagenum = Integer.parseInt(this.getPara("pagenum"));
		String mode = "\"%Y-%m-%d\"";
		String date = this.getPara("date");
		List<Record> result = new ArrayList<>();
		//查询本月的总照片数group by DATE_FORMAT( euploadtime, " + mode+ " )
		Record record = new Record();
		// 获取照片数，缓存
		List<Record> photo = CacheKit.get("ConcurrencyCache", groupid + "Photo");
		if (photo == null) {
			photo = Db.find(
					"select count(*) as gpicNum from pictures where pGroupid="
							+ groupid + " and pstatus=0 and date_format(puploadtime,'%Y-%m')='"+date+"'");
			CacheKit.put("ConcurrencyCache", groupid + "Photo", photo);
			Long picnum = photo.get(0).getLong("gpicNum");
			record.set("picnum", picnum);
			result.add(record);
		}
		
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
	}
	
	/**
	 * 显示照片短视频墙(查看更多一次显示30张)
	 */
	public void ShowPhotoAndVideoWallMore() {
		String groupid = this.getPara("groupid");
		int pagenum = Integer.parseInt(this.getPara("pagenum"));
		String uploadtime = this.getPara("uploadtime");
		List<Record> result = service.getPhotoAndVideoWallShowMore(groupid,uploadtime,pagenum);
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
	}

	/**
	 * 根据用户显示照片墙和短视频墙
	 */
	public void ShowPhotoAndVideoWallByUser() {
		String groupid = this.getPara("groupid");
		String type = this.getPara("type");
		String uploadTime = this.getPara("uploadTime");

		List<Record> list = new ArrayList<Record>();
		switch (type) {
		case "initialize":
			list = Db.find(
					"select userid,unickname,upic,count(*) as num,GROUP_CONCAT(pOriginal) as url,MAX(euploadtime) as uploadtime from users,`events`,pictures where userid=euserid and eid=peid and egroupid="
							+ groupid
							+ " and estatus=0 and pstatus=0 and eMain in (0,4) GROUP BY userid ORDER BY MAX(euploadtime) desc limit 10");
			break;
		case "loading":
			list = Db.find(
					"select userid,unickname,upic,count(*) as num,GROUP_CONCAT(pOriginal) as url,MAX(euploadtime) as uploadtime from users,`events`,pictures where userid=euserid and eid=peid and egroupid="
							+ groupid
							+ " and estatus=0 and pstatus=0 and eMain in (0,4) GROUP BY userid HAVING MAX( euploadtime ) < '"
							+ uploadTime + "'  ORDER BY MAX(euploadtime) desc limit 10");
			break;
		}
		// 资源授权
		QiniuOperate qiniu = new QiniuOperate();
		for (Record record : list) {
			String[] urlArray = record.getStr("url").split(",");		
			List<Record> picList = new ArrayList<>();
			int end = ((urlArray.length >= 9) ? 9 : urlArray.length);
			for (int i = 0; i < end; i++) {			
				String url = urlArray[i];
				String thumbnail = url + "?imageView2/1/w/200";
				String midThumbnail = url + "?imageView2/1/w/500";
				url = qiniu.getDownloadToken(url);
				midThumbnail = qiniu.getDownloadToken(midThumbnail);
				thumbnail = qiniu.getDownloadToken(thumbnail);
				Record picRecord = new Record().set("url", url).set("thumbnail", thumbnail).set("midThumbnail",
						midThumbnail);
				picList.add(picRecord);
			}
			record.remove("url");
			record.set("picList", picList);
		}
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);

	}
	
	/**
	 * 根据用户显示照片墙和短视频墙(新)按照前段要求修改高清图的返回字段
	 */
	public void ShowPhotoAndVideoWallByUserNew() {
		String groupid = this.getPara("groupid");
		String type = this.getPara("type");
		String uploadTime = this.getPara("uploadTime");

		List<Record> list = new ArrayList<Record>();
		switch (type) {
		case "initialize":
			list = Db.find(
					"select userid,unickname,upic,pMain,pcover as cover,count(*) as num,GROUP_CONCAT(poriginal) as url,MAX(puploadtime) as uploadtime from users,pictures where userid=puserid and pGroupid="
							+ groupid
							+ " and pstatus=0 GROUP BY userid ORDER BY MAX(puploadtime) desc limit 10");
			break;
		case "loading":
			list = Db.find(
					"select userid,unickname,upic,pMain,pcover as cover,count(*) as num,GROUP_CONCAT(pOriginal) as url,MAX(puploadtime) as uploadtime from users,pictures where userid=puserid and pGroupid="
							+ groupid
							+ " and pstatus=0 GROUP BY userid HAVING MAX(puploadtime ) < '"
							+ uploadTime + "'  ORDER BY MAX(puploadtime) desc limit 10");
			break;
		}
		// 资源授权
		QiniuOperate qiniu = new QiniuOperate();
		for (Record record : list) {
			String[] urlArray = record.getStr("url").split(",");
			String pMain = record.get("pMain").toString();
			List<Record> picList = new ArrayList<>();
			int end = ((urlArray.length >= 9) ? 9 : urlArray.length);
			for (int i = 0; i < end; i++) {			
				String url = urlArray[i];
				int one = url.lastIndexOf(".");
				String cover = url.substring((one+1),url.length());
				System.out.println("cover="+cover);
				String pcover = "";
				if(cover.equals("mp4")||cover.equals("MP4")
						||cover.equals("3gp")||cover.equals("3GP")
						||cover.equals("avi")||cover.equals("AVI")
						||cover.equals("flv")||cover.equals("FLV")
						||cover.equals("MKV")||cover.equals("mkv")) {
					pcover = qiniu.getDownloadToken(url + "?vframe/jpg/offset/1/w/750");
				}
				String thumbnail = url + "?imageView2/1/w/200";
				String midThumbnail = url + "?imageView2/1/w/500";
				url = qiniu.getDownloadToken(url);
				midThumbnail = qiniu.getDownloadToken(midThumbnail);
				thumbnail = qiniu.getDownloadToken(thumbnail);
				Record picRecord = new Record().set("poriginal", url).set("thumbnail", thumbnail).set("midThumbnail",
						midThumbnail).set("pMain", pMain).set("pcover", pcover);
				picList.add(picRecord);
			}
			record.remove("url");
			record.set("picList", picList);
		}
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);

	}
	
	/**
	 * 根据用户显示照片墙和短视频墙(按照时间显示)
	 */
	public void ShowPhotoAndVideoWallByUserTime() {
		String groupid = this.getPara("groupid");
		String uploadTime = this.getPara("uploadTime");
		int pagenum = Integer.parseInt(this.getPara("pagenum"));
		int page = (pagenum-1)*10;
		List<Record> list = new ArrayList<Record>();
		list = Db.find(
				"select userid,unickname,upic,pMain,peid,pcover as cover,count(*) as num,GROUP_CONCAT(pOriginal) as url,puploadtime as uploadtime from users,pictures where userid=puserid and pGroupid="
						+ groupid
						+ " and pstatus=0 and puploadtime like "
						+"'%"+ uploadTime +"%'"+ " group by userid ORDER BY puploadtime desc limit "+page+ ",10");
		if(list.size()!=0) {
			// 资源授权
			QiniuOperate qiniu = new QiniuOperate();
			for (Record record : list) {
				String[] urlArray = record.getStr("url").split(",");
				String pMain = record.get("pMain").toString();
				List<Record> picList = new ArrayList<>();
				int end = ((urlArray.length >= 9) ? 9 : urlArray.length);
				for (int i = 0; i < end; i++) {			
					String url = urlArray[i];
					int one = url.lastIndexOf(".");
					String cover = url.substring((one+1),url.length());
					System.out.println("cover="+cover);
					String pcover = "";
					if(cover.equals("mp4")||cover.equals("MP4")
							||cover.equals("3gp")||cover.equals("3GP")
							||cover.equals("avi")||cover.equals("AVI")
							||cover.equals("flv")||cover.equals("FLV")
							||cover.equals("MKV")||cover.equals("mkv")) {
						pcover = qiniu.getDownloadToken(url + "?vframe/jpg/offset/1/w/750");
					}
					String thumbnail = url + "?imageView2/1/w/200";
					String midThumbnail = url + "?imageView2/1/w/500";
					url = qiniu.getDownloadToken(url);
					midThumbnail = qiniu.getDownloadToken(midThumbnail);
					thumbnail = qiniu.getDownloadToken(thumbnail);
					Record picRecord = new Record().set("poriginal", url).set("thumbnail", thumbnail).set("midThumbnail",
							midThumbnail).set("pcover", pcover).set("pMain", pMain);
					picList.add(picRecord);
				}
				record.remove("url");
				record.set("picList", picList);
			}
		}
		
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);

	}

	/**
	 * 邀请网页获取信息接口 接受内容：userid,groupid 返回内容：邀请人头像、邀请人昵称、组名、组类别（状态码）、组照片、邀请码
	 */
	public void WebsiteInterface() {
		// 获取参数
		String key = this.getPara("key");
		try {
			String value = DES.decryptDES(key, CommonParam.DESSecretKey);
			List<Record> list = service.getWebsiteContent(value);
			if (list.size() == 0) {
				jsonString = jsonData.getJson(1012, "组已被删除");
			} else {
				jsonString = jsonData.getJson(0, "success", list);
			}
			// 返回结果
			renderText(jsonString);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			jsonString = jsonData.getJson(1014, "解密失败");
			// 返回结果
			renderText(jsonString);
			e.printStackTrace();
		}
	}

	/**
	 * 创建官方相册
	 */
	public void CreateOfficialAlbum() {
		// 获取参数
		String userid = this.getPara("userid");
		String groupName = this.getPara("groupName");
		String groupType = this.getPara("groupType");
		String url = this.getPara("url");
		String inviteCode = this.getPara("inviteCode");
		if (url.equals("")) {
			url += CommonParam.qiniuOpenAddress + CommonParam.otherGroup;
		} else {
			url = CommonParam.qiniuOpenAddress + url;
		}
		jsonString = service.createAlbum(groupName, userid, url, groupType, inviteCode, null);
		// 返回结果
		renderText(jsonString);
	}

	/*
	 * 点赞与取消点赞，官方相册版本，无具体表情
	 */
	public void LikeAndUnlike() {
		String userid = this.getPara("userid");
		String eventid = this.getPara("eventid");
		String type = this.getPara("type");
		boolean flag;
		switch (type) {
		case "like":
			flag = TxService.likeEvent(userid, eventid);
			jsonString = dataProcess.updateFlagResult(flag);
			break;
		case "unlike":
			flag = TxService.unlikeEvent(userid, eventid);
			jsonString = dataProcess.updateFlagResult(flag);
			break;
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 踢出相册
	 */
	public void KickMembers() {
		String owner = this.getPara("owner");
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");

		Group group = new Group().findById(groupid);
		boolean quitFlag = false;
		if (owner.equals(group.get("gcreator").toString())) {
			String[] IDs = userid.split(",");
			for (int i = 0; i < IDs.length; i++) {
				// 将成员踢出相册，删除组成员数据，将该成员发送的动态等数据隐藏，成功返回true失败返回false
				if (owner.equals(IDs[i])) {
					// 判断踢出的人是不是自己
					jsonString = jsonData.getJson(1035, "不能踢出自己");
				} else {
					quitFlag = TxService.kickOutAlbum(IDs[i], groupid);
					if (!quitFlag) {
						break;
					}
				}

			}
			jsonString = dataProcess.deleteFlagResult(quitFlag);
		} else {
			jsonString = jsonData.getJson(1034, "无权限踢人");
		}

		// 返回结果
		renderText(jsonString);

	}

	/**
	 * 修改官方相册信息
	 */
	public void ModifyOfficialAlbumInfo() {
		String groupid = this.getPara("groupid");
		String data = this.getPara("data");
		String type = this.getPara("type");
		switch (type) {
		case "groupPic":
			String url = CommonParam.qiniuOpenAddress + data;
			boolean flag = service.modifyGroupPic(url, groupid);
			jsonString = dataProcess.updateFlagResult(flag);
			break;
		case "groupName":
			jsonString = service.modifyGroupName(groupid, data);
			break;
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 获取、刷新、加载官方相册内动态
	 */
	public void GetOfficialAlbumEvents() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String eventid = this.getPara("eventid");
		String sign = this.getPara("sign");
		jsonString = service.getOfficialAlbumEvents(userid, groupid, eventid, sign);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 显示其他的官方相册
	 */
	public void ShowOtherOfficialAlbums() {
		String groupid = this.getPara("groupid");
		List<Record> list = dao.query("groupid,gname,gpic,ginvite", "groups",
				"gtype=5 and gstatus=0 and groupid not in (" + groupid + ")");
		jsonString = jsonData.getJson(0, "success", list);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 获取回忆信息
	 */
	public void GetMemory() {
		String key = this.getPara("userid");
		try {
			String value = DES.decryptDES(key, CommonParam.DESSecretKey);
			Record record = service.getPersonalMemory(value);
			List<Record> list = new ArrayList<Record>();
			list.add(record);
			jsonString = jsonData.getJson(1, "数据有效", list);
			renderText(jsonString);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			jsonString = jsonData.getJson(1014, "解密失败");
			// 返回结果
			renderText(jsonString);
			e.printStackTrace();

		}
	}

	/**
	 * 产品反馈
	 */
	public void ProductFeedback() {
		// 获取参数
		String userid = this.getPara("userid");
		String type = this.getPara("type");
		String content = this.getPara("content");
		String machine = this.getPara("machine");
		String system = this.getPara("system");
		boolean flag = service.insertFeedback(userid, content, type, machine, system);
		jsonString = dataProcess.insertFlagResult(flag);
		renderText(jsonString);
	}

	/**
	 * 清除所有消息（按评论、邀请、通知划分）
	 */
	public void ClearAllMessage() {
		// 获取参数
		String userid = this.getPara("userid");
		String type = this.getPara("type");
		boolean flag = TxService.clearAllMessage(userid, type);
		jsonString = dataProcess.updateFlagResult(flag);
		renderText(jsonString);
	}

	/**
	 * 删除单张照片
	 */
	@Before(CrossDomain.class)
	public void DeleteSinglePhoto() {
		// 获取参数
		String address = this.getPara("address");
		String eid = this.getPara("eid");
		boolean flag = service.deleteSinglePhoto(address, eid);
		jsonString = dataProcess.updateFlagResult(flag);
		renderText(jsonString);
	}

	/**
	 * 删除单张照片,带有userid，用于判断
	 */
	@Before(CrossDomain.class)
	public void DeleteSinglePhotoWithUserid() {
		// 获取参数
		String userid = this.getPara("userid");
		String address = this.getPara("address");
		String eid = this.getPara("eid");
		Event event = new Event().findById(eid);
		String euserid = event.get("euserid").toString();
		if (userid.equals(euserid)) {
			boolean flag = service.deleteSinglePhoto(address, eid);
			jsonString = dataProcess.updateFlagResult(flag);
		} else {
			jsonString = jsonData.getJson(1033, "无权限删除照片");
		}
		renderText(jsonString);
	}

	/**
	 * 获取评论消息信息
	 */
	public void GetCommentMessageInformation() {
		// 获取参数
		String userid = this.getPara("userid");
		String id = this.getPara("id");
		String type = this.getPara("type");
		jsonString = TxService.getCommentMessageInformation(userid, id, type);
		renderText(jsonString);
	}

	/**
	 * 获取邀请消息信息
	 */
	public void GetInviteMessageInformation() {
		// 获取参数
		String userid = this.getPara("userid");
		String id = this.getPara("id");
		String type = this.getPara("type");
		jsonString = TxService.getInviteMessageInformation(userid, id, type);
		renderText(jsonString);
	}

	/**
	 * 获取通知消息信息
	 */
	public void GetNotificationMessageInformation() {
		// 获取参数
		String userid = this.getPara("userid");
		String id = this.getPara("id");
		String type = this.getPara("type");
		jsonString = TxService.getNotificationMessageInformation(userid, id, type);
		renderText(jsonString);
	}

	/**
	 * 官方相册投稿
	 */
	@Before(Tx.class)
	public void ContributeToOfficialAlbum() {
		// 获取参数
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String picAddress = this.getPara("picAddress");
		String content = this.getPara("content");
		String shottime = this.getPara("shottime");
		String shotplace = this.getPara("shotplace");
		String mode = this.getPara("mode");
		String eventID = "";
		// 上传动态并获取动态的ID
		eventID = TxService.contributeToOfficialAlbum(userid, groupid, picAddress, content, shottime, shotplace, mode);
		if (!eventID.equals("")) {
			// 动态ID不为空时，代表上传成功，此时插入通知给管理员，并给管理员进行推送
			boolean notifyFlag = TxService.notifyAndPushToSingle(userid, CommonParam.superUserID, "contribute", groupid,
					"yes");
			if (notifyFlag) {
				jsonString = jsonData.getJson(0, "success");
			} else {
				jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
			}
		} else {
			jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 官方相册投稿记忆卡片
	 */
	@Before(Tx.class)
	public void ContributeMemoryCardToOfficialAlbum() {
		// 获取参数
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String picAddress = this.getPara("picAddress");
		String tags = this.getPara("tags");
		String audio = this.getPara("audio");
		String content = this.getPara("content");
		String storage = this.getPara("storage");
		String memorytime = this.getPara("memorytime");
		String cardstyle = this.getPara("cardstyle");
		String place = this.getPara("place");
		String audiotime = this.getPara("audiotime");
		String mode = this.getPara("mode");

		// 上传动态并获取动态的ID
		String eventID = TxService.contributeMemoryCardToOfficialAlbum(userid, groupid, picAddress, tags, audio,
				content, storage, memorytime, cardstyle, place, audiotime, 2, mode);
		if (!eventID.equals("")) {
			// 动态ID不为空时，代表上传成功，此时插入通知给管理员，并给管理员进行推送
			boolean notifyFlag = TxService.notifyAndPushToSingle(userid, CommonParam.superUserID, "contribute", groupid,
					"yes");
			if (notifyFlag) {
				jsonString = jsonData.getJson(0, "success");
			} else {
				jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
			}
		} else {
			jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 显示投稿内容界面
	 */
	public void ShowContributes() {
		// 获取参数
		String groupid = this.getPara("groupid");
		String type = this.getPara("type");
		String eventID = this.getPara("eventID");
		List<Record> list = service.getContributeContent(groupid, type, eventID);
		jsonString = jsonData.getJson(0, "success", list);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 审核投稿
	 */
	public void ExamineContributes() {
		// 获取参数
		String groupid = this.getPara("groupid");
		String eventID = this.getPara("eventID");
		String publishUserID = this.getPara("publishUserID");
		String type = this.getPara("type");
		jsonString = TxService.examineContributes(groupid, eventID, publishUserID, type);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 获取首页的banner
	 */
	public void GetMainBanner() {
		String userid = this.getPara("userid");
		List<Record> bannerList = service.getMainBanner(userid);
		jsonString = jsonData.getJson(0, "success", bannerList);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 获取活动列表
	 */
	public void GetActivityList() {
		List<Record> activityList = Db.find("select bid,btitle,bpic,bdata,btype from banner where bstatus=2  ");
		jsonString = jsonData.getSuccessJson(activityList);
		renderText(jsonString);
	}

	/**
	 * 显示官方相册列表
	 */
	public void ShowAllOfficialAlbum() {
		// 获取参数
		String userid = this.getPara("userid");
		List<Record> list = service.getOfficialAlbumList(userid);
		jsonString = jsonData.getJson(0, "success", list);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 显示校园相册列表
	 */
	public void ShowAllSchoolAlbum() {
		// 获取参数
		String userid = this.getPara("userid");
		List<Record> list = service.getSchoolAlbumList(userid);
		jsonString = jsonData.getJson(0, "success", list);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 显示单个官方相册的相册信息
	 */
	public void ShowSingleOffcialAlbumInfo() {
		String groupid = this.getPara("groupid");
		String userid = this.getPara("userid");
		List<Record> list = service.getSingleOfficialAlbumInfo(userid, groupid);
		jsonString = jsonData.getJson(0, "success", list);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 显示用户未进入时的官方相册的动态详情
	 */
	public void ShowOfficialAlbumEventsWhenUserNotIn() {
		// 获取参数
		String groupid = this.getPara("groupid");
		String eventid = this.getPara("eventid");
		String sign = this.getPara("sign");
		jsonString = service.getOfficialAlbumEventsWhenUserNotIn(groupid, eventid, sign);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 显示网页内相册信息
	 */
	public void ShowWebAlbumInformation() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		List<Record> list = service.getSingleOfficialAlbumInfo(userid, groupid);
		if (list.size() == 0) {
			jsonString = jsonData.getJson(1012, "相册已被删除");
		} else {
			jsonString = jsonData.getJson(0, "success", list);
		}
		renderText(jsonString);
	}

	/**
	 * 显示小程序相册信息
	 */
	@Before(CrossDomain.class)
	public void ShowSmallAppAlbumInformation() {
		String uid=this.getPara("userid");
		String gid=this.getPara("groupid");
		if(uid==null||gid==null||gid.equals("undefined")||gid.equals("NaN")){
			jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		//黑名单
		User u=new User().findById(uid);
		int inBlackList=1;
		if(null!=u&&null!=u.get("ustate")&&u.get("ustate").toString().equals("1")){
			inBlackList=0;
		}
		//黑名单
		// 基本参数
		int userid = this.getParaToInt("userid");
		int groupid = this.getParaToInt("groupid");
		// 用户来源参数
		String port = this.getPara("port");
		String fromUserID = this.getPara("fromUserID");
		
		Group group = new Group().findById(groupid);
		int eventQRCodeCanPublish=new GroupCanPublish().getGroupPublishByType(groupid+"", "1");
		//dialogShow=1 显示活动相册助手
		int dialogShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "2");
		//showAdvertisements=1 显示广告位
		int advertisementsShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "3");
		int status = Integer.parseInt(group.get("gstatus").toString());
		int gtype = Integer.parseInt(group.get("gtype").toString());
		int gAuthority = Integer.parseInt(group.get("gAuthority").toString());
		// 判断相册是否删除
		if (status == 0) {
			// 判断是否在相册中
			GroupMember gm = new GroupMember();
			boolean isInFlag = gm.judgeUserIsInTheAlbum(userid, groupid); // true时用户不在空间内

			boolean flag = true;
			boolean getNewGnum=false;
			int count = 1;
			Record record = new Record().set("joinStatus", 1);
			if (isInFlag) {
				// 不在相册，则插入用户数据
				gm = new GroupMember().set("gmgroupid", groupid).set("gmuserid", userid).set("gmPort", port)
						.set("gmFromUserID", fromUserID);

				// 捕获插入异常，用户重复点击时会导致插入失败
				try {
					flag = gm.save();
					// 更新分组表中组成员数量字段
					count = Db.update("update groups set gnum = gnum+1 where groupid='" + groupid + "' ");
					record.set("joinStatus", 0);
					getNewGnum=true;
				} catch (ActiveRecordException e) {
					flag = true;
					count = 1;

				}

			}

			if (flag && (count == 1)) {
				// 获取返回数据 ,gAuthority————0所有人 1只有创建者 2-部分
				record.set("gname", group.get("gname").toString()).set("gcreator", group.get("gcreator").toString())
						.set("gtype", gtype).set("gnum", group.get("gnum").toString())
						.set("gpic", group.get("gpic").toString()).set("gAuthority", gAuthority)
						.set("gOrigin", group.get("gOrigin").toString())
						.set("eventQRCodeCanPublish", eventQRCodeCanPublish)
						.set("dialogShow", dialogShow).set("inBlackList", inBlackList).set("advertisementsShow", advertisementsShow);
				if(getNewGnum){
					record.set("gnum", group.getLong("gnum")+1);
				}
				// 获取推送接收状态
				if (isInFlag) {
					// 不在空间内的用户直接返回0
					record.set("isPush", "0");
				} else {
					// 在空间内的用户去查询
					List<Record> push = Db.find("select gmIsPush from groupmembers where gmgroupid=" + groupid
							+ " and gmuserid=" + userid + "");
					record.set("isPush", push.get(0).get("gmIsPush").toString());
				}

				// 获取成员列表，缓存
				List<Record> groupMember = CacheKit.get("ConcurrencyCache", groupid + "Member");
				if (groupMember == null) {
					groupMember = Db.find(
							"select userid,unickname,upic,gmtime from users,groups,groupmembers where groupid=gmgroupid and users.userid=groupmembers.gmuserid and gmgroupid='"
									+ groupid + "' and gmstatus=0 order by gmid desc limit 10 ");
					CacheKit.put("ConcurrencyCache", groupid + "Member", groupMember);
				}

				// 获取照片数，缓存
				List<Record> photo = CacheKit.get("ConcurrencyCache", groupid + "Photo");
				if (photo == null) {
					photo = Db.find(
							"select count(*) as gpicNum from groups,events,pictures where peid=eid and groupid=egroupid and groupid="
									+ groupid + " and estatus in(0,3) and pstatus=0 ");
					CacheKit.put("ConcurrencyCache", groupid + "Photo", photo);
				}

				// 获取发布权限列表，缓存，当空间发布权限为部分人时才查询并返回
				if (gAuthority == 2) {
					List<Record> uploadAuthority = CacheKit.get("ConcurrencyCache", groupid + "Authority");
					if (uploadAuthority == null) {
						uploadAuthority = Db.find("select gmuserid as userid from groupmembers where gmgroupid="
								+ groupid + " and gmauthority=1 ");
						CacheKit.put("ConcurrencyCache", groupid + "Authority", uploadAuthority);
					}
					record.set("authorityList", uploadAuthority);
				}
				record.set("picNum", photo.get(0).get("gpicNum").toString()).set("memberList", groupMember);

				List<Record> result = new ArrayList<Record>();
				result.add(record);
				jsonString = jsonData.getSuccessJson(result);

			} else {
				jsonString = dataProcess.insertFlagResult(false);
			}

		} else if (status == 1) {
			jsonString = jsonData.getJson(1012, "相册已被删除");
		} else {
			jsonString = jsonData.getJson(1037, "相册已被封");
		}
		renderText(jsonString);
	}

	/**
	 * 显示相册信息，浏览模式
	 */
	@Before(CrossDomain.class)
	public void ShowSpaceInformationWithWatchMode() {
		String groupid = this.getPara("groupid");
		String source = this.getPara("source");

		// 来源为web则需要对groupid解密
		if (source != null && source.equals("web")) {
			groupid = dataProcess.decryptData(groupid, "groupid");
			System.out.println(groupid);
		}
		// 获取信息
		jsonString = service.getSpaceInfo(groupid);
		renderText(jsonString);
	}

	/**
	 * 显示相册信息，加入模式
	 */
	@Before(CrossDomain.class)
	public void ShowSpaceInformationWithJoinMode() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String source = this.getPara("source");

		// 来源为web则需要对userid,groupid解密
		if (source != null && source.equals("web")) {
			groupid = dataProcess.decryptData(groupid, "groupid");
			userid = dataProcess.decryptData(userid, "userid");
		}

		// 判断是否在相册中
		GroupMember gm = new GroupMember();
		boolean isInFlag = gm.judgeUserIsInTheAlbum(Integer.parseInt(userid), Integer.parseInt(groupid)); // true时用户不在空间内
		if (isInFlag) {
			// 不在相册，则插入用户数据
			gm = new GroupMember().set("gmgroupid", groupid).set("gmuserid", userid);
			// 捕获插入异常，用户重复点击时会导致插入失败
			try {
				gm.save();
				// 更新分组表中组成员数量字段
				Db.update("update groups set gnum = gnum+1 where groupid='" + groupid + "' ");
			} catch (ActiveRecordException e) {

			}
		}

		// 获取空间信息
		jsonString = service.getSpaceInfo(groupid);
		renderText(jsonString);
	}

	/**
	 * 显示网页内相册动态内容
	 */
	public void ShowWebAlbumContents() {
		String groupid = this.getPara("groupid");
		List<Record> list = service.getWebAlbumContent(groupid);
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * 申请进入相册，网页端接口
	 */
	@Before(Tx.class)
	public void EnterAlbum() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
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
	 * 申请进入相册，网页端接口 加密 by lk 
	 */
	@Before(Tx.class)
	public void EnterAlbumWithEncryption() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		if(null==userid||null==groupid||userid.equals("")||groupid.equals("")){
			jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		//开始解密
				boolean canDES=false;
				try{			
					userid=URLDecoder.decode(userid);
					userid=DES.decryptDES(userid, CommonParam.DESSecretKey);
					groupid=URLDecoder.decode(groupid);
					groupid=DES.decryptDES(groupid, CommonParam.DESSecretKey);
					canDES=true;
				}catch(Exception e){
					e.printStackTrace();
				}finally{
					if(!canDES){
						jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
						renderText(jsonString);
						return;
					}
					//System.out.println(a1);
				}
				//解密end
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
	 * 统计数据方法
	 */
	public void GetStatisticsInfo() {
		String userid = this.getPara("userid");
		String data = this.getPara("data");
		String type = this.getPara("type");
		boolean flag = service.insertStatisticsInfo(userid, type, data);
		jsonString = dataProcess.insertFlagResult(flag);
		renderText(jsonString);
	}

	/**
	 * 给单个用户发送系统消息
	 */
	public void SendSystemNotificationToSingleUser() {
		String userid = this.getPara("userid");
		String content = this.getPara("content");
		Notification notification = new Notification().set("nsender", 10).set("nreceiver", userid)
				.set("ncontent", content).set("ngroupid", 1).set("ntype", 0);
		if (notification.save()) {
			jsonString = jsonData.getJson(0, "success");
		} else {
			jsonString = "false";
		}
		renderText(jsonString);
	}

	/**
	 * 获取主题
	 */
	public void GetThemes() {
		String type = this.getPara("type");
		List<Record> list = service.getProjectPublicInfo("theme", type);
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * 获取卡片样式
	 */
	public void GetCardStyles() {
		List<Record> list = service.getProjectPublicInfo("cardStyle", null);
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * 获取固定相册封面列表
	 */
	public void GetAlbumCovers() {
		// String gtype =this.getPara("gtype");
		List<Record> list = service.getProjectPublicInfo("albumCover", null);
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * 相册排序
	 */
	public void SortAlbums() {
		String userid = this.getPara("userid");
		String order = this.getPara("order");
		Gson gson = new Gson();
		Receiver receiver = gson.fromJson(order, Receiver.class);
		List<String> list = receiver.getOrder();
		boolean flag = TxService.sortAlbumSequence(userid, list);
		jsonString = dataProcess.updateFlagResult(flag);
		renderText(jsonString);

	}

	/**
	 * 日活
	 */
	public void DailyUser() {
		Record totalUserRecord = Db.findFirst("select count(*) as num from users ");
		int totalUser = Integer.parseInt(totalUserRecord.get("num").toString());
		double dailyUserPercent = 0.2;
		int dailyUser = (int) (totalUser * dailyUserPercent);
		Random random = new Random();
		for (int i = 0; i < dailyUser; i++) {
			int num = random.nextInt(totalUser);
			String loginDate = "2016-04-13";
			int hour = random.nextInt(24);
			int minute = random.nextInt(60);
			int second = random.nextInt(60);
			String loginTime = hour + ":" + minute + ":" + second;
			String loginDateTime = loginDate + " " + loginTime;
			Db.update("update users set ulogintime='" + loginDateTime + "' where userid=" + num + " ");
		}
	}

	/**
	 * 显示音乐相册
	 */
	public void ShowMusicAlbums() {
		String groupid = this.getPara("groupid");
		String maID = this.getPara("maID");
		String type = this.getPara("type");
		List<Record> list = service.getMusicAlbums(groupid, maID, type);
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * 获取单个音乐相册
	 */
	public void H5GetSingleMusicAlbum() {
		String code = this.getPara("code");
		try {
			String value = DES.decryptDES(code, CommonParam.DESSecretKey);
			String[] array = value.split(",");
			String eid = "";
			for (int i = 0; i < array.length; i++) {
				if (((array[i].split("="))[0]).equals("eid")) {
					eid = (array[i].split("="))[1];
				}
			}
			List<Record> list = service.getSingleMusicAlbum(eid);
			if (list.size() == 0) {
				jsonString = jsonData.getJson(1019, "音乐相册已删除");
			} else {
				jsonString = jsonData.getJson(0, "success", list);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			jsonString = jsonData.getJson(-53, "获取数据库数据失败");
		}
		renderText(jsonString);

	}

	/**
	 * 显示模板与音乐信息
	 */
	public void ShowTempletAndMusicInfo() {
		List<Record> musicList = service.getMusicInfo();
		List<Record> templetList = service.getTempletInfo();
		for (Record templetRecord : templetList) {
			List<Record> list = new ArrayList<Record>();
			for (Record musicRecord : musicList) {
				if ((musicRecord.get("musicTempletID").toString()).equals(templetRecord.get("templetID").toString())) {
					list.add(musicRecord);
				}
			}
			templetRecord.set("music", list);
		}
		jsonString = jsonData.getJson(0, "success", templetList);
		renderText(jsonString);
	}

	/**
	 * 创建音乐相册
	 */
	public void CreateMusicAlbum() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String albumName = this.getPara("albumName");
		String musicid = this.getPara("musicid");
		String templetid = this.getPara("templetid");
		String content = this.getPara("content");
		String picAddress = this.getPara("picAddress");
		String maID = TxService.createMusicAlbum(userid, groupid, albumName, musicid, templetid, content, picAddress);
		if (!maID.equals("")) {
			List<Record> list = service.getSingleMusicAlbum(maID);
			jsonString = jsonData.getJson(0, "success", list);
		} else {
			jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
		}
		renderText(jsonString);
	}

	/**
	 * 删除音乐相册
	 */
	public void DeleteMusicAlbum() {
		String maID = this.getPara("maID");
		boolean flag = TxService.deleteMusicAlbum(maID);
		jsonString = dataProcess.deleteFlagResult(flag);
		renderText(jsonString);
	}

	/**
	 * 显示时光罗盘内容
	 */
	public void ShowTimeCompass() {
		String userid = this.getPara("userid");
		List<Record> list = service.getTimeCompass(userid);
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * 显示时光罗盘内的照片
	 */
	public void ShowPhotosInTimeCompass() {
		String pid = this.getPara("pid");
		String groupid = this.getPara("groupid");
		String year = this.getPara("year");
		String month = this.getPara("month");
		List<Record> list = service.getPhotosInTimeCompass(groupid, pid, year, month);
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * 判断用户是否为新用户
	 */
	public void JudgeUserIsNew() {
		String userid = this.getPara("userid");
		List<Record> list = Db.find("select * from groups,groupmembers where groupid=gmgroupid and gmuserid = " + userid
				+ " and gtype not in (6,7,8)   ");
		if (list.size() == 0) {
			jsonString = jsonData.getJson(0, "success");
		} else {
			jsonString = jsonData.getJson(2003, "不是新用户");
		}
		renderText(jsonString);
	}

	/**
	 * 获取用户数据
	 */
	@Before(CrossDomain.class)
	public void GetUserData() {
		PointsService pointsService = new PointsService();
		String userid = this.getPara("userid");
		String source = this.getPara("source");

		// userid解密
		if (source != null && source.equals("h5")) {
			userid = dataProcess.decryptData(userid, "userid");
		}
		// 黑名单
		User u = new User().findById(userid);
		int inBlackList = 1;
		if (null != u && null != u.get("ustate") && u.get("ustate").toString().equals("1")) {
			inBlackList = 0;
		}
		// 黑名单
		Record record = new Record();
		List<Record> list = CacheKit.get("DataSystem", userid + "_GetUserData");
		if (userid != null) {
			if (list == null) {
				Record albumRecord = Db.findFirst(
						"select count(*) as number from groups,groupmembers where groupid=gmgroupid and gmuserid="
								+ userid + " and gmstatus=0 and gstatus=0 and gtype not in (5,12) and groupid not in ("
								+ CommonParam.ActivitySpaceID + ")");
				Record eventRecord = Db
						.findFirst("select count(*) as number from events where euserid=" + userid + " and estatus=0 ");
				/*
				 * 老版本 Record photoRecord = Db.
				 * findFirst("select count(*) as number from events,pictures where euserid="
				 * + userid + " and eid=peid and estatus=0 and pstatus=0 ");
				 */
				// by lk
				Record photoRecord = Db.findFirst(
						"select count(*) as number from pictures where puserid=" + userid + " and pstatus=0 ");
				record.set("album", albumRecord.get("number").toString())
						.set("event", eventRecord.get("number").toString())
						.set("photo", photoRecord.get("number").toString()).set("inBlackList", inBlackList);
				// 获取用户存储空间信息
				Record storage = Db.findFirst(
						"select uusespace,utotalspace,upic,unickname,ubackground,userid from users where userid="
								+ userid + " ");
				java.text.DecimalFormat df = new java.text.DecimalFormat("########");
				// System.out.println("----"+df.format(storage.getDouble("utotalspace")));
				// System.out.println("111----"+df.format(storage.getDouble("uusespace")));
				record.setColumns(storage);
				int points = pointsService.getUseablePoints(userid);
				record.set("points", points);
				list = new ArrayList<Record>();
				list.add(record);
				// photo=Db.find("select * from 5268248");
				// allList =GetPublishListByExcell(groupid+"");
				CacheKit.put("DataSystem", userid + "_GetUserData", list);
				jsonString = jsonData.getJson(0, "success", list);
			} else {
				jsonString = jsonData.getJson(0, "success", list);
			}
		} else {
			record.set("album", 0).set("event", 0).set("photo", 0).set("uusespace", 0).set("utotalspace", 0)
					.set("inBlackList", inBlackList);
			list = new ArrayList<Record>();
			list.add(record);
			jsonString = jsonData.getJson(0, "success", list);
		}
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
	 * 显示今日忆年
	 */
	public void ShowTodayMemory() {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
		// 获取今天的日期
		String today = df.format(new Date());
		// 获取昨天日期
		Date d = new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
		String yesterday = df.format(d);
		List<Record> list = Db.find("select TMid,TMpic,TMaudio,TMtext,TMtime from todaymemory where TMtime like '"
				+ today + "%' or TMtime like '" + yesterday + "%' and TMstatus=0 order by TMtime limit 2");
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * 上传礼包内容
	 */
	public void UploadGiftContent() {
		String userid = this.getPara("userid");
		String nickname = this.getPara("nickname");
		String realName = this.getPara("realName");
		String phonenumber = this.getPara("phonenumber");
		String address = this.getPara("address");
		String picUrl = this.getPara("picUrl");
		String[] urls = picUrl.split(",");
		String newUrl = "";
		for (int i = 0; i < urls.length; i++) {
			newUrl += (CommonParam.qiniuOpenAddress + urls[i] + ",");
		}
		if (!newUrl.equals("")) {
			newUrl = newUrl.substring(0, newUrl.length() - 1);
		}
		Gift gift = new Gift().set("giftUserID", userid).set("giftUserNickname", nickname).set("giftRealName", realName)
				.set("giftPhoneNumber", phonenumber).set("giftAddress", address).set("giftPic", newUrl);
		boolean flag = gift.save();
		jsonString = dataProcess.insertFlagResult(flag);
		renderText(jsonString);

	}

	/**
	 * 获取第一份礼包
	 */
	public void GetFirstGiftPacket() {
		String userid = this.getPara("userid");
		List<Record> list = Db.find("select * from push where pushUserid=" + userid + " ");
		if (list.size() == 0) {
			Push push = new Push().set("pushUserid", userid);
			boolean flag = push.save();
			jsonString = dataProcess.insertFlagResult(flag);
		} else {
			jsonString = jsonData.getJson(1020, "用户已领取过第一份礼包");
		}
		renderText(jsonString);
	}

	/**
	 * 获取第二份礼包
	 */
	public void GetSecondGiftPacket() {
		String userid = this.getPara("userid");
		List<Record> list = Db.find("select pushID from push where pushUserid=" + userid + " ");
		String pushID = list.get(0).get("pushID").toString();
		Push push = new Push().findById(pushID);
		push.set("pushStatus", 1);
		boolean flag = push.update();
		jsonString = dataProcess.updateFlagResult(flag);
		renderText(jsonString);
	}

	/**
	 * 判断是否领取了第二份礼包
	 */
	public void JudgeWhetherGetSecondGiftPacket() {
		String userid = this.getPara("userid");
		List<Record> list = Db.find("select pushStatus from push where pushUserid=" + userid + " ");
		if (list.size() == 0) {
			jsonString = jsonData.getJson(1021, "用户未领取第一份礼包");
		} else {
			String status = list.get(0).get("pushStatus").toString();
			if (status.equals("0")) {
				jsonString = jsonData.getJson(0, "success");
			} else {
				jsonString = jsonData.getJson(1022, "用户已领取过第二份礼包");
			}
		}
		renderText(jsonString);
	}

	// /**
	// * 推送给用户今日忆
	// */
	// public void PushToUsersAboutTodayMemory() {
	// String content = this.getPara("content");
	// String code = this.getPara("code");
	// // 解密
	// try {
	// String result = DES.decryptDES(code, "YZadZjYx");
	// if (result.equals(CommonParam.TodayMemoryPushCode)) {
	// // 推送接口
	// Record record = new Record().set("content", content);
	// List<Record> list = new ArrayList<Record>();
	// list.add(record);
	// String transmissContent = jsonData.getJson(4, "推送全体", list);
	// Record data = new Record().set("content", content);
	// // 推送！
	// push.pushMessageToApp(transmissContent, data);
	// jsonString = jsonData.getSuccessJson();
	// } else {
	// jsonString = jsonData.getJson(1023, "推送密文错误");
	// }
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// jsonString = jsonData.getJson(1014, "解密失败");
	// }
	// renderText(jsonString);
	// }

	/**
	 * 推送通知用户领取第二份礼包
	 */
	public void PushToNotisfyUserToGetSecondGiftPacket() {
		// 获取昨天日期
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
		Date date = new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
		String yesterday = df.format(date);
		// 获取需要推送的用户Cid列表
		List<Record> cidList = Db
				.find("select userid,ucid,udevice from users,push where userid=pushUserid and pushTime like '"
						+ yesterday + "%' and pushStatus=0 ");
		// 推送列表不为空则进行推送
		if (cidList.size() != 0) {
			PushMessage pm = new PushMessage();
			Record cids = pm.disposeCidsByDevice(cidList);
			Record data = new Record().set("content", "进入忆年领取免费打印照片的机会~").set("transmissionContent",
					"进入忆年领取免费打印照片的机会~");
			// 推送
			push.yinianPushToListWithAndroidNotification(cids, data);
		}
		jsonString = jsonData.getSuccessJson();
		renderText(jsonString);
	}

	/**
	 * 修改用户邀请标志
	 */
	public void ModifyUserInviteSign() {
		String userid = this.getPara("userid");
		User user = new User().findById(userid);
		int uInviteTotal = Integer.parseInt(user.get("uInviteTotal").toString());
		user.set("uInviteTotal", uInviteTotal + 1);
		boolean flag = user.update();
		jsonString = dataProcess.updateFlagResult(flag);
		renderText(jsonString);
	}

	/**
	 * 用户空间领取情况 加入一个相册可领取一次，邀请一个好友可领取一次，因此可以把“个”和“次”看成是等价的
	 */
	public void UserStoragePlaceCondition() {
		String userid = this.getPara("userid");
		Record record = Db.findFirst(
				"select uInviteReward,uJoinReward,uCreateReward,uInviteTotal from users where userid=" + userid + " ");
		// 将变量都转成整型，用于后续的比较判断
		int uInviteReward = Integer.parseInt(record.get("uInviteReward").toString());
		int uJoinReward = Integer.parseInt(record.get("uJoinReward").toString());
		int uInviteTotal = Integer.parseInt(record.get("uInviteTotal").toString());

		// 获取加入相册奖励的可领取次数和是否可以继续领取
		List<Record> joinList = Db.find("select * from groups,groupmembers where groupid=gmgroupid and gmuserid="
				+ userid + " and gmuserid!=gcreator");
		int uJoinTotal = joinList.size();
		// 存入用户加入相册的总数
		record.set("uJoinTotal", uJoinTotal);
		if (uJoinReward < uJoinTotal) {
			record.set("uJoinReward", 2);
		} else {
			record.set("uJoinReward", 0);
		}

		// 判断用户邀请好友奖励的可领取次数和是否可以领取
		if (uInviteReward < uInviteTotal) {
			record.set("uInviteReward", 2);
		} else {
			record.set("uInviteReward", 0);
		}

		// 判断创建相册奖励情况，如果已领取，则直接返回，如果未领取，则判断是否创建超过三个相册（只有这个奖励只能领取一次，所以要判断是否领取了）
		List<Record> createList = Db.find("select * from groups where gcreator=" + userid + " ");
		int uCreateTotal = createList.size();
		// 存入用户创建相册总数
		record.set("uCreateTotal", uCreateTotal);
		if ((record.get("uCreateReward").toString()).equals("0")) {
			if (uCreateTotal >= 3) {
				record.set("uCreateReward", 2);
			}
		}

		// 更新用户数据
		User user = new User().findById(userid);
		user.set("uJoinTotal", uJoinTotal).set("uCreateTotal", uCreateTotal);
		user.update();

		List<Record> list = new ArrayList<Record>();
		list.add(record);
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * 用户领取奖励空间
	 */
	public void UserGetRewardStoragePlace() {
		String userid = this.getPara("userid");
		String type = this.getPara("type");
		User user = new User().findById(userid);

		// 获取用户相关数据
		int uInviteReward = Integer.parseInt(user.get("uInviteReward").toString());
		int uJoinReward = Integer.parseInt(user.get("uJoinReward").toString());
		int uInviteTotal = Integer.parseInt(user.get("uInviteTotal").toString());
		int uJoinTotal = Integer.parseInt(user.get("uJoinTotal").toString());

		Double totalSpace = user.getDouble("utotalspace");
		Double addQuantity = 0.0;

		int time = 1;// 领取的次数，默认为1

		switch (type) {
		case "uInviteReward":
			addQuantity = 524288.0;
			time = uInviteTotal - uInviteReward;
			totalSpace += (addQuantity * time);
			// 将已领取次数设置为最大
			user.set("uInviteReward", uInviteTotal);
			break;
		case "uJoinReward":
			addQuantity = 524288.0;
			time = uJoinTotal - uJoinReward;
			totalSpace += (addQuantity * time);
			// 将已领取次数设置为最大
			user.set("uJoinReward", uJoinTotal);
			break;
		case "uCreateReward":
			addQuantity = 2097152.0;
			totalSpace += addQuantity;
			user.set("uCreateReward", 1);
			break;
		}
		user.set("utotalspace", totalSpace);
		boolean flag = user.update();
		jsonString = dataProcess.updateFlagResult(flag);
		renderText(jsonString);
	}

	/**
	 * 获取用户历史标签与官方标签
	 */
	public void GetUserAllTags() {
		String userid = this.getPara("userid");
		// 获取官方标签
		List<Record> officialTagsList = Db.find(
				"select OfficialTagContent as tagContent,OfficialTagType as tagType from officialtag where OfficialTagType=2 ");
		// 获取历史标签
		List<Record> historyTagsList = Db.find(
				"select historyTagContent as tagContent,historyTagType as tagType from historytag where historyTagType=1 and historyTagUserID="
						+ userid + " ORDER BY historyTagID desc limit 15  ");
		// 将两个列表结合
		officialTagsList = dataProcess.combineTwoList(officialTagsList, historyTagsList);
		jsonString = jsonData.getJson(0, "success", officialTagsList);
		renderText(jsonString);
	}

	/**
	 * 用户删除历史标签
	 */
	@Before(Tx.class)
	public void UserDeleteHistoricalTag() {
		String userid = this.getPara("userid");
		String content = this.getPara("content");
		int count = Db.update("update historytag set historyTagType=0 where historyTagUserID=" + userid
				+ " and historyTagContent='" + content + "' and historyTagType=1 ");
		jsonString = dataProcess.updateFlagResult((count == 1));
		renderText(jsonString);
	}

	/**
	 * 修改动态
	 */
	public void ModifyEventInfo() {
		String eventid = this.getPara("eventid");
		String action = this.getPara("action");// add或modify或remove三种表示增加、修改或删除动态的某个信息
		String type = this.getPara("type");
		String data = this.getPara("data");
		String linkedData = this.getPara("linkedData");
		boolean flag = false;
		switch (action) {
		case "add":
			flag = TxService.addEventInfo(eventid, type, data, linkedData);
			break;
		case "modify":
			flag = TxService.modifyEventInfo(eventid, type, data, linkedData);
			break;
		case "remove":
			flag = service.removeEventInfo(eventid, type, data, linkedData);
			break;
		}
		jsonString = dataProcess.updateFlagResult(flag);
		renderText(jsonString);
	}

	/**
	 * 获取所有卡片样式
	 */
	public void ShowAllCardStyles() {
		List<Record> list = Db.find("select csid,csname,cspic,csurl from cardstyle where cstatus=0 and csid!=1 ");
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * 获取官方封面
	 */
	public void GetAllOfficialCovers() {
		String type = this.getPara("type");
		List<Record> list = new ArrayList<Record>();
		if (type == null || type.equals("")) {
			list = Db.find(
					"select officialCoverID,officialCoverPicture,officialCoverTheme,isRecommend from officialcover where coverType=2 and officialCoverStatus=0 ");
		} else {
			list = Db.find(
					"select officialCoverID,officialCoverPicture,officialCoverTheme,isRecommend from officialcover where coverType=3 and officialCoverStatus=0 ");
		}
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * 获取历史封面
	 */
	public void GetUserHistoryCovers() {
		String userid = this.getPara("userid");
		List<Record> list = Db
				.find("select historyCoverID,historyCoverPicture from historycover where historyCoverUserID=" + userid
						+ " and historyCoverStatus=0 ");
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * 获取封面主题
	 */
	public void GetCoverTheme() {
		String type = this.getPara("type");
		List<Record> list = new ArrayList<Record>();
		if (type == null || type.equals("")) {
			list = Db.find("select distinct officialCoverTheme from officialcover where coverType=2 ");
		} else {
			list = Db.find("select distinct officialCoverTheme from officialcover where coverType=3 ");
		}
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * 获取封面主题下的所有图片
	 */
	public void GetThemePicture() {
		String theme = this.getPara("theme");
		String type = this.getPara("type");
		List<Record> list = new ArrayList<Record>();
		if (type == null || type.equals("")) {
			list = Db.find(
					"select officialCoverID,officialCoverPicture,officialCoverTheme from officialcover where officialCoverTheme='"
							+ theme + "' and officialCoverStatus=0 and coverType=2 ");
		} else {
			list = Db.find(
					"select officialCoverID,officialCoverPicture,officialCoverTheme from officialcover where officialCoverTheme='"
							+ theme + "' and officialCoverStatus=0 and coverType=3 ");
		}

		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * 获取推荐封面
	 */
	public void GetRecommendCover() {
		String type = this.getPara("type");
		List<Record> list = new ArrayList<Record>();
		if (type == null || type.equals("")) {
			list = Db.find(
					"select officialCoverID,officialCoverPicture,officialCoverTheme,isRecommend from officialcover where isRecommend=1 and officialCoverStatus=0 and coverType=2 ");
		} else {
			list = Db.find(
					"select officialCoverID,officialCoverPicture,officialCoverTheme,isRecommend from officialcover where isRecommend=1 and officialCoverStatus=0 and coverType=3 ");
		}
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * 获取下载token
	 */
	public void GetDownloadToken() {
		String url = this.getPara("url");
		QiniuOperate qiniu = new QiniuOperate();

		String[] array = url.split(",");
		String newURL = "";
		for (int i = 0; i < array.length; i++) {
			newURL += qiniu.getDownloadToken(array[i]) + ",";
		}
		newURL = newURL.substring(0, newURL.length() - 1);
		List<Record> list = dataProcess.makeSingleParamToList("url", newURL);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 显示备份照片
	 */
	@Before(CrossDomain.class)
	public void ShowBackupPhotos() {
		String userid = this.getPara("userid");
		String sign = this.getPara("sign");
		String date = this.getPara("date");
		List<Record> list = service.showBackupPhotos(userid, date, sign);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);

	}

	/**
	 * 获取备份动态数
	 */
	@Before(CrossDomain.class)
	public void GetBackupEventNum() {
		String userid = this.getPara("userid");
		List<Record> list = Db.find(
				"select count(*) as number from backupevent where backupUserID=" + userid + " and backupStatus=0 ");
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 删除单张备份照片
	 */
	@Before(CrossDomain.class)
	public void DeleteSingleBackupPhoto() {
		String backupPhotoID = this.getPara("backupPhotoID");
		int size = backupPhotoID.split(",").length;
		int count = Db.update("update backupphoto set backupPStatus=1 where backupPhotoID in(" + backupPhotoID + ") ");
		jsonString = dataProcess.updateFlagResult(size == count);
		renderText(jsonString);
	}

	/**
	 * 删除单日备份照片
	 */
	@Before(Tx.class)
	public void DeleteSingleDayBackupPhotos() {
		String backupEventID = this.getPara("backupEventID");
		String[] array = backupEventID.split(",");
		int eventSize = array.length;
		int eventNum = Db
				.update("update backupevent set backupStatus=1 where backupEventID in (" + backupEventID + ") ");
		List<Record> list = Db
				.find("select * from backupphoto where backupPEid in (" + backupEventID + ") and backupPStatus=0");
		int photoSize = list.size();
		int photoNum = Db.update("update backupphoto set backupPStatus=1 where backupPEid in (" + backupEventID
				+ ") and backupPStatus=0 ");
		boolean flag = (eventSize == eventNum) && (photoSize == photoNum);
		jsonString = dataProcess.updateFlagResult(flag);
		renderText(jsonString);
	}

	/**
	 * 备份照片
	 */
	@Before({ Tx.class, CrossDomain.class })
	public void BackupPhotos() {
		String userid = this.getPara("userid");
		String data = this.getPara("data");
		String device = this.getPara("device");

		JSONArray array = JSONArray.parseArray(data);
		Double totalStorage = 0.0;
		boolean flag = true;

		for (int i = 0; i < array.size(); i++) {
			String temp = array.get(i).toString();
			JSONObject object = JSONObject.parseObject(temp);
			String date = object.get("date").toString();
			String photo = object.get("photo").toString();
			String hash = object.get("hash") == null ? "" : object.get("hash").toString();
			Double storage = 0.00;
			if (device.equals("iOS")) {
				String[] singleStorage = object.get("storage").toString().split(",");
				for (int j = 0; j < singleStorage.length; j++) {
					storage += Double.parseDouble(singleStorage[j]);
				}
			} else {
				storage = Double.parseDouble(object.get("storage").toString());
			}

			flag = TxService.backupSingleDayPhoto(userid, date, photo, storage, hash);
			if (!flag) {
				jsonString = jsonData.getJson(1028, "上传失败");
				break;
			}
			totalStorage += storage;
		}
		// 更新用户已用存储空间
		if (flag) {
			User user = new User().findById(userid);
			Double tempStorage = user.getDouble("uusespace");
			user.set("uusespace", totalStorage + tempStorage);
			jsonString = dataProcess.updateFlagResult(user.update());
		}

		renderText(jsonString);
	}

	/**
	 * 查看动态,记录查看历史
	 */
	public void WatchEvent() {
		String userid = this.getPara("userid");
		String eventid = this.getPara("eventid");

		String temp = Db.findFirst("select euserid from events where eid=" + eventid + " ").get("euserid").toString();
		if (temp.equals(userid)) {
			jsonString = jsonData.getSuccessJson();
		} else {
			List<Record> list = Db.find("select * from view where vuserid=" + userid + " and veid=" + eventid + " ");
			if (list.size() == 0) {
				View view = new View();
				view.set("vuserid", userid).set("veid", eventid);
				jsonString = dataProcess.insertFlagResult(view.save());
			} else {
				jsonString = jsonData.getSuccessJson();
			}
		}

		renderText(jsonString);
	}

	/**
	 * 显示空间内时光印记
	 */
	public void ShowTimeMarks() {
		String groupid = this.getPara("groupid");
		Group group = new Group().findById(groupid);
		int gstatus = Integer.parseInt(group.get("gstatus").toString());
		if (gstatus == 0) {
			List<Record> list = Db.find(
					"select userid,upic,unickname,markID,markUserID,markType,markContent,markDate,markColor,markTop,markPic,markNotify,markRepeat from mark,users where userid=markUserID and markGroupID="
							+ groupid + " and markStatus=0 order by markID desc ");
			jsonString = jsonData.getSuccessJson(list);
		} else {
			jsonString = jsonData.getJson(1012, "相册已被删除");
		}
		renderText(jsonString);
	}

	/**
	 * 添加时光印记
	 */
	public void AddTimeMark() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String type = this.getPara("type");
		String content = this.getPara("content");
		String date = this.getPara("date");
		String color = this.getPara("color");
		String top = this.getPara("top");
		String picture = this.getPara("picture");
		String notify = this.getPara("notify");
		String repeat = this.getPara("repeat");
		if (top == null || top.equals("")) {
			top = "0";
		}
		Mark mark = new Mark().set("markUserID", userid).set("markGroupID", groupid).set("markType", type)
				.set("markContent", content).set("markDate", date).set("markColor", color).set("markTop", top)
				.set("markPic", picture).set("markNotify", notify).set("markRepeat", repeat);
		jsonString = dataProcess.insertFlagResult(mark.save());
		renderText(jsonString);
	}

	/**
	 * 修改时光印记
	 */
	public void ModifyTimeMark() {
		String markID = this.getPara("markID");
		String type = this.getPara("type");
		String content = this.getPara("content");
		String date = this.getPara("date");
		String color = this.getPara("color");
		String top = this.getPara("top");
		String picture = this.getPara("picture");
		String notify = this.getPara("notify");
		String repeat = this.getPara("repeat");
		if (top == null || top.equals("")) {
			top = "0";
		}
		Mark mark = new Mark().findById(markID);
		mark.set("markType", type).set("markContent", content).set("markDate", date).set("markColor", color)
				.set("markTop", top).set("markPic", picture).set("markNotify", notify).set("markRepeat", repeat);
		jsonString = dataProcess.insertFlagResult(mark.update());
		renderText(jsonString);
	}

	/**
	 * 删除时光印记
	 */
	public void DeleteTimeMark() {
		String markID = this.getPara("markID");
		String userid = this.getPara("userid");
		Mark mark = new Mark().findById(markID).set("markStatus", 1);
		jsonString = dataProcess.updateFlagResult(mark.update());
		renderText(jsonString);
	}

	/**
	 * 显示时光印记类型
	 */
	public void ShowTimeMarkTypes() {
		List<Record> list = Db.find(
				"select distinct officialCoverTheme from officialcover where coverType=4 and officialCoverStatus=0 ");
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 显示时光印记封面
	 */
	public void ShowTimeMarkBackgroundPic() {
		// String type = this.getPara("type");
		List<Record> list = Db.find(
				"select officialCoverID,officialCoverPicture,isRecommend from officialcover where coverType=4 and officialCoverStatus=0 ");
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 显示个人时光印记
	 */
	public void ShowUserTimeMark() {
		String userid = this.getPara("userid");
		List<Record> list = Db.find(
				"select markID,markContent,markDate,markColor,markRepeat,groupid,gimid,gname,gpic,gnum,gtime,gtype,gcreator,ginvite from mark,groups where groupid=markGroupID and markUserID="
						+ userid + " and markStatus=0 ");
		// 获取组照片数
		List<Record> photoList = Db.find(
				"select groupid,count(*) as gpicnum from groups,groupmembers,events,pictures where groupid=gmgroupid and peid=eid and gmgroupid=egroupid and gmuserid="
						+ userid
						+ " and gstatus=0 and gmstatus=0 and estatus in(0,3) and pstatus=0 group by gmgroupid");
		// 将组照片数插入到组信息中
		for (Record groupRecord : list) {

			// 返回结果中增加加密后的groupid
			String groupid = groupRecord.get("groupid").toString();
			// 插入图片数量数据
			boolean flag = false;
			for (Record photoRecord : photoList) {
				if (groupid.equals((photoRecord.get("groupid").toString()))) {
					groupRecord.set("gpicnum", photoRecord.get("gpicnum"));
					flag = true;
					break;
				}
			}
			if (!flag) {
				groupRecord.set("gpicnum", 0);
			}
		}
		// 获取相册动态数
		List<Record> eventList = Db
				.find("select gmgroupid as groupid,count(*) as eventnum from groupmembers,events where gmuserid="
						+ userid + " and gmgroupid=egroupid and gmstatus=0 and estatus=0 GROUP BY gmgroupid");
		// 将组动态数插入到组信息中
		for (Record groupRecord : list) {
			boolean flag = false;
			for (Record eventRecord : eventList) {
				if ((groupRecord.get("groupid").toString()).equals((eventRecord.get("groupid").toString()))) {
					groupRecord.set("geventnum", eventRecord.get("eventnum"));
					flag = true;
					break;
				}
			}
			if (!flag) {
				groupRecord.set("eventnum", 0);
			}
		}
		// 将组类型转换成相应的文字
		list = dataProcess.changeGroupTypeIntoWord(list);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 备注用户
	 */
	public void NoteUserInSpace() {
		String groupid = this.getPara("groupid");
		String from = this.getPara("from");
		String to = this.getPara("to");
		String name = this.getPara("name");
		List<Record> list = Db.find("select noteID from note where noteGroupID=" + groupid + " and noteFrom=" + from
				+ " and noteTo=" + to + " ");
		Note note = new Note();
		if (list.size() == 0) {
			note.set("noteGroupID", groupid).set("noteFrom", from).set("noteTo", to).set("noteName", name);
			jsonString = dataProcess.insertFlagResult(note.save());
		} else {
			String noteID = list.get(0).get("noteID").toString();
			Note temp = new Note().findById(noteID);
			temp.set("noteName", name);
			jsonString = dataProcess.updateFlagResult(temp.update());
		}
		renderText(jsonString);
	}

	/**
	 * 获取最新版本
	 */
	public void GetNewestVersion() {
		List<Record> list = Db.find("select versionNo,versionUrl from version order by versionTime desc limit 1 ");
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 修改空间性质，用于将引导空间的性质修改成空间
	 */
	public void ModifySpaceType() {
		String groupid = this.getPara("groupid");
		Group group = new Group().findById(groupid);
		int gtype = group.getInt("gtype");
		if (gtype == 6 || gtype == 7 || gtype == 8) {
			group.set("gtype", 4);
			jsonString = dataProcess.updateFlagResult(group.update());
		} else {
			jsonString = jsonData.getJson(2026, "相册性质不能修改");
		}
		renderText(jsonString);
	}

	/**
	 * 修改状态
	 */
	public void ModifyEvent() {
		String userid = this.getPara("userid");
		String eid = this.getPara("eid");
		String type = this.getPara("type");
		String picture = this.getPara("picture");
		String content = this.getPara("content");
		String place = this.getPara("place");
		String memorytime = this.getPara("memorytime");
		String audio = this.getPara("audio");
		String cover = this.getPara("cover");
		String tag = this.getPara("tag");
		String storage = this.getPara("storage");

		Event event = new Event().findById(eid);

		if (userid.equals(event.get("euserid").toString())) {
			boolean flag = false;
			switch (type) {
			case "event":
				flag = service.ModifyEvent(event, eid, picture, content, place, storage);
				break;
			case "postCard":
				flag = service.ModifyPostCard(event, eid, picture, place, cover, memorytime, audio, storage);
				break;
			case "recordCard":
				flag = service.ModifyRecordCard(userid, event, eid, picture, content, place, memorytime, audio, tag,
						storage);
				break;
			}
			jsonString = dataProcess.updateFlagResult(flag);
		} else {
			jsonString = jsonData.getJson(6, "没有权限执行操作");
		}

		renderText(jsonString);
	}

	/**
	 * 迁移动态
	 */
	public void TransferEvent() {
		String userid = this.getPara("userid");
		String eid = this.getPara("eid");
		String groupid = this.getPara("groupid");

		String[] IDs = groupid.split(",");
		List<Record> resultList = new ArrayList<Record>();
		for (int i = 0; i < IDs.length; i++) {
			List<Record> tempList = service.transferEvent(userid, eid, IDs[i]);
			resultList.add(tempList.get(0));
		}
		jsonString = jsonData.getSuccessJson(resultList);
		renderText(jsonString);
	}

	/**
	 * 迁移图片
	 */
	public void TransferPhoto() {
		// 获取参数
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String picAddress = this.getPara("picAddress");

		List<Record> result = new ArrayList<Record>();

		String[] IDs = groupid.split(",");
		// 获取上传所在组的类型，若是官方相册（5），则不上传
		List<Record> list = dao.query("gtype", "groups", "groupid=" + IDs[0] + " ");
		String type = list.get(0).get("gtype").toString();
		if (type.equals("5")) {
			jsonString = jsonData.getJson(1015, "没有权限发布动态");
		} else {
			// 返回eid和euploadtime
			for (int i = 0; i < IDs.length; i++) {
				String eventID = TxService.upload(userid, IDs[i], picAddress, null, "0", null, "private", null, "app");// 上传动态并获取动态的ID
				if (!eventID.equals("")) {
					List<Record> tempList = service.getSingleEventWithList(eventID);// 获取动态的信息
					result.add(tempList.get(0));
				} else {
					jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
					break;
				}
			}
			jsonString = jsonData.getSuccessJson(result);
		}
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 共享相册点赞与取消点赞
	 * 
	 */
	public void AttachOrRemoveExpression() {
		String userid = this.getPara("userid");
		String eid = this.getPara("eid");
		String type = this.getPara("type");
		String source = this.getPara("source");
		String formID = this.getPara("formID"); // 小程序推送表单ID
		// 插入formID
		FormID.insertFormID(userid, formID);
		int status = 0;
		if (source != null && source.equals("app")) {
			// app直接传值
			status = Integer.parseInt(type);
		} else {
			// 小程序传英文,将type改成对应status
			switch (type) {
			case "like":
				status = 0;
				break;
			case "happy":
				status = 2;
				break;
			case "sad":
				status = 3;
				break;
			case "mad":
				status = 4;
				break;
			case "surprise":
				status = 5;
				break;
			case "unlike":
				status = 1;
				break;
			}
		}

		// 判断用户是否有相关操作
		List<Record> judge = Db
				.find("select * from `like` where likeEventID=" + eid + " and likeUserID=" + userid + " ");

		List<Record> result = new ArrayList<Record>();
		String sql = "select likeID,likeUserID,likeStatus,upic from `like`,users where userid=likeUserID and likeEventID="
				+ eid + " and likeStatus!=1";

		Like like;
		if (judge.size() == 0) {
			// 没有操作过
			//by lk test
//			Event e = new Event().findById(eid);
//			String groupid = e.get("egroupid").toString();
//			String euserid = e.get("euserid").toString(); 
			like = new Like().set("likeEventID", eid).set("likeUserID", userid)
					.set("likeStatus", status);
					//.set("likeGroupID", groupid).set("likeEventUserId", euserid);
			if (like.save()) {
				result = Db.find(sql);
				result = dataProcess.changeLikeStatusToWord(result);
				jsonString = jsonData.getSuccessJson(result);
				//点赞推送 begin
				if(CommonParam.canPublish&&status==0){
					Event event = new Event().findById(eid);
					String egroupid = event.get("egroupid").toString();
					//判断是否是普通相册 在普通相册点赞才推送
					String gOrigin =String.valueOf(new Group().findById(egroupid).getLong("gOrigin"));
					if(gOrigin.equals("0")) {
						User user = new User().findById(userid);
						//点赞人的姓名
						String username = user.get("unickname").toString();
						//发布人的uopenid
						String uid = event.get("euserid").toString();
						String uopenid = new User().findById(uid).get("uopenid");
						if(null!=uopenid&&!uopenid.equals("")&&!uid.equals(userid)){
							//提取发布人的formid
							List<Record> formidList = Db.find("select formID from formid where 1 "
									+ "and status=0 and userID="+uid+ " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
									+ "order by time asc limit 1");
							//点赞成功发送推送
							if (null!=formidList&&!formidList.isEmpty()) {
								String formid = formidList.get(0).get("formID");
								System.out.println("formid=="+formid);
								//点赞成功发送推送
								SmallAppPush smallAppPush = new SmallAppPush();
								smallAppPush.likeIsPush(formid, uopenid, eid, username);
								System.out.println("delete from formid where userID="+uid+" and formID='"+formid+"'");
								Db.update("delete from formid where userID="+uid+" and formID='"+formid+"'");						
							}
						}
					}
				}
				//点赞推送 end
				//雷雨点赞推送开关
//				Boolean likePushFlag = false;
//				Event event = new Event().findById(eid);
//				String egroupid = event.get("egroupid").toString();
//				//判断是否是普通相册 在普通相册点赞才推送
//				String gOrigin =String.valueOf(new Group().findById(egroupid).getLong("gOrigin"));
//				if(gOrigin.equals("1")) {
//					likePushFlag = false;
//				}
				//？？？？？
//				if(likePushFlag) {
//					User user = new User().findById(userid);
//					//点赞人的姓名
//					String username = user.get("unickname").toString();
//					//发布人的uopenid
//					String uid = event.get("euserid").toString();
//					String uopenid = new User().findById(uid).get("uopenid");
//					//提取发布人的formid
//					List<Record> formidList = Db.find("select formID from formid where 1 "
//							+ "and status=0 and userID="+uid+ " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
//							+ "order by time asc limit 1");
//					//点赞成功发送推送
//					if (null!=formidList&&!formidList.isEmpty()) {
//						String formid = formidList.get(0).get("formID");
//						System.out.println("formid=="+formid);
//						//点赞成功发送推送
//						SmallAppPush smallAppPush = new SmallAppPush();
//						smallAppPush.likeIsPush(formid, uopenid, eid, username);
//						System.out.println("delete from formid where userID="+uid+" and formID='"+formid+"'");
//						Db.update("delete from formid where userID="+uid+" and formID='"+formid+"'");						
//					}
//					
//				}
				//雷雨推送？？？？？？
			} else {
				jsonString = dataProcess.insertFlagResult(false);
			}

		} else {
			// 有操作过
			int likeID = Integer.parseInt(judge.get(0).get("likeID").toString());
			like = new Like().findById(likeID);
			like.set("likeStatus", status);
			if (like.update()) {
				result = Db.find(sql);
				result = dataProcess.changeLikeStatusToWord(result);
				jsonString = jsonData.getSuccessJson(result);
			} else {
				jsonString = dataProcess.updateFlagResult(false);
			}

		}

		renderText(jsonString);

	}

	/**
	 * 获取用户最近上传的40张照片
	 */
	public void GetUserRecentlyUploadPicture() {
		String userid = this.getPara("userid");
		User user = new User();
		List<Record> photo = user.GetUserRecentlyUploadPicture(userid);
		// 图片授权
		photo = dataProcess.GetOriginAndThumbnailAccess(photo, "poriginal");
		jsonString = jsonData.getSuccessJson(photo);
		renderText(jsonString);
	}

	/**
	 * 获取用户最近上传的4个地址
	 */
	public void GetUserRecentlyUploadAddress() {
		String userid = this.getPara("userid");
		User user = new User();
		List<Record> address = user.GetUserRecentlyAddress(userid);
		jsonString = jsonData.getSuccessJson(address);
		renderText(jsonString);
	}

	/**
	 * 生成小程序二维码
	 */
	public void CreateSmallAppQRCode() {
		String type = this.getPara("type");
		String id = this.getPara("id");

		SmallAppQRCode small = new SmallAppQRCode();

		String url = "";
		switch (type) {
		case "space":
			Group group = new Group().findById(id);
			String gQRCode = group.get("gQRCode");
			if (gQRCode == null) {
				url = small.GetSmallAppQRCodeURL(type, id);
				group.set("gQRCode", url);
				group.update();
			} else {
				url = gQRCode;
			}
			break;
		case "spaceEvent":			
				url = small.GetSmallAppQRCodeURL(type, id);			
			break;
		case "puzzle":
			Puzzle puzzle = new Puzzle().findById(id);
			String puzzleQRCode = puzzle.get("puzzleQRCode");
			if (puzzleQRCode == null) {
				url = small.GetSmallAppQRCodeURL(type, id);
				puzzle.set("puzzleQRCode", url);
				puzzle.update();
			} else {
				url = puzzleQRCode;
			}
			break;
		case "temp":
			Temp temp = new Temp().findById(id);
			String tempURL = temp.get("tempURL");
			if (tempURL == null) {
				url = small.GetSmallAppQRCodeURL(type, id);
				temp.set("tempURL", url);
				temp.update();
			} else {
				url = tempURL;
			}
			break;
		}
		jsonString = jsonData.getSuccessJson(dataProcess.makeSingleParamToList("QRCodeURL", url));
		renderText(jsonString);
	}

	/**
	 * 获取用户忆年好友
	 */
	public void GetUserYinianFriend() {
		String userid = this.getPara("userid");
		List<Record> list = Db.find(
				"SELECT DISTINCT userid,unickname,upic FROM users,groups,groupmembers WHERE groupid = gmgroupid AND userid = gmuserid AND groupid IN (SELECT gmgroupid FROM groupmembers WHERE gmuserid = "
						+ userid + " AND gmstatus = 0) and gtype in (0,1,2,3,4,6,7,8) and gnum<100 and gmuserid !="
						+ userid + " AND upic!='' and gstatus = 0 AND gmstatus = 0  ");
		int size = list.size();
		String[] array = new String[size];
		for (int i = 0; i < list.size(); i++) {
			String temp = (list.get(i).get("unickname").toString() + ";" + list.get(i).get("userid").toString() + ";"
					+ list.get(i).get("upic").toString());
			array[i] = temp;
		}
		Comparator<Object> com = Collator.getInstance(Locale.CHINA);
		Arrays.sort(array, com);

		List<Record> result = new ArrayList<Record>();
		Record record;
		for (int i = 0; i < array.length; i++) {
			String[] tempArray = array[i].split(";");
			int tempSize = tempArray.length;
			record = new Record().set("upic", tempArray[tempSize - 1]).set("userid", tempArray[tempSize - 2])
					.set("unickname", tempArray[tempSize - 3]);
			result.add(record);
		}
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
	}

	/**
	 * 获取用户上传的照片墙
	 */
	public void GetUserPhotoWall() {
		String userid = this.getPara("userid");
		String type = this.getPara("type");
		String date = this.getPara("date");

		User user = new User();
		List<Record> result = new ArrayList<Record>();
		switch (type) {
		case "initialize":
			result = user.initializeUserPhotoWall(userid);
			break;
		case "loading":
			result = user.loadingUserPhotoWall(userid, date);
			break;
		default:
			break;
		}

		// 按日期获取相应数据
		for (int i = 0; i < result.size(); i++) {
			String eids = result.get(i).get("eid").toString();
			List<Record> picList = Db.find(
					"select eid,euserid,pid,poriginal as url from events,pictures where peid=eid and eMain not in (4,5) and eid in("
							+ eids + ") and pstatus=0 ORDER BY pid DESC ");
			if (picList.size() != 0) {
				// 图片授权
				picList = dataProcess.GetOriginAndThumbnailAccessWithDirectCut(picList, "url");

				// 插入数据
				result.get(i).remove("eid");
				result.get(i).set("picture", picList);
			} else {

				result.remove(result.get(i));
				i--;
			}

		}

		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
	}

	/**
	 * 获取用户上传的视频墙
	 */
	public void GetUserVideoWall() {
		String userid = this.getPara("userid");
		String type = this.getPara("type");
		String pid = this.getPara("pid");

		User user = new User();
		List<Record> result = new ArrayList<Record>();
		switch (type) {
		case "initialize":
			result = user.initializeUserVideoWall(userid);
			break;
		case "loading":
			result = user.loadingUserVideoWall(userid, pid);
			break;
		default:
			break;
		}
		// 资源授权
		result = dataProcess.AuthorizeSingleResource(result, "poriginal");
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
	}
	/**
	 * 共享相册点赞与取消点赞 lk 修改返回对象
	 * 
	 */
	public void AttachOrRemoveExpressionByLkNew() {
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String eid = this.getPara("eid");
		String type = this.getPara("type");
		String source = this.getPara("source");
		String formID = this.getPara("formID")==null?"":this.getPara("formID"); // 小程序推送表单ID
		// 插入formID
		//FormID.insertFormID(userid, formID);
		if(!userid.equals("")&&!formID.equals("")){
			FormID.insert(userid, formID);
		}
		int status = 0;
		if (source != null && source.equals("app")) {
			// app直接传值
			status = Integer.parseInt(type);
		} else {
			// 小程序传英文,将type改成对应status
			switch (type) {
			case "like":
				status = 0;
				break;
			case "happy":
				status = 2;
				break;
			case "sad":
				status = 3;
				break;
			case "mad":
				status = 4;
				break;
			case "surprise":
				status = 5;
				break;
			case "unlike":
				status = 1;
				break;
			}
		}
		// 判断用户是否有相关操作
				List<Record> judge = Db
						.find("select * from `like` where likeEventID=" + eid + " and likeUserID=" + userid + " ");
				
				List<Record> result = new ArrayList<Record>();
//				String sql = "select likeID,likeUserID,likeStatus,upic from `like`,users where userid=likeUserID and likeEventID="
//						+ eid + " and likeStatus!=1";
				String sql = "select unickname,likeID,likeUserID,likeStatus,upic from `like`,users where userid=likeUserID and likeEventID="
						+ eid + " and likeStatus!=1 order by likeID desc limit 0,10";
				String likeCntSql = "select count(*) cnt from `like` where likeEventID="
						+ eid + " and likeStatus!=1 ";
				String iLikeSql="select count(*) cnt from `like`,users where userid=likeUserID and likeEventID="
             +eid+ " and userid='"+userid+ "' and likeStatus!=1 ";
				Like like;
				if (judge.size() == 0) {
					// 没有操作过
					//by lk test
//					Event e = new Event().findById(eid);
//					String groupid = e.get("egroupid").toString();
//					String euserid = e.get("euserid").toString();
					like = new Like().set("likeEventID", eid).set("likeUserID", userid).set("likeStatus", status);
							//.set("likeGroupID", groupid).set("likeEventUserId", euserid);
					//lk test end
					if (like.save()) {
						result = Db.find(sql);
						result = dataProcess.changeLikeStatusToWord(result);
						List<Record> returnList=new ArrayList<Record>();
						Record r=new Record();
						r.set("likeCnt",0);
						//点赞数读取redis缓存，若没有缓存则读取数据库，同时更新缓存
						Jedis jedis = RedisUtils.getRedis();
					/*	if(null!=jedis) {
							//从缓存中读取当前eid的点赞数
							String likeCnt = jedis.get("likeCnt_"+eid);
							if(null!=likeCnt&&!"".equals(likeCnt)) {
								//点赞成功后缓存点赞数加1
								int likeCntInt = Integer.valueOf(likeCnt) + 1;
								String jr = jedis.set("likeCnt_"+eid, String.valueOf(likeCntInt));
								if(null!=jr&&"OK".equals(jr)) {
									r.set("likeCnt", likeCntInt);
								}
							}else {
								//当前eid未缓存点赞数，从数据库count点赞数，并同步到缓存
								List<Record> cntList=Db.find(likeCntSql);
								if(!cntList.isEmpty()){
									r.set("likeCnt",cntList.get(0).get("cnt"));
									jedis.set("likeCnt_"+eid, cntList.get(0).get("cnt").toString());
								}
							}
							//释放redis
							RedisUtils.returnResource(jedis);
						}else {*/
							List<Record> cntList=Db.find(likeCntSql);
							if(!cntList.isEmpty()){
								r.set("likeCnt",cntList.get(0).get("cnt"));
							}
						//}
						r.set("likeUser",0);
						List<Record> iLikeCntList=Db.find(iLikeSql);
						if(!iLikeCntList.isEmpty()){
							r.set("likeUser",iLikeCntList.get(0).get("cnt"));
						}
						r.set("like", result);
						
						returnList.add(r);
						jsonString = jsonData.getSuccessJson(returnList);
						//点赞推送 begin
						if(CommonParam.canPublish&&status==0){
							Event event = new Event().findById(eid);
							String egroupid = event.get("egroupid").toString();
							//判断是否是普通相册 在普通相册点赞才推送
							String gOrigin =String.valueOf(new Group().findById(egroupid).getLong("gOrigin"));
							if(gOrigin.equals("0")) {
								User user = new User().findById(userid);
								//点赞人的姓名
								String username = user.get("unickname").toString();
								//发布人的uopenid
								String uid = event.get("euserid").toString();
								String uopenid = new User().findById(uid).get("uopenid");
								if(null!=uopenid&&!uopenid.equals("")&&!uid.equals(userid)){
									//提取发布人的formid
									List<Record> formidList = Db.find("select formID from formid where 1 "
											+ "and status=0 and userID="+uid+ " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
											+ "order by time asc limit 1");
									//点赞成功发送推送
									if (null!=formidList&&!formidList.isEmpty()) {
										String formid = formidList.get(0).get("formID");
										System.out.println("formid=="+formid);
										//点赞成功发送推送
										SmallAppPush smallAppPush = new SmallAppPush();
										smallAppPush.likeIsPush(formid, uopenid, eid, username);
										System.out.println("delete from formid where userID="+uid+" and formID='"+formid+"'");
										Db.update("delete from formid where userID="+uid+" and formID='"+formid+"'");						
									}
								}
							}
						}
						//点赞推送 end
//						//评论推送 begin
//						if(CommonParam.canPublish&&status==0){
//							Event event = new Event().findById(eid);
//							String egroupid = event.get("egroupid").toString();
////							//判断是否是普通相册 在普通相册才推送
//							String gOrigin =String.valueOf(new Group().findById(egroupid).getLong("gOrigin"));
//							if(gOrigin.equals("0")) {
//								User user = new User().findById(userid);
//								//评论人的姓名
//								String username = user.get("unickname").toString();
//								//被评论人的uopenid
//								String uid = event.get("euserid").toString();
//								String uopenid = new User().findById(uid).get("uopenid");
//								if(null!=uopenid&&uopenid.equals("")){
//									//提取发布人的formid
//									List<Record> formidList = Db.find("select formID from formid where 1 "
//											+ "and status=0 and userID="+uid+ " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
//											+ "order by time asc limit 1");
//									
//									if(null!=formidList&&!formidList.isEmpty()) {
//										String formid = formidList.get(0).get("formID");
//										//评论成功发送推送
//										SmallAppPush smallAppPush = new SmallAppPush();
//										smallAppPush.commentIsPush(formid, uopenid, eid, username);
//										Db.update("delete from formid where userID="+uid+" and formID='"+formid+"'");
//									}
//								}
//							}
//						}
//						//评论推送 end
					} else {
						jsonString = dataProcess.insertFlagResult(false);
					}

				} else {
					// 有操作过
					int likeID = Integer.parseInt(judge.get(0).get("likeID").toString());
					like = new Like().findById(likeID);
					like.set("likeStatus", status);
					if (like.update()) {
						result = Db.find(sql);
						result = dataProcess.changeLikeStatusToWord(result);
						List<Record> returnList=new ArrayList<Record>();
						//Record r=new Record();
						Record r=new Record();
						r.set("likeCnt",0);
						//点赞数读取redis缓存，若没有缓存则读取数据库，同时更新缓存
						Jedis jedis = RedisUtils.getRedis();
						if(null!=jedis) {
							//从缓存中读取当前eid的点赞数
							String likeCnt = jedis.get("likeCnt_"+eid);
							if(null!=likeCnt&&!"".equals(likeCnt)) {
								if(status == 1) {
									//取消点赞时，缓存点赞数减1
									int likeCntInt = Integer.valueOf(likeCnt)>0?Integer.valueOf(likeCnt) - 1:0;
									String jr = jedis.set("likeCnt_"+eid, String.valueOf(likeCntInt));
									if(null!=jr&&"OK".equals(jr)) {
										r.set("likeCnt", likeCntInt);
									}
								}else {
									//非取消点赞，缓存点赞数不变
									r.set("likeCnt", Integer.valueOf(likeCnt));
								}
							}else {
								//当前eid未缓存点赞数，从数据库count点赞数，并同步到缓存
								List<Record> cntList=Db.find(likeCntSql);
								if(!cntList.isEmpty()){
									r.set("likeCnt",cntList.get(0).get("cnt"));
									jedis.set("likeCnt_"+eid, cntList.get(0).get("cnt").toString());
								}
							}
							//释放redis
							RedisUtils.returnResource(jedis);
						}else {
							List<Record> cntList=Db.find(likeCntSql);
							if(!cntList.isEmpty()){
								r.set("likeCnt",cntList.get(0).get("cnt"));
							}
						}
						r.set("like", result);
						r.set("likeUser",0);
						List<Record> iLikeCntList=Db.find(iLikeSql);
						if(!iLikeCntList.isEmpty()){
							r.set("likeUser",iLikeCntList.get(0).get("cnt"));
						}
						returnList.add(r);
						jsonString = jsonData.getSuccessJson(returnList);
					} else {
						jsonString = dataProcess.updateFlagResult(false);
					}

				}

				renderText(jsonString);
		// 判断用户是否有相关操作
		

		

	}
	public void AttachOrRemoveExpressionByLk() {
		String userid = this.getPara("userid");
		String eid = this.getPara("eid");
		String type = this.getPara("type");
		String source = this.getPara("source");
		String formID = this.getPara("formID"); // 小程序推送表单ID
		// 插入formID
		FormID.insertFormID(userid, formID);
		int status = 0;
		if (source != null && source.equals("app")) {
			// app直接传值
			status = Integer.parseInt(type);
		} else {
			// 小程序传英文,将type改成对应status
			switch (type) {
			case "like":
				status = 0;
				break;
			case "happy":
				status = 2;
				break;
			case "sad":
				status = 3;
				break;
			case "mad":
				status = 4;
				break;
			case "surprise":
				status = 5;
				break;
			case "unlike":
				status = 1;
				break;
			}
		}
		// 判断用户是否有相关操作
				List<Record> judge = Db
						.find("select * from `like` where likeEventID=" + eid + " and likeUserID=" + userid + " ");
				
				List<Record> result = new ArrayList<Record>();
//				String sql = "select likeID,likeUserID,likeStatus,upic from `like`,users where userid=likeUserID and likeEventID="
//						+ eid + " and likeStatus!=1";
				String sql = "select likeID,likeUserID,likeStatus,upic from `like`,users where userid=likeUserID and likeEventID="
						+ eid + " and likeStatus!=1  limit 0,10";
			
				Like like;
				if (judge.size() == 0) {
					// 没有操作过
					like = new Like().set("likeEventID", eid).set("likeUserID", userid).set("likeStatus", status);
					if (like.save()) {
						result = Db.find(sql);
						result = dataProcess.changeLikeStatusToWord(result);
						List<Record> returnList=new ArrayList<Record>();
						Record r=new Record();
						r.set("like", result);
						r.set("likeUser", Db
								.find("select likeUserID from `like` where likeEventID=" + eid + " and likeStatus!=1 "));
						returnList.add(r);
						jsonString = jsonData.getSuccessJson(returnList);
						//jsonString = jsonData.getSuccessJson(result);
					} else {
						jsonString = dataProcess.insertFlagResult(false);
					}

				} else {
					// 有操作过
					int likeID = Integer.parseInt(judge.get(0).get("likeID").toString());
					like = new Like().findById(likeID);
					like.set("likeStatus", status);
					if (like.update()) {
						result = Db.find(sql);
						result = dataProcess.changeLikeStatusToWord(result);
						List<Record> returnList=new ArrayList<Record>();
						Record r=new Record();
						r.set("like", result);
						r.set("likeUser", Db
								.find("select likeUserID from `like` where likeEventID=" + eid + " and likeStatus!=1 "));
						returnList.add(r);
						jsonString = jsonData.getSuccessJson(returnList);
					} else {
						jsonString = dataProcess.updateFlagResult(false);
					}

				}

				renderText(jsonString);
				// 判断用户是否有相关操作
	}
	/**
	 * 显示小程序相册信息 by lk 
	 */
	@Before(CrossDomain.class)
	public void ShowSmallAppAlbumInformationNOUid() {
		//String uid=this.getPara("userid");
		String gid=this.getPara("groupid");
		if(gid==null||gid.equals("undefined")||gid.equals("NaN")){
			jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		// 基本参数		
		int groupid = this.getParaToInt("groupid");
		// 用户来源参数
		String port = this.getPara("port");
		String fromUserID = this.getPara("fromUserID");

		Group group = new Group().findById(groupid);
		int eventQRCodeCanPublish=new GroupCanPublish().getGroupPublishByType(groupid+"", "1");
		int status = Integer.parseInt(group.get("gstatus").toString());
		int gtype = Integer.parseInt(group.get("gtype").toString());
		int gAuthority = Integer.parseInt(group.get("gAuthority").toString());
		// 判断相册是否删除
		if (status == 0) {
			// 判断是否在相册中
			GroupMember gm = new GroupMember();
			
			boolean flag = true;
			int count = 1;
			Record record = new Record().set("joinStatus", 1);
			

			if (flag && (count == 1)) {
				// 获取返回数据 ,gAuthority————0所有人 1只有创建者 2-部分
				record.set("gname", group.get("gname").toString()).set("gcreator", group.get("gcreator").toString())
						.set("gtype", gtype).set("gnum", group.get("gnum").toString())
						.set("gpic", group.get("gpic").toString()).set("gAuthority", gAuthority).set("gOrigin",  group.get("gOrigin").toString())
						.set("eventQRCodeCanPublish",  eventQRCodeCanPublish);

				
				record.set("isPush", "0");
				// 获取成员列表，缓存
				List<Record> groupMember = CacheKit.get("ConcurrencyCache", groupid + "Member");
				if (groupMember == null) {
					groupMember = Db.find(
							"select userid,unickname,upic,gmtime from users,groups,groupmembers where groupid=gmgroupid and users.userid=groupmembers.gmuserid and gmgroupid='"
									+ groupid + "' and gmstatus=0 limit 10 ");
					CacheKit.put("ConcurrencyCache", groupid + "Member", groupMember);
				}

				// 获取照片数，缓存
				List<Record> photo = CacheKit.get("ConcurrencyCache", groupid + "Photo");
				if (photo == null) {
					photo = Db.find(
							"select count(*) as gpicNum from groups,events,pictures where peid=eid and groupid=egroupid and groupid="
									+ groupid + " and estatus in(0,3) and pstatus=0 ");
					CacheKit.put("ConcurrencyCache", groupid + "Photo", photo);
				}

				// 获取发布权限列表，缓存，当空间发布权限为部分人时才查询并返回
				if (gAuthority == 2) {
					List<Record> uploadAuthority = CacheKit.get("ConcurrencyCache", groupid + "Authority");
					if (uploadAuthority == null) {
						uploadAuthority = Db.find("select gmuserid as userid from groupmembers where gmgroupid="
								+ groupid + " and gmauthority=1 ");
						CacheKit.put("ConcurrencyCache", groupid + "Authority", uploadAuthority);
					}
					record.set("authorityList", uploadAuthority);
				}
				record.set("picNum", photo.get(0).get("gpicNum").toString()).set("memberList", groupMember);

				List<Record> result = new ArrayList<Record>();
				result.add(record);
				jsonString = jsonData.getSuccessJson(result);

			} else {
				jsonString = dataProcess.insertFlagResult(false);
			}

		} else if (status == 1) {
			jsonString = jsonData.getJson(1012, "相册已被删除");
		} else {
			jsonString = jsonData.getJson(1037, "相册已被封");
		}
		renderText(jsonString);
	}
	/**
	 * 显示相册 by lk 用户加入的相册或创建的相册
	 */
	@Before(CrossDomain.class)
	public void ShowGroupByCreateOrJoin() {
		// 获取参数
		int userid = Integer.parseInt(this.getPara("userid"));
		String mode = null==this.getPara("mode")?"create":this.getPara("mode");
		jsonString = service.showGroupByCreateOrJoin(userid,mode);
		// 返回结果
		renderText(jsonString);
	}
	/**
	 * 生成小程序二维码(圆形) by lk 
	 */
	public void CreateSmallAppRoundQRCode() {
		String type = this.getPara("type");
		String id = this.getPara("id");

		SmallAppQRCode small = new SmallAppQRCode();

		String url = "";
		switch (type) {
		
		case "spaceEvent":	
			Event event = new Event().findById(id);
			String eQRCode = event.get("eQRCode");
			if (eQRCode == null||eQRCode .equals("")) {
				url = small.GetSmallAppRoundQRCodeURL(type, id);
				event.set("eQRCode", url);
				event.update();
			} else {
				url = eQRCode;
			}					
			break;		
		}
		jsonString = jsonData.getSuccessJson(dataProcess.makeSingleParamToList("QRCodeURL", url));
		renderText(jsonString);
	}
	/**
	 * 生成小程序二维码(微信方案2) by lk 
	 */
	public void CreateSmallAppQRCodePlan2() {
		String type = this.getPara("type");
		String id = this.getPara("id");

		SmallAppQRCode small = new SmallAppQRCode();

		String url = "";
		switch (type) {
		
		case "spaceEvent":	
			Event event = new Event().findById(id);
			//String eQRCode = event.get("eQRCode");
			//if (eQRCode == null||eQRCode .equals("")) {
				url = small.GetSmallAppQRCodeURLPlan2(type, id);
//				event.set("eQRCode", url);
//				event.update();
//			} else {
//				url = eQRCode;
//			}					
			break;		
		}
		jsonString = jsonData.getSuccessJson(dataProcess.makeSingleParamToList("QRCodeURL", url));
		renderText(jsonString);
	}
	//by lk  设置相册置顶
	public void SetGroupIsTop(){
		String userid= this.getPara("userid");
		String groupid= this.getPara("groupid");
		String isTop=this.getPara("isTop")==null?"yes":this.getPara("isTop");
		if(null!=userid&&null!=groupid){
			jsonString = service.SetGroupIsTop(userid, groupid, isTop);
			
		}else{
			jsonString =jsonData.getJson(2, "参数错误");
		}
		renderText(jsonString);
	}
	
	/**
	 * 设置相册置顶 取消置顶(记录在groupmembers中) ly
	 */
	public void SetGroupIsTopNew(){
		String userid= this.getPara("userid");
		String groupid= this.getPara("groupid");
		String isTop=this.getPara("isTop")==null?"yes":this.getPara("isTop");
		if(null!=userid&&null!=groupid){
			jsonString = service.SetGroupIsTopNew(userid, groupid, isTop);
			
		}else{
			jsonString =jsonData.getJson(2, "参数错误");
		}
		renderText(jsonString);
	}
	/**
	 * 生成小程序二维码 plan2 不限制生成数量 by lk 
	 */
	public void CreateSmallAppPlan2QRCode() {
		String type = this.getPara("type");
		String id = this.getPara("id");

		SmallAppQRCode small = new SmallAppQRCode();

		String url = "";
		switch (type) {
		
		case "eventdetail2":	
			Event event = new Event().findById(id);
			String eQRCode = event.get("eQRCode");
			if (eQRCode == null||eQRCode .equals("")) {
				
				/*url = small.GetSmallAppQRCodeURLPlan2(type, id);
				if(url!=null&&url.indexOf("QRCodeError.png")!=-1) {
					
				}else{
					event.set("eQRCode", url);
					event.update();
				}*/
				if(null!=event.get("egroupid")&&event.get("egroupid").toString().equals("5460577")){
					url = small.GetSmallAppQRCodeURLPlan2_5460577(type, id);//使用统一底图
					if(url!=null&&url.indexOf("QRCodeError.png")!=-1) {
						
					}else{
						event.set("eQRCode", url);
						event.update();
					}
				}else{
					url = small.GetSmallAppQRCodeURLPlan2(type, id);
					if(url!=null&&url.indexOf("QRCodeError.png")!=-1) {
						
					}else{
						event.set("eQRCode", url);
						event.update();
					}
				}	
			} else {
				url = eQRCode;
			}					
			break;		
		}
		jsonString = jsonData.getSuccessJson(dataProcess.makeSingleParamToList("QRCodeURL", url));
		renderText(jsonString);
	}
	/**
	 * 时间轴二维码 plan2 by lk 
	 */
	@Before(CrossDomain.class)
	public void CreateSpacePlan2QRCode() {
		String type = "spaceQR";
		String id = this.getPara("id");

		SmallAppQRCode small = new SmallAppQRCode();

		String url = "";
//		switch (type) {
//		
//		case "spaceQR":	
			Group group = new Group().findById(id);
			String gQRCode = group.get("gQRCode");
			if (gQRCode == null) {
				url = small.GetSmallAppQRCodeURLPlan2(type, id);				
				if(url!=null&&url.indexOf("QRCodeError.png")!=-1) {
					
				}else{
					group.set("gQRCode", url);
					group.update();
				}
			} else {
				url = gQRCode;
			}
		//	break;	
		//}
		jsonString = jsonData.getSuccessJson(dataProcess.makeSingleParamToList("QRCodeURL", url));
		renderText(jsonString);
	}
	
	//用户加入空间
	public void joinGroup(){
		jsonString =jsonData.getJson(2, "参数错误");
		String uid=this.getPara("userid")==null?"":this.getPara("userid");
		String gid=this.getPara("groupid")==null?"":this.getPara("groupid");
		String port=this.getPara("port")==null?"":this.getPara("port");
		if(!uid.equals("")&&!gid.equals("")){
			int userid=Integer.parseInt(uid);
			int groupid=Integer.parseInt(gid);
			GroupMember gm = new GroupMember();
			boolean isInFlag = gm.judgeUserIsInTheAlbum(userid, groupid); // true时用户不在空间内

			boolean flag = true;
			int count = 1;
		
			if (isInFlag) {
				// 不在相册，则插入用户数据
				gm = new GroupMember().set("gmgroupid", groupid).set("gmuserid", userid).set("gmPort", port);
				// 捕获插入异常，用户重复点击时会导致插入失败
				try {
					flag = gm.save();
					// 更新分组表中组成员数量字段
					count = Db.update("update groups set gnum = gnum+1 where groupid='" + groupid + "' ");
				} catch (ActiveRecordException e) {
					flag = true;
					count = 1;
				}finally{
					jsonString =jsonData.getSuccessJson();
				}
			}else{
				jsonString =jsonData.getSuccessJson();
			}
		}
		renderText(jsonString);
	}
	//用户加入空间 加密
		public void joinGroupWithEncryption(){
			jsonString =jsonData.getJson(2, "参数错误");
			String uid=this.getPara("userid")==null?"":this.getPara("userid");
			String gid=this.getPara("groupid")==null?"":this.getPara("groupid");
			String port=this.getPara("port")==null?"":this.getPara("port");
			if(!uid.equals("")&&!gid.equals("")){
				//开始解密
				boolean canDES=false;
				try{			
					uid=URLDecoder.decode(uid);
					uid=DES.decryptDES(uid, CommonParam.DESSecretKey);
					gid=URLDecoder.decode(gid);
					gid=DES.decryptDES(gid, CommonParam.DESSecretKey);
					System.out.println("uid="+uid+"    gid="+gid);
					canDES=true;
				}catch(Exception e){
					e.printStackTrace();
				}finally{
					if(!canDES){
						jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
						renderText(jsonString);
						return;
					}
					//System.out.println(a1);
				}
				//解密end
				int userid=Integer.parseInt(uid);
				int groupid=Integer.parseInt(gid);
				GroupMember gm = new GroupMember();
				boolean isInFlag = gm.judgeUserIsInTheAlbum(userid, groupid); // true时用户不在空间内

				boolean flag = true;
				int count = 1;
			
				if (isInFlag) {
					// 不在相册，则插入用户数据
					gm = new GroupMember().set("gmgroupid", groupid).set("gmuserid", userid).set("gmPort", port);
					// 捕获插入异常，用户重复点击时会导致插入失败
					try {
						flag = gm.save();
						// 更新分组表中组成员数量字段
						count = Db.update("update groups set gnum = gnum+1 where groupid='" + groupid + "' ");
					} catch (ActiveRecordException e) {
						flag = true;
						count = 1;
					}finally{
						jsonString =jsonData.getSuccessJson();
					}
				}else{
					jsonString =jsonData.getSuccessJson();
				}
			}
			renderText(jsonString);
		}
	/**
	  * 统计虚拟人数接口
	  */
	public void getCountPeople() {
		  List<Record> peopleList = CacheKit.get("ConcurrencyCache", "peopleList");
		  if(null == peopleList) {
		   peopleList = Db.find("select remark as count from yntemp where id=46");
		   CacheKit.put("ConcurrencyCache", "peopleList",peopleList);
		  }
		  jsonString = jsonData.getJson(0,"success",peopleList);
		  renderText(jsonString);
		 }
	/**
	 * 显示小程序相册信息 pc端
	 */
	@Before(CrossDomain.class)
	public void ShowSmallAppAlbumInformationByPc() {
		String uid=this.getPara("userid");
		String gid=this.getPara("groupid");
		if(uid==null||gid==null||gid.equals("undefined")||gid.equals("NaN")){
			jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		//黑名单
		User u=new User().findById(uid);
		int inBlackList=1;
		if(null!=u&&null!=u.get("ustate")&&u.get("ustate").toString().equals("1")){
			inBlackList=0;
		}
		//黑名单
		// 基本参数
		int userid = this.getParaToInt("userid");
		int groupid = this.getParaToInt("groupid");
		// 用户来源参数
		String port = this.getPara("port");
		String fromUserID = this.getPara("fromUserID");
		
		Group group = new Group().findById(groupid);
		int eventQRCodeCanPublish=new GroupCanPublish().getGroupPublishByType(groupid+"", "1");
		//dialogShow=1 显示活动相册助手
		int dialogShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "2");
		//showAdvertisements=1 显示广告位
		int advertisementsShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "3");
		int status = Integer.parseInt(group.get("gstatus").toString());
		int gtype = Integer.parseInt(group.get("gtype").toString());
		int gAuthority = Integer.parseInt(group.get("gAuthority").toString());
		// 判断相册是否删除
		if (status == 0) {
//			// 判断是否在相册中
			GroupMember gm = new GroupMember();
			boolean isInFlag = gm.judgeUserIsInTheAlbum(userid, groupid); // true时用户不在空间内
			Record record = new Record().set("joinStatus", 1);
			// 获取返回数据 ,gAuthority————0所有人 1只有创建者 2-部分
			record.set("gname", group.get("gname").toString()).set("gcreator", group.get("gcreator").toString())
					.set("gtype", gtype).set("gnum", group.get("gnum").toString())
					.set("gpic", group.get("gpic").toString()).set("gAuthority", gAuthority)
					.set("gOrigin", group.get("gOrigin").toString())
					.set("eventQRCodeCanPublish", eventQRCodeCanPublish)
					.set("dialogShow", dialogShow).set("inBlackList", inBlackList).set("advertisementsShow", advertisementsShow);
			// 获取推送接收状态
			if (isInFlag) {
				// 不在空间内的用户直接返回0
				record.set("isPush", "0");
			} else {
				// 在空间内的用户去查询
				List<Record> push = Db.find("select gmIsPush from groupmembers where gmgroupid=" + groupid
						+ " and gmuserid=" + userid + "");
				record.set("isPush", push.get(0).get("gmIsPush").toString());
			}

			// 获取成员列表，缓存
			List<Record> groupMember = CacheKit.get("ConcurrencyCache", groupid + "Member");
			if (groupMember == null) {
				groupMember = Db.find(
						"select userid,unickname,upic,gmtime from users,groups,groupmembers where groupid=gmgroupid and users.userid=groupmembers.gmuserid and gmgroupid='"
								+ groupid + "' and gmstatus=0 order by gmid desc limit 10 ");
				CacheKit.put("ConcurrencyCache", groupid + "Member", groupMember);
			}

			// 获取照片数，缓存
			List<Record> photo = CacheKit.get("ConcurrencyCache", groupid + "Photo");
			if (photo == null) {
				photo = Db.find(
						"select count(*) as gpicNum from groups,events,pictures where peid=eid and groupid=egroupid and groupid="
								+ groupid + " and estatus in(0,3) and pstatus=0 ");
				CacheKit.put("ConcurrencyCache", groupid + "Photo", photo);
			}

			// 获取发布权限列表，缓存，当空间发布权限为部分人时才查询并返回
			if (gAuthority == 2) {
				List<Record> uploadAuthority = CacheKit.get("ConcurrencyCache", groupid + "Authority");
				if (uploadAuthority == null) {
					uploadAuthority = Db.find("select gmuserid as userid from groupmembers where gmgroupid="
							+ groupid + " and gmauthority=1 ");
					CacheKit.put("ConcurrencyCache", groupid + "Authority", uploadAuthority);
				}
				record.set("authorityList", uploadAuthority);
			}
			record.set("picNum", photo.get(0).get("gpicNum").toString()).set("memberList", groupMember);

			List<Record> result = new ArrayList<Record>();
			result.add(record);
			jsonString = jsonData.getSuccessJson(result);


		} else if (status == 1) {
			jsonString = jsonData.getJson(1012, "相册已被删除");
		} else {
			jsonString = jsonData.getJson(1037, "相册已被封");
		}
		renderText(jsonString);
	}

}
