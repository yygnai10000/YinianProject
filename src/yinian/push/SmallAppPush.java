package yinian.push;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.jfinal.log.Logger;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.model.GroupMember;
import yinian.utils.SmallAppQRCode;

public class SmallAppPush {

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static YinianDataProcess dataProcess = new YinianDataProcess();
	private static final Logger log = Logger.getLogger(SmallAppPush.class);

	public static void UploadPush(String groupid, String userid, String formID, int picNum) {
		// 获取access_token
		String accessToken = SmallAppQRCode.GetSmallAppAccessToken();

		// 获取所有成员的openID
		List<Record> openIDList = GroupMember.GetAllUserOpenIDInTheSpaceExceptCurrentUser(groupid, userid);

		// 模板ID
		String templateID = CommonParam.pushTemplateID;

		// 获取页面路径
		String path = "pages/viewscoll/viewscoll?groupid=" + groupid + "&from=timeline";

		// 获取相册名、发布者昵称、添加时间、添加张数
		List<Record> list = Db.find(
				"select unickname,gname from users,groups,groupmembers where userid=gmuserid and groupid=gmgroupid and gmgroupid="
						+ groupid + " and gmuserid=" + userid + "");
		String nickname = list.get(0).getStr("unickname");
		String gname = list.get(0).getStr("gname");
		String time = sdf.format(new Date());
		Record dataRecord = new Record().set("keyword1", new Record().set("value", gname))
				.set("keyword2", new Record().set("value", nickname)).set("keyword3", new Record().set("value", time))
				.set("keyword4", new Record().set("value", picNum + "张"));

		// 进行推送
		for (Record record : openIDList) {
			String openID = record.getStr("uopenid");
			if (!openID.equals("")) {
				// 构造请求URL和参数
				String url = "https://api.weixin.qq.com/cgi-bin/message/wxopen/template/send?access_token="
						+ accessToken + "";
				JSONObject param = new JSONObject();
				param.put("touser", openID);
				param.put("template_id", templateID);
				param.put("page", path);
				param.put("form_id", formID);
				param.put("data", dataRecord.toJson());
				// String params = "touser=" + openID + "&template_id=" + templateID + "&page="
				// + path + "&form_id="
				// + formID + "&data=" + dataRecord.toJson() + "";
				System.out.println(param.toJSONString());
				// 发送请求
				String result = dataProcess.sendPost(url, param.toJSONString(), "json");
				log.error("小程序推送结果：" + result);
			}
		}

	}

	/**
	 * 老用户召回推送
	 * 
	 * @param formID
	 * @param openID
	 */
	public static void callbackPush(String formID, String openID) {
		// 获取access_token
		String accessToken = SmallAppQRCode.GetSmallAppAccessToken();
		// 模板ID
		String templateID = CommonParam.callbackPushTemplateID;
		// 获取页面路径
		String path = "pages/index/index";
		// 参数
		String time = sdf.format(new Date());
		
		Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
		Map<String, String> tempMap = new HashMap<String, String>();
		tempMap.put("value", "您已经好几天没发新照片了，忆年里的小伙伴们都很想你，点这里进相册，快来更新照片吧！");
		map.put("keyword1", tempMap);
		tempMap = new HashMap<String, String>();
		tempMap.put("value", time);
		map.put("keyword2", tempMap);
		
		// 构造请求URL和参数
		String url = "https://api.weixin.qq.com/cgi-bin/message/wxopen/template/send?access_token=" + accessToken + "";
		JSONObject param = new JSONObject();
		param.put("touser", openID);
		param.put("template_id", templateID);
		param.put("page", path);
		param.put("form_id", formID);
		param.put("data", map);
		// 发送请求
		dataProcess.sendPost(url, param.toJSONString(), "json");
	}
	/**
	 * 老用户召回推送 隔天推送相册更新照片数量
	 * 
	 * @param formID
	 * @param openID
	 */
	public static void callbackPushPicCnt(String formID, String openID,String picCnt,String groupid,String gname) {
		// 获取access_token
		String accessToken = SmallAppQRCode.GetSmallAppAccessToken();
		// 模板ID
		String templateID = CommonParam.callbackPushPicCntTemplateID;
		// 获取页面路径
		String path = "pages/viewscoll/viewscoll?port=推送&groupid="+groupid;
		// 参数
		String time = sdf.format(new Date());
		
		Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
		Map<String, String> tempMap = new HashMap<String, String>();
		tempMap.put("value", picCnt+"张");
		tempMap.put("color", "#ffa200");
		map.put("keyword1", tempMap);
		tempMap = new HashMap<String, String>();
		tempMap.put("value", "您加入的《"+gname+"》相册有新增动态，快进来看看吧");//您加入的相册有新增动态，快进来看看吧
		tempMap.put("color", "#ffa200");
		map.put("keyword2", tempMap);
		
		// 构造请求URL和参数
		String url = "https://api.weixin.qq.com/cgi-bin/message/wxopen/template/send?access_token=" + accessToken + "";
		JSONObject param = new JSONObject();
		param.put("touser", openID);
		param.put("template_id", templateID);
		param.put("page", path);
		param.put("form_id", formID);
		param.put("data", map);
		param.put("emphasis_keyword","keyword1.DATA");
		// 发送请求
		String result=dataProcess.sendPost(url, param.toJSONString(), "json");
		log.error("小程序推送结果：" + result);
	}
	
	/**
	 * 点赞推送
	 * 
	 * @param formID
	 * @param openID
	 */
	public static void likeIsPush(String formID, String openID,String eid,String username) {
		// 获取access_token
		String accessToken = SmallAppQRCode.GetSmallAppAccessToken();
		// 模板ID
		String templateID = CommonParam.likePushTemplateID;//点赞推送模板ID
		// 获取页面路径
		String path = "pages/eventdetail/eventdetail?efrom=share&eid="+eid;
		// 参数
		String time = sdf.format(new Date());
		
		Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
		Map<String, String> tempMap = new HashMap<String, String>();
		tempMap.put("value", username);
		tempMap.put("color", "#ffa200");
		map.put("keyword1", tempMap);
		tempMap = new HashMap<String, String>();
		tempMap.put("value", "有人点赞了您的动态,快进去看看吧");
		tempMap.put("color", "#ffa200");
		map.put("keyword2", tempMap);
		
		// 构造请求URL和参数
		String url = "https://api.weixin.qq.com/cgi-bin/message/wxopen/template/send?access_token=" + accessToken + "";
		JSONObject param = new JSONObject();
		param.put("touser", openID);
		param.put("template_id", templateID);
		param.put("page", path);
		param.put("form_id", formID);
		param.put("data", map);
		param.put("emphasis_keyword","keyword1.DATA");
		// 发送请求
		String result=dataProcess.sendPost(url, param.toJSONString(), "json");
		log.error("小程序推送结果：" + result);
	}
	
	/**
	 * 评论推送
	 * 
	 * @param formID
	 * @param openID
	 */
	public static void commentIsPush(String formID, String openID,String eid,String username) {
		// 获取access_token
		String accessToken = SmallAppQRCode.GetSmallAppAccessToken();
		// 模板ID
		String templateID = CommonParam.commentPushTemplateID;//评论推送模板ID
		// 获取页面路径
		String path = "pages/eventdetail/eventdetail?efrom=share&eid="+eid;
		// 参数
		String time = sdf.format(new Date());
		
		Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
		Map<String, String> tempMap = new HashMap<String, String>();
		tempMap.put("value", username);
		tempMap.put("color", "#ffa200");
		map.put("keyword1", tempMap);
		tempMap = new HashMap<String, String>();
		tempMap.put("value", "有人评论了您的动态,快进去看看吧");
		tempMap.put("color", "#ffa200");
		map.put("keyword2", tempMap);
		
		
		// 构造请求URL和参数
		String url = "https://api.weixin.qq.com/cgi-bin/message/wxopen/template/send?access_token=" + accessToken + "";
		JSONObject param = new JSONObject();
		param.put("touser", openID);
		param.put("template_id", templateID);
		param.put("page", path);
		param.put("form_id", formID);
		param.put("data", map);
		param.put("emphasis_keyword","keyword1.DATA");
		// 发送请求
		String result=dataProcess.sendPost(url, param.toJSONString(), "json");
		log.error("小程序推送结果：" + result);
	}
	
//	public static void uploadEventPush(String groupid,int picNum) {
//		// 获取access_token
//		String accessToken = SmallAppQRCode.GetSmallAppAccessToken();
//
//		// 获取所有成员的openID和userid
//		List<Record> openIDList = GroupMember.GetAllUserOpenIDAndFormID(groupid);
//
//		// 模板ID
//		String templateID = CommonParam.callbackPushPicCntTemplateID;
//		// 获取页面路径
//		String path = "pages/viewscoll/viewscoll?port=推送&groupid="+groupid;
//
//		// 获取相册名
//		List<Record> list = Db.find(
//				"select gname from groups,groupmembers where groupid=gmgroupid and gmgroupid="+ groupid);
//		String gname = "";
//		if(list!=null&&list.size()!=0) {
//			gname = list.get(0).getStr("gname");
//		}
//		String time = sdf.format(new Date());
//		Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
//		Map<String, String> tempMap = new HashMap<String, String>();
//		tempMap.put("value", picNum+"张");
//		tempMap.put("color", "#ffa200");
//		map.put("keyword1", tempMap);
//		tempMap = new HashMap<String, String>();
//		tempMap.put("value", "您加入的《"+gname+"》相册有新增动态，快进来看看吧");//您加入的相册有新增动态，快进来看看吧
//		tempMap.put("color", "#ffa200");
//		map.put("keyword2", tempMap);
//		
//		ExecutorService exec = Executors.newCachedThreadPool();
//		
//		// 进行推送
//		for (Record record : openIDList) {
//			String openID = record.getStr("uopenid");
//			//String formid = record.get("fromID");
//			String userid = record.get("userid").toString();
//			List<Record> formidList = Db.find("select formID from formid where 1 "
//					+ "and status=0 and userID="+userid+ " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() "
//					+ "order by time asc limit 1");
//			if (null!=formidList&&!formidList.isEmpty()) {
//				String formid = formidList.get(0).get("formID");
//				if (!openID.equals("")&&!formid.equals("")) {
//					// 构造请求URL和参数
//					String url = "https://api.weixin.qq.com/cgi-bin/message/wxopen/template/send?access_token="
//							+ accessToken + "";
//					JSONObject param = new JSONObject();
//					param.put("touser", openID);
//					param.put("template_id", templateID);
//					param.put("page", path);
//					param.put("form_id", formid);
//					param.put("data", map);
//					param.put("emphasis_keyword","keyword1.DATA");
//					System.out.println(param.toJSONString());
//					// 发送请求
//					String result = dataProcess.sendPost(url, param.toJSONString(), "json");
//					log.error("小程序推送结果：" + result);
//					Db.update("delete from formid where userID="+userid+" and formID='"+formid+"'");
//				}
//			}
//			
//		}
//
//	}
	
}
