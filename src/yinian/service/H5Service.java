package yinian.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import yinian.app.YinianDataProcess;
import yinian.model.ActivityHintMsg;
import yinian.model.Event;
import yinian.model.Picture;
import yinian.utils.QiniuOperate;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

public class H5Service {

	private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类

	/**
	 * 获取照片墙数据
	 * 
	 * @param type
	 * @param groupid
	 * @param id
	 * @return
	 */
	public List<Record> getPhotoWall(String type, String groupid, int id) {
		String sql = "";
		switch (type) {
		case "initialize":
			sql = "select eid,euserid,pid,poriginal as url from events,pictures where peid=eid and egroupid="
					+ groupid
					+ " and estatus in(0,3) and eMain!=4 and pstatus=0 ORDER BY pid DESC limit 30";
			break;
		case "loading":
			sql = "select eid,euserid,pid,poriginal as url from events,pictures where peid=eid and egroupid="
					+ groupid
					+ " and pid<"
					+ id
					+ " and estatus in(0,3) and eMain!=4 and pstatus=0 ORDER BY pid DESC limit 30";
			break;
		case "refresh":
			sql = "select eid,euserid,pid,poriginal as url from events,pictures where peid=eid and egroupid="
					+ groupid
					+ " and pid>"
					+ id
					+ " and estatus in(0,3) and eMain!=4 and pstatus=0 ORDER BY pid DESC";
			break;
		}
		List<Record> list = Db.find(sql);
		// 处理图片，获取图片信息
		list =dataProcess.GetOriginAndThumbnailAccessWithImageInfo(list, "url");
		return list;
	}
	/**
	 * 获取活动提示
	 */
	public List<Record> GetActivityMsg(String userid){
		List<Record> returnlist=new ArrayList<>();
			List<Record> list=
					Db.find("select * from activityHint where isShow=0 order by id desc limit 0,1");
			List<Record> msgList=
					Db.find("select * from activityHintMsg where userid='"+userid+"' limit 0,1");
			if(!msgList.isEmpty()&&!list.isEmpty()){				
					Record a=list.get(0);
					Record m=msgList.get(0);
					if(a.getInt("id")==m.getInt("activityId")){					
						returnlist.add(activityMsg(false,new Record()));
					}else{						
						returnlist.add(activityMsg(true,a));
					}
			}else if(msgList.isEmpty()&&!list.isEmpty()){
					Record a=list.get(0);
					returnlist.add(activityMsg(true,a));
			}else if(msgList.isEmpty()&&list.isEmpty()){
				returnlist.add(activityMsg(false,new Record()));
			}else if(!msgList.isEmpty()&&list.isEmpty()){
				returnlist.add(activityMsg(false,new Record()));
			}
			return returnlist;
		}
	public Record activityMsg(boolean canGet,Record r){
		if(canGet){
			if(r.getInt("type")==1){//更新
				r.set("msg", r.get("msg").toString().split(";"));
			}		
			r.remove("isShow");
			r.remove("createTime");
		}else{
			r.set("type", 0);
		}
		return r;
	}
	/**
	 * 保存用户已显示的活动提示
	 */
	public void SetActivityMsg(String userid,String activityId){
		ActivityHintMsg msg=new ActivityHintMsg();
		List<Record> msgList=
				Db.find("select * from activityHintMsg where userid='"+userid+"' limit 0,1");
		if(!msgList.isEmpty()){
			if(msgList.get(0).getInt("activityId")!=Integer.parseInt(activityId)){
				msg.set("activityId", activityId);
				msg.set("userid", userid);
				msg.set("id", msgList.get(0).getInt("id"));
				msg.set("updateTime", new Date());
				msg.update();
			}
		}else{			
			msg.set("activityId", activityId);
			msg.set("userid", userid);
			msg.save();
		}
	}
	/*
	 * 判断用户是否自主创建相册
	 */
	public List<Record> getUserCreateGroupCnt(String userid){
		List<Record> list=Db.find("select count(*) cnt from groups where gtype=4 and gcreator="+userid);
		if(null!=list&&!list.isEmpty()){
			list.get(0).set("showCreate", null!=list.get(0).get("cnt").toString()&&Integer.parseInt(list.get(0).get("cnt").toString())>0?1:0);
			list.get(0).remove("cnt");
		}
		return list;
	}
	/*
	 * 删除照片 1:若用户id为相册创建者，且相册为活动相册（type=11），则可以删除全部照片
	 * 2：若相册不是活动相册，则用户只能删除自己上传的照片
	 */
	public List<Record> deletePic(String userid,String pids){
		List<Record> list=new ArrayList<Record>();
		Record record=new Record();
		int canDelete=0;
		String[] pidArray=pids.split(",");
		//判断相册开始
		if(pidArray.length>0){
			String sql="select * from groups,events,pictures where peid=eid and egroupid=groupid and pid="+pidArray[0];
			List<Record> groupList = Db.find(sql);
			if(!groupList.isEmpty()&&groupList.size()==1){
				Record g=groupList.get(0);
				if(null!=g.get("gtype")&&g.get("gtype").toString().equals("11")&&null!=g.get("gcreator")&&g.get("gcreator").toString().equals(userid)){
					canDelete=adminDeletePic(pidArray);
				}else{
					canDelete=deletePic(pidArray,userid);
				}			
			}
		}

		record.set("deleteCnt", canDelete);
		list.add(record);
		return list;
	}
		public int deletePic(String[] pidArray,String userid){
			int canDelete=0;
			for(int i=0;i<pidArray.length;i++){
				String pid=pidArray[i];
				String picsql="select * from pictures,events where peid=eid and pid="+pid;
				System.out.println("picsql:"+picsql);
				List<Record> photoList = Db.find(picsql);
				if(!photoList.isEmpty()&&photoList.size()==1){
					Record r=photoList.get(0);
					String eventPicCntSql="select count(*) cnt from pictures where pstatus=0 and peid="+r.get("peid");
					System.out.println("eventPicCntSql="+eventPicCntSql);
					System.out.println("euserid="+r.getLong("euserid").toString()+"   userid="+userid);
					if(r.getLong("euserid").toString().equals(userid)){
						System.out.println("eventPicCntSql="+eventPicCntSql);
						List<Record> cntList = Db.find(eventPicCntSql);
						if(!cntList.isEmpty()&&cntList.size()>0){
							System.out.println("cntList="+cntList.get(0).getLong("cnt").intValue());
							if(cntList.get(0).getLong("cnt").intValue()==1){
								System.out.println("eid="+r.get("peid"));
								Event e=new Event();
								e.set("eid", r.get("peid"));
								e.set("estatus", 1);
								e.update();
							}
						}
						Picture p=new Picture();
						p.set("pid", r.get("pid"));
						p.set("pstatus", 1);
						p.update();
						canDelete+=1;
					}
				}
			}
			return canDelete;
		}
	public int adminDeletePic(String[] pidArray){
		int canDelete=0;
		for(int i=0;i<pidArray.length;i++){
			String pid=pidArray[i];
			String picsql="select * from pictures,events where peid=eid and pid="+pid;
			//System.out.println("picsql:"+picsql);
			List<Record> photoList = Db.find(picsql);
			if(!photoList.isEmpty()&&photoList.size()==1){
				Record r=photoList.get(0);
				String eventPicCntSql="select count(*) cnt from pictures where pstatus=0 and peid="+r.get("peid");
				System.out.println("eventPicCntSql="+eventPicCntSql);							
				List<Record> cntList = Db.find(eventPicCntSql);
				if(!cntList.isEmpty()&&cntList.size()>0){
					System.out.println("cntList="+cntList.get(0).getLong("cnt").intValue());
					if(cntList.get(0).getLong("cnt").intValue()==1){
						System.out.println("eid="+r.get("peid"));
						Event e=new Event();
						e.set("eid", r.get("peid"));
						e.set("estatus", 1);
						e.update();
					}
				}
				Picture p=new Picture();
				p.set("pid", r.get("pid"));
				p.set("pstatus", 1);
				p.update();
				canDelete+=1;
				}
			}
		return canDelete;
	}
}