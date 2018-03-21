package yinian.schedule;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.push.PushMessage;
import yinian.push.YinianGetuiPush;

import com.jfinal.aop.Before;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;

public class PushTimer extends TimerTask {

	private YinianDataProcess dataProcess = new YinianDataProcess();// ���ݴ�����
	private YinianGetuiPush push = new YinianGetuiPush(); // ����������

	@Override
	@Before(Tx.class)
	public void run() {
		// TODO Auto-generated method stub
		// ��ȡ��������
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");// �������ڸ�ʽ
		Date date = new Date(System.currentTimeMillis());

		// ����ݵ����ڣ����ڲ��ظ�������
		String today = sdf.format(date);
		List<Record> listWithNoRepeat = Db
				.find("select distinct userid,upic,unickname,markID,markUserID,markType,markContent,markDate,markColor,markTop,markPic,markNotify,markRepeat,ucid,udevice from users,mark where markUserID=userid and markRepeat=0 and markNotify like '%"
						+ today + "%' and markStatus=0 ");
		// ������ݵ����ڣ������ظ�������
		sdf = new SimpleDateFormat("MM-dd");
		today = sdf.format(date);
		List<Record> listWithRepeat = Db
				.find("select distinct userid,upic,unickname,markID,markUserID,markType,markContent,markDate,markColor,markTop,markPic,markNotify,markRepeat,ucid,udevice from users,mark where markUserID=userid and markRepeat=1 and markNotify like '%"
						+ today + "%' and markStatus=0 ");
		// ��ȡ��Ҫ���͵��û�Cid�б�
		List<Record> list = dataProcess.combineTwoList(listWithNoRepeat,
				listWithRepeat);

		// �����б�Ϊ�����������
		for (int i = 0; i < list.size(); i++) {

			Record record = list.get(i);

			// ��������ʱ��ͷ���ʱ��Ĳ�ֵ������������
			String pushContent;
			String markDate = record.get("markDate").toString();
			markDate = markDate.substring(5, markDate.length());
			int different = dataProcess.computeTheDifferenceOfTwoDate(today,
					markDate);
			switch (different) {
			case 0:
				pushContent = "�����Ǹ�����Ҫ�����ӣ�";
				break;
			case 1:
				pushContent = "�����Ǹ�����Ҫ�����ӣ�";
				break;
			case 2:
				pushContent = "�������죬����һ����Ҫ������Ҫ������";
				break;
			case 7:
				pushContent = "�������һ������Ҫ�����ӣ��㻹�ǵ���";
				break;
			case -1:
			default:
				pushContent = "����һ��ʱ��ӡ�ǵ�������~";
				break;

			}

			// ��ȡ����Record������������List��
			List<Record> pushList = new ArrayList<Record>();
			PushMessage pm = new PushMessage();
			Record pushRecord = pm.getPushRecord(CommonParam.systemUserID,
					record.get("userid").toString(), null, pushContent, "0",
					"mark", record);
			if (!((pushRecord.toJson()).equals("{}"))) {
				// Record��Ϊ��ʱ�����뵽list��
				pushList.add(pushRecord);
			}
			push.yinianPushToSingle(pushList);

		}
	}

}
