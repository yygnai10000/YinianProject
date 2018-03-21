package yinian.schedule;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledExecutor {

	private ScheduledExecutorService scheduExec;

	public long start;

	ScheduledExecutor() {
		this.scheduExec = Executors.newScheduledThreadPool(10);
		this.start = System.currentTimeMillis();
	}

	/**
	 * 执行抓取聊天记录的任务————每次方法间隔10秒，持续执行
	 */
	public void ChatRecord() {
		scheduExec.scheduleWithFixedDelay(new ChatRecordTimer(), 15000, 1000 * 10, TimeUnit.MILLISECONDS);
	}

	/**
	 * 执行最美时光推送的任务————每天晚上8点定时执行
	 */
	public void Push() {
		// 获取启动时间
		long initDelay = getTimeMillis("20:00:00") - System.currentTimeMillis();
		long oneDay = 24 * 60 * 60 * 1000;
		initDelay = initDelay > 0 ? initDelay : oneDay + initDelay;
		scheduExec.scheduleWithFixedDelay(new PushTimer(), initDelay, oneDay, TimeUnit.MILLISECONDS);
	}

	/**
	 * 执行删除过期优惠券的任务————每天凌晨0点定时执行
	 */
	public void ExpiredCoupon() {
		// 获取启动时间
		long initDelay = getTimeMillis("00:00:10") - System.currentTimeMillis();
		long oneDay = 24 * 60 * 60 * 1000;
		initDelay = initDelay > 0 ? initDelay : oneDay + initDelay;
		scheduExec.scheduleWithFixedDelay(new ExpiredCouponTimer(), initDelay, oneDay, TimeUnit.MILLISECONDS);
	}

	/**
	 * 执行每日文案推送——下午3:00执行
	 */
	public void DailyPush() {
		// 获取启动时间
		long initDelay = getTimeMillis("15:00:00") - System.currentTimeMillis();
		long oneDay = 24 * 60 * 60 * 1000;
		initDelay = initDelay > 0 ? initDelay : oneDay + initDelay;
		scheduExec.scheduleWithFixedDelay(new DailyPushTimer(), initDelay, oneDay, TimeUnit.MILLISECONDS);
	}

	/**
	 * 小程序老用户唤醒——每日晚上8:00执行
	 */
	public void SmallAppUserCallback() {
		// 获取启动时间
		long initDelay = getTimeMillis("20:00:00") - System.currentTimeMillis();
		long oneDay = 24 * 60 * 60 * 1000;
		initDelay = initDelay > 0 ? initDelay : oneDay + initDelay;
		scheduExec.scheduleWithFixedDelay(new SmallAppOldUserCallbackTimer(), initDelay, oneDay, TimeUnit.MILLISECONDS);
	}
	/**
	 * 执行用户量自动增加 每日24:00  ScheduledExecutor
	 */
	public void AddUserCount() {
		// 获取启动时间
		long initDelay = getTimeMillis("23:59:00") - System.currentTimeMillis();
		//System.out.println("进来了---");
		long oneDay = 24 * 60 * 60 * 1000;
		initDelay = initDelay > 0 ? initDelay : oneDay + initDelay;
		scheduExec.scheduleWithFixedDelay(new AddPeopleCount(), initDelay, oneDay, TimeUnit.MILLISECONDS);
		
	}
	/*
	 * by lk 每天下午3点半开始扫描可推送的用户
	 */
	public void SmallAppUserCallbackByPicCnt() {
		// 获取启动时间
		long initDelay = getTimeMillis("17:00:00") - System.currentTimeMillis();
		long oneDay = 24 * 60 * 60 * 1000;
		initDelay = initDelay > 0 ? initDelay : oneDay + initDelay;
		for(int i=0;i<6;i++){
			//System.out.println("i="+i);
			scheduExec.scheduleWithFixedDelay(new SmallAppOldUserCallbackByPicCntTimer(i==0?0:i*500000,(i+1)*500000), initDelay, oneDay, TimeUnit.MILLISECONDS);
		}
		
	}
	public void pay(){
		long initDelay = getTimeMillis("15:56:00") - System.currentTimeMillis();
		long oneDay = 24 * 60 * 60 * 1000;
		initDelay = initDelay > 0 ? initDelay : oneDay + initDelay;
		//CanUserdMoney.getInstance().setMoney(100000);
		for(int i=0;i<6;i++){
			//System.out.println("i="+i);
			scheduExec.scheduleWithFixedDelay(new PayPhoto(i), initDelay, oneDay, TimeUnit.MILLISECONDS);
		}
	}
	/**
	 * 获取指定时间对应的毫秒数
	 * 
	 * @param time
	 *            "HH:mm:ss"
	 * @return
	 */
	private static long getTimeMillis(String time) {
		try {
			DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
			DateFormat dayFormat = new SimpleDateFormat("yy-MM-dd");
			Date curDate = dateFormat.parse(dayFormat.format(new Date()) + " " + time);
			return curDate.getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return 0;
	}

}
