package yinian.schedule;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jfinal.log.Logger;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

import yinian.model.TodayMemory;
import yinian.push.YinianGetuiPush;
import yinian.utils.JsonData;

public class DailyPushTimer extends TimerTask {

	private JsonData jsonData = new JsonData();
	private static final Logger log = Logger.getLogger(DailyPushTimer.class);

	@Override
	public void run() {
		// TODO Auto-generated method stub

		// ������ߣ����Ⲣ�������ظ�����
		try {
			Random random = new Random();
			long time = (random.nextInt(12) + 1) * 5000;
			Thread.sleep(time);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// �жϵ����Ƿ�ִ��
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String today = sdf.format(new Date());
		List<Record> judge = Db
				.find("select TMid,isPush from todaymemory where date(TMtime)='"
						+ today + "' ");
		int isPush = Integer.parseInt(judge.get(0).get("isPush").toString());
		if (isPush == 0) {

			// ������Ϊ�����ͣ����Ⲣ���ظ�����
			String TMid = judge.get(0).get("TMid").toString();
			TodayMemory tm = new TodayMemory().findById(TMid);
			tm.set("isPush", 1);
			tm.update();

			// ��ȡ�������ܼ�
			String weekDay = getWeekDays();

			// ��ȡ�������ݺͱ���
			String title = getPushTitle(weekDay);
			String content = getPushContet(weekDay);

			// ������app�û�����
			YinianGetuiPush push = new YinianGetuiPush();
			List<Record> list = new ArrayList<Record>();

			Record data = new Record().set("content", content).set("title",
					title);
			list.add(data);
			String transmissContent = jsonData.getJson(5, "�ճ�����", list);

			// ����
			String res = push.pushMessageToApp(transmissContent, data);

			log.error("ִ�����ճ�����" + res);

		}

	}

	/**
	 * ��ȡ����
	 * 
	 * @return
	 */
	public static String getWeekDays() {
		String[] weekDays = { "������", "����һ", "���ڶ�", "������", "������", "������", "������" };
		Calendar cal = Calendar.getInstance();
		Date curDate = new Date(System.currentTimeMillis());
		cal.setTime(curDate);
		int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
		if (w < 0)
			w = 0;

		return weekDays[w];
	}

	/**
	 * ��ȡ�������ͱ���
	 */
	public static String getPushTitle(String weekDay) {
		String title = "";

		switch (weekDay) {
		case "����һ":
			title = "��һ������׶��ƿ";
			break;
		case "���ڶ�":
			title = "�ܶ�����Ƭ�����";
			break;
		case "������":
			title = "������ϲ��������";
			break;
		case "������":
			title = "���ġ�����������";
			break;
		case "������":
			title = "�����������";
			break;
		case "������":
			title = "��������Ƭ����";
			break;
		case "������":
			title = "���ա���Ƭ����";
			break;
		}

		return title;
	}

	/**
	 * ��ȡ������������
	 */
	public static String getPushContet(String weekDay) {
		String content = "";

		switch (weekDay) {
		case "����һ":
			content = "��֪���ģ����ֺ����ּ�������ײ������Ļ�ѧ��Ӧ���ҿİͣ�˵���������������ĵģ�����һ����֪�����ҵ����ﻰ�ġ�ح��æ��æ��ʲôʱ���п�������>>";
			break;
		case "���ڶ�":
			content = "������һ���ȫ����ı�����顣������ʲô��ӵ���㣿�ҽ�������խ�Ľ֣���עһ�������䣬�Ľ������¡�ح��������Ƭ��ף���ʼ��¼>>";
			break;
		case "������":
			content = "��������վ������ϲ���ĵط�����������ϲ������ʳ���ֵ����������Ļ���������������Ķ����������������һ��...ح������Ϣ�ѷ���>>";
			break;
		case "������":
			content = "����ÿ��˵��ô�໰��û��һ��˵���Լ���ح�����꣬Ϊ����һ����Լ�����һ���������٣��װ��ģ���������>>";
			break;
		case "������":
			content = "�����˵�Ļ�/ͼƬ/����ؽ�����ߵ�ˮ�����ʼǱ�����ָ��ͬ�µ��ֱ۵��κ��㿴�ü�����Ʒ������������ã���ѷ����>>";
			break;
		case "������":
			content = "�ڴ������һ��������Ƭ��������ƶ˺�ȫ��������������ϴ��㱾������������Ƭ��>>";
			break;
		case "������":
			content = "�ڴ������һ��������Ƭ��������ƶ˺�ȫ��������������ϴ��㱾������������Ƭ��>>";
			break;
		}

		return content;
	}

}
