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

		// 随机休眠，避免并发导致重复推送
		try {
			Random random = new Random();
			long time = (random.nextInt(12) + 1) * 5000;
			Thread.sleep(time);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// 判断当天是否执行
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String today = sdf.format(new Date());
		List<Record> judge = Db
				.find("select TMid,isPush from todaymemory where date(TMtime)='"
						+ today + "' ");
		int isPush = Integer.parseInt(judge.get(0).get("isPush").toString());
		if (isPush == 0) {

			// 先设置为已推送，避免并发重复推送
			String TMid = judge.get(0).get("TMid").toString();
			TodayMemory tm = new TodayMemory().findById(TMid);
			tm.set("isPush", 1);
			tm.update();

			// 获取当天是周几
			String weekDay = getWeekDays();

			// 获取推送内容和标题
			String title = getPushTitle(weekDay);
			String content = getPushContet(weekDay);

			// 向整个app用户推送
			YinianGetuiPush push = new YinianGetuiPush();
			List<Record> list = new ArrayList<Record>();

			Record data = new Record().set("content", content).set("title",
					title);
			list.add(data);
			String transmissContent = jsonData.getJson(5, "日常推送", list);

			// 推送
			String res = push.pushMessageToApp(transmissContent, data);

			log.error("执行了日常推送" + res);

		}

	}

	/**
	 * 获取星期
	 * 
	 * @return
	 */
	public static String getWeekDays() {
		String[] weekDays = { "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六" };
		Calendar cal = Calendar.getInstance();
		Date curDate = new Date(System.currentTimeMillis());
		cal.setTime(curDate);
		int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
		if (w < 0)
			w = 0;

		return weekDays[w];
	}

	/**
	 * 获取当天推送标题
	 */
	public static String getPushTitle(String weekDay) {
		String title = "";

		switch (weekDay) {
		case "星期一":
			title = "周一×文字锥形瓶";
			break;
		case "星期二":
			title = "周二×照片博物馆";
			break;
		case "星期三":
			title = "周三×喜欢你坐标";
			break;
		case "星期四":
			title = "周四×声音记忆器";
			break;
		case "星期五":
			title = "周五×拍立得";
			break;
		case "星期六":
			title = "周六×照片备份";
			break;
		case "星期日":
			title = "周日×照片备份";
			break;
		}

		return title;
	}

	/**
	 * 获取当天推送内容
	 */
	public static String getPushContet(String weekDay) {
		String content = "";

		switch (weekDay) {
		case "星期一":
			content = "你知道的，文字和文字间总能碰撞出奇妙的化学反应。我磕巴，说不出来，颠三倒四的，但你一看就知道是我的心里话的。丨你忙归忙，什么时候有空来看看>>";
			break;
		case "星期二":
			content = "旅行是一封对全世界的表白情书。我能用什么来拥有你？我交给你狭窄的街，孤注一掷的日落，荒郊的冷月。丨用旅行照片表白，开始记录>>";
			break;
		case "星期三":
			content = "我现在正站在我最喜欢的地方，这里有我喜欢的美食、街道、艺术和文化，但我满脑子想的都是怎样和你分享这一切...丨坐标信息已发送>>";
			break;
		case "星期四":
			content = "我们每天说那么多话，没有一句说给自己。丨打开忆年，为认真一天的自己留下一句语音。嘿，亲爱的，你今天好吗？>>";
			break;
		case "星期五":
			content = "想把你说的话/图片/红包藏进你身边的水杯、笔记本、戒指、同事的手臂等任何你看得见的物品中嘛？体验拍立得，免费发红包>>";
			break;
		case "星期六":
			content = "内存君长呼一口气，照片存进忆年云端后全机轻松啦！点击上传你本周新增的张照片吧>>";
			break;
		case "星期日":
			content = "内存君长呼一口气，照片存进忆年云端后全机轻松啦！点击上传你本周新增的张照片吧>>";
			break;
		}

		return content;
	}

}
