package yinian.schedule;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class TimerListener implements ServletContextListener {

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        // webֹͣʱִ��
        System.out.println("��ʱ�ƻ��ر�");
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        System.out.println("��ʱ�ƻ�����");
        ScheduledExecutor executor = new ScheduledExecutor();
        // ץȡ�����¼
        // executor.ChatRecord();
        // ��������ʱ��
        // executor.Push();
        // ��������Ż�ȯ
        // executor.ExpiredCoupon();
        // �ճ�����
	/*	executor.DailyPush();
		// С�������û�����
		executor.SmallAppUserCallback();
		//�û�ÿ����ʾ 
		executor.SmallAppUserCallbackByPicCnt();
		//�������
		executor.AddUserCount();*/
    }

}