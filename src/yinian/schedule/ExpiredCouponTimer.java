package yinian.schedule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;

import com.jfinal.plugin.activerecord.Db;

public class ExpiredCouponTimer extends TimerTask {

	@Override
	public void run() {
		// TODO Auto-generated method stub
		// ��ȡ��������
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");// �������ڸ�ʽ
		Date date = new Date();
		String today = sdf.format(date);
		Db.update("update coupon set couponStatus=2 where couponDate='"+today+"' ");
	}

}
