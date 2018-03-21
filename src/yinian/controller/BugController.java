package yinian.controller;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import yinian.interceptor.CrossDomain;
import yinian.model.Event;
import yinian.model.Like;
import yinian.model.User;
import yinian.model.UserBlacklist;
import yinian.service.SpaceService;
import yinian.utils.JsonData;
import yinian.utils.QiniuOperate;

import java.util.ArrayList;
import java.util.List;

public class BugController extends Controller {
	private String jsonString; // 返回的json字符串
	private JsonData jsonData = new JsonData(); // json操作类
	/**
	 * 刷赞工具，eid=动态id,cnt=需要刷票数-1，如需要刷40票，则cnt传39
	 */
	public void like(){
		 String eid =this.getPara("eid");
		 String likeCnt=this.getPara("cnt");
		 List<Record> list = Db
		 .find("select userid,uphone from users where uphone like '9999999%' order by uphone desc");
		 int i = 0;
		 for (Record record : list) {
		 Like like = new Like().set("likeEventID", eid)
		 .set("likeUserID", record.get("userid").toString())
		 .set("likeStatus", 0);
		 try {
		 like.save();
		 Thread.currentThread().sleep(1000);
		 i++;
		 System.out.println(i);
		 } catch (Exception e) {
		 System.out.println("已点赞");
		 }
		 if (i > Integer.parseInt(likeCnt))
		 break;
		 }
		 List<Record> rlist=new ArrayList<>();
		 Record r=new Record().set("状态", "刷票成功共"+i+" 票");
		 rlist.add(r);
		 jsonString = jsonData.getSuccessJson(rlist);
		 renderText(jsonString);
	}
	/**
	 * 点赞排行
	 */
	@Before(CrossDomain.class)
	public void GetGroupLikeList() {
		String beginUserId=this.getPara("beginUserId");		
		String endUserId=this.getPara("endUserId");
		String endTime=this.getPara("endTime");
		if(this.getPara("groupid")==null||this.getPara("groupid").equals("")||this.getPara("uid")==null||this.getPara("uid").equals("")){
			jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		String gid=this.getPara("groupid");
		String userid=this.getPara("uid");
		if(userid.equals("")||gid.equals("")||gid==null||gid.equals("undefined")||gid.equals("NaN")){
			jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		int groupid = Integer.parseInt(this.getPara("groupid"));
		int uid = Integer.parseInt(this.getPara("uid"));
		int searchLimit = Integer
				.parseInt(this.getPara("searchLimit") != null && !this.getPara("searchLimit").equals("")
						? this.getPara("searchLimit")
						: "50");
		//List<Record> list = CacheKit.get("ConcurrencyCache", groupid + "GroupLikeList");
		//if (list == null) {
		List<Record> list = new SpaceService().GetOldOrNewUserListByElikeAndGroup(groupid, uid, searchLimit,beginUserId,endUserId,endTime);
			//CacheKit.put("ConcurrencyCache", groupid + "GroupLikeList", list);
	//	}
		//List<Record> list = new SpaceService().GetListByElikeAndGroup(groupid, uid, searchLimit);
		// List<Record> list=new Event().GetListByElikeAndGroup(groupid,eid);
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}
	/**
	 * 加黑名单
	 */
	@Before(CrossDomain.class)
	public void addBlackList(){
		jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
		String userid=this.getPara("userid");
		String mark=this.getPara("mark");
		String adminid=this.getPara("adminid");
		if(null!=userid&&!userid.equals("")){
			User u=new User().findById(userid);
			if(null!=u){
				u.set("ustate", 1);
				u.update();
				UserBlacklist ubl=new UserBlacklist();
				ubl.set("userid", userid);
				ubl.set("adminid", adminid);
				ubl.set("mark", mark);
				ubl.set("status", 0);
				ubl.save();
				//Db.update("update events set estatus=1 where euserid="+userid);
				jsonString = jsonData.getSuccessJson();
			}
		}
		renderText(jsonString);
	}
	/*
	 * 根据用户名和相册id查找用户
	 */
	@Before(CrossDomain.class)
	public void getGroupUserByName(){
		jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
		String name=this.getPara("name");
		String groupid=this.getPara("groupid");
		if(null!=groupid&&!groupid.equals("")&&null!=name&&!name.equals("")){
			String sql="SELECT unickname,users.userid,upic,ustate,adminName,mark  FROM groupmembers, users "
					+ " left join userblacklist on userblacklist.userid=users.userid and userblacklist.status=0 "
					+ " left join pcadmin on userblacklist.adminid=pcadmin.id "
					+ " WHERE gmgroupid = "+groupid+" AND gmuserid = userid AND unickname LIKE '%"+name+"%'";
			List<Record> list=Db.find(sql);
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}
	/*
	 * 根据用户id查询用户在相册发布的最新10条动态首图
	 */
	@Before(CrossDomain.class)
	public void getEventByUserid(){
		jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
		QiniuOperate operate = new QiniuOperate();
		String userid=this.getPara("userid");
		if(null!=userid&&!userid.equals("")){
			String sql="select etext,efirstpic from events where euserid="+userid+" and estatus=0 order by eid desc limit 10";
			List<Record> list=Db.find(sql);
			for(Record r:list){
				//String pic=r.get("efirstpic").toString();
				if(null!=r.get("efirstpic")&&!r.get("efirstpic").toString().equals("")){
					r.set("pic", operate.getDownloadToken(r.get("efirstpic").toString()));
				}else{
					r.set("pic","");
				}
			}
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}
	/*
	 * 根据用户id，时间段查询用户在相册发布的最新条动态首图，默认显示100张
	 */
	@Before(CrossDomain.class)
	public void getEventByUseridAndTime(){
		jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
		QiniuOperate operate = new QiniuOperate();
		String userid=this.getPara("userid");
		String begin=this.getPara("begin");
		String end=this.getPara("end");
		if(null!=userid&&!userid.equals("")){
			String sql="select etext,efirstpic from events where euserid="+userid+" and estatus=0 order by eid desc limit 100";
			if(null!=begin&&null!=end&&!begin.equals("")&&!end.equals("")){
				sql="select etext,efirstpic from events where euserid="+userid+" and  BETWEEN '"+begin+"' and '"+end+"' and estatus=0 order by eid desc";
			}
			
			List<Record> list=Db.find(sql);
			for(Record r:list){
				//String pic=r.get("efirstpic").toString();
				if(null!=r.get("efirstpic")&&!r.get("efirstpic").toString().equals("")){
					r.set("pic", operate.getDownloadToken(r.get("efirstpic").toString()));
				}else{
					r.set("pic","");
				}
			}
			jsonString = jsonData.getSuccessJson(list);
		}
		renderText(jsonString);
	}
	/*
	 * 删除用户动态
	 */
	public void deleteEvent(){
		jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
		String ids=this.getPara("ids");
		String[] idArray=ids.split(",");
		for(int i=0;i<idArray.length;i++){
			Event e=new Event().findById(idArray[i]);
			e.set("estatus", 1);
			e.update();
		}
		jsonString = jsonData.getSuccessJson();
		renderText(jsonString);
	}
	/*
	 * 解除黑名单
	 */
	@Before(CrossDomain.class)
	public void delFromBlackList(){
		jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
		String userid=this.getPara("userid");
		if(null!=userid&&!userid.equals("")){
			User u=new User().findById(userid);
			if(null!=u){
				u.set("ustate", 0);
				u.update();
				Db.update("update userblacklist set status=1 where userid="+userid);
				jsonString = jsonData.getSuccessJson();
			}
		}
		renderText(jsonString);
	}
}
