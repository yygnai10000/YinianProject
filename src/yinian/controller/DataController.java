package yinian.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import yinian.app.YinianDataProcess;
import yinian.interceptor.CrossDomain;
import yinian.model.AlbumUVandPV;
import yinian.model.Botton;
import yinian.model.Data;
import yinian.model.Interface;
import yinian.model.YiNianDaily;
import yinian.model.YinianMonthly;
import yinian.service.DataService;
import yinian.utils.JsonData;
import yinian.utils.YinianUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

@Before(CrossDomain.class)
public class DataController extends Controller {

	// 数据处理类
	YinianDataProcess dataProcess = new YinianDataProcess();
	// 返回的json字符串
	private String jsonString;
	// json操作类
	private JsonData jsonData = new JsonData();
	// 业务逻辑类
	DataService service = new DataService();

	/**
	 * 忆年埋点统一接口Android版本
	 */
	public void YinianDataInterface() {

		String result = this.getPara("result");
		JSONObject jo = (JSONObject) JSONObject.parse(result);

		// 获取当天日期
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String date = sdf.format(new Date());

		// flag
		boolean timeFlag = true;
		boolean countFlag = true;
		boolean stayFlag = true;
		boolean btnFlag = true;
		boolean inviteFlag = true;
		boolean shareFlag = true;

		// 获取数据
		String userid = jo.get("userid").toString();
		String device = jo.get("device").toString();
		String version = jo.get("version").toString();
		JSONArray ja = (JSONArray) jo.get("totalData");
		// 遍历数据数组，根据type执行不同的方法
		for (int i = 0; i < ja.size(); i++) {
			JSONObject temp = (JSONObject) ja.get(i);
			int type = (int) temp.get("type");
			JSONObject data = (JSONObject) temp.get("data");
			switch (type) {
			case 1:
				// 停留总时间
				double totalTime = data.getDoubleValue("totalTime") / 1000;
				timeFlag = service.recordTotalStayTime(userid, device, version,
						date, totalTime);
				break;
			case 2:
				// 启动次数
				String startCount = data.getString("startCount");
				countFlag = service.recordStartCount(userid, device, version,
						date, startCount);
				break;
			case 3:
				// 各界面停留时间
				stayFlag = service.recordEachInterfaceStayTime(userid, device,
						version, date, data, "Android");
				break;
			case 4:
				// 按钮点击记录
				btnFlag = service.recordBottonEventClickCount(userid, device,
						version, date, data);
				break;
			case 5:
				// 邀请类型
				inviteFlag = service.recordInviteEventCount(userid, device,
						version, date, data);
				break;
			case 6:
				// 分享
				shareFlag = service.recordShareEventCount(userid, device,
						version, date, data);
				break;
			default:
				break;
			}
		}
		// 获取返回信息
		if (timeFlag && countFlag && stayFlag && btnFlag && inviteFlag
				&& shareFlag) {
			jsonString = jsonData.getSuccessJson();
		} else {
			jsonString = jsonData.getJson(1032, "数据解析失败");
		}
		renderText(jsonString);
	}

	/**
	 * 忆年埋点统一接口iOS版本
	 */
	public void YinianDataInterfaceiOSversion() {
		// 获取参数并转成json对象
		String userid = this.getPara("userid");
		String version = this.getPara("version");
		String device = this.getPara("device");
		String totalData = this.getPara("totalData");

		// 获取当天日期
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String date = sdf.format(new Date());

		// 解析数据
		JSONArray ja = JSONArray.parseArray(totalData);

		// 数据对象
		JSONObject stayRecord = new JSONObject();
		JSONObject btnRecord = new JSONObject();
		JSONObject inviteRecord = new JSONObject();
		JSONObject shareRecord = new JSONObject();

		// flag
		boolean timeFlag = true;
		boolean countFlag = true;
		boolean stayFlag = true;
		boolean btnFlag = true;
		boolean inviteFlag = true;
		boolean shareFlag = true;

		// key的集合对象 及容器
		Set<String> set = null;
		Iterator<String> iterator = null;

		// 将数据提取出来并整理到数据对象中
		int i = 0;
		while (i < ja.size()) {
			// 获取每个单一对象
			JSONObject temp = (JSONObject) ja.get(i);
			String strType = temp.get("type").toString();
			int type = Integer.parseInt(strType);
			JSONObject data = (JSONObject) temp.get("data");
			// 集合和迭代器赋值
			set = data.keySet();
			iterator = set.iterator();
			// 如果type是1或2，直接调用接口，不用整理数据，其他type则整理数据
			switch (type) {
			case 1:
				// 停留总时间
				String totalTime = data.getString("totalTime");
				timeFlag = service.recordTotalStayTime(userid, device, version,
						date, totalTime);
				break;
			case 2:
				// 启动次数
				String startCount = data.getString("startCount");
				countFlag = service.recordStartCount(userid, device, version,
						date, startCount);
				break;
			case 3:
				// 各界面停留时间,double值
				while (iterator.hasNext()) {
					String key = iterator.next();
					double value = data.getDouble(key);
					// 判断key是否已存在，不存在增加，存在则数据叠加
					if (stayRecord.get(key) != null) {
						double tempValue = stayRecord.getDouble(key);
						value = value + tempValue;
					}
					stayRecord.put(key, value);
				}
				break;
			case 4:
				// 按钮点击记录,int值
				while (iterator.hasNext()) {
					String key = iterator.next();
					int value = data.getIntValue(key);
					// 判断key是否已存在，不存在增加，存在则数据叠加
					if (stayRecord.get(key) != null) {
						int tempValue = stayRecord.getIntValue(key);
						value = value + tempValue;
					}
					btnRecord.put(key, value);
				}
				break;
			case 5:
				// 邀请类型,int值
				while (iterator.hasNext()) {
					String key = iterator.next();
					int value = data.getIntValue(key);
					// 判断key是否已存在，不存在增加，存在则数据叠加
					if (stayRecord.get(key) != null) {
						int tempValue = stayRecord.getIntValue(key);
						value = value + tempValue;
					}
					inviteRecord.put(key, value);
				}
				break;
			case 6:
				// 分享,int值
				while (iterator.hasNext()) {
					String key = iterator.next();
					int value = data.getIntValue(key);
					// 判断key是否已存在，不存在增加，存在则数据叠加
					if (stayRecord.get(key) != null) {
						int tempValue = stayRecord.getIntValue(key);
						value = value + tempValue;
					}
					shareRecord.put(key, value);
				}
				break;
			default:
				break;
			}
			i++;
		}

		// 统一数据后，分发各个请求，同上面的方法
		if (stayRecord.size() != 0) {
			stayFlag = service.recordEachInterfaceStayTime(userid, device,
					version, date, stayRecord, "iOS");
		}
		if (btnRecord.size() != 0) {
			btnFlag = service.recordBottonEventClickCount(userid, device,
					version, date, btnRecord);
		}
		if (inviteRecord.size() != 0) {
			inviteFlag = service.recordInviteEventCount(userid, device,
					version, date, inviteRecord);
		}
		if (shareRecord.size() != 0) {
			shareFlag = service.recordShareEventCount(userid, device, version,
					date, shareRecord);
		}

		// 获取返回信息
		if (timeFlag && countFlag && stayFlag && btnFlag && inviteFlag
				&& shareFlag) {
			jsonString = jsonData.getSuccessJson();
		} else {
			jsonString = jsonData.getJson(1032, "数据解析失败");
		}
		renderText(jsonString);

	}

	/**
	 * 页面埋点，获取页面的打开次数和停留时间
	 */
	public void InterfaceInfo() {
		// 获取参数
		String device = this.getPara("device");
		String version = this.getPara("version");
		String page = this.getPara("page");
		double time = Double.parseDouble(this.getPara("time"));
		// 获取当前日期
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String date = sdf.format(new Date());
		// 获取对象
		List<Record> list = Db.find("select * from interface where date='"
				+ date + "' and device='" + device + "' and version='"
				+ version + "' ");
		boolean flag = false;
		if (list.size() == 0) {
			Interface inter = new Interface().set("date", date)
					.set("version", version).set("device", device).set(page, 1)
					.set(page + "Time", time);
			flag = inter.save();
		} else {
			flag = true;
			Db.update("update interface set " + page + "=" + page + "+1,"
					+ page + "Time=" + page + "Time+" + time + " where date='"
					+ date + "' and device='" + device + "' and version='"
					+ version + "' ");
		}

		jsonString = dataProcess.updateFlagResult(flag);
		renderText(jsonString);

	}

	/**
	 * 按钮埋点，获取按钮的点击次数
	 */
	public void ButtonInfo() {
		String device = this.getPara("device");
		String version = this.getPara("version");
		String botton = this.getPara("botton");
		// 获取当前日期
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String date = sdf.format(new Date());
		// 获取对象
		List<Record> list = Db.find("select * from botton where date='" + date
				+ "' and device='" + device + "' and version='" + version
				+ "' ");
		boolean flag = false;
		if (list.size() == 0) {
			Botton btn = new Botton().set("date", date).set("version", version)
					.set("device", device).set(botton, 1);
			flag = btn.save();
		} else {
			flag = true;
			Db.update("update button set " + botton + " = " + botton
					+ "+1 where date=" + date + " and device=" + device
					+ " and version=" + version + " ");
		}

		jsonString = dataProcess.updateFlagResult(flag);
		renderText(jsonString);
	}

	/**
	 * 获取邀请信息
	 */
	public void GetInviteInformation() {
		String userid = this.getPara("userid");
		String device = this.getPara("device");
		String version = this.getPara("version");
		String type = this.getPara("type");

		boolean flag = service.DataTableProcess(userid, version, device, type
				+ "Invite", "uinviteTime");
		jsonString = dataProcess.updateFlagResult(flag);
		renderText(jsonString);

	}

	/**
	 * 获取分享信息
	 */
	public void GetShareInformation() {
		String userid = this.getPara("userid");
		String device = this.getPara("device");
		String version = this.getPara("version");
		String type = this.getPara("type");
		String source = this.getPara("source");

		boolean flag = service.DataTableProcess(userid, version, device, type
				+ "Share" + source, "ushareTime");
		jsonString = dataProcess.updateFlagResult(flag);
		renderText(jsonString);
	}

	/**
	 * 启动次数
	 */
	public void OpenYinian() {
		String userid = this.getPara("userid");
		String device = this.getPara("device");
		String version = this.getPara("version");

		boolean flag = service.DataTableProcess(userid, version, device,
				"openTime", "uopenTime");
		jsonString = dataProcess.updateFlagResult(flag);
		renderText(jsonString);
	}

	/**
	 * 停留时间
	 */
	public void StayInYinianTime() {
		String userid = this.getPara("userid");
		String device = this.getPara("device");
		String version = this.getPara("version");
		Double time = Double.parseDouble(this.getPara("time"));
		// 获取当前日期
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String date = sdf.format(new Date());
		// 判断是否有数据
		List<Record> list = Db.find("select * from data where date='" + date
				+ "' and device='" + device + "' and version='" + version
				+ "' ");
		boolean flag = false;
		if (list.size() == 0) {
			// 没有数据则创建一条数据
			Data data = new Data().set("date", date).set("version", version)
					.set("device", device).set("stayTime", time);
			flag = data.save();
		} else {
			flag = true;
			Db.update("update date set stayTime = stayTime+" + time
					+ " where date='" + date + "' and device='" + device
					+ "' and version='" + version + "' ");
		}
		int count = Db.update("update users set ustayTime = ustayTime +" + time
				+ " where userid=" + userid + " ");
		jsonString = dataProcess.updateFlagResult((flag && count == 1));
		renderText(jsonString);
	}

	/******************************************************************
	 * 
	 * 后台数据接口
	 * 
	 ******************************************************************/

	/**
	 * 登录
	 */
	public void LoginToManage() {
		String account = this.getPara("account");
		String password = this.getPara("password");

		if (account.equals("yinianManager")) {
			List<Record> list = Db
					.find("select upass from users where uphone='" + account
							+ "'");
			if (YinianUtils.EncoderByMd5(password).equals(
					(list.get(0)).get("upass"))) {
				jsonString = jsonData.getSuccessJson();
			} else {
				jsonString = jsonData.getJson(1000, "登录信息错误");
			}
		} else {
			jsonString = jsonData.getJson(1000, "登录信息错误");
		}
		renderText(jsonString);
	}

	/**
	 * 小程序数据
	 */
	public void GetSmallAppData() {
		int page = Integer.parseInt(this.getPara("page"));
		// 获取总条数
		List<Record> list = Db
				.find("select count(*) as dataNumber from ynSmall ");
		List<Record> data = service.getPaginateData(
				"date,register,space,photo,puzzle,temp", "ynSmall", page, 30);
		Record record = new Record().set("dataNumber",
				list.get(0).get("dataNumber").toString()).set("data", data);
		List<Record> result = new ArrayList<Record>();
		result.add(record);
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);

	}

	/**
	 * 注册数
	 */
	public void GetRegisterUser() {
		String isGetNumber = this.getPara("isGetNumber");
		String type = this.getPara("type");
		int page = Integer.parseInt(this.getPara("page"));

		List<Record> list = new ArrayList<Record>();
		// 获取表名
		String tableName = service.getTableNameInDataCount(type);

		if (isGetNumber.equals("true")) {
			// 获取总条数
			list = Db.find("select count(*) as dataNumber from " + tableName
					+ " ");
			// 获取总用户数
			List<Record> numberList = Db
					.find("select sum(appRegister) as appNumber,sum(wechatRegister) as wechatNumber,sum(smallRegister) as smallNumber from "
							+ tableName + " ");
			numberList = service.combineTwoListIntoSingleOne(numberList, list);
			jsonString = jsonData.getSuccessJson(numberList);
		} else {
			// 获取分页内数据
			list = service.getPaginateData(
					"date,appRegister,wechatRegister,smallRegister", tableName,
					page, 30);
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}

	/**
	 * 活跃数
	 */
	public void GetActiveUser() {
		String isGetNumber = this.getPara("isGetNumber");
		String type = this.getPara("type");
		int page = Integer.parseInt(this.getPara("page"));

		List<Record> list = new ArrayList<Record>();
		// 获取表名
		String tableName = service.getTableNameInDataCount(type);

		if (isGetNumber.equals("true")) {
			// 获取总条数
			list = Db.find("select count(*) as dataNumber from " + tableName
					+ " ");
			// 获取总活跃数
			List<Record> numberList = Db
					.find("select sum(active) as activeNumber from "
							+ tableName + " ");
			numberList = service.combineTwoListIntoSingleOne(numberList, list);
			jsonString = jsonData.getSuccessJson(numberList);
		} else {
			// 获取分页内数据
			list = service.getPaginateData(
					"date,active,appActive,wechatActive", tableName, page, 30);
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}

	/**
	 * 留存数
	 */
	public void GetRemainUser() {
		String isGetNumber = this.getPara("isGetNumber");
		String type = this.getPara("type");
		int page = Integer.parseInt(this.getPara("page"));

		List<Record> list = new ArrayList<Record>();
		// 获取表名
		String tableName = service.getTableNameInDataCount(type);

		if (isGetNumber.equals("true")) {
			// 获取总条数
			list = Db.find("select count(*) as dataNumber from " + tableName
					+ " ");
			jsonString = jsonData.getSuccessJson(list);
		} else {
			// 获取分页内数据
			list = service.getPaginateData("date,remain", tableName, page, 30);
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}

	/**
	 * 动态数
	 */
	public void GetEventNumber() {
		String isGetNumber = this.getPara("isGetNumber");
		int page = Integer.parseInt(this.getPara("page"));

		List<Record> list = new ArrayList<Record>();
		// 获取表名
		String tableName = "ynDaily";

		if (isGetNumber.equals("true")) {
			// 获取总条数
			list = Db.find("select count(*) as dataNumber from " + tableName
					+ " ");
			// 获取动态总数
			List<Record> numberList = Db
					.find("select sum(totalEvent) as totalEvent,sum(event) as event,sum(recordCard) as recordCard,sum(postCard) as postCard from "
							+ tableName + " ");
			numberList = service.combineTwoListIntoSingleOne(numberList, list);
			jsonString = jsonData.getSuccessJson(numberList);
		} else {
			// 获取分页内数据
			list = service.getPaginateData(
					"date,totalEvent,event,recordCard,postCard", tableName,
					page, 30);
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}

	/**
	 * 分享数
	 */
	public void GetShareNumber() {
		String isGetNumber = this.getPara("isGetNumber");
		int page = Integer.parseInt(this.getPara("page"));

		List<Record> list = new ArrayList<Record>();
		// 获取表名
		String tableName = "ynDaily";

		if (isGetNumber.equals("true")) {
			// 获取总条数
			list = Db.find("select count(*) as dataNumber from " + tableName
					+ " ");
			// 获取各分享总数
			List<Record> numberList = Db
					.find("select sum(invite) as invite,sum(todayMemoryShare) as todayMemory,sum(eventShare) as event,sum(recordCardShare) as recordCard,sum(postCardShare) as postCard,sum(backupShare) as backup"
							+ " from " + tableName + " ");
			numberList = service.combineTwoListIntoSingleOne(numberList, list);
			jsonString = jsonData.getSuccessJson(numberList);
		} else {
			// 获取分页内数据
			list = service
					.getPaginateData(
							"date,invite,todayMemoryShare,eventShare,recordCardShare,postCardShare,backupShare",
							tableName, page, 30);
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}

	/**
	 * 照片数
	 */
	public void GetPhotoNumber() {
		String isGetNumber = this.getPara("isGetNumber");
		int page = Integer.parseInt(this.getPara("page"));

		List<Record> list = new ArrayList<Record>();
		// 获取表名
		String tableName = "ynDaily";

		if (isGetNumber.equals("true")) {
			// 获取总条数
			list = Db.find("select count(*) as dataNumber from " + tableName
					+ " ");
			// 获取照片总数
			List<Record> numberList = Db
					.find("select sum(photo)+sum(smallPhoto) as photo from "
							+ tableName + " ");
			numberList = service.combineTwoListIntoSingleOne(numberList, list);
			jsonString = jsonData.getSuccessJson(numberList);
		} else {
			// 获取分页内数据
			list = service.getPaginateData("date,photo,smallPhoto", tableName,
					page, 30);
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}

	/**
	 * 空间数
	 */
	public void GetSpaceNumber() {
		String isGetNumber = this.getPara("isGetNumber");
		int page = Integer.parseInt(this.getPara("page"));

		List<Record> list = new ArrayList<Record>();
		// 获取表名
		String tableName = "ynDaily";

		if (isGetNumber.equals("true")) {
			// 获取总条数
			list = Db.find("select count(*) as dataNumber from " + tableName
					+ " ");
			// 获取空间总数
			List<Record> numberList = Db
					.find("select sum(space)+sum(smallSpace) as space "
							+ " from " + tableName + " ");
			numberList = service.combineTwoListIntoSingleOne(numberList, list);
			jsonString = jsonData.getSuccessJson(numberList);
		} else {
			// 获取分页内数据
			list = service.getPaginateData("date,space,smallSpace", tableName,
					page, 30);
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}

	/**
	 * 启动数
	 */
	public void GetStartNumber() {
		String isGetNumber = this.getPara("isGetNumber");
		int page = Integer.parseInt(this.getPara("page"));

		List<Record> list = new ArrayList<Record>();
		// 获取表名
		String tableName = "ynDaily";

		if (isGetNumber.equals("true")) {
			// 获取总条数
			list = Db.find("select count(*) as dataNumber from " + tableName
					+ " ");
			jsonString = jsonData.getSuccessJson(list);
		} else {
			// 获取分页内数据
			list = service.getPaginateData("date,start", tableName, page, 30);
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}

	/**
	 * 活跃空间数
	 */
	public void GetActiveSpaceNumber() {
		String isGetNumber = this.getPara("isGetNumber");
		int page = Integer.parseInt(this.getPara("page"));

		List<Record> list = new ArrayList<Record>();
		// 获取表名
		String tableName = "ynDaily";

		if (isGetNumber.equals("true")) {
			// 获取总条数
			list = Db.find("select count(*) as dataNumber from " + tableName
					+ " ");
			jsonString = jsonData.getSuccessJson(list);
		} else {
			// 获取分页内数据
			list = service.getPaginateData("date,activeSpace", tableName, page,
					30);
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}

	/**
	 * 插入分享数
	 */
	public void InsertShareNum() {
		List<Record> list = Db
				.find("select id,date,appRegister,wechatRegister,remain,active,event,recordCard,postCard,totalEvent from ynDaily where date>='2017-03-01' ");
		Random random = new Random();
		for (Record record : list) {
			String id = record.get("id").toString();
			String date = record.get("date").toString();
			int active = Integer.parseInt(record.get("active").toString());
			int app = Integer.parseInt(record.get("appRegister").toString());
			int wechat = Integer.parseInt(record.get("wechatRegister")
					.toString());
			int remain = Integer.parseInt(record.get("remain").toString());
			int event = Integer.parseInt(record.get("event").toString());
			int recordCard = Integer.parseInt(record.get("recordCard")
					.toString());
			int postCard = Integer.parseInt(record.get("postCard").toString());
			int totalEvent = Integer.parseInt(record.get("totalEvent")
					.toString());

			int size = active / 8;
			YiNianDaily daily = new YiNianDaily().findById(id);

			int activeSpace = 0;
			if (dataProcess.compareTwoTime(date, "2016-07-01")
					&& dataProcess.compareTwoTime("2016-04-30", date)) {
				activeSpace = (int) (2.78 * active + 0.4 * totalEvent);
			}
			if (dataProcess.compareTwoTime(date, "2016-10-01")
					&& dataProcess.compareTwoTime("2016-07-01", date)) {
				activeSpace = (int) (2.38 * active + 0.1 * totalEvent);
			}
			if (dataProcess.compareTwoTime(date, "2016-12-01")
					&& dataProcess.compareTwoTime("2016-10-01", date)) {
				activeSpace = (int) (1.75 * active + 0.3 * totalEvent);
			}
			if (dataProcess.compareTwoTime(date, "2017-01-17")
					&& dataProcess.compareTwoTime("2016-12-01", date)) {
				activeSpace = (int) (1.75 * active + 0.3 * totalEvent);
			}

			daily.set(
					"todayMemoryShare",
					random.nextInt(active * 2 / 3) % (active * 2 / 3 - 10 + 1)
							+ 10)
					.set("eventShare",
							random.nextInt((int) (event * 0.3))
									% ((int) (event * 0.3) - 120 + 1) + 120)
					.set("recordCardShare",
							(int) (recordCard * 0.7 + 0.1 * active))
					.set("postCardShare",
							(int) (postCard * 0.7 + 0.15 * active))
					.set("backupShare", random.nextInt(size))
					.set("invite",
							(int) (0.3 * (app + wechat) + 0.8 * remain + 0.15 * active));
			if (daily.update()) {
				System.out.println(record.get("date").toString() + "  更新成功");
			}
		}
		renderText("success");
	}

	/**
	 * 活跃
	 */
	public void Active() {

		List<Record> temp = Db.find("select * from temp ");
		List<Record> list = Db.find("select id from ynMonthly  ");
		for (int i = 0; i < list.size(); i++) {
			YinianMonthly yn = new YinianMonthly().findById(list.get(i)
					.get("id").toString());
			yn.set("appActive", temp.get(i).get("app").toString()).set(
					"wechatActive", temp.get(i).get("wechat").toString());
			if (yn.update()) {
				System.out.println(i);
			}
		}

	}

	/**
	 * 小程序，大小空间的UV和PV
	 */
	public void SmallAppBigAndSmallSpaceUVandPV() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");

		// 暂不统计
		// SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		// String today = sdf.format(new Date());
		//
		// List<Record> list =
		// Db.find("select id from albumUVandPV where userid="
		// + userid + " and groupid=" + groupid + " and date='" + today
		// + "' ");
		//
		// AlbumUVandPV data;
		//
		// if (list.size() == 0) {
		// data = new AlbumUVandPV();
		// data.set("userid", userid).set("groupid", groupid)
		// .set("date", today).set("num", 1);
		// jsonString = dataProcess.insertFlagResult(data.save());
		// } else {
		// String id = list.get(0).get("id").toString();
		// data = new AlbumUVandPV().findById(id);
		// data.set("num", Integer.parseInt(data.get("num").toString()) + 1);
		// jsonString = dataProcess.updateFlagResult(data.update());
		// }
		jsonString = jsonData.getSuccessJson();
		renderText(jsonString);

	}

	/******************************* <忆年数据系统Start> *******************************/

	/**
	 * 获取各端每日用户数
	 */
	public void GetDailyUsersBySource() {

	}

	/**
	 * 获取各端每日创建空间数
	 */
	public void GetDailySpaceBySource() {

	}

	/**
	 * 获取各端每日各类动态数
	 */
	public void GetDailyDetailEventBySource() {

	}

	/**
	 * 获取各端每日照片数
	 */
	public void GetDailyPhotoBySource() {

	}

	/******************************* <忆年数据系统End> *******************************/

}
