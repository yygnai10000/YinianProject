package yinian.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import yinian.app.YinianDAO;
import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.hx.api.ChatGroupAPI;
import yinian.hx.api.ChatMessageAPI;
import yinian.hx.api.IMUserAPI;
import yinian.hx.comm.ClientContext;
import yinian.hx.comm.EasemobRestAPIFactory;
import yinian.hx.comm.body.ChatGroupBody;
import yinian.hx.comm.body.IMUserBody;
import yinian.hx.comm.body.IMUsersBody;
import yinian.hx.wrapper.BodyWrapper;
import yinian.hx.wrapper.ResponseWrapper;
import yinian.model.Chat;
import yinian.push.YinianGetuiPush;
import yinian.service.IMService;
import yinian.service.YinianService;
import yinian.utils.JsonData;
import yinian.utils.QiniuOperate;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.UploadManager;

public class IMController extends Controller {

	// 环信工厂方法
	private EasemobRestAPIFactory factory = ClientContext.getInstance()
			.init(ClientContext.INIT_FROM_PROPERTIES).getAPIFactory();
	// 环信用户对象
	private IMUserAPI user = (IMUserAPI) factory
			.newInstance(EasemobRestAPIFactory.USER_CLASS);
	// 环信群组对象
	private ChatGroupAPI chatgroup = (ChatGroupAPI) factory
			.newInstance(EasemobRestAPIFactory.CHATGROUP_CLASS);
	// 环信聊天消息对象
	private ChatMessageAPI chatMessage = (ChatMessageAPI) factory
			.newInstance(EasemobRestAPIFactory.MESSAGE_CLASS);

	private String jsonString; // 返回的json字符串
	private JsonData jsonData = new JsonData(); // json操作类
	private IMService service = new IMService();// 业务层对象
	private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类
	// enhance方法对目标进行AOP增强
	YinianService TxService = enhance(YinianService.class);
	private YinianGetuiPush push = new YinianGetuiPush(); // 个推推送类
	private YinianDAO dao = new YinianDAO();
	private static Log log = LogFactory.getLog(IMController.class);

	/**
	 * 批量注册用户
	 */
	public void BatchRegisterUsers() {

		List<Record> list = Db
				.find("select userid,unickname from users where userid>51027");

		for (int i = 0; i < list.size(); i += 50) {
			int t;
			if ((i + 50) > list.size()) {
				t = list.size();
			} else {
				t = (i + 50);
			}
			List<IMUserBody> users = new ArrayList<IMUserBody>();
			for (int j = i; j < t; j++) {
				String userid = list.get(j).get("userid").toString();
				System.out.print(userid + "  ");
				String nickname = list.get(j).get("unickname").toString();
				String password = userid
						+ (CommonParam.APP_USER_PASSWORD_SUFFIX);
				users.add(new IMUserBody(userid, password, nickname));
			}
			BodyWrapper usersBody = new IMUsersBody(users);
			ResponseWrapper rw = (ResponseWrapper) user
					.createNewIMUserBatch(usersBody);
			System.out.println();
			System.out.println(rw.getResponseStatus());
			System.out.println("注册");
		}
		renderText("success");
	}

	/**
	 * 批量注册群组
	 */
	public void BatchRegisterGroups() {
		List<Record> list = Db
				.find("select groupid,gname,gcreator,GROUP_CONCAT(gmuserid) as members from groups,groupmembers where groupid=gmgroupid and gtype not in(5) and groupid not in (104622,104845,104844) and gstatus=0 and groupid>104880 and gOrigin=0 GROUP BY groupid  ");
		for (Record record : list) {
			String groupName = record.get("gname").toString();
			String desc = record.get("groupid").toString();
			String owner = record.get("gcreator").toString();
			String[] members = record.get("members").toString().split(",");
			BodyWrapper groupBody = new ChatGroupBody(groupName, desc, true,
					(long) 1000, false, owner, members);
			ResponseWrapper rw = (ResponseWrapper) chatgroup
					.createChatGroup(groupBody);
			ObjectNode ob = (ObjectNode) rw.getResponseBody();
			int status = rw.getResponseStatus();
			if (status == 400) {
				System.out.println("群组注册失败，前往注册群组内的所有用户");
				String users = owner;
				if (members.length != 0) {
					users += ("," + (record.get("members").toString()));
				}
				List<Record> temp = Db
						.find("select userid,unickname from users where userid in ("
								+ users + ") ");
				for (Record tempRecord : temp) {
					String userid = tempRecord.get("userid").toString();
					String nickname = tempRecord.get("unickname").toString();
					String password = userid
							+ (CommonParam.APP_USER_PASSWORD_SUFFIX);
					BodyWrapper userBody = new IMUserBody(userid, password,
							nickname);
					user.createNewIMUserSingle(userBody);
					System.out.println(userid);
				}
				rw = (ResponseWrapper) chatgroup.createChatGroup(groupBody);
				ob = (ObjectNode) rw.getResponseBody();
			}
			String gimid = ob.get("data").get("groupid").textValue();
			Db.update("update groups set gimid='" + gimid + "' where groupid="
					+ desc + " ");
			System.out.println(desc + "号相册注册成功");
		}
		renderText("success");
	}

	/**
	 * 添加聊天消息
	 */
	public void AddChatMessage() {
		String userid = this.getPara("userid");
		String groupid = this.getPara("groupid");
		// type表示数据类型，值包括 文本、图片、语音、位置、记录卡片、时光明信片
		String type = this.getPara("type");
		String content = this.getPara("content");
		String url = this.getPara("url");
		String data = this.getPara("data");
		int newType = dataProcess.changeChatTypeIntoNumber(type);
		Chat chat = new Chat().set("chatFrom", userid).set("chatTo", groupid)
				.set("chatType", newType).set("chatContent", content)
				.set("chatUrl", url).set("chatData", data);
		jsonString = dataProcess.insertFlagResult(chat.save());
		renderText(jsonString);
	}

	/**
	 * 查看聊天记录
	 */
	public void GetChatRecord() {
		String gimid = this.getPara("gimid");
		String chatID = this.getPara("chatID");
		String type = this.getPara("type");
		List<Record> list = service.getChatRecordHXVersion(gimid, chatID, type);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 导出聊天记录
	 * 
	 * @throws IOException
	 */
	@Before(Tx.class)
	public void ExportChatRecord() {
		String lastTime = Db
				.findFirst(
						"select chatTime from chat order by chatTime desc limit 1 ")
				.get("chatTime").toString();
		String response = (String) chatMessage.exportChatMessages((long) 1000,
				null, lastTime);
		JSONObject json = JSONObject.parseObject(response);
		JSONArray node = (JSONArray) json.get("entities");

		for (int i = 0; i < node.size(); i++) {
			JSONObject ob = node.getJSONObject(i);
			String from = ob.get("from").toString();
			String to = ob.get("to").toString();
			String timestamp = ob.get("timestamp").toString();
			JSONObject payload = (JSONObject) ob.get("payload");
			String ext = payload.get("ext").toString();
			JSONArray arr = (JSONArray) payload.get("bodies");
			JSONObject bodies = (JSONObject) arr.get(0);
			String type = bodies.get("type").toString();
			Chat chat = new Chat().set("chatFrom", from).set("chatTo", to)
					.set("chatType", type).set("chatExt", ext)
					.set("chatTime", timestamp);
			if (type.equals("txt")) {
				String msg = bodies.get("msg").toString();
				chat.set("chatMsg", msg);
			} else {
				String filename = bodies.get("filename").toString();
				String secret = bodies.get("secret").toString();
				String url = bodies.get("url").toString();
				String length = (bodies.get("length") == null ? null : bodies
						.get("length").toString());
				// 将URL下载到并上传到七牛云上，返回新的url
				url = service.DownloadFromHXAndUploadToQiniu(url);
				chat.set("chatFilename", filename).set("chatSecret", secret)
						.set("chatUrl", url).set("chatLength", length);
			}
			System.out.println(ext);
			chat.save();
		}
		renderText(response);
	}

}
