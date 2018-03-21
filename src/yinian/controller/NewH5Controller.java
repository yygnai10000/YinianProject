package yinian.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.bouncycastle.util.encoders.Base64;

import com.alibaba.fastjson.JSONArray;
import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.ActiveRecordException;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.ehcache.CacheKit;

import net.sf.json.JSONObject;
import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.interceptor.CrossDomain;
import yinian.model.FormID;
import yinian.model.Group;
import yinian.model.GroupCanPublish;
import yinian.model.GroupMember;
import yinian.model.PartnerRecord;
import yinian.model.Points;
import yinian.model.User;
import yinian.service.ActivityService;
import yinian.service.NewH5Service;
import yinian.service.PointsService;
import yinian.service.YinianService;
import yinian.utils.AES;
import yinian.utils.DES;
import yinian.utils.HttpUtils;
import yinian.utils.JsonData;
import yinian.utils.MD5;

public class NewH5Controller extends Controller{
	private String jsonString; // 返回的json字符串
	private JsonData jsonData = new JsonData(); // json操作类
	private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类
	/**
	 * 创建相册
	 */
	@Before(CrossDomain.class)
	public void CreateDefaultAlbum() {
		// 获取参数
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String groupName = this.getPara("groupName");
		String groupType = this.getPara("groupType");
		String url = this.getPara("url");
		String source = this.getPara("source");
		String formID = this.getPara("formID")==null?"":this.getPara("formID"); // 小程序推送表单ID
		String isDefault=this.getPara("isDefault")==null?"0":this.getPara("isDefault");//0为普通相册，1为群相册
		String groupNewType=this.getPara("groupNewType")==null||this.getPara("groupNewType").equals("")||this.getPara("groupNewType").equals("0")?"13":this.getPara("groupNewType");//0为默认
		//1.朋友 2聚会 3个人 4家人 5亲子 6出游 7团体 8兴趣 9校园 10公司 11情侣 12活动 13其它 14宠物
		//String openGId=this.getPara("openGId");//0为默认
		if(!userid.equals("")&&!formID.equals("")){
			FormID.insert(userid, formID);
		}
		if (url == null || url.equals("")) {
			// 根据组类别获取相应的群头像图片
			switch (groupNewType) {
			case "1":// 朋友
				List<Record> coverList = CacheKit.get("EternalCache", "spaceCover1");
				// 缓存为空，重新查询
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(11);
					CacheKit.put("EternalCache", "spaceCover1", coverList);
				}
				int size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "2":// 聚会
				coverList = CacheKit.get("EternalCache", "spaceCover2");
				// 缓存为空，重新查询
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(12);
					CacheKit.put("EternalCache", "spaceCover2", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "3":// 个人
				coverList = CacheKit.get("EternalCache", "spaceCover3");
				// 缓存为空，重新查询
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(13);
					CacheKit.put("EternalCache", "spaceCover3", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "4":// 家人  只有一张				
				url = CommonParam.qiniuOpenAddress+"20180104-4-1.jpg";
				break;
			case "5":// 亲子
				coverList = CacheKit.get("EternalCache", "spaceCover5");
				// 缓存为空，重新查询
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(15);
					CacheKit.put("EternalCache", "spaceCover5", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "6":// 出游
				coverList = CacheKit.get("EternalCache", "spaceCover6");
				// 缓存为空，重新查询
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(16);
					CacheKit.put("EternalCache", "spaceCover6", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "7":// 团体
				coverList = CacheKit.get("EternalCache", "spaceCover7");
				// 缓存为空，重新查询
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(17);
					CacheKit.put("EternalCache", "spaceCover7", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "8":// 兴趣
				coverList = CacheKit.get("EternalCache", "spaceCover8");
				// 缓存为空，重新查询
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(18);
					CacheKit.put("EternalCache", "spaceCover8", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "9":// 校园
				coverList = CacheKit.get("EternalCache", "spaceCover9");
				// 缓存为空，重新查询
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(19);
					CacheKit.put("EternalCache", "spaceCover9", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "10":// 公司
				coverList = CacheKit.get("EternalCache", "spaceCover10");
				// 缓存为空，重新查询
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(20);
					CacheKit.put("EternalCache", "spaceCover10", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "11":// 情侣
				coverList = CacheKit.get("EternalCache", "spaceCover11");
				// 缓存为空，重新查询
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(21);
					CacheKit.put("EternalCache", "spaceCover11", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "12":// 活动 只有一张
				url = CommonParam.qiniuOpenAddress+"20180104-12-1.jpg";
				break;
			case "13":// 其它
				coverList = CacheKit.get("EternalCache", "spaceCover13");
				// 缓存为空，重新查询
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(23);
					CacheKit.put("EternalCache", "spaceCover13", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			case "14":// 宠物
				coverList = CacheKit.get("EternalCache", "spaceCover14");
				// 缓存为空，重新查询
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(24);
					CacheKit.put("EternalCache", "spaceCover14", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			default:
				coverList = CacheKit.get("EternalCache", "spaceCover13");
				// 缓存为空，重新查询
				if (coverList == null) {
					coverList = Group.GetNewSpaceDefaultCoverList(23);
					CacheKit.put("EternalCache", "spaceCover13", coverList);
				}
				size = coverList.size();
				url = coverList.get(new Random().nextInt(size)).getStr("acurl");
				break;
			}
		} else {
			String temp = url.substring(0, 7);
			if (!(temp.equals("http://"))) {
				url = CommonParam.qiniuOpenAddress + url;
			}

		}
		String inviteCode = Group.CreateSpaceInviteCode();
		NewH5Service service=new NewH5Service();
		jsonString = service.createAlbum(groupName, userid, url, groupType, inviteCode, source,isDefault,groupNewType,null);

		// 返回结果
		renderText(jsonString);
	}
	/*
	 * 纸微合作，get请求
	 */
	public void postToZW(String zj,String userid,String uwechatid){
		//Long ts=System.currentTimeMillis();
		String url="https://java.zk2013.com/kedie2017/AdvApi/WxSmallProg/FreeUpCoin.do";
		String params="scene="+zj+"&thirdopenid="+uwechatid;
		String sign=MD5.getMD5(params+"&3ecb837979debb4b2b92a0a250ed8f8a").toUpperCase();
		//System.out.println("params=="+params);
		//System.out.println("url=="+params+"&Sign="+sign);
				//&Sign=9B9959C1C28DA87112A520B5588B6986";
		HttpUtils u=new HttpUtils();		
		savePartnerRecord(userid, zj, u.sendGet(url,params+"&Sign="+sign), "纸微");
		//{"status":-1,"status_str":"已发放奖励","api_data":[]}
	}
	/*
	 * 口袋商务合作，get请求
	 */
	public void postToKD(String zj,String userid){
		Long ts=System.currentTimeMillis();
		String url="http://www.pocketuniversity.cn/signin/Sxcard/qrNotive?qrcode="+zj;		
		
		HttpUtils u=new HttpUtils();
		u.sendGet(url,null);
		//savePartnerRecord(userid, zj, u.sendGet(url,null), "口袋");
		//{"status":-1,"status_str":"已发放奖励","api_data":[]}
	}
	/*
	 * 共享纸巾post-第一次合作完成，可能有后续，代码先保留
	 */
	public void postToZJ(String zj,String userid){
		Long ts=System.currentTimeMillis();
		String url="http://www.zhenhuaonline.cn/api/in/thirdparty/op_app/v1/handle?app_id=a_qLCogTZJjJbMU6&timestamp="+ts+"&sign=";		
		String param1 = getMD5("api=User.Tissue.Order.TicketOrdertimestamp="+ts+"tissue_ticket="+zj+"bjSjRNJmXvBE16faKxo3caLcFjGSZ5bV");
		url+=param1;
		param1="api=User.Tissue.Order.TicketOrder&timestamp="+ts+"&tissue_ticket="+zj;
		HttpUtils u=new HttpUtils();
		savePartnerRecord(userid, zj, u.sendPost(url, param1), "zho");
		//{"status":-1,"status_str":"已发放奖励","api_data":[]}
	}
	/*保存合作记录*/
	public void savePartnerRecord(String userid,String postValue,String getValue,String partner){
		PartnerRecord pr=new PartnerRecord();
		pr.set("userid", userid).set("postValue", postValue).set("getValue", getValue).set("partner", partner);
		pr.save();
	}
	 /** 
     * 生成md5 
     *  
     * @param message 
     * @return 
     */  
    public static String getMD5(String message) {  
    String md5str = "";  
    try {  
        // 1 创建一个提供信息摘要算法的对象，初始化为md5算法对象  
        MessageDigest md = MessageDigest.getInstance("MD5");  
  
        // 2 将消息变成byte数组  
        byte[] input = message.getBytes();  
  
        // 3 计算后获得字节数组,这就是那128位了  
        byte[] buff = md.digest(input);  
  
        // 4 把数组每一字节（一个字节占八位）换成16进制连成md5字符串  
        md5str = bytesToHex(buff);
        md5str = md5str.toLowerCase();
        System.out.println(md5str);
  
    } catch (Exception e) {  
        e.printStackTrace();  
    }  
    return md5str;  
    }  
    /** 
     * 二进制转十六进制 
     *  
     * @param bytes 
     * @return 
     */  
    public static String bytesToHex(byte[] bytes) {  
    StringBuffer md5str = new StringBuffer();  
    // 把数组每一字节换成16进制连成md5字符串  
    int digital;  
    for (int i = 0; i < bytes.length; i++) {  
        digital = bytes[i];  
  
        if (digital < 0) {  
        digital += 256;  
        }  
        if (digital < 16) {  
        md5str.append("0");  
        }  
        md5str.append(Integer.toHexString(digital));  
    }  
    return md5str.toString().toUpperCase();  
    } 
	/**
	 * 显示小程序相册信息
	 */
    //显示小程序相册信息--加密
    @Before(CrossDomain.class)
	public void ShowSmallAppAlbumInformationWithEncryption() {
		String uid=this.getPara("userid");
		String gid=this.getPara("groupid");
		String zj=this.getPara("zj");
		String inviteuserid=this.getPara("inviteuserid");
		if(uid==null||gid==null||gid.equals("undefined")||gid.equals("NaN")){
			jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		//开始解密
		boolean canDES=false;
		try{			
			uid=URLDecoder.decode(uid);
			uid=DES.decryptDES(uid, CommonParam.DESSecretKey);
			gid=URLDecoder.decode(gid);
			gid=DES.decryptDES(gid, CommonParam.DESSecretKey);
			canDES=true;
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(!canDES){
				jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
				renderText(jsonString);
				return;
			}
			//System.out.println(a1);
		}
		//解密end
		User inviteuser=new User();
		if(null!=inviteuserid&&!inviteuserid.equals("0")&&!inviteuserid.equals("undefined")&&!inviteuserid.equals("NaN")&&!inviteuserid.equals("")){
			inviteuser=new User().findById(inviteuserid);
		}
		//黑名单
		User u=new User().findById(uid);
		int inBlackList=1;
		if(null!=u&&null!=u.get("ustate")&&u.get("ustate").toString().equals("1")){
			inBlackList=0;
		}
		//黑名单
		// 基本参数
		int userid = Integer.parseInt(uid);
		int groupid = Integer.parseInt(gid);
		// 用户来源参数
		String port = this.getPara("port");
		String fromUserID = this.getPara("fromUserID");
		
		Group group = new Group().findById(groupid);
		int eventQRCodeCanPublish=new GroupCanPublish().getGroupPublishByType(groupid+"", "1");
		//dialogShow=1 显示活动相册助手
		int dialogShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "2");
		//showAdvertisements=1 显示广告位
		//int advertisementsShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "3");
		/*
		 * 1000W照片活动
		 */
		int advertisementsShow=0;
		if(CommonParam.pOpenBanner){
			
			advertisementsShow=1;
			if(groupid==CommonParam.pGroupId7){
				YinianService service1 = new YinianService();
				service1.SetGroupIsTop(userid+"", CommonParam.pGroupId7+"", "yes");
//				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//				String time = sdf.format(new Date());
//				Db.update("update groupmembers set isTop=1 and topTime='"+time+"' where gmuserid="+userid);
				advertisementsShow=0;
			}
		}else{
			advertisementsShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "3");
		}
		/*
		 * 1000W照片活动 end
		 */
		int status = Integer.parseInt(group.get("gstatus").toString());
		int gtype = Integer.parseInt(group.get("gtype").toString());
		int gAuthority = Integer.parseInt(group.get("gAuthority").toString());
		Record record = new Record();
		String upic = u.get("upic").toString();//用户头像
		record.set("upic", upic);
		//口袋商务
		if(null!=zj&&!zj.toString().equals("")&&zj.length()<4&&groupid==5193566){
			postToKD(zj,userid+"");
		}
		//纸微合作
		if(null!=zj&&!zj.toString().equals("")&&groupid==5275402){
			postToZW(zj,userid+"",u.get("uwechatid"));
		}
		//zho
		if(null!=zj&&!zj.toString().equals("")&&groupid==5298806){
			postToZJ(zj,userid+"");
		}
		//友宝
		if(null!=zj&&!zj.toString().equals("")&&groupid==5451186){
			postToYB(zj,u.get("uwechatid").toString());
		}
		// 判断相册是否删除
		if (status == 0) {
			// 判断是否在相册中
			GroupMember gm = new GroupMember();
			boolean isInFlag = gm.judgeUserIsInTheAlbum(userid, groupid); // true时用户不在空间内

			boolean flag = true;
			boolean getNewGnum=false;
			int count = 1;
			record.set("joinStatus", 1);
			if (isInFlag) {	
				
				// 不在相册，则插入用户数据
				gm = new GroupMember().set("gmgroupid", groupid).set("gmuserid", userid).set("gmPort", port)
						.set("gmFromUserID", fromUserID);

				// 捕获插入异常，用户重复点击时会导致插入失败
				try {
					flag = gm.save();
					/*
					 * 共享纸巾(5166970==口袋校园）
					 */
//					if(null!=zj&&!zj.toString().equals("")&&groupid==5192130){
//						postToZJ(zj,userid+"");
//					}
					
					// 更新分组表中组成员数量字段
					count = Db.update("update groups set gnum = gnum+1 where groupid='" + groupid + "' ");
					//加入活动相册 begin
					//获取活动相册列表	
					//List<Record> allActivitiGroupList =new ArrayList<Record>();
					String actGroupids = CacheKit.get("DataSystem", "allActivitiGroupIds");
					if(actGroupids == null) {
						//缓存为空  从数据库查询						
						List<Record> actGroupList = Db.find("select activitiGroupid from activitigroups");
						if(null!=actGroupList&&!actGroupList.isEmpty()){
							StringBuffer ids=new StringBuffer();
							for(int i=0;i<actGroupList.size();i++){
								ids.append(actGroupList.get(i).get("activitiGroupid").toString()).append(",");
							}
							if(ids.length()>0){
								actGroupids=ids.substring(0, ids.length()-1);
								CacheKit.put("DataSystem", "ArrayList",actGroupids);			
							}		
						}						
					}
					if(Arrays.asList(actGroupids.split(",")).contains(groupid+"")){
						List uActList=Db.find("select joinUserid from activitimembers where joinGroupid="+groupid+" and joinUserid="+userid);
						if(uActList==null||uActList.isEmpty()){
							Db.update("insert into activitimembers (joinUserid,joinGroupid) values ("+userid+","+groupid+")");
						}
					}
					//加入活动相册 end
					count = Db.update("update groups set gnum = gnum+1 where groupid='" + groupid + "' ");
					record.set("joinStatus", 0);
					getNewGnum=true;
				} catch (ActiveRecordException e) {
					flag = true;
					count = 1;

				}

			}

			if (flag && (count == 1)) {
				PointsService pointsService = new PointsService();
				int points = pointsService.getUseablePoints(userid+"");
				// 获取返回数据 ,gAuthority————0所有人 1只有创建者 2-部分
				record.set("gname", group.get("gname").toString()).set("gcreator", group.get("gcreator").toString())
						.set("gtype", gtype).set("gnum", group.get("gnum").toString())
						.set("gpic", group.get("gpic").toString()).set("gAuthority", gAuthority)
						.set("gOrigin", group.get("gOrigin").toString())
						.set("eventQRCodeCanPublish", eventQRCodeCanPublish)
						.set("dialogShow", dialogShow).set("inBlackList", inBlackList).set("advertisementsShow", advertisementsShow)
						.set("isDefault", group.get("isDefault")).set("groupNewType", group.get("groupNewType"))
						.set("openGId", group.get("openGId"))
						.set("points", points)//points 积分;
						.set("inviteusername", inviteuser.get("unickname"));//邀请人姓名
				if(getNewGnum){
					record.set("gnum", group.getLong("gnum")+1);
				}
				//活动规则 begin
				record.set("arTitle", "");
				record.set("arValue", new String[0]);
				if(null!=group.get("gOrigin").toString()&&!group.get("gOrigin").toString().equals("")&&group.get("gOrigin").toString().equals("1")){
										//String re="1、我曹;2、我靠;3、我日;3、我xx;4、我00";
					List<Record> ruleList=Db.find("select arTitle,arValue from activitygrouprule where arGroupid="+group.get("groupid").toString());
					if(null!=ruleList&&!ruleList.isEmpty()){
						record.set("arTitle", ruleList.get(0).get("arTitle"));
						record.set("arValue", ruleList.get(0).get("arValue").toString().split(";"));
					}
					
				}
				//活动规则 end
				// 获取推送接收状态
				if (isInFlag) {
					// 不在空间内的用户直接返回0
					record.set("isPush", "0");
				} else {
					// 在空间内的用户去查询
					List<Record> push = Db.find("select gmIsPush from groupmembers where gmgroupid=" + groupid
							+ " and gmuserid=" + userid + "");
					record.set("isPush", push.get(0).get("gmIsPush").toString());
				}

				// 获取成员列表，缓存
				List<Record> groupMember = CacheKit.get("ConcurrencyCache", groupid + "Member");
				if (groupMember == null) {
					groupMember = Db.find(
							"select userid,unickname,upic,gmtime from users,groups,groupmembers where groupid=gmgroupid and users.userid=groupmembers.gmuserid and gmgroupid='"
									+ groupid + "' and gmstatus=0 order by gmid desc limit 3 ");
					CacheKit.put("ConcurrencyCache", groupid + "Member", groupMember);
				}

				// 获取照片数，缓存
				List<Record> photo = CacheKit.get("ConcurrencyCache", groupid + "Photo");
				if (photo == null) {
					photo = Db.find(
							"select count(*) as gpicNum from groups,events,pictures where peid=eid and groupid=egroupid and groupid="
									+ groupid + " and estatus in(0,3) and pstatus=0 ");
					CacheKit.put("ConcurrencyCache", groupid + "Photo", photo);
				}

				// 获取发布权限列表，缓存，当空间发布权限为部分人时才查询并返回
				if (gAuthority == 2) {
					List<Record> uploadAuthority = CacheKit.get("ConcurrencyCache", groupid + "Authority");
					if (uploadAuthority == null) {
						uploadAuthority = Db.find("select gmuserid as userid from groupmembers where gmgroupid="
								+ groupid + " and gmauthority=1 ");
						CacheKit.put("ConcurrencyCache", groupid + "Authority", uploadAuthority);
					}
					record.set("authorityList", uploadAuthority);
				}
				record.set("picNum", photo.get(0).get("gpicNum").toString()).set("memberList", groupMember);
				
				List<Record> result = new ArrayList<Record>();
				result.add(record);
				jsonString = jsonData.getSuccessJson(result);

			} else {
				jsonString = dataProcess.insertFlagResult(false);
			}

		} else if (status == 1) {
			jsonString = jsonData.getJson(1012, "相册已被删除");
		} else {
			jsonString = jsonData.getJson(1037, "相册已被封");
		}
		renderText(jsonString);
	}
	@Before(CrossDomain.class)
	public void ShowSmallAppAlbumInformation() {
		String uid=this.getPara("userid");
		String gid=this.getPara("groupid");
		String zj=this.getPara("zj");
		String inviteuserid=this.getPara("inviteuserid");
		if(uid==null||gid==null||gid.equals("undefined")||gid.equals("NaN")){
			jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		User inviteuser=new User();
		if(null!=inviteuserid&&!inviteuserid.equals("0")&&!inviteuserid.equals("undefined")&&!inviteuserid.equals("NaN")&&!inviteuserid.equals("")){
			inviteuser=new User().findById(inviteuserid);
		}
		//黑名单
		User u=new User().findById(uid);
		int inBlackList=1;
		if(null!=u&&null!=u.get("ustate")&&u.get("ustate").toString().equals("1")){
			inBlackList=0;
		}
		//黑名单
		// 基本参数
		int userid = this.getParaToInt("userid");
		int groupid = this.getParaToInt("groupid");
		// 用户来源参数
		String port = this.getPara("port");
		String fromUserID = this.getPara("fromUserID");
		
		Group group = new Group().findById(groupid);
		int eventQRCodeCanPublish=new GroupCanPublish().getGroupPublishByType(groupid+"", "1");
		//dialogShow=1 显示活动相册助手
		int dialogShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "2");
		//showAdvertisements=1 显示广告位
		//int advertisementsShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "3");
		/*
		 * 1000W照片活动
		 */
		int advertisementsShow=0;
		if(CommonParam.pOpenBanner){
			
			advertisementsShow=1;
			if(groupid==CommonParam.pGroupId7){
				YinianService service1 = new YinianService();
				service1.SetGroupIsTop(userid+"", CommonParam.pGroupId7+"", "yes");
//				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//				String time = sdf.format(new Date());
//				Db.update("update groupmembers set isTop=1 and topTime='"+time+"' where gmuserid="+userid);
				advertisementsShow=0;
			}
		}else{
			advertisementsShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "3");
		}
		/*
		 * 1000W照片活动 end
		 */
		int status = Integer.parseInt(group.get("gstatus").toString());
		int gtype = Integer.parseInt(group.get("gtype").toString());
		int gAuthority = Integer.parseInt(group.get("gAuthority").toString());
		Record record = new Record();
		String upic = u.get("upic").toString();//用户头像
		record.set("upic", upic);
		//口袋商务
		if(null!=zj&&!zj.toString().equals("")&&zj.length()<4&&groupid==5193566){
			postToKD(zj,userid+"");
		}
		//纸微合作
		if(null!=zj&&!zj.toString().equals("")&&groupid==5275402){
			postToZW(zj,userid+"",u.get("uwechatid"));
		}
		//zho
		if(null!=zj&&!zj.toString().equals("")&&groupid==5298806){
			postToZJ(zj,userid+"");
		}
		//友宝
		if(null!=zj&&!zj.toString().equals("")&&groupid==5451186){
			postToYB(zj,u.get("uwechatid").toString());
		}
		// 判断相册是否删除
		if (status == 0) {
			// 判断是否在相册中
			GroupMember gm = new GroupMember();
			boolean isInFlag = gm.judgeUserIsInTheAlbum(userid, groupid); // true时用户不在空间内

			boolean flag = true;
			boolean getNewGnum=false;
			int count = 1;
			record.set("joinStatus", 1);
			if (isInFlag) {	
				
				// 不在相册，则插入用户数据
				gm = new GroupMember().set("gmgroupid", groupid).set("gmuserid", userid).set("gmPort", port)
						.set("gmFromUserID", fromUserID);

				// 捕获插入异常，用户重复点击时会导致插入失败
				try {
					flag = gm.save();
					/*
					 * 共享纸巾(5166970==口袋校园）
					 */
//					if(null!=zj&&!zj.toString().equals("")&&groupid==5192130){
//						postToZJ(zj,userid+"");
//					}
					//加入活动相册 begin
					//获取活动相册列表	
					//List<Record> allActivitiGroupList =new ArrayList<Record>();
					String actGroupids = CacheKit.get("DataSystem", "allActivitiGroupIds");
					if(actGroupids == null) {
						//缓存为空  从数据库查询						
						List<Record> actGroupList = Db.find("select activitiGroupid from activitigroups");
						if(null!=actGroupList&&!actGroupList.isEmpty()){
							StringBuffer ids=new StringBuffer();
							for(int i=0;i<actGroupList.size();i++){
								ids.append(actGroupList.get(i).get("activitiGroupid").toString()).append(",");
							}
							if(ids.length()>0){
								actGroupids=ids.substring(0, ids.length()-1);
								CacheKit.put("DataSystem", "ArrayList",actGroupids);			
							}		
						}						
					}
					if(Arrays.asList(actGroupids.split(",")).contains(groupid+"")){
						List uActList=Db.find("select joinUserid from activitimembers where joinGroupid="+groupid+" and joinUserid="+userid);
						if(uActList==null||uActList.isEmpty()){
							Db.update("insert into activitimembers (joinUserid,joinGroupid) values ("+userid+","+groupid+")");
						}
					}
					//加入活动相册 end
					// 更新分组表中组成员数量字段
					count = Db.update("update groups set gnum = gnum+1 where groupid='" + groupid + "' ");
					record.set("joinStatus", 0);
					getNewGnum=true;
				} catch (ActiveRecordException e) {
					flag = true;
					count = 1;

				}

			}

			if (flag && (count == 1)) {
				PointsService pointsService = new PointsService();
				int points = pointsService.getUseablePoints(userid+"");
				// 获取返回数据 ,gAuthority————0所有人 1只有创建者 2-部分
				record.set("gname", group.get("gname").toString()).set("gcreator", group.get("gcreator").toString())
						.set("gtype", gtype).set("gnum", group.get("gnum").toString())
						.set("gpic", group.get("gpic").toString()).set("gAuthority", gAuthority)
						.set("gOrigin", group.get("gOrigin").toString())
						.set("eventQRCodeCanPublish", eventQRCodeCanPublish)
						.set("dialogShow", dialogShow).set("inBlackList", inBlackList).set("advertisementsShow", advertisementsShow)
						.set("isDefault", group.get("isDefault")).set("groupNewType", group.get("groupNewType"))
						.set("openGId", group.get("openGId"))
						.set("points", points)//points 积分;
						.set("inviteusername", inviteuser.get("unickname"));//邀请人姓名
				if(getNewGnum){
					record.set("gnum", group.getLong("gnum")+1);
				}
				//活动规则 begin
				record.set("arTitle", "");
				record.set("arValue", new String[0]);
				if(null!=group.get("gOrigin").toString()&&!group.get("gOrigin").toString().equals("")&&group.get("gOrigin").toString().equals("1")){
										//String re="1、我曹;2、我靠;3、我日;3、我xx;4、我00";
					List<Record> ruleList=Db.find("select arTitle,arValue from activitygrouprule where arGroupid="+group.get("groupid").toString());
					if(null!=ruleList&&!ruleList.isEmpty()){
						record.set("arTitle", ruleList.get(0).get("arTitle"));
						record.set("arValue", ruleList.get(0).get("arValue").toString().split(";"));
					}
					
				}
				//活动规则 end
				// 获取推送接收状态
				if (isInFlag) {
					// 不在空间内的用户直接返回0
					record.set("isPush", "0");
				} else {
					// 在空间内的用户去查询
					List<Record> push = Db.find("select gmIsPush from groupmembers where gmgroupid=" + groupid
							+ " and gmuserid=" + userid + "");
					record.set("isPush", push.get(0).get("gmIsPush").toString());
				}

				// 获取成员列表，缓存
				List<Record> groupMember = CacheKit.get("ConcurrencyCache", groupid + "Member");
				if (groupMember == null) {
					groupMember = Db.find(
							"select userid,unickname,upic,gmtime from users,groups,groupmembers where groupid=gmgroupid and users.userid=groupmembers.gmuserid and gmgroupid='"
									+ groupid + "' and gmstatus=0 order by gmid desc limit 3 ");
					CacheKit.put("ConcurrencyCache", groupid + "Member", groupMember);
				}

				// 获取照片数，缓存
				List<Record> photo = CacheKit.get("ConcurrencyCache", groupid + "Photo");
				if (photo == null) {
					photo = Db.find(
							"select count(*) as gpicNum from groups,events,pictures where peid=eid and groupid=egroupid and groupid="
									+ groupid + " and estatus in(0,3) and pstatus=0 ");
					CacheKit.put("ConcurrencyCache", groupid + "Photo", photo);
				}

				// 获取发布权限列表，缓存，当空间发布权限为部分人时才查询并返回
				if (gAuthority == 2) {
					List<Record> uploadAuthority = CacheKit.get("ConcurrencyCache", groupid + "Authority");
					if (uploadAuthority == null) {
						uploadAuthority = Db.find("select gmuserid as userid from groupmembers where gmgroupid="
								+ groupid + " and gmauthority=1 ");
						CacheKit.put("ConcurrencyCache", groupid + "Authority", uploadAuthority);
					}
					record.set("authorityList", uploadAuthority);
				}
				record.set("picNum", photo.get(0).get("gpicNum").toString()).set("memberList", groupMember);
				
				List<Record> result = new ArrayList<Record>();
				result.add(record);
				jsonString = jsonData.getSuccessJson(result);

			} else {
				jsonString = dataProcess.insertFlagResult(false);
			}

		} else if (status == 1) {
			jsonString = jsonData.getJson(1012, "相册已被删除");
		} else {
			jsonString = jsonData.getJson(1037, "相册已被封");
		}
		renderText(jsonString);
	}
	public void postToYB(String zj,String openid){
		Long ts=System.currentTimeMillis();
		String url="https://api.uboxsale.com/Miniapp/SendCouponYi/umid/"+zj+"/thirdid/"+openid;		
//		String param1 = getMD5("api=User.Tissue.Order.TicketOrdertimestamp="+ts+"tissue_ticket="+zj+"bjSjRNJmXvBE16faKxo3caLcFjGSZ5bV");
//		url+=param1;
//		param1="api=User.Tissue.Order.TicketOrder&timestamp="+ts+"&tissue_ticket="+zj;
		HttpUtils u=new HttpUtils();
		
		savePartnerRecord(0+"", zj+"   "+openid, u.sendPost(url, ""), "Miniapp");
		//{"status":-1,"status_str":"已发放奖励","api_data":[]}
	}
	/**
	 * 群相册-显示相册信息
	 */
	@Before(CrossDomain.class)
	public void ShowSmallAppFlockAlbumInformation() {
		String uid=this.getPara("userid");
		String gid=this.getPara("groupid");
		if(uid==null||gid==null||gid.equals("undefined")||gid.equals("NaN")){
			jsonString=jsonData.getJson(2, "参数错误",new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		//黑名单
//		User u=new User().findById(uid);
//		int inBlackList=1;
//		if(null!=u&&null!=u.get("ustate")&&u.get("ustate").toString().equals("1")){
//			inBlackList=0;
//		}
		//黑名单
		// 基本参数
		int userid = this.getParaToInt("userid");
		int groupid = this.getParaToInt("groupid");
		// 用户来源参数
		String port = this.getPara("port");
		String fromUserID = this.getPara("fromUserID");
		
		Group group = new Group().findById(groupid);
		User user=new User().findById(userid);
//		int eventQRCodeCanPublish=new GroupCanPublish().getGroupPublishByType(groupid+"", "1");
//		//dialogShow=1 显示活动相册助手
//		int dialogShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "2");
//		//showAdvertisements=1 显示广告位
//		int advertisementsShow=new GroupCanPublish().getGroupPublishByType(groupid+"", "3");
		int status = Integer.parseInt(group.get("gstatus").toString());
		int gtype = Integer.parseInt(group.get("gtype").toString());
		int gAuthority = Integer.parseInt(group.get("gAuthority").toString());
		// 判断相册是否删除
		if (status == 0) {
			// 判断是否在相册中
			GroupMember gm = new GroupMember();
			boolean isInFlag = gm.judgeUserIsInTheAlbum(userid, groupid); // true时用户不在空间内

			boolean flag = true;
			boolean getNewGnum=false;
			int count = 1;
			Record record = new Record().set("joinStatus", 1);
			if (isInFlag) {
				// 不在相册，则插入用户数据
				gm = new GroupMember().set("gmgroupid", groupid).set("gmuserid", userid).set("gmPort", port)
						.set("gmFromUserID", fromUserID);

				// 捕获插入异常，用户重复点击时会导致插入失败
				try {
					flag = gm.save();
					// 更新分组表中组成员数量字段
					//count = Db.update("update groups set gnum = gnum+1 where groupid='" + groupid + "' ");
					record.set("joinStatus", 0);
					getNewGnum=true;
				} catch (ActiveRecordException e) {
					flag = true;
					count = 1;

				}

			}

			if (flag && (count == 1)) {
				// 获取返回数据 ,gAuthority————0所有人 1只有创建者 2-部分
				PointsService pointsService = new PointsService();
				int points = pointsService.getUseablePoints(userid+"");
				//int points = pointsService.getUseablePoints(userid);
				record.set("gname", group.get("gname").toString()).set("gcreator", group.get("gcreator").toString())
						.set("gtype", gtype).set("gnum", group.get("gnum").toString())
						.set("gpic", group.get("gpic").toString()).set("gAuthority", gAuthority)
						.set("gOrigin", group.get("gOrigin").toString())
					//	.set("eventQRCodeCanPublish", eventQRCodeCanPublish)
					//	.set("dialogShow", dialogShow).set("inBlackList", inBlackList).set("advertisementsShow", advertisementsShow)
						.set("isDefault", group.get("isDefault")).set("groupNewType", group.get("groupNewType"))
						.set("openGId", group.get("openGId"))
						.set("points", points);//points 积分
				if(getNewGnum){
					record.set("gnum", group.getLong("gnum")+1);
				}
				if(null!=user){
					record.set("unickname", user.get("unickname").toString());
					record.set("upic", user.get("upic").toString());
				}
				// 获取推送接收状态
				if (isInFlag) {
					// 不在空间内的用户直接返回0
					record.set("isPush", "0");
				} else {
					// 在空间内的用户去查询
					List<Record> push = Db.find("select gmIsPush from groupmembers where gmgroupid=" + groupid
							+ " and gmuserid=" + userid + "");
					record.set("isPush", push.get(0).get("gmIsPush").toString());
				}

				// 获取成员列表，缓存
//				List<Record> groupMember = CacheKit.get("ConcurrencyCache", groupid + "Member");
//				if (groupMember == null) {
//					groupMember = Db.find(
//							"select userid,unickname,upic,gmtime from users,groups,groupmembers where groupid=gmgroupid and users.userid=groupmembers.gmuserid and gmgroupid='"
//									+ groupid + "' and gmstatus=0 order by gmid desc limit 3 ");
//					CacheKit.put("ConcurrencyCache", groupid + "Member", groupMember);
//				}
//
//				// 获取照片数，缓存
				List<Record> photo = CacheKit.get("ConcurrencyCache", groupid + "Photo");
				if (photo == null) {
					photo = Db.find(
							"select count(*) as gpicNum from groups,events,pictures where peid=eid and groupid=egroupid and groupid="
									+ groupid + " and estatus in(0,3) and pstatus=0 ");
					CacheKit.put("ConcurrencyCache", groupid + "Photo", photo);
				}

				// 获取发布权限列表，缓存，当空间发布权限为部分人时才查询并返回
//				if (gAuthority == 2) {
//					List<Record> uploadAuthority = CacheKit.get("ConcurrencyCache", groupid + "Authority");
//					if (uploadAuthority == null) {
//						uploadAuthority = Db.find("select gmuserid as userid from groupmembers where gmgroupid="
//								+ groupid + " and gmauthority=1 ");
//						CacheKit.put("ConcurrencyCache", groupid + "Authority", uploadAuthority);
//					}
//					record.set("authorityList", uploadAuthority);
//				}
				record.set("picNum", photo.get(0).get("gpicNum").toString());

				List<Record> result = new ArrayList<Record>();
				result.add(record);
				jsonString = jsonData.getSuccessJson(result);

			} else {
				jsonString = dataProcess.insertFlagResult(false);
			}

		} else if (status == 1) {
			jsonString = jsonData.getJson(1012, "相册已被删除");
		} else {
			jsonString = jsonData.getJson(1037, "相册已被封");
		}
		renderText(jsonString);
	}
	/*
	 * 群相册 -获取相册openId
	 */
	public void getFlockAlbumOpenId_new(){
		 YinianDataProcess dataProcess = new YinianDataProcess();
		 NewH5Service service=new NewH5Service();
		 String code=this.getPara("code")==null?"":this.getPara("code");
		// System.out.println("code="+code);
		 String encryptedData=this.getPara("encryptedData")==null?"":this.getPara("encryptedData");
		// System.out.println("encryptedData="+encryptedData);
		 String iv=this.getPara("iv")==null?"":this.getPara("iv");
		 String userid = this.getPara("userid")==null||this.getPara("userid").toString().equals("")?"816596":this.getPara("userid");
		// System.out.println("iv="+iv);
		 /*可能传递的字段*/
		 String groupType="4";
		 String groupNewType=this.getPara("groupNewType")==null||this.getPara("groupNewType").toString().equals("")||this.getPara("groupNewType").toString().equals("0")?"13":this.getPara("groupNewType");
		 String eid=this.getPara("eid")==null||this.getPara("eid").toString().equals("0")?"":this.getPara("eid");
		 AES aes = new AES();
		 List<Record> list = new ArrayList<Record>();
		 Record r=new Record();
		 String openGId="";
		 /*获取封面*/
		 String url= getUrlByNewType(groupNewType);
		 if(!code.equals("")&&!encryptedData.equals("")&&!iv.equals("")){
			 String result= dataProcess.sentNetworkRequest(CommonParam.getWechatSessionKeyUrl + code);
			 com.alibaba.fastjson.JSONObject jo = com.alibaba.fastjson.JSONObject.parseObject(result);
			 //System.out.println("jo="+jo);
			 byte[] resultByte;
			 String session_key = jo.getString("session_key");
				try {					
					resultByte = 
							//Pkcs7Encoder.decryptOfDiyIV(Base64.decode(encryptedData), Base64.decode(session_key), Base64.decode(iv));
							aes.decrypt_lk(Base64.decode(encryptedData), Base64.decode(session_key), Base64.decode(iv));
					// System.out.println("resultByte="+resultByte);
					if (null != resultByte && resultByte.length > 0) {
						String u = new String(resultByte, "UTF-8");
						com.alibaba.fastjson.JSONObject jou = com.alibaba.fastjson.JSONObject.parseObject(u);					
						//r.set("openGId", jou.get("openGId"));	
						openGId=jou.get("openGId").toString();
					}
					if(null==openGId||openGId.equals("")){
						jsonString = jsonData.getJson(2, "参数错误openGId为空", new ArrayList<Record>());
						renderText(jsonString);
						return;
					}
					if(service.canCreateFlockAlbum(openGId)){
						String inviteCode = Group.CreateSpaceInviteCode();
						jsonString = service.createFlockAlbum("群相册", userid, url, groupType, inviteCode, "小程序","1",groupNewType,openGId,eid);
						
					}else{
						//获取相册id			
						jsonString = jsonData.getSuccessJson(service.getFlockAlbumByOpenGId(openGId));
					}
					
					//list.add(r);
					
					// System.out.println("ivb="+ivb);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		 }else{
			 jsonString = jsonData.getJson(2, "参数错误", new ArrayList<Record>());			
		 }
		 renderText(jsonString);
	 }
	/*
	 * 群相册-分享热点图片创建相册
	 */
	/*
	 * 群相册 -获取相册openId
	 */
	public void getFlockAlbumOpenId_withHotPic(){
		 YinianDataProcess dataProcess = new YinianDataProcess();
		 NewH5Service service=new NewH5Service();
		 String code=this.getPara("code")==null?"":this.getPara("code");
		// System.out.println("code="+code);
		 String encryptedData=this.getPara("encryptedData")==null?"":this.getPara("encryptedData");
		// System.out.println("encryptedData="+encryptedData);
		 String iv=this.getPara("iv")==null?"":this.getPara("iv");
		 String userid = this.getPara("userid")==null||this.getPara("userid").toString().equals("")?"816596":this.getPara("userid");
		// System.out.println("iv="+iv);
		 /*可能传递的字段*/
		 String groupType="4";
		 String groupNewType=this.getPara("groupNewType")==null||this.getPara("groupNewType").toString().equals("")||this.getPara("groupNewType").toString().equals("0")?"13":this.getPara("groupNewType");
		 String hid=this.getPara("hid")==null||this.getPara("hid").toString().equals("0")?"":this.getPara("hid");		
		 AES aes = new AES();
		 List<Record> list = new ArrayList<Record>();
		 Record r=new Record();
		 String openGId="";
		 /*获取封面*/
		 String url= getUrlByNewType(groupNewType);
		 if(!code.equals("")&&!encryptedData.equals("")&&!iv.equals("")){
			 String result= dataProcess.sentNetworkRequest(CommonParam.getWechatSessionKeyUrl + code);
			 com.alibaba.fastjson.JSONObject jo = com.alibaba.fastjson.JSONObject.parseObject(result);
			 //System.out.println("jo="+jo);
			 byte[] resultByte;
			 String session_key = jo.getString("session_key");
				try {					
					resultByte = 
							//Pkcs7Encoder.decryptOfDiyIV(Base64.decode(encryptedData), Base64.decode(session_key), Base64.decode(iv));
							aes.decrypt_lk(Base64.decode(encryptedData), Base64.decode(session_key), Base64.decode(iv));
					// System.out.println("resultByte="+resultByte);
					if (null != resultByte && resultByte.length > 0) {
						String u = new String(resultByte, "UTF-8");
						com.alibaba.fastjson.JSONObject jou = com.alibaba.fastjson.JSONObject.parseObject(u);					
						//r.set("openGId", jou.get("openGId"));	
						openGId=jou.get("openGId").toString();
					}
					if(null==openGId||openGId.equals("")){
						jsonString = jsonData.getJson(2, "参数错误openGId为空", new ArrayList<Record>());
						renderText(jsonString);
						return;
					}
					if(service.canCreateFlockAlbum(openGId)){
						String inviteCode = Group.CreateSpaceInviteCode();
						jsonString = service.createFlockAlbumWithHotPic("群相册", userid, url, groupType, inviteCode, "小程序","1",groupNewType,openGId,hid);
						
					}else{
						//获取相册id	
						List<Record> groupidList=service.getFlockAlbumByOpenGId(openGId);
						if(null!=groupidList&&!groupidList.isEmpty()){
							String groupid=groupidList.get(0).getLong("groupid").toString();
							service.createHotPicEvent(groupid, userid, hid);
						}
						jsonString = jsonData.getSuccessJson(groupidList);
					}
					
					//list.add(r);
					
					// System.out.println("ivb="+ivb);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		 }else{
			 jsonString = jsonData.getJson(2, "参数错误", new ArrayList<Record>());			
		 }
		 renderText(jsonString);
	 }
	public void getFlockAlbumOpenId(){
		 YinianDataProcess dataProcess = new YinianDataProcess();
		 String code=this.getPara("code");
		 System.out.println("code="+code);
		 String encryptedData=this.getPara("encryptedData");
		 System.out.println("encryptedData="+encryptedData);
		 String iv=this.getPara("iv");
		 System.out.println("iv="+iv);
		 AES aes = new AES();
		 List<Record> list = new ArrayList<Record>();
		 Record r=new Record();
		 if(null!=encryptedData&&null!=iv){
			 String result= dataProcess.sentNetworkRequest(CommonParam.getWechatSessionKeyUrl + code);
			 com.alibaba.fastjson.JSONObject jo = com.alibaba.fastjson.JSONObject.parseObject(result);
			 System.out.println("jo="+jo);
			 byte[] resultByte;
			 String session_key = jo.getString("session_key");
				try {					
					resultByte = 
							//Pkcs7Encoder.decryptOfDiyIV(Base64.decode(encryptedData), Base64.decode(session_key), Base64.decode(iv));
							aes.decrypt_lk(Base64.decode(encryptedData), Base64.decode(session_key), Base64.decode(iv));
					 System.out.println("resultByte="+resultByte);
					if (null != resultByte && resultByte.length > 0) {
						String u = new String(resultByte, "UTF-8");
						com.alibaba.fastjson.JSONObject jou = com.alibaba.fastjson.JSONObject.parseObject(u);					
						r.set("openGId", jou.get("openGId"));						
					}else{
						r.set("openGId", "");	
					}
					list.add(r);
					
					// System.out.println("ivb="+ivb);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}finally{
					jsonString = jsonData.getJson(0, "success", list);					
			}
		 }else{
			 jsonString = jsonData.getJson(2, "参数错误", new ArrayList<Record>());			
		 }
		 renderText(jsonString);
	 }
	/*
	 * 群相册 -创建相册
	 */
	@Before(CrossDomain.class)
	public void CreateFlockAlbum() {
		NewH5Service service=new NewH5Service();
		// 获取参数
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String groupName = this.getPara("groupName");
		String groupType = this.getPara("groupType");
		String url = this.getPara("url");
		String source = this.getPara("source");
		//String formID = this.getPara("formID")==null?"":this.getPara("formID"); // 小程序推送表单ID
		String isDefault=this.getPara("isDefault")==null?"1":this.getPara("isDefault");//0为普通相册，1为群相册
		String groupNewType=this.getPara("groupNewType")==null||this.getPara("groupNewType").equals("")||this.getPara("groupNewType").equals("0")?"13":this.getPara("groupNewType");//0为默认		
		String openGId=this.getPara("openGId");
		if(null==openGId||openGId.equals("")){
			jsonString = jsonData.getJson(2, "参数错误", new ArrayList<Record>());
			renderText(jsonString);
		}
		if(service.canCreateFlockAlbum(openGId)){
//		if(!userid.equals("")&&!formID.equals("")){
//			FormID.insert(userid, formID);
//		}
		//if (url == null || url.equals("")) {
			// 根据组类别获取相应的群头像图片
			//返回url
		url= getUrlByNewType(groupNewType);
//		}else {
//			String temp = url.substring(0, 7);
//			if (!(temp.equals("http://"))) {
//				url = CommonParam.qiniuOpenAddress + url;
//			}
//
//		}
		String inviteCode = Group.CreateSpaceInviteCode();
		
		jsonString = service.createAlbum("群相册", userid, url, groupType, inviteCode, source,isDefault,groupNewType,openGId);
		}else{
			jsonString = jsonData.getJson(2, "该相册已创建", new ArrayList<Record>());
		}
		// 返回结果
		renderText(jsonString);
	}
	/*
	 * 群相册-根据openGId判断相册是否存在，若不存在则创建相册，返回相册id
	 */
	public void getGroupidByOpenGId(){
		NewH5Service service=new NewH5Service();
		String openGId=this.getPara("openGId");
		String userid = this.getPara("userid")==null||this.getPara("userid").toString().equals("")?"816596":this.getPara("userid");
		if(null==openGId||openGId.equals("")){
			jsonString = jsonData.getJson(2, "参数错误", new ArrayList<Record>());
			renderText(jsonString);
			return;
		}
		if(service.canCreateFlockAlbum(openGId)){
			String inviteCode = Group.CreateSpaceInviteCode();
			jsonString = service.createFlockAlbum("群相册", userid, "", "4", inviteCode, "群相册","1","19",openGId,"");
		}else{
			//获取相册id			
			jsonString = jsonData.getSuccessJson(service.getFlockAlbumByOpenGId(openGId));
		}
		renderText(jsonString);
	}
	public String getUrlByNewType(String groupNewType){
		String url=CommonParam.qiniuOpenAddress+"20180104-13-1.jpg";
		switch (groupNewType) {
		case "1":// 朋友
			List<Record> coverList = CacheKit.get("EternalCache", "spaceCover1");
			// 缓存为空，重新查询
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(11);
				CacheKit.put("EternalCache", "spaceCover1", coverList);
			}
			int size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		case "2":// 聚会
			coverList = CacheKit.get("EternalCache", "spaceCover2");
			// 缓存为空，重新查询
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(12);
				CacheKit.put("EternalCache", "spaceCover2", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		case "3":// 个人
			coverList = CacheKit.get("EternalCache", "spaceCover3");
			// 缓存为空，重新查询
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(13);
				CacheKit.put("EternalCache", "spaceCover3", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		case "4":// 家人  只有一张				
			url = CommonParam.qiniuOpenAddress+"20180104-4-1.jpg";
			break;
		case "5":// 亲子
			coverList = CacheKit.get("EternalCache", "spaceCover5");
			// 缓存为空，重新查询
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(15);
				CacheKit.put("EternalCache", "spaceCover5", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		case "6":// 出游
			coverList = CacheKit.get("EternalCache", "spaceCover6");
			// 缓存为空，重新查询
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(16);
				CacheKit.put("EternalCache", "spaceCover6", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		case "7":// 团体
			coverList = CacheKit.get("EternalCache", "spaceCover7");
			// 缓存为空，重新查询
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(17);
				CacheKit.put("EternalCache", "spaceCover7", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		case "8":// 兴趣
			coverList = CacheKit.get("EternalCache", "spaceCover8");
			// 缓存为空，重新查询
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(18);
				CacheKit.put("EternalCache", "spaceCover8", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		case "9":// 校园
			coverList = CacheKit.get("EternalCache", "spaceCover9");
			// 缓存为空，重新查询
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(19);
				CacheKit.put("EternalCache", "spaceCover9", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		case "10":// 公司
			coverList = CacheKit.get("EternalCache", "spaceCover10");
			// 缓存为空，重新查询
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(20);
				CacheKit.put("EternalCache", "spaceCover10", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		case "11":// 情侣
			coverList = CacheKit.get("EternalCache", "spaceCover11");
			// 缓存为空，重新查询
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(21);
				CacheKit.put("EternalCache", "spaceCover11", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		case "12":// 活动 只有一张
			url = CommonParam.qiniuOpenAddress+"20180104-12-1.jpg";
			break;
		case "13":// 其它
			coverList = CacheKit.get("EternalCache", "spaceCover13");
			// 缓存为空，重新查询
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(23);
				CacheKit.put("EternalCache", "spaceCover13", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		default:
			coverList = CacheKit.get("EternalCache", "spaceCover13");
			// 缓存为空，重新查询
			if (coverList == null) {
				coverList = Group.GetNewSpaceDefaultCoverList(23);
				CacheKit.put("EternalCache", "spaceCover13", coverList);
			}
			size = coverList.size();
			url = coverList.get(new Random().nextInt(size)).getStr("acurl");
			break;
		}
		return url;
	}
	/*
	 * 发现-热点图片列表
	 */
	 public void getHotPic(){
		 NewH5Service service=new NewH5Service();
		 String uid=this.getPara("uid");
		 jsonString = jsonData.getJson(2, "参数错误", new ArrayList<Record>());
			
		 if(null!=uid&&!uid.equals("")){
			 List<Record> list=service.getUserHotPicList(uid);
			 jsonString = jsonData.getSuccessJson(list);
		 }
		 renderText(jsonString);
		 return;
	 }
	 /*
	  * 发现-用户最后查看的最后一个热点图片
	  */
	 public void setUserHotPic(){
		 jsonString = jsonData.getJson(2, "参数错误", new ArrayList<Record>());
		 String uid=this.getPara("uid");
		 String hid=this.getPara("hid");
		 NewH5Service service=new NewH5Service();
		if(service.setUserHotPic(uid, hid)){
			 jsonString = jsonData.getSuccessJson(new ArrayList<Record>());
		}
		 renderText(jsonString);
		 return;
	 }
	 /*
	  * 提示-用户第一次发布动态判断
	  */
	 public void userFirstPublish(){
		 jsonString = jsonData.getJson(2, "参数错误", new ArrayList<Record>());
		 String uid=this.getPara("uid"); 
		 if(null!=uid&&!uid.equals("")){
			 NewH5Service service=new NewH5Service();
			 Record r=new Record();
			 r.set("isFirstPublish", service.userFirstPublish(uid));
			 List<Record> list=new ArrayList<Record>();
			 list.add(r);
			 jsonString = jsonData.getSuccessJson(list);
		 }
		 renderText(jsonString);
		 return;
	 }
	 
	 /**
	  * 埋点-开关
	 * @throws IOException 
	  */
	 public void sendValue() throws IOException {
		 
		 BufferedReader reader = null; 
			try {
				String strUrl = "http://picture.zhuiyinanian.com/yinian/sendValue.txt";
				URL url = new URL(strUrl); 
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				InputStreamReader input = new InputStreamReader(conn.getInputStream());
				reader = new BufferedReader(input);
				//reader = new BufferedReader(new InputStreamReader(new FileInputStream("D:\\leiyu\\sendValue.txt")));
				StringBuffer buffer = new StringBuffer();
				String line = reader.readLine();
				while (line!=null) {
					buffer.append(line);
					line = reader.readLine();
				}
				reader.close(); 
				String content = buffer.toString();
				
				List<Record> resultList = new ArrayList<Record>();
				Record r = new Record();
				if (content != null && content.length() > 0) {
					//当内容不为空时关闭
					r.set("sendValue", 1);//0 开启  1关闭
				}else {
					//当内容为空时开启
					r.set("sendValue", 0);//0 开启  1关闭
				}
				List<Record> newReturnList = new ArrayList<Record>();
				newReturnList.add(r);
				jsonString = jsonData.getSuccessJson(newReturnList);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				reader.close();
				renderText(jsonString);
			}
	 }
	 /*
	  * by lk 相册封面 带翻页(翻页先取消)
	  */
	 public void getAllDefaultAlbumCover(){
		 //String acid=null==this.getPara("acid")||this.getPara("acid").equals("")?"0":this.getPara("acid");
		 NewH5Service service=new NewH5Service();		
		 List<Record> coverList = CacheKit.get("EternalCache", "AllDefaultAlbumCover");
		 if(null==coverList){
			 //System.out.println("不是缓存");
				coverList=service.getAllDefaultAlbumCover(null);
				CacheKit.put("EternalCache", "AllDefaultAlbumCover", coverList);			
		 }
		 jsonString = jsonData.getSuccessJson(coverList);
		 renderText(jsonString);
	 }
	 /*
	  * by lk 相册输入密码时显示秘钥key
	  */
	 public void getUserKey(){
		 String userid=this.getPara("userid");
		 List<Record> list=new ArrayList<Record>();
		 Record r=new Record();
		 r.set("userkey", "");
		 if(null!=userid&&!userid.equals("")){
			 NewH5Service service=new NewH5Service();
			 r.set("userkey", URLEncoder.encode(service.getKey(userid)));
		 }
		 list.add(r);
		 jsonString = jsonData.getSuccessJson(list);
		 renderText(jsonString); 
	 }
	 /*
	  * by lk 通过秘钥key显示id
	  */
	 public void getUseridByKey(){
		 String userkey=this.getPara("userkey");
		 List<Record> list=new ArrayList<Record>();
		 Record r=new Record();
		 r.set("userid", "");
		 if(null!=userkey&&!userkey.equals("")){
			 NewH5Service service=new NewH5Service();
			 r.set("userid", service.getUseridByKey(URLDecoder.decode(userkey)));
		 }
		 list.add(r);
		 jsonString = jsonData.getSuccessJson(list);
		 renderText(jsonString); 
	 }
}
