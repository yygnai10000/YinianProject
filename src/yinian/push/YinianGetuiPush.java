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

	// Ĭ�ϲ�����ֱ�Ӹ�ֵ
	static String appId = CommonParam.AppID;
	static String appkey = CommonParam.AppKey;
	static String master = CommonParam.MasterSecret;
	static String host = CommonParam.GetuiHost;
	YinianPushTemplate yinianTemplate = new YinianPushTemplate();

	/**
	 * ����ר�����͸������û�
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
	 * ����ר�����͸�һ���˷�����AndroidΪ͸��
	 * 
	 * @param cidRecod
	 * @param data
	 */
	public void yinianPushToList(Record cidRecord, Record data) {
		// ��ȡ��ͬ�豸��Cid�б�
		String AndroidCids = cidRecord.get("AndroidCids").toString();
		String iOSCids = cidRecord.get("iOSCids").toString();
		// ����Android�û�������
		AndroidPushMessageToList(AndroidCids, data);
		// ����iOS�û�������
		iOSPushMessageToList(iOSCids, data);

	}

	/**
	 * ����ר�����͸�һ���˷�����Android����͸����Ϊֱ��֪ͨ
	 * 
	 * @param cidRecod
	 * @param data
	 */
	public void yinianPushToListWithAndroidNotification(Record cidRecord, Record data) {
		// ��ȡ��ͬ�豸��Cid�б�
		String AndroidCids = cidRecord.get("AndroidCids").toString();
		String iOSCids = cidRecord.get("iOSCids").toString();
		// ����Android�û�������
		AndroidPushMessageToListByNotification(AndroidCids, data);
		// ����iOS�û�������
		iOSPushMessageToList(iOSCids, data);
	}

	/**
	 * android���͸������û�
	 * 
	 * @param CID
	 */
	public void AndroidPushMessageToSingle(String cid, String pushContent, String transmissionContent) {

		IGtPush push = new IGtPush(host, appkey, master);
		// ֪ͨ͸��ģ��
		// NotificationTemplate template = yinianTemplate
		// .YinianNotificationTemplate("����", pushContent, "", "",
		// transmissionContent);

		TransmissionTemplate template = yinianTemplate.AndroidTransmissionTemplate(pushContent,transmissionContent);

		SingleMessage message = new SingleMessage();
		message.setOffline(true);
		// ������Чʱ�䣬��λΪ���룬��ѡ
		message.setOfflineExpireTime(24 * 3600 * 1000);
		message.setData(template);
		message.setPushNetWorkType(0); // ��ѡ���ж��Ƿ�ͻ����Ƿ�wifi���������ͣ�1Ϊ��WIFI�����£�0Ϊ���������绷����
		Target target = new Target();

		target.setAppId(appId);
		target.setClientId(cid);
		// �û��������ͣ�cid���û�����ֻ��2��ѡ��һ
		// String alias = "��";
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
			System.out.println("��������Ӧ�쳣");
		}
	}

	/**
	 * iOS���͸������û�
	 * 
	 * @param CID
	 */
	public void iOSPushMessageToSingle(String cid, Record data, String transmissionContent) {
		IGtPush push = new IGtPush(host, appkey, master);

		// // ����cid��ȡ�û���userid��Ŀ����Ϊ�˻�ȡ���û�δ����Ϣ������ȷ��badge�����õ�����Ϊ����
		// List<Record> list = Db.find("select userid from users where ucid="+cid+" ");
		// ֪ͨ͸��ģ��
		TransmissionTemplate template = yinianTemplate.YinianTransmissionTemplate(transmissionContent, data);
		SingleMessage message = new SingleMessage();
		message.setOffline(true);
		// ������Чʱ�䣬��λΪ���룬��ѡ
		message.setOfflineExpireTime(24 * 3600 * 1000);
		message.setData(template);
		message.setPushNetWorkType(0); // ��ѡ���ж��Ƿ�ͻ����Ƿ�wifi���������ͣ�1Ϊ��WIFI�����£�0Ϊ���������绷����
		Target target = new Target();

		target.setAppId(appId);
		target.setClientId(cid);
		// �û��������ͣ�cid���û�����ֻ��2��ѡ��һ
		// String alias = "��";
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
			System.out.println("��������Ӧ�쳣");
		}
	}

	/**
	 * ��׿��ָ���б��û�������Ϣ��͸���汾
	 * 
	 * @param CIDs
	 */
	public void AndroidPushMessageToList(String CIDs, Record data) {
		// ���÷���ÿ���û������û�״̬����ѡ
		System.setProperty("gexin.rp.sdk.pushlist.needDetails", "true");
		IGtPush push = new IGtPush(host, appkey, master);

		// ֪ͨ͸��ģ��
		// NotificationTemplate template = yinianTemplate
		// .YinianNotificationTemplate("����", data.getStr("content"), "",
		// "", data.getStr("transmissionContent"));
		String pushContent = data.get("content")==null?null:data.get("content");
		TransmissionTemplate template = yinianTemplate.AndroidTransmissionTemplate(pushContent,data.getStr("transmissionContent"));
		ListMessage message = new ListMessage();
		message.setData(template);

		// ������Ϣ���ߣ�����������ʱ��
		message.setOffline(true);
		// ������Чʱ�䣬��λΪ���룬��ѡ
		message.setOfflineExpireTime(24 * 1000 * 3600);

		// ��CIDs��ֳ�����
		String[] cidArray = CIDs.split(",");
		// ��������Ŀ��
		List<Target> targets = new ArrayList<Target>();
		for (int i = 0; i < cidArray.length; i++) {
			Target target = new Target();
			target.setAppId(appId);
			target.setClientId(cidArray[i]);
			targets.add(target);
		}
		// �û��������ͣ�cid���û�����2��ֻ��ѡ��һ
		// String alias1 = "��";
		// target1.setAlias(alias1);
		// ��ȡtaskID
		String taskId = push.getContentId(message);
		// ʹ��taskID��Ŀ���������
		IPushResult ret = push.pushMessageToList(taskId, targets);
		// ��ӡ������������Ϣ
		System.out.println(ret.getResponse().toString());
	}

	/**
	 * ��׿��ָ���б��û�������Ϣ��ֱ֪ͨͨ�汾
	 * 
	 * @param CIDs
	 */
	public void AndroidPushMessageToListByNotification(String CIDs, Record data) {
		// ���÷���ÿ���û������û�״̬����ѡ
		System.setProperty("gexin.rp.sdk.pushlist.needDetails", "true");
		IGtPush push = new IGtPush(host, appkey, master);

		// ֪ͨ͸��ģ��
		NotificationTemplate template = yinianTemplate.YinianNotificationTemplate("����", data.getStr("content"), "", "",
				data.getStr("transmissionContent"));
		// TransmissionTemplate template =
		// yinianTemplate.AndroidTransmissionTemplate(data.getStr("transmissionContent"));
		ListMessage message = new ListMessage();
		message.setData(template);

		// ������Ϣ���ߣ�����������ʱ��
		message.setOffline(true);
		// ������Чʱ�䣬��λΪ���룬��ѡ
		message.setOfflineExpireTime(24 * 1000 * 3600);

		// ��CIDs��ֳ�����
		String[] cidArray = CIDs.split(",");
		// ��������Ŀ��
		List<Target> targets = new ArrayList<Target>();
		for (int i = 0; i < cidArray.length; i++) {
			Target target = new Target();
			target.setAppId(appId);
			target.setClientId(cidArray[i]);
			targets.add(target);
		}
		// �û��������ͣ�cid���û�����2��ֻ��ѡ��һ
		// String alias1 = "��";
		// target1.setAlias(alias1);
		// ��ȡtaskID
		String taskId = push.getContentId(message);
		// ʹ��taskID��Ŀ���������
		IPushResult ret = push.pushMessageToList(taskId, targets);
		// ��ӡ������������Ϣ
		System.out.println(ret.getResponse().toString());
	}

	/**
	 * iOS��ָ���б��û�������Ϣ
	 * 
	 * @param CIDs
	 */
	public void iOSPushMessageToList(String CIDs, Record data) {
		// ���÷���ÿ���û������û�״̬����ѡ
		System.setProperty("gexin.rp.sdk.pushlist.needDetails", "true");
		IGtPush push = new IGtPush(host, appkey, master);

		// ֪ͨ͸��ģ��
		TransmissionTemplate template = yinianTemplate
				.YinianTransmissionTemplateWithoutBadge(data.getStr("transmissionContent"), data);

		ListMessage message = new ListMessage();
		message.setData(template);

		// ������Ϣ���ߣ�����������ʱ��
		message.setOffline(true);
		// ������Чʱ�䣬��λΪ���룬��ѡ
		message.setOfflineExpireTime(24 * 1000 * 3600);

		// ��CIDs��ֳ�����
		String[] cidArray = CIDs.split(",");
		// ��������Ŀ��
		List<Target> targets = new ArrayList<Target>();
		for (int i = 0; i < cidArray.length; i++) {
			Target target = new Target();
			target.setAppId(appId);
			target.setClientId(cidArray[i]);
			targets.add(target);
		}
		// �û��������ͣ�cid���û�����2��ֻ��ѡ��һ
		// String alias1 = "��";
		// target1.setAlias(alias1);
		// ��ȡtaskID
		String taskId = push.getContentId(message);
		// ʹ��taskID��Ŀ���������
		IPushResult ret = push.pushMessageToList(taskId, targets);
		// ��ӡ������������Ϣ
		System.out.println(ret.getResponse().toString());
	}

	/**
	 * ��ָ��Ӧ��Ⱥ����Ϣ
	 * 
	 * @param data
	 */
	public String pushMessageToApp(String transmissionContent, Record data) {
		IGtPush push = new IGtPush(host, appkey, master);

		// ֪ͨ͸��ģ��
		TransmissionTemplate template = yinianTemplate.YinianTransmissionTemplateWithoutBadge(transmissionContent,
				data);
		AppMessage message = new AppMessage();
		message.setData(template);
		// ������Ϣ���ߣ�����������ʱ��
		message.setOffline(true);
		// ������Чʱ�䣬��λΪ���룬��ѡ
		message.setOfflineExpireTime(24 * 1000 * 3600);
		// ��������Ŀ����������
		List<String> appIdList = new ArrayList<String>();
		appIdList.add(appId);
		message.setAppIdList(appIdList);

		// List phoneTypeList = new ArrayList();
		// List provinceList = new ArrayList();
		// List tagList = new ArrayList();
		// ���û���
		// phoneTypeList.add("ANDROID");
		// //����ʡ��
		// provinceList.add("�㽭");
		// //���ñ�ǩ����
		// tagList.add("����");
		// message.setPhoneTypeList(phoneTypeList);
		// message.setProvinceList(provinceList);
		// message.setTagList(tagList);

		IPushResult ret = push.pushMessageToApp(message);
		return ret.getResponse().toString();
	}

	/**
	 * ��ȡ�û�״̬�ӿ�
	 * 
	 * @param cid
	 */
	public void getUserStatus(String cid) {
		IGtPush push = new IGtPush(host, appkey, master);
		IQueryResult result = push.getClientIdStatus(appId, cid);
		System.out.println(result.getResponse());
	}
}
