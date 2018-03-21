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

	private YinianDataProcess dataProcess = new YinianDataProcess();// ���ݴ�����
	private QiniuOperate qiniu = new QiniuOperate();
	private UserService userService = new UserService();// �û�ҵ���߼���
	/**
	 * �ϴ���̬���ɹ��󷵻��¼���ID by lk ���ͼƬpGroupid�ֶ�
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

		// �жϵ�ͼͼƬ�������Ƿ����ϴ�
		if (audio != null && !audio.equals("")) {
			audio = CommonParam.qiniuPrivateAddress + audio;
		}
		if (placePic != null && !placePic.equals("")) {
			placePic = CommonParam.qiniuPrivateAddress + placePic;
		}

		// �����¼�
		Event event = new Event().set("egroupid", groupid).set("euserid", userid).set("efirstpic", firstPic)
				.set("etext", content).set("eaudio", audio).set("eplace", place).set("ePlacePic", placePic)
				.set("eMain", main).set("ePeopleName", peopleName).set("eStoragePlace", storage).set("eSource", source)
				.set("isSynchronize", isSynchronize);

		// ������ֵ�ж�
		if (placeLongitude != null && placeLatitude != null && !placeLongitude.equals("")
				&& !placeLatitude.equals("")) {
			event.set("ePlaceLongitude", placeLongitude).set("ePlaceLatitude", placeLatitude);
		}

		if (event.save()) {
			eventID = Integer.parseInt(event.get("eid").toString());

			// app�Ÿ���״̬ //lk ��Ӿ����С�����ж� like С����
			if (source != null && !source.equals("С����") &&!source.equals("�����С����")&& !source.equals("����")) {
				// ���¼��������״̬��Ϊ����״̬
				gm.UpdateAllGroupMembersNewDynamic(groupid);
				// �����ߵ�״̬��Ϊ����״̬����Ϊ������˷����¶�̬�������߷�����������ڶ�̬����
				gm.UpdateSingleGroupMenberNoNewDynamic(groupid, userid);
			}

			// ��ͼƬ�򱣴�ͼƬ
			for (int i = 0; i < picArray.length; i++) {
				Picture pic = new Picture();
				//by lk test
				pic.set("peid", eventID).set("poriginal", picArray[i]).set("pGroupid", groupid).set("pUserid", userid).set("pMain", main);
				pic.save();
			}

		}

		// �����û���ʹ�ÿռ�
		User user = new User();
		user.updateUserStoragePlace(userid, storage, "add");

		// APP����
		if (isPush != null && isPush.equals("yes")) {
			PushMessage push = new PushMessage();
			push.pushToSpaceMember(groupid, userid, push.PUSH_TYPE_OF_EVENT);
		}
		// С��������
		if (source != null && source.equals("С����") && isPush != null && isPush.equals("true")) {
			SmallAppPush.UploadPush(groupid, userid, formID, picArray.length);
		}
		// �����С�������� by lk 
				if (source != null && source.equals("�����С����") && isPush != null && isPush.equals("true")) {
					SmallAppPush.UploadPush(groupid, userid, formID, picArray.length);
				}

		return eventID;

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
	public int upload(String userid, String groupid, String[] picArray, String content, String audio, String place,
			String placePic, String placeLongitude, String placeLatitude, String peopleName, String main,
			double storage, String firstPic, String isPush, String source, int isSynchronize, String formID) {

		int eventID = 0;
		GroupMember gm = new GroupMember();

		// �жϵ�ͼͼƬ�������Ƿ����ϴ�
		if (audio != null && !audio.equals("")) {
			audio = CommonParam.qiniuPrivateAddress + audio;
		}
		if (placePic != null && !placePic.equals("")) {
			placePic = CommonParam.qiniuPrivateAddress + placePic;
		}

		// �����¼�
		Event event = new Event().set("egroupid", groupid).set("euserid", userid).set("efirstpic", firstPic)
				.set("etext", content).set("eaudio", audio).set("eplace", place).set("ePlacePic", placePic)
				.set("eMain", main).set("ePeopleName", peopleName).set("eStoragePlace", storage).set("eSource", source)
				.set("isSynchronize", isSynchronize);

		// ������ֵ�ж�
		if (placeLongitude != null && placeLatitude != null && !placeLongitude.equals("")
				&& !placeLatitude.equals("")) {
			event.set("ePlaceLongitude", placeLongitude).set("ePlaceLatitude", placeLatitude);
		}

		if (event.save()) {
			eventID = Integer.parseInt(event.get("eid").toString());

			// app�Ÿ���״̬ //lk ��Ӿ����С�����ж� like С����
			if (source != null && !source.equals("С����") &&!source.equals("�����С����")&& !source.equals("����")) {
				// ���¼��������״̬��Ϊ����״̬
				gm.UpdateAllGroupMembersNewDynamic(groupid);
				// �����ߵ�״̬��Ϊ����״̬����Ϊ������˷����¶�̬�������߷�����������ڶ�̬����
				gm.UpdateSingleGroupMenberNoNewDynamic(groupid, userid);
			}

			// ��ͼƬ�򱣴�ͼƬ
			for (int i = 0; i < picArray.length; i++) {
				Picture pic = new Picture();
				pic.set("peid", eventID).set("poriginal", picArray[i])
				.set("pGroupid", groupid).set("puserid", userid).set("pMain", main);
				pic.save();
			}

		}

		// �����û���ʹ�ÿռ�
		User user = new User();
		user.updateUserStoragePlace(userid, storage, "add");

		// APP����
		if (isPush != null && isPush.equals("yes")) {
			PushMessage push = new PushMessage();
			push.pushToSpaceMember(groupid, userid, push.PUSH_TYPE_OF_EVENT);
		}
		// С��������
		
		if (source != null && source.equals("С����") && isPush != null && isPush.equals("true")) {
			SmallAppPush.UploadPush(groupid, userid, formID, picArray.length);
		}
		// �����С�������� by lk 
		if (source != null && source.equals("�����С����") && isPush != null && isPush.equals("true")) {
			SmallAppPush.UploadPush(groupid, userid, formID, picArray.length);
		}

		return eventID;

	}

	/**
	 * �ϴ������ã��ɹ��󷵻��¼���ID
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

		// ����ַ�ַ���ת��������
		String firstPic = null;
		String[] picArray = new String[0];
		if (picAddress != null && !picAddress.equals("")) {
			picArray = dataProcess.getPicAddress(picAddress, "private");
			// ��ȡ��̬��һ��ͼƬ��ַ,����û���ϴ�ͼƬ
			firstPic = (picArray.length == 0 ? null : picArray[0]);
		}

		// �ж������Ƿ����ϴ�
		if (audio != null && !audio.equals("")) {
			audio = CommonParam.qiniuPrivateAddress + audio;
		}

		// �ȶ�ͼƬ����ǰ׺
		verifyPicAddress = CommonParam.qiniuPrivateAddress + verifyPicAddress;

		// �����¼�
		Event event = new Event().set("egroupid", groupid).set("euserid", userid).set("efirstpic", firstPic)
				.set("etext", content).set("eaudio", audio).set("eMain", 5).set("eVerifyPic", verifyPicAddress)
				.set("eStoragePlace", storage).set("eSource", source);

		if (event.save()) {
			eventID = Integer.parseInt(event.get("eid").toString());

			// ���¼��������״̬��Ϊ����״̬
			gm.UpdateAllGroupMembersNewDynamic(groupid);
			// �����ߵ�״̬��Ϊ����״̬����Ϊ������˷����¶�̬�������߷�����������ڶ�̬����
			gm.UpdateSingleGroupMenberNoNewDynamic(groupid, userid);

			// ��ͼƬ�򱣴�ͼƬ
			for (int i = 0; i < picArray.length; i++) {
				Picture pic = new Picture();
				pic.set("peid", eventID).set("poriginal", picArray[i])
				.set("pGroupid", groupid).set("puserid", userid).set("pMain", 5);
				pic.save();
			}

			// ��������Ϣ
			if (totalMoney != 0.00 && totalNum != 0) {
				RedEnvelop re = new RedEnvelop();
				re.set("redEnvelopUserID", userid).set("redEnvelopEventID", eventID).set("redEnvelopTotalNum", totalNum)
						.set("redEnvelopTotalMoney", totalMoney).set("redEnvelopRemainNum", totalNum)
						.set("redEnvelopRemainMoney", totalMoney);
				if (re.save()) {
					String redEnvelopID = re.get("redEnvelopID").toString();
					// �۳��û�Ǯ���ܽ��
					boolean judge = userService.ExpenseMoney(userid, new BigDecimal(String.valueOf(totalMoney)),
							CommonParam.keyOfSendRedEnvelop, redEnvelopID);
					if (!judge)
						return 0;
				}
			}

		}

		// �����û���ʹ�ÿռ�
		User user = new User();
		user.updateUserStoragePlace(userid, storage, "add");

		if (isPush == null || isPush.equals("yes")) {
			// ������Ϣ��Ĭ��Ϊ����
			PushMessage push = new PushMessage();
			push.pushToSpaceMember(groupid, userid, push.PUSH_TYPE_OF_EVENT);
		}

		return eventID;

	}

	/**
	 * �ϴ�����Ƶ
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

		// �����¼�
		Event event = new Event().set("egroupid", groupid).set("euserid", userid).set("eMain", 4).set("etext", content)
				.set("eStoragePlace", storage).set("eplace", place).set("eSource", source)
				.set("isSynchronize", isSynchronize);

		if (event.save()) {
			eventID = Integer.parseInt(event.get("eid").toString());

			/**
			 * С�����ø����¶�̬��app����ʾ<br>
			 * // ���¼��������״̬��Ϊ����״̬<br>
			 * gm.UpdateAllGroupMembersNewDynamic(groupid);<br>
			 * // �����ߵ�״̬��Ϊ����״̬����Ϊ������˷����¶�̬�������߷�����������ڶ�̬����<br>
			 * gm.UpdateSingleGroupMenberNoNewDynamic(groupid, userid);<br>
			 **/

			// �������Ƶ·��
			Picture pic = new Picture();
			pic.set("peid", eventID).set("poriginal", address).set("pcover", cover).set("ptime", time)
			.set("pGroupid", groupid).set("puserid", userid).set("pMain", 4);
			pic.save();

		}

		// �����û���ʹ�ÿռ�
		User user = new User();
		user.updateUserStoragePlace(userid, storage, "add");

		return eventID;

	}

	/**
	 * ��ȡ������̬��Ϣ
	 * 
	 * @param eventId
	 * @return
	 */
	public List<Record> getSingleEvent(int eid, String source) {

		// ��ȡ������̬����
		Event event = new Event();
		List<Record> eventList = event.GetSingleEventContent(eid);
		
		// ��̬��ɾ���򷵻ؿ�
		if (eventList.size() == 0)
			return null;
		int eventQRCodeCanPublish=new GroupCanPublish().getGroupPublishByType(eventList.get(0).getLong("egroupid").intValue()+"", "1");
		// ������̬������Ƭ����Դ
		for (Record record : eventList) {
			record = event.CombineEventAndPicture(record);
			record.set("eventQRCodeCanPublish", eventQRCodeCanPublish);
		}
		// ��ȡ������̬����
		Comment comment = new Comment();
		List<Record> commentList = comment.GetSingleEventComments(eid);

		// ��װ�¼��������ڵ��û���
		eventList = dataProcess.encapsulationEventList(eventList);
		commentList = dataProcess.encapsulationCommentList(commentList);
		List<Record> list = dataProcess.combieEventAndComment(eventList, commentList);
		
		// ��һ��ͼƬͼƬ��Ȩ
		if (list.get(0).get("efirstpic") != null) {
			String auth = qiniu.getDownloadToken(list.get(0).get("efirstpic").toString());
			list.get(0).set("efirstpic", auth);
		}

		// �ж��Ƿ��������ö�̬
		if (list.get(0).get("eMain").toString().equals("5")) {
			// �Ա�ͼƬ��Ȩ
			list.get(0).set("eVerifyPic", qiniu.getDownloadToken(list.get(0).get("eVerifyPic").toString()));

			// ��������Ϣ
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

		// ��װ�¼��͵���
		list = dataProcess.combineEventWithLike(list, source);
		// ��Դ��Ȩ������ȡͼƬ����ͼ
		list = dataProcess.AuthorizeResourceAndGetThumbnail(list);

		return list;
	}

	/**
	 * ��ȡʱ��������
	 * 
	 * @param userid
	 * @param groupid
	 * @param type
	 * @param eid
	 * @return
	 */
	public List<Record> getSpaceTimeAxisContent(String userid, String groupid, String type, int eid, String source) {

		// ��ȡ�ռ���������
		List<Record> commentList = new Comment().GetAllCommentsOfOneSpace(groupid);
		// ��ȡ��̬����
		List<Record> eventList = new ArrayList<Record>();
		// �ö���̬�б�
		List<Record> topEventList = new ArrayList<Record>();

		// �����������ͻ�ȡ��Ӧ����
		Event event = new Event();
		switch (type) {
		case "initialize":
			// ��ȡ�ö���̬
			topEventList = event.GetAllTopEvent(groupid, source);
			// ��ʼ�����ö���̬
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

		// ��̬���ݷ�װ
		List<Record> list = dataProcess.eventDataEncapsulation(eventList, commentList, source);

		// �ж��Ƿ�Ϊ���ᣬ ����������������
		// Group group = new Group().findById(groupid);
		// if (group.get("gtype").toString().equals("12")) {
		// Event.AddRandomView(eventList);
		// }

		// ��������
		return list;
	}

	/**
	 * ��ȡ���ҡ��������ж�̬,�ڶ���
	 * 
	 * @param userid
	 * @return
	 */
	public List<Record> getMyEvents2ndVersion(String userid, int minID, String source) {

		// ��ȡ�����б�
		List<Record> commentList = Comment.GetCommentsOfUserUploadEvents(userid);
		// ��ȡ��̬�б�
		List<Record> eventList = new ArrayList<Record>();

		if (minID <= 0) {
			// ��ʼ��
			eventList = Event.InitializeUserUploadEvents(userid, source);
		} else {
			// ����
			eventList = Event.LoadingUserUploadEvents(userid, minID, source);
		}

		// ��̬���ݷ�װ
		List<Record> list = dataProcess.eventDataEncapsulation(eventList, commentList, source);

		// ��������
		return list;
	}

	/**
	 * ��ȡ�ռ��Ա�Ķ�̬
	 * 
	 * @param groupid
	 * @param userid
	 * @param minID
	 * @return
	 */
	public List<Record> getSpaceMemberEvents(int groupid, int userid, int minID, String source) {

		// ��ȡ�����б�
		List<Record> commentList = Comment.GetCommentsOfSpaceMemberUploadEvents(userid, groupid);
		// ��ȡ��̬�б�
		List<Record> eventList = new ArrayList<Record>();

		if (minID <= 0) {
			// ��ʼ��
			eventList = Event.InitializeSpaceMemberUploadEvents(userid, groupid, source);
		} else {
			// ����
			eventList = Event.LoadingSpaceMemberUploadEvents(userid, groupid, minID, source);
		}

		// ��̬���ݷ�װ
		List<Record> list = dataProcess.eventDataEncapsulation(eventList, commentList, source);

		return list;
	}
	
	/**
	 * ��ȡ�ռ��Ա�Ķ�̬
	 * 
	 * @param groupid
	 * @param userid
	 * @param minID
	 * @return
	 */
	public List<Record> getSpaceMemberEventsNew(int groupid, int userid,String ownUserid, int pagenum ,String source) {
		String mode = "\"%Y-%m-%d\"";
		int page = (pagenum-1)*10;
		// ��ȡ��̬�б�
		List<Record> eventList = Db.find("select eid,etext,eMain,elevel,DATE_FORMAT( euploadtime, " + mode+ " ) as euploadtime from `events` where euserid="+userid
				+" and egroupid="+groupid +" and estatus=0"+ " order by euploadtime desc" +" limit "+page + ",10");
		// ��̬���ݷ�װ
		List<Record> list = dataProcess.eventDataEncapsulationNew(eventList,source,userid,ownUserid);

		return list;
	}
	

	/**
	 * ��ȡ����Ƶǽ����
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

		// ��Դ��Ȩ
		videoList = dataProcess.AuthorizeSingleResource(videoList, "poriginal");

		// ���ؽ��
		return videoList;
	}

	/**
	 * �˻����
	 * 
	 * @param redEnvelopID
	 * @param userid
	 * @return
	 */
	@Before(Tx.class)
	public boolean returnRedEnvelop(String redEnvelopID) {
		// ��ȡ�˻�ʱ��
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String now = sdf.format(new Date());

		// ���������Ϊ���˻�
		RedEnvelop re = new RedEnvelop().findById(redEnvelopID);
		String owner = re.get("redEnvelopUserID").toString();

		// ��ȡ�˻����
		BigDecimal leftMoney = new BigDecimal(re.get("redEnvelopRemainMoney").toString());
		// �޸ĺ��״̬
		re.set("redEnvelopStatus", 2).set("redEnvelopReturnTime", now);

		// �����û����
		boolean returnFlag = userService.incomeMoney(owner, leftMoney, CommonParam.keyOfRefund, redEnvelopID);

		return re.update() && returnFlag;

	}

	/**
	 * �ж϶�̬�Ƿ�������
	 * 
	 * @return 0--������<br>
	 *         ������ --���ID
	 */
	public String JudgeEventIsContainRedEnvelop(String eventID) {
		Event event = new Event().findById(eventID);
		// ���ж��Ƿ�Ϊ������
		if ((event.get("eMain").toString()).equals("5")) {
			List<Record> list = Db.find("select redEnvelopID from redEnvelop where redEnvelopEventID=" + eventID
					+ " and redEnvelopStatus!=2 ");

			return list.size() == 0 ? "0" : list.get(0).get("redEnvelopID").toString();
		} else {
			return "0";
		}
	}

	/**
	 * ��ȡʱ��
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
		//��ȡ�����б�
		List<Record> commentList = Comment.GetCommentsInEventList(momentsList);
		//List<Record> commentList =new ArrayList<Record>();
		// ��̬���ݷ�װ
		//List<Record> list = dataProcess.eventDataEncapsulationShowMoments(momentsList, commentList, "С����");
		List<Record> list = dataProcess.eventDataEncapsulation(momentsList, commentList, "С����");

		return list;
	}
	/**
	 * ��ȡʱ�� by lk �򻯰�
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
		//��ȡ�����б�
		//List<Record> commentList = Comment.GetCommentsInEventList(momentsList);
		List<Record> commentList =new ArrayList<Record>();
		// ��̬���ݷ�װ
		List<Record> list = dataProcess.eventDataEncapsulationShowMoments(momentsList, commentList, "С����");
		//List<Record> list = dataProcess.eventDataEncapsulation(momentsList, commentList, "С����");

		return list;
	}
	/**
	 * ��ȡʱ�� by lk �򻯰�-����ʾ��������
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
		//��ȡ�����б�
		//List<Record> commentList = Comment.GetCommentsInEventList(momentsList);
		List<Record> commentList =new ArrayList<Record>();
		// ��̬���ݷ�װ
		List<Record> list = dataProcess.eventDataEncapsulationShowMoments(momentsList, commentList, "С����");
		//List<Record> list = dataProcess.eventDataEncapsulation(momentsList, commentList, "С����");

		return list;
	}
	/**
	 * ��ȡʱ�������� by lk
	 * 
	 * @param userid
	 * @param groupid
	 * @param type
	 * @param eid
	 * @return
	 */
	public List<Record> getSpaceTimeAxisContentByLk(String userid, String groupid, String type, int eid, String source) {

		
		// ��ȡ��̬����
		List<Record> eventList = new ArrayList<Record>();
		// �ö���̬�б�
		List<Record> topEventList = new ArrayList<Record>();
		// �����������ͻ�ȡ��Ӧ����
		Event event = new Event();
		switch (type) {
		case "initialize":
			// ��ȡ�ö���̬
			System.out.println("ʱ���� ��ȡ�ö���̬ ��ʼ��"+System.currentTimeMillis());
			topEventList = event.GetAllTopEvent(groupid, source);
			System.out.println("ʱ���� ��ȡ�ö���̬ ������"+System.currentTimeMillis());
			// ��ʼ�����ö���̬
			System.out.println("ʱ���� ��ȡ���ö���̬ ��ʼ��"+System.currentTimeMillis());
			eventList = event.InitializeEventContent(groupid, source);
			System.out.println("ʱ���� ��ȡ���ö���̬ ������"+System.currentTimeMillis());
			System.out.println("ʱ���� ��̬list�ϲ� ��ʼ��"+System.currentTimeMillis());
			eventList = dataProcess.combineTwoList(topEventList, eventList);
			System.out.println("ʱ���� ��̬list�ϲ� ������"+System.currentTimeMillis());
			break;
		case "refresh":
			eventList = event.RefreshEventContent(groupid, eid, source);
			break;
		case "loading":
			eventList = event.LoadingEventContent(groupid, eid, source);
			break;
		}
		// ��ȡ�ռ���������
		System.out.println("ʱ���� ��ȡ�ռ��������� ��ʼ��"+System.currentTimeMillis());
		//by lk �޸����ۻ�ȡ����
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
		
		System.out.println("ʱ���� ��ȡ�ռ��������� ������"+System.currentTimeMillis());
				
		System.out.println("ʱ���� ��̬���ݷ�װ ��ʼ��"+System.currentTimeMillis());
		// ��̬���ݷ�װ
		List<Record> list = dataProcess.eventDataEncapsulationByLk(eventList, commentList, source);
		System.out.println("ʱ���� ��̬���ݷ�װ end��"+System.currentTimeMillis());

		// �ж��Ƿ�Ϊ���ᣬ ����������������
		// Group group = new Group().findById(groupid);
		// if (group.get("gtype").toString().equals("12")) {
		// Event.AddRandomView(eventList);
		// }

		// ��������
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
