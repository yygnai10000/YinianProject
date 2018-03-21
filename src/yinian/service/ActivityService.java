package yinian.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.ehcache.CacheKit;

import yinian.app.YinianDataProcess;
import yinian.model.Comment;
import yinian.model.Encourage;
import yinian.model.EncourageLogistics;
import yinian.model.Event;
import yinian.model.Group;
import yinian.model.Sign;
import yinian.model.Signold;
import yinian.utils.JsonData;

public class ActivityService {

	private String jsonString;// ���ؽ��
	private JsonData jsonData = new JsonData(); // json������
	private YinianDataProcess dataProcess = new YinianDataProcess();// ���ݴ�����
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	/**
	 * ��ȡ��������
	 * 
	 * @return
	 */
	public List<Record> getActivitySpace(int userid) {

		// �ȴӻ����в���
		List<Record> list = CacheKit.get("ServiceCache", "activitySpace");
		// �������Ϊ��
		if (list == null) {
			// ��ȡ�ռ������Ϣ
			list = Group.GetActivitySpaceBasicInfo();
			// ����������Ϣ
			for (Record record : list) {
				record.set("shareTitle", "���׷���ح������ɹ�����׷壬�����ô󽱰ɣ�")
						.set("shareContent", "���ϴ������׷����Ƭ�����а��п������Ұ����ð����ջ�Ա��ͬ��Diorī���ɣ��������������Ŷ��")
						.set("bannerPic", "7xlmtr.com1.z0.glb.clouddn.com/activity_bannerliyifeng.jpg")
						.set("introducePic", "7xlmtr.com1.z0.glb.clouddn.com/wxtp_20170810140647.jpg");
			}
			// ��ȡ�ռ���Ƭ��
			List<Record> photoList = Group.GetSpacePhotoNum(list);
			// ���ռ���Ƭ�����뵽�ռ���Ϣ��
			list = dataProcess.combineSpaceInfoWithPhotoNum(list, photoList, userid);
			// ��ȡ�ռ䶯̬��
			List<Record> eventList = Group.GetSpaceEventNum(list);
			// ���ռ䶯̬�����뵽�ռ���Ϣ��
			list = dataProcess.combineSpaceInfoWithEventNum(list, eventList);
			// ���ռ�����ת������Ӧ������
			list = dataProcess.changeGroupTypeIntoWord(list);

			// �����ݷ��뻺����
			CacheKit.put("ServiceCache", "activitySpace", list);
		}

		// ���ؽ��
		return list;
	}

	/**
	 * ��ȡ������̬
	 * 
	 * @param groupid
	 * @param nickname
	 * @return
	 */
	public List<Record> getSearchEvents(String groupid, String nickname) {

		// ��ȡ�ռ���������
		List<Record> commentList = new Comment().GetAllCommentsOfOneSpace(groupid);

		// ��ȡ��̬����
		Event event = new Event();
		List<Record> eventList = event.SearchSpaceEventsByNickname(groupid, nickname);

		// ��̬���ݷ�װ
		List<Record> list = dataProcess.eventDataEncapsulation(eventList, commentList, "app");

		// ��������
		return list;

	}

	/**
	 * ��ȡ�ռ����а�
	 * 
	 * @param groupid
	 * @return
	 */
	public List<Record> getSpaceEventChart(int groupid) {
		// ��ȡ���������Ķ�̬
		List<Record> eventList = Db.find(
				"SELECT userid,unickname,upic,count(*) AS num FROM users,`events`,`like` WHERE userid = euserid AND eid = likeEventID AND estatus = 0 AND likeStatus != 1 AND egroupid = "
						+ groupid + " GROUP BY eid HAVING num >= 3 ORDER BY num DESC");
		// ɸѡ
		Set<String> userSet = new HashSet<String>();
		int totalCount = 1;// ��¼����
		int sameCount = 1;// ��¼ͬ����
		int lastNum = 0;// ��¼��һ���˵ĵ�����
		List<Record> result = new ArrayList<Record>();

		for (Record record : eventList) {
			String userid = record.get("userid").toString();
			int num = Integer.parseInt(record.get("num").toString());
			if (!userSet.contains(userid)) {
				// ˵�����û����ظ�
				userSet.add(userid);
				if (num == lastNum) {
					// ��ǰһ������
					record.set("rank", sameCount);
				} else {
					// ��ǰһ��С
					record.set("rank", totalCount);
					lastNum = num;
					sameCount = totalCount;
				}
				totalCount++;
				result.add(record);

			}
		}

		// ����50���������ȡ������Ϣ
		if (result.size() > 50) {
			int fiftyRank = result.get(49).getInt("rank");
			for (int i = 50; i < result.size(); i++) {
				int temp = result.get(i).getInt("rank");
				if (temp != fiftyRank) {
					// �����ٲ���50��
					result = new ArrayList<Record>(result.subList(0, i));
				}
			}
		}

		return result;
	}

	/**
	 * ǩ��
	 * 
	 * @param userid
	 * @param signType
	 *            ǩ������ 0--С���� 1--APP
	 * @return
	 */
	public boolean signIn (String userid, String signType) throws ParseException{

		// ��ȡ����
		Calendar cal = Calendar.getInstance();
		String today = sdf.format(cal.getTime());
		cal.add(Calendar.DATE, -1);
		String yesterday = sdf.format(cal.getTime());

		// �ж��û�֮ǰ�Ƿ�ǩ����
		Sign sign = new Sign();
		List<Record> userSignInfo = sign.getUserSignInInfo(userid, signType);
		if (userSignInfo.size() == 0) {
			// δǩ����
			sign.set("signUserID", userid).set("signStartDate", today).set("signEndDate", today).set("signType",
					signType);
			return sign.save();
		} else {
			// �ж��Ƿ��ǩ
			String signEndDate = userSignInfo.get(0).get("signEndDate").toString();
			String signID = userSignInfo.get(0).get("signID").toString();

			// ��ǩ����,�����޷���ǩ��
			if (signEndDate.equals(today))
				return false;

			sign = new Sign().findById(signID);
			if (signEndDate.equals(yesterday)) {
				// δ��ǩ
				sign.set("signEndDate", today);
			} else {
				// �Ѷ�ǩ
				sign.set("signStartDate", today).set("signEndDate", today);
			}
			return sign.update();

		}

	}
	
	/**
	 * ǩ��(��)
	 * 
	 * @param userid
	 * @param signType
	 *            ǩ������ 0--С���� 1--APP
	 * @return
	 */
	public boolean signIn2 (String userid, String signType) throws ParseException{

		// ��ȡ����
		Calendar cal = Calendar.getInstance();
		String today = sdf.format(cal.getTime());
		cal.add(Calendar.DATE, -1);
		String yesterday = sdf.format(cal.getTime());

		// �ж��û�֮ǰ�Ƿ�ǩ����
		Sign sign = new Sign();
		List<Record> userSignInfo = sign.getUserSignInInfo(userid, signType);
		System.out.println(userSignInfo);
		if (userSignInfo.size() == 0) {
			// δǩ����
			sign.set("signUserID", userid).set("signStartDate", today).set("signEndDate", today).set("signType",
					signType).set("signCount", 1);
			return sign.save();
		} else {
			// �ж��Ƿ��ǩ
			String signEndDate = userSignInfo.get(0).get("signEndDate").toString();
			String signStartDate = userSignInfo.get(0).get("signStartDate").toString();
			String signID = userSignInfo.get(0).get("signID").toString();

			// ��ǩ����,�����޷���ǩ��
			if (signEndDate.equals(today))
				return false;

			sign = new Sign().findById(signID);
			if (signEndDate.equals(yesterday)) {
				// δ��ǩ
				Integer signCount = userSignInfo.get(0).getInt("signCount");//�����ʷǩ��ʱ��
				//�ѵ�һ�ε����콱����ʱ����Ϣ���浽������(��Ҫ���״�ǩ����ʱ��͵�һ�ε����콱������ʱ��)
				EncourageLogistics enlog = new EncourageLogistics();
				switch (signCount) {
				case 6:
					enlog.set("elUserID", userid).set("elSignStartTime", signStartDate)
							.set("elType", "signLevelOne").set("elFirstReceiveTime", today);
					enlog.save();
					break;
				case 13:
					enlog.set("elUserID", userid).set("elSignStartTime", signStartDate)
					.set("elType", "signLevelTwo").set("elFirstReceiveTime", today);
					enlog.save();
					break;
				case 29:
					enlog.set("elUserID", userid).set("elSignStartTime", signStartDate)
					.set("elType", "signLevelThree").set("elFirstReceiveTime", today);
					enlog.save();
					break;
				case 99:
					enlog.set("elUserID", userid).set("elSignStartTime", signStartDate)
					.set("elType", "signLevelFour").set("elFirstReceiveTime", today);
					enlog.save();
					break;
				case 364:
					enlog.set("elUserID", userid).set("elSignStartTime", signStartDate)
					.set("elType", "signLevelFive").set("elFirstReceiveTime", today);
					enlog.save();
					break;
				default:
					break;
				}
				
				long to = sdf.parse(signEndDate).getTime();
				long from = sdf.parse(signStartDate).getTime();
				int signDay = (int) ((to - from) / (1000 * 60 * 60 * 24)) + 1;
				//�ж϶�ǩ������ǩ�������Ƿ������ʷ���ǩ������
				if(signDay>=signCount) {
					sign.set("signCount", signDay+1);
				}
				sign.set("signEndDate", today);
				
			} else {
				// �Ѷ�ǩ
				sign.set("signStartDate", today).set("signEndDate", today);
			}
			return sign.update();

		}

	}
	
	/**
	 * ǩ����ȡ����---ly
	 */
	public boolean signInPoints (String userid, String signType) throws ParseException{

		// ��ȡ����
		Calendar cal = Calendar.getInstance();
		String today = sdf.format(cal.getTime());
		cal.add(Calendar.DATE, -1);
		String yesterday = sdf.format(cal.getTime());

		// �ж��û�֮ǰ�Ƿ�ǩ����
		Signold sign = new Signold();
		List<Record> userSignInfo = sign.getUserSignInInfo(userid, signType);
		if (userSignInfo.size() == 0) {
			// δǩ����
			sign.set("signUserID", userid).set("signStartDate", today).set("signEndDate", today).set("signType",
					signType);
			return sign.save();
		} else {
			// �ж��Ƿ��ǩ
			String signEndDate = userSignInfo.get(0).get("signEndDate").toString();
			String signStartDate = userSignInfo.get(0).get("signStartDate").toString();
			String signID = userSignInfo.get(0).get("signID").toString();

			// ��ǩ����,�����޷���ǩ��
			if (signEndDate.equals(today))
				return false;

			sign = new Signold().findById(signID);
			if (signEndDate.equals(yesterday)) {
				// δ��ǩ
				sign.set("signEndDate", today);
				long to = sdf.parse(signEndDate).getTime();
				long from = sdf.parse(signStartDate).getTime();
				int signDay = (int) ((to - from) / (1000 * 60 * 60 * 24)) + 1;
				if(signDay >= 7) {
					sign.set("signStartDate", today).set("signEndDate", today);
				}
			} else {
				// �Ѷ�ǩ
				sign.set("signStartDate", today).set("signEndDate", today);
			}
			return sign.update();
		}
	}
	/**
	 * ��¼�ɹ��������
	 */
	public boolean recordSuccessInviteFriend(String userid) {
		Encourage en = new Encourage();
		List<Record> encourageInfo = Encourage.getEncourageInfo(userid);

		if (encourageInfo.size() == 0) {
			// �����ݣ�����������
			en.set("encourageUserID", userid).set("inviteNum", 1);
			return en.save();
		} else {
			// �����ݣ�����ԭ������
			String encourageID = encourageInfo.get(0).get("encourageID").toString();
			int inviteNum = Integer.parseInt(encourageInfo.get(0).get("inviteNum").toString());
			en = new Encourage().findById(encourageID).set("inviteNum", inviteNum + 1);
			return en.update();
		}
	}
	
	
	
	

	/**
	 * ��ȡ��������
	 */
	public List<Record> getCityRank() {
		List<Record> list = CacheKit.get("ServiceCache", "cityRank");
		if (list == null) {
			list = Db.find(
					"select groupid,gname,gpic,count(*) as num from groups,`events`,pictures where groupid=egroupid and eid=peid and gtype=14 and gstatus=0 and estatus=0 and pstatus=0 group by groupid order by count(*) desc");
			// ��������
			int count = 1;
			String num = "-1";
			for (int i = 0; i < list.size(); i++) {
				String picNum = list.get(i).get("num").toString();
				if (picNum.equals(num)) {
					list.get(i).set("rank", count);
				} else {
					count = (i + 1);
					num = picNum;
					list.get(i).set("rank", count);
				}
			}
			CacheKit.put("ServiceCache", "cityRank", list);
		}

		return list;
	}

	/**
	 * ��ȡ���й��װ�
	 */
	public List<Record> getCityContributionRank(String groupid) {
		List<Record> list = CacheKit.get("ServiceCache", groupid + "cityContributionRank");
		if (list == null) {
			list = Db.find(
					"select userid,unickname,upic,count(*) as num from users,`events`,pictures where userid=euserid and eid=peid and egroupid = "
							+ groupid
							+ " and estatus=0 and pstatus=0 GROUP BY userid order by count(*) desc limit 100");
			int count = 1;
			String num = "-1";
			for (int i = 0; i < list.size(); i++) {
				String picNum = list.get(i).get("num").toString();
				if (picNum.equals(num)) {
					list.get(i).set("rank", count);
				} else {
					count = (i + 1);
					num = picNum;
					list.get(i).set("rank", count);
				}
			}
			CacheKit.put("ServiceCache", groupid + "cityContributionRank", list);
		}
		return list;
	}

	/**
	 * ��ȡ���пռ���Ϣ
	 */
	public Record getCitySpaceInfo(String groupid) {
		Record record = CacheKit.get("ConcurrencyCache", groupid + "citySpaceInfo");
		if (record == null) {
			List<Record> cityList = getCityRank();
			for (Record temp : cityList) {
				String tempGroupID = temp.get("groupid").toString();
				if (tempGroupID.equals(groupid)) {
					record = temp;
					break;
				}
			}
			CacheKit.put("ConcurrencyCache", groupid + "citySpaceInfo", record);
		}
		return record;
	}

	/**
	 * ��ȡ�ҵĹ���
	 */
	public Record getMyContributionInCitySpace(String userid, String groupid) {
		int myRank = 0;
		// ��ȡ�ҵ�����
		List<Record> rankList = getCityContributionRank(groupid);
		for (Record record : rankList) {
			String tempUserid = record.get("userid").toString();
			if (userid.equals(tempUserid)) {
				// �ڰ���
				myRank = record.getInt("rank");
			}
		}
		// ��ȡ����
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String date = sdf.format(new Date());
		
		List<Record> total = Db.find("select count(*) as num from `events`,pictures where eid=peid and egroupid="
				+ groupid + " and euserid=" + userid + " and estatus=0 and pstatus=0 ");
		List<Record> today = Db.find(
				"select count(*) as num from `events`,pictures where eid=peid and egroupid=" + groupid + " and euserid="
						+ userid + " and date(euploadtime)='" + date + "' and estatus=0 and pstatus=0");
		String totalContribution = total.get(0).get("num").toString();
		String todayContribution = today.get(0).get("num").toString();

		Record result = new Record().set("myRank", myRank).set("totalContribution", totalContribution)
				.set("todayContribution", todayContribution);
		return result;
	}
	/**
	 * ��ȡ���л���
	 */
	public  List<Record> getAllActivitiGroups(String type,String number){
		List<Record> activitiGroupList = new ArrayList<>();
		if(type.equals("initialize")) {
			activitiGroupList = Db.find("select gname,gpic,number,groupid,activitiStatus,activitiStartTime,activitiEndTime from activitigroups,groups "
					+ " where groupid=activitiGroupid and isDelete=0 "
					+ " order by number asc limit 2");
		}else {
			//��ҳ����
			activitiGroupList = Db.find("select gname,gpic,number,groupid,activitiStatus,activitiStartTime,activitiEndTime from activitigroups,groups "
					+ " where groupid=activitiGroupid and number>"+number+" and isDelete=0 "
					+ " order by number asc limit 2");
		}
		return activitiGroupList;
		
	}
	
	/**
	 * ��ȡ�ҲμӵĻ���
	 */
	public List<Record> getMyActivitiGroups(String jointime,String userid,String type){
		List<Record> myActivitiList = new ArrayList<>();
		if(type.equals("initialize")) {
			myActivitiList = Db.find("select gname,gpic,number,groupid,activitiStatus,activitiStartTime,activitiEndTime,joinTime from activitimembers,activitigroups,groups "
					+ "where groupid=activitiGroupid and groupid=joinGroupid and joinUserid="+userid+" and isDelete=0 order by joinTime desc limit 2");
//			myActivitiList = Db.find("select gname,gpic,groupid,activitiStatus,activitiStartTime,activitiEndTime from groupmembers,activitigroups,groups "
//					+ "where groupid=activitiGroupid and groupid=gmGroupid and gmUserid="+userid+" order by groupid desc limit 10");
		}else {
			//��ҳ����
			myActivitiList = Db.find("select gname,gpic,number,groupid,activitiStatus,activitiStartTime,activitiEndTime,joinTime from activitimembers,activitigroups,groups "
					+ "where groupid=activitiGroupid and groupid=joinGroupid and joinUserid="+userid+ " and jointime<'"+jointime+"' and isDelete=0 order by joinTime desc limit 2");
//			myActivitiList = Db.find("select gname,gpic,groupid,activitiStatus,activitiStartTime,activitiEndTime from groupmembers,activitigroups,groups "
//					+ "where groupid=activitiGroupid and groupid=gmGroupid and gmUserid="+userid+ " and groupid<"+groupid+" order by groupid desc limit 10");
		}
		return myActivitiList;
		
	}
	
	
	/**
	 * �����ҳbannerͼ
	 * @return
	 */
	public List<Record> getActivitiBanner(){
		List<Record> bannerList = Db.find("select * from activitibanner where id=1");
		return bannerList;
		
	}

}
