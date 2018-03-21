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

	private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类
	private YinianGetuiPush push = new YinianGetuiPush(); // 个推推送类

	@Override
	@Before(Tx.class)
	public void run() {
		// TODO Auto-generated method stub
		// 获取当天日期
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
		Date date = new Date(System.currentTimeMillis());

		// 带年份的日期，用于不重复的推送
		String today = sdf.format(date);
		List<Record> listWithNoRepeat = Db
				.find("select distinct userid,upic,unickname,markID,markUserID,markType,markContent,markDate,markColor,markTop,markPic,markNotify,markRepeat,ucid,udevice from users,mark where markUserID=userid and markRepeat=0 and markNotify like '%"
						+ today + "%' and markStatus=0 ");
		// 不带年份的日期，用于重复的推送
		sdf = new SimpleDateFormat("MM-dd");
		today = sdf.format(date);
		List<Record> listWithRepeat = Db
				.find("select distinct userid,upic,unickname,markID,markUserID,markType,markContent,markDate,markColor,markTop,markPic,markNotify,markRepeat,ucid,udevice from users,mark where markUserID=userid and markRepeat=1 and markNotify like '%"
						+ today + "%' and markStatus=0 ");
		// 获取需要推送的用户Cid列表
		List<Record> list = dataProcess.combineTwoList(listWithNoRepeat,
				listWithRepeat);

		// 推送列表不为空则进行推送
		for (int i = 0; i < list.size(); i++) {

			Record record = list.get(i);

			// 计算推送时间和发布时间的差值构造推送内容
			String pushContent;
			String markDate = record.get("markDate").toString();
			markDate = markDate.substring(5, markDate.length());
			int different = dataProcess.computeTheDifferenceOfTwoDate(today,
					markDate);
			switch (different) {
			case 0:
				pushContent = "今天是个很重要的日子！";
				break;
			case 1:
				pushContent = "明天是个很重要的日子！";
				break;
			case 2:
				pushContent = "还有两天，又有一个重要的日子要到啦！";
				break;
			case 7:
				pushContent = "七天后是一个很重要的日子，你还记得吗？";
				break;
			case -1:
			default:
				pushContent = "你有一个时光印记的新提醒~";
				break;

			}

			// 获取推送Record，并放入推送List中
			List<Record> pushList = new ArrayList<Record>();
			PushMessage pm = new PushMessage();
			Record pushRecord = pm.getPushRecord(CommonParam.systemUserID,
					record.get("userid").toString(), null, pushContent, "0",
					"mark", record);
			if (!((pushRecord.toJson()).equals("{}"))) {
				// Record不为空时，加入到list中
				pushList.add(pushRecord);
			}
			push.yinianPushToSingle(pushList);

		}
	}

}
