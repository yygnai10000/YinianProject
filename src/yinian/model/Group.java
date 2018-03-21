package yinian.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import yinian.common.CommonParam;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class Group extends Model<Group> {
	public static final Group dao = new Group();
	public static String selectForActivitySpace = "select groupid,gimid,gname,gpic,gnum,gtime,gtype,gcreator,ginvite,gintroduceText,gintroducePic ";
	public static String selectForSpace = "select isDefault,openGId,groupid,gimid,gname,gpic,gnum,gtime,gmnotify,gtype,gcreator,ginvite,gintroduceText,gintroducePic,gmorder,gmtime,gOrigin,simAppPic ";

	/**
	 * 判断是不是官方空间,true为是官方空间
	 * 
	 * @param groupid
	 * @return
	 */
	public boolean JudgeIsOfficialSpace(int groupid) {
		Group group = dao.findById(groupid);
		int gtype = Integer.parseInt(group.get("gtype").toString());
		return gtype == CommonParam.officialSpaceTypeValue;
	}

	/**
	 * 获取活动空间基本信息
	 * 
	 * @return
	 */
	public static List<Record> GetActivitySpaceBasicInfo() {
		List<Record> list = Db.find(selectForActivitySpace
				+ " from groups where gIsActivitySpace=0 and gIsShow=0 and gstatus=0 order by groupid desc ");
		return list;
	}

	/**
	 * 获取未排序空间基本信息
	 * 
	 * @param userid
	 * @return
	 */
	public static List<Record> GetUnOrderSpaceBasicInfo(int userid) {
		System.out.println(selectForSpace + " from groups,groupmembers where groupid=gmgroupid and gmuserid="
				+ userid + " and gstatus=0 and gmstatus=0 and gtype not in(5,12) and groupid not in ('"
				+ CommonParam.ActivitySpaceID + "') order by gtype asc,gmtime desc" );
		List<Record> list = Db.find(selectForSpace + " from groups,groupmembers where groupid=gmgroupid and gmuserid="
				+ userid + " and gstatus=0 and gmstatus=0 and gtype not in(5,12) and groupid not in ("
				+ CommonParam.ActivitySpaceID + ") order by gtype asc,gmtime desc ");
		return list;
	}
//	/**
//	 * 获取未排序空间基本信息 by lk 查询是否置顶
//	 * 
//	 * @param userid
//	 * @return
//	 */
//	public static List<Record> GetUnOrderSpaceBasicInfoWithTop(int userid) {
//		String sql=selectForSpace+",isTop" + " from groups,groupmembers left join groupsistop on tGroupId=gmgroupid where groupid=gmgroupid and gmuserid="
//				+ userid + " and gstatus=0 and gmstatus=0 and gtype not in(5,12) and groupid not in ("
//				+ CommonParam.ActivitySpaceID + ") order by isTop desc, gtype asc,gmtime desc ";
//		List<Record> list = Db.find(sql);
//		return list;
//	}
	/**
	 * 获取已排序空间基本信息
	 * 
	 * @param userid
	 * @return
	 */
	public static List<Record> GetAlreadyOrderSpaceBasicInfo(int userid) {
		List<Record> list = Db.find(selectForSpace + " from groups,groupmembers where groupid=gmgroupid and gmuserid="
				+ userid + " and gstatus=0 and gmstatus=0 and gtype not in(5,12) and groupid not in ("
				+ CommonParam.ActivitySpaceID + ") order by gmorder ");
		return list;
	}

	/**
	 * 获取搜索空间基本信息
	 * 
	 * @param spaceName
	 * @return
	 */
	public static List<Record> GetSearchSpaceBasicInfo(String spaceName) {
		List<Record> list = Db.find(
				"select groupid,gimid,gname,gpic,gnum,gtime,gtype,gcreator,ginvite,gintroduceText,gintroducePic from groups where gname like '%"
						+ spaceName + "%' and gstatus=0 and gtype=13");
		return list;
	}

	/**
	 * 获取空间照片数
	 */
	public static List<Record> GetSpacePhotoNum(List<Record> spaceList) {

		if (spaceList.size() == 0)
			return spaceList;

		String groupid = "";
		for (Record record : spaceList) {
			if(record.get("groupid").toString().equals(CommonParam.pGroupId+"")
					||record.get("groupid").toString().equals(CommonParam.pGroupId2+"")
					||record.get("groupid").toString().equals(CommonParam.pGroupId3+"")){
				//record.set("gpicnum", "100000");
			}else{
				groupid += record.get("groupid").toString() + ",";
			}
		}
		groupid = groupid.substring(0, groupid.length() - 1);
//		System.out.println("首页："+System.currentTimeMillis());
//		List<Record> photoList = Db.find(
//				"select groupid,count(*) as gpicnum from groups,events,pictures where peid=eid and groupid=egroupid and groupid in ("
//						+ groupid + ") and gstatus=0 and estatus in(0,3) and pstatus=0 group by groupid desc");
//						System.out.println("首页end："+System.currentTimeMillis());
//		
		//by lk		
		List<Record> photoList = Db.find(
				"select pGroupid groupid,count(*) as gpicnum from pictures where pGroupid in ("
						+ groupid + ") and pstatus=0 group by pGroupid desc");
		return photoList;
	}
	

	/**
	 * 获取空间照片数
	 */
	public static List<Record> GetSpacePhotoNumNew(List<Record> spaceList) {

		if (spaceList.size() == 0)
			return spaceList;

		String groupid = "";
		for (Record record : spaceList) {
			groupid += record.get("groupid").toString() + ",";
		}
		groupid = groupid.substring(0, groupid.length() - 1);

		List<Record> photoList = Db.find(
				"select groupid,count(*) as gpicnum from groups,events,pictures where peid=eid and groupid=egroupid and groupid in ("
						+ groupid + ") and gstatus=0 and estatus=0 and pstatus=0 group by groupid desc");

		return photoList;
	}

	/**
	 * 获取空间动态数
	 */
	public static List<Record> GetSpaceEventNum(List<Record> spaceList) {

		if (spaceList.size() == 0)
			return spaceList;

		String groupid = "";
		for (Record record : spaceList) {
			groupid += record.get("groupid").toString() + ",";
		}
		groupid = groupid.substring(0, groupid.length() - 1);

		List<Record> eventList = Db
				.find("select groupid,count(*) as eventnum from groups,events where groupid=egroupid and groupid in ("
						+ groupid + ") and gstatus=0 and estatus in(0,3) GROUP BY groupid desc");

		return eventList;
	}
	
	/**
	 * 获取空间动态数
	 */
	public static List<Record> GetSpaceEventNumNew(List<Record> spaceList) {

		if (spaceList.size() == 0)
			return spaceList;

		String groupid = "";
		for (Record record : spaceList) {
			groupid += record.get("groupid").toString() + ",";
		}
		groupid = groupid.substring(0, groupid.length() - 1);

		List<Record> eventList = Db
				.find("select groupid,count(*) as eventnum from groups,events where groupid=egroupid and groupid in ("
						+ groupid + ") and gstatus=0 and estatus in(0,3) GROUP BY groupid desc");

		return eventList;
	}

	/**
	 * 获取空间内所有拍立得
	 */
	public List<Record> getAllScanEventsInSpace(String groupid) {
		List<Record> list = Db.find("select eid,eVerifyPic from events where eMain=5 and estatus=0 and egroupid="
				+ groupid + " group by eid desc ");
		return list;
	}

	/**
	 * 获取空间默认封面列表
	 */
	public static List<Record> GetSpaceDefaultCoverList() {
		List<Record> list = Db.find("select acurl from albumcover ");
		return list;
	}
	/**
	 * 获取空间新默认封面列表
	 */
	public static List<Record> GetNewSpaceDefaultCoverList(int type) {
		List<Record> list = Db.find("select acurl from albumcover where acgtype="+type+" and acstatus=0");
		return list;
	}
	/**
	 * 获取所有空间邀请码
	 */
	public static List<Record> GetAllSpaceInviteCode() {
		List<Record> list = Db.find("select distinct ginvite from groups ");
		return list;
	}

	/**
	 * 生成空间邀请码
	 */
	public static String CreateSpaceInviteCode() {
		// 生成8位随机码当做相册邀请码
		String base = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		Random random = new Random();

		// 获取唯一的邀请码
		boolean flag = false;// false表示邀请码已存在，true表示邀请码不存在
		String inviteCode = "";
		do {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < 6; i++) {
				int number = random.nextInt(base.length());
				sb.append(base.charAt(number));
			}
			inviteCode = sb.toString();// inviteCode为随机生成的邀请码
			// 进行判断
			List<Record> list = Db.find("select ginvite from groups where ginvite='" + inviteCode + "' ");
			flag = (list.size() == 0);

		} while (!flag);

		return inviteCode;
	}
	/**
	 * 获取未排序空间基本信息 by lk 查找自己加入或创建的相册
	 * 
	 * @param userid
	 * @return
	 */
	public static List<Record> GetUnOrderSpaceBasicInfoByCreateOrJoin(int userid,String mode) {
		List<Record> list=new ArrayList<Record>();
		if(mode.equals("create")){
			list = Db.find(selectForSpace + " from groups,groupmembers where gcreator="+userid+" and groupid=gmgroupid and gmuserid="
					+ userid + " and gstatus=0 and gmstatus=0 and gtype not in(5,12) and groupid not in ("
					+ CommonParam.ActivitySpaceID + ") order by gtype asc,gmtime desc ");			
		}else{
			list = Db.find(selectForSpace + " from groups,groupmembers where gcreator <>"+userid+" and groupid=gmgroupid and gmuserid="
					+ userid + " and gstatus=0 and gmstatus=0 and gtype not in(5,12) and groupid not in ("
					+ CommonParam.ActivitySpaceID + ") order by gtype asc,gmtime desc ");	
		}
		
		return list;
	}
}
