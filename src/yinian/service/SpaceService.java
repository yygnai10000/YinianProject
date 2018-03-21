package yinian.service;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jfinal.aop.Before;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.jfinal.plugin.ehcache.CacheKit;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.WorkbookSettings;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import redis.clients.jedis.Jedis;
import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.model.Event;
import yinian.model.Group;
import yinian.model.Like;
import yinian.model.Picture;
import yinian.model.User;
import yinian.utils.EmojiUtils;
import yinian.utils.JsonData;
import yinian.utils.QiniuOperate;
import yinian.utils.RedisUtils;

public class SpaceService {

	private String jsonString;// 返回结果
	private JsonData jsonData = new JsonData(); // json操作类
	private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类

	/**
	 * 获得组内信息
	 * 
	 * @param groupid
	 * @return
	 */
	public String getGroupContent(String userid, String groupid, String minID) {

		// 评论查询语句
		String sqlForComment = CommonParam.selectForComment
				+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and (egroupid="
				+ groupid + " or eRecommendGroupID = " + groupid + " )and cstatus=0 ORDER BY ceid,ctime asc";

		List<Record> event;

		if (minID.equals("0")) {
			// minID==0代表获取组内信息的初始化信息
			// 获取置顶的所有动态
			List<Record> topEvents = Db.find(CommonParam.selectForEvent
					+ "FROM users,`events`,pictures WHERE userid = euserid AND eid = peid AND ((egroupid = " + groupid
					+ " AND elevel = 1) OR (eRecommendGroupID = " + groupid
					+ " AND isTopInRecommendGroup = 1)) AND estatus = 0 AND pstatus = 0 GROUP BY peid desc order by eTopTime asc");
			int size = topEvents.size();

			// 获取除置顶外其他的动态
			event = Db.find(CommonParam.selectForEvent
					+ " FROM users,`events`,pictures WHERE userid=euserid and eid=peid and ((egroupid = " + groupid
					+ " AND elevel = 0) OR (eRecommendGroupID = " + groupid
					+ " AND isTopInRecommendGroup = 0)) AND estatus = 0 AND pstatus = 0 GROUP BY eid desc limit "
					+ (10 - size) + "");
			topEvents.addAll(event);
			event = topEvents;

		} else {
			// minID不等于0则查询小于该ID的前十条数据
			event = Db.find(CommonParam.selectForEvent
					+ " from users,events,pictures where userid=euserid and eid=peid and ((egroupid = " + groupid
					+ " AND elevel = 0) OR (eRecommendGroupID = " + groupid
					+ " AND isTopInRecommendGroup = 0)) and eid < " + minID
					+ " and estatus=0 and pstatus=0 group by eid DESC limit 10");
		}

		// 获取来源空间的名称
		for (Record record : event) {
			String recommendGroupName = record.get("eRecommendGroupID") == null ? null
					: new Group().findById(record.get("egroupid").toString()).get("gname").toString();
			record.set("recommendGroupName", recommendGroupName);
		}

		event = dataProcess.encapsulationEventList(event);// 获取事件的相关信息并封装里面的用户对象
		List<Record> comment = dataProcess.encapsulationCommentList(Db.find(sqlForComment));// 获取事件的评论信息并封装里面的用户对象
		List<Record> list = dataProcess.combieEventAndComment(event, comment);// 拼接事件与评论
		// 封装事件和点赞
		list = dataProcess.combineEventWithLike(list, "smallApp");
		// 修改upic字段格式
		list = dataProcess.ChangePicAsArrayDirectCutVersion(list);

		jsonString = jsonData.getSuccessJson(list);
		return jsonString;
	}
	
	/**
	 * 获取点赞排行  新老用户数量对比
	 * 
	 * @param groupid
	 * @param uid
	 * @param searchLimit
	 * @return
	 */
	public List<Record> GetOldOrNewUserListByElikeAndGroup(int groupid, int uid, int searchLimit,String begin,String end,String likeTime) {
//		if(null==begin||begin.equals("")){
//			begin="1000000";
//		}
//		if(null==end||end.equals("")){
//			end="1623423";
//		}
		List<Record> eventList = new Event().GetListByGroup(groupid, searchLimit);// 获取动态		
		for (Record r : eventList) {
//			System.out.println("select count(*) cnt from `like` where "
//					+ " likeEventID="+r.getLong("eid").intValue()+" and likeStatus=0 and likeUserID <="+begin+" and likeTime<'"+likeTime+"'");
			List<Record> moreOldList=Db.find("select count(*) cnt from `like` where "
					+ " likeEventID="+r.getLong("eid").intValue()+" and likeStatus=0 and likeUserID <="+begin+" and likeTime<'"+likeTime+"'");
			List<Record> allList=Db.find("select count(*) cnt from `like` where "
					+ " likeEventID="+r.getLong("eid").intValue()+" and likeStatus=0 and  likeTime<'"+likeTime+"'");
			r.set("总点赞数", allList.get(0).get("cnt"));
			if(null!=end&&!end.equals("")){
				List<Record> oldList=Db.find("select count(*) cnt from `like` where "
						+ " likeEventID="+r.getLong("eid").intValue()+" and likeStatus=0 and likeUserID > "+begin+" and likeUserID <="+end+" and likeTime<'"+likeTime+"'");
				List<Record> newList=Db.find("select count(*) cnt from `like` where "
						+ " likeEventID="+r.getLong("eid").intValue()+" and likeStatus=0 and likeUserID >"+end+" and likeTime<'"+likeTime+"'");
				r.set("中间用户点赞数",oldList.get(0).get("cnt"));
				r.set("新用户点赞数", newList.get(0).get("cnt"));
			}else{
				List<Record> newList=Db.find("select count(*) cnt from `like` where "
						+ " likeEventID="+r.getLong("eid").intValue()+" and likeStatus=0 and likeUserID >"+begin+" and likeTime<'"+likeTime+"'");
				r.set("新用户点赞数", newList.get(0).get("cnt"));
			}
			r.set("老用户点赞数",moreOldList.get(0).get("cnt"));
			
		}
		return eventList;
		// returnList.set("", value)

	}

	/**
	 * 获取点赞排行
	 * 
	 * @param groupid
	 * @param uid
	 * @param searchLimit
	 * @return
	 */
	public List<Record> GetListByElikeAndGroup(int groupid, int uid, int searchLimit) {
		List<Record> eventList = new Event().GetListByGroup(groupid, searchLimit);// 获取动态
		YinianDataProcess ydp=new YinianDataProcess();
		String ids = "";
		for (Record r : eventList) {
			// r.set("pictures", null);
			ids += r.getLong("eid") + ",";
		}
		if (ids.length() > 0) {
			ids = ids.substring(0, ids.length() - 1);
		}
		// 获取每个动态的图片
		List<Record> allPictureList = new Picture().GetByEids(ids);
		Map<Integer, List<Record>> map = new HashMap<>();
		for (Record r : allPictureList) {
			int eid = r.getLong("peid").intValue();
			QiniuOperate q = new QiniuOperate();
			// String url = q.getDownloadToken(r.getStr("poriginal") +
			// "?imageView2/2/w/200");
			// r.set("thumbImg", url);
			if(r.get("poriginal").toString().indexOf(".mp4")!=-1||r.get("poriginal").toString().indexOf(".MP4")!=-1
					||r.get("poriginal").toString().indexOf(".3gp")!=-1||r.get("poriginal").toString().indexOf(".3GP")!=-1
					||r.get("poriginal").toString().indexOf(".avi")!=-1||r.get("poriginal").toString().indexOf(".AVI")!=-1
					||r.get("poriginal").toString().indexOf(".flv")!=-1||r.get("poriginal").toString().indexOf(".FLV")!=-1
					||r.get("poriginal").toString().indexOf(".MKV")!=-1||r.get("poriginal").toString().indexOf(".mkv")!=-1){
				String pcover=ydp.getVideoCover(r.get("poriginal").toString(),"4");
				r.set("thumbnail", pcover);
				// 中等缩略图授权
				r.set("midThumbnail", pcover);
				// 原图授权
				r.set("poriginal", pcover);
			}else{
				// 缩略图授权
				r.set("thumbnail", q.getDownloadToken(r.get("poriginal").toString() + "?imageView2/2/w/250"));
				// 中等缩略图授权
				r.set("midThumbnail", q.getDownloadToken(r.get("poriginal").toString() + "?imageView2/2/w/500"));
				// 原图授权
				r.set("poriginal", q.getDownloadToken(r.get("poriginal").toString()));
			}
			if (null != map.get(eid)) {
				map.get(eid).add(r);
			} else {
				List<Record> rlist = new ArrayList<Record>();
				rlist.add(r);
				map.put(eid, rlist);
			}
		}
		// 获取每个动态自己的点赞情况
		List<Record> userLikeList = new Like().GetCountListByEventidAndUid(ids, uid);
		Map<Integer, List<Record>> likemap = new HashMap<>();
		for (Record r : userLikeList) {
			int eid = r.getLong("likeEventID").intValue();
			if (null != likemap.get(eid)) {
				likemap.get(eid).add(r);
			} else {
				List<Record> rlist = new ArrayList<Record>();
				rlist.add(r);
				likemap.put(eid, rlist);
			}
		}
		for (Record r : eventList) {
			if(r.get("eMain").toString().equals("4")){
				r.set("eMain", 0);
			}
			r.set("pictures", map.get(r.getLong("eid").intValue()));
			r.set("like", likemap.get(r.getLong("eid").intValue()));
		}
		return eventList;
		// returnList.set("", value)

	}

	/**
	 * 获取照片达人排行
	 * 
	 * @param uid
	 * @param groupid
	 * @param searchLimit
	 * @return
	 */
	public List<Record> GetPublishList(int uid, int groupid, int searchLimit) {
		List<Record> iList = new Event().GetUsePublishPhotoCont(groupid, uid, true, searchLimit);
		// Record r=new Record();
		if (null == iList || iList.isEmpty()) {
			iList = new User().getUserNameAndPic(uid);
			if (null != iList && !iList.isEmpty()) {
				// r=iList.get(0);
				iList.get(0).set("eid", null);
				iList.get(0).set("egroupid", groupid);
				iList.get(0).set("euserid", uid);
				iList.get(0).set("cnt", 0);
				iList.get(0).set("rowNo", "未上榜");
			}
		}
		List<Record> allList = new Event().GetUsePublishPhotoCont(groupid, uid, false, searchLimit);

		for (int i = 0; i < allList.size(); i++) {
			if ((uid + "").equals(allList.get(i).get("euserid").toString())) {
				if (null != iList && !iList.isEmpty()) {
					iList.get(0).set("rowNo", i + 1 + " 名");
				}
			}
		}
		iList.addAll(allList);
		return iList;
	}
	/**
	 * 获取照片达人排行--固定相册内所有用户在任意相册发布的照片总数排行
	 * 
	 * @param uid
	 * @param groupid
	 * @param searchLimit
	 * @return
	 */
	//redis 方案
	public List<Record> GetGroupAllUserPublishList_3(int uid, int groupid, int searchLimit) {
	/*	//List<Record> iList = new Event().GetUsePublishPhotoCont(groupid, uid, true, searchLimit);
		List<Record> iList = new Event().GetUseAllPublishPhotoCont(groupid, uid, true, searchLimit);
		// Record r=new Record();
		if (null == iList || iList.isEmpty()) {
			iList = new User().getUserNameAndPic(uid);
			if (null != iList && !iList.isEmpty()) {
				// r=iList.get(0);
				iList.get(0).set("eid", null);
				iList.get(0).set("egroupid", groupid);
				iList.get(0).set("euserid", uid);
				iList.get(0).set("cnt", 0);
				iList.get(0).set("rowNo", "未上榜");
			}
		}*/
		List<Record> iList = new User().getUserNameAndPic(uid);
		if (null != iList && !iList.isEmpty()) {
			// r=iList.get(0);
			iList.get(0).set("eid", null);
			iList.get(0).set("egroupid", groupid);
			iList.get(0).set("euserid", uid);
			iList.get(0).set("cnt", "---");
			iList.get(0).set("rowNo", "---");
		}	
		//redis 开始
		
		List<Record> allList=new ArrayList<Record>();
		Jedis jedis = RedisUtils.getRedis();
		if(null!=jedis) {
			//从缓存中读取当前eid的点赞数
			String pList = jedis.get("PublishList_"+groupid);
			if(null!=pList&&!"".equals(pList)) {
				JSONArray ja=JSONArray.fromObject(jedis.get("PublishList_"+groupid));
				
				for(int i=0;i<ja.size();i++){
					JSONObject jo=ja.getJSONObject(i);	
					if(null!=jo&&!jo.equals("")){
						//String values=EmojiUtils.emojiRecovery(jo.toString());
						JSONObject jvalue=JSONObject.fromObject(jo.toString());
						Record r=new Record();
						r.set("upic", jvalue.get("upic"));
						r.set("gmuserid", jvalue.get("gmuserid"));
						r.set("cnt", jvalue.get("cnt"));
						r.set("unickname", EmojiUtils.emojiRecovery(jvalue.get("unickname").toString()));
						allList.add(r);
					}
					
				}
			}else {
				//当前eid未缓存点赞数，从数据库count点赞数，并同步到缓存
					allList  =new Event().GetUseAllPublishPhotoCont(groupid, uid, false, searchLimit);
					JSONArray json = new JSONArray();
		            for(Record r : allList){
		                JSONObject jo = new JSONObject();
		                jo.put("upic", r.get("upic"));
		                jo.put("gmuserid", r.get("gmuserid"));
		                jo.put("cnt", r.get("cnt"));
		                jo.put("unickname", EmojiUtils.emojiFilter(r.get("unickname")));
		                json.add(jo);
		            }
					jedis.set("PublishList_"+groupid,json.toString());
					jedis.expire("PublishList_"+groupid, 60*30); 
				}
				RedisUtils.returnResource(jedis);
			}else{
				allList = CacheKit.get("DataSystem", groupid + "GetAllPublishList");
				if (allList == null) {
					allList =new Event().GetUseAllPublishPhotoCont(groupid, uid, false, searchLimit);					
					CacheKit.put("DataSystem", groupid + "GetAllPublishList", allList);
				}
			}
			//释放redis
				

//		List<Record> allList = CacheKit.get("DataSystem", groupid + "GetAllPublishList");
//		if (allList == null) {
//			//photo=Db.find("select * from 5268248");
//			allList =GetPublishListByExcell(groupid+"");
//			CacheKit.put("DataSystem", groupid + "GetAllPublishList", allList);
//		}
		//List<Record> allList = GetPublishListByExcell(groupid+"");
		for (int i = 0; i < allList.size(); i++) {
			//System.out.println(allList.get(i).get("gmuserid").toString());
			allList.get(i).set("eid", null);
			allList.get(i).set("egroupid", groupid);
			if ((uid + "").equals(allList.get(i).get("gmuserid").toString())) {
				if (null != iList && !iList.isEmpty()) {
					iList.get(0).set("rowNo", i + 1 + " 名");
					iList.get(0).set("eid", null);
				}
			}
		}
		iList.addAll(allList);
		return iList;
	}

	//excel方案
	public List<Record> GetGroupAllUserPublishList_2(int uid, int groupid, int searchLimit) {
		
		List<Record> iList = new User().getUserNameAndPic(uid);
			if (null != iList && !iList.isEmpty()) {
				// r=iList.get(0);
				iList.get(0).set("eid", null);
				iList.get(0).set("egroupid", groupid);
				iList.get(0).set("euserid", uid);
				iList.get(0).set("cnt", "---");
				iList.get(0).set("rowNo", "---");
			}	
		//redis 开始
		/*
		List<Record> allList=new ArrayList<Record>();
		Jedis jedis = RedisUtils.getRedis();
		if(null!=jedis) {
			//从缓存中读取当前eid的点赞数
			String pList = jedis.get("PublishList_"+groupid);
			if(null!=pList&&!"".equals(pList)) {
				//点赞成功后缓存点赞数加1			
					System.out.println("redis:"+jedis.get("PublishList_"+groupid));
			}else {
				//当前eid未缓存点赞数，从数据库count点赞数，并同步到缓存
					allList  =new Event().GetUseAllPublishPhotoCont(groupid, uid, false, searchLimit);
					jedis.set("PublishList_"+groupid,JSONArray.fromObject(allList).toString());
					//System.out.println("no redis:"+jedis.get("likeCnt_12233"));
				}
			}
			//释放redis
			RedisUtils.returnResource(jedis);	
			*/
		List<Record> allList = CacheKit.get("DataSystem", groupid + "GetAllPublishList");
		if (allList == null) {
			//photo=Db.find("select * from 5268248");
			allList =GetPublishListByExcell(groupid+"");
			CacheKit.put("DataSystem", groupid + "GetAllPublishList", allList);
		}
		//List<Record> allList = GetPublishListByExcell(groupid+"");
		for (int i = 0; i < allList.size(); i++) {
			//System.out.println(allList.get(i).get("gmuserid").toString());
			allList.get(i).set("eid", null);
			allList.get(i).set("egroupid", groupid);
			if ((uid + "").equals(allList.get(i).get("gmuserid").toString())) {
				if (null != iList && !iList.isEmpty()) {
					iList.get(0).set("rowNo", i + 1 + " 名");
					iList.get(0).set("eid", null);
				}
			}
		}
		iList.addAll(allList);
		return iList;
	}
	public List<Record> GetGroupAllUserPublishList(int uid, int groupid, int searchLimit) {
		//List<Record> iList = new Event().GetUsePublishPhotoCont(groupid, uid, true, searchLimit);
		List<Record> iList = new Event().GetUseAllPublishPhotoCont(groupid, uid, true, searchLimit);
		// Record r=new Record();
		if (null == iList || iList.isEmpty()) {
			iList = new User().getUserNameAndPic(uid);
			if (null != iList && !iList.isEmpty()) {
				// r=iList.get(0);
				iList.get(0).set("eid", null);
				iList.get(0).set("egroupid", groupid);
				iList.get(0).set("euserid", uid);
				iList.get(0).set("cnt", 0);
				iList.get(0).set("rowNo", "未上榜");
			}
		}
		//List<Record> allList = new Event().GetUsePublishPhotoCont(groupid, uid, false, searchLimit);
		List<Record> allList = CacheKit.get("DataSystem", groupid + "GetAllPublishList");
		if (allList == null) {
			//photo=Db.find("select * from 5268248");
			allList =new Event().GetUseAllPublishPhotoCont(groupid, uid, false, searchLimit);
			CacheKit.put("DataSystem", groupid + "GetAllPublishList", allList);
		}
		//List<Record> allList = new Event().GetUseAllPublishPhotoCont(groupid, uid, false, searchLimit);
		for (int i = 0; i < allList.size(); i++) {
			allList.get(i).set("eid", null);
			allList.get(i).set("egroupid", groupid);
			if ((uid + "").equals(allList.get(i).get("gmuserid").toString())) {
				if (null != iList && !iList.isEmpty()) {
					iList.get(0).set("rowNo", i + 1 + " 名");
				}
			}
		}
		iList.addAll(allList);
		return iList;
	}
	/**
	 * 获取组员列表 by lk
	 * 
	 * @param groupid
	 * @return
	 */
	public String getMemberList(String userid, String groupid, int lastUid) {

		String sql = "select userid,unickname,upic,gmtime,gname from users,groups,groupmembers where groupid=gmgroupid and users.userid=groupmembers.gmuserid and gmgroupid='"
				+ groupid + "' and userid > " + lastUid + " order by userid asc limit 20 ";
		List<Record> list = Db.find(sql);

		// 区分新旧版本，增加备注名字段
		if (userid != null && !(userid.equals(""))) {
			String sqlForNote = "select noteName,noteTo from note where noteGroupID=" + groupid + " and noteFrom="
					+ userid + " ";
			List<Record> noteList = Db.find(sqlForNote);
			for (Record record : list) {
				record.set("noteName", null);
				record.set("selected", false);
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
	 * 获取组员列表 by ly  管理员置顶
	 * 
	 * @param groupid
	 * @return
	 */
	@Before(Tx.class)
	public String getMemberTopList(String userid, String groupid, int pagenum) {
		
		int page = (pagenum-1)*20;
		String sql = "select userid,unickname,upic,gnum,gname,isAdmin from users,groups,groupmembers where groupid=gmgroupid and users.userid=groupmembers.gmuserid and gmgroupid="
				+ groupid + " ORDER BY isAdmin DESC,gmtime limit " +page+",20";
		List<Record> list = Db.find(sql);

		// 区分新旧版本，增加备注名字段
		if (userid != null && !(userid.equals(""))) {
			String sqlForNote = "select noteName,noteTo from note where noteGroupID=" + groupid + " and noteFrom="
					+ userid + " ";
			List<Record> noteList = Db.find(sqlForNote);
			for (Record record : list) {
				record.set("noteName", null);
				record.set("selected", false);
				//record.set("IsAdmin", record.get("IsAdmin"));
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
	  * 获取照片达人排行从excell中获取
	  * 
	  */
	 public List<Record> GetPublishListByExcell(String groupid) {
		 //String url="http://picture.zhuiyinanian.com/yinian/"+groupid+".xls";
		 	Sheet sheet;
	        Workbook book;
	        Cell cell1,cell2,cell3,cell4;
	        List list = new ArrayList<>();
	       
	        WorkbookSettings workbookSettings = new WorkbookSettings();
	        workbookSettings.setEncoding("GBK");
	        List<Record> ilist = new ArrayList<>();
	        try { 
	            //t.xls为要读取的excel文件名
	           // book= Workbook.getWorkbook(new File("D:\\leiyu\\activiti.xls")); 
	            String strUrl = "http://picture.zhuiyinanian.com/yinian/"+groupid+".xls";
	           // String strUrl = "http://localhost/~liukai/activiti.xls";
	   URL url = new URL(strUrl); 
	   HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	   InputStream input = conn.getInputStream();
	  
	   book = Workbook.getWorkbook(input,workbookSettings);
	            
	            //获得第一个工作表对象(ecxel中sheet的编号从0开始,0,1,2,3,....)
	            sheet=book.getSheet(0); 
	          
	            for(int j=0;j<=99;j++) {
	             Record record = new Record();
	              //获取每一行的单元格 
	                cell1=sheet.getCell(0,j);//（列，行）
	                cell2=sheet.getCell(1,j);
	                cell3=sheet.getCell(2,j);
	                cell4=sheet.getCell(3, j);
	                record.set("cnt", cell1.getContents());
	                record.set("gmuserid", cell2.getContents());
	                record.set("unickname", cell3.getContents());
	                record.set("upic", cell4.getContents());
	                record.set("egroupid", groupid);
	                record.set("eid", null);
	                ilist.add(record);
	            }
	            book.close(); 
	        }
	        catch(Exception e)  {
	         e.printStackTrace();
	        }
	  return ilist;
	 }
}
