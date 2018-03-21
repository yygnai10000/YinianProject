package yinian.push;

import java.util.ArrayList;
import java.util.List;

import yinian.app.YinianDAO;
import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.model.Group;
import yinian.model.GroupMember;
import yinian.model.User;
import yinian.utils.JsonData;
import yinian.utils.YinianUtils;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

public class PushMessage {

	private JsonData jsonData = new JsonData(); // json操作类
	private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类
	private YinianGetuiPush push = new YinianGetuiPush(); // 个推推送类
	private YinianDAO dao = new YinianDAO();
	private YinianUtils utils = new YinianUtils();

	public String PUSH_TYPE_OF_EVENT = "event";

	/**
	 * 推送消息给空间成员
	 * 
	 * @param groupid
	 * @param sender
	 */
	public void pushToSpaceMember(String groupid, String sender, String type) {

		// 获取组内除发送者外所有用户的ID
		GroupMember gm = new GroupMember();
		List<Record> useridList = gm.GetAllUseridInTheSpaceExceptCurrentUser(
				groupid, sender);

		// useridList不为null时才进行推送
		if (!(useridList.isEmpty())) {
			// 将useridList中的用户ID拼接成字符串
			String userIDs = dataProcess.changeListToString(useridList,
					"gmuserid");

			// userids不为空字符串时才进行推送
			if (!(userIDs.equals(""))) {

				// 获取要进行推送的用户的cid,按Android和iOS区分
				Record cid = getUsersCids(userIDs, "userid");
				// 获取推送内容
				String pushContent = getPushContent(groupid, sender, type);
				// 获取透传内容
				String transmissionContent = getTransmissionContent(groupid,
						sender, pushContent, type);

				// 构造推送数据对象
				Record data = new Record().set("content", pushContent).set(
						"transmissionContent", transmissionContent);
				// 推送
				push.yinianPushToList(cid, data);
			}
		}
	}
	
	/**
	 * 发送消息给单个用户
	 * @param sender
	 * @param receiver
	 * @param type
	 */
	public void pushToSingleUser(String sender,String receiver,String type){
		
	}

	/**
	 * 获取推送内容
	 * 
	 * @param groupid
	 * @param sender
	 * @param type
	 * @return
	 */
	public String getPushContent(String groupid, String sender, String type) {

		String content = "";

		// 获取消息发送者昵称
		User user = new User().findById(sender);
		String nickname = user.getStr("unickname");

		// 获取空间名称
		Group group = new Group().findById(groupid);
		String gname = group.getStr("gname");

		switch (type) {
		case "event":
			content = nickname + "在圈子“" + gname + "”中发布了新动态";
			break;
		default:
			break;
		}

		return content;
	}

	/**
	 * 获取推送内容
	 * 
	 * @param groupid
	 * @param sender
	 * @param pushContent
	 * @param type
	 * @return
	 */
	public String getTransmissionContent(String groupid, String sender,
			String pushContent, String type) {

		String transmissionContent = "";
		Record transmissionRecord = new Record();
		List<Record> list = new ArrayList<Record>();

		switch (type) {
		case "event":
			transmissionRecord.set("groupid", groupid).set("pushContent",
					pushContent);
			list.add(transmissionRecord);
			transmissionContent = jsonData.getJson(3, "发表动态", list);
			break;
		default:
			break;
		}

		return transmissionContent;
	}

	/**
	 * 通过用户的标识符获取用户的cid 一般是手机号码phone或者用户id——userid
	 * 
	 * @param identyfier
	 * @return
	 */
	public Record getUsersCids(String identyfier, String type) {
		List<Record> cidList = new ArrayList<Record>();
		switch (type) {
		case "phone":
			cidList = Db
					.find("select ucid,udevice from users where uphone in ("
							+ identyfier + ") ");
			break;
		case "userid":
			cidList = Db
					.find("select ucid,udevice from users where userid in ("
							+ identyfier + ") ");
			break;
		}
		Record result = disposeCidsByDevice(cidList);
		return result;
	}

	/**
	 * 根据设备拆分cid
	 * 
	 * @param list
	 * @return
	 */
	public Record disposeCidsByDevice(List<Record> list) {
		
		String AndroidCids = ""; // 存放Android的cid
		String iOSCids = ""; // 存放iOS的cid
		
		for (Record record : list) {
			if (record.get("udevice") != null && record.get("ucid") != null) {
				if ((record.get("udevice").toString()).equals("Android")) {
					AndroidCids += record.get("ucid").toString();
					AndroidCids += ",";
				}
				if ((record.get("udevice").toString()).equals("iOS")) {
					iOSCids += record.get("ucid").toString();
					iOSCids += ",";
				}
			}
		}
		
		// 字符串处理
		if (!AndroidCids.equals("")) {
			AndroidCids = AndroidCids.substring(0, AndroidCids.length() - 1);
		}
		if (!iOSCids.equals("")) {
			iOSCids = iOSCids.substring(0, iOSCids.length() - 1);
		}
		
		Record result = new Record().set("AndroidCids", AndroidCids).set(
				"iOSCids", iOSCids);
		return result;
	}

	/**
	 * 获取推送的Record
	 * 
	 * @param senderID
	 * @param receiverID
	 * @param groupid
	 * @param content
	 * @param id
	 * @param type
	 * @return
	 */
	public Record getPushRecord(String senderID, String receiverID,
			String groupid, String content, String id, String type,
			Record otherRecord) {
		// 返回码
		int code = 0;
		// 返回消息
		String message = "success";
		// 组头像
		String groupPicture = "";
		// 获取接收者的ucid和udevice
		Record pushRecord = dao.queryUserInfo(receiverID, "ucid,udevice");
		// ucid和udevice都不能为null才能进行推送
		if (pushRecord.get("udevice") != null && pushRecord.get("ucid") != null
				&& !((pushRecord.get("ucid").toString()).equals(""))) {
			// 获取发送者的信息
			Record senderInfoRecord = dao.queryUserInfo(senderID,
					"unickname,upic");
			// 构造推送内容
			String pushContent = senderInfoRecord.getStr("unickname") + content;

			// 构造透传内容
			String time = utils.getTimeNow();// 获取系统时间

			int newID = Integer.parseInt(id);// 将id转成整型

			// 拼接透传Record
			Record transmissionRecord = new Record();
			switch (type) {
			case "inviteGroup":
				transmissionRecord.set("igid", newID).set("igstatus", 0)
						.set("unickname", senderInfoRecord.getStr("unickname"))
						.set("igcontent", content).set("igtime", time)
						.set("upic", senderInfoRecord.getStr("upic"))
						.set("type", 1).set("iggroupid", groupid);
				code = 0;
				message = "邀请与申请进组消息";
				break;
			case "applyIntoGroup":
				transmissionRecord.set("igid", newID).set("igstatus", 0)
						.set("unickname", senderInfoRecord.getStr("unickname"))
						.set("igcontent", content).set("igtime", time)
						.set("upic", senderInfoRecord.getStr("upic"))
						.set("type", 2).set("iggroupid", groupid);
				code = 0;
				message = "邀请与申请进组消息";
				break;
			case "comment":
				pushContent = senderInfoRecord.getStr("unickname") + ":"
						+ content;
				transmissionRecord
						.set("mid", newID)
						.set("mstatus", 0)
						.set("unickname", senderInfoRecord.getStr("unickname"))
						.set("mcontent", content)
						.set("mtime", time)
						.set("upic", senderInfoRecord.getStr("upic"))
						.set("type", 0)
						.set("efirstpic",
								(otherRecord.get("efirstpic") == null ? CommonParam.yinianLogo
										: otherRecord.get("efirstpic")
												.toString()))
						.set("meid", otherRecord.get("eid").toString());
				code = 1;
				message = "评论回复消息";
				break;
			case "notification":
				// 获取组头像,组聊天ID
				groupPicture = Group.dao.findById(groupid).getStr("gpic");
				String gimid = Group.dao.findById(groupid).getStr("gimid");
				transmissionRecord.set("nid", newID).set("nstatus", 0)
						.set("unickname", senderInfoRecord.getStr("unickname"))
						.set("ncontent", content).set("ntime", time)
						.set("gpic", groupPicture).set("gimid", gimid);
				code = 2;
				message = "通知";
				break;
			case "agree":
				// 已成功进入组的通知
				// 获取组信息
				Record groupRecord = Db
						.findFirst("select groupid,gimid,gname,gpic,gnum,gtime,gtype,gcreator,ginvite from groups where groupid="
								+ groupid + "  ");
				// 获取进入者的昵称，用户IM界面中的通知
				String notifyName = Db
						.find("select unickname from users where userid="
								+ receiverID + " ").get(0).get("unickname")
						.toString();
				transmissionRecord
						.set("nid", newID)
						.set("nstatus", 0)
						.set("unickname", senderInfoRecord.getStr("unickname"))
						.set("ncontent", content)
						.set("ntime", time)
						.set("gpic", groupRecord.get("gpic").toString())
						.set("groupid", groupRecord.get("groupid").toString())
						.set("gname", groupRecord.get("gname").toString())
						.set("gnum", groupRecord.get("gnum").toString())
						.set("gtime", groupRecord.get("gtime").toString())
						.set("gtype", groupRecord.get("gtype").toString())
						.set("gcreator", groupRecord.get("gcreator").toString())
						.set("ginvite", groupRecord.get("ginvite").toString())
						.set("gimid", groupRecord.get("gimid").toString())
						.set("notifyName", notifyName);
				pushContent = content;
				code = 2;
				message = "同意进组";
				break;
			case "mark":
				// 最美时光推送
				pushContent = content;
				transmissionRecord = otherRecord;
				code = 3;
				message = "时光印记";
				break;

			}
			List<Record> list = new ArrayList<Record>();
			list.add(transmissionRecord);
			// 将透传数据转为json字符串
			String transmissionContent = jsonData.getJson(code, message, list);
			// 将内容加入Record中
			pushRecord.set("pushContent", pushContent)
					.set("transmissionContent", transmissionContent)
					.set("receiverID", receiverID);
			// 返回结果
			return pushRecord;
		} else {
			// 返回空Record
			Record record = new Record();
			return record;
		}
	}
}
