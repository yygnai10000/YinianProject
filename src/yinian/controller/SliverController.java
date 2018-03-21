package yinian.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.model.AuthorizePhoto;
import yinian.model.Peep;
import yinian.model.User;
import yinian.model.UserPhoto;
import yinian.service.SliverService;
import yinian.utils.JsonData;
import yinian.utils.QiniuOperate;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;

public class SliverController extends Controller {

	private String jsonString; // ���ص�json�ַ���
	private JsonData jsonData = new JsonData(); // json������
	private YinianDataProcess dataProcess = new YinianDataProcess();// ���ݴ�����
	private QiniuOperate operate = new QiniuOperate(); // ��ţ�ƴ�����
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
	private SimpleDateFormat sdfOfDate = new SimpleDateFormat("yyyy-MM-dd");
	private SliverService service = new SliverService();

	/**
	 * �洢�û���Ƭ
	 */
	@Before(Tx.class)
	public void SaveUserPhoto() {

		String userid = this.getPara("userid");
		String[] url = this.getPara("url").split(",");
		String[] key = this.getPara("key").split(",");
		String time = this.getPara("time");

		String[] timeArray;
		if (time == null) {
			// û�д�ʱ���򽫵�ǰʱ����ΪĬ��ֵ
			String now = sdf.format(new Date());
			timeArray = new String[url.length];
			for (int i = 0; i < timeArray.length; i++)
				timeArray[i] = now;
		} else {
			timeArray = time.split(",");
			// ��ʱ���ʽ���м��
			for (int i = 0; i < timeArray.length; i++) {
				int year = Integer.parseInt(timeArray[i].split("-")[0]);
				if (year > 2100)
					timeArray[i] = sdf.format(new Date());
			}
		}

		if (userid.equals("2")) {
			jsonString = jsonData.getSuccessJson();
		} else {
			if (url.length == key.length) {
				boolean flag = false;
				for (int i = 0; i < url.length; i++) {
					UserPhoto photo = new UserPhoto()
							.set("userid", userid)
							.set("address",
									CommonParam.qiniuPrivateAddress + url[i])
							.set("key", key[i]).set("shootTime", timeArray[i]);
					if (photo.save()) {
						flag = true;
					} else {
						flag = false;
						break;
					}
				}
				jsonString = dataProcess.insertFlagResult(flag);
			} else {
				jsonString = jsonData.getJson(2, "�����������");
			}
		}

		renderText(jsonString);
	}

	/**
	 * ɾ���û���Ƭ
	 */
	@Before(Tx.class)
	public void DeleteUserPhoto() {

		String userid = this.getPara("userid");
		String[] keys = this.getPara("key").split(",");

		String key = "";
		for (int i = 0; i < keys.length; i++) {
			key += "'" + keys[i] + "',";
		}
		key = key.substring(0, key.length() - 1);

		int count = Db.update("update userPhoto set status=1 where `key` in ("
				+ key + ") and userid=" + userid + " and status=0 ");
		jsonString = dataProcess.updateFlagResult(count == keys.length);

		renderText(jsonString);

	}

	/**
	 * ��ȡ�û����е�ͼƬhashֵ
	 */
	public void GetUserAllPhotoHash() {
		String userid = this.getPara("userid");
		List<Record> list = Db
				.find("select distinct `key`,shootTime from userPhoto where userid="
						+ userid
						+ " and status=0 and `key`!='' and `key` is not null");
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * ��ȡ��Ա��Ȩ�б�
	 */
	public void GetGroupMemberAuthorizeList() {
		int userid = Integer.parseInt(this.getPara("userid"));
		int groupid = this.getParaToInt("groupid");

		Peep peep = new Peep();
		String today = sdfOfDate.format(new Date());

		// �ж��û��Ƿ���ɽ̳�
		User user = new User().findById(userid);
		String isFinishWatchPhotoTutorial = user.get(
				"isFinishWatchPhotoTutorial").toString();// 0--δ��� 1--�����

		// ��ȡ��Ա�б����Ȩ��Ϣ
		List<Record> groupMemberList = Db
				.find("select userid,unickname,upic from users,groupmembers where userid=gmuserid and gmgroupid='"
						+ groupid
						+ "' and gmuserid != "
						+ userid
						+ "  and gmstatus=0 ");
		List<Record> authorizeList = Db
				.find("select authorizeUserID,authorizeBeUserID,authorizeStatus,authorizeCancelTime from authorizePhoto where (authorizeUserID="
						+ userid
						+ " or authorizeBeUserID="
						+ userid
						+ ") and authorizeGroupID=" + groupid + " ");

		// ������Ȩ������б�
		List<Record> mutualAuthorizeList = new ArrayList<Record>(); // ������Ȩ�б�
		List<Record> mutualUnauthorizeList = new ArrayList<Record>();// ������Ȩ�б�
		List<Record> unilateralOfMeList = new ArrayList<Record>();// �ҵ�����Ȩ�б�
		List<Record> unilateralOfOthersList = new ArrayList<Record>();// �Է�������Ȩ�б�

		// ÿ����Ա�������鿴��Ȩ���������
		for (Record record : groupMemberList) {

			int memberID = Integer.parseInt(record.get("userid").toString());
			boolean meAuthorize = false; // ���Ƿ�Ըó�Ա��Ȩ��Ĭ��Ϊ��
			boolean youAuthorize = false; // �ó�Ա�Ƿ������Ȩ��Ĭ��Ϊ��

			for (Record authorize : authorizeList) {
				int authorizeUserID = Integer.parseInt(authorize.get(
						"authorizeUserID").toString());
				int authorizeBeUserID = Integer.parseInt(authorize.get(
						"authorizeBeUserID").toString());
				int status = Integer.parseInt(authorize.get("authorizeStatus")
						.toString());

				if (authorizeUserID == userid && authorizeBeUserID == memberID
						&& status == 1) {
					// ����Ȩ�˶Է�
					meAuthorize = true;
				}
				if (authorizeUserID == memberID && authorizeBeUserID == userid) {
					if (status == 1) {
						// �Է���Ȩ����
						youAuthorize = true;
					} else {
						// �Է�����ȡ����Ȩ�����ǽ��쿴���ҵ����ݣ��������ҵ���ʾ��Ȼ�ǶԷ�������Ȩ��
						if (peep.JudgeIsUserPeepOtherToday(memberID, userid,
								groupid, today))
							youAuthorize = true;
					}
				}

			}

			if (meAuthorize && youAuthorize) {
				// ˫��������Ȩ
				mutualAuthorizeList.add(record);
			}
			if (!meAuthorize && !youAuthorize) {
				// ˫����δ��Ȩ
				mutualUnauthorizeList.add(record);
			}
			if (meAuthorize && !youAuthorize) {
				// �ҵ�����Ȩ�Է�
				unilateralOfMeList.add(record);
			}
			if (!meAuthorize && youAuthorize) {
				// �Է�������Ȩ��
				unilateralOfOthersList.add(record);
			}

		}

		// ��ȡ������Ȩ�б��е����û���鿴����Ϣ
		for (Record record : mutualAuthorizeList) {
			int isMeSee = 0; // �ҽ����Ƿ�鿴��Է���3����Ƭ 0--δ�鿴�� 1--�Ѳ鿴��
			int seeNum = 0;// �Է��鿴������
			int memberID = Integer.parseInt(record.get("userid").toString());

			seeNum = peep.GetPeepPhotoNum(memberID, userid, groupid, today);
			// �ж����Ƿ񿴹��Է�
			if (peep.JudgeIsUserPeepOtherToday(userid, memberID, groupid, today)
					&& peep.GetPeepPhotoNum(userid, memberID, groupid, today) == 3) {
				isMeSee = 1;
			}
			record.set("isMeSee", isMeSee).set("seeNum", seeNum);
		}

		// ���췵�ض���
		List<Record> resultList = new ArrayList<Record>();
		Record record = new Record();
		record.set("mutualAuthorizeList", mutualAuthorizeList)
				.set("mutualUnauthorizeList", mutualUnauthorizeList)
				.set("unilateralOfMeList", unilateralOfMeList)
				.set("unilateralOfOthersList", unilateralOfOthersList)
				.set("isFinishWatchPhotoTutorial", isFinishWatchPhotoTutorial);
		resultList.add(record);
		jsonString = jsonData.getSuccessJson(resultList);
		renderText(jsonString);

	}

	/**
	 * ��Ȩ��ȡ����Ȩ
	 */
	@Before(Tx.class)
	public void AuthorizeOrCancelForWatchPhoto() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String beUserid = this.getPara("beUserid");
		String type = this.getPara("type");

		String nowTime = sdf.format(new Date());
		AuthorizePhoto ap;
		switch (type) {
		case "authorize":
			// ������Ȩ
			// �ж��Ƿ��й���Ȩ
			List<Record> list = Db
					.find("select authorizeID from authorizePhoto where authorizeGroupID="
							+ groupid
							+ " and authorizeUserID="
							+ userid
							+ " and authorizeBeUserID=" + beUserid + "");
			if (list.size() == 0) {
				ap = new AuthorizePhoto().set("authorizeGroupID", groupid)
						.set("authorizeUserID", userid)
						.set("authorizeBeUserID", beUserid)
						.set("authorizeStatus", 1);
				jsonString = dataProcess.insertFlagResult(ap.save());
			} else {
				ap = new AuthorizePhoto().findById(list.get(0)
						.get("authorizeID").toString());
				ap.set("authorizeStatus", 1);
				jsonString = dataProcess.updateFlagResult(ap.update());
			}

			break;
		case "cancel":
			// ����ȡ����Ȩ
			int count = Db
					.update("update authorizePhoto set authorizeStatus=0,authorizeCancelTime='"
							+ nowTime
							+ "' where authorizeGroupID="
							+ groupid
							+ " and authorizeUserID="
							+ userid
							+ " and authorizeBeUserID= " + beUserid + "  ");
			jsonString = dataProcess.updateFlagResult(count == 1);
			break;
		case "cancelAll":
			// ���������ڵĺ���ȫȡ����Ȩ
			List<Record> judge = Db
					.find("select * from authorizePhoto where authorizeUserID="
							+ userid + " and authorizeStatus=1 ");
			int batch = Db
					.update("update authorizePhoto set authorizeStatus=0,authorizeCancelTime='"
							+ nowTime
							+ "' where authorizeUserID="
							+ userid
							+ " and authorizeStatus=1  ");
			jsonString = dataProcess.updateFlagResult(batch == judge.size());
			break;
		case "authorizeAll":
			// ��Ȩ�ռ��ڵ�ȫ������
			List<Record> groupMemberList = Db
					.find("select userid,unickname,upic from users,groupmembers where userid=gmuserid and gmgroupid='"
							+ groupid
							+ "' and gmuserid != "
							+ userid
							+ "  and gmstatus=0 ");

			// ��ȡ�ռ�������Ȩ�����û�����
			List<Record> authorizeBeforeList = Db
					.find("select authorizeID,authorizeBeUserID from authorizePhoto where authorizeGroupID="
							+ groupid + " and authorizeUserID=" + userid + " ");

			boolean flag = false;
			for (Record record : groupMemberList) {
				int be = Integer.parseInt(record.get("userid").toString());

				boolean judgeFlag = false; // falses��ʾû����Ȩ�������������
											// true��ʾ��Ȩ�������������
				int tempData = 0;
				for (Record temp : authorizeBeforeList) {
					int authorizeID = Integer.parseInt(temp.get("authorizeID")
							.toString());
					int authorizeBeUserID = Integer.parseInt(temp.get(
							"authorizeBeUserID").toString());
					if (be == authorizeBeUserID) {
						judgeFlag = true;
						tempData = authorizeID;
						break;
					}
				}
				if (judgeFlag) {
					ap = new AuthorizePhoto().findById(tempData);
					ap.set("authorizeStatus", 1);
					flag = ap.update();
					if (!flag) {
						jsonString = dataProcess.updateFlagResult(flag);
						break;
					}
				} else {
					ap = new AuthorizePhoto().set("authorizeGroupID", groupid)
							.set("authorizeUserID", userid)
							.set("authorizeBeUserID", be)
							.set("authorizeStatus", 1);
					flag = ap.save();
					if (!flag) {
						jsonString = dataProcess.insertFlagResult(flag);
						break;
					}
				}

			}
			if (flag) {
				jsonString = jsonData.getSuccessJson();
			}
			break;
		default:
			jsonString = jsonData.getJson(2, "�����������");
			break;
		}
		renderText(jsonString);
	}

	/**
	 * �鿴��������
	 */
	public void ShowPeepInterface() {
		int userid = this.getParaToInt("userid");
		int beUserid = this.getParaToInt("beUserid");
		int groupid = this.getParaToInt("groupid");

		String today = sdfOfDate.format(new Date());

		// ��ȡ˫����Ϣ
		List<Record> userList = Db
				.find("select userid,unickname,upic from users where userid in ("
						+ userid + "," + beUserid + ")  ");
		// ��ȡ˫��������Ϣ
		List<Record> photoList = Db
				.find("select peepBeUserid,peepPhoto,peepPosition from peep where peepGroupid="
						+ groupid
						+ " and peepUserid in ("
						+ userid
						+ ","
						+ beUserid + ") and peepDate='" + today + "'  ");

		// ��Ϣ�洢����
		Record myInfo = new Record();
		Record yourInfo = new Record();

		// ��Ӹ�����Ϣ
		for (Record record : userList) {
			int tempID = Integer.parseInt(record.get("userid").toString());
			if (userid == tempID) {
				myInfo.set("userInfo", record);
			}
			if (beUserid == tempID) {
				yourInfo.set("userInfo", record);
			}
		}

		// boolean flag = true; // true��ʾ��δ���ú��ѵ���Ƭ

		// ���͵����Ϣ
		if (photoList.size() == 0) {
			myInfo.set("photoInfo", null);
			yourInfo.set("photoInfo", null);
		} else {
			for (Record record : photoList) {
				int tempID = Integer.parseInt(record.get("peepBeUserid")
						.toString());
				// ͼƬ��Ȩ
				String[] peepPhoto = record.get("peepPhoto").toString()
						.split(",");
				String[] position = record.get("peepPosition").toString()
						.split(",");
				List<Record> photoInfoArray = new ArrayList<Record>();
				for (int i = 0; i < peepPhoto.length; i++) {
					Record temp = new Record()
							.set("photo",
									operate.getDownloadToken(peepPhoto[i]
											+ CommonParam.pictureThumbnailPara))
							.set("year", position[i].split(";")[0])
							.set("position", position[i].split(";")[1]);
					photoInfoArray.add(temp);
				}

				if (userid == tempID) {
					myInfo.set("photoInfo", photoInfoArray);
				}
				if (beUserid == tempID) {
					yourInfo.set("photoInfo", photoInfoArray);
					// // �жϿ��˺��Ѽ�����Ƭ
					// int size = peepPhoto.length;
					// if (size < 3) {
					// yourInfo.set("unseenPhoto",
					// service.GetFriendPhoto(3 - size, beUserid));
					// }
					// flag = false;
				}
			}

		}

		// ��ӶԷ����������Ϣ
		yourInfo.set("unseenPhoto", service.GetFriendPhoto(3, beUserid));

		Record result = new Record().set("myInfo", myInfo).set("yourInfo",
				yourInfo);
		List<Record> list = new ArrayList<Record>();
		list.add(result);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);

	}

	/**
	 * ͵���û�ͼƬ
	 */
	public void PeepUserPhoto() {
		int userid = this.getParaToInt("userid");
		int beUserid = this.getParaToInt("beUserid");
		int groupid = this.getParaToInt("groupid");
		int year = this.getParaToInt("year");
		int position = this.getParaToInt("position");

		// ��ȡ͵����ͼƬ
		List<Record> peepPhoto = Db
				.find("select address from userPhoto where year(shootTime)="
						+ year + " and userid=" + beUserid + " and status=0 ");
		int size = peepPhoto.size();
		int random = (int) (Math.random() * (size - 1));
		String address = peepPhoto.get(random).get("address").toString();
		String data = address;
		address = operate.getDownloadToken(address
				+ CommonParam.pictureThumbnailPara);
		List<Record> result = dataProcess.makeSingleParamToList("photo",
				address);

		// �洢ͼƬλ��
		String location = (year + ";" + position);

		String today = sdfOfDate.format(new Date());
		// �жϽ����Ƿ񿴹�������Ƭ
		Peep peep = new Peep();
		List<Record> list = peep.GetPeepDataToday(userid, beUserid, groupid,
				today);
		if (list.size() == 0) {
			// δ����
			peep = new Peep().set("peepGroupid", groupid)
					.set("peepUserid", userid).set("peepBeUserid", beUserid)
					.set("peepPhoto", data).set("peepDate", today)
					.set("peepPosition", location);
			if (peep.save()) {
				jsonString = jsonData.getSuccessJson(result);
			} else {
				jsonString = dataProcess.insertFlagResult(false);
			}

		} else {
			// ��������
			if (list.size() < 3) {
				// �ܿ�����
				String peepID = list.get(0).get("peepID").toString();
				peep = new Peep().findById(peepID);
				peep.set("peepPhoto",
						peep.get("peepPhoto").toString() + "," + data).set(
						"peepPosition",
						peep.get("peepPosition").toString() + "," + location);
				if (peep.update()) {
					jsonString = jsonData.getSuccessJson(result);
				} else {
					jsonString = dataProcess.updateFlagResult(false);
				}
			} else {
				// �����ٿ�����
				jsonString = jsonData.getJson(1038, "���첻����͵���ú���");
			}
		}

		// ����ѷ�������

		renderText(jsonString);

	}

	/**
	 * ��ɽ̳�
	 */
	public void FinishWatchPhotoTutorial() {
		String userid = this.getPara("userid");
		User user = new User().findById(userid);
		user.set("isFinishWatchPhotoTutorial", 1);
		jsonString = dataProcess.updateFlagResult(user.update());
		renderText(jsonString);
	}

}
