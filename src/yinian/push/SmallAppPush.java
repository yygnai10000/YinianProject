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
		// ��ȡaccess_token
		String accessToken = SmallAppQRCode.GetSmallAppAccessToken();

		// ��ȡ���г�Ա��openID
		List<Record> openIDList = GroupMember.GetAllUserOpenIDInTheSpaceExceptCurrentUser(groupid, userid);

		// ģ��ID
		String templateID = CommonParam.pushTemplateID;

		// ��ȡҳ��·��
		String path = "pages/viewscoll/viewscoll?groupid=" + groupid + "&from=timeline";

		// ��ȡ��������������ǳơ����ʱ�䡢�������
		List<Record> list = Db.find(
				"select unickname,gname from users,groups,groupmembers where userid=gmuserid and groupid=gmgroupid and gmgroupid="
						+ groupid + " and gmuserid=" + userid + "");
		String nickname = list.get(0).getStr("unickname");
		String gname = list.get(0).getStr("gname");
		String time = sdf.format(new Date());
		Record dataRecord = new Record().set("keyword1", new Record().set("value", gname))
				.set("keyword2", new Record().set("value", nickname)).set("keyword3", new Record().set("value", time))
				.set("keyword4", new Record().set("value", picNum + "��"));

		// ��������
		for (Record record : openIDList) {
			String openID = record.getStr("uopenid");
			if (!openID.equals("")) {
				// ��������URL�Ͳ���
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
				// ��������
				String result = dataProcess.sendPost(url, param.toJSONString(), "json");
				log.error("С�������ͽ����" + result);
			}
		}

	}

	/**
	 * ���û��ٻ�����
	 * 
	 * @param formID
	 * @param openID
	 */
	public static void callbackPush(String formID, String openID) {
		// ��ȡaccess_token
		String accessToken = SmallAppQRCode.GetSmallAppAccessToken();
		// ģ��ID
		String templateID = CommonParam.callbackPushTemplateID;
		// ��ȡҳ��·��
		String path = "pages/index/index";
		// ����
		String time = sdf.format(new Date());
		
		Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
		Map<String, String> tempMap = new HashMap<String, String>();
		tempMap.put("value", "���Ѿ��ü���û������Ƭ�ˣ��������С����Ƕ������㣬���������ᣬ����������Ƭ�ɣ�");
		map.put("keyword1", tempMap);
		tempMap = new HashMap<String, String>();
		tempMap.put("value", time);
		map.put("keyword2", tempMap);
		
		// ��������URL�Ͳ���
		String url = "https://api.weixin.qq.com/cgi-bin/message/wxopen/template/send?access_token=" + accessToken + "";
		JSONObject param = new JSONObject();
		param.put("touser", openID);
		param.put("template_id", templateID);
		param.put("page", path);
		param.put("form_id", formID);
		param.put("data", map);
		// ��������
		dataProcess.sendPost(url, param.toJSONString(), "json");
	}
	/**
	 * ���û��ٻ����� ����������������Ƭ����
	 * 
	 * @param formID
	 * @param openID
	 */
	public static void callbackPushPicCnt(String formID, String openID,String picCnt,String groupid,String gname) {
		// ��ȡaccess_token
		String accessToken = SmallAppQRCode.GetSmallAppAccessToken();
		// ģ��ID
		String templateID = CommonParam.callbackPushPicCntTemplateID;
		// ��ȡҳ��·��
		String path = "pages/viewscoll/viewscoll?port=����&groupid="+groupid;
		// ����
		String time = sdf.format(new Date());
		
		Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
		Map<String, String> tempMap = new HashMap<String, String>();
		tempMap.put("value", picCnt+"��");
		tempMap.put("color", "#ffa200");
		map.put("keyword1", tempMap);
		tempMap = new HashMap<String, String>();
		tempMap.put("value", "������ġ�"+gname+"�������������̬�������������");//������������������̬�������������
		tempMap.put("color", "#ffa200");
		map.put("keyword2", tempMap);
		
		// ��������URL�Ͳ���
		String url = "https://api.weixin.qq.com/cgi-bin/message/wxopen/template/send?access_token=" + accessToken + "";
		JSONObject param = new JSONObject();
		param.put("touser", openID);
		param.put("template_id", templateID);
		param.put("page", path);
		param.put("form_id", formID);
		param.put("data", map);
		param.put("emphasis_keyword","keyword1.DATA");
		// ��������
		String result=dataProcess.sendPost(url, param.toJSONString(), "json");
		log.error("С�������ͽ����" + result);
	}
	
	/**
	 * ��������
	 * 
	 * @param formID
	 * @param openID
	 */
	public static void likeIsPush(String formID, String openID,String eid,String username) {
		// ��ȡaccess_token
		String accessToken = SmallAppQRCode.GetSmallAppAccessToken();
		// ģ��ID
		String templateID = CommonParam.likePushTemplateID;//��������ģ��ID
		// ��ȡҳ��·��
		String path = "pages/eventdetail/eventdetail?efrom=share&eid="+eid;
		// ����
		String time = sdf.format(new Date());
		
		Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
		Map<String, String> tempMap = new HashMap<String, String>();
		tempMap.put("value", username);
		tempMap.put("color", "#ffa200");
		map.put("keyword1", tempMap);
		tempMap = new HashMap<String, String>();
		tempMap.put("value", "���˵��������Ķ�̬,���ȥ������");
		tempMap.put("color", "#ffa200");
		map.put("keyword2", tempMap);
		
		// ��������URL�Ͳ���
		String url = "https://api.weixin.qq.com/cgi-bin/message/wxopen/template/send?access_token=" + accessToken + "";
		JSONObject param = new JSONObject();
		param.put("touser", openID);
		param.put("template_id", templateID);
		param.put("page", path);
		param.put("form_id", formID);
		param.put("data", map);
		param.put("emphasis_keyword","keyword1.DATA");
		// ��������
		String result=dataProcess.sendPost(url, param.toJSONString(), "json");
		log.error("С�������ͽ����" + result);
	}
	
	/**
	 * ��������
	 * 
	 * @param formID
	 * @param openID
	 */
	public static void commentIsPush(String formID, String openID,String eid,String username) {
		// ��ȡaccess_token
		String accessToken = SmallAppQRCode.GetSmallAppAccessToken();
		// ģ��ID
		String templateID = CommonParam.commentPushTemplateID;//��������ģ��ID
		// ��ȡҳ��·��
		String path = "pages/eventdetail/eventdetail?efrom=share&eid="+eid;
		// ����
		String time = sdf.format(new Date());
		
		Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
		Map<String, String> tempMap = new HashMap<String, String>();
		tempMap.put("value", username);
		tempMap.put("color", "#ffa200");
		map.put("keyword1", tempMap);
		tempMap = new HashMap<String, String>();
		tempMap.put("value", "�������������Ķ�̬,���ȥ������");
		tempMap.put("color", "#ffa200");
		map.put("keyword2", tempMap);
		
		
		// ��������URL�Ͳ���
		String url = "https://api.weixin.qq.com/cgi-bin/message/wxopen/template/send?access_token=" + accessToken + "";
		JSONObject param = new JSONObject();
		param.put("touser", openID);
		param.put("template_id", templateID);
		param.put("page", path);
		param.put("form_id", formID);
		param.put("data", map);
		param.put("emphasis_keyword","keyword1.DATA");
		// ��������
		String result=dataProcess.sendPost(url, param.toJSONString(), "json");
		log.error("С�������ͽ����" + result);
	}
	
//	public static void uploadEventPush(String groupid,int picNum) {
//		// ��ȡaccess_token
//		String accessToken = SmallAppQRCode.GetSmallAppAccessToken();
//
//		// ��ȡ���г�Ա��openID��userid
//		List<Record> openIDList = GroupMember.GetAllUserOpenIDAndFormID(groupid);
//
//		// ģ��ID
//		String templateID = CommonParam.callbackPushPicCntTemplateID;
//		// ��ȡҳ��·��
//		String path = "pages/viewscoll/viewscoll?port=����&groupid="+groupid;
//
//		// ��ȡ�����
//		List<Record> list = Db.find(
//				"select gname from groups,groupmembers where groupid=gmgroupid and gmgroupid="+ groupid);
//		String gname = "";
//		if(list!=null&&list.size()!=0) {
//			gname = list.get(0).getStr("gname");
//		}
//		String time = sdf.format(new Date());
//		Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
//		Map<String, String> tempMap = new HashMap<String, String>();
//		tempMap.put("value", picNum+"��");
//		tempMap.put("color", "#ffa200");
//		map.put("keyword1", tempMap);
//		tempMap = new HashMap<String, String>();
//		tempMap.put("value", "������ġ�"+gname+"�������������̬�������������");//������������������̬�������������
//		tempMap.put("color", "#ffa200");
//		map.put("keyword2", tempMap);
//		
//		ExecutorService exec = Executors.newCachedThreadPool();
//		
//		// ��������
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
//					// ��������URL�Ͳ���
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
//					// ��������
//					String result = dataProcess.sendPost(url, param.toJSONString(), "json");
//					log.error("С�������ͽ����" + result);
//					Db.update("delete from formid where userID="+userid+" and formID='"+formid+"'");
//				}
//			}
//			
//		}
//
//	}
	
}
