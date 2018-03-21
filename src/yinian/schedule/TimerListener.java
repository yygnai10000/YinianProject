package yinian.schedule;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class TimerListener implements ServletContextListener {

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        // web停止时执行
        System.out.println("定时计划关闭");
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        System.out.println("定时计划启动");
        ScheduledExecutor executor = new ScheduledExecutor();
        // 抓取聊天记录
        // executor.ChatRecord();
        // 推送最美时光
        // executor.Push();
        // 清除过期优惠券
        // executor.ExpiredCoupon();
        // 日常推送
	/*	executor.DailyPush();
		// 小程序老用户唤醒
		executor.SmallAppUserCallback();
		//用户每日提示 
		executor.SmallAppUserCallbackByPicCnt();
		//添加人数
		executor.AddUserCount();*/
    }

}