package yinian.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

import yinian.model.Event;
import yinian.model.Picture;
import yinian.model.SimH5Share;
import yinian.utils.DateUtil;
import yinian.utils.QiniuOperate;

public class SimplificationH5Service {
	/*
	 * 根据相册、用户id、排序方式、查询时间获取照片列表
	 */
	public List<Record> getGroupPhotoList(String groupId,String userId,String orderBy,String searchTime,int pid){
//		String sql="SELECT eid,egroupid,userid,unickname,upic,etext,efirstpic,eVerifyPic"
//				+ ",ePeopleName,eaudio,eplace,ePlacePic,eMain,euploadtime,eView,isTopInRecommendGroup,"
//				+ "isRecommend,elevel FROM users,`events` WHERE userid = euserid AND egroupid = " + groupId
//				+ " AND estatus = 0  GROUP BY eid  "+orderBy;
		
		Record returnRecord=new Record();
		QiniuOperate operate = new QiniuOperate();
		String pidConds="";
		if(orderBy.equals("desc")&&pid!=0){
			pidConds=" and pid <"+pid;
		}else{
			pidConds=" and pid >"+pid;
		}
		String timeConds="";
		String limits=" limit 0, 21 ";
		if(!searchTime.equals("")){
			try{
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");  
				int searhEndDay=DateUtil.getDaysOfMonth(sdf.parse(searchTime+"-01"));
		        timeConds=" and puploadtime between '"+searchTime+"-01 00:00:00'"+" and '"+searchTime+"-"+searhEndDay+" 23:59:59 '";
			}catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}			
		}
//		else{
//			limits=" limit 0, 20 ";
//		}
		
		
		String sql="SELECT eid,euserid as userid,eMain FROM `events` WHERE egroupid = " + groupId
				+ " AND estatus = 0  GROUP BY eid  ";//获取空间内所有动态
		List<Record> eventList = Db.find(sql);
		StringBuffer eidBuffer=new StringBuffer();
		for(Record r:eventList){
			eidBuffer.append(r.get("eid").toString()).append(",");
		}
		if(eidBuffer.length()>0){
			String photoSql="select pid,poriginal,ptime,pcover "
					+ " from pictures where peid in (" +eidBuffer.substring(0, eidBuffer.length()-1)+ ") and "
							+ "pstatus=0 "+timeConds+pidConds+" order by puploadtime "+orderBy +" ,pid "+orderBy +limits;//获取空间内所有动态
			String cntSql="select count(*) cnt "
					+ " from pictures where peid in (" +eidBuffer.substring(0, eidBuffer.length()-1)+ ") and "
							+ "pstatus=0 "+timeConds;//获取空间内所有动态cnt
			List<Record> photoList = Db.find(photoSql);
			List<Record> cntList = Db.find(cntSql);
			returnRecord.set("picCnt", 0);
			if(!cntList.isEmpty()){
				returnRecord.set("picCnt",cntList.get(0).get("cnt"));
			}
			for(Record picRecord:photoList){
				String cover=picRecord.get("poriginal");
				if(picRecord.get("pcover")!=null&&(cover.indexOf(".mp4")!=-1||cover.indexOf(".MP4")!=-1
						||cover.indexOf(".3gp")!=-1||cover.indexOf(".3GP")!=-1
						||cover.indexOf(".avi")!=-1||cover.indexOf(".AVI")!=-1
						||cover.indexOf(".flv")!=-1||cover.indexOf(".FLV")!=-1
						||cover.indexOf(".MKV")!=-1||cover.indexOf(".mkv")!=-1)){
					picRecord.set("pcover", operate
							.getDownloadToken(cover + "?vframe/jpg/offset/1/w/750"));	
				}
				
				picRecord.set("thumbnail", operate
						.getDownloadToken(picRecord.get("poriginal").toString() + "?imageView2/2/w/250"));
				// 中等缩略图授权
				picRecord.set("midThumbnail", operate
						.getDownloadToken(picRecord.get("poriginal").toString() + "?imageView2/2/w/1000"));
				// 原图授权
				picRecord.set("poriginal", operate.getDownloadToken(picRecord.get("poriginal").toString()));
				picRecord.set("cheaked", false);
			}
			returnRecord.set("picList", photoList);
		}else{
			returnRecord.set("picCnt", 0);returnRecord.set("picList", new ArrayList<Record>());
		}
		List<Record> reruenList=new ArrayList<>();
		reruenList.add(returnRecord);
		return reruenList;
	}
	
	/*
	 * 根据相册、用户id、排序方式、查询时间获取照片列表(根据pictures查询)
	 */
	public List<Record> getGroupPhotoListNew(String groupId,String userId,String orderBy,String searchTime,int pid){
		Record returnRecord=new Record();
		QiniuOperate operate = new QiniuOperate();
		String pidConds="";
		if(orderBy.equals("desc")&&pid!=0){
			pidConds=" and pid <"+pid;
		}else{
			pidConds=" and pid >"+pid;
		}
		String timeConds="";
		String limits=" limit 0, 21 ";
		if(!searchTime.equals("")){
			try{
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");  
				int searhEndDay=DateUtil.getDaysOfMonth(sdf.parse(searchTime+"-01"));
		        timeConds=" and puploadtime between '"+searchTime+"-01 00:00:00'"+" and '"+searchTime+"-"+searhEndDay+" 23:59:59 '";
			}catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}			
		}
		
		String sql="SELECT eid,euserid as userid,eMain FROM `events` WHERE egroupid = " + groupId
				+ " AND estatus = 0  GROUP BY eid  ";//获取空间内所有动态
		List<Record> eventList = Db.find(sql);
		StringBuffer eidBuffer=new StringBuffer();
		for(Record r:eventList){
			eidBuffer.append(r.get("eid").toString()).append(",");
		}
		if(eidBuffer.length()>0){
			String photoSql="select pid,poriginal,ptime,pcover,pMain "
					+ " from pictures where pstatus=0"+timeConds+pidConds+" order by puploadtime "+orderBy +" ,pid "+orderBy +limits;//获取空间内所有动态
			String cntSql="select count(*) cnt "
					+ " from pictures where peid in (" +eidBuffer.substring(0, eidBuffer.length()-1)+ ") and "
							+ "pstatus=0 "+timeConds;//获取空间内所有动态cnt
			List<Record> photoList = Db.find(photoSql);
			List<Record> cntList = Db.find(cntSql);
			returnRecord.set("picCnt", 0);
			if(!cntList.isEmpty()){
				returnRecord.set("picCnt",cntList.get(0).get("cnt"));
			}
			for(Record picRecord:photoList){
				picRecord.set("thumbnail", operate
						.getDownloadToken(picRecord.get("poriginal").toString() + "?imageView2/2/w/250"));
				// 中等缩略图授权
				picRecord.set("midThumbnail", operate
						.getDownloadToken(picRecord.get("poriginal").toString() + "?imageView2/2/w/1000"));
				// 原图授权
				picRecord.set("poriginal", operate.getDownloadToken(picRecord.get("poriginal").toString()));
				picRecord.set("cheaked", false);
			}
			returnRecord.set("picList", photoList);
		}else{
			returnRecord.set("picCnt", 0);returnRecord.set("picList", new ArrayList<Record>());
		}
		List<Record> reruenList=new ArrayList<>();
		reruenList.add(returnRecord);
		return reruenList;
	}
	public List<Record> deletePic(String userid,String pids){
		List<Record> list=new ArrayList<Record>();
		Record record=new Record();
		int canDelete=0;
		String[] pidArray=pids.split(",");
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
		record.set("deleteCnt", canDelete);
		list.add(record);
		return list;
	}
	/**
	 * 保存分享信息id
	 * @param ids
	 * @param uid
	 */
	public String addShare(String ids,String uid){
		String shareId=null;
		SimH5Share share =new SimH5Share();
		share.set("pids", ids).set("createUserId", uid).set("status", 0);
		if(share.save()){
			shareId=share.getInt("id").toString();
		}
		return shareId;
	}
	public List<Record> getShareValue(String id){
		QiniuOperate operate = new QiniuOperate();
		List<Record> returnList=new ArrayList<Record>();
		List<Record> shareList=Db.find("select pids from simH5Share where id="+id);
		if(!shareList.isEmpty()){
			String pids=shareList.get(0).get("pids");
			List<Record> picList=Db.find("select pid,poriginal,peid as eid,pcover from pictures where pid in ("+pids+")");
			for(Record r:picList){
				String cover=r.get("poriginal");
				if(r.get("pcover")!=null&&(cover.indexOf(".mp4")!=-1||cover.indexOf(".MP4")!=-1
						||cover.indexOf(".3gp")!=-1||cover.indexOf(".3GP")!=-1
						||cover.indexOf(".avi")!=-1||cover.indexOf(".AVI")!=-1
						||cover.indexOf(".flv")!=-1||cover.indexOf(".FLV")!=-1
						||cover.indexOf(".MKV")!=-1||cover.indexOf(".mkv")!=-1)){
					r.set("pcover", operate
							.getDownloadToken(cover + "?vframe/jpg/offset/1/w/750"));	
				}
				r.set("thumbnail", operate
						.getDownloadToken(r.get("poriginal").toString() + "?imageView2/2/w/600"));
				// 中等缩略图授权
				r.set("midThumbnail", operate
						.getDownloadToken(r.get("poriginal").toString() + "?imageView2/2/w/1000"));
				// 原图授权
				r.set("poriginal",
						operate.getDownloadToken(r.get("poriginal").toString()));
			}
			returnList=picList;
//			String[] pidArray=pids.split(",");
//			for(int i=0;i<pidArray.length;i++){
//				List<Record> picList=Db.find("select pid")
//			}
		}
		return returnList;
	}
}
