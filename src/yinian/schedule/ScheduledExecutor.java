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
	 * ִ��ץȡ�����¼�����񡪡�����ÿ�η������10�룬����ִ��
	 */
	public void ChatRecord() {
		scheduExec.scheduleWithFixedDelay(new ChatRecordTimer(), 15000, 1000 * 10, TimeUnit.MILLISECONDS);
	}

	/**
	 * ִ������ʱ�����͵����񡪡�����ÿ������8�㶨ʱִ��
	 */
	public void Push() {
		// ��ȡ����ʱ��
		long initDelay = getTimeMillis("20:00:00") - System.currentTimeMillis();
		long oneDay = 24 * 60 * 60 * 1000;
		initDelay = initDelay > 0 ? initDelay : oneDay + initDelay;
		scheduExec.scheduleWithFixedDelay(new PushTimer(), initDelay, oneDay, TimeUnit.MILLISECONDS);
	}

	/**
	 * ִ��ɾ�������Ż�ȯ�����񡪡�����ÿ���賿0�㶨ʱִ��
	 */
	public void ExpiredCoupon() {
		// ��ȡ����ʱ��
		long initDelay = getTimeMillis("00:00:10") - System.currentTimeMillis();
		long oneDay = 24 * 60 * 60 * 1000;
		initDelay = initDelay > 0 ? initDelay : oneDay + initDelay;
		scheduExec.scheduleWithFixedDelay(new ExpiredCouponTimer(), initDelay, oneDay, TimeUnit.MILLISECONDS);
	}

	/**
	 * ִ��ÿ���İ����͡�������3:00ִ��
	 */
	public void DailyPush() {
		// ��ȡ����ʱ��
		long initDelay = getTimeMillis("15:00:00") - System.currentTimeMillis();
		long oneDay = 24 * 60 * 60 * 1000;
		initDelay = initDelay > 0 ? initDelay : oneDay + initDelay;
		scheduExec.scheduleWithFixedDelay(new DailyPushTimer(), initDelay, oneDay, TimeUnit.MILLISECONDS);
	}

	/**
	 * С�������û����ѡ���ÿ������8:00ִ��
	 */
	public void SmallAppUserCallback() {
		// ��ȡ����ʱ��
		long initDelay = getTimeMillis("20:00:00") - System.currentTimeMillis();
		long oneDay = 24 * 60 * 60 * 1000;
		initDelay = initDelay > 0 ? initDelay : oneDay + initDelay;
		scheduExec.scheduleWithFixedDelay(new SmallAppOldUserCallbackTimer(), initDelay, oneDay, TimeUnit.MILLISECONDS);
	}
	/**
	 * ִ���û����Զ����� ÿ��24:00  ScheduledExecutor
	 */
	public void AddUserCount() {
		// ��ȡ����ʱ��
		long initDelay = getTimeMillis("23:59:00") - System.currentTimeMillis();
		//System.out.println("������---");
		long oneDay = 24 * 60 * 60 * 1000;
		initDelay = initDelay > 0 ? initDelay : oneDay + initDelay;
		scheduExec.scheduleWithFixedDelay(new AddPeopleCount(), initDelay, oneDay, TimeUnit.MILLISECONDS);
		
	}
	/*
	 * by lk ÿ������3��뿪ʼɨ������͵��û�
	 */
	public void SmallAppUserCallbackByPicCnt() {
		// ��ȡ����ʱ��
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
	 * ��ȡָ��ʱ���Ӧ�ĺ�����
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
