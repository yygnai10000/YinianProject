package yinian.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.jfinal.aop.Before;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.model.Event;
import yinian.model.Group;
import yinian.model.GroupMember;
import yinian.model.HotPic;
import yinian.model.Picture;
import yinian.model.UserHotPic;
import yinian.utils.DES;
import yinian.utils.Jm;
import yinian.utils.JsonData;
import yinian.utils.QiniuOperate;

public class NewH5Service {
	private String jsonString;// 返回结果
	private JsonData jsonData = new JsonData(); // json操作类
	private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类
	/**
	 * 创建相册
	 * 
	 * @param groupName
	 * @param userid
	 * @param address
	 * @param groupType
	 * @return
	 */
	@Before(Tx.class)
	public String createAlbum(String groupName, String userid, String address, String groupType, String inviteCode,
			String source,String isDefault,String groupNewType,String openGId) {
		
		Group group = new Group().set("gname", groupName).set("gcreator", userid).set("gpic", address)
				.set("gtype", groupType).set("gnum", 1).set("ginvite", inviteCode)
				.set("isDefault", isDefault).set("groupNewType", groupNewType);
		if (source != null) {
			group.set("gsource", source);
		}
		if(isDefault.equals("1")){
			group.set("openGId", openGId);
		}

		if (group.save()) {
			String groupid = group.get("groupid").toString();			
			// 插入数据到groupmembers表中
			boolean insertFlag = insertIntoGroupMembers(userid, groupid);
			if (insertFlag) {
				if (!(groupType.equals("5"))) {
					String encodeGroupid = "";
					try {
						encodeGroupid = DES.encryptDES("groupid=" + groupid, CommonParam.DESSecretKey);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}			
					List<Record> list = getSingleAlbum(groupid);// 获取创建的组的信息
					list.get(0).set("encodeGroupid", encodeGroupid);
					list = dataProcess.changeGroupTypeIntoWord(list);// 转换组类型为文字

					jsonString = jsonData.getSuccessJson(list);

				} else {
					List<Record> list = getSingleAlbum(groupid);// 获取创建的组的信息
					list = dataProcess.changeGroupTypeIntoWord(list);// 转换组类型为文字
					jsonString = jsonData.getJson(0, "success", list);
				}

			} else {
				jsonString = jsonData.getJson(-50, "插入数据失败");
			}
		} else {
			jsonString = jsonData.getJson(-50, "插入数据失败");
		}
		return jsonString;
	}
	/**
	 * 创建群相册-返回相册id
	 * 
	 * @param groupName
	 * @param userid
	 * @param address
	 * @param groupType
	 * @return
	 */
	@Before(Tx.class)
	public String createFlockAlbum(String groupName, String userid, String address, String groupType, String inviteCode,
			String source,String isDefault,String groupNewType,String openGId,String eid) {
		
		Group group = new Group().set("gname", groupName).set("gcreator", userid).set("gpic", address)
				.set("gtype", groupType).set("gnum", 1).set("ginvite", inviteCode)
				.set("isDefault", isDefault).set("groupNewType", groupNewType);
		if (source != null) {
			group.set("gsource", source);
		}
		if(isDefault.equals("1")){
			group.set("openGId", openGId);
		}

		if (group.save()) {
			String groupid = group.get("groupid").toString();	
			if(!eid.equals("")){
							//发布动态
				transferEvent(userid, eid, groupid);
			}
			// 插入数据到groupmembers表中
			boolean insertFlag = insertIntoGroupMembers(userid, groupid);
			if (insertFlag) {
				if (!(groupType.equals("5"))) {
					String encodeGroupid = "";
					try {
						encodeGroupid = DES.encryptDES("groupid=" + groupid, CommonParam.DESSecretKey);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}			
//					List<Record> list = getSingleAlbum(groupid);// 获取创建的组的信息
//					list.get(0).set("encodeGroupid", encodeGroupid);
//					list = dataProcess.changeGroupTypeIntoWord(list);// 转换组类型为文字
//
//					jsonString = jsonData.getSuccessJson(list);

				} else {
//					List<Record> list = getSingleAlbum(groupid);// 获取创建的组的信息
//					list = dataProcess.changeGroupTypeIntoWord(list);// 转换组类型为文字
//					jsonString = jsonData.getJson(0, "success", list);
				}
				List<Record> list =new ArrayList<Record>();
				Record r=new Record();
				r.set("groupid", groupid);
				list.add(r);
				jsonString = jsonData.getSuccessJson(list);
			} else {
				jsonString = jsonData.getJson(-50, "插入数据失败");
			}
		} else {
			jsonString = jsonData.getJson(-50, "插入数据失败");
		}
		return jsonString;
	}
	/*
	 * 群相册-根据相册ID和热点图片id创建动态
	 */
	public void createHotPicEvent(String groupid,String userid,String hid){
		if(!hid.equals("")){
			//发布动态
			//transferEvent(userid, eid, groupid);
			HotPic hotPic=new HotPic().findById(hid);
			Event e=new Event();
			if(hotPic.get("hType").equals("0")){
				e.set("efirstpic", hotPic.get("hPic"));
			}
			e.set("egroupid", groupid).set("euserid", userid).set("eMain", hotPic.get("hType")).set("etype",0)
			.set("elike",0).set("eView",0).set("euploadtime",new Date()).set("eStoragePlace",0).set("estatus",0).set("elevel",0)
			.set("isAnonymous",0).set("isRecommend",0).set("isTopInRecommendGroup",0).set("isSynchronize",1).set("ecardstyle",0);
			if(e.save()){
				Picture p=new Picture();
				if(null!=hotPic.get("hPcover")){
					p.set("pcover", hotPic.get("hPcover"));
				}
				p.set("peid", e.get("eid").toString()).set("poriginal", hotPic.get("hPic"))
					.set("pGroupid", groupid).set("puserid", userid).set("pMain", hotPic.get("hType"));
				p.save();
			}

		}
	}
	/**
	 * 创建群相册-返回相册id-热点图片版
	 * 
	 * @param groupName
	 * @param userid
	 * @param address
	 * @param groupType
	 * @return
	 */
	@Before(Tx.class)
	public String createFlockAlbumWithHotPic(String groupName, String userid, String address, String groupType, String inviteCode,
			String source,String isDefault,String groupNewType,String openGId,String hid) {
		
		Group group = new Group().set("gname", groupName).set("gcreator", userid).set("gpic", address)
				.set("gtype", groupType).set("gnum", 1).set("ginvite", inviteCode)
				.set("isDefault", isDefault).set("groupNewType", groupNewType);
		if (source != null) {
			group.set("gsource", source);
		}
		if(isDefault.equals("1")){
			group.set("openGId", openGId);
		}

		if (group.save()) {
			String groupid = group.get("groupid").toString();	
			if(!hid.equals("")){
							//发布动态
				//transferEvent(userid, eid, groupid);
				HotPic hotPic=new HotPic().findById(hid);
				Event e=new Event();
				if(hotPic.get("hType").equals("0")){
					e.set("efirstpic", hotPic.get("hPic"));
				}
				e.set("egroupid", groupid).set("euserid", userid).set("eMain", hotPic.get("hType")).set("etype",0)
				.set("elike",0).set("eView",0).set("euploadtime",new Date()).set("eStoragePlace",0).set("estatus",0).set("elevel",0)
				.set("isAnonymous",0).set("isRecommend",0).set("isTopInRecommendGroup",0).set("isSynchronize",1).set("ecardstyle",0);
				if(e.save()){
					Picture p=new Picture();
					if(null!=hotPic.get("hPcover")){
						p.set("pcover", hotPic.get("hPcover"));
					}
					p.set("peid", e.get("eid").toString()).set("poriginal", hotPic.get("hPic"))
							.set("pGroupid", groupid).set("puserid", userid).set("pMain", hotPic.get("hType"));
					p.save();
				}
				
			}
			// 插入数据到groupmembers表中
			boolean insertFlag = insertIntoGroupMembers(userid, groupid);
			if (insertFlag) {
				if (!(groupType.equals("5"))) {
					String encodeGroupid = "";
					try {
						encodeGroupid = DES.encryptDES("groupid=" + groupid, CommonParam.DESSecretKey);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}			
//					List<Record> list = getSingleAlbum(groupid);// 获取创建的组的信息
//					list.get(0).set("encodeGroupid", encodeGroupid);
//					list = dataProcess.changeGroupTypeIntoWord(list);// 转换组类型为文字
//
//					jsonString = jsonData.getSuccessJson(list);

				} else {
//					List<Record> list = getSingleAlbum(groupid);// 获取创建的组的信息
//					list = dataProcess.changeGroupTypeIntoWord(list);// 转换组类型为文字
//					jsonString = jsonData.getJson(0, "success", list);
				}
				List<Record> list =new ArrayList<Record>();
				Record r=new Record();
				r.set("groupid", groupid);
				list.add(r);
				jsonString = jsonData.getSuccessJson(list);
			} else {
				jsonString = jsonData.getJson(-50, "插入数据失败");
			}
		} else {
			jsonString = jsonData.getJson(-50, "插入数据失败");
		}
		return jsonString;
	}
	/**
	 * 插入groupmembers表，需判断用户是否在相册里面
	 * 
	 * @param userid
	 * @param groupid
	 * @return
	 */
	public boolean insertIntoGroupMembers(String userid, String groupid) {
		boolean flag;
		int count = 0; // 排序值
		GroupMember groupMember = new GroupMember().set("gmuserid", userid).set("gmgroupid", groupid).set("gmorder",
				count).set("isAdmin", 1);
		flag = groupMember.save();
		return flag;
	}
	
	/**
	 * 插入groupmembers表，将置顶和创建者信息记录下来
	 * @param userid
	 * @param groupid
	 * @return
	 */
	public boolean insertIntoGroupMembersNew(String userid, String groupid) {
		boolean flag;
		int count = 0; // 排序值
		GroupMember groupMember = new GroupMember().set("gmuserid", userid).set("gmgroupid", groupid).set("gmorder",
				count).set("IsAdmin", 1);
		flag = groupMember.save();
		return flag;
	}
	/**
	 * 获取单个相册信息
	 * 
	 * @param groupid
	 * @return
	 */
	public List<Record> getSingleAlbum(String groupid) {
		String sql = "select groupid,gimid,gname,gpic,gnum,gtime,gtype,gcreator,ginvite from groups where groupid ='"
				+ groupid + "' ";
		List<Record> list = Db.find(sql);
		// 刚创建相册时，直接把相册照片数设置为0
		if(null!=list&&!list.isEmpty()){
			list.get(0).set("gpicnum", 0);
		}
		return list;
	}
	/**
	 * 根据openGId获取群相册信息
	 * 
	 * @param groupid
	 * @return
	 */
	public List<Record> getFlockAlbumByOpenGId(String openGId) {
		String sql = "select groupid from groups where openGId ='"
				+ openGId + "' and gstatus=0 limit 1";
		List<Record> list = Db.find(sql);
		// 刚创建相册时，直接把相册照片数设置为0
		//list.get(0).set("gpicnum", 0);
		return list;
	}
	/**
	 * 获取根据群相册openGId判断群相册是否创建
	 * 
	 * @param groupid
	 * @return
	 */
	public boolean canCreateFlockAlbum(String openGId) {
		boolean canCreate=false;
		String sql = "select count(*) cnt from groups where openGId ='"
				+ openGId + "' and gstatus=0 ";
		List<Record> list = Db.find(sql);
		// 刚创建相册时，直接把相册照片数设置为0
		if(null!=list&&!list.isEmpty()){
			System.out.println("cnt="+list.get(0).get("cnt"));
			if(list.get(0).get("cnt").toString().equals("0")){
				canCreate=true;
			}
		}else{
			canCreate=true;
		}
		System.out.println("canCreate="+canCreate);
		return canCreate;
	}
	/*
	 * 动态转移到新相册
	 */
	public void transferEvent(String userid, String eid, String groupid) {
		boolean createEvent=false;
		// 转移动态
		Event event = new Event().findById(eid);
		// 新动态发布时间
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String time = sdf.format(new Date());
		event.set("euserid", userid).set("egroupid", groupid).set("euploadtime", time).set("isSynchronize", 1);
		event.remove("eid").remove("etext").remove("eVerifyPic")
		.remove("ePeopleName").remove("eplace").remove("ePlacePic").remove("ePlaceLongitude")
		.remove("ePlaceLatitude").remove("elike").remove("eView");
		if (event.save()) {
			// 获得新的eid
			String newEid = event.get("eid").toString();
			// 转移照片
			List<Record> pictureList = Db.find("select * from pictures where peid=" + eid + " and pstatus=0 ");
			for (Record record : pictureList) {
				String address = record.get("poriginal").toString();
				Picture picture = new Picture().set("poriginal", address).set("peid", newEid)
						.set("pGroupid", groupid).set("puserid", userid).set("pMain", event.get("eMain"));
				boolean flag = picture.save();
				if (!flag) {
					//return null;
				}
			}
			//EventService eService = new EventService();
			//return eService.getSingleEvent(Integer.parseInt(newEid), "app");
		} else {
			//return null;
		}
	}
	/*
	 * 发现-根据用户id查看用户最后一次查看的图片，若用户没有看过或已看到最后一张，则从头开始浏览，每次获取10张图片
	 */
	public List<Record> getUserHotPicList(String userid){
		List<Record> returnList=new ArrayList<Record>();
		List<Record> userList=Db.find("select * from userhotpic where uid="+userid+" limit 1");
		if(null==userList||userList.isEmpty()){
			//直接取取10条
			returnList=Db.find("select * from hotpic where hStatus=0 order by hid asc limit 20");
		}else{
			//判断是否属于最后10条，如果是，则从头取数据
			List<Record> maxH=Db.find("select max(hid) hid from hotpic where hStatus=0");
			String uHid=userList.get(0).get("hid").toString();
			if(null==maxH||maxH.isEmpty()){
				returnList=Db.find("select * from hotpic where hStatus=0 order by hid asc limit 20");
			}else{
				String maxHid=maxH.get(0).getLong("hid").toString();
				if(Integer.parseInt(maxHid)-Integer.parseInt(uHid)<10){
					returnList=Db.find("select * from hotpic where hStatus=0 order by hid asc limit 20");
				}else{
					returnList=Db.find("select * from hotpic where hStatus=0 and hid >"+uHid+" order by hid asc limit 20");
				}
			}
		}
		return getPrivatePic(returnList);
	}
	/*
	 * 热点图片授权
	 */
	public List<Record> getPrivatePic(List<Record> list){
		QiniuOperate operate = new QiniuOperate();
		YinianDataProcess yp=new YinianDataProcess();
		if (list.size() != 0) {
		for (Record record : list) {
			if(record.get("hid").toString().equals("28")){
				System.out.println("id===28");
			}
			String eMain=record.get("hType").toString();
			String hPic=record.get("hPic").toString();
			// 获取图片访问权限
			if(eMain.equals("4")){
			//视频封面
			record.set("hPcover", null!=record.get("hPcover")&&!record.get("hPcover").equals("")
								?record.get("hPcover"):yp.getVideoCover(record.get("hPic").toString(),eMain));
			record.set("hPic", operate
					.getDownloadToken(hPic));
			}else{
			// 缩略图授权
			record.set("hPic", operate
								.getDownloadToken(hPic + "?imageView2/2/w/600"));					
			}
			}

		
		}
		return list;
	}
	/*
	 * 发现-记录用户最后一次浏览的图片
	 */
	public boolean setUserHotPic(String userid,String hid){
		//boolean status=false;
		List<Record> uhList=Db.find("select * from userhotpic where uid="+userid+" limit 1");
		if(null!=uhList&&!uhList.isEmpty()){
			Record r=uhList.get(0);
			UserHotPic up=new UserHotPic().findById(r.getLong("uhid"));
			up.set("hid", hid);
			return up.update();
		}else{
			UserHotPic up=new UserHotPic();
			up.set("uid", userid);
			up.set("hid", hid);
			return up.save();
		}
	}
	/*
	 * 判断用户是否第一次发布动态
	 */
	public boolean userFirstPublish(String userid){
		boolean status=false;
		List<Record> list=Db.find("select * from events where euserid="+userid+" limit 2");
		if(null!=list&&!list.isEmpty()){
			if(list.size()==1){
				status=true;
			}
		}else{
			status=true;
		}
		return status;
	}
	/*
	 * 相册封面-获取相册封面列表
	 */
	public List<Record> getAllDefaultAlbumCover(String acid){		
		String conds="";
		if(null!=acid&&!acid.equals("")){
			conds+="and acid >"+acid;		
		 }
		String sql="select * from albumcover where acgtype >=11 "+conds+" order by acid asc ";//limit 20
		return Db.find(sql);
	}
	/*
	 * 相册密码-通过用户id获取秘钥key
	 */
	public String getKey(String userid){		
		String key="";
		try{
			key= new Jm().encrypt(userid);
		}catch(Exception e){
			
		}finally{
			return key;
		}
	}
	/*
	 * 相册密码-通过秘钥key获取用户id
	 */
	public String getUseridByKey(String key){		
		String userid="";
		try{
			userid= new Jm().decrypt(key);
		}catch(Exception e){
			
		}finally{
			return userid;
		}
	}
}
