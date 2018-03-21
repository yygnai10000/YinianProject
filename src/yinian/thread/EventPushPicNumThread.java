package yinian.thread;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.alibaba.fastjson.JSONObject;
import com.jfinal.log.Logger;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.model.GroupMember;
import yinian.push.SmallAppPush;
import yinian.utils.SmallAppQRCode;

public class EventPushPicNumThread extends Thread {
	private String groupid;
	private int picNum;
	private String userid;
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static YinianDataProcess dataProcess = new YinianDataProcess();
	private static final Logger log = Logger.getLogger(SmallAppPush.class);

	public EventPushPicNumThread(String groupid, int picNum,String userid) {
		this.groupid = groupid;
		this.picNum = picNum;
		this.userid = userid;
	}

	@Override
	public void run() {
		// ��ȡaccess_token
		String accessToken = SmallAppQRCode.GetSmallAppAccessToken();

		// ��ȡ���г�Ա��openID��userid
		List<Record> openIDList = GroupMember.GetAllUserOpenIDAndFormID(groupid,userid);

		// ģ��ID
		String templateID = CommonParam.callbackPushPicCntTemplateID;
		// ��ȡҳ��·��
		String path = "pages/viewscoll/viewscoll?port=����&groupid=" + groupid;

		// ��ȡ�����
		List<Record> list = Db
				.find("select gname from groups where groupid=" + groupid);
		String gname = "";
		if (list != null && list.size() != 0) {
			gname = list.get(0).getStr("gname");
		}
		// String time = sdf.format(new Date());
		Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
		Map<String, String> tempMap = new HashMap<String, String>();
		tempMap.put("value", picNum + "��");
		tempMap.put("color", "#ffa200");
		map.put("keyword1", tempMap);
		tempMap = new HashMap<String, String>();
		tempMap.put("value", "������ġ�" + gname + "�������������̬�������������");// ������������������̬�������������
		tempMap.put("color", "#ffa200");
		map.put("keyword2", tempMap);

		ExecutorService exec = Executors.newCachedThreadPool();

		// ��������
		for (Record record : openIDList) {
			String openID = record.getStr("uopenid");
			// String formid = record.get("fromID");
			String userid = record.get("userid").toString();
			List<Record> formidList = Db.find("select formID from formid where 1 " + "and status=0 and userID=" + userid
					+ " and time between DATE_ADD(Now(),INTERVAL -7 day) and Now() " + "order by time asc limit 1");
			if (null != formidList && !formidList.isEmpty()) {
				String formid = formidList.get(0).get("formID");
				if (!openID.equals("") && !formid.equals("")) {
					// ��������URL�Ͳ���
					String url = "https://api.weixin.qq.com/cgi-bin/message/wxopen/template/send?access_token="
							+ accessToken + "";
					JSONObject param = new JSONObject();
					param.put("touser", openID);
					param.put("template_id", templateID);
					param.put("page", path);
					param.put("form_id", formid);
					param.put("data", map);
					param.put("emphasis_keyword", "keyword1.DATA");
					System.out.println(param.toJSONString());
					// ��������
					String result = dataProcess.sendPost(url, param.toJSONString(), "json");
					log.error("С�������ͽ����" + result);
					Db.update("delete from formid where userID=" + userid + " and formID='" + formid + "'");
				}
			}
		}
	}
}
