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

	// Ĭ�ϲ�����ֱ�Ӹ�ֵ
	static String appId = CommonParam.AppID;
	static String appkey = CommonParam.AppKey;
	static String master = CommonParam.MasterSecret;
	static String host = CommonParam.GetuiHost;

	/**
	 * ���֪ͨ����ҳģ��
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
		// ����APPID��APPKEY
		template.setAppId(appId);
		template.setAppkey(appkey);
		// ����֪ͨ������������
		template.setTitle(title);
		template.setText(text);
		// ����֪ͨ��ͼ��
		template.setLogo(logo);
		// ����֪ͨ������ͼ�꣬��дͼ��URL��ַ
		template.setLogoUrl(logoUrl);
		// ����֪ͨ�Ƿ����壬�𶯣����߿����
		template.setIsRing(true);
		template.setIsVibrate(true);
		template.setIsClearable(true);
		// ���ô򿪵���ַ��ַ
		template.setUrl(url);
		// ���ö�ʱչʾʱ��
		// template.setDuration("2015-01-16 11:40:00", "2015-01-16 12:24:00");
		return template;
	}

	/**
	 * 
	 * ���֪ͨ��Ӧ��ģ��
	 * 
	 * @param title
	 * @param text
	 * @param logo
	 * @param logoUrl
	 * @param transmissionContent
	 * @return iOS���Ƽ�ʹ�ø�ģ�壬�����뿴iOSģ��˵��
	 */
	public NotificationTemplate YinianNotificationTemplate(String title, String text, String logo, String logoUrl,
			String transmissionContent) {
		NotificationTemplate template = new NotificationTemplate();
		// ����APPID��APPKEY
		template.setAppId(appId);
		template.setAppkey(appkey);
		// ����֪ͨ������������
		template.setTitle(title);
		template.setText(text);
		// ����֪ͨ��ͼ��
		template.setLogo(logo);
		// ����֪ͨ������ͼ�꣬��дͼ��URL��ַ
		template.setLogoUrl(CommonParam.yinianLogo);
		// ����֪ͨ�Ƿ����壬�𶯣����߿����
		template.setIsRing(true);
		template.setIsVibrate(true);
		template.setIsClearable(true);
		// ͸����Ϣ���ã�1Ϊǿ������Ӧ�ã��ͻ��˽��յ���Ϣ��ͻ���������Ӧ�ã�2Ϊ�ȴ�Ӧ������
		template.setTransmissionType(1);
		template.setTransmissionContent(transmissionContent);
		// ���ö�ʱչʾʱ��
		// template.setDuration("2015-01-16 11:40:00", "2015-01-16 12:24:00");
		return template;
	}

	/**
	 * ���֪ͨ��������ģ��
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
		// ����APPID��APPKEY
		template.setAppId(appId);
		template.setAppkey(appkey);
		// ����֪ͨ������������
		template.setNotyTitle(notyTitle);
		template.setNotyContent(notyContent);
		// ����֪ͨ��ͼ��
		template.setNotyIcon(notyIcon);
		// ����֪ͨ�Ƿ����壬�𶯣����߿����
		template.setBelled(true);
		template.setVibrationed(true);
		template.setCleared(true);
		// ���õ������������
		template.setPopTitle(popTitle);
		template.setPopContent(popContent);
		// ���õ�����ʾ��ͼƬ
		template.setPopImage(popImage);
		template.setPopButton1("����");
		template.setPopButton2("ȡ��");

		// �������ر���
		template.setLoadTitle(loadTitle);
		template.setLoadIcon(loadIcon);
		// �������ص�ַ
		template.setLoadUrl(loadUrl);
		// ���ö�ʱչʾʱ��
		// template.setDuration("2015-01-16 11:40:00", "2015-01-16 12:24:00");
		return template;
	}

	/**
	 * ͸����Ϣģ�� ios
	 * 
	 * @param transmissionContent
	 * @param payload
	 * @return
	 */
	public TransmissionTemplate YinianTransmissionTemplate(String transmissionContent, Record data) {
		TransmissionTemplate template = new TransmissionTemplate();
		template.setAppId(appId);
		template.setAppkey(appkey);
		// ͸����Ϣ���ã�1Ϊǿ������Ӧ�ã��ͻ��˽��յ���Ϣ��ͻ���������Ӧ�ã�2Ϊ�ȴ�Ӧ������
		template.setTransmissionType(2);
		template.setTransmissionContent(transmissionContent);

		APNPayload payload = new APNPayload();
		// iOS������APNPayload����
		String userid = data.get("receiverID").toString();
		// ��ȡBadge��������
		YinianService service = new YinianService();
		int number = service.getNumbersInBadge(userid);
		payload.setBadge(number);
		payload.setContentAvailable(1);
		payload.setSound("default");
		payload.setCategory("$�ɿͻ��˶���");
		// payload.setAlertMsg(new APNPayload.SimpleAlertMsg("hello"));
		// �ֵ�ģʽʹ������
		payload.setAlertMsg(getDictionaryAlertMsg(data));
		template.setAPNInfo(payload);

		// ���ö�ʱչʾʱ��
		// template.setDuration("2015-01-16 11:40:00", "2015-01-16 12:24:00");
		return template;
	}

	/**
	 * ͸����Ϣģ�� ios����badge��������̬��������ᡢ�ճ����͵���Ӧ����ʹ�øýӿ�
	 * 
	 * @param transmissionContent
	 * @param payload
	 * @return
	 */
	public TransmissionTemplate YinianTransmissionTemplateWithoutBadge(String transmissionContent, Record data) {
		TransmissionTemplate template = new TransmissionTemplate();
		template.setAppId(appId);
		template.setAppkey(appkey);
		// ͸����Ϣ���ã�1Ϊǿ������Ӧ�ã��ͻ��˽��յ���Ϣ��ͻ���������Ӧ�ã�2Ϊ�ȴ�Ӧ������
		template.setTransmissionType(2);
		template.setTransmissionContent(transmissionContent);

		APNPayload payload = new APNPayload();
		// iOS������APNPayload����
		// payload.setBadge(1);
		payload.setContentAvailable(1);
		payload.setSound("default");
		payload.setCategory("$�ɿͻ��˶���");
		// payload.setAlertMsg(new APNPayload.SimpleAlertMsg("hello"));
		// �ֵ�ģʽʹ������
		payload.setAlertMsg(getDictionaryAlertMsg(data));
		template.setAPNInfo(payload);

		// ���ö�ʱչʾʱ��
		// template.setDuration("2015-01-16 11:40:00", "2015-01-16 12:24:00");
		return template;
	}

	/**
	 * ��ȡ�ֵ���ʾ��Ϣ
	 * 
	 * @return
	 */
	public static APNPayload.DictionaryAlertMsg getDictionaryAlertMsg(Record data) {
		APNPayload.DictionaryAlertMsg alertMsg = new APNPayload.DictionaryAlertMsg();
		alertMsg.setBody(data.getStr("content"));
		alertMsg.setActionLocKey("�鿴");
		alertMsg.setLocKey(data.getStr("content"));
		alertMsg.addLocArg("loc-args");
		alertMsg.setLaunchImage("launch-image");
		// IOS8.2���ϰ汾֧��
		String title = data.get("title") == null ? "����" : data.getStr("title");
		alertMsg.setTitle(title);
		alertMsg.setTitleLocKey(title);
		alertMsg.addTitleLocArg("TitleLocArg");
		return alertMsg;
	}

	/**
	 * Android͸��ģ��
	 * 
	 * @return
	 */
	public TransmissionTemplate AndroidTransmissionTemplate(String pushContent, String transmissionContent) {
		TransmissionTemplate template = new TransmissionTemplate();
		template.setAppId(appId);
		template.setAppkey(appkey);
		// ͸����Ϣ���ã�1Ϊǿ������Ӧ�ã��ͻ��˽��յ���Ϣ��ͻ���������Ӧ�ã�2Ϊ�ȴ�Ӧ������
		template.setTransmissionType(1);
		template.setTransmissionContent(transmissionContent);

		if (pushContent != null) {
			Notify notify = new Notify();
			notify.setTitle("����");
			notify.setContent(pushContent);
			notify.setPayload("");
			notify.setType(Type._payload);

			template.set3rdNotifyInfo(notify);// ���õ�����֪ͨ
		}
		// ���ö�ʱչʾʱ��
		// template.setDuration("2015-01-16 11:40:00", "2015-01-16 12:24:00");
		return template;
	}

}
