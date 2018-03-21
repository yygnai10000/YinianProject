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

	private JsonData jsonData = new JsonData(); // json������
	private YinianDataProcess dataProcess = new YinianDataProcess();// ���ݴ�����
	private YinianGetuiPush push = new YinianGetuiPush(); // ����������
	private YinianDAO dao = new YinianDAO();
	private YinianUtils utils = new YinianUtils();

	public String PUSH_TYPE_OF_EVENT = "event";

	/**
	 * ������Ϣ���ռ��Ա
	 * 
	 * @param groupid
	 * @param sender
	 */
	public void pushToSpaceMember(String groupid, String sender, String type) {

		// ��ȡ���ڳ��������������û���ID
		GroupMember gm = new GroupMember();
		List<Record> useridList = gm.GetAllUseridInTheSpaceExceptCurrentUser(
				groupid, sender);

		// useridList��Ϊnullʱ�Ž�������
		if (!(useridList.isEmpty())) {
			// ��useridList�е��û�IDƴ�ӳ��ַ���
			String userIDs = dataProcess.changeListToString(useridList,
					"gmuserid");

			// userids��Ϊ���ַ���ʱ�Ž�������
			if (!(userIDs.equals(""))) {

				// ��ȡҪ�������͵��û���cid,��Android��iOS����
				Record cid = getUsersCids(userIDs, "userid");
				// ��ȡ��������
				String pushContent = getPushContent(groupid, sender, type);
				// ��ȡ͸������
				String transmissionContent = getTransmissionContent(groupid,
						sender, pushContent, type);

				// �����������ݶ���
				Record data = new Record().set("content", pushContent).set(
						"transmissionContent", transmissionContent);
				// ����
				push.yinianPushToList(cid, data);
			}
		}
	}
	
	/**
	 * ������Ϣ�������û�
	 * @param sender
	 * @param receiver
	 * @param type
	 */
	public void pushToSingleUser(String sender,String receiver,String type){
		
	}

	/**
	 * ��ȡ��������
	 * 
	 * @param groupid
	 * @param sender
	 * @param type
	 * @return
	 */
	public String getPushContent(String groupid, String sender, String type) {

		String content = "";

		// ��ȡ��Ϣ�������ǳ�
		User user = new User().findById(sender);
		String nickname = user.getStr("unickname");

		// ��ȡ�ռ�����
		Group group = new Group().findById(groupid);
		String gname = group.getStr("gname");

		switch (type) {
		case "event":
			content = nickname + "��Ȧ�ӡ�" + gname + "���з������¶�̬";
			break;
		default:
			break;
		}

		return content;
	}

	/**
	 * ��ȡ��������
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
			transmissionContent = jsonData.getJson(3, "����̬", list);
			break;
		default:
			break;
		}

		return transmissionContent;
	}

	/**
	 * ͨ���û��ı�ʶ����ȡ�û���cid һ�����ֻ�����phone�����û�id����userid
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
	 * �����豸���cid
	 * 
	 * @param list
	 * @return
	 */
	public Record disposeCidsByDevice(List<Record> list) {
		
		String AndroidCids = ""; // ���Android��cid
		String iOSCids = ""; // ���iOS��cid
		
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
		
		// �ַ�������
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
	 * ��ȡ���͵�Record
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
		// ������
		int code = 0;
		// ������Ϣ
		String message = "success";
		// ��ͷ��
		String groupPicture = "";
		// ��ȡ�����ߵ�ucid��udevice
		Record pushRecord = dao.queryUserInfo(receiverID, "ucid,udevice");
		// ucid��udevice������Ϊnull���ܽ�������
		if (pushRecord.get("udevice") != null && pushRecord.get("ucid") != null
				&& !((pushRecord.get("ucid").toString()).equals(""))) {
			// ��ȡ�����ߵ���Ϣ
			Record senderInfoRecord = dao.queryUserInfo(senderID,
					"unickname,upic");
			// ������������
			String pushContent = senderInfoRecord.getStr("unickname") + content;

			// ����͸������
			String time = utils.getTimeNow();// ��ȡϵͳʱ��

			int newID = Integer.parseInt(id);// ��idת������

			// ƴ��͸��Record
			Record transmissionRecord = new Record();
			switch (type) {
			case "inviteGroup":
				transmissionRecord.set("igid", newID).set("igstatus", 0)
						.set("unickname", senderInfoRecord.getStr("unickname"))
						.set("igcontent", content).set("igtime", time)
						.set("upic", senderInfoRecord.getStr("upic"))
						.set("type", 1).set("iggroupid", groupid);
				code = 0;
				message = "���������������Ϣ";
				break;
			case "applyIntoGroup":
				transmissionRecord.set("igid", newID).set("igstatus", 0)
						.set("unickname", senderInfoRecord.getStr("unickname"))
						.set("igcontent", content).set("igtime", time)
						.set("upic", senderInfoRecord.getStr("upic"))
						.set("type", 2).set("iggroupid", groupid);
				code = 0;
				message = "���������������Ϣ";
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
				message = "���ۻظ���Ϣ";
				break;
			case "notification":
				// ��ȡ��ͷ��,������ID
				groupPicture = Group.dao.findById(groupid).getStr("gpic");
				String gimid = Group.dao.findById(groupid).getStr("gimid");
				transmissionRecord.set("nid", newID).set("nstatus", 0)
						.set("unickname", senderInfoRecord.getStr("unickname"))
						.set("ncontent", content).set("ntime", time)
						.set("gpic", groupPicture).set("gimid", gimid);
				code = 2;
				message = "֪ͨ";
				break;
			case "agree":
				// �ѳɹ��������֪ͨ
				// ��ȡ����Ϣ
				Record groupRecord = Db
						.findFirst("select groupid,gimid,gname,gpic,gnum,gtime,gtype,gcreator,ginvite from groups where groupid="
								+ groupid + "  ");
				// ��ȡ�����ߵ��ǳƣ��û�IM�����е�֪ͨ
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
				message = "ͬ�����";
				break;
			case "mark":
				// ����ʱ������
				pushContent = content;
				transmissionRecord = otherRecord;
				code = 3;
				message = "ʱ��ӡ��";
				break;

			}
			List<Record> list = new ArrayList<Record>();
			list.add(transmissionRecord);
			// ��͸������תΪjson�ַ���
			String transmissionContent = jsonData.getJson(code, message, list);
			// �����ݼ���Record��
			pushRecord.set("pushContent", pushContent)
					.set("transmissionContent", transmissionContent)
					.set("receiverID", receiverID);
			// ���ؽ��
			return pushRecord;
		} else {
			// ���ؿ�Record
			Record record = new Record();
			return record;
		}
	}
}
