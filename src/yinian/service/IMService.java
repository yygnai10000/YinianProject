package yinian.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.qiniu.common.QiniuException;
import com.qiniu.storage.UploadManager;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.hx.api.ChatGroupAPI;
import yinian.hx.api.IMUserAPI;
import yinian.hx.comm.ClientContext;
import yinian.hx.comm.EasemobRestAPIFactory;
import yinian.hx.comm.body.ChatGroupBody;
import yinian.hx.comm.body.IMUserBody;
import yinian.hx.wrapper.BodyWrapper;
import yinian.hx.wrapper.ResponseWrapper;
import yinian.utils.DES;
import yinian.utils.QiniuOperate;

public class IMService {

	// 环信工厂方法
	private EasemobRestAPIFactory factory = ClientContext.getInstance()
			.init(ClientContext.INIT_FROM_PROPERTIES).getAPIFactory();
	// 环信用户对象
	private IMUserAPI user = (IMUserAPI) factory
			.newInstance(EasemobRestAPIFactory.USER_CLASS);
	// 环信群组对象
	private ChatGroupAPI chatgroup = (ChatGroupAPI) factory
			.newInstance(EasemobRestAPIFactory.CHATGROUP_CLASS);
	private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类
	// 七牛云操作类
	QiniuOperate qiniu = new QiniuOperate();
	// 日志操作类
	private static Log log = LogFactory.getLog(IMService.class);

	/**
	 * 获取环信登录密码
	 */
	public String getHXLoginPassword(String userid) {
		String hxPassword = userid + (CommonParam.APP_USER_PASSWORD_SUFFIX);
		try {
			// 密码进行DES加密
			hxPassword = DES.encryptDES(hxPassword, "YZadZjYx");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return hxPassword;
	}
	
	/**
	 * 增加单个环信用户
	 * 
	 * @param userid
	 * @return
	 */
	public boolean AddSingleIMUser(String userid, String nickname) {

		String password = userid + (CommonParam.APP_USER_PASSWORD_SUFFIX);
		BodyWrapper userBody = new IMUserBody(userid, password, nickname);
		ResponseWrapper rw = (ResponseWrapper) user
				.createNewIMUserSingle(userBody);
		return JudgeIMOperateStatus(rw);
	}

	/**
	 * 创建聊天群组
	 * 
	 * @param groupid
	 * @param gname
	 * @param gcreator
	 * @return
	 */
	public String CreatChatGroup(String groupid, String gname, String gcreator) {
		BodyWrapper groupBody = new ChatGroupBody(gname, groupid, true,
				(long) 1000, false, gcreator, null);
		ResponseWrapper rw = (ResponseWrapper) chatgroup
				.createChatGroup(groupBody);
		if (!JudgeIMOperateStatus(rw)) {
			AddSingleIMUser(gcreator, gname);
			rw = (ResponseWrapper) chatgroup.createChatGroup(groupBody);
		}
		ObjectNode ob = (ObjectNode) rw.getResponseBody();
		String gimid = ob.get("data").get("groupid").textValue();
		return gimid;
	}

	/**
	 * 解散聊天群组
	 * 
	 * @param groupid
	 * @return
	 */
	public boolean DismissChatGroup(String groupid) {
		String gimid = Db
				.findFirst(
						"select gimid from groups where groupid=" + groupid
								+ " ").get("gimid").toString();
		ResponseWrapper rw = (ResponseWrapper) chatgroup.deleteChatGroup(gimid);
		return JudgeIMOperateStatus(rw);
	}

	/**
	 * 添加聊天群组成员
	 * 
	 * @param userid
	 * @param groupid
	 * @return
	 */
	public boolean AddChatGroupMember(String userid, String groupid) {
		String gimid = Db
				.findFirst(
						"select gimid from groups where groupid=" + groupid
								+ " ").get("gimid").toString();
		ResponseWrapper rw = (ResponseWrapper) chatgroup
				.addSingleUserToChatGroup(gimid, userid);
		if (!JudgeIMOperateStatus(rw)) {
			AddSingleIMUser(userid, "yinianUser");
			rw = (ResponseWrapper) chatgroup.addSingleUserToChatGroup(gimid,
					userid);
		}
		return JudgeIMOperateStatus(rw);
	}

	/**
	 * 移除聊天群组成员
	 * 
	 * @param userid
	 * @param groupid
	 * @return
	 */
	public boolean RemoveChatGroupMember(String userid, String groupid) {
		String gimid = Db
				.findFirst(
						"select gimid from groups where groupid=" + groupid
								+ " ").get("gimid").toString();
		ResponseWrapper rw = (ResponseWrapper) chatgroup
				.removeSingleUserFromChatGroup(gimid, userid);
		return JudgeIMOperateStatus(rw);
	}

	/**
	 * 修改IM用户昵称
	 * 
	 * @param userid
	 * @param nickname
	 * @return
	 */
	public boolean ModifyIMUserNickname(String userid, String nickname) {
		BodyWrapper userBody = new IMUserBody(userid, null, nickname);
		ResponseWrapper rw = (ResponseWrapper) user
				.modifyIMUserNickNameWithAdminToken(userid, userBody);
		if (!JudgeIMOperateStatus(rw)) {
			AddSingleIMUser(userid, nickname);
			rw = (ResponseWrapper) user.modifyIMUserNickNameWithAdminToken(
					userid, userBody);
		}
		return JudgeIMOperateStatus(rw);
	}

	/**
	 * 判断环信操作是否成功，通过200状态码
	 * 
	 * @param rw
	 * @return
	 */
	public boolean JudgeIMOperateStatus(ResponseWrapper rw) {
		int status = rw.getResponseStatus();
		if (status == 200) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 环信相关操作失败后回滚重试方法
	 * 
	 * @param userid
	 * @param groupid
	 * @param data
	 * @param type
	 */
	public void IMOperateRallbackMethod(String userid, String groupid,
			Record data, String type) {
		// 注册用户
		AddSingleIMUser(userid, "yinianUser");
		// 重新执行方法
		switch (type) {
		case "addUser":
			AddSingleIMUser(userid, "yinianUser");
			break;
		case "createGroup":
			CreatChatGroup(groupid, data.get("gname"), data.get("gcreator"));
			break;
		case "dismissGroup":
			DismissChatGroup(groupid);
			break;
		case "addGroupMember":
			AddChatGroupMember(userid, groupid);
			break;
		case "removeGroupMember":
			RemoveChatGroupMember(userid, groupid);
			break;
		case "modifyName":
			ModifyIMUserNickname(userid, data.get("unickname"));
			break;
		}
	}

	/**
	 * 获取聊天记录
	 * 
	 * @param groupid
	 * @param chatID
	 * @param type
	 * @return
	 */
	public List<Record> getChatRecord(String groupid, String chatID, String type) {
		String sql = "";
		switch (type) {
		case "initialize":
			sql = "select chatID,chatFrom,chatTo,chatType,chatMsg,chatUrl,chatFilename,chatSecret,chatExt,chatTime,upic,unickname from chat,users where chatFrom=userid and chatTo="
					+ groupid
					+ " and chatStatus=0 order by chatTime desc limit 2 ";
			break;
		case "loading":
			sql = "select chatID,chatFrom,chatTo,chatType,chatMsg,chatUrl,chatFilename,chatSecret,chatExt,chatTime,upic,unickname from chat,users where chatFrom=userid and chatTo="
					+ groupid
					+ " and chatID<"
					+ chatID
					+ " and chatStatus=0 order by chatTime desc limit 2 ";
			break;
		}
		List<Record> list = Db.find(sql);
		// 封装消息发布者个人信息
		list = dataProcess.encapsulationChatMessagePublisher(list);
		return list;
	}

	/**
	 * 环信版本获取聊天记录
	 * 
	 * @param groupid
	 * @param chatID
	 * @param type
	 * @return
	 */
	public List<Record> getChatRecordHXVersion(String groupid, String chatID,
			String type) {
		String sql = "";
		switch (type) {
		case "initialize":
			sql = "select chatID,chatFrom,chatTo,chatType,chatLength,chatMsg,chatUrl,chatFilename,chatSecret,chatExt,chatTime,upic,unickname from chat,users where chatFrom=userid and chatTo="
					+ groupid
					+ " and chatStatus=0 order by chatTime desc limit 30 ";
			break;
		case "loading":
			sql = "select chatID,chatFrom,chatTo,chatType,chatLength,chatMsg,chatUrl,chatFilename,chatSecret,chatExt,chatTime,upic,unickname from chat,users where chatFrom=userid and chatTo="
					+ groupid
					+ " and chatID<"
					+ chatID
					+ " and chatStatus=0 order by chatTime desc limit 30 ";
			break;
		}
		List<Record> list = Db.find(sql);
		List<Record> result = new ArrayList<Record>();
		for (Record record : list) {
			String id = record.get("chatID").toString();
			String from = record.get("chatFrom").toString();
			String to = record.get("chatTo").toString();
			String ctype = record.get("chatType").toString();
			String msg = record.get("chatMsg") == null ? "" : record.get(
					"chatMsg").toString();
			String url = record.get("chatUrl").toString();
			String filename = record.get("chatFilename").toString();
			String secret = record.get("chatSecret").toString();
			String ext = record.get("chatExt").toString();
			String length = (record.get("chatLength") == null ? null : record
					.get("chatLength").toString());
			double timestamp = Double.parseDouble(record.get("chatTime")
					.toString());
			Record temp = new Record().set("type", "chatmessage")
					.set("from", from).set("msg_id", null)
					.set("chat_type", "groupchat").set("timestamp", timestamp)
					.set("to", to).set("chatID", id);
			Record body = new Record().set("msg", msg).set("type", ctype)
					.set("length", length).set("url", url)
					.set("filename", filename).set("secret", secret)
					.set("lat", null).set("lng", null).set("addr", null);
			if (ctype.equals("img")) {
				body.set("thumb", url + "?imageView2/2/w/300");
			}
			Record[] array = new Record[1];
			array[0] = body;
			Record payload = new Record().set("bodies", array).set("ext", ext);
			temp.set("payload", payload);
			result.add(temp);
		}
		// 倒序输出
		Collections.reverse(result);
		return result;
	}

	/**
	 * 下载环信资源并上传到七牛云中，返回新的资源地址
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public String DownloadFromHXAndUploadToQiniu(String hxUrl) {
		String fileName = "";
		if (hxUrl == null || hxUrl.equals("")) {
			;
		} else {
			try {
				URL url = new URL(hxUrl);
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				// 得到输入流
				InputStream inputStream = conn.getInputStream();
				byte[] buffer = new byte[1024];
				int len = 0;
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				while ((len = inputStream.read(buffer)) != -1) {
					bos.write(buffer, 0, len);
				}
				bos.close();
				// 获取数据数组
				byte[] getData = bos.toByteArray();
				String token = qiniu.getUploadToken();
				UploadManager uploadManager = new UploadManager();
				File file = new File(hxUrl);
				fileName = file.getName();
				uploadManager.put(getData, fileName, token);
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				log.error("URL解析失败");
				e1.printStackTrace();
			} catch (QiniuException e) {
				// TODO Auto-generated catch block
				log.error("七牛云上传失败");
				e.printStackTrace();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				log.error("IO错误");
				e2.printStackTrace();
			}
		}

		return CommonParam.qiniuOpenAddress + fileName;
	}

}
