package yinian.push;

import com.gexin.rp.sdk.base.notify.Notify;
import com.gexin.rp.sdk.base.payload.APNPayload;
import com.gexin.rp.sdk.dto.GtReq.NotifyInfo.Type;
import com.gexin.rp.sdk.template.LinkTemplate;
import com.gexin.rp.sdk.template.NotificationTemplate;
import com.gexin.rp.sdk.template.NotyPopLoadTemplate;
import com.gexin.rp.sdk.template.TransmissionTemplate;
import com.jfinal.plugin.activerecord.Record;

import yinian.common.CommonParam;
import yinian.service.YinianService;

public class YinianPushTemplate {

	// 默认参数，直接赋值
	static String appId = CommonParam.AppID;
	static String appkey = CommonParam.AppKey;
	static String master = CommonParam.MasterSecret;
	static String host = CommonParam.GetuiHost;

	/**
	 * 点击通知打开网页模版
	 * 
	 * @param title
	 * @param text
	 * @param logo
	 * @param logoUrl
	 * @param url
	 * @return
	 */
	public LinkTemplate YinianLinkTemplate(String title, String text, String logo, String logoUrl, String url) {
		LinkTemplate template = new LinkTemplate();
		// 设置APPID与APPKEY
		template.setAppId(appId);
		template.setAppkey(appkey);
		// 设置通知栏标题与内容
		template.setTitle(title);
		template.setText(text);
		// 配置通知栏图标
		template.setLogo(logo);
		// 配置通知栏网络图标，填写图标URL地址
		template.setLogoUrl(logoUrl);
		// 设置通知是否响铃，震动，或者可清除
		template.setIsRing(true);
		template.setIsVibrate(true);
		template.setIsClearable(true);
		// 设置打开的网址地址
		template.setUrl(url);
		// 设置定时展示时间
		// template.setDuration("2015-01-16 11:40:00", "2015-01-16 12:24:00");
		return template;
	}

	/**
	 * 
	 * 点击通知打开应用模板
	 * 
	 * @param title
	 * @param text
	 * @param logo
	 * @param logoUrl
	 * @param transmissionContent
	 * @return iOS不推荐使用该模板，详情请看iOS模板说明
	 */
	public NotificationTemplate YinianNotificationTemplate(String title, String text, String logo, String logoUrl,
			String transmissionContent) {
		NotificationTemplate template = new NotificationTemplate();
		// 设置APPID与APPKEY
		template.setAppId(appId);
		template.setAppkey(appkey);
		// 设置通知栏标题与内容
		template.setTitle(title);
		template.setText(text);
		// 配置通知栏图标
		template.setLogo(logo);
		// 配置通知栏网络图标，填写图标URL地址
		template.setLogoUrl(CommonParam.yinianLogo);
		// 设置通知是否响铃，震动，或者可清除
		template.setIsRing(true);
		template.setIsVibrate(true);
		template.setIsClearable(true);
		// 透传消息设置，1为强制启动应用，客户端接收到消息后就会立即启动应用；2为等待应用启动
		template.setTransmissionType(1);
		template.setTransmissionContent(transmissionContent);
		// 设置定时展示时间
		// template.setDuration("2015-01-16 11:40:00", "2015-01-16 12:24:00");
		return template;
	}

	/**
	 * 点击通知弹窗下载模板
	 * 
	 * @param notyTitle
	 * @param notyContent
	 * @param notyIcon
	 * @param popTitle
	 * @param popContent
	 * @param popImage
	 * @param loadTitle
	 * @param loadIcon
	 * @param loadUrl
	 * @return
	 */
	public NotyPopLoadTemplate YinianNotyPopLoadTemplate(String notyTitle, String notyContent, String notyIcon,
			String popTitle, String popContent, String popImage, String loadTitle, String loadIcon, String loadUrl) {
		NotyPopLoadTemplate template = new NotyPopLoadTemplate();
		// 设置APPID与APPKEY
		template.setAppId(appId);
		template.setAppkey(appkey);
		// 设置通知栏标题与内容
		template.setNotyTitle(notyTitle);
		template.setNotyContent(notyContent);
		// 配置通知栏图标
		template.setNotyIcon(notyIcon);
		// 设置通知是否响铃，震动，或者可清除
		template.setBelled(true);
		template.setVibrationed(true);
		template.setCleared(true);
		// 设置弹框标题与内容
		template.setPopTitle(popTitle);
		template.setPopContent(popContent);
		// 设置弹框显示的图片
		template.setPopImage(popImage);
		template.setPopButton1("下载");
		template.setPopButton2("取消");

		// 设置下载标题
		template.setLoadTitle(loadTitle);
		template.setLoadIcon(loadIcon);
		// 设置下载地址
		template.setLoadUrl(loadUrl);
		// 设置定时展示时间
		// template.setDuration("2015-01-16 11:40:00", "2015-01-16 12:24:00");
		return template;
	}

	/**
	 * 透传消息模板 ios
	 * 
	 * @param transmissionContent
	 * @param payload
	 * @return
	 */
	public TransmissionTemplate YinianTransmissionTemplate(String transmissionContent, Record data) {
		TransmissionTemplate template = new TransmissionTemplate();
		template.setAppId(appId);
		template.setAppkey(appkey);
		// 透传消息设置，1为强制启动应用，客户端接收到消息后就会立即启动应用；2为等待应用启动
		template.setTransmissionType(2);
		template.setTransmissionContent(transmissionContent);

		APNPayload payload = new APNPayload();
		// iOS需设置APNPayload参数
		String userid = data.get("receiverID").toString();
		// 获取Badge具体数字
		YinianService service = new YinianService();
		int number = service.getNumbersInBadge(userid);
		payload.setBadge(number);
		payload.setContentAvailable(1);
		payload.setSound("default");
		payload.setCategory("$由客户端定义");
		// payload.setAlertMsg(new APNPayload.SimpleAlertMsg("hello"));
		// 字典模式使用下者
		payload.setAlertMsg(getDictionaryAlertMsg(data));
		template.setAPNInfo(payload);

		// 设置定时展示时间
		// template.setDuration("2015-01-16 11:40:00", "2015-01-16 12:24:00");
		return template;
	}

	/**
	 * 透传消息模板 ios，无badge，发布动态、音乐相册、日常推送等相应推送使用该接口
	 * 
	 * @param transmissionContent
	 * @param payload
	 * @return
	 */
	public TransmissionTemplate YinianTransmissionTemplateWithoutBadge(String transmissionContent, Record data) {
		TransmissionTemplate template = new TransmissionTemplate();
		template.setAppId(appId);
		template.setAppkey(appkey);
		// 透传消息设置，1为强制启动应用，客户端接收到消息后就会立即启动应用；2为等待应用启动
		template.setTransmissionType(2);
		template.setTransmissionContent(transmissionContent);

		APNPayload payload = new APNPayload();
		// iOS需设置APNPayload参数
		// payload.setBadge(1);
		payload.setContentAvailable(1);
		payload.setSound("default");
		payload.setCategory("$由客户端定义");
		// payload.setAlertMsg(new APNPayload.SimpleAlertMsg("hello"));
		// 字典模式使用下者
		payload.setAlertMsg(getDictionaryAlertMsg(data));
		template.setAPNInfo(payload);

		// 设置定时展示时间
		// template.setDuration("2015-01-16 11:40:00", "2015-01-16 12:24:00");
		return template;
	}

	/**
	 * 获取字典提示信息
	 * 
	 * @return
	 */
	public static APNPayload.DictionaryAlertMsg getDictionaryAlertMsg(Record data) {
		APNPayload.DictionaryAlertMsg alertMsg = new APNPayload.DictionaryAlertMsg();
		alertMsg.setBody(data.getStr("content"));
		alertMsg.setActionLocKey("查看");
		alertMsg.setLocKey(data.getStr("content"));
		alertMsg.addLocArg("loc-args");
		alertMsg.setLaunchImage("launch-image");
		// IOS8.2以上版本支持
		String title = data.get("title") == null ? "忆年" : data.getStr("title");
		alertMsg.setTitle(title);
		alertMsg.setTitleLocKey(title);
		alertMsg.addTitleLocArg("TitleLocArg");
		return alertMsg;
	}

	/**
	 * Android透传模板
	 * 
	 * @return
	 */
	public TransmissionTemplate AndroidTransmissionTemplate(String pushContent, String transmissionContent) {
		TransmissionTemplate template = new TransmissionTemplate();
		template.setAppId(appId);
		template.setAppkey(appkey);
		// 透传消息设置，1为强制启动应用，客户端接收到消息后就会立即启动应用；2为等待应用启动
		template.setTransmissionType(1);
		template.setTransmissionContent(transmissionContent);

		if (pushContent != null) {
			Notify notify = new Notify();
			notify.setTitle("忆年");
			notify.setContent(pushContent);
			notify.setPayload("");
			notify.setType(Type._payload);

			template.set3rdNotifyInfo(notify);// 设置第三方通知
		}
		// 设置定时展示时间
		// template.setDuration("2015-01-16 11:40:00", "2015-01-16 12:24:00");
		return template;
	}

}
