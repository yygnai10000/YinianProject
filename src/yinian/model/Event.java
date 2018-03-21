package yinian.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.ehcache.CacheKit;

import redis.clients.jedis.Jedis;
import yinian.common.CommonParam;
import yinian.service.SpaceService;
import yinian.utils.RedisUtils;

public class Event extends Model<Event> {
	public static final Event dao = new Event();
	public static String selectForEvent = "SELECT eid,egroupid,userid,unickname,upic,etext,efirstpic,eVerifyPic,ePeopleName,eaudio,eplace,ePlacePic,eMain,euploadtime,eView,isTopInRecommendGroup,isRecommend,elevel ";
	public static String selectForEvent2 = "SELECT gname,isDefault,openGId,eid,egroupid,userid,unickname,upic,etext,efirstpic,eVerifyPic,ePeopleName,eaudio,eplace,ePlacePic,eMain,euploadtime,eView,isTopInRecommendGroup,isRecommend,elevel ";
	private static String selectForMyEvent = "SELECT eid,etext,egroupid,gname,userid,unickname,upic,etext,efirstpic,eVerifyPic,ePeopleName,eaudio,eplace,ePlacePic,eMain,euploadtime,eView,isTopInRecommendGroup,isRecommend,elevel";
	public static String selectForEvent_sk = "SELECT gname,isDefault,openGId,eid,egroupid,euserid userid,etext,efirstpic,eVerifyPic,ePeopleName,eaudio,eplace,ePlacePic,eMain,euploadtime,eView,isTopInRecommendGroup,isRecommend,elevel ";
	/********************* <Tip> **********************/
	// 1--APP端不显示短视频，即eMain不为4
	// 2--小程序端只显示图片和短视频，即eMain为0和4
	/********************* <Tip> **********************/

	/**
	 * 获取单条动态内容
	 * 
	 * @param eid
	 * @return
	 */
	public List<Record> GetSingleEventContent(int eid) {
		List<Record> list = Db.find(
				"SELECT eid,egroupid,gcreator,userid,unickname,upic,etext,efirstpic,eVerifyPic,ePeopleName,eaudio,eplace,ePlacePic,eMain,euploadtime,eView,isTopInRecommendGroup,isRecommend,elevel FROM users,`events`,groups WHERE userid = euserid and egroupid=groupid AND eid = "
						+ eid + " AND estatus = 0 ");
		return list;
	}

	/**
	 * 获取空间内置顶动态//by lk 添加小程序特殊判断
	 */
	public List<Record> GetAllTopEvent(String groupid, String source) {

		String sqlForEvent = "";
		if (source != null && source.equals("ynxc")) {
			sqlForEvent = selectForEvent + " FROM users,`events` WHERE userid = euserid AND egroupid = " + groupid
					+ " AND estatus = 0 and eMain in (0,4) and elevel=1 GROUP BY eTopTime DESC ";//eid
		}else if (source != null && source.equals("smallApp")) {
			sqlForEvent = selectForEvent + " FROM users,`events` WHERE userid = euserid AND egroupid = " + groupid
					+ " AND estatus = 0 and elevel=1 GROUP BY eTopTime DESC ";
		} else {
			sqlForEvent = selectForEvent + " FROM users,`events` WHERE userid = euserid AND egroupid = " + groupid
					+ " AND estatus = 0 and eMain!=4 and elevel=1 GROUP BY eTopTime DESC ";
		}
		List<Record> list = Db.find(sqlForEvent);

		return list;
	}

	/**
	 * 初始化空间动态内容 by lk 添加h5 特殊判断
	 * 
	 * @param groupid
	 * @return
	 */
	public List<Record> InitializeEventContent(String groupid, String source) {

		String sqlForEvent = "";
		if (source != null && source.equals("ynxc")) {
			sqlForEvent = selectForEvent + " FROM users,`events` WHERE userid = euserid AND egroupid = " + groupid
					+ " AND estatus = 0  and eMain in (0,4) and elevel=0 GROUP BY eid DESC LIMIT 10";
		}else if ((source != null && (source.equals("smallApp") || source.equals("PC")))) {
			sqlForEvent = selectForEvent + " FROM users,`events` WHERE userid = euserid AND egroupid = " + groupid
					+ " AND estatus = 0 and elevel=0 GROUP BY eid DESC LIMIT 10";
		} else {
			sqlForEvent = selectForEvent + " FROM users,`events` WHERE userid = euserid AND egroupid = " + groupid
					+ " AND estatus = 0 and eMain!=4 and elevel=0 GROUP BY eid DESC LIMIT 10";
		}
		List<Record> list = Db.find(sqlForEvent);

		return list;

	}

	/**
	 * 加载空间动态内容 by lk 添加h5特殊判断
	 *
	 * @param groupid
	 * @return
	 */
	public List<Record> LoadingEventContent(String groupid, int eid, String source) {

		String sqlForEvent = "";
		if (source != null &&source.equals("ynxc")) {
			sqlForEvent = selectForEvent + " FROM users,`events` WHERE userid = euserid AND egroupid = " + groupid
					+ " and eid <" + eid + " and eMain in (0,4) AND estatus = 0 and elevel=0 GROUP BY eid DESC LIMIT 10";
		}else if ((source != null && (source.equals("smallApp") || source.equals("PC"))) || groupid.equals("16876")) {
			sqlForEvent = selectForEvent + " FROM users,`events` WHERE userid = euserid AND egroupid = " + groupid
					+ " and eid <" + eid + " AND estatus = 0 and elevel=0 GROUP BY eid DESC LIMIT 10";
		} else {
			sqlForEvent = selectForEvent + " FROM users,`events` WHERE userid = euserid AND egroupid = " + groupid
					+ " and eid <" + eid + " AND estatus = 0 and eMain!=4 and elevel=0 GROUP BY eid DESC LIMIT 10";
		}

		List<Record> list = Db.find(sqlForEvent);
		return list;

	}

	/**
	 * 刷新空间动态内容 by lk 添加h5特殊判断
	 * 
	 * @param groupid
	 * @return
	 */
	public List<Record> RefreshEventContent(String groupid, int eid, String source) {

		String sqlForEvent = "";
		if (source != null && source.equals("ynxc")) {
			sqlForEvent = selectForEvent + " FROM users,`events` WHERE userid = euserid AND egroupid = " + groupid
					+ " and eid >" + eid + " and eMain in (0,4) AND estatus = 0 and elevel=0 GROUP BY eid DESC ";
		} else if ((source != null && (source.equals("smallApp") || source.equals("PC"))) || groupid.equals("16876")) {
			sqlForEvent = selectForEvent + " FROM users,`events` WHERE userid = euserid AND egroupid = " + groupid
					+ " and eid >" + eid + " AND estatus = 0 and elevel=0 GROUP BY eid DESC ";
		} else {
			sqlForEvent = selectForEvent + " FROM users,`events` WHERE userid = euserid AND egroupid = " + groupid
					+ " and eid >" + eid + " AND estatus = 0 and eMain!=4 and elevel=0 GROUP BY eid DESC ";
		}

		List<Record> list = Db.find(sqlForEvent);
		return list;

	}

	/**
	 * 初始化用户上传的动态
	 * 
	 * @param userid
	 * @return
	 */
	public static List<Record> InitializeUserUploadEvents(String userid, String source) {

		String sqlForEvent = "";
		if (source != null && source.equals("smallApp")) {
			sqlForEvent = selectForMyEvent
					+ " FROM users,groups,events WHERE userid=euserid and groupid = egroupid and euserid = " + userid
					+ " and egroupid!=104851 and estatus in(0,3)  GROUP BY eid DESC limit 10";
		} else {
			sqlForEvent = selectForMyEvent
					+ " FROM users,groups,events WHERE userid=euserid and groupid = egroupid and euserid = " + userid
					+ " and egroupid!=104851 and estatus in(0,3) and eMain!=4  GROUP BY eid DESC limit 10";
		}

		List<Record> list = Db.find(sqlForEvent);
		return list;
	}

	/**
	 * 加载用户上传的动态
	 * 
	 * @param userid
	 * @param eid
	 * @return
	 */
	public static List<Record> LoadingUserUploadEvents(String userid, int minID, String source) {

		String sqlForEvent = "";
		if (source != null && source.equals("smallApp")) {
			sqlForEvent = selectForMyEvent
					+ " FROM users,groups,events WHERE userid=euserid and groupid = egroupid and euserid = " + userid
					+ " and egroupid!=104851 and estatus in(0,3) and eid<" + minID + " GROUP BY eid DESC limit 10";
		} else {
			sqlForEvent = selectForMyEvent
					+ " FROM users,groups,events WHERE userid=euserid and groupid = egroupid and euserid = " + userid
					+ " and egroupid!=104851 and estatus in(0,3) and eid<" + minID
					+ " and eMain!=4  GROUP BY eid DESC limit 10";
		}

		List<Record> list = Db.find(sqlForEvent);
		return list;
	}

	/**
	 * 初始化空间成员上传的动态
	 * 
	 * @param userid
	 * @return
	 */
	public static List<Record> InitializeSpaceMemberUploadEvents(int userid, int groupid, String source) {

		String sqlForEvent = "";
		if (source != null && source.equals("smallApp")) {
			sqlForEvent = selectForEvent + " FROM events,users where userid = euserid and egroupid = " + groupid
					+ " and euserid = " + userid + " and estatus=0 GROUP BY eid DESC limit 10";
		} else {
			sqlForEvent = selectForEvent + " FROM events,users where userid = euserid and egroupid = " + groupid
					+ " and euserid = " + userid + " and estatus=0 and eMain!=4 GROUP BY eid DESC limit 10";
		}

		List<Record> list = Db.find(sqlForEvent);
		return list;
	}
	

	/**
	 * 加载空间成员上传的动态
	 * 
	 * @param userid
	 * @param eid
	 * @return
	 */
	public static List<Record> LoadingSpaceMemberUploadEvents(int userid, int groupid, int minID, String source) {

		String sqlForEvent = "";
		if (source != null && source.equals("smallApp")) {
			sqlForEvent = selectForEvent + " FROM events,users where userid = euserid and egroupid = " + groupid
					+ " and euserid = " + userid + " and eid < " + minID + " and estatus=0 GROUP BY eid DESC limit 10";
		} else {
			sqlForEvent = selectForEvent + " FROM events,users where userid = euserid and egroupid = " + groupid
					+ " and euserid = " + userid + " and eid < " + minID
					+ " and estatus=0 and eMain!=4 GROUP BY eid DESC limit 10";
		}

		List<Record> list = Db.find(sqlForEvent);
		return list;
	}
	

	/**
	 * 关联动态和其照片
	 */
	public Record CombineEventAndPicture(Record eventRecord) {
		int eid = Integer.parseInt(eventRecord.get("eid").toString());
		List<Record> picList = Db
				.find("select pid,poriginal,ptime,pcover from pictures where peid=" + eid + " and pstatus=0 ");
		eventRecord.set("picList", picList);
		return eventRecord;
	}
	/**
	 * 关联动态和其照片
	 */
	public Record CombineEventAndPictureNew(Record eventRecord) {
		int eid = Integer.parseInt(eventRecord.get("eid").toString());
		List<Record> picList = Db
				.find("select pid,poriginal,ptime,pcover from pictures where peid=" + eid + " and pstatus=0 ");	
		eventRecord.set("picnum", picList.size());
		if(picList.size()!=0) {
			if(picList.size()>9) {
				for(int j=9;j<picList.size();j++) {
					picList.remove(j);
					j--;
				}
			}
			eventRecord.set("picList", picList);
		}else {
			eventRecord.set("picList", new ArrayList<>());
		}
		return eventRecord;
	}
	/**
	 * 关联动态和其照片 by lk 时刻
	 */
	public Record CombineEventAndPictureShowMoments(Record eventRecord) {
		int eid = Integer.parseInt(eventRecord.get("eid").toString());
		List<Record> picList = Db
				.find("select count(*) cnt,pid,poriginal,ptime,pcover from pictures where peid=" + eid + " and pstatus=0 order by pid asc limit 1");
		eventRecord.set("picList", picList);
		return eventRecord;
	}
	/**
	 * 关联动态和其照片 by lk
	 */
	public List<Record> CombineEventAndPictureByLk(String eids) {
		//int eid = Integer.parseInt(eventRecord.get("eid").toString());
		List<Record> picList = Db
				.find("select peid as eid,pid,poriginal,ptime,pcover from pictures where peid in("+eids+") and pstatus=0 ");
		//eventRecord.set("picList", picList);
		return picList;
	}
	/**
	 * 根据昵称搜索空间动态
	 */
	public List<Record> SearchSpaceEventsByNickname(String groupid, String nickname) {
		List<Record> list = Db.find(selectForEvent + " FROM events,users where userid = euserid and egroupid = "
				+ groupid + " and unickname like '%" + nickname + "%' and estatus=0 and eMain!=4  GROUP BY eid DESC ");
		return list;
	}

	/**
	 * 初始化短视频墙
	 */
	public List<Record> initializeShortVideoWall(String groupid) {
		List<Record> list = Db
				.find("select pid,poriginal from events,pictures where peid=eid and eMain=4 and estatus=0 and egroupid="
						+ groupid + " order by pid desc limit 30 ");
		return list;
	}

	/**
	 * 加载短视频墙
	 */
	public List<Record> loadingShortVideoWall(String groupid, String pid) {
		List<Record> list = Db
				.find("select pid,poriginal from events,pictures where peid=eid and eMain=4 and estatus=0 and egroupid="
						+ groupid + " and pid<" + pid + " order by pid desc limit 30  ");
		return list;
	}

	/**
	 * 增加随机浏览量
	 */
	public static void AddRandomView(List<Record> eventList) {
		Random random = new Random();
		for (Record record : eventList) {
			int num = random.nextInt(90) + 10;
			String tempEid = record.get("eid").toString();
			Db.update("update `events` set eView=eView+" + num + " where eid=" + tempEid + " ");

		}
	}

	/**
	 * 初始化时刻
	 */
	public static List<Record> initializeMoments(String userid) {
		String sqlForEvent = selectForEvent2
				+ " from `events`,users,groups,groupmembers where userid=euserid and egroupid=groupid and groupid=gmgroupid and gmuserid="
				+ userid
				+ " and gstatus=0 and estatus=0 and gmstatus=0 and gtype not in (5) and eMain in (0,4) order by eid desc limit 20";
		List<Record> list = Db.find(sqlForEvent);
		return list;
	}
	/**
	 * 初始化时刻 by lk 显示10条
	 */
	public static List<Record> initializeMomentsBySim(String userid) {
		String sqlForEvent = selectForEvent2
				+ " from `events`,users,groups,groupmembers where userid=euserid and egroupid=groupid and groupid=gmgroupid and gmuserid="
				+ userid
				+ " and gstatus=0 and estatus=0 and gmstatus=0 and gtype not in (5) and eMain in (0,4) order by eid desc limit 10";
		List<Record> list = Db.find(sqlForEvent);
		return list;
	}
	/**
	 * 初始化时刻 by lk 显示10条
	 */
	public static List<Record> initializeMomentsBySim_new(String userid) {
		String sqlForEvent = selectForEvent_sk
				+ " from `events`,groups,groupmembers where 1 and egroupid=groupid and groupid=gmgroupid and gmuserid="
				+ userid
				+ " and gstatus=0 and estatus=0 and gmstatus=0 and gtype not in (5,11) and eMain in (0,4) order by eid desc limit 10";
		List<Record> list = Db.find(sqlForEvent);
		return list;
	}
	/**
	 * 初始化时刻 by lk 显示10条-不显示活动相册内容
	 */
	public static List<Record> initializeMomentsBySim_noGtype(String userid) {
		String sqlForEvent = selectForEvent2
				+ " from `events`,users,groups,groupmembers where userid=euserid and egroupid=groupid and groupid=gmgroupid and gmuserid="
				+ userid
				+ " and gstatus=0 and estatus=0 and gmstatus=0 and gtype not in (5,11) and eMain in (0,4) order by eid desc limit 10";
		List<Record> list = Db.find(sqlForEvent);
		return list;
	}
	/**
	 * 加载时刻
	 */
	public static List<Record> loadingMoments(String userid, String eid) {
		String sqlForEvent = selectForEvent2
				+ " from `events`,users,groups,groupmembers where userid=euserid and egroupid=groupid and groupid=gmgroupid and gmuserid="
				+ userid + " and gstatus=0 and estatus=0 and gmstatus=0 and eid<" + eid
				+ " and gtype not in (5) and eMain in (0,4) order by eid desc limit 20";
		List<Record> list = Db.find(sqlForEvent);
		return list;
	}
	/**
	 * 加载时刻
	 */
	public static List<Record> loadingMoments_new(String userid, String eid) {
		String sqlForEvent = selectForEvent_sk
				+ " from `events`,groups,groupmembers where 1 and egroupid=groupid and groupid=gmgroupid and gmuserid="
				+ userid + " and gstatus=0 and estatus=0 and gmstatus=0 and eid<" + eid
				+ " and gtype not in (5,11) and eMain in (0,4) order by eid desc limit 20";
		List<Record> list = Db.find(sqlForEvent);
		return list;
	}
	/**
	 * 加载时刻-不显示活动相册内容
	 */
	public static List<Record> loadingMoments_noGtype(String userid, String eid) {
		String sqlForEvent = selectForEvent2
				+ " from `events`,users,groups,groupmembers where userid=euserid and egroupid=groupid and groupid=gmgroupid and gmuserid="
				+ userid + " and gstatus=0 and estatus=0 and gmstatus=0 and eid<" + eid
				+ " and gtype not in (5,11) and eMain in (0,4) order by eid desc limit 20";
		List<Record> list = Db.find(sqlForEvent);
		return list;
	}
	/**
	 * 刷新时刻
	 */
	public static List<Record> refreshMoments(String userid, String eid) {
		String sqlForEvent = selectForEvent2
				+ " from `events`,users,groups,groupmembers where userid=euserid and egroupid=groupid and groupid=gmgroupid and gmuserid="
				+ userid + " and gstatus=0 and estatus=0 and gmstatus=0 and eid>" + eid
				+ " and gtype not in (5) and eMain in (0,4) order by eid desc ";
		List<Record> list = Db.find(sqlForEvent);
		return list;
	}
	/**
	 * 刷新时刻
	 */
	public static List<Record> refreshMoments_new(String userid, String eid) {
		String sqlForEvent = selectForEvent_sk
				+ " from `events`,groups,groupmembers where 1 and egroupid=groupid and groupid=gmgroupid and gmuserid="
				+ userid + " and gstatus=0 and estatus=0 and gmstatus=0 and eid>" + eid
				+ " and gtype not in (5,11) and eMain in (0,4) order by eid desc ";
		List<Record> list = Db.find(sqlForEvent);
		return list;
	}
	/**
	 * 刷新时刻-不显示活动相册内容
	 */
	public static List<Record> refreshMoments_noGtype(String userid, String eid) {
		String sqlForEvent = selectForEvent2
				+ " from `events`,users,groups,groupmembers where userid=euserid and egroupid=groupid and groupid=gmgroupid and gmuserid="
				+ userid + " and gstatus=0 and estatus=0 and gmstatus=0 and eid>" + eid
				+ " and gtype not in (5,11) and eMain in (0,4) order by eid desc ";
		List<Record> list = Db.find(sqlForEvent);
		return list;
	}
	/**
	 * 点赞排行
	 * 
	 * @param groupid
	 * @param limit
	 * @return
	 */
	public List<Record> GetListByGroup(int groupid, int limit) {
		
		List<Record> list = new ArrayList<Record>();
		//取redis缓存中该groupid相册下的动态的点赞数，得到点赞数前limit的动态的信息
		//如果缓存中没有则查询数据库并更新缓存
		/*Jedis jedis = RedisUtils.getRedis();
		if(null!=jedis) {
			//先获取groupid下的动态信息
			List<Record> eventList = Db.find("select eid,egroupid,euserid,eMain,unickname,upic from users,`events` "
					+ "where egroupid=" + groupid + " and userid=euserid and eMain in(0,4) AND estatus = 0 group by eid");
			//根据eid从redis读取点赞数，若缓存中没有则读取数据库并更新缓存
			if(!eventList.isEmpty()) {
				for(Record r : eventList) {
					String eid = r.get("eid").toString();
					String likeCnt = jedis.get("likeCnt_"+eid);
					if(null!=likeCnt&&!"".equals(likeCnt)) {
						r.set("cnt", Integer.valueOf(likeCnt));
					}else {
						List<Record> cntList = Db.find("select count(*) cnt from `like` where likeEventID="
								+ eid + " and likeStatus!=1 ");
						if(!cntList.isEmpty()) {
							r.set("cnt", cntList.get(0).getLong("cnt"));
							jedis.set("likeCnt_"+eid, cntList.get(0).get("cnt").toString());
						}
					}
				}
				//释放redis
				RedisUtils.returnResource(jedis);
				//eventList按点赞数降序排序
				Collections.sort(eventList,new Comparator<Record>(){
					public int compare(Record r0, Record r1) {
						return (r1.getInt("cnt")).compareTo(r0.getInt("cnt"));
					}
				});
				//返回点赞数前limit个结果
				list = eventList.size()>limit?eventList.subList(0, limit):eventList;
			}
				return list;
		}else {*/
			String sql = "select etext,eid,egroupid,euserid,eMain,unickname,upic,count(*) cnt " + "from users,`like`,`events` "
					+ "where egroupid=" + groupid
					+ " and userid=euserid  and likeEventID=eid and eMain in(0,4) AND estatus = 0 AND likeStatus != 1  group by eid order by cnt desc limit "
					+ limit;
			list = CacheKit.get("ServiceCache", groupid + "GetListByElikeAndGroup");
			if (list == null) {
				list = Db.find(sql);
				CacheKit.put("ServiceCache", groupid + "GetListByElikeAndGroup", list);
			}
			//list = Db.find(sql);
			
			return list;
	//	}
//		String sql = "select eid,egroupid,euserid,eMain,unickname,upic,count(*) cnt " + "from users,`like`,`events` "
//				+ "where egroupid=" + groupid
//				+ " and userid=euserid  and likeEventID=eid and eMain = 0 AND estatus = 0 AND likeStatus != 1  group by eid order by cnt desc limit "
//				+ limit;
//		List<Record> list = Db.find(sql);
	}
	
	/**
	 * 获取用户空间内发布照片数
	 * 
	 * @param groupid
	 * @param uid
	 * @param isFirst
	 * @param limit
	 * @return
	 */
	public List<Record> GetUsePublishPhotoCont(int groupid, int uid, boolean isFirst, int limit) {
		String sql = "select eid,egroupid,euserid,unickname,upic,count(*) cnt  from users,events,pictures "
				+ "where userid=euserid and eid=peid and egroupid=" + groupid
				+ " and estatus=0 and pstatus=0 group by userid order by count(*) desc limit " + limit;
		if (uid != 0) {
			sql = "select eid,egroupid,euserid,unickname,upic,count(*) cnt  from users,events,pictures "
					+ "where userid=euserid and eid=peid and euserid=" + uid + " and egroupid=" + groupid
					+ " and estatus=0 and pstatus=0 group by userid order by count(*) desc";
			if (!isFirst) {
				sql = "select eid,egroupid,euserid,unickname,upic,count(*) cnt  " + "from users,events,pictures "
						+ "where userid=euserid and eid=peid and egroupid=" + groupid
						+ " and estatus=0 and pstatus=0 group by userid order by count(*) desc,userid desc limit "
						+ limit;
			}
		}
		return Db.find(sql);
	}
	/**
	 * 获取用户发布照片数
	 * 
	 * @param groupid
	 * @param uid
	 * @param isFirst
	 * @param limit
	 * @return
	 */
	public List<Record> GetUseAllPublishPhotoCont(int groupid, int uid, boolean isFirst, int limit) {
		/*Calendar c = Calendar.getInstance();
	      int year = c.get(Calendar.YEAR);//获取年份
	      int month=c.get(Calendar.MONTH)+1;//获取月份
	      int day=c.get(Calendar.DATE);//获取日
	     String beginDate=year+":"+month+":"+day+" 00:00:00";
	     String endDate=year+":"+month+":"+day+" 23:59:59";*/
		
	     String beginDate=CommonParam.pBeginData;
	    String endDate=CommonParam.pEndData;
	    if(groupid==CommonParam.pGroupId2){
	    	beginDate=CommonParam.pBeginData2;
	    	endDate=CommonParam.pEndData2;
	    }
	    if(groupid==CommonParam.pGroupId3){
	    	beginDate=CommonParam.pBeginData3;
	    	endDate=CommonParam.pEndData3;
	    }
	    if(groupid==CommonParam.pGroupId6){
	    	beginDate=CommonParam.pBeginData6;
	    	endDate=CommonParam.pEndData6;
	    }
		String sql = "select count(DISTINCT(poriginal)) cnt,gmuserid,unickname,upic from pictures,groupmembers,users where puserid =gmuserid "
					+ "and gmgroupid ="+groupid+" and puserid=userid and pstatus=0 and ustate=0 and puploadtime between '"+beginDate+"' and '"+endDate+"' "
					+ "group by gmuserid order by cnt desc limit " + limit;
		if (uid != 0) {
			sql = "select count(DISTINCT(poriginal)) cnt,gmuserid,unickname,upic from pictures,groupmembers,users where puserid =gmuserid "
					+ "and gmgroupid ="+groupid+" and puserid="+uid+" and puserid=userid and pstatus=0 and ustate=0  and puploadtime  between '"+beginDate+"' and '"+endDate+"' "
					+ "group by gmuserid order by cnt desc";
			if (!isFirst) {
				sql = "select count(DISTINCT(poriginal)) cnt,gmuserid,unickname,upic from pictures,groupmembers,users where puserid =gmuserid "
						+ "and gmgroupid ="+groupid+" and puserid=userid and pstatus=0 and puploadtime  between '"+beginDate+"' and '"+endDate+"' "
						+ "group by gmuserid order by cnt desc limit " + limit;
			}
		}
		return Db.find(sql);
	}
//	public Record CombineEventAndPictureNew(Record eventRecord) {
//		int eid = Integer.parseInt(eventRecord.get("eid").toString());
//		List<Record> picList = Db
//				.find("select pid,poriginal,ptime,pcover from pictures where peid=" + eid + " and pstatus=0 ");
//		
//		List<Record> commentList = Db
//				.find("select count(*) cnt from comments where ceid=" + eid + " and cstatus=0 ");
//		eventRecord.set("commentnum", 0);
//		if(null!=commentList&&!commentList.isEmpty()){
//			eventRecord.set("commentnum", commentList.get(0).get("cnt"));
//		}
//		eventRecord.set("picnum", picList.size());
//		if(picList.size()!=0) {
//			if(picList.size()>9) {
//				for(int j=9;j<picList.size();j++) {
//					picList.remove(j);
//					j--;
//				}
//			}
//			eventRecord.set("picList", picList);
//		}
//		return eventRecord;
//	}
}
