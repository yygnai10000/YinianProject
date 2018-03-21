package yinian.push;

import java.util.ArrayList;
import java.util.List;

import com.gexin.rp.sdk.base.IPushResult;
import com.gexin.rp.sdk.base.IQueryResult;
import com.gexin.rp.sdk.base.impl.AppMessage;
import com.gexin.rp.sdk.base.impl.ListMessage;
import com.gexin.rp.sdk.base.impl.SingleMessage;
import com.gexin.rp.sdk.base.impl.Target;
import com.gexin.rp.sdk.exceptions.RequestException;
import com.gexin.rp.sdk.http.IGtPush;
import com.gexin.rp.sdk.template.NotificationTemplate;
import com.gexin.rp.sdk.template.TransmissionTemplate;
import com.jfinal.plugin.activerecord.Record;

import yinian.common.CommonParam;

public class YinianGetuiPush {

	// 默认参数，直接赋值
	static String appId = CommonParam.AppID;
	static String appkey = CommonParam.AppKey;
	static String master = CommonParam.MasterSecret;
	static String host = CommonParam.GetuiHost;
	YinianPushTemplate yinianTemplate = new YinianPushTemplate();

	/**
	 * 忆年专用推送给单个用户
	 * 
	 * @param pushList
	 */
	public void yinianPushToSingle(List<Record> pushList) {
		if (pushList.size() != 0) {
			for (Record pushRecord : pushList) {
				String device = pushRecord.getStr("udevice");
				String cid = pushRecord.getStr("ucid");
				switch (device) {
				case "Android":
					AndroidPushMessageToSingle(cid, pushRecord.getStr("pushContent"),
							pushRecord.getStr("transmissionContent"));
					break;
				case "iOS":
					Record data = new Record().set("content", pushRecord.getStr("pushContent")).set("receiverID",
							pushRecord.getStr("receiverID"));
					iOSPushMessageToSingle(cid, data, pushRecord.getStr("transmissionContent"));
					break;
				}
			}
		}
	}

	/**
	 * 忆年专用推送给一组人方法，Android为透传
	 * 
	 * @param cidRecod
	 * @param data
	 */
	public void yinianPushToList(Record cidRecord, Record data) {
		// 获取不同设备的Cid列表
		String AndroidCids = cidRecord.get("AndroidCids").toString();
		String iOSCids = cidRecord.get("iOSCids").toString();
		// 进行Android用户的推送
		AndroidPushMessageToList(AndroidCids, data);
		// 进行iOS用户的推送
		iOSPushMessageToList(iOSCids, data);

	}

	/**
	 * 忆年专用推送给一组人方法，Android不是透传，为直接通知
	 * 
	 * @param cidRecod
	 * @param data
	 */
	public void yinianPushToListWithAndroidNotification(Record cidRecord, Record data) {
		// 获取不同设备的Cid列表
		String AndroidCids = cidRecord.get("AndroidCids").toString();
		String iOSCids = cidRecord.get("iOSCids").toString();
		// 进行Android用户的推送
		AndroidPushMessageToListByNotification(AndroidCids, data);
		// 进行iOS用户的推送
		iOSPushMessageToList(iOSCids, data);
	}

	/**
	 * android推送给单个用户
	 * 
	 * @param CID
	 */
	public void AndroidPushMessageToSingle(String cid, String pushContent, String transmissionContent) {

		IGtPush push = new IGtPush(host, appkey, master);
		// 通知透传模板
		// NotificationTemplate template = yinianTemplate
		// .YinianNotificationTemplate("忆年", pushContent, "", "",
		// transmissionContent);

		TransmissionTemplate template = yinianTemplate.AndroidTransmissionTemplate(pushContent,transmissionContent);

		SingleMessage message = new SingleMessage();
		message.setOffline(true);
		// 离线有效时间，单位为毫秒，可选
		message.setOfflineExpireTime(24 * 3600 * 1000);
		message.setData(template);
		message.setPushNetWorkType(0); // 可选。判断是否客户端是否wifi环境下推送，1为在WIFI环境下，0为不限制网络环境。
		Target target = new Target();

		target.setAppId(appId);
		target.setClientId(cid);
		// 用户别名推送，cid和用户别名只能2者选其一
		// String alias = "个";
		// target.setAlias(alias);
		IPushResult ret = null;
		try {
			ret = push.pushMessageToSingle(message, target);
		} catch (RequestException e) {
			e.printStackTrace();
			ret = push.pushMessageToSingle(message, target, e.getRequestId());
		}
		if (ret != null) {
			System.out.println(ret.getResponse().toString());
		} else {
			System.out.println("服务器响应异常");
		}
	}

	/**
	 * iOS推送给单个用户
	 * 
	 * @param CID
	 */
	public void iOSPushMessageToSingle(String cid, Record data, String transmissionContent) {
		IGtPush push = new IGtPush(host, appkey, master);

		// // 根据cid获取用户的userid，目的是为了获取该用户未读消息数，以确定badge该设置的数字为多少
		// List<Record> list = Db.find("select userid from users where ucid="+cid+" ");
		// 通知透传模板
		TransmissionTemplate template = yinianTemplate.YinianTransmissionTemplate(transmissionContent, data);
		SingleMessage message = new SingleMessage();
		message.setOffline(true);
		// 离线有效时间，单位为毫秒，可选
		message.setOfflineExpireTime(24 * 3600 * 1000);
		message.setData(template);
		message.setPushNetWorkType(0); // 可选。判断是否客户端是否wifi环境下推送，1为在WIFI环境下，0为不限制网络环境。
		Target target = new Target();

		target.setAppId(appId);
		target.setClientId(cid);
		// 用户别名推送，cid和用户别名只能2者选其一
		// String alias = "个";
		// target.setAlias(alias);
		IPushResult ret = null;
		try {
			ret = push.pushMessageToSingle(message, target);
		} catch (RequestException e) {
			e.printStackTrace();
			ret = push.pushMessageToSingle(message, target, e.getRequestId());
		}
		if (ret != null) {
			System.out.println(ret.getResponse().toString());
		} else {
			System.out.println("服务器响应异常");
		}
	}

	/**
	 * 安卓对指定列表用户推送消息，透传版本
	 * 
	 * @param CIDs
	 */
	public void AndroidPushMessageToList(String CIDs, Record data) {
		// 配置返回每个用户返回用户状态，可选
		System.setProperty("gexin.rp.sdk.pushlist.needDetails", "true");
		IGtPush push = new IGtPush(host, appkey, master);

		// 通知透传模板
		// NotificationTemplate template = yinianTemplate
		// .YinianNotificationTemplate("忆年", data.getStr("content"), "",
		// "", data.getStr("transmissionContent"));
		String pushContent = data.get("content")==null?null:data.get("content");
		TransmissionTemplate template = yinianTemplate.AndroidTransmissionTemplate(pushContent,data.getStr("transmissionContent"));
		ListMessage message = new ListMessage();
		message.setData(template);

		// 设置消息离线，并设置离线时间
		message.setOffline(true);
		// 离线有效时间，单位为毫秒，可选
		message.setOfflineExpireTime(24 * 1000 * 3600);

		// 将CIDs拆分成数组
		String[] cidArray = CIDs.split(",");
		// 配置推送目标
		List<Target> targets = new ArrayList<Target>();
		for (int i = 0; i < cidArray.length; i++) {
			Target target = new Target();
			target.setAppId(appId);
			target.setClientId(cidArray[i]);
			targets.add(target);
		}
		// 用户别名推送，cid和用户别名2者只能选其一
		// String alias1 = "个";
		// target1.setAlias(alias1);
		// 获取taskID
		String taskId = push.getContentId(message);
		// 使用taskID对目标进行推送
		IPushResult ret = push.pushMessageToList(taskId, targets);
		// 打印服务器返回信息
		System.out.println(ret.getResponse().toString());
	}

	/**
	 * 安卓对指定列表用户推送消息，直通通知版本
	 * 
	 * @param CIDs
	 */
	public void AndroidPushMessageToListByNotification(String CIDs, Record data) {
		// 配置返回每个用户返回用户状态，可选
		System.setProperty("gexin.rp.sdk.pushlist.needDetails", "true");
		IGtPush push = new IGtPush(host, appkey, master);

		// 通知透传模板
		NotificationTemplate template = yinianTemplate.YinianNotificationTemplate("忆年", data.getStr("content"), "", "",
				data.getStr("transmissionContent"));
		// TransmissionTemplate template =
		// yinianTemplate.AndroidTransmissionTemplate(data.getStr("transmissionContent"));
		ListMessage message = new ListMessage();
		message.setData(template);

		// 设置消息离线，并设置离线时间
		message.setOffline(true);
		// 离线有效时间，单位为毫秒，可选
		message.setOfflineExpireTime(24 * 1000 * 3600);

		// 将CIDs拆分成数组
		String[] cidArray = CIDs.split(",");
		// 配置推送目标
		List<Target> targets = new ArrayList<Target>();
		for (int i = 0; i < cidArray.length; i++) {
			Target target = new Target();
			target.setAppId(appId);
			target.setClientId(cidArray[i]);
			targets.add(target);
		}
		// 用户别名推送，cid和用户别名2者只能选其一
		// String alias1 = "个";
		// target1.setAlias(alias1);
		// 获取taskID
		String taskId = push.getContentId(message);
		// 使用taskID对目标进行推送
		IPushResult ret = push.pushMessageToList(taskId, targets);
		// 打印服务器返回信息
		System.out.println(ret.getResponse().toString());
	}

	/**
	 * iOS对指定列表用户推送消息
	 * 
	 * @param CIDs
	 */
	public void iOSPushMessageToList(String CIDs, Record data) {
		// 配置返回每个用户返回用户状态，可选
		System.setProperty("gexin.rp.sdk.pushlist.needDetails", "true");
		IGtPush push = new IGtPush(host, appkey, master);

		// 通知透传模板
		TransmissionTemplate template = yinianTemplate
				.YinianTransmissionTemplateWithoutBadge(data.getStr("transmissionContent"), data);

		ListMessage message = new ListMessage();
		message.setData(template);

		// 设置消息离线，并设置离线时间
		message.setOffline(true);
		// 离线有效时间，单位为毫秒，可选
		message.setOfflineExpireTime(24 * 1000 * 3600);

		// 将CIDs拆分成数组
		String[] cidArray = CIDs.split(",");
		// 配置推送目标
		List<Target> targets = new ArrayList<Target>();
		for (int i = 0; i < cidArray.length; i++) {
			Target target = new Target();
			target.setAppId(appId);
			target.setClientId(cidArray[i]);
			targets.add(target);
		}
		// 用户别名推送，cid和用户别名2者只能选其一
		// String alias1 = "个";
		// target1.setAlias(alias1);
		// 获取taskID
		String taskId = push.getContentId(message);
		// 使用taskID对目标进行推送
		IPushResult ret = push.pushMessageToList(taskId, targets);
		// 打印服务器返回信息
		System.out.println(ret.getResponse().toString());
	}

	/**
	 * 对指定应用群推消息
	 * 
	 * @param data
	 */
	public String pushMessageToApp(String transmissionContent, Record data) {
		IGtPush push = new IGtPush(host, appkey, master);

		// 通知透传模板
		TransmissionTemplate template = yinianTemplate.YinianTransmissionTemplateWithoutBadge(transmissionContent,
				data);
		AppMessage message = new AppMessage();
		message.setData(template);
		// 设置消息离线，并设置离线时间
		message.setOffline(true);
		// 离线有效时间，单位为毫秒，可选
		message.setOfflineExpireTime(24 * 1000 * 3600);
		// 设置推送目标条件过滤
		List<String> appIdList = new ArrayList<String>();
		appIdList.add(appId);
		message.setAppIdList(appIdList);

		// List phoneTypeList = new ArrayList();
		// List provinceList = new ArrayList();
		// List tagList = new ArrayList();
		// 设置机型
		// phoneTypeList.add("ANDROID");
		// //设置省份
		// provinceList.add("浙江");
		// //设置标签内容
		// tagList.add("开心");
		// message.setPhoneTypeList(phoneTypeList);
		// message.setProvinceList(provinceList);
		// message.setTagList(tagList);

		IPushResult ret = push.pushMessageToApp(message);
		return ret.getResponse().toString();
	}

	/**
	 * 获取用户状态接口
	 * 
	 * @param cid
	 */
	public void getUserStatus(String cid) {
		IGtPush push = new IGtPush(host, appkey, master);
		IQueryResult result = push.getClientIdStatus(appId, cid);
		System.out.println(result.getResponse());
	}
}
