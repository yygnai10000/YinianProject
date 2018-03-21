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

	private String jsonString;// ���ؽ��
	private JsonData jsonData = new JsonData(); // json������
	private YinianDataProcess dataProcess = new YinianDataProcess();// ���ݴ�����
	private YinianGetuiPush push = new YinianGetuiPush(); // ����������
	private YinianDAO dao = new YinianDAO();
	// private IMService im = new IMService();
	private QiniuOperate qiniu = new QiniuOperate();
	private PushMessage pushMessage = new PushMessage();
	private EventService eventService = new EventService();

	@Before(Tx.class)
	public void Test(String userid) {
		// �����ռ�
		String inviteCode = Group.CreateSpaceInviteCode();
		String groupid = createAlbum("˽�˿ռ�", userid, "http://7xlmtr.com1.z0.glb.clouddn.com/defaultCoverOfSpace.png",
				"4", inviteCode, "app");
		System.out.println("��ᴴ���ɹ�");
		// ��ȡ���ж�̬
		List<Record> eventList = Db.find("select backupEventID,backupDate from backupevent where backupUserID=" + userid
				+ " and backupStatus=0 order by backupDate asc ");
		int size = eventList.size();
		System.out.println("��ʼ����" + userid + "��̬������" + size + "��");
		for (int i = 0; i < size; i++) {
			System.out.println("���ڱ��ݵ�" + i + "����̬");
			String eventID = eventList.get(i).get("backupEventID").toString();
			String backupDate = eventList.get(i).get("backupDate").toString() + " 00:00:00";
			// ��ȡ��̬�ڵ�����ͼƬ
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
					System.out.println("��" + i + "����̬���ɳɹ�");
				}
			} else {
				System.out.println("��" + i + "����̬����ʧ��");
			}

			System.out.println("�������ݵ�" + i + "����̬");
		}

	}

	/**
	 * ��½
	 * 
	 * @param username
	 * @param password
	 * @return String
	 */
	public String login(String username, String password, String source) {
		// ͨ���û������������������Ϣ
		List<Record> list = User.QueryUserLoginBasicInfo(username, "uphone");
		if (list.size() == 0) {
			// �鵽������Ϊ��
			jsonString = jsonData.getJson(1000, "��¼��Ϣ����");
		} else {
			if (YinianUtils.EncoderByMd5(password).equals((list.get(0)).get("upass"))) {
				List<Record> userInfo = new ArrayList<Record>();
				Record record = new Record();
				String userid = list.get(0).get("userid").toString();
				// ��ӻ��ŵ�¼����
				String hxPassword = userid + (CommonParam.APP_USER_PASSWORD_SUFFIX);
				try {
					// �������DES����
					hxPassword = DES.encryptDES(hxPassword, "YZadZjYx");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				record.set("userid", userid).set("password", hxPassword);
				// �ж��û��Ƿ��΢��
				if (list.get(0).get("uwechatid") == null) {
					record.set("isBindWechat", 0);
				} else {
					record.set("isBindWechat", 1);
				}
				userInfo.add(record);
				if (updateLastLoginInfo(list.get(0).get("userid").toString(), null)) {
					// �޸��û�����¼ʱ��
					jsonString = jsonData.getJson(0, "success", userInfo);
				} else {
					jsonString = jsonData.getJson(-51, "�������ݿ�����ʧ��");
				}
			} else {
				jsonString = jsonData.getJson(1001, "��¼��Ϣ����");
			}
		}
		return jsonString;
	}

	/**
	 * ��֤�ֻ���
	 * 
	 * @param phonenumber
	 * @return boolean
	 */
	public boolean verifyPhone(String phonenumber) {
		List<Record> list = Db.find("select * from users where uphone='" + phonenumber + "'");// �����ֻ�����
		if (list.size() == 0) {
			// ������Ϊfalse
			return false;
		} else {
			// ����Ϊtrue
			return true;
		}
	}

	/**
	 * ������֤����
	 * 
	 * @param phonenumber
	 * @return
	 */
	public String verifyMessage(String phonenumber) {
		String feedback = "";// �������ط��ؽ��
		String content = "�����꡿ע����֤�룺"; // ��������
		List<Record> list = new ArrayList<Record>();
		// ���������λ������Ϊ��֤��
		int verifyCode = (int) (Math.random() * 9000 + 1000);
		SendMessage sm = new SendMessage();
		content += verifyCode + "������֤��ֻ���ڵ�¼�����ꡱAPP������ת�����ˡ�";
		// ������֤�벢��ȡ���ؽ��
		try {
			feedback = sm.send(phonenumber, content);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if ((feedback.substring(0, 5)).equals("error")) {
			jsonString = jsonData.getJson(1003, "������֤�뷢��ʧ��");
		} else {
			Record record = new Record();
			record.set("verifyCode", verifyCode);
			list.add(record);
			jsonString = jsonData.getJson(0, "success", list);
		}
		return jsonString;
	}

	/**
	 * ע��(����)
	 * 
	 * @param username
	 * @param password
	 * @return
	 */
	public String register(String username, String password) {
		String encoder = YinianUtils.EncoderByMd5(password);// �������
		SimpleDateFormat birthDf = new SimpleDateFormat("yyyy-MM-dd");// �������ڸ�ʽ
		String birth = birthDf.format(new Date()); // new
													// Date()Ϊ��ȡ��ǰϵͳʱ�䣬���õ�ǰ����Ϊ����

		int sex = 0;// Ĭ���Ա�ΪŮ
		// ����8λ����뵱���û��ǳ�
		String base = "abcdefghijklmnopqrstuvwxyz0123456789";
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < 8; i++) {
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		String nickname = sb.toString();// nicknameΪ������ɵ��ǳ�
		String defaultHead = CommonParam.qiniuOpenAddress + CommonParam.userDefaultHeadPic;// Ĭ��ͷ��ĵ�ַ
		String defaultBackground = CommonParam.qiniuOpenAddress + CommonParam.userDefaultBackground;// Ĭ�ϱ����ĵ�ַ

		User user = new User().set("uphone", username).set("upass", encoder).set("usex", sex).set("unickname", nickname)
				.set("ubirth", birth).set("upic", defaultHead).set("ubackground", defaultBackground);

		// �û�ID��ע��ɹ�������Ӧ��ֵ��ע��ʧ��Ϊ���ַ���
		String userid = "";
		if (user.save()) {
			userid = user.get("userid").toString();
		}
		return userid;
	}

	/**
	 * ע�ᣨ��д������Ϣ��
	 * 
	 * @param username
	 * @param password
	 * @param nickname
	 * @param sex
	 * @param birthday
	 * @return
	 */
	public String register(String username, String password, String nickname, String sex, String birthday) {
		// ���ܺ������
		String encoder = YinianUtils.EncoderByMd5(password);
		// �Ա�
		int newSex;
		if (sex.equals("Ů")) {
			newSex = 0;
		} else {
			newSex = 1;
		}
		String defaultHead = CommonParam.qiniuOpenAddress + CommonParam.userDefaultHeadPic;// Ĭ��ͷ��ĵ�ַ
		String defaultBackground = CommonParam.qiniuOpenAddress + CommonParam.userDefaultBackground;// Ĭ�ϱ����ĵ�ַ

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
	 * ��������
	 * 
	 * @param phonenumber
	 * @param password
	 * @return
	 */
	public String resetPassword(String phonenumber, String password) {
		// ���������
		String encoder = YinianUtils.EncoderByMd5(password);
		int result = Db.update("update users set upass=? where uphone=?", encoder, phonenumber);
		if (result == 1) {
			String Idsql = "select userid from users where uphone='" + phonenumber + "'";
			List<Record> list = Db.find(Idsql);
			jsonString = jsonData.getJson(0, "success", list);
		} else {
			jsonString = jsonData.getJson(-51, "�������ݿ�����ʧ��");
		}
		return jsonString;
	}

	/**
	 * ��ʾ���б�
	 * 
	 * @param userid 
	 * @return
	 */
	public String showGroup(int userid) {
		// �ж�����б��Ƿ��������δ������ʱ��˳������
		// List<Record> judgeList = Db.find("select gmorder from groups,groupmembers
		// where groupid=gmgroupid and gmuserid="
		// + userid + " and gstatus=0 and gmstatus=0 and gtype not in(5,12)");//
		// �ų��ٷ�����С���ͻ���
		// if (judgeList.size() != 0) {
		// Record judgeRecord = judgeList.get(0);
		// boolean judgeFlag = true;
		// if ((judgeRecord.get("gmorder").toString()).equals("0")) {
		// judgeFlag = false;
		// }
		// // �����жϽ����ȡ����Ϣ
		// List<Record> list = new ArrayList<Record>();
		// if (judgeFlag) {
		//
		// list = Group.GetAlreadyOrderSpaceBasicInfo(userid);
		// } else {
		//
		// list = Group.GetUnOrderSpaceBasicInfo(userid);
		//
		// // �����¼��������ö�
		// int size = list.size();
		// if (size != 0 && size != 1) {
		// // �ҳ����¼���ʱ��
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
		// // ��װ�ռ�����
		// list = dataProcess.spaceDataEncapsulation(list, userid);
		//
		// jsonString = jsonData.getJson(0, "success", list);
		// } else {
		// jsonString = jsonData.getJson(0, "success", judgeList);
		// }

		List<Record> list = Group.GetUnOrderSpaceBasicInfo(userid);
		// ��װ�ռ�����
		list = dataProcess.spaceDataEncapsulation(list, userid);
		jsonString = jsonData.getJson(0, "success", list);
		return jsonString;
	}

	/**
	 * �����ռ�
	 * 
	 * @param spaceName
	 * @return
	 */
	public List<Record> searchSpace(String spaceName) {
		List<Record> list = Group.GetSearchSpaceBasicInfo(spaceName);

		// ��װ�ռ�����
		list = dataProcess.spaceDataEncapsulation(list, 0);
		return list;
	}

	/**
	 * ��ȡ���е绰���룬�ӻ���!!!!!!!!!!!!!!!
	 * 
	 * @return
	 */
	public List<Record> getAllPhone() {
		List<Record> list = Db.find("select uphone from users where uphone is not null");
		return list;
	}

	/**
	 * ��ȡ���еȴ��ߵĵ绰���룬�ӻ���!!!!!!!!!!!!!!!
	 * 
	 * @return
	 */
	public List<Record> getAllWaitPhone() {
		List<Record> list = Db.find("select wsender,wphone from waits");
		return list;
	}

	/**
	 * ��ȡ���ŷ������ݵ������Ϣ
	 * 
	 * @param groupid
	 * @param userid
	 * @return
	 */
	public Record getMessageInfo(String groupid, String userid) {
		List<Record> list = Db.find("select gname,gtype,ginvite from groups where groupid='" + groupid + "'");
		List<Record> list2 = Db.find("select unickname from users where userid='" + userid + "' ");
		String groupName = list.get(0).get("gname").toString();// ��ȡ�������������Ϣʱʹ��
		String userName = list2.get(0).get("unickname").toString();// ��ȡ�������ǳƣ�������Ϣʱʹ��
		String groupType = list.get(0).get("gtype").toString(); // ��ȡ�������
		String ginvite = list.get(0).get("ginvite").toString();
		String newType = "";
		switch (groupType) {
		case "0":
			newType += "�������";
			break;
		case "1":
			newType += "�������";
			break;
		case "2":
			newType += "�������";
			break;
		case "3":
			newType += "�������";
			break;
		case "4":
			newType += "�������";
		}
		Record messageInfo = new Record();
		messageInfo.set("userName", userName).set("type", newType).set("groupName", groupName).set("ginvite", ginvite);
		// String content = "�������" + userName + "���������" + newType + "��"
		// + groupName + "����һ���ڡ����꡿���湹���������ϵļ�"; // ��������
		return messageInfo;
	}

	/**
	 * �����������
	 * 
	 * @param content
	 * @param phonenumber
	 * @return
	 */
	public boolean sendInviteMessage(String content, String messagePhone) {
		String feedback = "";// �������ط��ؽ��
		// ������֤�벢��ȡ���ؽ��
		SendMessage sm = new SendMessage();
		boolean flag;
		try {
			feedback = sm.send(messagePhone, content);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// jsonString = jsonData.getJson(1007, "���������뷢��ʧ��");
			flag = false;
		}

		flag = (feedback.substring(0, 5)).equals("error") ? false : true;

		return flag;
	}

	/**
	 * �������
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
			// �������ݵ�groupmembers����
			boolean insertFlag = insertIntoGroupMembers(userid, groupid);
			if (insertFlag) {
				if (!(groupType.equals("5"))) {
					// ������������Ⱥ��
					// String gimid = im
					// .CreatChatGroup(groupid, groupName, userid);
					// groupid���ܣ�����web��
					String encodeGroupid = "";
					try {
						encodeGroupid = DES.encryptDES("groupid=" + groupid, CommonParam.DESSecretKey);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// group.set("gimid", gimid);
					// boolean updateFlag = group.update();
					List<Record> list = getSingleAlbum(groupid);// ��ȡ�����������Ϣ
					list.get(0).set("encodeGroupid", encodeGroupid);
					list = dataProcess.changeGroupTypeIntoWord(list);// ת��������Ϊ����

					jsonString = jsonData.getSuccessJson(list);

				} else {
					List<Record> list = getSingleAlbum(groupid);// ��ȡ�����������Ϣ
					list = dataProcess.changeGroupTypeIntoWord(list);// ת��������Ϊ����
					jsonString = jsonData.getJson(0, "success", list);
				}

			} else {
				jsonString = jsonData.getJson(-50, "��������ʧ��");
			}
		} else {
			jsonString = jsonData.getJson(-50, "��������ʧ��");
		}
		return jsonString;
	}
	

	/**
	 * ����Ĭ�����
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
		case "0":// ������
			url += CommonParam.qiniuOpenAddress + CommonParam.familyGroup;
			gname += "�������";
			break;
		case "1":// ������
			url += CommonParam.qiniuOpenAddress + CommonParam.bestieGroup;
			gname += "�������";
			break;
		case "2":// ������
			url += CommonParam.qiniuOpenAddress + CommonParam.friendGrop;
			gname += "�������";
			break;
		case "3":// ������
			url += CommonParam.qiniuOpenAddress + CommonParam.coupleGroup;
			gname += "�������";
			break;
		case "4":// ����
			url += CommonParam.qiniuOpenAddress + CommonParam.otherGroup;
			gname += "�������";
			break;
		case "6":// ϵͳ����������
			url += CommonParam.qiniuOpenAddress + CommonParam.familySpaceCover;
			gname += "����һ����";
			break;
		case "7":// ϵͳ����������
			url += CommonParam.qiniuOpenAddress + CommonParam.loverSpaceCover;
			gname += "���¿ռ�";
			break;
		case "8":// ϵͳ����������
			url += CommonParam.qiniuOpenAddress + CommonParam.friendSpaceCover;
			gname += "���Ѿۻ�";
			break;
		case "9":// ����С����ϵͳ����������
			url += CommonParam.qiniuOpenAddress + CommonParam.simH5familySpaceCover;
			gname += "����һ����";
			break;
		case "10":// ����С����ϵͳ����������
			url += CommonParam.qiniuOpenAddress + CommonParam.simH5loverSpaceCover;
			gname += "���¿ռ�";
			break;
		case "11":// ����С����ϵͳ����������
			url += CommonParam.qiniuOpenAddress + CommonParam.simH5friendSpaceCover;
			gname += "���Ѿۻ�";
			break;
		}

		// ��ȡ������
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
			// // ���������鲢�������û����뵽��������
			// String gimid = im.CreatChatGroup(groupid, gname, userid);
			// group.set("gimid", gimid);
			// boolean updateFlag = group.update();

			// �������ݵ�groupmembers����
			boolean insertFlag = insertIntoGroupMembers(userid, groupid);
			return insertFlag;
		} else {
			return false;
		}
	}
	/**
	 * ����Ĭ����Ტ����Ĭ�϶�̬
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
		case "0":// ������
			url += CommonParam.qiniuOpenAddress + CommonParam.familyGroup;
			gname += "�������";
			break;
		case "1":// ������
			url += CommonParam.qiniuOpenAddress + CommonParam.bestieGroup;
			gname += "�������";
			break;
		case "2":// ������
			url += CommonParam.qiniuOpenAddress + CommonParam.friendGrop;
			gname += "�������";
			break;
		case "3":// ������
			url += CommonParam.qiniuOpenAddress + CommonParam.coupleGroup;
			gname += "�������";
			break;
		case "4":// ����
			url += CommonParam.qiniuOpenAddress + CommonParam.otherGroup;
			gname += "�������";
			break;
		case "6":// ϵͳ����������
			url += CommonParam.qiniuOpenAddress + CommonParam.familySpaceCover;
			gname += "����һ����";
			break;
		case "7":// ϵͳ����������
			url += CommonParam.qiniuOpenAddress + CommonParam.loverSpaceCover;
			gname += "���¿ռ�";
			break;
		case "8":// ϵͳ����������
			url += CommonParam.qiniuOpenAddress + CommonParam.friendSpaceCover;
			gname += "���Ѿۻ�";
			break;
		case "9":// ����С����ϵͳ����������
			url += CommonParam.qiniuOpenAddress + CommonParam.simH5familySpaceCover;
			gname += "����һ����";
			break;
		case "10":// ����С����ϵͳ����������
			url += CommonParam.qiniuOpenAddress + CommonParam.simH5loverSpaceCover;
			gname += "���¿ռ�";
			break;
		}

		// ��ȡ������
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
			//����Ĭ�϶�̬
			//YinianService TxService = enhance(YinianService.class);
			Calendar c = Calendar.getInstance();//���Զ�ÿ��ʱ���򵥶��޸�
			int year = c.get(Calendar.YEAR); 
			int month = c.get(Calendar.MONTH)+1; 
			int date = c.get(Calendar.DATE); 
			//by lk �Ȳ�������̬ 20171108
			String picAddress="http://7xpend.com1.z0.glb.clouddn.com/tmp_1840006904o6zAJs7TrsuV9RMorlB_3dksq1YE2e0ff03bac96b62f6df9266a5504314b.png,http://7xpend.com1.z0.glb.clouddn.com/tmp_1840006904o6zAJs7TrsuV9RMorlB_3dksq1YE0dc3d9fa7b52e5bf13c3d6a652f36070.png";
			String eventID = uploadDefault(userid, groupid, picAddress, "���깲����ᣬ�������ɹ�������˲��...", "600", year+"-"+month+"-"+date, "", "",
					"С����");// �ϴ���̬����ȡ��̬��ID
			//lk end 
			// // ���������鲢�������û����뵽��������
			// String gimid = im.CreatChatGroup(groupid, gname, userid);
			// group.set("gimid", gimid);
			// boolean updateFlag = group.update();

			// �������ݵ�groupmembers����
			boolean insertFlag = insertIntoGroupMembers(userid, groupid);
			return insertFlag;
		} else {
			return false;
		}
	}
	/**
	 * ����һЩ���
	 * 
	 * @param userid
	 * @param gtype
	 * @return
	 */
	public boolean creatSomeAlbum(String userid, String gtype) {
		String url = "";
		String gname = "";
		switch (gtype) {
		case "0":// ������
			url += CommonParam.qiniuOpenAddress + CommonParam.familyGroup;
			gname += "�������";
			break;
		case "1":// ������
			url += CommonParam.qiniuOpenAddress + CommonParam.bestieGroup;
			gname += "�������";
			break;
		case "2":// ������
			url += CommonParam.qiniuOpenAddress + CommonParam.friendGrop;
			gname += "�������";
			break;
		case "3":// ������
			url += CommonParam.qiniuOpenAddress + CommonParam.coupleGroup;
			gname += "�������";
			break;
		case "4":// ����
			url += CommonParam.qiniuOpenAddress + CommonParam.otherGroup;
			gname += "�������";
			break;
		}
		// ����8λ����뵱�����������
		String base = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < 6; i++) {
			int number = random.nextInt(base.length());
			sb.append(base.charAt(number));
		}
		String inviteCode = sb.toString();// inviteCodeΪ������ɵ�������
		Group group = new Group().set("gname", gname).set("gcreator", userid).set("gpic", url).set("gtype", gtype)
				.set("gnum", 1).set("ginvite", inviteCode).set("gOrigin", 1);
		if (group.save()) {
			String groupid = group.get("groupid").toString();
			// �������ݵ�groupmembers����
			boolean insertFlag = insertIntoGroupMembers(userid, groupid);
			return insertFlag;
		} else {
			return false;
		}
	}

	/**
	 * ��ȡ���������Ϣ
	 * 
	 * @param groupid
	 * @return
	 */
	public List<Record> getSingleAlbum(String groupid) {
		String sql = "select groupid,gimid,gname,gpic,gnum,gtime,gtype,gcreator,ginvite from groups where groupid ='"
				+ groupid + "' ";
		List<Record> list = Db.find(sql);
		// �մ������ʱ��ֱ�Ӱ������Ƭ������Ϊ0
		list.get(0).set("gpicnum", 0);
		return list;
	}

	/**
	 * ���������Ϣ
	 * 
	 * @param groupid
	 * @return
	 */
	public String getGroupContent(String userid, String groupid, String minID) {
		// ���۲�ѯ���
		String sqlForComment = CommonParam.selectForComment
				+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and egroupid="
				+ groupid + " and cstatus=0 ORDER BY ceid,ctime asc";
		List<Record> event;
		if (minID.equals("0")) {
			// minID==0�����ȡ������Ϣ�ĳ�ʼ����Ϣ
			// ��ȡ�ö������ж�̬
			List<Record> topEvents = Db.find(CommonParam.selectForEvent
					+ " from users,events,pictures where userid=euserid and eid=peid and egroupid=" + groupid
					+ " and estatus=0 and pstatus=0 and elevel=1 and eMain!=5 group by peid DESC order by eTopTime asc ");
			int size = topEvents.size();

			event = Db.find(CommonParam.selectForEvent
					+ " from users,events,pictures where userid=euserid and eid=peid and egroupid=" + groupid
					+ " and estatus=0 and pstatus=0 and elevel=0 and eMain!=5 group by peid DESC  limit " + (10 - size)
					+ "");
			// ��ʼ����ʱ��������״̬Ϊ���¶�̬
			Db.update(
					"update groupmembers set gmnotify=0 where gmgroupid=" + groupid + " and gmuserid=" + userid + " ");
			topEvents.addAll(event);
			event = topEvents;
		} else {
			// minID������0���ѯС�ڸ�ID��ǰʮ������
			event = Db.find(CommonParam.selectForEvent
					+ " from users,events,pictures where userid=euserid and eid=peid and egroupid=" + groupid
					+ " and eid < " + minID
					+ " and estatus=0 and pstatus=0 and elevel=0 and eMain!=5 group by peid DESC limit 10");
		}
		event = dataProcess.encapsulationEventList(event);// ��ȡ�¼��������Ϣ����װ������û�����
		List<Record> commentList = new ArrayList<Record>();
		if (!groupid.equals("1065266")) {
			commentList = Db.find(sqlForComment);
		}

		List<Record> comment = dataProcess.encapsulationCommentList(commentList);// ��ȡ�¼���������Ϣ����װ������û�����
		List<Record> list = dataProcess.combieEventAndComment(event, comment);// ƴ���¼�������
		// ��װ�¼��͵���
		list = dataProcess.combineEventWithLike(list, "smallApp");
		// ��װ�¼��ͱ�ǩ
		list = dataProcess.combineEventWithTags(list);
		// ��װ�¼��Ϳ�Ƭ��ʽ
		list = dataProcess.combineEventWithCardStyle(list);
		// �޸�upic�ֶθ�ʽ
		list = dataProcess.ChangePicAsArrayDirectCutVersion(list);

		jsonString = jsonData.getSuccessJson(list);
		return jsonString;
	}

	/**
	 * ��ȡ��Ա�б�
	 * 
	 * @param groupid
	 * @return
	 */
	public String getMemberList(String userid, String groupid) {

		String sql = "select userid,unickname,upic,gmtime,gname from users,groups,groupmembers where groupid=gmgroupid and users.userid=groupmembers.gmuserid and gmgroupid='"
				+ groupid + "' limit 100 ";
		List<Record> list = Db.find(sql);

		// �����¾ɰ汾�����ӱ�ע���ֶ�
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
	 * ��ȡ�û�ͷ��Ϣ
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
	 * ��������
	 * 
	 * @param commentUserId
	 * @param commentedUserId
	 * @param eventId
	 * @param content
	 * @return
	 */
	@Before(Tx.class)
	public boolean sendComment(String commentUserId, String commentedUserId, String eventId, String content) {

		// ����list
		List<Record> pushList = new ArrayList<Record>();

		// ���������Ƿ�ɹ��жϱ���
		boolean commentFlag;
		// ������Ϣ�Ƿ�ɹ��жϱ���
		boolean messageFlag = true;

		// �������۶��󲢴洢������Ϣ
		Comment comment = new Comment().set("ceid", eventId).set("cuserid", commentUserId)
				.set("ceduserid", commentedUserId).set("ccontent", content);
		// ��������
		if (comment.save()) {
			commentFlag = true;
		} else {
			commentFlag = false;
		}
		// ��ȡ��̬�����ߵ�ID
		Record PublisherID = Db.findFirst("select euserid from events where eid=" + eventId + " ");
		// �洢��Ҫ������Ϣ���û�ID�ļ���
		Set<Integer> useridSet = new HashSet<Integer>();
		// ���뷢���ߵ���Ϣ
		useridSet.add(Integer.parseInt(PublisherID.get("euserid").toString()));
		// �жϱ������˵�ID�����ΪϵͳID��������Ϣ�����������Ա
		if (commentedUserId.equals(CommonParam.systemUserID)) {
			// ��ȡ�����û�ID
			List<Record> list = Db.find("select distinct cuserid from comments where ceid=" + eventId + " ");
			// ����������Set��
			for (Record record : list) {
				useridSet.add(Integer.parseInt(record.get("cuserid").toString()));
			}
			// ȥ�������˵�ID
			useridSet.remove(Integer.parseInt(commentUserId));
			// ����������Ϣ
			Iterator<Integer> it = useridSet.iterator();
			while (it.hasNext()) {
				int mreceiver = it.next();
				// ������Ϣ����
				Message message = new Message().set("msender", Integer.parseInt(commentUserId))
						.set("mreceiver", mreceiver).set("meid", Integer.parseInt(eventId)).set("mcontent", content);
				// ��������
				if (message.save()) {
					messageFlag = true;
					// ������Ϣ�洢�ɹ���ͬʱ,����͸�����ݲ���������
					String mid = message.get("mid").toString();
					// ��ȡ��̬�ĵ�һ��ͼ
					Record eventRecord = Db.findFirst("select eid,efirstpic from events where eid=" + eventId + " ");
					Record pushRecord = pushMessage.getPushRecord(commentUserId, commentedUserId, "", content, mid,
							"comment", eventRecord);
					if (!((pushRecord.toJson()).equals("{}"))) {
						// Record��Ϊ��ʱ�����뵽list��
						pushList.add(pushRecord);
					}
				} else {
					messageFlag = false;
					break;
				}
			}
		} else {
			// ��ӱ������˵�ID
			useridSet.add(Integer.parseInt(commentedUserId));
			// ��ȥ�����˵�ID
			useridSet.remove(Integer.parseInt(commentUserId));
			// ����������Ϣ
			Iterator<Integer> it = useridSet.iterator();
			while (it.hasNext()) {
				int mreceiver = it.next();
				// ������Ϣ����
				Message message = new Message().set("msender", Integer.parseInt(commentUserId))
						.set("mreceiver", mreceiver).set("meid", Integer.parseInt(eventId)).set("mcontent", content);
				if (message.save()) {
					messageFlag = true;
					// ������Ϣ�洢�ɹ���ͬʱ,����͸�����ݲ���������
					String mid = message.get("mid").toString();
					// ��ȡ��̬�ĵ�һ��ͼ
					Record eventRecord = Db.findFirst("select eid,efirstpic from events where eid=" + eventId + " ");
					Record pushRecord = pushMessage.getPushRecord(commentUserId, commentedUserId, "", content, mid,
							"comment", eventRecord);
					if (!((pushRecord.toJson()).equals("{}"))) {
						// Record��Ϊ��ʱ�����뵽list��
						pushList.add(pushRecord);
					}
				} else {
					messageFlag = false;
					break;
				}
			}
		}

		if (commentFlag && messageFlag) {
			// �������Ͳ�����true
			push.yinianPushToSingle(pushList);
			return true;
		} else {
			return false;
		}

	}

	/**
	 * ��������1 1.1�汾 ��������cid�ֶ�
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

		// ����list
		List<Record> pushList = new ArrayList<Record>();

		// ���������Ƿ�ɹ��жϱ���
		boolean commentFlag;
		// ������Ϣ�Ƿ�ɹ��жϱ���
		boolean messageFlag = true;

		// �������۶��󲢴洢������Ϣ
		Comment comment = new Comment().set("ceid", eventId).set("cuserid", commentUserId)
				.set("ceduserid", commentedUserId).set("ccontent", content).set("cplace", place);
		// ��������
		if (comment.save()) {
			commentFlag = true;
		} else {
			commentFlag = false;
		}
		// ��ȡ��������۵�ID
		String cid = comment.get("cid").toString();
		// ��ȡ��̬�����ߵ�ID
		Record PublisherID = Db.findFirst("select euserid from events where eid=" + eventId + " ");
		// �洢��Ҫ������Ϣ���û�ID�ļ���
		Set<Integer> useridSet = new HashSet<Integer>();
		// ���뷢���ߵ���Ϣ
		useridSet.add(Integer.parseInt(PublisherID.get("euserid").toString()));
		// �жϱ������˵�ID�����ΪϵͳID��������Ϣ�����������Ա
		if (commentedUserId.equals(CommonParam.systemUserID)) {
			// ��ȡ�����û�ID
			List<Record> list = Db.find("select distinct cuserid from comments where ceid=" + eventId + " ");
			// ����������Set��
			for (Record record : list) {
				useridSet.add(Integer.parseInt(record.get("cuserid").toString()));
			}
			// ȥ�������˵�ID
			useridSet.remove(Integer.parseInt(commentUserId));
			// ����������Ϣ
			Iterator<Integer> it = useridSet.iterator();
			while (it.hasNext()) {
				int mreceiver = it.next();
				// ������Ϣ����
				Message message = new Message().set("msender", Integer.parseInt(commentUserId))
						.set("mreceiver", mreceiver).set("meid", Integer.parseInt(eventId)).set("mcontent", content);
				// ��������
				if (message.save()) {
					messageFlag = true;
					// ������Ϣ�洢�ɹ���ͬʱ,����͸�����ݲ���������
					String mid = message.get("mid").toString();
					// ��ȡ��̬�ĵ�һ��ͼ
					Record eventRecord = Db.findFirst("select eid,efirstpic from events where eid=" + eventId + " ");
					Record pushRecord = pushMessage.getPushRecord(commentUserId, String.valueOf(mreceiver), "", content,
							mid, "comment", eventRecord);
					if (!((pushRecord.toJson()).equals("{}"))) {
						// Record��Ϊ��ʱ�����뵽list��
						pushList.add(pushRecord);
					}
				} else {
					messageFlag = false;
					break;
				}
			}
		} else {
			// ��ӱ������˵�ID
			useridSet.add(Integer.parseInt(commentedUserId));
			// ��ȥ�����˵�ID
			useridSet.remove(Integer.parseInt(commentUserId));
			// ����������Ϣ
			Iterator<Integer> it = useridSet.iterator();
			while (it.hasNext()) {
				int mreceiver = it.next();
				// ������Ϣ����
				Message message = new Message().set("msender", Integer.parseInt(commentUserId))
						.set("mreceiver", mreceiver).set("meid", Integer.parseInt(eventId)).set("mcontent", content);
				if (message.save()) {
					messageFlag = true;
					// ������Ϣ�洢�ɹ���ͬʱ,����͸�����ݲ���������
					String mid = message.get("mid").toString();
					// ��ȡ��̬�ĵ�һ��ͼ
					Record eventRecord = Db.findFirst("select eid,efirstpic from events where eid=" + eventId + " ");
					Record pushRecord = pushMessage.getPushRecord(commentUserId, commentedUserId, "", content, mid,
							"comment", eventRecord);
					if (!((pushRecord.toJson()).equals("{}"))) {
						// Record��Ϊ��ʱ�����뵽list��
						pushList.add(pushRecord);
					}
				} else {
					messageFlag = false;
					break;
				}
			}
		}
		if (commentFlag && messageFlag) {
			// �������Ͳ�����cid
			push.yinianPushToSingle(pushList);
			return cid;
		} else {
			return "";
		}
	}

	/**
	 * �ڹٷ�����ڷ������� 1.2�汾 2016.1.30 1������Ա�յ��������۶�̬������Ļظ�ʱ�������� 2����ͨ��Ա�յ������Ļظ�ʱ��������
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
		// ����list
		List<Record> pushList = new ArrayList<Record>();

		// ���������Ƿ�ɹ��жϱ���
		boolean commentFlag;
		// ������Ϣ�Ƿ�ɹ��жϱ���
		boolean messageFlag = true;

		// �������۶��󲢴洢������Ϣ
		Comment comment = new Comment().set("ceid", eventId).set("cuserid", commentUserId)
				.set("ceduserid", commentedUserId).set("ccontent", content);
		// ��������
		if (comment.save()) {
			commentFlag = true;
		} else {
			commentFlag = false;
		}
		// ��ȡ��������۵�ID
		String cid = comment.get("cid").toString();
		if (commentedUserId.equals(CommonParam.systemUserID)) {
			// �������ۣ�ֻ������������ʱ������Ա�յ���Ϣ������
			if (commentUserId.equals(CommonParam.superUserID)) {
				// ����Ա�Լ����ۣ������

			} else {
				// ��ͨ��Ա���ۣ�����Ա�յ���Ϣ������
				// ������Ϣ����
				Message message = new Message().set("msender", Integer.parseInt(commentUserId))
						.set("mreceiver", CommonParam.systemUserID).set("meid", Integer.parseInt(eventId))
						.set("mcontent", content);
				// ��������
				if (message.save()) {
					messageFlag = true;
					// ������Ϣ�洢�ɹ���ͬʱ,����͸�����ݲ���������
					String mid = message.get("mid").toString();
					// ��ȡ��̬�ĵ�һ��ͼ
					Record eventRecord = Db.findFirst("select eid,efirstpic from events where eid=" + eventId + " ");
					Record pushRecord = pushMessage.getPushRecord(commentUserId,
							String.valueOf(CommonParam.superUserID), "", content, mid, "comment", eventRecord);
					if (!((pushRecord.toJson()).equals("{}"))) {
						// Record��Ϊ��ʱ�����뵽list��
						pushList.add(pushRecord);
					}
				} else {
					messageFlag = false;
				}
			}
		} else {
			// ����ظ���ֻ�б��ظ����յ���Ϣ������
			// ������Ϣ����
			Message message = new Message().set("msender", Integer.parseInt(commentUserId))
					.set("mreceiver", Integer.parseInt(commentedUserId)).set("meid", Integer.parseInt(eventId))
					.set("mcontent", content);
			if (message.save()) {
				messageFlag = true;
				// ������Ϣ�洢�ɹ���ͬʱ,����͸�����ݲ���������
				String mid = message.get("mid").toString();
				// ��ȡ��̬�ĵ�һ��ͼ
				Record eventRecord = Db.findFirst("select eid,efirstpic from events where eid=" + eventId + " ");
				Record pushRecord = pushMessage.getPushRecord(commentUserId, commentedUserId, "", content, mid,
						"comment", eventRecord);
				if (!((pushRecord.toJson()).equals("{}"))) {
					// Record��Ϊ��ʱ�����뵽list��
					pushList.add(pushRecord);
				}
			} else {
				messageFlag = false;
			}
		}

		if (commentFlag && messageFlag) {
			// �������Ͳ�����cid
			push.yinianPushToSingle(pushList);
			return cid;
		} else {
			return "";
		}
	}

	/**
	 * ɾ����̬
	 * 
	 * @param eventId
	 * @return
	 */
	@Before(Tx.class)
	public boolean deleteEvent(String eventId) {

		// ���¼�����Ƭ�����۵�״ֵ̬��Ϊ1
		Db.update("update pictures set pstatus=1 where peid=" + eventId + " ");
		Db.update("update comments set cstatus=1 where ceid=" + eventId + " ");
		// ����ö�̬��ص�������Ϣɾ��
		Db.update("update messages set mstatus=2 where meid=" + eventId + " ");
		int eventCount = Db.update("update events set estatus=1 where eid=" + eventId + " ");

		// �۳��û��洢�ռ�
		Event event = new Event().findById(eventId);
		double storage = Double.parseDouble(event.get("eStoragePlace").toString());
		String userid = event.get("euserid").toString();
		if (storage != 0.0)
			updateUserStoragePlace(userid, storage, "reduce");

		// ����������ã����˻����
		String redEnvelopID = eventService.JudgeEventIsContainRedEnvelop(eventId);
		if (!(redEnvelopID.equals("0")))
			eventService.returnRedEnvelop(redEnvelopID);

		return (eventCount == 1);
	}

	/**
	 * ��ȡ���Ա�����鷢���Ķ�̬��Ϣ
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
		// ��ȡ�¼��������Ϣ
		List<Record> event = Db.find(SqlForEvent);
		// ��ȡ�¼���������Ϣ
		List<Record> comment = Db.find(commentSql);
		// ��װ�����ڵ��û���
		comment = dataProcess.encapsulationCommentList(comment);
		List<Record> list = dataProcess.combieEventAndComment(event, comment);
		// ��װ�¼��ͱ�ǩ
		list = dataProcess.combineEventWithTags(list);
		// ��װ�¼��Ϳ�Ƭ��ʽ
		list = dataProcess.combineEventWithCardStyle(list);
		// �޸�upic�ֶθ�ʽ
		list = dataProcess.ChangePicAsArray(list);

		jsonString = jsonData.getJson(0, "success", list);
		return jsonString;
	}

	/**
	 * ˢ�³�Ա���˶�̬
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
		List<Record> event = Db.find(SqlForEvent);// ��ȡ�¼��������Ϣ
		List<Record> comment = Db.find(commentSql);// ��ȡ�¼���������Ϣ
		comment = dataProcess.encapsulationCommentList(comment);// ��װ�����ڵ��û���
		List<Record> list = dataProcess.combieEventAndComment(event, comment);
		// ��װ�¼��ͱ�ǩ
		list = dataProcess.combineEventWithTags(list);
		// ��װ�¼��Ϳ�Ƭ��ʽ
		list = dataProcess.combineEventWithCardStyle(list);
		list = dataProcess.ChangePicAsArray(list);// �޸�upic�ֶθ�ʽ
		jsonString = jsonData.getJson(0, "success", list);
		return jsonString;
	}

	/**
	 * ɾ���ռ�
	 * 
	 * @param groupid
	 * @return
	 */
	@Before(Tx.class)
	public String deleteGroup(String groupid, String source) {

		// ��ȡ��Ҫ����֪ͨ�������û���ID
		List<Record> UserID = getGroupMemberID(groupid);
		// �޸ķ������Ա����̬����Ӧ���ݵ�״ֵ̬Ϊ1
		Db.update("update groups set gstatus=1 where groupid=" + groupid + " ");
		Db.update("update groupmembers set gmstatus=1 where gmgroupid=" + groupid + " ");
		Db.update("update events set estatus=1 where egroupid=" + groupid + " ");

		// ��ȡ���������ж�̬��ID
		List<Record> list = Db.find("select eid from events where egroupid=" + groupid + " ");

		// ��ÿ����̬��Ӧ��״̬��ͼƬ״̬������״̬��Ϊ1
		for (Record record : list) {
			deleteEvent(record.get("eid").toString());
		}

		// �������Ӧ�����н���������Ϣ�ͽ���֪ͨ��״̬��Ϊ2��ʾ��ɾ��
		Db.update(" update invitegroup set igstatus=2 where iggroupid=" + groupid + " ");
		Db.update(" update notifications set nstatus=2 where ntype=1 and ngroupid=" + groupid + " ");

		// ����list
		List<Record> pushList = new ArrayList<Record>();
		// ����֪ͨ�ɹ��жϱ�־
		boolean flag = true;
		if (source == null || !source.equals("smallApp")) {

			// ��ȡ֪ͨ���������
			Record contentRecord = Db
					.findFirst("select gcreator,gtype,gname from groups where groupid=" + groupid + " ");
			// ��ȡ�����ߵ�ID
			int creator = Integer.parseInt(contentRecord.get("gcreator").toString());
			// ��ȡ֪ͨ����
			String content = dataProcess.getNotificationContent(contentRecord, "delete");

			// ����֪ͨ����
			for (Record record : UserID) {
				int userid = Integer.parseInt(record.get("gmuserid").toString());
				if (creator != userid) {
					// ����֪ͨ
					Notification notification = new Notification()
							.set("nsender", Integer.parseInt(contentRecord.get("gcreator").toString()))
							.set("nreceiver", Integer.parseInt(record.get("gmuserid").toString()))
							.set("ncontent", content).set("ntype", 2).set("ngroupid", groupid);
					if (notification.save()) {
						flag = true;
						// ֪ͨ�洢�ɹ���ͬʱ,����͸�����ݲ���������
						String nid = notification.get("nid").toString();
						Record pushRecord = pushMessage.getPushRecord(contentRecord.get("gcreator").toString(),
								record.get("gmuserid").toString(), groupid, content, nid, "notification", null);
						if (!((pushRecord.toJson()).equals("{}"))) {
							// Record��Ϊ��ʱ�����뵽list��
							pushList.add(pushRecord);
						}
					} else {
						flag = false;
						break;
					}
				}
			}
		}
		// ɾ������ʱ����е�����
		Db.update("update lovertimemachine set ltmStatus=1 where ltmGroupID=" + groupid + " ");
		// ɾ�������е�������
		// im.DismissChatGroup(groupid);

		if (flag) {
			push.yinianPushToSingle(pushList);
			jsonString = jsonData.getJson(0, "success");
		} else {
			jsonString = jsonData.getJson(-52, "ɾ�����ݿ�����ʧ��");
		}
		return jsonString;
	}

	/**
	 * �³�Ա������
	 * 
	 * @param groupid
	 * @param userid
	 * @return
	 */
	@Before(Tx.class)
	public List<Record> enterGroup(String groupid, String userid) {
		String updateSql = "update groups set gnum = gnum+1 where groupid='" + groupid + "' "; // ���·���������Ա�����ֶ�
		// �������ݵ�groupmembers����
		boolean insertFlag = insertIntoGroupMembers(userid, groupid);
		int count = Db.update(updateSql);
		// ��ȡ����Ϣ
		List<Record> list = Db.find(
				"select groupid,gname,gpic,gnum,gtime,gmnotify,gtype,gcreator,ginvite from groups,groupmembers where groups.groupid=groupmembers.gmgroupid and gstatus=0 and groupid="
						+ groupid + " and gmuserid=" + userid + " ");
		// ��ȡ�����Ƭ��
		String sqlForPhotos = "select groupid,count(*) as gpicnum from groups,events,pictures where peid=eid and groupid=egroupid and gstatus=0 and estatus in(0,3) and pstatus=0 and groupid="
				+ groupid + " group by groupid";
		List<Record> photoList = Db.find(sqlForPhotos);
		// ����Ƭ���ֶη��������Ϣ��
		if (photoList.size() == 0) {
			list.get(0).set("gpicnum", "0");
		} else {
			list.get(0).set("gpicnum", photoList.get(0).get("gpicnum").toString());
		}
		// �û�����IMȺ����
		// im.AddChatGroupMember(userid, groupid);
		// ��������ʻ��ɶ�Ӧ����
		list = dataProcess.changeGroupTypeIntoWord(list);
		if (insertFlag && count == 1) {
			return list;
		} else {
			return null;
		}
	}

	/**
	 * �³�Ա����ٷ����
	 * 
	 * @param groupid
	 * @param userid
	 * @return
	 */
	@Before(Tx.class)
	public List<Record> enterOfficialAlbum(String groupid, String userid) {
		String updateSql = "update groups set gnum = gnum+1 where groupid='" + groupid + "' "; // ���·���������Ա�����ֶ�
		// �������ݵ�groupmembers����
		boolean insertFlag = insertIntoGroupMembers(userid, groupid);
		int count = Db.update(updateSql);
		List<Record> list = Db.find(
				"select groupid,gname,gpic,gnum,gtime,gmnotify,gtype,gcreator,ginvite from groups,groupmembers where groups.groupid=groupmembers.gmgroupid and gstatus=0 and groupid="
						+ groupid + " and gmuserid=" + userid + " ");
		list.get(0).remove("gtype");
		list.get(0).set("gtype", "5");
		// ��ȡ�����Ƭ��
		String sqlForPhotos = "select groupid,count(*) as gpicnum from groups,events,pictures where peid=eid and groupid=egroupid and gstatus=0 and gtype=5 and estatus in(0,3) and pstatus=0 and groupid="
				+ groupid + " group by groupid";
		List<Record> photoList = Db.find(sqlForPhotos);
		// ����Ƭ���ֶη��������Ϣ��
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
	 * ����groupmembers�����ж��û��Ƿ����������
	 * 
	 * @param userid
	 * @param groupid
	 * @return
	 */
	public boolean insertIntoGroupMembers(String userid, String groupid) {
		boolean flag;
		int count = 0; // ����ֵ
		// // ��ȡ�����������ֵ
		// List<Record> judgeList = Db
		// .find("select max(gmorder) as gmorder from groupmembers where gmuserid="
		// + userid + " and gmstatus=0");
		// // �ж�����б��Ƿ��������δ������ʱ��˳������
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
	 * ��ȡ���ҡ��������ж�̬
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
		List<Record> event = Db.find(SqlForEvent);// ��ȡ�¼��������Ϣ
		List<Record> comment = Db.find(commentSql);// ��ȡ�¼���������Ϣ
		event = dataProcess.encapsulationEventList(event);// ��ȡ�¼��������Ϣ����װ������û�����
		comment = dataProcess.encapsulationCommentList(comment);// ��װ�����ڵ��û���
		List<Record> list = dataProcess.combieEventAndComment(event, comment);
		// ��װ�¼��ͱ�ǩ
		list = dataProcess.combineEventWithTags(list);
		// ��װ�¼��Ϳ�Ƭ��ʽ
		list = dataProcess.combineEventWithCardStyle(list);
		list = dataProcess.ChangePicAsArray(list);// �޸�upic�ֶθ�ʽ
		// ��װ�¼��͵���
		list = dataProcess.combineEventWithLike(list, "smallApp");
		jsonString = jsonData.getJson(0, "success", list);
		return jsonString;
	}

	/**
	 * �޸ĸ�����Ϣ
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
		if (sex.equals("Ů")) {
			newSex = 0;
		} else {
			newSex = 1;
		}
		User user = new User().set("userid", userid).set("upic", pic).set("usex", newSex).set("unickname", nickname)
				.set("ubirth", birthday);
		return user.update();
		// if (user.update()) {
		// // �޸�IM���û����ǳ�
		// im.ModifyIMUserNickname(userid, nickname);
		// return true;
		// } else {
		// return false;
		// }
	}

	/**
	 * �޸ĸ��˵�������
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
			if (data.equals("Ů")) {
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
			// �޸��û�IM�е��ǳ�
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
	 * ��ø�����Ϣ
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
	 * �޸�����
	 * 
	 * @param groupid
	 * @param groupName
	 * @return
	 */
	public String modifyGroupName(String groupid, String groupName) {
		if (Group.dao.findById(groupid).set("gname", groupName).update()) {
			jsonString = jsonData.getJson(0, "success");
		} else {
			jsonString = jsonData.getJson(-51, "�������ݿ�����ʧ��");
		}
		return jsonString;
	}

	/**
	 * ��Ӿٱ�
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
			jsonString = jsonData.getJson(-50, "��������ʧ��");
		}
		return jsonString;
	}

	/**
	 * ��ȡ������̬��Ϣ
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
		List<Record> event = Db.find(eventSql);// ��ȡ�¼��������Ϣ
		String status = event.get(0).get("estatus").toString();
		if (status.equals("1")) {
			jsonString = jsonData.getJson(1027, "��̬�ѱ�ɾ��");
		} else {
			// ��һ��ͼƬͼƬ��Ȩ
			if (event.get(0).get("efirstpic") != null) {
				String auth = qiniu.getDownloadToken(event.get(0).get("efirstpic").toString());
				event.get(0).set("efirstpic", auth);
			}

			List<Record> comment = Db.find(commentSql);// ��ȡ�¼���������Ϣ
			// ��װ�¼��������ڵ��û���
			event = dataProcess.encapsulationEventList(event);
			comment = dataProcess.encapsulationCommentList(comment);
			List<Record> list = dataProcess.combieEventAndComment(event, comment);
			// ��װ�¼��ͱ�ǩ
			list = dataProcess.combineEventWithTags(list);
			// ��Դ��Ȩ
			list = dataProcess.ChangePicAsArray(list);
			jsonString = jsonData.getJson(0, "success", list);
		}
		return jsonString;
	}

	/**
	 * ��ȡ������̬��Ϣ
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
		List<Record> event = Db.find(eventSql);// ��ȡ�¼��������Ϣ
		List<Record> list = new ArrayList<Record>();
		String status = event.get(0).get("estatus").toString();
		if (status.equals("1")) {
			jsonString = jsonData.getJson(1027, "��̬�ѱ�ɾ��");
		} else {
			// ��һ��ͼƬͼƬ��Ȩ
			String auth = qiniu.getDownloadToken(event.get(0).get("efirstpic").toString());
			event.get(0).set("efirstpic", auth);
			List<Record> comment = Db.find(commentSql);// ��ȡ�¼���������Ϣ
			// ��װ�¼��������ڵ��û���
			event = dataProcess.encapsulationEventList(event);
			comment = dataProcess.encapsulationCommentList(comment);
			list = dataProcess.combieEventAndComment(event, comment);
			// ��װ�¼��ͱ�ǩ
			list = dataProcess.combineEventWithTags(list);
			// ��װ�¼��Ϳ�Ƭ��ʽ
			list = dataProcess.combineEventWithCardStyle(list);
			list = dataProcess.ChangePicAsArray(list);// �޸�upic�ֶθ�ʽ
		}
		return list;
	}

	/**
	 * ��ȡ�ٷ�����ڵĵ�����̬��Ϣ�������û���Ҫ���ذ������޵Ķ�̬
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
		List<Record> event = Db.find(eventSql);// ��ȡ�¼��������Ϣ
		List<Record> comment = Db.find(commentSql);// ��ȡ�¼���������Ϣ
		// ��װ�¼��������ڵ��û���
		event = dataProcess.encapsulationEventList(event);
		comment = dataProcess.encapsulationCommentList(comment);
		List<Record> list = dataProcess.combieEventAndComment(event, comment);
		// ��װ�¼��ͱ�ǩ
		list = dataProcess.combineEventWithTags(list);
		// ��װ�¼��Ϳ�Ƭ��ʽ
		list = dataProcess.combineEventWithCardStyle(list);
		list = dataProcess.ChangePicAsArray(list);// �޸�upic�ֶθ�ʽ
		jsonString = jsonData.getJson(0, "success", list);
		return jsonString;
	}

	/**
	 * ��ȡ�������г�Ա��ID
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
	 * �޸����������Ϣ״̬Ϊ��ɾ��
	 * 
	 * @param messageID
	 * @return
	 */
	public boolean deleteMessage(String messageID) {
		boolean flag = InviteGroup.dao.findById(messageID).set("igstatus", 2).update();
		return flag;
	}

	/**
	 * �޸�������Ϣ״̬Ϊ��ɾ��
	 * 
	 * @param messageID
	 * @return
	 */
	public boolean deleteCommentMessage(String messageID) {
		boolean flag = Message.dao.findById(messageID).set("mstatus", 2).update();
		return flag;
	}

	/**
	 * �޸�֪ͨ״̬Ϊ��ɾ��
	 * 
	 * @param notificationID
	 * @return
	 */
	public boolean deleteNotification(String notificationID) {
		boolean flag = Notification.dao.findById(notificationID).set("nstatus", 2).update();
		return flag;
	}

	/**
	 * ��ȡ��Ϣ�б�
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
			// minID==0�����ʼ��������
			SqlForMessage = "select mid,unickname,upic,efirstpic,meid,mcontent,type,mtime,mstatus from users,messages,events where msender = userid and mstatus in(0,1) and meid = eid and mreceiver='"
					+ userid + "' group by mid desc limit 10";
		} else {
			SqlForMessage = "select mid,unickname,upic,efirstpic,meid,mcontent,type,mtime,mstatus from users,messages,events where msender = userid and mstatus in(0,1) and meid = eid and mreceiver='"
					+ userid + "' and mid < " + minID + " group by mid desc limit 10";
		}
		// ��ȡӦ����ʾ��������Ϣ�б�
		List<Record> messageList = Db.find(SqlForMessage);
		// ��ȡ���û����е���Ϣ��ID
		List<Record> messageID = Db
				.find("select mid from messages where mreceiver=" + userid + " and mstatus in(0,1) ");
		// ��������Ϣ�б��е�ÿ����¼״̬���Ϊ�Ѷ�
		for (Record record : messageID) {
			Db.update(" update messages set mstatus=1 where mid=" + record.get("mid").toString() + " ");
		}
		if (sign.equals("initialization")) {
			// ��־Ϊ����ʼ��������ȡ���������Ϣ�б�
			List<Record> inviteList = Db.find(sqlForInvite);
			List<Record> list = dataProcess.combineTwoList(inviteList, messageList);
			jsonString = jsonData.getJson(0, "success", list);
		} else {
			if (sign.equals("loading")) {
				// ��־λ�����ء���ֻ��ȡ�����ȡ��������Ϣ�б�
				jsonString = jsonData.getJson(0, "success", messageList);
			} else {
				jsonString = jsonData.getJson(2, "��������");
			}
		}
		return jsonString;
	}

	/**
	 * ��ȡ������Ϣ����Ϣ
	 * 
	 * @param userid
	 * @param id
	 * @param type
	 * @return
	 */
	@Before(Tx.class)
	public String getCommentMessageInformation(String userid, String id, String type) {
		String sqlForCommentMessage = "";
		// ����type������Ӧ������
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
			jsonString = jsonData.getJson(2, "��������");
			return jsonString;
		}
		// ��ȡӦ����ʾ��������Ϣ�б�
		List<Record> messageList = Db.find(sqlForCommentMessage);
		// ͼƬ��Ȩ
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

		// ˢ�¡���ʼ��ʱ����Ӧ����������Ϊ�Ѷ�
		switch (type) {
		case "initialize":
			// ��ȡ���û����е���Ϣ��ID
			List<Record> messageID = Db
					.find("select mid from messages where mreceiver=" + userid + " and mstatus in(0,1) ");
			// ��������Ϣ�б��е�ÿ����¼״̬���Ϊ�Ѷ�
			for (Record record : messageID) {
				Db.update(" update messages set mstatus=1 where mid=" + record.get("mid").toString() + " ");
			}
			break;
		case "refresh":
			// ��ˢ�º��õ�������Ϣ�б��е�Ԫ�ص�״̬��Ϊ�Ѷ�
			for (Record record : messageList) {
				Db.update(" update messages set mstatus=1 where mid=" + record.get("mid").toString() + " ");
			}
			break;
		}
		jsonString = jsonData.getJson(0, "success", messageList);
		return jsonString;
	}

	/**
	 * ��ȡ֪ͨ��Ϣ����Ϣ
	 * 
	 * @param userid
	 * @param id
	 * @param type
	 * @return
	 */
	@Before(Tx.class)
	public String getNotificationMessageInformation(String userid, String id, String type) {
		String SqlForNotification = "";
		// ����type������Ӧ������
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
			jsonString = jsonData.getJson(2, "��������");
			return jsonString;
		}
		// ��ȡӦ����ʾ��������Ϣ�б�
		List<Record> notificationList = Db.find(SqlForNotification);
		// ˢ�¡���ʼ��ʱ����Ӧ����������Ϊ�Ѷ�
		switch (type) {
		case "initialize":
			// ��ȡ���û�����֪ͨ��ID
			List<Record> notificationID = Db
					.find("select nid from notifications where nreceiver = " + userid + " and nstatus in(0,1) ");
			// ��֪ͨ�б��е�ÿ��Ԫ�ص�״̬�޸�Ϊ�Ѷ�
			for (Record record : notificationID) {
				Db.update(" update notifications set nstatus=1 where nid=" + record.get("nid").toString() + " ");
			}
			break;
		case "refresh":
			// ��֪ͨ�б��е�ÿ��Ԫ�ص�״̬�޸�Ϊ�Ѷ�
			for (Record record : notificationList) {
				Db.update(" update notifications set nstatus=1 where nid=" + record.get("nid").toString() + " ");
			}
			break;
		}
		jsonString = jsonData.getJson(0, "success", notificationList);
		return jsonString;
	}

	/**
	 * ��ȡ������Ϣ����Ϣ
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
			jsonString = jsonData.getJson(2, "��������");
			return jsonString;
		}
		List<Record> inviteList = Db.find(sqlForInviteMessage);
		jsonString = jsonData.getJson(0, "success", inviteList);
		return jsonString;
	}

	/**
	 * ��ȡ֪ͨ�б�
	 * 
	 * @param userid
	 * @param page
	 * @return
	 */
	@Before(Tx.class)
	public String getNotificationList(String userid, int minID) {
		String SqlForNotification = "";
		if (minID == 0) {
			// minID==0�����ǳ�ʼ�������
			// tip:��ͷ��gpic���ֶ��û�ͷ��upic���棬ǰ�˲����޸ģ���ͬ�����Դ˷�ֹ����
			SqlForNotification = "select nid,ncontent,ntype,ntime,nstatus,unickname,gpic as upic from notifications,users,groups where nsender=userid and ngroupid=groupid and nreceiver='"
					+ userid + "' and nstatus in(0,1) GROUP BY nid DESC limit 10";
			// ��ȡ���û�����֪ͨ��ID
			List<Record> notificationID = Db
					.find("select nid from notifications where nreceiver = " + userid + " and nstatus in(0,1) ");
			// ��֪ͨ�б��е�ÿ��Ԫ�ص�״̬�޸�Ϊ�Ѷ�
			for (Record record : notificationID) {
				Db.update(" update notifications set nstatus=1 where nid=" + record.get("nid").toString() + " ");
			}
		} else {
			SqlForNotification = "select nid,ncontent,ntype,ntime,nstatus,unickname,gpic as upic from notifications,users,groups where nsender=userid and ngroupid=groupid and nreceiver='"
					+ userid + "' and nid<" + minID + " and nstatus in(0,1) GROUP BY nid DESC limit 10";
		}
		List<Record> notification = Db.find(SqlForNotification);// ��ȡ�¼��������Ϣ

		jsonString = jsonData.getJson(0, "success", notification);
		return jsonString;
	}

	/**
	 * ��ȡδ����Ϣ��֪ͨ������
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
	 * ��ȡ���͵���badge������
	 */
	public int getNumbersInBadge(String userid) {
		// ��ȡ�������ֵ�δ����Ϣ����
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
	 * �ϴ���̬���ɹ��󷵻��¼���ID
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

		// ����ַ�ַ���ת��������
		String[] picArray = dataProcess.getPicAddress(picAddress, mode);

		// ͼƬ����
		picArray = dataProcess.PictureVerify(picArray);

		// ��ȡ��̬��һ��ͼƬ��ַ
		String firstPic = picArray[0];

		// ��ռ�ÿռ������תΪdouble
		Double place = Double.parseDouble(storage);

		String newTime = "";
		// �����¼�
		Event event = new Event();

		// �ж�memorytime�ֶ��Ƿ��д�����
		if (memorytime == null || memorytime.equals("")) {
			event.set("egroupid", groupid).set("euserid", userid).set("etext", content).set("efirstpic", firstPic)
					.set("eStoragePlace", place).set("eSource", source).set("isSynchronize", 1);
		} else {
			// ƴ���ַ���
			newTime = memorytime + " 00:00:00";
			event.set("egroupid", groupid).set("euserid", userid).set("etext", content).set("efirstpic", firstPic)
					.set("ememorytime", newTime).set("eStoragePlace", place).set("eSource", source)
					.set("isSynchronize", 1);
		}

		// �ж�mode�ǲ��ǵ���dayMark ����ǩ���������etypeΪ3
		if (mode.equals("dayMark")) {
			event.set("etype", 3);
		}

		// �ж�λ����Ϣ
		if (location != null && !location.equals("")) {
			event.set("eplace", location);
		}

		if (event.save()) {
			eventID = event.get("eid").toString();

			// ���¼��������״̬��Ϊ����״̬
			// Db.update("update groupmembers set gmnotify=1 where gmgroupid ="
			// + groupid + " ");
			// // �����ߵ�״̬��Ϊ����״̬����Ϊ������˷����¶�̬�������߷�����������ڶ�̬����
			// Db.update("update groupmembers set gmnotify=0 where gmgroupid ="
			// + groupid + " and gmuserid = " + userid + " ");

			// �ж�memorytime�ֶ��Ƿ��д�����
			if (memorytime == null || memorytime.equals("")) {
				// ����ͼƬ
				for (int i = 0; i < picArray.length; i++) {
					Picture pic = new Picture();
					pic.set("peid", eventID).set("poriginal", picArray[i]);
					pic.save();
				}
			} else {
				// newTime����Ҫ��ƴ��
				// ����ͼƬ
				for (int i = 0; i < picArray.length; i++) {
					Picture pic = new Picture();
					pic.set("peid", eventID).set("poriginal", picArray[i]).set("pmemorytime", newTime);
					pic.save();
				}
			}

		}

		// �����û���ʹ�ÿռ�
		if (place != 0) {
			updateUserStoragePlace(userid, place, "add");
		}

		// // ��������׼��
		// // ��ȡ���������û���ID
		// List<Record> useridList = Db
		// .find("select gmuserid from groupmembers where gmgroupid = "
		// + groupid + " ");
		// // ȥ�������ߵ�ID
		// for (Record record : useridList) {
		// if ((record.get("gmuserid").toString()).equals(userid)) {
		// useridList.remove(record);
		// break;
		// }
		// }
		// // useridList��Ϊnullʱ�Ž�������
		// if (!(useridList.isEmpty())) {
		// // ��list�е�IDƴ�ӳ��ַ���
		// String userids = dataProcess.changeListToString(useridList,
		// "gmuserid");
		// // userids��Ϊ���ַ���ʱ�Ž�������
		// if (!(userids.equals(""))) {
		// // ��ȡҪ�������͵��û���cid
		// Record cid = pushMessage.getUsersCids(userids, "userid");
		// // ��ȡ��̬�����ߵ��ǳ�
		// String nickname = dao.getUserSingleInfo(userid, "nickname");
		// // ��ȡ������̬���������Ϣ
		// List<Record> groupList = Db
		// .find("select gname,gtype from groups where groupid = "
		// + groupid + " ");
		// groupList = dataProcess.changeGroupTypeIntoWord(groupList);
		// // ƴ����������
		// String pushContent = nickname + "��˽�ܿռ䡰"
		// + groupList.get(0).getStr("gname") + "���������¶�̬";
		// Record data = new Record().set("content", pushContent);
		// // ����͸������
		// int gid = Integer.parseInt(groupid);
		// Record transmissionRecord = new Record().set("groupid", gid)
		// .set("pushContent", pushContent)
		// .set("gname", groupList.get(0).getStr("gname"));
		// List<Record> list = new ArrayList<Record>();
		// list.add(transmissionRecord);
		// String transmissionContent = jsonData.getJson(3, "����̬", list);
		// data.set("transmissionContent", transmissionContent);
		// // ����
		// push.yinianPushToList(cid, data);
		// }
		// }
		return eventID;

	}

	/**
	 * �ϴ�һЩ��̬
	 * 
	 */
	@Before(Tx.class)
	public boolean uploadSomeEvent(String userid, String groupid, String picAddress, String content, String storage) {
		String eventID = "";
		// ����ַ�ַ���ת��������
		String[] picArray = dataProcess.getPicAddress(picAddress, null);
		// ��ȡ��̬��һ��ͼƬ��ַ
		String firstPic = picArray[0];
		// ��ռ�ÿռ������תΪdouble
		Double place = Double.parseDouble(storage);

		// �����¼�
		Event event = new Event();
		// �ж�memorytime�ֶ��Ƿ��д�����

		event.set("egroupid", groupid).set("euserid", userid).set("etext", content).set("efirstpic", firstPic)
				.set("eStoragePlace", place).set("eOrigin", 1);

		if (event.save()) {
			eventID = event.get("eid").toString();
			// ����ͼƬ
			for (int i = 0; i < picArray.length; i++) {
				Picture pic = new Picture();
				pic.set("peid", eventID).set("poriginal", picArray[i]).set("pOrigin", 1)
					.set("pGroupid", groupid).set("puserid", userid);
				pic.save();
			}

		}
		// �����û���ʹ�ÿռ�
		boolean flag = updateUserStoragePlace(userid, place, "add");

		return flag;

	}

	/**
	 * �ϴ����俨Ƭ
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
		// ����ַ�ַ���ת��������
		String[] picArray = dataProcess.getPicAddress(picAddress, mode);
		// ��ȡ��̬��һ��ͼƬ��ַ
		String firstPic = picArray[0];
		// �ж�ռ�ÿռ��ֵ����ռ�ÿռ������תΪdouble
		if (storage == null || storage.equals("")) {
			storage = "0";
		}
		Double storagePlace = Double.parseDouble(storage);

		String newTime = "";
		if ((audiotime == null || audiotime.equals(""))) {
			audiotime = "0";
		}
		// �����¼�,�ȴ��һ������ȥ�Ĳ������������ٸ�������ж�
		Event event = new Event().set("egroupid", groupid).set("euserid", userid).set("etext", content)
				.set("efirstpic", firstPic).set("eStoragePlace", storagePlace).set("ecardstyle", cardstyle)
				.set("eplace", place).set("etype", 1).set("eaudiotime", audiotime);
		// �ж�memorytime�ֶ��Ƿ��д�����
		if (!(memorytime.equals("")) && memorytime != null) {
			// ƴ���ַ���
			newTime = memorytime + " 00:00:00";
			event.set("ememorytime", newTime);
		}
		// �ж�audio�Ƿ���ǰ׺���Դ����ϴ�������ͬ���������ܿ��Լ���
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
			// ���¼��������״̬��Ϊ����״̬
			Db.update("update groupmembers set gmnotify=1 where gmgroupid =" + groupid + " ");
			// �����ߵ�״̬��Ϊ����״̬����Ϊ������˷����¶�̬�������߷�����������ڶ�̬����
			Db.update("update groupmembers set gmnotify=0 where gmgroupid =" + groupid + " and gmuserid = " + userid
					+ " ");

			// �ж�memorytime�ֶ��Ƿ��д�����
			if (memorytime == null || memorytime.equals("")) {
				// ����ͼƬ
				for (int i = 0; i < picArray.length; i++) {
					Picture pic = new Picture();
					pic.set("peid", eventID).set("poriginal", picArray[i]).set("pGroupid", groupid).set("puserid", userid);
					pic.save();
				}
			} else {
				// newTime����Ҫ��ƴ��
				// ����ͼƬ
				for (int i = 0; i < picArray.length; i++) {
					Picture pic = new Picture();
					pic.set("peid", eventID).set("poriginal", picArray[i]).set("pmemorytime", newTime)
					.set("pGroupid", groupid).set("puserid", userid);
					pic.save();
				}
			}

			// �жϱ�ǩtags�Ƿ��д�ֵ��������в���
			if (tags != null) {
				// ͨ�� &nbsp ����������tag
				String[] eventTag = tags.split("&nbsp");
				for (int i = 0; i < eventTag.length; i++) {
					Tag tag = new Tag().set("tagEventID", eventID).set("tagContent", eventTag[i]);
					tag.save();
					// ��ÿ����ǩ������ʷ��ǩ����
					handleTagsInHistoryTags(userid, eventTag[i]);
				}
			}

		}
		// �����û���ʹ�ÿռ�
		updateUserStoragePlace(userid, storagePlace, "add");

		// ��������׼��
		// ��ȡ���������û���ID
		List<Record> useridList = Db.find("select gmuserid from groupmembers where gmgroupid = " + groupid + " ");
		// ȥ�������ߵ�ID
		for (Record record : useridList) {
			if ((record.get("gmuserid").toString()).equals(userid)) {
				useridList.remove(record);
				break;
			}
		}
		// useridList��Ϊnullʱ�Ž�������
		if (!(useridList.isEmpty())) {
			// ��list�е�IDƴ�ӳ��ַ���
			String userids = dataProcess.changeListToString(useridList, "gmuserid");
			// userids��Ϊ���ַ���ʱ�Ž�������
			if (!(userids.equals(""))) {
				// ��ȡҪ�������͵��û���cid
				Record cid = pushMessage.getUsersCids(userids, "userid");
				// ��ȡ��̬�����ߵ��ǳ�
				String nickname = dao.getUserSingleInfo(userid, "nickname");
				// ��ȡ������̬���������Ϣ
				List<Record> groupList = Db.find("select gname,gtype from groups where groupid = " + groupid + " ");
				groupList = dataProcess.changeGroupTypeIntoWord(groupList);
				// ƴ����������
				String pushContent = nickname + "��˽�ܿռ䡰" + groupList.get(0).getStr("gname") + "���ϴ���һ�ż��俨Ƭ";
				Record data = new Record().set("content", pushContent);
				// ����͸������
				int gid = Integer.parseInt(groupid);
				Record transmissionRecord = new Record().set("groupid", gid).set("pushContent", pushContent)
						.set("gname", groupList.get(0).getStr("gname"));
				List<Record> list = new ArrayList<Record>();
				list.add(transmissionRecord);
				String transmissionContent = jsonData.getJson(3, "����̬", list);
				data.set("transmissionContent", transmissionContent);
				// ����
				push.yinianPushToList(cid, data);
			}
		}
		return eventID;
	}

	/**
	 * �ϴ�ʱ������Ƭ
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
		// ����ַ�ַ���ת��������
		String[] picArray = dataProcess.getPicAddress(picAddress, mode);
		// ��ȡ��̬��һ��ͼƬ��ַ
		String firstPic = picArray[0];
		// �ж�ռ�ÿռ��ֵ����ռ�ÿռ������תΪdouble
		if (storage == null || storage.equals("")) {
			storage = "0";
		}
		Double storagePlace = Double.parseDouble(storage);

		String newTime = "";
		// �����¼�,�ȴ��һ������ȥ�Ĳ������������ٸ�������ж�
		Event event = new Event().set("egroupid", groupid).set("euserid", userid).set("etext", content)
				.set("efirstpic", firstPic).set("eStoragePlace", storagePlace).set("ecover", coverUrl)
				.set("eplace", place).set("etype", 2).set("eaudiotime", audiotime);
		// �ж�memorytime�ֶ��Ƿ��д�����
		if (!(memorytime.equals("")) && memorytime != null) {
			// ƴ���ַ���
			newTime = memorytime + " 00:00:00";
			event.set("ememorytime", newTime);
		}
		// �ж�audio�Ƿ���ǰ׺���Դ����ϴ�������ͬ���������ܿ��Լ���
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
			// ���¼��������״̬��Ϊ����״̬
			Db.update("update groupmembers set gmnotify=1 where gmgroupid =" + groupid + " ");
			// �����ߵ�״̬��Ϊ����״̬����Ϊ������˷����¶�̬�������߷�����������ڶ�̬����
			Db.update("update groupmembers set gmnotify=0 where gmgroupid =" + groupid + " and gmuserid = " + userid
					+ " ");

			// �ж�memorytime�ֶ��Ƿ��д�����
			if (memorytime == null || memorytime.equals("")) {
				// ����ͼƬ
				for (int i = 0; i < picArray.length; i++) {
					Picture pic = new Picture();
					pic.set("peid", eventID).set("poriginal", picArray[i])
					.set("pGroupid", groupid).set("puserid", userid);
					pic.save();
				}
			} else {
				// newTime����Ҫ��ƴ��
				// ����ͼƬ
				for (int i = 0; i < picArray.length; i++) {
					Picture pic = new Picture();
					pic.set("peid", eventID).set("poriginal", picArray[i]).set("pmemorytime", newTime)
					.set("pGroupid", groupid).set("puserid", userid);
					pic.save();
				}
			}

			// �жϷ����Ƿ�Ϊ��ʷ���棬��������뵽��ʷ��¼���У�ͨ��URL�����ж�
			List<Record> list = Db.find("select * from historycover where historyCoverPicture='" + coverUrl
					+ "' and historyCoverUserID=" + userid + " and historyCoverStatus=0 ");
			if (list.size() == 0) {
				HistoryCover hc = new HistoryCover().set("historyCoverPicture", coverUrl).set("historyCoverUserID",
						userid);
				hc.save();
			}

		}
		// �����û���ʹ�ÿռ�
		updateUserStoragePlace(userid, storagePlace, "add");

		// ��������׼��
		// ��ȡ���������û���ID
		List<Record> useridList = Db.find("select gmuserid from groupmembers where gmgroupid = " + groupid + " ");
		// ȥ�������ߵ�ID
		for (Record record : useridList) {
			if ((record.get("gmuserid").toString()).equals(userid)) {
				useridList.remove(record);
				break;
			}
		}
		// useridList��Ϊnullʱ�Ž�������
		if (!(useridList.isEmpty())) {
			// ��list�е�IDƴ�ӳ��ַ���
			String userids = dataProcess.changeListToString(useridList, "gmuserid");
			// userids��Ϊ���ַ���ʱ�Ž�������
			if (!(userids.equals(""))) {
				// ��ȡҪ�������͵��û���cid
				Record cid = pushMessage.getUsersCids(userids, "userid");
				// ��ȡ��̬�����ߵ��ǳ�
				String nickname = dao.getUserSingleInfo(userid, "nickname");
				// ��ȡ������̬���������Ϣ
				List<Record> groupList = Db.find("select gname,gtype from groups where groupid = " + groupid + " ");
				groupList = dataProcess.changeGroupTypeIntoWord(groupList);
				// ƴ����������
				String pushContent = nickname + "��˽�ܿռ䡰" + groupList.get(0).getStr("gname") + "�����ϴ���һ��ʱ������Ƭ";
				Record data = new Record().set("content", pushContent);
				// ����͸������
				int gid = Integer.parseInt(groupid);
				Record transmissionRecord = new Record().set("groupid", gid).set("pushContent", pushContent)
						.set("gname", groupList.get(0).getStr("gname"));
				List<Record> list = new ArrayList<Record>();
				list.add(transmissionRecord);
				String transmissionContent = jsonData.getJson(3, "����̬", list);
				data.set("transmissionContent", transmissionContent);
				// ����
				push.yinianPushToList(cid, data);
			}
		}
		return eventID;
	}

	/**
	 * �����͵��ϴ���̬ 1.2�汾 2016.1.29����
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
		// ����ַ�ַ���ת��������
		String[] picArray = dataProcess.getPicAddress(picAddress, mode);
		// ��ȡ��̬��һ��ͼƬ��ַ
		String firstPic = picArray[0];

		// �����¼�
		Event event = new Event();
		event.set("egroupid", groupid).set("euserid", userid).set("etext", content).set("efirstpic", firstPic);
		if (event.save()) {
			eventID = event.get("eid").toString();
			// ���¼��������״̬��Ϊ����״̬
			Db.update("update groupmembers set gmnotify=1 where gmgroupid =" + groupid + " ");
			// �����ߵ�״̬��Ϊ����״̬����Ϊ������˷����¶�̬�������߷�����������ڶ�̬����
			Db.update("update groupmembers set gmnotify=0 where gmgroupid =" + groupid + " and gmuserid = " + userid
					+ " ");
			// ����ͼƬ \
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
	 * Ͷ�����ٷ���� 2016.3.1����
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
		// ����ַ�ַ���ת��������
		String[] picArray = dataProcess.getPicAddress(picAddress, mode);
		// ��ȡ��̬��һ��ͼƬ��ַ
		String firstPic = picArray[0];

		// �����¼�,ͬʱ���ø�����̬��״̬Ϊ2 ������ˣ�
		Event event = new Event();
		event.set("egroupid", groupid).set("euserid", userid).set("etext", content).set("efirstpic", firstPic)
				.set("estatus", 2);
		if (event.save()) {
			eventID = event.get("eid").toString();
			// ���¼��������״̬��Ϊ����״̬
			Db.update("update groupmembers set gmnotify=1 where gmgroupid =" + groupid + " ");
			// �����ߵ�״̬��Ϊ����״̬����Ϊ������˷����¶�̬�������߷�����������ڶ�̬����
			Db.update("update groupmembers set gmnotify=0 where gmgroupid =" + groupid + " and gmuserid = " + userid
					+ " ");
			// ����ͼƬ
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
	 * Ͷ����俨Ƭ���ٷ����
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
		// ����ַ�ַ���ת��������
		String[] picArray = dataProcess.getPicAddress(picAddress, mode);
		// ��ȡ��̬��һ��ͼƬ��ַ
		String firstPic = picArray[0];
		// �ж�ռ�ÿռ��ֵ����ռ�ÿռ������תΪdouble
		if (storage == null || storage.equals("")) {
			storage = "0";
		}
		Double storagePlace = Double.parseDouble(storage);

		String newTime = "";
		// �����¼�,�ȴ��һ������ȥ�Ĳ������������ٸ�������ж�
		Event event = new Event().set("egroupid", groupid).set("euserid", userid).set("etext", content)
				.set("efirstpic", firstPic).set("eStoragePlace", storagePlace).set("ecardstyle", cardstyle)
				.set("eplace", place).set("etype", 1).set("eaudiotime", audiotime).set("estatus", estatus);
		// �ж�memorytime�ֶ��Ƿ��д�����
		if (!(memorytime.equals("")) && memorytime != null) {
			// ƴ���ַ���
			newTime = memorytime + " 00:00:00";
			event.set("ememorytime", newTime);
		}
		// �ж�audio�Ƿ���ǰ׺���Դ����ϴ�������ͬ���������ܿ��Լ���
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

			// �ж�memorytime�ֶ��Ƿ��д�����
			if (memorytime == null || memorytime.equals("")) {
				// ����ͼƬ
				for (int i = 0; i < picArray.length; i++) {
					Picture pic = new Picture();
					pic.set("peid", eventID).set("poriginal", picArray[i]);
					pic.save();
				}
			} else {
				// newTime����Ҫ��ƴ��
				// ����ͼƬ
				for (int i = 0; i < picArray.length; i++) {
					Picture pic = new Picture();
					pic.set("peid", eventID).set("poriginal", picArray[i]).set("pmemorytime", newTime);
					pic.save();
				}
			}

			// �жϱ�ǩtags�Ƿ��д�ֵ��������в���
			if (tags != null) {
				// ͨ�� &nbsp ����������tag
				String[] eventTag = tags.split("&nbsp");
				for (int i = 0; i < eventTag.length; i++) {
					Tag tag = new Tag().set("tagEventID", eventID).set("tagContent", eventTag[i]);
					tag.save();
					// ��ÿ����ǩ������ʷ��ǩ����
					handleTagsInHistoryTags(userid, eventTag[i]);
				}
			}

		}
		// �����û���ʹ�ÿռ�
		updateUserStoragePlace(userid, storagePlace, "add");
		return eventID;
	}

	/**
	 * ˢ�¶�̬
	 * 
	 * @param maxEventID
	 * @param groupid
	 * @return
	 */
	public String refreshEvent(String maxEventID, String groupid) {
		// �¼���ѯ���
		String sqlForEvent = "select eid,egroupid,userid,unickname,upic,etext,eaudio,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\" ) as url from users,events,pictures where userid=euserid and eid=peid and egroupid="
				+ groupid + " and eid > " + maxEventID + " and estatus=0 and pstatus=0 group by peid DESC";
		// ���۲�ѯ���
		String sqlForComment = CommonParam.selectForComment
				+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and egroupid="
				+ groupid + " and cstatus=0 ORDER BY ceid,ctime asc";

		List<Record> event = dataProcess.encapsulationEventList(Db.find(sqlForEvent));// ��ȡ�¼��������Ϣ����װ������û�����
		List<Record> comment = dataProcess.encapsulationCommentList(Db.find(sqlForComment));// ��ȡ�¼���������Ϣ����װ������û�����
		List<Record> list = dataProcess.combieEventAndComment(event, comment);// ƴ���¼�������
		// ��װ�¼��ͱ�ǩ
		list = dataProcess.combineEventWithTags(list);
		// ��װ�¼��Ϳ�Ƭ��ʽ
		list = dataProcess.combineEventWithCardStyle(list);
		list = dataProcess.ChangePicAsArray(list);// �޸�upic�ֶθ�ʽ
		jsonString = jsonData.getJson(0, "success", list);
		return jsonString;
	}

	/**
	 * ��ʼ����ˢ�¡����عٷ�����ڵĶ�̬
	 * 
	 * @param userid
	 * @param groupid
	 * @param eventid
	 * @param sign
	 * @return
	 */
	public String getOfficialAlbumEvents(String userid, String groupid, String eventid, String sign) {
		int count = 1;
		// ������Ӧ��SQL���
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
			// ��ʼ����ʱ��������״̬Ϊ���¶�̬
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
		List<Record> event = dataProcess.encapsulationEventList(Db.find(sqlForEvent));// ��ȡ�¼��������Ϣ����װ������û�����
		List<Record> comment = dataProcess.encapsulationCommentList(Db.find(sqlForComment));// ��ȡ�¼���������Ϣ����װ������û�����
		List<Record> list = dataProcess.combieEventAndComment(event, comment);// ƴ���¼�������
		// ��װ�¼��ͱ�ǩ
		list = dataProcess.combineEventWithTags(list);
		// ��װ�¼��Ϳ�Ƭ��ʽ
		list = dataProcess.combineEventWithCardStyle(list);
		list = dataProcess.ChangePicAsArray(list);// �޸�upic�ֶθ�ʽ
		jsonString = jsonData.getJson(0, "success", list);

		return jsonString;
	}

	/**
	 * ˢ������
	 * 
	 * @param maxCommentID
	 * @param eventID
	 * @return
	 */
	public String loadComment(String maxCommentID, String eventID) {
		// ���۲�ѯ���
		String sqlForComment = CommonParam.selectForComment
				+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and eid ="
				+ eventID + " and cid>" + maxCommentID + " ORDER BY cid,ctime asc";
		List<Record> list = Db.find(sqlForComment);
		List<Record> comment = dataProcess.encapsulationCommentList(list);
		jsonString = jsonData.getJson(0, "success", comment);
		return jsonString;
	}

	/**
	 * ��������������Ϣ
	 * 
	 * @param userid
	 * @param groupid
	 * @param phonenumbers
	 * @return
	 */
	public List<Record> sendInviteNotification(String userid, String groupid, Record messageInfo, String phonenumbers) {

		// �洢���������Ϣ���б�
		List<Record> pushList = new ArrayList<Record>();
		// ��Ҫ֪ͨ�ĵ绰�����ֳ�����
		String[] phoneArray = phonenumbers.split(",");
		// �����������
		String content = "���������" + messageInfo.getStr("type").toString() + "��"
				+ messageInfo.getStr("groupName").toString() + "��";
		// �������񣬶�����Ϣ����ɹ����ύ
		boolean succeed = Db.tx(new IAtom() {
			public boolean run() throws SQLException {
				boolean flag = true;
				Record UserRecord;
				for (int i = 0; i < phoneArray.length; i++) {
					// ��ѯ�����ߵ��û�ID
					UserRecord = Db.findFirst("select userid from users where uphone='" + phoneArray[i] + "' ");
					// ��������
					InviteGroup invite = new InviteGroup().set("igsender", userid)
							.set("igreceiver", UserRecord.get("userid")).set("igcontent", content)
							.set("iggroupid", groupid);
					if (invite.save()) {
						flag = true;
						// ��ȡID
						String igid = invite.get("igid").toString();
						// ��ȡ����Record
						Record pushRecord = pushMessage.getPushRecord(userid, UserRecord.get("userid").toString(),
								groupid, content, igid, "inviteGroup", null);
						if (!((pushRecord.toJson()).equals("{}"))) {
							// Record��Ϊ��ʱ�����뵽list��
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
			// ����ɹ�����pushList
			return pushList;
		} else {
			// ����ʧ�ܷ��ؿյ�list
			List<Record> list = new ArrayList<Record>();
			return list;
		}
	}

	/**
	 * �����Ա����֪ͨ
	 * 
	 * @param groupid
	 * @param userid
	 * @param useridList
	 * @return
	 */
	@Before(Tx.class)
	public boolean insertEnterNotification(String groupid, String userid, List<Record> useridList) {

		// ����list
		List<Record> pushList = new ArrayList<Record>();
		boolean flag = true;
		// ��ȡ������
		Record record = Db.findFirst("select gtype,gname from groups where groupid = " + groupid + " ");
		record.set("groupid", groupid);
		// ��������ת��������
		List<Record> list = new ArrayList<Record>();
		list.add(record);
		list = dataProcess.changeGroupTypeIntoWord(list);
		record = list.get(0);
		// ����֪ͨ����
		String content = dataProcess.getNotificationContent(record, "enter");

		// ��������
		for (Record user : useridList) {
			int userID = Integer.parseInt(user.get("gmuserid").toString());
			Notification notification = new Notification().set("nsender", Integer.parseInt(userid))
					.set("nreceiver", userID).set("ncontent", content).set("ntype", 1).set("ngroupid", groupid);
			if (notification.save()) {
				flag = true;
				// ֪ͨ�洢�ɹ���ͬʱ,����͸�����ݲ����뵽����list��
				String nid = notification.get("nid").toString();
				Record pushRecord = pushMessage.getPushRecord(userid, user.get("gmuserid").toString(), groupid, content,
						nid, "notification", null);
				if (!((pushRecord.toJson()).equals("{}"))) {
					// Record��Ϊ��ʱ�����뵽list��
					pushList.add(pushRecord);
				}
			} else {
				flag = false;
			}
		}
		// ��������
		if (flag) {
			push.yinianPushToSingle(pushList);
		}
		return flag;
	}

	/**
	 * ��Ա����ٷ��������֪ͨ������
	 * 
	 * @param groupid
	 * @param userid
	 * @return
	 */
	public boolean enterOfficialAlbumNotification(String groupid, String userid) {
		// ����list
		List<Record> pushList = new ArrayList<Record>();
		boolean flag = true;
		// ��ȡ������
		Record record = Db.findFirst("select gtype,gname from groups where groupid = " + groupid + " ");
		record.set("groupid", groupid);
		// ��������ת��������
		List<Record> list = new ArrayList<Record>();
		list.add(record);
		list = dataProcess.changeGroupTypeIntoWord(list);
		record = list.get(0);
		// ����֪ͨ����
		String content = dataProcess.getNotificationContent(record, "enter");
		// ����֪ͨ����
		Notification notification = new Notification().set("nsender", Integer.parseInt(userid))
				.set("nreceiver", CommonParam.superUserID).set("ncontent", content).set("ntype", 1)
				.set("ngroupid", groupid);
		if (notification.save()) {
			flag = true;
			// ֪ͨ�洢�ɹ���ͬʱ,����͸�����ݲ����뵽����list��
			String nid = notification.get("nid").toString();
			Record pushRecord = pushMessage.getPushRecord(userid, CommonParam.superUserID, groupid, content, nid,
					"notification", null);
			if (!((pushRecord.toJson()).equals("{}"))) {
				// Record��Ϊ��ʱ�����뵽list��
				pushList.add(pushRecord);
			}
		} else {
			flag = false;
		}
		// ��������
		if (flag) {
			push.yinianPushToSingle(pushList);
		}
		return flag;
	}

	/**
	 * ������ͬ�������˽�����
	 * 
	 * @param groupid
	 * @param applyUserid
	 * @param useridList
	 * @return
	 */
	@Before(Tx.class)
	public boolean agreeJoinGroup(String groupid, String applyUserid, List<Record> useridList) {
		// ����list
		List<Record> pushList = new ArrayList<Record>();
		boolean flag1 = true;
		boolean flag2 = true;
		// ��ȡ����Ϣ
		Record record = Db.findFirst("select gtype,gname from groups where groupid = " + groupid + " ");
		record.set("groupid", groupid);
		// ��������ת��������
		List<Record> list = new ArrayList<Record>();
		list.add(record);
		list = dataProcess.changeGroupTypeIntoWord(list);
		record = list.get(0);
		// ����֪ͨ����
		String successContent = dataProcess.getNotificationContent(record, "success");// ��������
		String enterContent = dataProcess.getNotificationContent(record, "enter");// ������������Ա
		// ������������Ա������������˽����֪ͨ����
		for (Record user : useridList) {
			int userID = Integer.parseInt(user.get("gmuserid").toString());
			Notification notification = new Notification().set("nsender", Integer.parseInt(applyUserid))
					.set("nreceiver", userID).set("ncontent", enterContent).set("ntype", 1).set("ngroupid", groupid);
			if (notification.save()) {
				flag1 = true;
				// ֪ͨ�洢�ɹ���ͬʱ,����͸�����ݲ����뵽����list��
				String nid = notification.get("nid").toString();
				Record pushRecord = pushMessage.getPushRecord(applyUserid, user.get("gmuserid").toString(), groupid,
						enterContent, nid, "notification", null);
				if (!((pushRecord.toJson()).equals("{}"))) {
					// Record��Ϊ��ʱ�����뵽list��
					pushList.add(pushRecord);
				}
			} else {
				flag1 = false;
				break;
			}
		}
		// �������˲����ѳɹ������ϵͳ֪ͨ
		Notification notification = new Notification().set("nsender", CommonParam.systemUserID)
				.set("nreceiver", Integer.parseInt(applyUserid)).set("ncontent", successContent).set("ntype", 0)
				.set("ngroupid", groupid);
		if (notification.save()) {
			flag2 = true;
			// ֪ͨ�洢�ɹ���ͬʱ,����͸�����ݲ����뵽����list��
			String nid = notification.get("nid").toString();
			Record pushRecord = pushMessage.getPushRecord(CommonParam.systemUserID, applyUserid, groupid,
					successContent, nid, "agree", null);
			if (!((pushRecord.toJson()).equals("{}"))) {
				// Record��Ϊ��ʱ�����뵽list��
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
	 * ��ȡ�µ�֪ͨ
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
		// ��֪ͨ�б��е�ÿ��Ԫ�ص�״̬�޸�Ϊ�Ѷ�
		for (Record record : list) {
			Db.update(" update notifications set nstatus=1 where nid=" + record.get("nid").toString() + " ");
		}
		return list;
	}

	/**
	 * ��ȡ�µġ��ҵġ�
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
		List<Record> event = Db.find(SqlForEvent);// ��ȡ�¼��������Ϣ
		List<Record> comment = Db.find(commentSql);// ��ȡ�¼���������Ϣ
		comment = dataProcess.encapsulationCommentList(comment);// ��װ�����ڵ��û���
		List<Record> list = dataProcess.combieEventAndComment(event, comment);
		// ��װ�¼��ͱ�ǩ
		list = dataProcess.combineEventWithTags(list);
		// ��װ�¼��Ϳ�Ƭ��ʽ
		list = dataProcess.combineEventWithCardStyle(list);
		list = dataProcess.ChangePicAsArray(list);// �޸�upic�ֶθ�ʽ
		return list;
	}

	/**
	 * ��ȡ�µ���Ϣ�б�
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
		// ��ȡ������Ϣ�б�
		List<Record> messageList = Db.find(SqlForMessage);
		// ��������Ϣ�б��е�Ԫ�ص�״̬��Ϊ�Ѷ�
		for (Record record : messageList) {
			Db.update(" update messages set mstatus=1 where mid=" + record.get("mid").toString() + " ");
		}
		// ��ȡ���������Ϣ�б�
		List<Record> inviteList = Db.find(sqlForInvite);
		List<Record> list = dataProcess.combineTwoList(inviteList, messageList);
		return list;
	}

	/**
	 * ������Ϣ���ȴ�����
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
		// ��Ҫ֪ͨ�ĵ绰�����ֳ�����
		String[] phoneArray = messagePhone.split(",");
		// �����������
		String content = "���������" + messageInfo.getStr("type").toString() + "��"
				+ messageInfo.getStr("groupName").toString() + "��";
		// ��������wait����
		for (int i = 0; i < phoneArray.length; i++) {
			Wait wait = new Wait().set("wsender", userid).set("wphone", phoneArray[i]).set("wcontent", content)
					.set("wgroupid", groupid);
			// ����һ��ʧ�ܣ�����ʧ��
			if (!wait.save()) {
				flag = false;
			}
		}
		return flag;
	}

	/**
	 * ����֪ͨ����������
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
	 * ����ϵͳ��Ϣ
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
	 * �޸���ͷ��
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
	 * �жϵ绰������û��Ƿ���������
	 * 
	 * @param notifyPhone
	 * @return
	 */
	public String judgeUserInGroup(String groupid, String notifyPhone) {
		// ����ַ���
		String[] phoneArray = notifyPhone.split(",");
		String notInGroupPhone = ""; // �������ڵĳ�Ա�ĵ绰
		// ��ȡ���ڳ�Ա��ID
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
		// �ַ�������
		if (!notInGroupPhone.equals("")) {
			notInGroupPhone = notInGroupPhone.substring(0, notInGroupPhone.length() - 1);
		}
		return notInGroupPhone;
	}

	/**
	 * ɾ������
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
	 * �˳����
	 * 
	 * @param userid
	 * @param groupid
	 * @return
	 */
	@Before(Tx.class)
	public boolean quitAlbum(String userid, String groupid, String source) {

		// �����б�
		List<Record> pushList = new ArrayList<Record>();

		// 1.ֱ�ӽ���Ա��Ϣɾ��
		// ��ȡ����Ա�ڳ�Ա���еı��
		List<Record> userRecord = Db
				.find("select gmid from groupmembers where gmuserid=" + userid + " and gmgroupid=" + groupid + " ");

		int gmid = userRecord.size() == 0 ? 0 : Integer.parseInt(userRecord.get(0).get("gmid").toString());
		if (gmid == 0)
			return true;

		GroupMember gm = new GroupMember().set("gmid", gmid).set("gmuserid", Integer.parseInt(userid)).set("gmgroupid",
				Integer.parseInt(groupid));

		boolean deleteFlag = gm.delete();

		// 2.�޸����Ա��������һ
		int count = Db.update("update groups set gnum=gnum-1 where groupid=" + groupid + " ");

		// 3.������Ա�����Ķ�̬�������������
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
			// 4.������������Ա����֪ͨ,typeΪ10.11ʱ����Ҫ
			Group group = new Group().findById(groupid);
			int gtype = Integer.parseInt(group.get("gtype").toString());
			if (gtype != 10 && gtype != 11) {
				// ��ȡ��Ҫ����֪ͨ�������û���ID
				List<Record> UserID = getGroupMemberID(groupid);
				// ��ȡ֪ͨ���������
				Record contentRecord = Db.findFirst("select gtype,gname from groups where groupid=" + groupid + " ");
				// ��ȡ֪ͨ����
				String content = dataProcess.getNotificationContent(contentRecord, "quit");

				// ����֪ͨ����
				for (Record record : UserID) {
					int receiverID = Integer.parseInt(record.get("gmuserid").toString());
					Notification notification = new Notification().set("nsender", userid).set("nreceiver", receiverID)
							.set("ncontent", content).set("ntype", 3).set("ngroupid", groupid);
					if (notification.save()) {
						notificationFlag = true;
						// ֪ͨ�洢�ɹ���ͬʱ,����͸�����ݲ���������
						String nid = notification.get("nid").toString();
						Record pushRecord = pushMessage.getPushRecord(userid, record.get("gmuserid").toString(),
								groupid, content, nid, "notification", null);
						if (!((pushRecord.toJson()).equals("{}"))) {
							// Record��Ϊ��ʱ�����뵽list��
							pushList.add(pushRecord);
						}
					} else {
						notificationFlag = false;
						break;
					}
				}
			}
		}
		// 5.ɾ������ʱ����е�����
		Db.update("update lovertimemachine set ltmStatus=1 where ltmGroupID=" + groupid + " and ltmUserID=" + userid
				+ " ");
		// // 6.�û��˳�IM����Ⱥ��
		// im.RemoveChatGroupMember(userid, groupid);

		if (deleteFlag && eventFlag && notificationFlag && count == 1) {
			// �ɹ���������
			push.yinianPushToSingle(pushList);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * �˳��ٷ����
	 * 
	 * @param userid
	 * @param groupid
	 */
	@Before(Tx.class)
	public boolean quitOfficialAlbum(String userid, String groupid) {

		// �����б�
		List<Record> pushList = new ArrayList<Record>();

		// 1.ֱ�ӽ���Ա��Ϣɾ��
		// ��ȡ����Ա�ڳ�Ա���еı��
		Record userRecord = Db.findFirst(
				"select gmid from groupmembers where gmuserid=" + userid + " and gmgroupid=" + groupid + " ");
		int gmid = Integer.parseInt(userRecord.get("gmid").toString());
		GroupMember gm = new GroupMember().set("gmid", gmid).set("gmuserid", Integer.parseInt(userid)).set("gmgroupid",
				Integer.parseInt(groupid));
		boolean deleteFlag = gm.delete();
		// 2.�޸����Ա��������һ
		int count = Db.update("update groups set gnum=gnum-1 where groupid=" + groupid + " ");
		// 3.������Ա�����Ķ�̬�������������
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
		// 4.ɾ��likes���е�����
		boolean deleteLikes = true;
		// ��ȡ���ڸ��������еĶ�̬ID
		List<Record> eventsList = Db.find("select eid from events where egroupid=" + groupid + " and estatus in (0,3)");
		for (Record eventRecord : eventsList) {
			Record record = Db.findFirst("select lid from likes where leid=" + eventRecord.get("eid").toString()
					+ " and luserid=" + userid + " ");
			Likes like = new Likes().set("lid", record.get("lid").toString());
			deleteLikes = like.delete();
		}

		// 5.������Ա����֪ͨ����������
		boolean notificationFlag = true;
		// ��ȡ֪ͨ���������
		Record contentRecord = Db.findFirst("select gtype,gname from groups where groupid=" + groupid + " ");
		// ��ȡ֪ͨ����
		String content = dataProcess.getNotificationContent(contentRecord, "kick");

		// ֪ͨ����
		Notification notification = new Notification().set("nsender", userid).set("nreceiver", CommonParam.superUserID)
				.set("ncontent", content).set("ntype", 3).set("ngroupid", groupid);
		if (notification.save()) {
			notificationFlag = true;
			// ֪ͨ�洢�ɹ���ͬʱ,����͸�����ݲ���������
			String nid = notification.get("nid").toString();
			Record pushRecord = pushMessage.getPushRecord(userid, CommonParam.superUserID, groupid, content, nid,
					"notification", null);
			if (!((pushRecord.toJson()).equals("{}"))) {
				// Record��Ϊ��ʱ�����뵽list��
				pushList.add(pushRecord);
			}
		} else {
			notificationFlag = false;
		}
		if (deleteFlag && eventFlag && notificationFlag && count == 1 && deleteLikes) {
			// �ɹ���������
			push.yinianPushToSingle(pushList);
			return true;
		} else {
			return false;
		}

	}

	/**
	 * �߳����
	 * 
	 * @param userid
	 * @param groupid
	 */
	@Before(Tx.class)
	public boolean kickOutAlbum(String userid, String groupid) {

		// �����б�
		List<Record> pushList = new ArrayList<Record>();

		// 1.ֱ�ӽ���Ա��Ϣɾ��
		// ��ȡ����Ա�ڳ�Ա���еı��
		Record userRecord = Db.findFirst(
				"select gmid from groupmembers where gmuserid=" + userid + " and gmgroupid=" + groupid + " ");
		int gmid = Integer.parseInt(userRecord.get("gmid").toString());
		GroupMember gm = new GroupMember().set("gmid", gmid).set("gmuserid", Integer.parseInt(userid)).set("gmgroupid",
				Integer.parseInt(groupid));

		boolean deleteFlag = gm.delete();

		// 2.�޸����Ա��������һ
		int count = Db.update("update groups set gnum=gnum-1 where groupid=" + groupid + " ");

		// 3.������Ա�����Ķ�̬�������������
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

		// 4.�����߳��߷���֪ͨ����������
		boolean notificationFlag = true;
		// ��ȡ֪ͨ���������
		Record contentRecord = Db.findFirst("select gtype,gname from groups where groupid=" + groupid + " ");
		// ��ȡ֪ͨ����
		String content = dataProcess.getNotificationContent(contentRecord, "kick");

		// ֪ͨ����
		Notification notification = new Notification().set("nsender", CommonParam.systemUserID).set("nreceiver", userid)
				.set("ncontent", content).set("ntype", 4).set("ngroupid", groupid);
		if (notification.save()) {
			notificationFlag = true;
			// ֪ͨ�洢�ɹ���ͬʱ,����͸�����ݲ���������
			String nid = notification.get("nid").toString();
			Record pushRecord = pushMessage.getPushRecord(CommonParam.systemUserID, userid, groupid, content, nid,
					"notification", null);
			if (!((pushRecord.toJson()).equals("{}"))) {
				// Record��Ϊ��ʱ�����뵽list��
				pushList.add(pushRecord);
			}
		} else {
			notificationFlag = false;
		}
		if (deleteFlag && eventFlag && notificationFlag && count == 1) {
			// �ɹ���������
			push.yinianPushToSingle(pushList);
			return true;
		} else {
			return false;
		}

	}

	/**
	 * �޸��û�����ͼ
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
	 * ��ȡ���¶�̬���������
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
	 * �����û���cid
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
	 * ��֤�����Ƿ���ȷ
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
	 * ��ȡ���ڵ�������Ƭ
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
	 * ���ա��»�ȡ��Ƭǽ����
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
		// �����ڻ�ȡ��Ӧ����
		for (int i = 0; i < list.size(); i++) {
			String eids = list.get(i).get("eid").toString();
			List<Record> picList = Db.find(
					"select eid,euserid,pid,poriginal as url,eMain from events,pictures where peid=eid and eMain not in (4,5) and eid in("
							+ eids + ") and pstatus=0 ORDER BY pid DESC ");
			if (picList.size() != 0) {
				// ͼƬ��Ȩ
				picList = dataProcess.GetOriginAndThumbnailAccessWithDirectCut(picList, "url");

				// ��������
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
	 * ��ȡ��Ƭ����Ƶǽ
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
		// �����ڻ�ȡ��Ӧ����
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
				// ͼƬ��Ȩ
				picList = dataProcess.GetOriginAndThumbnailAccessWithDirectCut(picList, "url");
				
				// ��������
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
	 * ��ȡ��Ƭ����Ƶǽ
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
		// �����ڻ�ȡ��Ӧ����
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
			// ��Դ��Ȩ
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
	 * ��ȡ��Ƭ����Ƶǽ
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
			// �����ڻ�ȡ��Ӧ����
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
				// ��Դ��Ȩ
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
	 * ��ȡȫ����Ƭ����Ƶǽ
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
			// ͼƬ��Ȩ
			list = dataProcess.GetOriginAndThumbnailAccessWithDirectCutNew(list);
		}

		return list;
	}


	/**
	 * ���������
	 * 
	 * @param userid
	 * @param inviteCode
	 * @return
	 */
	public int applyJoinGroup(String userid, String inviteCode) {

		// ���ر�־
		int type; // 0--�ɹ� 1--�����벻���� 2--����ɾ�� 3--�û��Ѿ������� 4--������Ϣʧ�� 5--�ٷ����
		// �����б�
		List<Record> pushList = new ArrayList<Record>();

		// ͨ���������ȡ���������Ϣ
		List<Record> list = Db
				.find("select groupid,gcreator,gtype,gname,gstatus from groups where ginvite='" + inviteCode + "'  ");
		// �ж��������Ƿ����
		if (list.size() == 0) {
			type = 1;
			return type;
		}
		Record groupRecord = list.get(0);
		// �ж����Ƿ�ɾ��
		if ((groupRecord.get("gstatus").toString()).equals("1")) {
			type = 2;
			return type;
		}
		// �ж��������Ƿ��Ѿ�������
		boolean inGroupFlag = judgeUserInGroupByUserid(userid, groupRecord.get("groupid").toString());
		if (inGroupFlag) {
			type = 3;
			return type;
		}
		// �жϽ����������ͣ� ����ǹٷ����(gtypeΪ5)������5
		if ((groupRecord.get("gtype").toString()).equals("5")) {
			type = 5;
			return type;
		}
		// ����invitegroup����
		String content = "�������˽�ܿռ䡰" + groupRecord.getStr("gname") + "��";
		// ����֪ͨ��Ϣ
		InviteGroup invite = new InviteGroup().set("igsender", userid).set("igreceiver", groupRecord.get("gcreator"))
				.set("igcontent", content).set("iggroupid", groupRecord.get("groupid")).set("type", 2);
		if (invite.save()) {
			type = 0;
			// ��ȡID
			String igid = invite.get("igid").toString();
			// ��ȡ����Record
			Record pushRecord = pushMessage.getPushRecord(userid, groupRecord.get("gcreator").toString(),
					groupRecord.get("groupid").toString(), content, igid, "applyIntoGroup", null);
			if (!((pushRecord.toJson()).equals("{}"))) {
				// Record��Ϊ��ʱ�����뵽list��
				pushList.add(pushRecord);
			}
		} else {
			type = 4;
			return type;
		}
		// ��������
		push.yinianPushToSingle(pushList);
		return type;
	}

	/**
	 * �ж��û��Ƿ�������
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
	 * ��ȡ��������վ����
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
	 * ����
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
	 * ȡ������
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
	 * �ϴ���̬��������ݵ����ޱ���
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
	 * �û�����ٷ����ʱ�������ݵ����ޱ���
	 * 
	 * @param userid
	 * @param groupid
	 * @return
	 */
	@Before(Tx.class)
	public boolean newUserJoinInsertLikes(String userid, String groupid) {
		boolean flag = true;
		// ��ȡ�������еĶ�̬
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
	 * ��ȡ���˻�����Ϣ
	 * 
	 * @throws ParseException
	 */
	public Record getPersonalMemory(String userid) throws ParseException {
		Record userRecord = Db.findFirst(
				"select unickname as uname,utime,count(*) as gnum from users,groupmembers where userid=gmuserid and userid="
						+ userid + " and gmstatus=0 ");
		// ��ȡ��ǰ������
		Date now = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		// �û�ע��ʱ��
		String registerDate = (userRecord.get("utime").toString()).substring(0, 10);
		Date register = sdf.parse(registerDate);
		long day = (now.getTime() - register.getTime()) / (24 * 60 * 60 * 1000) > 0
				? (now.getTime() - register.getTime()) / (24 * 60 * 60 * 1000)
				: (register.getTime() - now.getTime()) / (24 * 60 * 60 * 1000);
		userRecord.set("ucount", day);

		// ��ȡ������
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
	 * ���뷴��
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
	 * ���������Ϣ�������ۡ����롢֪ͨ���֣�
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
	 * ɾ��������Ƭ
	 * 
	 * @param pid
	 * @return
	 */
	public boolean deleteSinglePhoto(String address, String eid) {
		int count = 0;
		// �ж��Ƿ�Ϊ��̬�����һ����Ƭ���ǵĻ�����Ӧ�Ķ�̬ɾ��
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
	 * ֪ͨ�����͸�������
	 * 
	 * @param sender
	 * @param receiver
	 * @param type
	 * @param groupid
	 * @param isPush
	 *            yes--���� no--������
	 * @return
	 */
	@Before(Tx.class)
	public boolean notifyAndPushToSingle(String sender, String receiver, String type, String groupid, String isPush) {
		boolean notificationFlag = false;
		// ��ȡ֪ͨ���������
		Record contentRecord = Db.findFirst("select gtype,gname from groups where groupid=" + groupid + " ");
		// �����б�
		List<Record> pushList = new ArrayList<Record>();
		// ֪ͨ����
		String content = "";
		// ֪ͨ����
		Notification notification = new Notification();
		switch (type) {
		case "contribute":
			// ��ȡ֪ͨ����
			content = dataProcess.getNotificationContent(contentRecord, "contribute");
			notification.set("nsender", sender).set("nreceiver", receiver).set("ncontent", content).set("ntype", 5)
					.set("ngroupid", groupid);

			break;
		case "acceptContribution":
			// ��ȡ֪ͨ����
			content = dataProcess.getNotificationContent(contentRecord, "acceptContribution");
			notification.set("nsender", sender).set("nreceiver", receiver).set("ncontent", content).set("ntype", 6)
					.set("ngroupid", groupid);
			break;
		case "refuseContribution":
			// ��ȡ֪ͨ����
			content = dataProcess.getNotificationContent(contentRecord, "refuseContribution");
			notification.set("nsender", sender).set("nreceiver", receiver).set("ncontent", content).set("ntype", 7)
					.set("ngroupid", groupid);
			break;
		default:
			return false;
		}
		if (notification.save()) {
			notificationFlag = true;
			// �ж��Ƿ��������
			if (isPush.equals("yes")) {
				String nid = notification.get("nid").toString();
				Record pushRecord = pushMessage.getPushRecord(sender, receiver, groupid, content, nid, "notification",
						null);
				if (!((pushRecord.toJson()).equals("{}"))) {
					// Record��Ϊ��ʱ�����뵽list��
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
	 * ��ʾͶ�����
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
		List<Record> event = dataProcess.encapsulationEventList(Db.find(sqlForContributeEvents));// ��ȡ�¼��������Ϣ����װ������û�����
		// ��װ�¼��ͱ�ǩ
		event = dataProcess.combineEventWithTags(event);
		// ��װ�¼��Ϳ�Ƭ��ʽ
		event = dataProcess.combineEventWithCardStyle(event);

		// ͼƬ��Ȩ
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
	 * ���Ͷ��
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
			// ��̬��״̬��Ϊ���ͨ��
			count = Db.update("update events set estatus=3 where eid=" + eventID + " ");
			// ��Ͷ���߷�������
			boolean flag1 = notifyAndPushToSingle(CommonParam.superUserID, publishUserID, "acceptContribution", groupid,
					"yes");
			// ���ͨ����������ݵ�likes����
			// ��ȡ�������г�Ա��ID
			List<Record> memberList = getGroupMemberID(groupid);
			// �������ݵ�likes����
			boolean flag = insertDataIntoLikes(eventID, memberList);
			if (flag && flag1) {
				jsonString = getSingleEventInOfficialAlbum(CommonParam.superUserID, eventID);// ��ȡ��̬����Ϣ
			} else {
				jsonString = jsonData.getJson(-50, "��������ʧ��");
			}

			break;
		case "refuse":
			// �޸Ķ�̬״̬
			count = Db.update("update events set estatus=4 where eid=" + eventID + " ");
			// ��Ͷ���߷�������
			boolean flag2 = notifyAndPushToSingle(CommonParam.superUserID, publishUserID, "refuseContribution", groupid,
					"yes");
			if (flag2 && count == 1) {
				jsonString = jsonData.getJson(2001, "Ͷ�����δͨ��");
			} else {
				jsonString = jsonData.getJson(-50, "��������ʧ��");
			}
			break;
		default:
			break;
		}
		return jsonString;
	}

	/**
	 * ��ȡ��ҳbanner��Ϣ
	 * 
	 * @param userid
	 * @return
	 */
	public List<Record> getMainBanner(String userid) {
		List<Record> bannerList = Db.find("select bid,btitle,bpic,bdata,btype from banner where bstatus=0  ");
		for (Record banner : bannerList) {
			String type = banner.get("btype").toString();
			if (type.equals("2")) {
				// �ж��û��Ƿ��ڸùٷ�����ڣ�ͨ��isInAlbum�ֶη��� 0--���� 1--��
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
	 * ��ȡ���û���صĹٷ�����б�
	 * 
	 * @param userid
	 * @return
	 */
	public List<Record> getOfficialAlbumList(String userid) {

		// ��ȡ�ٷ������Ϣ
		List<Record> list = Db.find(
				"select groupid,gname,gpic,gnum,gtime,gtype,gcreator,ginvite,gintroduceText,gintroducePic from groups where gstatus=0 and gtype=5 and gname not in ("
						+ CommonParam.SchoolAlbumsName + ") order by gorder  ");
		// ��ȡ����Ƭ��
		List<Record> photoList = Db.find(
				"select groupid,count(*) as gpicnum from groups,events,pictures where peid=eid and groupid=egroupid and gstatus=0 and gtype=5 and estatus in(0,3) and pstatus=0 group by groupid");
		// ������Ƭ�����뵽����Ϣ��
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
		// ��������ת������Ӧ������
		list = dataProcess.changeGroupTypeIntoWord(list);
		// �ж��û��Ƿ��ڹٷ������ ������isInAlbum�ֶ� 0--���� 1--��
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
	 * ��ȡ���û���صĹٷ�����б�
	 * 
	 * @param userid
	 * @return
	 */
	public List<Record> getSchoolAlbumList(String userid) {
		// ��ȡУ԰�����Ϣ
		List<Record> list = Db.find(
				"select groupid,gname,gpic,gnum,gtime,gtype,gcreator,ginvite,gintroduceText,gintroducePic from groups where gstatus=0 and gtype=5 and gname in ("
						+ CommonParam.SchoolAlbumsName + ")  ");
		// ��ȡ����У԰�����Ƭ��
		List<Record> photoList = Db.find(
				"select groupid,count(*) as gpicnum from groups,events,pictures where peid=eid and groupid=egroupid and gstatus=0 and gtype=5 and estatus in(0,3) and pstatus=0 group by groupid");
		// ������Ƭ�����뵽����Ϣ��
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
		// ��������ת������Ӧ������
		list = dataProcess.changeGroupTypeIntoWord(list);
		// �ж��û��Ƿ��ڹٷ������ ������isInAlbum�ֶ� 0--���� 1--��
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
	 * ��ȡ�����ٷ����������Ϣ
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
			// ��ȡ����Ƭ��
			List<Record> photoList = Db.find(sqlForPhotos);
			// �ж��û��Ƿ��ڹٷ������
			List<Record> user = Db.find(sqlForJudge);
			if (user.size() == 0) {
				list.get(0).set("isInAlbum", 0);
			} else {
				list.get(0).set("isInAlbum", 1);
			}
			// ������Ƭ�����뵽����Ϣ��
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
			// ��������ת������Ӧ������
			list = dataProcess.changeGroupTypeIntoWord(list);
		}
		return list;
	}

	/**
	 * ��ʼ����ˢ�¡������û�δ����ʱ�Ĺٷ�����ڵĶ�̬
	 * 
	 * @param userid
	 * @param groupid
	 * @param eventid
	 * @param sign
	 * @return
	 */
	public String getOfficialAlbumEventsWhenUserNotIn(String groupid, String eventid, String sign) {
		// ������Ӧ��SQL���
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
		List<Record> event = dataProcess.encapsulationEventList(Db.find(sqlForEvent));// ��ȡ�¼��������Ϣ����װ������û�����
		if (event.size() != 0) {
			for (Record record : event) {
				record.set("lislike", 0);
			}
		}
		List<Record> comment = dataProcess.encapsulationCommentList(Db.find(sqlForComment));// ��ȡ�¼���������Ϣ����װ������û�����
		List<Record> list = dataProcess.combieEventAndComment(event, comment);// ƴ���¼�������
		// ��װ�¼��ͱ�ǩ
		list = dataProcess.combineEventWithTags(list);
		// ��װ�¼��Ϳ�Ƭ��ʽ
		list = dataProcess.combineEventWithCardStyle(list);
		list = dataProcess.ChangePicAsArray(list);// �޸�upic�ֶθ�ʽ
		jsonString = jsonData.getJson(0, "success", list);
		return jsonString;
	}

	/**
	 * �ж��û�΢�Ż�qq��ID
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
	 * ��΢�����ֻ�
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
	 * ΢��ע��
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
		SimpleDateFormat birthDf = new SimpleDateFormat("yyyy-MM-dd");// �������ڸ�ʽ
		String birth = birthDf.format(new Date()); // ��ȡ��ǰϵͳʱ�䣬���õ�ǰ����Ϊ����
		// �Ա��ж�
		int newSex;
		if (sex.equals("1")) {
			newSex = 1;
		} else {
			newSex = 0;
		}
		// ͷ���ж�
		if (pic == null || pic.equals("")) {
			//pic = CommonParam.yinianLogo;
			pic="http://7xlmtr.com1.z0.glb.clouddn.com/20180313_1.png";
		}
		// Ĭ�ϱ����ĵ�ַ
		String defaultBackground = CommonParam.qiniuOpenAddress + CommonParam.userDefaultBackground;
		User user = new User().set("uwechatid", id).set("usex", newSex).set("unickname", nickname).set("ubirth", birth)
				.set("upic", pic).set("ubackground", defaultBackground).set("usource", source)
				.set("uprovince", province).set("ucity", city).set("uversion", version).set("uport", port)
				.set("uloginSource", source).set("uFromUserID", fromUserID).set("uFromSpaceID", fromSpaceID)
				.set("uFromEventID", fromEventID).set("uopenid", openID);

		String userid;
		// �û������Ѿ�ע��ɹ����������ݿ�����쳣������userid
		try {
			user.save();
			userid = user.get("userid").toString();
			return userid;
		} catch (ActiveRecordException e) {
			// �����û�ID������
			e.printStackTrace();
			userid = User.QueryUserLoginBasicInfo(id, "uwechatid").get(0).get("userid").toString();
			return userid;
		}

	}

	/**
	 * QQ�û�ע��
	 * 
	 * @param id
	 * @param nickname
	 * @param pic
	 * @param sex
	 * @return
	 */
	public String qqUserRegister(String id, String nickname, String pic, String sex) {
		String userid = "";
		SimpleDateFormat birthDf = new SimpleDateFormat("yyyy-MM-dd");// �������ڸ�ʽ
		String birth = birthDf.format(new Date()); // ��ȡ��ǰϵͳʱ�䣬���õ�ǰ����Ϊ����
		int newSex;
		if (sex.equals("��")) {
			newSex = 1;
		} else {
			newSex = 0;
		}
		String defaultBackground = CommonParam.qiniuOpenAddress + CommonParam.userDefaultBackground;// Ĭ�ϱ����ĵ�ַ
		User user = new User().set("uqqid", id).set("usex", newSex).set("unickname", nickname).set("ubirth", birth)
				.set("upic", pic).set("ubackground", defaultBackground);
		// �û�ID��ע��ɹ�������Ӧ��ֵ��ע��ʧ��Ϊ���ַ���
		if (user.save()) {
			userid = user.get("userid").toString();
			return userid;
		} else {
			return "";
		}

	}

	/**
	 * ��ȡ��ҳ�������
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
			comment = dataProcess.encapsulationCommentList(Db.find(sqlForComment));// ��ȡ�¼���������Ϣ����װ������û�����
		} else {
			sqlForEvent = "select eid,egroupid,userid,unickname,upic,etext,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\" ) as url from users,events,pictures where userid=euserid and eid=peid and egroupid="
					+ groupid + " and estatus in(0,3) and pstatus=0 group by peid DESC limit 2";
		}
		event = dataProcess.encapsulationEventList(Db.find(sqlForEvent));// ��ȡ�¼��������Ϣ����װ������û�����
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
			list = dataProcess.combieEventAndComment(event, comment);// ƴ���¼�������
		} else {
			list = event;
		}
		list = dataProcess.ChangePicAsArray(list);// �޸�upic�ֶθ�ʽ
		jsonString = jsonData.getJson(0, "success", list);
		return list;
	}

	// /**
	// * ��ȡ��ҳ�������
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
	// .find(sqlForComment));// ��ȡ�¼���������Ϣ����װ������û�����
	// } else {
	// sqlForEvent =
	// "select eid,userid,unickname,upic,etext,euploadtime,GROUP_CONCAT(poriginal
	// SEPARATOR \",\" ) as url from users,events,pictures where userid=euserid and
	// eid=peid and egroupid="
	// + groupid
	// + " and estatus in(0,3) and pstatus=0 group by peid DESC limit 2";
	// }
	// event = dataProcess.encapsulationEventList(Db.find(sqlForEvent));//
	// ��ȡ�¼��������Ϣ����װ������û�����
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
	// list = dataProcess.combieEventAndComment(event, comment);// ƴ���¼�������
	// } else {
	// list = event;
	// }
	// list = dataProcess.ChangePicAsArray(list);// �޸�upic�ֶθ�ʽ
	// jsonString = jsonData.getJson(0, "success", list);
	// return list;
	// }

	/**
	 * ����ͳ������
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
	 * ��ȡ��Ŀ�еĹ�����Ϣ
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
	 * �������
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
	 * ��ȡ�������
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
	 * ��ȡģ����Ϣ
	 * 
	 * @return
	 */
	public List<Record> getTempletInfo() {
		List<Record> list = Db.find(
				"select templetID,templetName,templetPic,templetUrl,templetProportion,templetDefaultMusicID,musicUrl as templetDefaultMusicUrl from templet,music where templetDefaultMusicID=musicID and templetStatus=0");
		return list;
	}

	/**
	 * ��ȡ������Ϣ
	 * 
	 * @return
	 */
	public List<Record> getMusicInfo() {
		List<Record> list = Db.find("select musicID,musicName,musicTempletID,musicUrl from music where musicStatus=0 ");
		return list;
	}

	/**
	 * �����������
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

		// ����ַ�ַ���ת��������
		String[] picArray = dataProcess.getPicAddress(picAddress, "private");

		// ��ȡ��̬��һ��ͼƬ��ַ
		String firstPic = picArray[0];

		// �����������
		MusicAlbum musicAlbum = new MusicAlbum();
		musicAlbum.set("maName", albumName).set("maCreatorID", userid).set("maContent", content)
				.set("maGroupID", groupid).set("maMusicID", musicid).set("maTempletID", templetid)
				.set("maCover", firstPic);
		if (musicAlbum.save()) {
			maID = musicAlbum.get("maID").toString();
			// ���¼��������״̬��Ϊ����״̬
			Db.update("update groupmembers set gmnotify=1 where gmgroupid =" + groupid + " ");
			// �����ߵ�״̬��Ϊ����״̬����Ϊ������˷����¶�̬�������߷�����������ڶ�̬����
			Db.update("update groupmembers set gmnotify=0 where gmgroupid =" + groupid + " and gmuserid = " + userid
					+ " ");

			// ����ͼƬ
			for (int i = 0; i < picArray.length; i++) {
				MAPicture maPic = new MAPicture();
				maPic.set("mapOriginal", picArray[i]).set("mapMusicAlbumID", maID);
				maPic.save();
			}
		}

		// ��������׼��
		// ��ȡ���������û���ID
		List<Record> useridList = Db.find("select gmuserid from groupmembers where gmgroupid = " + groupid + " ");
		// ȥ�������ߵ�ID
		for (Record record : useridList) {
			if ((record.get("gmuserid").toString()).equals(userid)) {
				useridList.remove(record);
				break;
			}
		}
		// useridList��Ϊnullʱ�Ž�������
		if (!(useridList.isEmpty())) {
			// ��list�е�IDƴ�ӳ��ַ���
			String userids = dataProcess.changeListToString(useridList, "gmuserid");
			// userids��Ϊ���ַ���ʱ�Ž�������
			if (!(userids.equals(""))) {
				// ��ȡҪ�������͵��û���cid
				Record cid = pushMessage.getUsersCids(userids, "userid");
				// ��ȡ��̬�����ߵ��ǳ�
				String nickname = dao.getUserSingleInfo(userid, "nickname");
				// ��ȡ������̬���������Ϣ
				List<Record> groupList = Db.find("select gname,gtype from groups where groupid = " + groupid + " ");
				groupList = dataProcess.changeGroupTypeIntoWord(groupList);
				// ƴ����������
				String pushContent = nickname + "��" + groupList.get(0).getStr("gtype") + "��"
						+ groupList.get(0).getStr("gname") + "���������µ��������";
				Record data = new Record().set("content", pushContent);
				// ����͸������
				int gid = Integer.parseInt(groupid);
				Record transmissionRecord = new Record().set("groupid", gid);
				List<Record> list = new ArrayList<Record>();
				list.add(transmissionRecord);
				String transmissionContent = jsonData.getJson(3, "����̬", list);
				data.set("transmissionContent", transmissionContent);
				// ����
				push.yinianPushToListWithAndroidNotification(cid, data);
			}
		}

		return maID;
	}

	/**
	 * ��ȡ�����������
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
	 * ��ȡʱ����������
	 * 
	 * @param userid
	 * @return
	 */
	public List<Record> getTimeCompass(String userid) {
		// ��ȡ�û��ڵ����зǹٷ����������Ϣ
		List<Record> albumList = Db
				.find("select groupid,gname,gtype from groups,groupmembers where groupid=gmgroupid and gmuserid="
						+ userid + " and gstatus=0 and gtype not in(5)  ");
		// ��������ת������
		albumList = dataProcess.changeGroupTypeIntoWord(albumList);
		// ��ȡ�û��ڵ��������е����ж�̬��Ϣ
		List<Record> dateList = Db.find(
				"select groupid,ememorytime,efirstpic from groups,groupmembers,events where groupid=gmgroupid and gmgroupid=egroupid and gtype not in (5) and gmuserid="
						+ userid + " and estatus=0 and gmstatus=0");
		for (Record albumRecord : albumList) {
			String albumGroupID = albumRecord.get("groupid").toString();
			String lastYear = "";// ���ڴ洢��һ��dateRecord�е����
			String lastMonth = "";// ���ڴ洢��һ��dateRecord�е����
			ArrayList<Record> list = new ArrayList<Record>();// list���ڴ洢��Ӧ��date��¼
			ArrayList<Record> temp = new ArrayList<Record>();// ��ʱlist���ڴ洢��albumRecord��ͬ���date��¼

			// ������albumRecord����id��ͬ�ļ�¼
			for (Record dateRecord : dateList) {
				String dateGroupID = dateRecord.get("groupid").toString();
				if (albumGroupID.equals(dateGroupID)) {
					temp.add(dateRecord);
				}
			}

			Record tempRecord;
			String minium;
			int index;
			// ���б��е�record������ʱ������
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

			String url = "";// �洢ͬһ�����µ�ͼƬ��url
			// ��ͬһ�����µ����ݹ�����һ��
			for (int i = 0; i < temp.size(); i++) {
				// ��ȡ�ꡢ��
				String time = temp.get(i).get("ememorytime").toString();
				String year = time.substring(0, 4);
				String month = time.substring(5, 7);
				if (i == 0) {
					// ����ʼֵ
					lastYear = year;
					lastMonth = month;
				}
				if (lastYear.equals(year) && lastMonth.equals(month)) {
					url += temp.get(i).get("efirstpic").toString() + ",";
					// �ж��ǲ����Ѿ������һ��Ԫ��
					if (i == temp.size() - 1) {
						url = url.substring(0, url.length() - 1);
						Record record = new Record().set("year", year).set("month", month).set("picAddress", url);
						list.add(record);
						// ����ѭ��
						break;
					}
				} else {
					// ��ֵ����list��
					url = url.substring(0, url.length() - 1);
					Record record = new Record().set("year", lastYear).set("month", lastMonth).set("picAddress", url);
					list.add(record);
					// ������ֵ
					lastYear = year;
					lastMonth = month;
					url = temp.get(i).get("efirstpic").toString() + ",";
					// �ж��ǲ��� �Ѿ������һ��Ԫ��
					if (i == temp.size() - 1) {
						url = url.substring(0, url.length() - 1);
						Record record1 = new Record().set("year", year).set("month", month).set("picAddress", url);
						list.add(record1);
						// ����ѭ��
						break;
					}
				}
			}
			// ����list�е�picAddress�ֶΣ�����ͼƬ��Ȩ��ͬʱ��������ͼ
			for (Record record : list) {
				String picAddress = record.get("picAddress").toString();
				String thumbnail = "";
				String[] array = picAddress.split(",");
				if (array.length == 1) {
					// ֻ��һ��ͼ����������Ĭ�ϵ�
					thumbnail = qiniu.getDownloadToken((array[0] + "?imageView2/2/w/300")) + ","
							+ CommonParam.timeCompassDefaultPicOne + "," + CommonParam.timeCompassDefaultPicTwo;
					array[0] = qiniu.getDownloadToken(array[0]);
					picAddress += "," + CommonParam.timeCompassDefaultPicOne + ","
							+ CommonParam.timeCompassDefaultPicTwo;

				}
				if (array.length == 2) {
					// ������ͼ���ټ�һ��Ĭ�ϵ�
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
				// ������ֵ
				record.set("picAddress", picAddress).set("thumbnail", thumbnail);
			}

			albumRecord.set("date", list);
		}

		return albumList;
	}

	/**
	 * ��ȡʱ�������ڵ���Ƭ
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
		// ��ȡͼƬ����Ȩ��
		list = dataProcess.GetOriginAndThumbnailAccess(list, "poriginal");
		if (pid.equals("0")) {
			// ��ʼ�����ж��Ƿ�����ʮ�ţ����򷵻�30�ţ�����ȫ������
			if (list.size() <= 30) {
				return list;
			} else {
				list = list.subList(0, 30);
			}
		} else {
			if (pid.equals("-1")) {
				// pidΪ1������������Ƭ
				return list;
			} else {
				int index = 0;
				// �ҵ�pid����Ӧ���±�
				for (int i = 0; i < list.size(); i++) {
					if ((list.get(i).get("pid").toString()).equals(pid)) {
						index = i;
						break;
					}
				}
				// �ж��±�
				if (index == list.size() - 1) {
					// ��ʾ�����һ����
					List<Record> temp = new ArrayList<Record>();
					return temp;
				} else {
					if (index + 30 >= list.size()) {
						// ��ʾ�ܰ�ʣ���ȫ������
						list = list.subList(index + 1, list.size());
					} else {
						// ��ʾֻ���ڷ���30��
						list = list.subList(index + 1, index + 31);
					}
				}
			}
		}
		return list;
	}

	/**
	 * ɾ���������
	 * 
	 * @param maID
	 * @return
	 */
	@Before(Tx.class)
	public boolean deleteMusicAlbum(String maID) {
		// ɾ��ͼƬ
		Db.update("update mapicture set mapStatus=1 where mapMusicAlbumID=" + maID + " ");
		// ɾ���������
		int count = Db.update("update musicalbum set maStatus=1 where maID=" + maID + " ");
		return count == 1;
	}

	/**
	 * �����û����һ�ε�¼����Ϣ��Ŀǰ�е�¼ʱ����û���Դ����
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
	 * �޸Ķ�̬�еĵ�����Ϣ
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
			// ����Ƭ��ʽ��ID
			event.set("ecardstyle", data);
			break;
		case "text":
			event.set("etext", data);
			break;
		case "audio":
			data = (CommonParam.qiniuOpenAddress + data);
			event.set("eaudio", data);
			// �����û�ʹ�õĴ洢�ռ�
			Double storage = Double.parseDouble(linkedData);
			String userid = event.get("euserid").toString();
			updateUserStoragePlace(userid, storage, "add");
			break;
		case "memorytime":
			// �������� ��ʽΪyyyy-MM-dd
			data += " 00:00:00";
			event.set("ememorytime", data);
			// ��ȡ�ö�̬Ŀǰ���е���Ƭ���޸�������Ƭ�Ļ���ʱ��
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
	 * ���Ӷ�̬�еĵ�����Ϣ
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
			// ��ȡ��̬�Ļ���ʱ�䣬������ͼƬ�Ļ���ʱ������һ��
			String memorytime = event.get("ememorytime").toString();
			// ���Ӷ���ͼƬ����Ӣ�ı��Ķ��Ÿ���
			String[] picAddress = data.split(",");
			for (int i = 0; i < picAddress.length; i++) {
				String url = CommonParam.qiniuOpenAddress + picAddress[i];
				Picture picture = new Picture().set("poriginal", url).set("peid", eventid).set("pmemorytime",
						memorytime).set("pGroupid", groupid).set("puserid", userid).set("pMain", event.get("eMain"));
				picture.save();
			}
			// �����û�ʹ�õĴ洢�ռ�
			Double storage = Double.parseDouble(linkedData);
			updateUserStoragePlace(userid, storage, "add");
			break;
		case "tag":
			// һ��ֻ������һ��tag
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
	 * ɾ����̬�ڵĵ�����Ϣ
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
			// dataΪ��tag��ID
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
	 * �����û����ô洢�ռ�
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
	 * ����ʷ��ǩ���д����¼���ı�ǩ
	 */
	public boolean handleTagsInHistoryTags(String userid, String tagContent) {
		boolean flag = false;
		// �жϱ��и��û��Ƿ񷢹�ͬ�����ݵı�ǩ�Ҹñ�ǩ����ʷ��¼û�б�ɾ��
		List<Record> list = Db.find("select historyTagContent from historytag where historyTagUserID=" + userid
				+ " and historyTagContent='" + tagContent + "' and historyTagType=1 ");
		if (list.size() > 0) {
			// ˵���ñ�ǩ����ʷ��ǩ��δɾ��
			flag = true;
		} else {
			HistoryTag tag = new HistoryTag().set("historyTagUserID", userid).set("historyTagContent", tagContent);
			flag = tag.save();
		}
		return flag;
	}

	/**
	 * ��ʼ�������ء�ˢ��˽������ڶ�̬
	 */
	public String getPrivateAlbumEvents(String userid, String groupid, String eventid, String sign) {

		int count = 1;
		// ������Ӧ��SQL���
		String sqlForEvent = "";
		String sqlForComment = "";
		switch (sign) {
		case "initialize":
			sqlForEvent = "select eid,egroupid,userid,unickname,upic,etext,eaudio,eaudiotime,ecover,ememorytime,ecardstyle,eplace,etype,eStoragePlace,euploadtime,GROUP_CONCAT(poriginal SEPARATOR \",\" ) as url from users,events,pictures where userid=euserid and eid=peid and egroupid="
					+ groupid + " and estatus=0 and pstatus=0 group by peid DESC limit 10";
			sqlForComment = CommonParam.selectForComment
					+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and egroupid="
					+ groupid + " and cstatus=0 ORDER BY ceid,ctime asc";
			// ��ʼ����ʱ��������״̬Ϊ���¶�̬
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
		// ��������
		List<Record> event = dataProcess.encapsulationEventList(Db.find(sqlForEvent));// ��ȡ�¼��������Ϣ����װ������û�����
		List<Record> comment = dataProcess.encapsulationCommentList(Db.find(sqlForComment));// ��ȡ�¼���������Ϣ����װ������û�����
		List<Record> list = dataProcess.combieEventAndComment(event, comment);// ƴ���¼�������
		// ��װ�¼��ͱ�ǩ
		list = dataProcess.combineEventWithTags(list);
		// ��װ�¼��Ϳ�Ƭ��ʽ
		list = dataProcess.combineEventWithCardStyle(list);
		// �޸�upic�ֶθ�ʽ
		list = dataProcess.ChangePicAsArray(list);

		jsonString = jsonData.getJson(0, "success", list);

		return jsonString;

	}

	/**
	 * ��ʾ������Ƭ���� ?imageView2/w/300
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
			// ��ȡͼƬ����Ȩ��
			photoList = dataProcess.GetOriginAndThumbnailAccessWithDirectCut(photoList, "backupPhotoURL");
			record.set("backupPhoto", photoList);
		}

		return eventList;
	}

	/**
	 * ���ݵ�����Ƭ
	 */
	@Before(Tx.class)
	public boolean backupSingleDayPhoto(String userid, String date, String photo, double storage, String hash) {
		boolean flag = false;
		// ͼƬ��ַ����
		String[] url = dataProcess.getPicAddress(photo, "secret");

		// �жϸ��û����������Ƿ��Ѿ��ϴ�����̬���ǵĻ�ֱ�Ӳ��뵽�ö�̬����
		List<Record> judge = Db.find("select backupEventID from backupevent where backupUserID=" + userid
				+ " and backupDate='" + date + "' and backupStatus=0 ");
		if (judge.size() == 0) {
			BackupEvent event = new BackupEvent().set("backupUserID", userid).set("backupDate", date)
					.set("backupStoragePlace", storage);
			if (event.save()) {
				String eid = event.get("backupEventID").toString();
				// �洢ͼƬ
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
				// �洢ͼƬ
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
	 * �޸���ͨ��̬
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
	 * �޸ļ�¼��Ƭ
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
	 * �޸�����ͼ��
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
	 * ����ͼƬ
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

		// �����ַ����������ݿ��ѯ
		String newPicAddress = "";
		for (int i = 0; i < picArray.length; i++) {
			newPicAddress += ("'" + picArray[i] + "',");
		}
		newPicAddress = newPicAddress.substring(0, newPicAddress.length() - 1);

		/**
		 * ɾ��ͼƬ
		 */
		// ��ѯҪɾ����ͼƬ��
		List<Record> list = Db.find("select substring_index(poriginal, '/', -1) as poriginal from pictures where peid="
				+ eid + " and substring_index(poriginal, '/', -1) not in (" + newPicAddress + ")  and pstatus=0");
		int size = list.size();
		// �޸�Ҫɾ����ͼƬ��״̬
		int count = Db.update("update pictures set pstatus=1 where peid=" + eid
				+ " and substring_index(poriginal, '/', -1) not in (" + newPicAddress + ")  and pstatus=0");
		// �ȶ�size��count�Ƿ�һ�£�һ�����ʾ�����ɹ�
		deleteFlag = (size == count);

		/**
		 * ����ͼƬ
		 */
		// ��ѯ��ɾ����ͼƬ
		List<Record> existList = Db
				.find("select substring_index(poriginal, '/', -1) as poriginal from pictures where peid=" + eid
						+ " and pstatus=0");
		// ת��������list��ȡ��Ҫ���ӵ�ͼƬ��ַlist
		List<String> oldList = new ArrayList<String>();
		for (Record record : existList) {
			oldList.add(record.get("poriginal").toString());
			
		}
		// ȥ������Ԫ��
		Iterator<String> it = newList.iterator();
		while (it.hasNext()) {
			if (oldList.contains(it.next())) {
				it.remove();
			}
		}

		//��ѯ��̬���������û�
		List<Record> eventList = Db.find("SELECT euserid,egroupid FROM `events` where eid="+eid);
		String groupid = eventList.get(0).get("egroupid").toString();
		String userid = eventList.get(0).get("euserid").toString();
		// �������ݿ�
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
	 * ���ñ�ǩ
	 */
	@Before(Tx.class)
	public boolean resetTag(String userid, String eid, String tag) {
		// �жϱ�ǩtags�Ƿ��д�ֵ��������в���
		if (tag != null) {
			// ͨ�� &nbsp ����������tag
			String[] eventTag = tag.split("&nbsp");
			// ���ԭ���ı�ǩ
			Db.update("update tags set tagStatus=1 where tagEventID=" + eid + " ");
			// �����ǩ������ÿ����ǩ������ʷ��ǩ����
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
	 * ת�ƶ�̬
	 */
	@Before(Tx.class)
	public List<Record> transferEvent(String userid, String eid, String groupid) {
		/** ������**/
//		User u=new User().findById(userid);
//		if(null!=u&&null!=u.get("ustate")&&u.get("ustate").toString().equals("1")){
//			Db.update("update pictures set poriginal='http://oibl5dyji.bkt.clouddn.com/Resource_violation_pic.jpg' where peid=" + eid);
//		}
		/** ������**/
		// ת�ƶ�̬
		Event event = new Event().findById(eid);
		// �¶�̬����ʱ��
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String time = sdf.format(new Date());
		event.set("euserid", userid).set("egroupid", groupid).set("euploadtime", time).set("isSynchronize", 1);
		event.remove("eid");
		if (event.save()) {
			// ����µ�eid
			String newEid = event.get("eid").toString();
			// ת����Ƭ
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
	 * ��ȡ�����Ϣ
	 */
	public String getSpaceInfo(String groupid) {
		Group group = new Group().findById(groupid);

		int status = Integer.parseInt(group.get("gstatus").toString());
		int gtype = Integer.parseInt(group.get("gtype").toString());
		// �ж�����Ƿ�ɾ��
		if (status == 0) {
			// ��ȡ��������
			Record record = new Record().set("gname", group.get("gname").toString())
					.set("gcreator", group.get("gcreator").toString()).set("gtype", gtype)
					.set("gnum", group.get("gnum").toString());
			// ��ȡ��Ա�б�
			List<Record> groupMember = Db.find(
					"select userid,unickname,upic,gmtime from users,groups,groupmembers where groupid=gmgroupid and users.userid=groupmembers.gmuserid and gmgroupid='"
							+ groupid + "' and gmstatus=0 limit 10 ");
			// ��ȡ��Ƭ��
			List<Record> photo = Db.find(
					"select count(*) as gpicNum from groups,events,pictures where peid=eid and groupid=egroupid and groupid="
							+ groupid + " and estatus in(0,3) and pstatus=0 ");
			record.set("picNum", photo.get(0).get("gpicNum").toString()).set("memberList", groupMember);

			List<Record> result = new ArrayList<Record>();
			result.add(record);
			jsonString = jsonData.getSuccessJson(result);
		} else if (status == 1) {
			jsonString = jsonData.getJson(1012, "����ѱ�ɾ��");
		} else {
			jsonString = jsonData.getJson(1037, "����ѱ���");
		}
		return jsonString;
	}
	/**
	 * ��ʾ���б�
	 * 
	 * @param userid model by lk mode=create Ϊ�Լ���������ᣬjoinΪ�Լ���������
	 * @return
	 */
	public String showGroupByCreateOrJoin(int userid ,String mode) {
		List<Record> list = Group.GetUnOrderSpaceBasicInfoByCreateOrJoin(userid,mode);
		// ��װ�ռ�����
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
					jsonString = jsonData.getJson(2, "��������");
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
	 * ����ö���ȡ���ö�
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
			jsonString = jsonData.getJson(-50, "��������");
			return jsonString;
		}
		
		//System.out.println(top);
		member = new GroupMember().findById(gmid);
		if(isTop.equals("yes")){			
			if(top==0){
				Timestamp timestamp = new Timestamp(System.currentTimeMillis());
				member.set("isTop", 1).set("topTime", timestamp);
				if(!member.update()) {
					jsonString = jsonData.getJson(-50, "��������");
				}
			}
		}else{
			if(top==1){
				member.set("isTop", 0).set("topTime", null);
				//System.out.println("������");
				if(!member.update()) {
					jsonString = jsonData.getJson(-50, "��������");
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
					jsonString = jsonData.getJson(-50, "��������");
				}
			}
		}else{
			if(top==1){
				member.set("isTop", 0).set("topTime", null);
				//System.out.println("������");
				if(!member.update()) {
					jsonString = jsonData.getJson(-50, "��������");
				}
			}
		}
		jsonString = jsonData.getJson(0, "success");
		return jsonString;
	}*/
	
	/**
	 * ��ʾ���б� ��ʾ�Ƿ��ö� by lk  ����С����ר��
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
			//��ȡ��Ƶ���棬���û�з��棬���ȡ���һ���ϴ���ͼƬ
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
			// ��װ�ռ�����
			list = dataProcess.spaceDataEncapsulation(topIsFirst, userid);
			jsonString = jsonData.getJson(0, "success", list);
			return jsonString;

	}
	
	/**
	 * ��ʾ���б� ��ʾ�Ƿ��ö�   ����С����ר��
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
	 * ��ʾ���б� ��ʾ�Ƿ��ö� by lk 
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
			// ��װ�ռ�����
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
//		// ��װ�ռ�����
//		list = dataProcess.spaceDataEncapsulation(topIsFirst, userid);
//		jsonString = jsonData.getJson(0, "success", list);
//		return jsonString;
	}
	/**
	 * ��ʾ���б� ��ʾ�Ƿ��ö� ��ҳ ����ʾͼƬ�����û��� by ly  
	 * 
	 * @param userid 
	 * @return
	 */
	public String showNoPicGroupWithTopNew(int userid,int pagenum,String type) {
		List<Record> toplist = new ArrayList<>();
		String actGroupids = CacheKit.get("DataSystem", "showGroupWithTopNew_actGroupids");
		if(actGroupids == null) {
			//����Ϊ��  �����ݿ��ѯ
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
	 * ��ʾ���б� ��ʾ�Ƿ��ö� ��ҳ by ly  
	 * 
	 * @param userid 
	 * @return
	 */
	public String showGroupWithTopNew(int userid,int pagenum,String type) {
		List<Record> toplist = new ArrayList<>();
		String actGroupids = CacheKit.get("DataSystem", "showGroupWithTopNew_actGroupids");
		if(actGroupids == null) {
			//����Ϊ��  �����ݿ��ѯ
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
	 * �ϴ�Ĭ�϶�̬���ɹ��󷵻��¼���ID eOrigin=1 by lk
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

		// ����ַ�ַ���ת��������
		String[] picArray = dataProcess.getPicAddress(picAddress, mode);

		// ͼƬ����
		picArray = dataProcess.PictureVerify(picArray);

		// ��ȡ��̬��һ��ͼƬ��ַ
		String firstPic = picArray[0];

		// ��ռ�ÿռ������תΪdouble
		Double place = Double.parseDouble(storage);

		String newTime = "";
		// �����¼�
		Event event = new Event();
		event.set("eOrigin", 1);
		// �ж�memorytime�ֶ��Ƿ��д�����
		if (memorytime == null || memorytime.equals("")) {
			event.set("egroupid", groupid).set("euserid", userid).set("etext", content).set("efirstpic", firstPic)
					.set("eStoragePlace", place).set("eSource", source).set("isSynchronize", 1);
		} else {
			// ƴ���ַ���
			newTime = memorytime + " 00:00:00";
			event.set("egroupid", groupid).set("euserid", userid).set("etext", content).set("efirstpic", firstPic)
					.set("ememorytime", newTime).set("eStoragePlace", place).set("eSource", source)
					.set("isSynchronize", 1);
		}

		// �ж�mode�ǲ��ǵ���dayMark ����ǩ���������etypeΪ3
		if (mode.equals("dayMark")) {
			event.set("etype", 3);
		}

		// �ж�λ����Ϣ
		if (location != null && !location.equals("")) {
			event.set("eplace", location);
		}

		if (event.save()) {
			eventID = event.get("eid").toString();

			// ���¼��������״̬��Ϊ����״̬
			// Db.update("update groupmembers set gmnotify=1 where gmgroupid ="
			// + groupid + " ");
			// // �����ߵ�״̬��Ϊ����״̬����Ϊ������˷����¶�̬�������߷�����������ڶ�̬����
			// Db.update("update groupmembers set gmnotify=0 where gmgroupid ="
			// + groupid + " and gmuserid = " + userid + " ");

			// �ж�memorytime�ֶ��Ƿ��д�����
			if (memorytime == null || memorytime.equals("")) {
				// ����ͼƬ
				for (int i = 0; i < picArray.length; i++) {
					Picture pic = new Picture();
					pic.set("peid", eventID).set("poriginal", picArray[i])
						.set("pGroupid", groupid).set("puserid", userid);
					pic.save();
				}
			} else {
				// newTime����Ҫ��ƴ��
				// ����ͼƬ
				for (int i = 0; i < picArray.length; i++) {
					Picture pic = new Picture();
					pic.set("peid", eventID).set("poriginal", picArray[i]).set("pmemorytime", newTime).set("pGroupid", groupid).set("puserid", userid);
					pic.save();
				}
			}

		}

		// �����û���ʹ�ÿռ�
		if (place != 0) {
			updateUserStoragePlace(userid, place, "add");
		}

		// // ��������׼��
		// // ��ȡ���������û���ID
		// List<Record> useridList = Db
		// .find("select gmuserid from groupmembers where gmgroupid = "
		// + groupid + " ");
		// // ȥ�������ߵ�ID
		// for (Record record : useridList) {
		// if ((record.get("gmuserid").toString()).equals(userid)) {
		// useridList.remove(record);
		// break;
		// }
		// }
		// // useridList��Ϊnullʱ�Ž�������
		// if (!(useridList.isEmpty())) {
		// // ��list�е�IDƴ�ӳ��ַ���
		// String userids = dataProcess.changeListToString(useridList,
		// "gmuserid");
		// // userids��Ϊ���ַ���ʱ�Ž�������
		// if (!(userids.equals(""))) {
		// // ��ȡҪ�������͵��û���cid
		// Record cid = pushMessage.getUsersCids(userids, "userid");
		// // ��ȡ��̬�����ߵ��ǳ�
		// String nickname = dao.getUserSingleInfo(userid, "nickname");
		// // ��ȡ������̬���������Ϣ
		// List<Record> groupList = Db
		// .find("select gname,gtype from groups where groupid = "
		// + groupid + " ");
		// groupList = dataProcess.changeGroupTypeIntoWord(groupList);
		// // ƴ����������
		// String pushContent = nickname + "��˽�ܿռ䡰"
		// + groupList.get(0).getStr("gname") + "���������¶�̬";
		// Record data = new Record().set("content", pushContent);
		// // ����͸������
		// int gid = Integer.parseInt(groupid);
		// Record transmissionRecord = new Record().set("groupid", gid)
		// .set("pushContent", pushContent)
		// .set("gname", groupList.get(0).getStr("gname"));
		// List<Record> list = new ArrayList<Record>();
		// list.add(transmissionRecord);
		// String transmissionContent = jsonData.getJson(3, "����̬", list);
		// data.set("transmissionContent", transmissionContent);
		// // ����
		// push.yinianPushToList(cid, data);
		// }
		// }
		return eventID;

	}	

}