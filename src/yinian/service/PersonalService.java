package yinian.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

import yinian.common.CommonParam;
import yinian.model.PersonalAlbum;
import yinian.model.PersonalPhotoAlbum;
import yinian.utils.QiniuOperate;

public class PersonalService {
	//保存用户上传个人相册的照片
	public List<Record> insertPhoto(String userid,String proiginal,String md5,
		String createtime,String createmonth, int type){
		boolean status=false;
		QiniuOperate operate = new QiniuOperate();
		List<Record> list=new ArrayList<Record>();
		String[] pArray=proiginal.split(",");	
		String[] md5Array=md5.split(",");
		String[] createtimeArray=createtime.split(",");
		if(pArray.length>0&&pArray.length==md5Array.length&&pArray.length==createtimeArray.length){
			for (int i = 0; i < pArray.length; i++) {
				Record r=new Record();
				PersonalPhotoAlbum ppa=new PersonalPhotoAlbum();
				String month=createtimeArray[i].substring(0, 7);
				int pstatus = 0;
				if(Db.find("select md5 from personalPhotoAlbum where status=0 and userid="+userid+" and md5='"+md5Array[i]+"'").isEmpty()) {
					r.set("isRepeat", false);//是否为重复上传 false：否
				}else {
					pstatus = 1;//重复上传保存时状态置为"1"，以便查询时自动过滤去重
					r.set("isRepeat", true);//是否为重复上传 true：是
				}
				r.set("aid", Integer.parseInt(ppa.insert(userid, CommonParam.personalQiniuPrivateAddress+pArray[i], md5Array[i], createtimeArray[i], month, new Date(), pstatus, "", "", type)));
				r.set("md5",md5Array[i]);
				r.set("createTime",createtimeArray[i]);
				r.set("thumbnail", operate
						.getDownloadToken(CommonParam.personalQiniuPrivateAddress+pArray[i] + "?imageView2/2/w/250"));
				r.set("eMain", type);
				list.add(r);				
			}
			
			status=true;
		}
		return list;
	}
	
	//保存用户上传到个人相册的短视频
	public List<Record> insertShortVedio(String userid, String proiginal, String md5, 
		String createTime, String cover, String storage, int type) {
		
//		QiniuOperate operate = new QiniuOperate();
		List<Record> list = new ArrayList<Record>();
		String month = createTime.substring(0, 7);
		
		Record r = new Record();
		r.set("aid", PersonalPhotoAlbum.insert(userid, proiginal, md5, createTime, month, new Date(), 0, cover, storage, type));
		r.set("md5", md5);
//		r.set("thumbnail", operate
//				.getDownloadToken(CommonParam.qiniuPrivateAddress+proiginal + "?imageView2/2/w/250"));
		r.set("eMain", type);
		list.add(r);
		
		return list;
	}
	
	public boolean isCanDel(String userid,String aids){
		boolean returnStatus=false;
		//a[]=aids.split(",").length
		if(getCntByUserIdAndAids(userid,aids)==aids.split(",").length){
			returnStatus=true;
		}
		return returnStatus;
	}
	public int getCntByUserIdAndAids(String userid,String aids){
		List<Record> list=Db.find("select count(*) cnt from personalPhotoAlbum where userid="+userid+
				" and aid in ("+aids+")");
		int cnt=0;
		if(!list.isEmpty()){
			cnt=list.get(0).getLong("cnt").intValue();
		}
		return cnt;
	}
	//根据用户id,图片id删除图片
	public boolean deletePhoto(String aids,String userid){
		boolean returnStatus=false;
		
		String[] aidArray=aids.split(",");
		//StringBuffer str=new StringBuffer();
		List<String> md5List=new ArrayList<>();
		for(int i=0;i<aidArray.length;i++){
			PersonalPhotoAlbum p=new PersonalPhotoAlbum().findById(aidArray[i]);
			md5List.add(p.getStr("md5"));
		}
		if(Db.update("update personalPhotoAlbum set status =1 where aid in ("+aids+")")>0){
			for(int i=0;i<md5List.size();i++){
				String sql="update personalPhotoAlbum set status =1 where userid="+userid+" and md5='"+md5List.get(i)+"'";
				System.out.println(sql);
				Db.update(sql);
			}
			returnStatus=true;
		}
		return returnStatus;
	}
	//根据用户id获取用户上传个人相册的照片 按日或月排序
	public List<Record> findListByUserIdAndMode(String userId,String mode){
		QiniuOperate operate = new QiniuOperate();
		List<Record> returnList=new ArrayList<Record>();
		if(mode.equals("month")){//按月查询
			List<Record> picList=Db.find("select aid,poriginal,md5,createtime,createmonth from personalPhotoAlbum where status=0 and userid="+userId+" order by createmonth desc ");
			//Map<String, List<Record>> map=new HashMap<String, List<Record>>();
			 LinkedHashMap <String,List<Record> > map = new LinkedHashMap<String,List<Record>>(10, 0.75f, false);
			Set<String> md5Set = new HashSet<String>();//重复图片去重：记录md5，出现重复md5的结果将被过滤
			for(Record r:picList){
				if(!md5Set.contains(r.get("md5"))||r.get("md5")=="") {
					md5Set.add(r.get("md5"));
					
					String createmonth=r.get("createmonth").toString();
					r.set("thumbnail", operate
							.getDownloadToken(r.get("poriginal").toString() + "?imageView2/2/w/250"));
					r.remove("poriginal");
					if(null!=map.get(createmonth)){
						//List l=map.get(r.get("createmonth"));
						r.remove("createmonth");
						map.get(createmonth).add(r);
					}else{
						List<Record> list=new ArrayList<Record>();
						r.remove("createmonth");
						list.add(r);
						map.put(createmonth, list);
					}
				}
			} 
			for (Map.Entry<String, List<Record>> entry : map.entrySet()) {  
				Record record=new Record();
				record.set("date", entry.getKey());
				record.set("List", entry.getValue());
				returnList.add(record); 
			}  		
		}
		if(mode.equals("day")){
			List<Record> picList=Db.find("select aid,poriginal,md5,DATE_FORMAT(createtime,'%Y-%m-%d') createday,createtime from personalPhotoAlbum where status=0 and userid="+userId+" order by createtime desc ");
			//Map<String, List<Record>> map=new HashMap<String, List<Record>>();
			LinkedHashMap <String,List<Record> > map = new LinkedHashMap<String,List<Record>>(10, 0.75f, false);
			Set<String> md5Set = new HashSet<String>();//重复图片去重：记录md5，出现重复md5的结果将被过滤
			for(Record r:picList){
				if(!md5Set.contains(r.get("md5"))||r.get("md5")=="") {
					md5Set.add(r.get("md5"));
					
					r.set("thumbnail", operate
							.getDownloadToken(r.get("poriginal").toString() + "?imageView2/2/w/250"));
					r.remove("poriginal");
					// 缩略图授权
					
					String createtime=r.get("createday").toString();
					r.remove("createday");
					if(null!=map.get(createtime)){
						//List l=map.get(r.get("createmonth"));
						//r.remove("createmonth");
						map.get(createtime).add(r);
					}else{
						List<Record> list=new ArrayList<Record>();
						
						list.add(r);
						map.put(createtime, list);
					}
				}
			} 
			for (Map.Entry<String, List<Record>> entry : map.entrySet()) {  
				Record record=new Record();
				record.set("date", entry.getKey());
				record.set("List", entry.getValue());
				System.out.println(entry.getKey());
				returnList.add(record); 
			}  		
		}
		//Collections.reverse(returnList);
		return returnList;
	}
	//根据用户id和图片id获取图片详情
	public List<Record> getUserPhotoById(String userid,String aid){
		QiniuOperate operate = new QiniuOperate();
		List<Record> picList=Db.find("select aid,poriginal,remark from personalPhotoAlbum where aid="+aid+" and  userid="+userid+" order by createtime desc ");
		if(picList.size()>0){
			for(Record r:picList){
				r.set("midThumbnail", operate
						.getDownloadToken(r.get("poriginal").toString() + "?imageView2/2/w/1000"));
				// 原图授权
				r.set("poriginal", operate.getDownloadToken(r.get("poriginal").toString()));
				r.set("remark", r.get("remark")==null?"":r.get("remark"));
			}
		}
		return picList;
	}
	//根据用户id和图片id设置图片备注信息
	public List<Record> setPhotoRemark(String userid, String aid, String remark){
		List<Record> list=new ArrayList<Record>();
		Record r = new Record();
		r.set("aid", aid);
		if(Db.update("update personalPhotoAlbum set remark ='"+remark+"' where aid="+aid+" and userid="+userid)>0) {
			r.set("remark", remark);
		}
		list.add(r);
		
		return list;
	}
	//根据用户id和图片id获取图片下载地址
	public List<Record> getUserPhotoAddressById(String userid,String aid){
		QiniuOperate operate = new QiniuOperate();
		List<Record> picList=Db.find("select aid,poriginal,md5 from personalPhotoAlbum where status=0 and aid in ("+aid+") and  userid="+userid+" order by createtime desc ");
		if(picList.size()>0){
			for(Record r:picList){				
				// 原图授权
				r.set("poriginal", operate.getDownloadToken(r.get("poriginal").toString()));
			}
		}
		return picList;
	}
	//pc端专用
	//根据用户id获取用户上传个人相册的照片 按日或月排序
	
	
		public List<Record> pcFindListByUserIdAndMode(String userId,String mode,String endDate){
			QiniuOperate operate = new QiniuOperate();
			List<Record> returnList=new ArrayList<Record>();
			String conds="";
			
			if(mode.equals("month")){//按月查询
				if(endDate!=""){
					conds=" and createmonth<'"+endDate+"' ";
				}
				String sql="select aid,poriginal,md5,createtime,createmonth,type from personalPhotoAlbum where status=0 "+conds+" and userid="+userId+" order by createmonth desc ";
				List<Record> picList=Db.find(sql);
				//Map<String, List<Record>> map=new HashMap<String, List<Record>>();
				LinkedHashMap <String,List<Record> > map = new LinkedHashMap<String,List<Record>>(10, 0.75f, false);
				Set<String> md5Set = new HashSet<String>();//重复图片去重：记录md5，出现重复md5的结果将被过滤
				for(Record r:picList){
					if(!md5Set.contains(r.get("md5"))||r.get("md5")=="") {
						md5Set.add(r.get("md5"));
						
						String mapLastMonth="";
						String createmonth=r.get("createmonth").toString();
						r.set("thumbnail", operate
								.getDownloadToken(r.get("poriginal").toString() + "?imageView2/2/w/500"));
						r.set("midThumbnail", operate
								.getDownloadToken(r.get("poriginal").toString() + "?imageView2/2/w/1000"));
						// 原图授权
						r.set("poriginal", operate.getDownloadToken(r.get("poriginal").toString()));
						//图片视频区分
						r.set("eMain", r.get("type").toString());
						r.remove("type");
						//r.remove("poriginal");
						if(null!=map.get(createmonth)){
							
							System.out.println("null!=map.get"+createmonth+"===");
							//List l=map.get(r.get("createmonth"));
							r.remove("createmonth");
							map.get(createmonth).add(r);
						}else{
							mapLastMonth=createmonth;
							System.out.println("null==map.get"+createmonth+"==="+r.get("aid"));
							List<Record> list=new ArrayList<Record>();
							r.remove("createmonth");
							list.add(r);
							map.put(createmonth, list);
						}
						if(map.size()>3){
							map.remove(mapLastMonth);
							break;
						}
					}
				} 
				for (Map.Entry<String, List<Record>> entry : map.entrySet()) {  
					Record record=new Record();
					record.set("date", entry.getKey());
					record.set("List", entry.getValue());
					returnList.add(record); 
				}  		
			}
			if(mode.equals("day")){
				if(endDate!=""){
					conds=" and createtime<'"+endDate+"' ";
				}
				String sql="select aid,poriginal,md5,DATE_FORMAT(createtime,'%Y-%m-%d') createday,createtime,type from personalPhotoAlbum where status=0 "+conds+" and userid="+userId+" order by createtime desc ";
				List<Record> picList=Db.find(sql);
				//Map<String, List<Record>> map=new HashMap<String, List<Record>>();
				LinkedHashMap <String,List<Record> > map = new LinkedHashMap<String,List<Record>>(10, 0.75f, false); 
				// Map<String,Object> cntMap=new HashMap<>();
				Set<String> md5Set = new HashSet<String>();//重复图片去重：记录md5，出现重复md5的结果将被过滤
				for(Record r:picList){
					if(!md5Set.contains(r.get("md5"))||r.get("md5")=="") {
						md5Set.add(r.get("md5"));
						
						String mapLastCreatetime="";
						r.set("thumbnail", operate
								.getDownloadToken(r.get("poriginal").toString() + "?imageView2/2/w/500"));
						r.set("midThumbnail", operate
								.getDownloadToken(r.get("poriginal").toString() + "?imageView2/2/w/1000"));
						// 原图授权
						r.set("poriginal", operate.getDownloadToken(r.get("poriginal").toString()));
						// 缩略图授权
						//图片视频区分
						r.set("eMain", r.get("type").toString());
						r.remove("type");
						
						String createtime=r.get("createday").toString();
						r.remove("createday");
						if(null!=map.get(createtime)){
							
							//List l=map.get(r.get("createmonth"));
							//r.remove("createmonth");
							map.get(createtime).add(r);
						}else{
							mapLastCreatetime=createtime;
							List<Record> list=new ArrayList<Record>();						
							list.add(r);
							map.put(createtime, list);
						}
						
						if(map.size()>5){
							map.remove(mapLastCreatetime);
							break;
						}
					}
				} 
				for (Map.Entry<String, List<Record>> entry : map.entrySet()) {  
					Record record=new Record();
					record.set("date", entry.getKey());
					record.set("List", entry.getValue());
					System.out.println(entry.getKey());
					returnList.add(record); 
				}  		
			}
			//Collections.reverse(returnList);
			return returnList;
		}
		
		/**
		 * pc端获取用户个人相册照片，按上传时间排序
		 * @param userId
		 * @param mode
		 * @param endDate
		 * @return
		 */
		public List<Record> pcFindListByUserIdAndModeAndUploadtime(String userId,String mode,String endDate){
			QiniuOperate operate = new QiniuOperate();
			List<Record> returnList=new ArrayList<Record>();
			String conds="";
			
			if(mode.equals("month")){//按月查询
				if(endDate!=""){
					conds=" and DATE_FORMAT(uploadtime,'%Y-%m')<'"+endDate+"' ";
				}
				String sql="select aid,poriginal,md5,uploadtime,DATE_FORMAT(uploadtime,'%Y-%m') uploadmonth,type from personalPhotoAlbum where status=0 "+conds+" and userid="+userId+" order by uploadtime desc ";
				List<Record> picList=Db.find(sql);
				//Map<String, List<Record>> map=new HashMap<String, List<Record>>();
				LinkedHashMap <String,List<Record> > map = new LinkedHashMap<String,List<Record>>(10, 0.75f, false);
				Set<String> md5Set = new HashSet<String>();//重复图片去重：记录md5，出现重复md5的结果将被过滤
				for(Record r:picList){
					if(!md5Set.contains(r.get("md5"))||r.get("md5")=="") {
						md5Set.add(r.get("md5"));
						
						String mapLastMonth="";
						String uploadmonth=r.get("uploadmonth").toString();
						r.set("thumbnail", operate
								.getDownloadToken(r.get("poriginal").toString() + "?imageView2/2/w/500"));
						r.set("midThumbnail", operate
								.getDownloadToken(r.get("poriginal").toString() + "?imageView2/2/w/1000"));
						// 原图授权
						r.set("poriginal", operate.getDownloadToken(r.get("poriginal").toString()));
						//图片视频区分
						r.set("eMain", r.get("type").toString());
						r.remove("type");
						//r.remove("poriginal");
						if(null!=map.get(uploadmonth)){
							
							System.out.println("null!=map.get"+uploadmonth+"===");
							//List l=map.get(r.get("createmonth"));
							r.remove("uploadmonth");
							map.get(uploadmonth).add(r);
						}else{
							mapLastMonth=uploadmonth;
							System.out.println("null==map.get"+uploadmonth+"==="+r.get("aid"));
							List<Record> list=new ArrayList<Record>();
							r.remove("uploadmonth");
							list.add(r);
							map.put(uploadmonth, list);
						}
						if(map.size()>3){
							map.remove(mapLastMonth);
							break;
						}
					}
				} 
				for (Map.Entry<String, List<Record>> entry : map.entrySet()) {  
					Record record=new Record();
					record.set("date", entry.getKey());
					record.set("List", entry.getValue());
					returnList.add(record); 
				}  		
			}
			if(mode.equals("day")){
				if(endDate!=""){
					conds=" and uploadtime<'"+endDate+"' ";
				}
				String sql="select aid,poriginal,md5,DATE_FORMAT(uploadtime,'%Y-%m-%d') uploadday,uploadtime,type from personalPhotoAlbum where status=0 "+conds+" and userid="+userId+" order by uploadtime desc ";
				List<Record> picList=Db.find(sql);
				//Map<String, List<Record>> map=new HashMap<String, List<Record>>();
				LinkedHashMap <String,List<Record> > map = new LinkedHashMap<String,List<Record>>(10, 0.75f, false); 
				// Map<String,Object> cntMap=new HashMap<>();
				Set<String> md5Set = new HashSet<String>();//重复图片去重：记录md5，出现重复md5的结果将被过滤
				for(Record r:picList){
					if(!md5Set.contains(r.get("md5"))||r.get("md5")=="") {
						md5Set.add(r.get("md5"));
						
						String mapLastUploadtime="";
						r.set("thumbnail", operate
								.getDownloadToken(r.get("poriginal").toString() + "?imageView2/2/w/500"));
						r.set("midThumbnail", operate
								.getDownloadToken(r.get("poriginal").toString() + "?imageView2/2/w/1000"));
						// 原图授权
						r.set("poriginal", operate.getDownloadToken(r.get("poriginal").toString()));
						// 缩略图授权
						//图片视频区分
						r.set("eMain", r.get("type").toString());
						r.remove("type");
						
						String uploadtime=r.get("uploadday").toString();
						r.remove("uploadday");
						if(null!=map.get(uploadtime)){
							
							//List l=map.get(r.get("createmonth"));
							//r.remove("createmonth");
							map.get(uploadtime).add(r);
						}else{
							mapLastUploadtime=uploadtime;
							List<Record> list=new ArrayList<Record>();						
							list.add(r);
							map.put(uploadtime, list);
						}
						
						if(map.size()>5){
							map.remove(mapLastUploadtime);
							break;
						}
					}
				} 
				for (Map.Entry<String, List<Record>> entry : map.entrySet()) {  
					Record record=new Record();
					record.set("date", entry.getKey());
					record.set("List", entry.getValue());
					System.out.println(entry.getKey());
					returnList.add(record); 
				}  		
			}
			//Collections.reverse(returnList);
			return returnList;
		}
		
	//保存用户创建的个人相册
//	public List<Record> insertPersonalAlbum(String userid, String pename, String pepicAddress){
//		PersonalAlbum pa = new PersonalAlbum();
//		pa.set("userid", userid).set("pename", pename).set("pepic", pepicAddress);
//		List<Record> list = new ArrayList<Record>();
//		if(pa.save()) {
//			String peid = pa.get("peid").toString();
//			list = getPersonalAlbumInfo(peid, userid);
//		}
//		return list;
//	}
	//根据用户id获取用户个人相册信息
	public List<Record> getPersonalAlbumInfo(String userid){
		QiniuOperate operate = new QiniuOperate();
		List<Record> list = Db.find("select userid,pename,pepic,pestatus,petime from personalAlbum where pestatus=0 and userid = "+userid);
		if(!list.isEmpty()) {
			Record r= list.get(0);
			r.set("pepic", operate.getDownloadToken(r.get("pepic").toString()));
		}
		return list;
	}
	//根据用户id设置个人相册名
	public List<Record> setPersonalAlbumName(String userid, String pename){
		List<Record> resultList = getPersonalAlbumInfo(userid);
		List<Record> list=new ArrayList<Record>();
		Record r = new Record();
		r.set("userid", userid);
		if(!resultList.isEmpty()) {
			if(Db.update("update personalAlbum set pename ='"+pename+"' where userid="+userid)>0) {
				r.set("pename", pename);
			}
		}else {
			PersonalAlbum.insert(userid, pename, new Date(), "");
			r.set("pename", pename);
		}
		list.add(r);
		
		return list;
	}
	//根据用户id设置个人相册名
	public List<Record> setPersonalAlbumPic(String userid, String pepic){
		QiniuOperate operate = new QiniuOperate();
		List<Record> resultList = getPersonalAlbumInfo(userid);
		List<Record> list=new ArrayList<Record>();
		Record r = new Record();
		r.set("userid", userid);
		String pepicAddress = CommonParam.qiniuPrivateAddress+pepic;
		if(!resultList.isEmpty()) {
			if(Db.update("update personalAlbum set pepic ='"+pepicAddress+"' where userid="+userid)>0) {
				r.set("pepic", operate.getDownloadToken(pepicAddress));
			}
		}else {
			PersonalAlbum.insert(userid, "", new Date(), pepicAddress);
			r.set("pepic", operate.getDownloadToken(pepicAddress));
		}
		list.add(r);
		
		return list;
	}
}
