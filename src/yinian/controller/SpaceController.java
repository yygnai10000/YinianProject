package yinian.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.interceptor.CrossDomain;
import yinian.model.Event;
import yinian.model.Group;
import yinian.model.GroupMember;
import yinian.model.User;
import yinian.service.SpaceService;
import yinian.service.TestService;
import yinian.service.YinianService;
import yinian.utils.JsonData;
import yinian.utils.QiniuOperate;
import yinian.utils.SmallAppQRCode;
import yinian.utils.UrlUtils;

import com.jfinal.aop.Before;
import com.jfinal.core.ActionKey;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.jfinal.plugin.ehcache.CacheKit;

import net.sf.json.JSONArray;

public class SpaceController extends Controller {

	private String jsonString; // 返回的json字符串
	private JsonData jsonData = new JsonData(); // json操作类
	private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类
	private QiniuOperate operate = new QiniuOperate(); // 七牛云处理类
	private SpaceService service = new SpaceService();
	// enhance方法对目标进行AOP增强
	private YinianService TxService = enhance(YinianService.class);
	private static String activityMenuUrl = "http://picture.zhuiyinanian.com/yinian/activityMenu.json";
	private static String guideUrl = "http://picture.zhuiyinanian.com/yinian/guide.json";
	/**
	 * 修改动态等级,置顶功能
	 */
	@ActionKey("/yinian/ModifyEventLevel")
	public void ModifyEventLevel() {
		String userid = this.getPara("userid");
		int groupid = Integer.parseInt(this.getPara("groupid"));
		String eid = this.getPara("eid");
		String type = this.getPara("type");

		// 权限判定
		Group group = new Group().findById(groupid);
		String gcreator = group.get("gcreator").toString();  
		if (userid.equals(gcreator)) {
			Event event = new Event().findById(eid);

			// 判断是在原空间还是推荐款空间置顶
			int egroupid = Integer.parseInt(event.get("egroupid").toString());

			String key = (egroupid == groupid ? "elevel" : "isTopInRecommendGroup");

			if (type.equals("stick")) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String time = sdf.format(new Date());
				event.set(key, 1).set("eTopTime", time);
			}
			if (type.equals("cancel")) {
				event.set(key, 0);
			}
			jsonString = dataProcess.updateFlagResult(event.update());
		} else {
			jsonString = jsonData.getJson(6, "没有权限执行操作");
		}
		renderText(jsonString);

	}

	/**
	 * 推荐功能
	 */
	public void RecommendAndCancel() {
		int recommendUserID = Integer.parseInt(this.getPara("recommendUserID"));
		String fromGroupID = this.getPara("fromGroupID");
		String eid = this.getPara("eid");
		String type = this.getPara("type");

		Event event;

		Group group = new Group().findById(fromGroupID);
		int fatherGroupID = group.get("gFatherGroup") == null ? 0
				: Integer.parseInt(group.get("gFatherGroup").toString());

		if (fatherGroupID == 0) {
			jsonString = jsonData.getJson(1036, "未绑定大空间");
		} else {
			int gcreator = Integer.parseInt(group.get("gcreator").toString());
			if (recommendUserID == gcreator) {
				switch (type) {
				case "recommend":
					event = new Event().findById(eid).set("isRecommend", 1).set("eRecommendGroupID", fatherGroupID)
							.set("eRecommendUserID", recommendUserID);
					jsonString = dataProcess.updateFlagResult(event.update());
					break;
				case "cancel":
					event = new Event().findById(eid).set("isRecommend", 0).set("eRecommendGroupID", null)
							.set("eRecommendUserID", null).set("isTopInRecommendGroup", 0);
					jsonString = dataProcess.updateFlagResult(event.update());
					break;
				default:
					jsonString = jsonData.getJson(6, "没有权限执行操作");
					break;
				}
			} else {
				jsonString = jsonData.getJson(2, "请求参数错误");
			}
		}

		renderText(jsonString);

	}

	/**
	 * 显示大空间内容，增加推荐内容，与普通空间区分
	 */
	public void ShowBigSpaceContent() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String minID = this.getPara("minID");
		jsonString = service.getGroupContent(userid, groupid, minID);
		// 返回结果
		renderText(jsonString);
	}

	/**
	 * 设置发布权限
	 */
	@Before({Tx.class,CrossDomain.class})
	public void SetUploadAuthority() {
		String groupid = this.getPara("groupid");
		String userid = this.getPara("userid");
		String authorityType = this.getPara("authorityType");// 三个值:all,onlyCreator,part
		String type = this.getPara("type");

		Group group = new Group().findById(groupid);
		switch (authorityType) {
		case "all":
			group.set("gAuthority", 0);
			jsonString = dataProcess.updateFlagResult(group.update());
			break;
		case "onlyCreator":
			group.set("gAuthority", 1);
			jsonString = dataProcess.updateFlagResult(group.update());
			break;
		case "part":
			group.set("gAuthority", 2);
			boolean groupFlag = group.update();
			int size = userid.split(",").length;
			int count = 0;// 更新数据库计数
			if (type.equals("add")) {
				// 添加权限，1表示有权限
				count = Db.update("update groupmembers set gmauthority=1 where gmuserid in (" + userid
						+ ") and gmgroupid=" + groupid + "  ");
			} else {
				// 取消权限，0表示无权限，默认为0
				count = Db.update("update groupmembers set gmauthority=0 where gmuserid in (" + userid
						+ ") and gmgroupid=" + groupid + "  ");
			}
			boolean groupmemberFlag = (count == size);
			jsonString = dataProcess.updateFlagResult(groupFlag && groupmemberFlag);
			break;
		}
		renderText(jsonString);
	}

	/**
	 * 搜索空间成员
	 */
	public void SearchSpaceMembers() {
		String groupid = this.getPara("groupid");
		String name = this.getPara("name");

		List<Record> list = Db
				.find("select userid,unickname,upic from users,groupmembers where gmuserid=userid and gmgroupid="
						+ groupid + " and unickname like '%" + name + "%' ");

		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);

	}

	/**
	 * 查看空间成员权限列表
	 */
	public void GetSpaceMemberAuthorityList() {
		String groupid = this.getPara("groupid");
		String type = this.getPara("type");// 两个值，大于100人与小于100人时返回不同的数据

		String sql = "";
		switch (type) {
		case "bigger":
			// 只显示授权者信息
			sql = "select userid,unickname,upic,gmauthority from users,groupmembers where userid=gmuserid and gmgroupid="
					+ groupid + " and gmauthority=1 ";
			break;
		case "smaller":
			// 显示空间成员所有授权信息
			sql = "select userid,unickname,upic,gmauthority from users,groupmembers where userid=gmuserid and gmgroupid="
					+ groupid + " ";
			break;
		}
		List<Record> list = Db.find(sql);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 空间成员设置是否接受动态推送
	 */
	public void SpaceMemberSetIsPush() {
		String groupid = this.getPara("groupid");
		String userid = this.getPara("userid");
		String isPush = this.getPara("isPush");

		int count = 0;
		if (isPush.equals("true")) {
			count = Db.update(
					"update groupmembers set gmIsPush=1 where gmgroupid=" + groupid + " and gmuserid=" + userid + " ");
		} else {
			count = Db.update(
					"update groupmembers set gmIsPush=0 where gmgroupid=" + groupid + " and gmuserid=" + userid + " ");
		}
		jsonString = dataProcess.updateFlagResult(count == 1);
		renderText(jsonString);
	}

	/**
	 * 获取同步空间列表——排除无上传权限空间
	 */
	@Before(CrossDomain.class)
	public void GetSynchronizeSpaceList() {
		String userid = this.getPara("userid");
		String eid = this.getPara("eid");
		if(eid!=null&&!eid.equals("")&&!eid.equals("undefined")) {
			if(userid!=null&&!userid.equals("")){
				Event event = new Event().findById(eid);
				String groupid = event.get("egroupid").toString();
				List<Record> list = Db.find(
						"select isDefault,openGId,groupid,gname,gpic from groups,groupmembers where groupid=gmgroupid and gstatus=0 and gmstatus=0 and gmuserid="
								+ userid +" and groupid!="+groupid+ " and gtype!=5 and (gcreator=" + userid + " or (gcreator!=" + userid
								+ " and (gAuthority=0 or (gAuthority=2 and gmauthority=1) )))");
				jsonString = jsonData.getSuccessJson(list);
			}else{
				jsonString = jsonData.getJson(2, "请求参数错误");
			}
		}else {
			if(userid!=null&&!userid.equals("")){
				List<Record> list = Db.find(
						"select isDefault,openGId,groupid,gname,gpic from groups,groupmembers where groupid=gmgroupid and gstatus=0 and gmstatus=0 and gmuserid="
								+ userid + " and gtype!=5 and (gcreator=" + userid + " or (gcreator!=" + userid
								+ " and (gAuthority=0 or (gAuthority=2 and gmauthority=1) )))");
				jsonString = jsonData.getSuccessJson(list);
			}else{
				jsonString = jsonData.getJson(2, "请求参数错误");
			}
		}
	
		renderText(jsonString);
	}

	/**
	 * by lk 读取活动菜单
	 */
	public void GetActivityMenu() {
		String returnValue = jsonData.getJson(2, "请求参数错误");
		int groupid = Integer.parseInt(
				null != this.getPara("groupid") && !this.getPara("groupid").equals("") ? this.getPara("groupid") : "0");
		if (groupid == 0) {
			renderText(returnValue);
			return;
		}
		Group group = new Group().findById(groupid);

		// 获取照片数，缓存
		List<Record> photo = CacheKit.get("ConcurrencyCache", groupid + "Photo");
		if (photo == null) {
			photo = Db.find(
					"select count(*) as gpicNum from groups,events,pictures where peid=eid and groupid=egroupid and groupid="
							+ groupid + " and estatus in(0,3) and pstatus=0 ");
			CacheKit.put("ConcurrencyCache", groupid + "Photo", photo);
		}
		String content = UrlUtils.loadJson(activityMenuUrl);
		// List<Record> likelist = new Event().GetListByGroup(groupid, 1);// 获取点赞第一名
		// List<Record> publishlist = new Event().GetUsePublishPhotoCont(groupid, 0,
		// false, 1);// 获取照片达人第一名

		// String returnValue=jsonData.getJson(2, "请求参数错误");
		Record r = new Record();
		if (content != null && content.length() > 0) {
			// returnValue=jsonData.getJson(0, "success", JSONArray.fromObject(content));
			r.set("menu", JSONArray.fromObject(content));
		}
		List<Record> newReturnList = new ArrayList<Record>();

		// r.set("like", likelist);
		// r.set("publish", publishlist);
		r.set("memberCnt", group.get("gnum").toString());
		r.set("photoCnt", photo.get(0).get("gpicNum").toString());
		r.set("gpic", group.get("gpic").toString());
		r.set("gname", group.get("gname").toString());
		newReturnList.add(r);
		returnValue = jsonData.getSuccessJson(newReturnList);
		renderText(returnValue);
	}

	/**
	 * 点赞排行
	 */

	public void GetGroupLikeList() {
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
		List<Record> list = CacheKit.get("ConcurrencyCache", groupid + "GroupLikeList");
		if (list == null) {
			list = new SpaceService().GetListByElikeAndGroup(groupid, uid, searchLimit);
			CacheKit.put("ConcurrencyCache", groupid + "GroupLikeList", list);
		}
		//List<Record> list = new SpaceService().GetListByElikeAndGroup(groupid, uid, searchLimit);
		// List<Record> list=new Event().GetListByElikeAndGroup(groupid,eid);
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}
//	/**
//	 * 照片达人
//	 */
//	public void GetPublishList_lktest() {	
//		System.out.println("照片达人开始："+System.currentTimeMillis());
//		if(this.getPara("groupid")==null||this.getPara("groupid").equals("")||this.getPara("uid")==null||this.getPara("uid").equals("")){
//			jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
//			renderText(jsonString);
//			return;
//		}
//		int groupid = Integer.parseInt(this.getPara("groupid"));
//		int uid = Integer.parseInt(this.getPara("uid"));
//		if(groupid==CommonParam.pGroupId||groupid==CommonParam.pGroupId2){
//			List<Record> photo =new SpaceService().GetGroupAllUserPublishList_lk(uid, groupid, 100) ;
//				
//			String returnValue = jsonData.getJson(0, "success",photo
//					);
//			renderText(returnValue);
//			System.out.println("照片达人结束："+System.currentTimeMillis());
//			return;
//		}		
//		int searchLimit = Integer
//				.parseInt(this.getPara("searchLimit") != null && !this.getPara("searchLimit").equals("")
//						? this.getPara("searchLimit")
//						: "100");
//		String returnValue = jsonData.getJson(0, "success",
//				new SpaceService().GetPublishList(uid, groupid, searchLimit));
//		renderText(returnValue);
//		// 缩略图 QiniuOperate a = new QiniuOperate();
//		// String url = a.getDownloadToken(url+"?imageView2/2/w/200");
//	}
	/**
	 * 照片达人
	 */
	public void GetPublishList() {		
		System.out.println("照片达人开始："+System.currentTimeMillis());
		if(this.getPara("groupid")==null||this.getPara("groupid").equals("")||this.getPara("uid")==null||this.getPara("uid").equals("")){
			jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		int groupid = Integer.parseInt(this.getPara("groupid"));
		int uid = Integer.parseInt(this.getPara("uid"));
		if(groupid==CommonParam.pGroupId||groupid==CommonParam.pGroupId2||groupid==CommonParam.pGroupId3){
			List<Record> photo = new SpaceService().GetGroupAllUserPublishList_2(uid, groupid, 100) ;				
			String returnValue = jsonData.getJson(0, "success",photo
					);
			renderText(returnValue);
			System.out.println("照片达人结束："+System.currentTimeMillis());
			return;
		}
		if(groupid==CommonParam.pGroupId6){
			//List<Record> photo = new SpaceService().GetGroupAllUserPublishList_3(uid, groupid, 100) ;
			List<Record> photo = new SpaceService().GetGroupAllUserPublishList_2(uid, groupid, 100) ;	
			String returnValue = jsonData.getJson(0, "success",photo
					);
			renderText(returnValue);
			System.out.println("照片达人结束："+System.currentTimeMillis());
			return;
		}
		int searchLimit = Integer
				.parseInt(this.getPara("searchLimit") != null && !this.getPara("searchLimit").equals("")
						? this.getPara("searchLimit")
						: "100");
		String returnValue = jsonData.getJson(0, "success",
				new SpaceService().GetPublishList(uid, groupid, searchLimit));
		renderText(returnValue);
		// 缩略图 QiniuOperate a = new QiniuOperate();
		// String url = a.getDownloadToken(url+"?imageView2/2/w/200");
	}

	/**
	 * 显示成员列表 by lk 添加分页
	 */
	@Before(CrossDomain.class)
	public void ShowGroupMember() {
		// 获取参数
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String source = this.getPara("source");
		int lastUid = Integer.parseInt(
				this.getPara("lastUid") != null && !this.getPara("lastUid").equals("") ? this.getPara("lastUid") : "0");
		if (source != null && !source.equals("")) {
			groupid = dataProcess.decryptData(groupid, "groupid");
		}
		jsonString = new SpaceService().getMemberList(userid, groupid, lastUid);
		// 返回结果
		renderText(jsonString);
	}
	
	/**
	 * 显示成员列表 by ly 添加分页 管理员置顶
	 */
	@Before({ Tx.class, CrossDomain.class })
	public void ShowGroupMemberTop() {
		// 获取参数
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		String source = this.getPara("source");
		int pagenum = Integer.parseInt(this.getPara("pagenum"));
		if (source != null && !source.equals("")) {
			groupid = dataProcess.decryptData(groupid, "groupid");
		}
		
/*		List<Record> list = Db.find("select gcreator from groups where groupid="+groupid);
		Long gcreator = list.get(0).getLong("gcreator");
		System.out.println("gcreator="+gcreator);
		Long uid = Long.parseLong(userid);
		System.out.println("uid="+uid);
		if(uid.equals(gcreator)) {
			List<Record> list2 = Db.find("select * from groupmembers where gmuserid="+gcreator+" and gmgroupid="+groupid);
			Long gmid = list2.get(0).getLong("gmid");
			GroupMember member = new GroupMember().findById(gmid);
			member.set("isAdmin", 1);
			member.update();
		}*/
		jsonString = new SpaceService().getMemberTopList(userid, groupid, pagenum);
		// 返回结果
		renderText(jsonString);
	}
	

	/**
	 * 空间成员总人数
	 */
	public void MembersNum() {
		String groupid = this.getPara("groupid");
		List<Record> list = Db.find("select gnum from groups where groupid="+groupid);
		List<Record> gmlist = Db.find("SELECT gmuserid from groupmembers WHERE gmgroupid="+groupid+" AND isAdmin=1");
		if(null!=gmlist&&gmlist.size()!=0&&null!=list&&list.size()!=0) {
			Long gnum = list.get(0).getLong("gnum");
			Long userid = gmlist.get(0).getLong("gmuserid");
			Record record = new Record();
			record.set("gnum", gnum).set("userid", userid);
			List<Record> result = new ArrayList<>();
			result.add(record);
			jsonString = jsonData.getSuccessJson(result);			
		}else {
			jsonString = jsonData.getJson(-50,"参数错误");
		}
		renderText(jsonString);
		
	}
//	public void MembersNum() {
//		String groupid = this.getPara("groupid");
//		List<Record> list = Db.find("select gnum from groups where groupid="+groupid);
//		Long gnum = list.get(0).getLong("gnum");
//		List<Record> gmlist = Db.find("SELECT gmuserid from groupmembers WHERE gmgroupid="+groupid+" AND isAdmin=1");
//		Long userid = gmlist.get(0).getLong("gmuserid");
//		Record record = new Record();
//		record.set("gnum", gnum).set("userid", userid);
//		List<Record> result = new ArrayList<>();
//		result.add(record);
//		jsonString = jsonData.getSuccessJson(result);
//		renderText(jsonString);
//	}
	/**
	 * 搜索空间成员 by lk 修改返回数据内容
	 */
	public void SearchSpaceMembersReturnFormat() {
		String groupid = this.getPara("groupid");
		String name = this.getPara("name");
		String sql = "select userid,unickname,upic,gmtime,gname from users,groups,groupmembers where groupid=gmgroupid and users.userid=groupmembers.gmuserid and gmgroupid='"
				+ groupid + "' and  unickname like '%" + name + "%' ";
		List<Record> list = Db.find(sql);
		List<Record> returnList = new ArrayList<Record>();
		for (Record r : list) {
			Record record = new Record();
			record.set("gmtime", list.get(0).get("gmtime"));
			record.set("gname", list.get(0).get("gname").toString());
			record.set("user", r);
			record.set("selected", false);
			returnList.add(record);
		}
		jsonString = jsonData.getSuccessJson(returnList);
		renderText(jsonString);

	}
	
	/**
	 * 搜索空间成员 by lk 修改返回数据内容
	 */
	public void SearchSpaceMembersNew() {
		String groupid = this.getPara("groupid");
		String name = this.getPara("name");
		String sql = "select userid,unickname,upic,gmtime,gname,isAdmin from users,groups,groupmembers where groupid=gmgroupid and users.userid=groupmembers.gmuserid and gmgroupid='"
				+ groupid + "' and  unickname like '%" + name + "%' ";
		List<Record> list = Db.find(sql);
		List<Record> returnList = new ArrayList<Record>();
		for (Record r : list) {
			Record record = new Record();
			record.set("gmtime", list.get(0).get("gmtime"));
			record.set("gname", list.get(0).get("gname").toString());
			record.set("user", r);
			record.set("selected", false);
			record.set("isAdmin", list.get(0).get("isAdmin"));
			returnList.add(record);
		}
		jsonString = jsonData.getSuccessJson(returnList);
		renderText(jsonString);

	}
	
	/**
	 * 转让管理员
	 */
	@Before(Tx.class)
	public void TransferAdministrator() {
		String groupid = this.getPara("groupid");
		String adminUserid = this.getPara("adminUserid");//管理员id
		String toAdminUserid = this.getPara("toAdminUserid");//被转让的用户id
		
		//管理员信息
		List<Record> adminList = Db.find("select * from groupmembers where gmuserid="+adminUserid+" and gmgroupid="+groupid);
		Long adminID = adminList.get(0).getLong("gmid");
		GroupMember admin = new GroupMember().findById(adminID);
		//修改状态，将管理员转成普通成员
		admin.set("isAdmin", 0);
		boolean adminFlag = admin.update();
		//被转让管理员信息
		List<Record> toAdminList = Db.find("select * from groupmembers where gmuserid="+toAdminUserid+" and gmgroupid="+groupid);
		Long toAdminID = toAdminList.get(0).getLong("gmid");
		GroupMember toAdmin = new GroupMember().findById(toAdminID);
		//修改管理员状态，将被转让者转让成管理员
		toAdmin.set("isAdmin", 1);
		//将相册创建者修改为新的管理员
		Group group = new Group().findById(groupid);
		group.set("gcreator", toAdminUserid);
		boolean groupFlag = group.update();
		boolean toAdminFlag = toAdmin.update();
		if(adminFlag && toAdminFlag &&groupFlag) {
			jsonString = jsonData.getJson(0,"转让成功");
		}else {
			jsonString = jsonData.getJson(-50,"转让失败");
		}
		renderText(jsonString);
	}
	
	/**
	 * 剔除群成员
	 */
	@Before(Tx.class)
	public void deleteMember() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		Boolean quitFlag = false;
		if(userid!=null&&!userid.equals("")
				&&groupid!=null&&!groupid.equals("")) {
			quitFlag = TxService.kickOutAlbum(userid, groupid);
		}
		
		if(quitFlag) {
			jsonString = jsonData.getJson(0,"删除成功");
		}else {
			jsonString = jsonData.getJson(-50,"删除失败");
		}
		renderText(jsonString);
		
	}
	
	/**
	 * 获取引导页
	 */
	public void GetGuideValue(){
		String content = UrlUtils.loadJson(guideUrl);
		//List<Record> likelist = new Event().GetListByGroup(groupid, 1);// 获取点赞第一名
		//List<Record> publishlist = new Event().GetUsePublishPhotoCont(groupid, 0, false, 1);// 获取照片达人第一名

		// String returnValue=jsonData.getJson(2, "请求参数错误");
		Record r = new Record();
		if (content != null && content.length() > 0) {
			// returnValue=jsonData.getJson(0, "success", JSONArray.fromObject(content));
			r.set("menu", JSONArray.fromObject(content));
		}
		List<Record> newReturnList = new ArrayList<Record>();
		newReturnList.add(r);
		jsonString = jsonData.getSuccessJson(newReturnList);
		renderText(jsonString);
	}
	
	
	/**
	 * 分享相册二维码
	 */
	public void sharePhotoQRCode(){
		QiniuOperate operate=new QiniuOperate();
		SmallAppQRCode small = new SmallAppQRCode();
		String groupid = this.getPara("groupid");
		String userid = this.getPara("userid");
		//查询相册内最新发布的动态id 如果为null时eid默认为0
		List<Record> eventsList = Db.find("select IFNULL(MAX(eid),0) as eid from events where egroupid=" + groupid + " and estatus=0" +" and eMain=0");
		String eventid=eventsList.get(0).getLong("eid").toString();
		User u = new User().findById(userid);
		String url = "";
		Group g = new Group();
		if(groupid!=null&&!groupid.equals("")) {
			g=new Group().findById(groupid);
			String gQRCode = g.get("gQRCode");
			if(gQRCode == null||gQRCode .equals("")) {
				String mpicUrl = "";
				String picUrl = "";
				Event e = new Event();
				if(!eventid.equals("0")) {
					e=new Event().findById(eventid);
					//获取相册最新动态的第一张照片作为二维码的背景图
					mpicUrl =operate.getDownloadToken(e.get("efirstpic")+"?imageMogr2/auto-orient/blur/1x1/thumbnail/1179x");
					//获取用户头像
					picUrl =operate.getDownloadToken(u.get("upic")+"?imageView2/1/w/126/h/126/");
				}else {
					//如果相册为null则采用默认相册封面为背景图
					mpicUrl =operate.getDownloadToken(g.get("gpic")+"?imageMogr2/auto-orient/blur/1x1/thumbnail/1179x");
					picUrl =operate.getDownloadToken(u.get("upic")+"?imageView2/1/w/126/h/126/");
				}
					
				String gname = g.get("gname");
				int numberCharacter = 0;
			    int enCharacter = 0;
				if(gname.length()>6) {
					gname = gname.substring(0, 6);
					gname = gname + "..";
					//统计相册名中的字母数字
					for (int i = 0; i < gname.length(); i++) {
			            char tmp = gname.charAt(i);
			            if ((tmp >= 'A' && tmp <= 'Z') || (tmp >= 'a' && tmp <= 'z')) {
			                enCharacter ++;
			            } else if ((tmp >= '0') && (tmp <= '9')) {
			                numberCharacter ++;
			            } 
			        }
					int count = numberCharacter + enCharacter;
					int mm = count/2;
					for(int x=0;x<mm;x++) {
						gname =" "+gname;
					}
				}else {
					
					int len = (6-gname.length())/2;
					int le = (6-gname.length())%2;
					for (int i = 0; i < gname.length(); i++) {
			            char tmp = gname.charAt(i);
			            if ((tmp >= 'A' && tmp <= 'Z') || (tmp >= 'a' && tmp <= 'z')) {
			                enCharacter ++;
			            } else if ((tmp >= '0') && (tmp <= '9')) {
			                numberCharacter ++;
			            } 
			        }
					int count = numberCharacter + enCharacter;
					int mm = count/2;
					if(le==0) {
						for(int x=0;x<=len;x++) {
							gname = "  "+gname;
						}
					}
					if(le==1) {
						for(int x=0;x<=len;x++) {
							gname = "  "+gname;
						}
						gname = " " + gname;
					}
						for(int x=0;x<mm;x++) {
							gname =" "+gname;
						}
				}
				url = small.GetShareSmallAppQRCodeURL("spaceQR", groupid,gname,picUrl,mpicUrl);
				if(url!=null&&url.indexOf("QRCodeError.png")!=-1) {
					
				}else{
					e.set("gQRCode", url);
					e.update();
				}
			} else {
				url = gQRCode;
			}
		}
		System.out.println(url);
		Record resultRecord = new Record();
		resultRecord.set("url", url);
		List<Record> resultList = new ArrayList<Record>();
		resultList.add(resultRecord);
		jsonString = jsonData.getSuccessJson(resultList);
		renderText(jsonString);
	}
	
}
