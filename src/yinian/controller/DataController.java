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

	// ���ݴ�����
	YinianDataProcess dataProcess = new YinianDataProcess();
	// ���ص�json�ַ���
	private String jsonString;
	// json������
	private JsonData jsonData = new JsonData();
	// ҵ���߼���
	DataService service = new DataService();

	/**
	 * �������ͳһ�ӿ�Android�汾
	 */
	public void YinianDataInterface() {

		String result = this.getPara("result");
		JSONObject jo = (JSONObject) JSONObject.parse(result);

		// ��ȡ��������
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String date = sdf.format(new Date());

		// flag
		boolean timeFlag = true;
		boolean countFlag = true;
		boolean stayFlag = true;
		boolean btnFlag = true;
		boolean inviteFlag = true;
		boolean shareFlag = true;

		// ��ȡ����
		String userid = jo.get("userid").toString();
		String device = jo.get("device").toString();
		String version = jo.get("version").toString();
		JSONArray ja = (JSONArray) jo.get("totalData");
		// �����������飬����typeִ�в�ͬ�ķ���
		for (int i = 0; i < ja.size(); i++) {
			JSONObject temp = (JSONObject) ja.get(i);
			int type = (int) temp.get("type");
			JSONObject data = (JSONObject) temp.get("data");
			switch (type) {
			case 1:
				// ͣ����ʱ��
				double totalTime = data.getDoubleValue("totalTime") / 1000;
				timeFlag = service.recordTotalStayTime(userid, device, version,
						date, totalTime);
				break;
			case 2:
				// ��������
				String startCount = data.getString("startCount");
				countFlag = service.recordStartCount(userid, device, version,
						date, startCount);
				break;
			case 3:
				// ������ͣ��ʱ��
				stayFlag = service.recordEachInterfaceStayTime(userid, device,
						version, date, data, "Android");
				break;
			case 4:
				// ��ť�����¼
				btnFlag = service.recordBottonEventClickCount(userid, device,
						version, date, data);
				break;
			case 5:
				// ��������
				inviteFlag = service.recordInviteEventCount(userid, device,
						version, date, data);
				break;
			case 6:
				// ����
				shareFlag = service.recordShareEventCount(userid, device,
						version, date, data);
				break;
			default:
				break;
			}
		}
		// ��ȡ������Ϣ
		if (timeFlag && countFlag && stayFlag && btnFlag && inviteFlag
				&& shareFlag) {
			jsonString = jsonData.getSuccessJson();
		} else {
			jsonString = jsonData.getJson(1032, "���ݽ���ʧ��");
		}
		renderText(jsonString);
	}

	/**
	 * �������ͳһ�ӿ�iOS�汾
	 */
	public void YinianDataInterfaceiOSversion() {
		// ��ȡ������ת��json����
		String userid = this.getPara("userid");
		String version = this.getPara("version");
		String device = this.getPara("device");
		String totalData = this.getPara("totalData");

		// ��ȡ��������
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String date = sdf.format(new Date());

		// ��������
		JSONArray ja = JSONArray.parseArray(totalData);

		// ���ݶ���
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

		// key�ļ��϶��� ������
		Set<String> set = null;
		Iterator<String> iterator = null;

		// ��������ȡ�������������ݶ�����
		int i = 0;
		while (i < ja.size()) {
			// ��ȡÿ����һ����
			JSONObject temp = (JSONObject) ja.get(i);
			String strType = temp.get("type").toString();
			int type = Integer.parseInt(strType);
			JSONObject data = (JSONObject) temp.get("data");
			// ���Ϻ͵�������ֵ
			set = data.keySet();
			iterator = set.iterator();
			// ���type��1��2��ֱ�ӵ��ýӿڣ������������ݣ�����type����������
			switch (type) {
			case 1:
				// ͣ����ʱ��
				String totalTime = data.getString("totalTime");
				timeFlag = service.recordTotalStayTime(userid, device, version,
						date, totalTime);
				break;
			case 2:
				// ��������
				String startCount = data.getString("startCount");
				countFlag = service.recordStartCount(userid, device, version,
						date, startCount);
				break;
			case 3:
				// ������ͣ��ʱ��,doubleֵ
				while (iterator.hasNext()) {
					String key = iterator.next();
					double value = data.getDouble(key);
					// �ж�key�Ƿ��Ѵ��ڣ����������ӣ����������ݵ���
					if (stayRecord.get(key) != null) {
						double tempValue = stayRecord.getDouble(key);
						value = value + tempValue;
					}
					stayRecord.put(key, value);
				}
				break;
			case 4:
				// ��ť�����¼,intֵ
				while (iterator.hasNext()) {
					String key = iterator.next();
					int value = data.getIntValue(key);
					// �ж�key�Ƿ��Ѵ��ڣ����������ӣ����������ݵ���
					if (stayRecord.get(key) != null) {
						int tempValue = stayRecord.getIntValue(key);
						value = value + tempValue;
					}
					btnRecord.put(key, value);
				}
				break;
			case 5:
				// ��������,intֵ
				while (iterator.hasNext()) {
					String key = iterator.next();
					int value = data.getIntValue(key);
					// �ж�key�Ƿ��Ѵ��ڣ����������ӣ����������ݵ���
					if (stayRecord.get(key) != null) {
						int tempValue = stayRecord.getIntValue(key);
						value = value + tempValue;
					}
					inviteRecord.put(key, value);
				}
				break;
			case 6:
				// ����,intֵ
				while (iterator.hasNext()) {
					String key = iterator.next();
					int value = data.getIntValue(key);
					// �ж�key�Ƿ��Ѵ��ڣ����������ӣ����������ݵ���
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

		// ͳһ���ݺ󣬷ַ���������ͬ����ķ���
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

		// ��ȡ������Ϣ
		if (timeFlag && countFlag && stayFlag && btnFlag && inviteFlag
				&& shareFlag) {
			jsonString = jsonData.getSuccessJson();
		} else {
			jsonString = jsonData.getJson(1032, "���ݽ���ʧ��");
		}
		renderText(jsonString);

	}

	/**
	 * ҳ����㣬��ȡҳ��Ĵ򿪴�����ͣ��ʱ��
	 */
	public void InterfaceInfo() {
		// ��ȡ����
		String device = this.getPara("device");
		String version = this.getPara("version");
		String page = this.getPara("page");
		double time = Double.parseDouble(this.getPara("time"));
		// ��ȡ��ǰ����
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String date = sdf.format(new Date());
		// ��ȡ����
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
	 * ��ť��㣬��ȡ��ť�ĵ������
	 */
	public void ButtonInfo() {
		String device = this.getPara("device");
		String version = this.getPara("version");
		String botton = this.getPara("botton");
		// ��ȡ��ǰ����
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String date = sdf.format(new Date());
		// ��ȡ����
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
	 * ��ȡ������Ϣ
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
	 * ��ȡ������Ϣ
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
	 * ��������
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
	 * ͣ��ʱ��
	 */
	public void StayInYinianTime() {
		String userid = this.getPara("userid");
		String device = this.getPara("device");
		String version = this.getPara("version");
		Double time = Double.parseDouble(this.getPara("time"));
		// ��ȡ��ǰ����
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String date = sdf.format(new Date());
		// �ж��Ƿ�������
		List<Record> list = Db.find("select * from data where date='" + date
				+ "' and device='" + device + "' and version='" + version
				+ "' ");
		boolean flag = false;
		if (list.size() == 0) {
			// û�������򴴽�һ������
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
	 * ��̨���ݽӿ�
	 * 
	 ******************************************************************/

	/**
	 * ��¼
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
				jsonString = jsonData.getJson(1000, "��¼��Ϣ����");
			}
		} else {
			jsonString = jsonData.getJson(1000, "��¼��Ϣ����");
		}
		renderText(jsonString);
	}

	/**
	 * С��������
	 */
	public void GetSmallAppData() {
		int page = Integer.parseInt(this.getPara("page"));
		// ��ȡ������
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
	 * ע����
	 */
	public void GetRegisterUser() {
		String isGetNumber = this.getPara("isGetNumber");
		String type = this.getPara("type");
		int page = Integer.parseInt(this.getPara("page"));

		List<Record> list = new ArrayList<Record>();
		// ��ȡ����
		String tableName = service.getTableNameInDataCount(type);

		if (isGetNumber.equals("true")) {
			// ��ȡ������
			list = Db.find("select count(*) as dataNumber from " + tableName
					+ " ");
			// ��ȡ���û���
			List<Record> numberList = Db
					.find("select sum(appRegister) as appNumber,sum(wechatRegister) as wechatNumber,sum(smallRegister) as smallNumber from "
							+ tableName + " ");
			numberList = service.combineTwoListIntoSingleOne(numberList, list);
			jsonString = jsonData.getSuccessJson(numberList);
		} else {
			// ��ȡ��ҳ������
			list = service.getPaginateData(
					"date,appRegister,wechatRegister,smallRegister", tableName,
					page, 30);
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}

	/**
	 * ��Ծ��
	 */
	public void GetActiveUser() {
		String isGetNumber = this.getPara("isGetNumber");
		String type = this.getPara("type");
		int page = Integer.parseInt(this.getPara("page"));

		List<Record> list = new ArrayList<Record>();
		// ��ȡ����
		String tableName = service.getTableNameInDataCount(type);

		if (isGetNumber.equals("true")) {
			// ��ȡ������
			list = Db.find("select count(*) as dataNumber from " + tableName
					+ " ");
			// ��ȡ�ܻ�Ծ��
			List<Record> numberList = Db
					.find("select sum(active) as activeNumber from "
							+ tableName + " ");
			numberList = service.combineTwoListIntoSingleOne(numberList, list);
			jsonString = jsonData.getSuccessJson(numberList);
		} else {
			// ��ȡ��ҳ������
			list = service.getPaginateData(
					"date,active,appActive,wechatActive", tableName, page, 30);
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}

	/**
	 * ������
	 */
	public void GetRemainUser() {
		String isGetNumber = this.getPara("isGetNumber");
		String type = this.getPara("type");
		int page = Integer.parseInt(this.getPara("page"));

		List<Record> list = new ArrayList<Record>();
		// ��ȡ����
		String tableName = service.getTableNameInDataCount(type);

		if (isGetNumber.equals("true")) {
			// ��ȡ������
			list = Db.find("select count(*) as dataNumber from " + tableName
					+ " ");
			jsonString = jsonData.getSuccessJson(list);
		} else {
			// ��ȡ��ҳ������
			list = service.getPaginateData("date,remain", tableName, page, 30);
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}

	/**
	 * ��̬��
	 */
	public void GetEventNumber() {
		String isGetNumber = this.getPara("isGetNumber");
		int page = Integer.parseInt(this.getPara("page"));

		List<Record> list = new ArrayList<Record>();
		// ��ȡ����
		String tableName = "ynDaily";

		if (isGetNumber.equals("true")) {
			// ��ȡ������
			list = Db.find("select count(*) as dataNumber from " + tableName
					+ " ");
			// ��ȡ��̬����
			List<Record> numberList = Db
					.find("select sum(totalEvent) as totalEvent,sum(event) as event,sum(recordCard) as recordCard,sum(postCard) as postCard from "
							+ tableName + " ");
			numberList = service.combineTwoListIntoSingleOne(numberList, list);
			jsonString = jsonData.getSuccessJson(numberList);
		} else {
			// ��ȡ��ҳ������
			list = service.getPaginateData(
					"date,totalEvent,event,recordCard,postCard", tableName,
					page, 30);
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}

	/**
	 * ������
	 */
	public void GetShareNumber() {
		String isGetNumber = this.getPara("isGetNumber");
		int page = Integer.parseInt(this.getPara("page"));

		List<Record> list = new ArrayList<Record>();
		// ��ȡ����
		String tableName = "ynDaily";

		if (isGetNumber.equals("true")) {
			// ��ȡ������
			list = Db.find("select count(*) as dataNumber from " + tableName
					+ " ");
			// ��ȡ����������
			List<Record> numberList = Db
					.find("select sum(invite) as invite,sum(todayMemoryShare) as todayMemory,sum(eventShare) as event,sum(recordCardShare) as recordCard,sum(postCardShare) as postCard,sum(backupShare) as backup"
							+ " from " + tableName + " ");
			numberList = service.combineTwoListIntoSingleOne(numberList, list);
			jsonString = jsonData.getSuccessJson(numberList);
		} else {
			// ��ȡ��ҳ������
			list = service
					.getPaginateData(
							"date,invite,todayMemoryShare,eventShare,recordCardShare,postCardShare,backupShare",
							tableName, page, 30);
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}

	/**
	 * ��Ƭ��
	 */
	public void GetPhotoNumber() {
		String isGetNumber = this.getPara("isGetNumber");
		int page = Integer.parseInt(this.getPara("page"));

		List<Record> list = new ArrayList<Record>();
		// ��ȡ����
		String tableName = "ynDaily";

		if (isGetNumber.equals("true")) {
			// ��ȡ������
			list = Db.find("select count(*) as dataNumber from " + tableName
					+ " ");
			// ��ȡ��Ƭ����
			List<Record> numberList = Db
					.find("select sum(photo)+sum(smallPhoto) as photo from "
							+ tableName + " ");
			numberList = service.combineTwoListIntoSingleOne(numberList, list);
			jsonString = jsonData.getSuccessJson(numberList);
		} else {
			// ��ȡ��ҳ������
			list = service.getPaginateData("date,photo,smallPhoto", tableName,
					page, 30);
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}

	/**
	 * �ռ���
	 */
	public void GetSpaceNumber() {
		String isGetNumber = this.getPara("isGetNumber");
		int page = Integer.parseInt(this.getPara("page"));

		List<Record> list = new ArrayList<Record>();
		// ��ȡ����
		String tableName = "ynDaily";

		if (isGetNumber.equals("true")) {
			// ��ȡ������
			list = Db.find("select count(*) as dataNumber from " + tableName
					+ " ");
			// ��ȡ�ռ�����
			List<Record> numberList = Db
					.find("select sum(space)+sum(smallSpace) as space "
							+ " from " + tableName + " ");
			numberList = service.combineTwoListIntoSingleOne(numberList, list);
			jsonString = jsonData.getSuccessJson(numberList);
		} else {
			// ��ȡ��ҳ������
			list = service.getPaginateData("date,space,smallSpace", tableName,
					page, 30);
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}

	/**
	 * ������
	 */
	public void GetStartNumber() {
		String isGetNumber = this.getPara("isGetNumber");
		int page = Integer.parseInt(this.getPara("page"));

		List<Record> list = new ArrayList<Record>();
		// ��ȡ����
		String tableName = "ynDaily";

		if (isGetNumber.equals("true")) {
			// ��ȡ������
			list = Db.find("select count(*) as dataNumber from " + tableName
					+ " ");
			jsonString = jsonData.getSuccessJson(list);
		} else {
			// ��ȡ��ҳ������
			list = service.getPaginateData("date,start", tableName, page, 30);
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}

	/**
	 * ��Ծ�ռ���
	 */
	public void GetActiveSpaceNumber() {
		String isGetNumber = this.getPara("isGetNumber");
		int page = Integer.parseInt(this.getPara("page"));

		List<Record> list = new ArrayList<Record>();
		// ��ȡ����
		String tableName = "ynDaily";

		if (isGetNumber.equals("true")) {
			// ��ȡ������
			list = Db.find("select count(*) as dataNumber from " + tableName
					+ " ");
			jsonString = jsonData.getSuccessJson(list);
		} else {
			// ��ȡ��ҳ������
			list = service.getPaginateData("date,activeSpace", tableName, page,
					30);
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}

	/**
	 * ���������
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
				System.out.println(record.get("date").toString() + "  ���³ɹ�");
			}
		}
		renderText("success");
	}

	/**
	 * ��Ծ
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
	 * С���򣬴�С�ռ��UV��PV
	 */
	public void SmallAppBigAndSmallSpaceUVandPV() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");

		// �ݲ�ͳ��
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

	/******************************* <��������ϵͳStart> *******************************/

	/**
	 * ��ȡ����ÿ���û���
	 */
	public void GetDailyUsersBySource() {

	}

	/**
	 * ��ȡ����ÿ�մ����ռ���
	 */
	public void GetDailySpaceBySource() {

	}

	/**
	 * ��ȡ����ÿ�ո��ද̬��
	 */
	public void GetDailyDetailEventBySource() {

	}

	/**
	 * ��ȡ����ÿ����Ƭ��
	 */
	public void GetDailyPhotoBySource() {

	}

	/******************************* <��������ϵͳEnd> *******************************/

}
