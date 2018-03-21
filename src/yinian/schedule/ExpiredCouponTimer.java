package yinian.schedule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;

import com.jfinal.plugin.activerecord.Db;

public class ExpiredCouponTimer extends TimerTask {

	@Override
	public void run() {
		// TODO Auto-generated method stub
		// 获取当天日期
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
		Date date = new Date();
		String today = sdf.format(date);
		Db.update("update coupon set couponStatus=2 where couponDate='"+today+"' ");
	}

}
