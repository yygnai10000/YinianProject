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

	private String jsonString;// 返回结果
	private JsonData jsonData = new JsonData(); // json操作类
	private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	/**
	 * 获取活动相册内容
	 * 
	 * @return
	 */
	public List<Record> getActivitySpace(int userid) {

		// 先从缓存中查找
		List<Record> list = CacheKit.get("ServiceCache", "activitySpace");
		// 如果缓存为空
		if (list == null) {
			// 获取空间基本信息
			list = Group.GetActivitySpaceBasicInfo();
			// 插入其他信息
			for (Record record : list) {
				record.set("shareTitle", "李易峰周丨来赞我晒的李易峰，帮我拿大奖吧！")
						.set("shareContent", "我上传了李易峰的照片，拜托拜托快来赞我帮我拿爱奇艺会员和同款Dior墨镜吧！进来就能抢红包哦！")
						.set("bannerPic", "7xlmtr.com1.z0.glb.clouddn.com/activity_bannerliyifeng.jpg")
						.set("introducePic", "7xlmtr.com1.z0.glb.clouddn.com/wxtp_20170810140647.jpg");
			}
			// 获取空间照片数
			List<Record> photoList = Group.GetSpacePhotoNum(list);
			// 将空间照片数插入到空间信息中
			list = dataProcess.combineSpaceInfoWithPhotoNum(list, photoList, userid);
			// 获取空间动态数
			List<Record> eventList = Group.GetSpaceEventNum(list);
			// 将空间动态数插入到空间信息中
			list = dataProcess.combineSpaceInfoWithEventNum(list, eventList);
			// 将空间类型转换成相应的文字
			list = dataProcess.changeGroupTypeIntoWord(list);

			// 将数据放入缓存中
			CacheKit.put("ServiceCache", "activitySpace", list);
		}

		// 返回结果
		return list;
	}

	/**
	 * 获取搜索动态
	 * 
	 * @param groupid
	 * @param nickname
	 * @return
	 */
	public List<Record> getSearchEvents(String groupid, String nickname) {

		// 获取空间所有评论
		List<Record> commentList = new Comment().GetAllCommentsOfOneSpace(groupid);

		// 获取动态内容
		Event event = new Event();
		List<Record> eventList = event.SearchSpaceEventsByNickname(groupid, nickname);

		// 动态数据封装
		List<Record> list = dataProcess.eventDataEncapsulation(eventList, commentList, "app");

		// 返回数据
		return list;

	}

	/**
	 * 获取空间排行榜
	 * 
	 * @param groupid
	 * @return
	 */
	public List<Record> getSpaceEventChart(int groupid) {
		// 获取满足条件的动态
		List<Record> eventList = Db.find(
				"SELECT userid,unickname,upic,count(*) AS num FROM users,`events`,`like` WHERE userid = euserid AND eid = likeEventID AND estatus = 0 AND likeStatus != 1 AND egroupid = "
						+ groupid + " GROUP BY eid HAVING num >= 3 ORDER BY num DESC");
		// 筛选
		Set<String> userSet = new HashSet<String>();
		int totalCount = 1;// 记录总数
		int sameCount = 1;// 记录同名次
		int lastNum = 0;// 记录上一个人的点赞数
		List<Record> result = new ArrayList<Record>();

		for (Record record : eventList) {
			String userid = record.get("userid").toString();
			int num = Integer.parseInt(record.get("num").toString());
			if (!userSet.contains(userid)) {
				// 说明该用户不重复
				userSet.add(userid);
				if (num == lastNum) {
					// 与前一个并列
					record.set("rank", sameCount);
				} else {
					// 比前一个小
					record.set("rank", totalCount);
					lastNum = num;
					sameCount = totalCount;
				}
				totalCount++;
				result.add(record);

			}
		}

		// 超过50的排名则截取多余信息
		if (result.size() > 50) {
			int fiftyRank = result.get(49).getInt("rank");
			for (int i = 50; i < result.size(); i++) {
				int temp = result.get(i).getInt("rank");
				if (temp != fiftyRank) {
					// 不在再并列50名
					result = new ArrayList<Record>(result.subList(0, i));
				}
			}
		}

		return result;
	}

	/**
	 * 签到
	 * 
	 * @param userid
	 * @param signType
	 *            签到类型 0--小程序 1--APP
	 * @return
	 */
	public boolean signIn (String userid, String signType) throws ParseException{

		// 获取日期
		Calendar cal = Calendar.getInstance();
		String today = sdf.format(cal.getTime());
		cal.add(Calendar.DATE, -1);
		String yesterday = sdf.format(cal.getTime());

		// 判断用户之前是否签过到
		Sign sign = new Sign();
		List<Record> userSignInfo = sign.getUserSignInInfo(userid, signType);
		if (userSignInfo.size() == 0) {
			// 未签到过
			sign.set("signUserID", userid).set("signStartDate", today).set("signEndDate", today).set("signType",
					signType);
			return sign.save();
		} else {
			// 判断是否断签
			String signEndDate = userSignInfo.get(0).get("signEndDate").toString();
			String signID = userSignInfo.get(0).get("signID").toString();

			// 已签到过,当日无法再签到
			if (signEndDate.equals(today))
				return false;

			sign = new Sign().findById(signID);
			if (signEndDate.equals(yesterday)) {
				// 未断签
				sign.set("signEndDate", today);
			} else {
				// 已断签
				sign.set("signStartDate", today).set("signEndDate", today);
			}
			return sign.update();

		}

	}
	
	/**
	 * 签到(新)
	 * 
	 * @param userid
	 * @param signType
	 *            签到类型 0--小程序 1--APP
	 * @return
	 */
	public boolean signIn2 (String userid, String signType) throws ParseException{

		// 获取日期
		Calendar cal = Calendar.getInstance();
		String today = sdf.format(cal.getTime());
		cal.add(Calendar.DATE, -1);
		String yesterday = sdf.format(cal.getTime());

		// 判断用户之前是否签过到
		Sign sign = new Sign();
		List<Record> userSignInfo = sign.getUserSignInInfo(userid, signType);
		System.out.println(userSignInfo);
		if (userSignInfo.size() == 0) {
			// 未签到过
			sign.set("signUserID", userid).set("signStartDate", today).set("signEndDate", today).set("signType",
					signType).set("signCount", 1);
			return sign.save();
		} else {
			// 判断是否断签
			String signEndDate = userSignInfo.get(0).get("signEndDate").toString();
			String signStartDate = userSignInfo.get(0).get("signStartDate").toString();
			String signID = userSignInfo.get(0).get("signID").toString();

			// 已签到过,当日无法再签到
			if (signEndDate.equals(today))
				return false;

			sign = new Sign().findById(signID);
			if (signEndDate.equals(yesterday)) {
				// 未断签
				Integer signCount = userSignInfo.get(0).getInt("signCount");//最大历史签到时间
				//把第一次到达领奖条件时的信息保存到物流表(主要是首次签到的时间和第一次到达领奖条件的时间)
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
				//判断断签后连续签到天数是否大于历史最大签到天数
				if(signDay>=signCount) {
					sign.set("signCount", signDay+1);
				}
				sign.set("signEndDate", today);
				
			} else {
				// 已断签
				sign.set("signStartDate", today).set("signEndDate", today);
			}
			return sign.update();

		}

	}
	
	/**
	 * 签到获取积分---ly
	 */
	public boolean signInPoints (String userid, String signType) throws ParseException{

		// 获取日期
		Calendar cal = Calendar.getInstance();
		String today = sdf.format(cal.getTime());
		cal.add(Calendar.DATE, -1);
		String yesterday = sdf.format(cal.getTime());

		// 判断用户之前是否签过到
		Signold sign = new Signold();
		List<Record> userSignInfo = sign.getUserSignInInfo(userid, signType);
		if (userSignInfo.size() == 0) {
			// 未签到过
			sign.set("signUserID", userid).set("signStartDate", today).set("signEndDate", today).set("signType",
					signType);
			return sign.save();
		} else {
			// 判断是否断签
			String signEndDate = userSignInfo.get(0).get("signEndDate").toString();
			String signStartDate = userSignInfo.get(0).get("signStartDate").toString();
			String signID = userSignInfo.get(0).get("signID").toString();

			// 已签到过,当日无法再签到
			if (signEndDate.equals(today))
				return false;

			sign = new Signold().findById(signID);
			if (signEndDate.equals(yesterday)) {
				// 未断签
				sign.set("signEndDate", today);
				long to = sdf.parse(signEndDate).getTime();
				long from = sdf.parse(signStartDate).getTime();
				int signDay = (int) ((to - from) / (1000 * 60 * 60 * 24)) + 1;
				if(signDay >= 7) {
					sign.set("signStartDate", today).set("signEndDate", today);
				}
			} else {
				// 已断签
				sign.set("signStartDate", today).set("signEndDate", today);
			}
			return sign.update();
		}
	}
	/**
	 * 记录成功邀请好友
	 */
	public boolean recordSuccessInviteFriend(String userid) {
		Encourage en = new Encourage();
		List<Record> encourageInfo = Encourage.getEncourageInfo(userid);

		if (encourageInfo.size() == 0) {
			// 无数据，插入新数据
			en.set("encourageUserID", userid).set("inviteNum", 1);
			return en.save();
		} else {
			// 有数据，更新原有数据
			String encourageID = encourageInfo.get(0).get("encourageID").toString();
			int inviteNum = Integer.parseInt(encourageInfo.get(0).get("inviteNum").toString());
			en = new Encourage().findById(encourageID).set("inviteNum", inviteNum + 1);
			return en.update();
		}
	}
	
	
	
	

	/**
	 * 获取城市排行
	 */
	public List<Record> getCityRank() {
		List<Record> list = CacheKit.get("ServiceCache", "cityRank");
		if (list == null) {
			list = Db.find(
					"select groupid,gname,gpic,count(*) as num from groups,`events`,pictures where groupid=egroupid and eid=peid and gtype=14 and gstatus=0 and estatus=0 and pstatus=0 group by groupid order by count(*) desc");
			// 插入排名
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
	 * 获取城市贡献榜
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
	 * 获取城市空间信息
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
	 * 获取我的贡献
	 */
	public Record getMyContributionInCitySpace(String userid, String groupid) {
		int myRank = 0;
		// 获取我的排名
		List<Record> rankList = getCityContributionRank(groupid);
		for (Record record : rankList) {
			String tempUserid = record.get("userid").toString();
			if (userid.equals(tempUserid)) {
				// 在榜单内
				myRank = record.getInt("rank");
			}
		}
		// 获取贡献
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
	 * 获取所有活动相册
	 */
	public  List<Record> getAllActivitiGroups(String type,String number){
		List<Record> activitiGroupList = new ArrayList<>();
		if(type.equals("initialize")) {
			activitiGroupList = Db.find("select gname,gpic,number,groupid,activitiStatus,activitiStartTime,activitiEndTime from activitigroups,groups "
					+ " where groupid=activitiGroupid and isDelete=0 "
					+ " order by number asc limit 2");
		}else {
			//分页加载
			activitiGroupList = Db.find("select gname,gpic,number,groupid,activitiStatus,activitiStartTime,activitiEndTime from activitigroups,groups "
					+ " where groupid=activitiGroupid and number>"+number+" and isDelete=0 "
					+ " order by number asc limit 2");
		}
		return activitiGroupList;
		
	}
	
	/**
	 * 获取我参加的活动相册
	 */
	public List<Record> getMyActivitiGroups(String jointime,String userid,String type){
		List<Record> myActivitiList = new ArrayList<>();
		if(type.equals("initialize")) {
			myActivitiList = Db.find("select gname,gpic,number,groupid,activitiStatus,activitiStartTime,activitiEndTime,joinTime from activitimembers,activitigroups,groups "
					+ "where groupid=activitiGroupid and groupid=joinGroupid and joinUserid="+userid+" and isDelete=0 order by joinTime desc limit 2");
//			myActivitiList = Db.find("select gname,gpic,groupid,activitiStatus,activitiStartTime,activitiEndTime from groupmembers,activitigroups,groups "
//					+ "where groupid=activitiGroupid and groupid=gmGroupid and gmUserid="+userid+" order by groupid desc limit 10");
		}else {
			//分页加载
			myActivitiList = Db.find("select gname,gpic,number,groupid,activitiStatus,activitiStartTime,activitiEndTime,joinTime from activitimembers,activitigroups,groups "
					+ "where groupid=activitiGroupid and groupid=joinGroupid and joinUserid="+userid+ " and jointime<'"+jointime+"' and isDelete=0 order by joinTime desc limit 2");
//			myActivitiList = Db.find("select gname,gpic,groupid,activitiStatus,activitiStartTime,activitiEndTime from groupmembers,activitigroups,groups "
//					+ "where groupid=activitiGroupid and groupid=gmGroupid and gmUserid="+userid+ " and groupid<"+groupid+" order by groupid desc limit 10");
		}
		return myActivitiList;
		
	}
	
	
	/**
	 * 获得首页banner图
	 * @return
	 */
	public List<Record> getActivitiBanner(){
		List<Record> bannerList = Db.find("select * from activitibanner where id=1");
		return bannerList;
		
	}

}
