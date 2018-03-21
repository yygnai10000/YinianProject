package yinian.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import yinian.model.Botton;
import yinian.model.Data;
import yinian.model.Interface;
import yinian.model.Invite;
import yinian.model.Share;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Before;
import com.jfinal.kit.JsonKit;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;

public class DataService {

	/**
	 * 数据表处理方法
	 * 
	 * @param userid
	 * @param date
	 * @param version
	 * @param device
	 * @param dataFild
	 * @param userFild
	 * @return
	 */
	public boolean DataTableProcess(String userid, String version,
			String device, String dataField, String userField) {
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
					.set("device", device).set(dataField, 1);
			flag = data.save();
		} else {
			flag = true;
			Db.update("update date set " + dataField + " = " + dataField
					+ "+1 where date='" + date + "' and device='" + device
					+ "' and version='" + version + "' ");
		}
		int count = Db.update("update users set " + userField + " = "
				+ userField + "+1 where userid=" + userid + " ");
		return (flag && count == 1);
	}

	/**
	 * 记录产品停留时间
	 */
	@Before(Tx.class)
	public boolean recordTotalStayTime(String userid, String device,
			String version, String date, Object data) {
		List<Record> list = judgeList("data", device, version, date);
		boolean flag = true;
		Data yinianData = null;
		if (list.size() == 0) {
			yinianData = new Data().set("date", date).set("version", version)
					.set("device", device);
			flag = yinianData.save();
		}
		if (flag) {
			int dataCount = Db.update("update data set stayTime=stayTime+"
					+ data + " where  date='" + date + "' and device='"
					+ device + "' and version='" + version + "'  ");
			int userCount = Db.update("update users set ustayTime=ustayTime+"
					+ data + " where userid =" + userid + "  ");
			return (dataCount == 1) && (userCount == 1);
		} else {
			return false;
		}
	}

	/**
	 * 记录启动次数
	 */
	public boolean recordStartCount(String userid, String device,
			String version, String date, Object data) {
		List<Record> list = judgeList("data", device, version, date);
		boolean flag = true;
		Data yinianData = null;
		if (list.size() == 0) {
			yinianData = new Data().set("date", date).set("version", version)
					.set("device", device);
			flag = yinianData.save();
		}
		if (flag) {
			int dataCount = Db.update("update data set openCount=openCount+"
					+ data + " where  date='" + date + "' and device='"
					+ device + "' and version='" + version + "'  ");
			int userCount = Db
					.update("update users set ustartCount=ustartCount+" + data
							+ " where userid =" + userid + "  ");
			return (dataCount == 1) && (userCount == 1);
		} else {
			return false;
		}
	}

	/**
	 * 记录各界面停留时间
	 */
	@Before(Tx.class)
	public boolean recordEachInterfaceStayTime(String userid, String device,
			String version, String date, JSONObject data, String source) {
		// 返回结果标志
		boolean resultFlag = false;
		// 获取键的集合
		Set<String> set = data.keySet();
		Iterator<String> it = set.iterator();
		// 多个数据需要使用迭代器一一判断添加
		while (it.hasNext()) {
			String key = it.next();
			List<Record> list = judgeList("interface", device, version, date,
					key);
			boolean flag = true;
			if (list.size() == 0) {
				Interface inter = new Interface().set("date", date)
						.set("version", version).set("device", device)
						.set("name", key);
				flag = inter.save();
			}
			if (flag) {
				// 获取键对应的值
				Double value = data.getDouble(key);
				if (source.equals("Android")) {
					// Android的单位是毫秒，转成秒
					value = value / 1000;
				}
				int interfaceStay = Db.update("update interface set time=time+"
						+ value + " where  date='" + date + "' and device='"
						+ device + "' and version='" + version + "' and name='"
						+ key + "'  ");
				resultFlag = (interfaceStay == 1);
				if (!resultFlag) {
					break;
				}
			} else {
				break;
			}
		}
		return resultFlag;
	}

	/**
	 * 记录按钮事件点击次数
	 */
	@Before(Tx.class)
	public boolean recordBottonEventClickCount(String userid, String device,
			String version, String date, JSONObject data) {
		// 返回结果标志
		boolean resultFlag = false;
		// 获取键的集合
		Set<String> set = data.keySet();
		Iterator<String> it = set.iterator();
		// 多个数据需要使用迭代器一一判断添加
		while (it.hasNext()) {
			String key = it.next();
			List<Record> list = judgeList("botton", device, version, date, key);
			boolean flag = true;
			if (list.size() == 0) {
				Botton btn = new Botton().set("date", date)
						.set("version", version).set("device", device)
						.set("name", key);
				flag = btn.save();
			}
			if (flag) {
				// 获取键对应的值
				int value = data.getIntValue(key);
				int btnClick = Db.update("update botton set count=count+"
						+ value + " where  date='" + date + "' and device='"
						+ device + "' and version='" + version + "' and name='"
						+ key + "'  ");
				resultFlag = (btnClick == 1);
				if (!resultFlag) {
					break;
				}
			} else {
				break;
			}
		}
		return resultFlag;
	}

	/**
	 * 记录邀请事件数
	 */
	@Before(Tx.class)
	public boolean recordInviteEventCount(String userid, String device,
			String version, String date, JSONObject data) {
		// 返回结果标志
		boolean resultFlag = false;
		// 获取键的集合
		Set<String> set = data.keySet();
		Iterator<String> it = set.iterator();
		// 用户邀请数的存储变量
		int userInvite = 0;
		// 多个数据需要使用迭代器一一判断添加
		while (it.hasNext()) {
			String key = it.next();
			List<Record> list = judgeList("invite", device, version, date, key);
			boolean flag = true;
			if (list.size() == 0) {
				Invite invite = new Invite().set("date", date)
						.set("version", version).set("device", device)
						.set("name", key);
				flag = invite.save();
			}
			if (flag) {
				// 获取键对应的值
				int value = data.getIntValue(key);
				// 累计之后统一处理
				userInvite = userInvite + value;
				int interfaceStay = Db.update("update invite set count=count+"
						+ value + " where  date='" + date + "' and device='"
						+ device + "' and version='" + version + "' and name='"
						+ key + "'  ");
				resultFlag = (interfaceStay == 1);
				if (!resultFlag) {
					break;
				}
			} else {
				break;
			}
		}
		// 更新用户个人数据
		int userCount = Db.update("update users set uinviteTime = uinviteTime+"
				+ userInvite + " where userid = " + userid + " ");
		return resultFlag && (userCount == 1);
	}

	/**
	 * 记录分享事件数
	 */
	@Before(Tx.class)
	public boolean recordShareEventCount(String userid, String device,
			String version, String date, JSONObject data) {
		// 返回结果标志
		boolean resultFlag = false;
		// 获取键的集合
		Set<String> set = data.keySet();
		Iterator<String> it = set.iterator();
		// 用户分享数的存储变量
		int userShare = 0;
		// 多个数据需要使用迭代器一一判断添加
		while (it.hasNext()) {
			String key = it.next();
			List<Record> list = judgeList("share", device, version, date, key);
			boolean flag = true;
			if (list.size() == 0) {
				Share share = new Share().set("date", date)
						.set("version", version).set("device", device)
						.set("name", key);
				flag = share.save();
			}
			if (flag) {
				// 获取键对应的值
				int value = data.getIntValue(key);
				// 累计之后统一处理
				userShare = userShare + value;
				int interfaceStay = Db.update("update share set count=count+"
						+ value + " where  date='" + date + "' and device='"
						+ device + "' and version='" + version + "' and name='"
						+ key + "'  ");
				resultFlag = (interfaceStay == 1);
				if (!resultFlag) {
					break;
				}
			} else {
				break;
			}
		}
		// 更新用户个人数据
		int userCount = Db.update("update users set ushareTime = ushareTime+"
				+ userShare + " where userid = " + userid + " ");
		return resultFlag && (userCount == 1);
	}

	/**
	 * 判断List，用于判断当日是否有数据
	 * 
	 * @param device
	 * @param version
	 * @param date
	 * @return
	 */
	private static List<Record> judgeList(String table, String device,
			String version, String date) {
		List<Record> list = Db.find("select * from " + table + " where date='"
				+ date + "' and device='" + device + "' and version='"
				+ version + "'  ");
		return list;
	}

	/**
	 * 判断List，需增加name，用于判断当日是否有数据
	 * 
	 * @param table
	 * @param device
	 * @param version
	 * @param date
	 * @param name
	 * @return
	 */
	private static List<Record> judgeList(String table, String device,
			String version, String date, String name) {
		List<Record> list = Db.find("select * from " + table + " where date='"
				+ date + "' and device='" + device + "' and version='"
				+ version + "' and name ='" + name + "'  ");
		return list;
	}

	public String testData() {
		// 基本数据
		Record record = new Record().set("userid", 2).set("device", "iOS")
				.set("version", "3.0.0");
		// // 停留时间
		Record record1 = new Record().set("totalTime", 180.5);
		Record stayTime = new Record().set("type", 1).set("data", record1);
		// 启动次数
		Record record2 = new Record().set("startCount", 5);
		Record startCount1 = new Record().set("type", 2).set("data", record2);
		// 各见面停留时间
		Record temp1 = new Record().set("login", 1);
		Record stay = new Record().set("type", 3).set("data", temp1);

		Record temp11 = new Record().set("homepage", 5);
		Record stay1 = new Record().set("type", 3).set("data", temp11);

		Record temp12 = new Record().set("spaceDetail", 3.3);
		Record stay2 = new Record().set("type", 3).set("data", temp12);

		// 按钮点击次数
		Record temp2 = new Record().set("createAlbum", 1);
		Record botton = new Record().set("type", 4).set("data", temp2);
		Record temp21 = new Record().set("recordCard", 3);
		Record botton1 = new Record().set("type", 4).set("data", temp21);
		Record temp22 = new Record().set("timePostcard", 5);
		Record botton2 = new Record().set("type", 4).set("data", temp22);

		// 邀请类型
		Record temp3 = new Record().set("weChatInvite", 1);
		Record invite = new Record().set("type", 5).set("data", temp3);
		Record temp31 = new Record().set("qqInvite", 3);
		Record invite1 = new Record().set("type", 5).set("data", temp31);
		Record temp32 = new Record().set("messageInvite", 5);
		Record invite2 = new Record().set("type", 5).set("data", temp32);

		// 分享
		Record temp4 = new Record().set("todayMemoryFriendCircle", 1);
		Record share = new Record().set("type", 6).set("data", temp4);
		Record temp41 = new Record().set("todayMemoryWechat", 3);
		Record share1 = new Record().set("type", 6).set("data", temp41);
		Record temp42 = new Record().set("todayMemoryQZone", 5);
		Record share2 = new Record().set("type", 6).set("data", temp42);
		Record temp43 = new Record().set("eventQQ", 1);
		Record share3 = new Record().set("type", 6).set("data", temp43);

		List<Record> list = new ArrayList<Record>();
		// list.add(stayTime);
		// list.add(startCount1);
		list.add(stay);
		list.add(stay1);
		list.add(stay2);
		// list.add(botton);
		// list.add(botton1);
		// list.add(botton2);
		list.add(invite);
		list.add(invite1);
		list.add(invite2);
		// list.add(share);
		// list.add(share1);
		// list.add(share2);
		// list.add(share3);
		record.set("totalData", list);
		String result = JsonKit.toJson(record);
		return result;
	}

	/**
	 * 获取数据统计存储表的表名
	 */
	public String getTableNameInDataCount(String type) {
		String tableName = "";
		switch (type) {
		case "day":
			tableName = "ynDaily";
			break;
		case "week":
			tableName = "ynWeekly";
			break;
		case "month":
			tableName = "ynMonthly";
			break;
		}
		return tableName;
	}

	/**
	 * 获取分页数据集
	 */
	public List<Record> getPaginateData(String keys, String tableName,
			int page, int number) {
		String sqlForSelect = "select " + keys + " ";
		String sqlExceptSelect = " from " + tableName + " order by id desc   ";
		Page<Record> pageList = Db.paginate(page, number, sqlForSelect,
				sqlExceptSelect);
		return pageList.getList();
	}

	/**
	 * 将两个list合成一个
	 */
	public List<Record> combineTwoListIntoSingleOne(List<Record> main,
			List<Record> follow) {
		main.get(0).set("dataNumber", follow.get(0).get("dataNumber"));
		return main;
	}

}
