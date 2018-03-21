package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class PointsReceive extends Model<PointsReceive> {
	public static final PointsReceive dao = new PointsReceive();
	
	/**
	 * 查询积分记录详情
	 */
	public static List<Record> getPointsReceInfo(String userid) {
		List<Record> list = Db
				.find("select * from pointsReceive where puserid=" + userid
						+ "");
		return list;
	}
	
	/**
	 * 查询今日积分详情
	 */
	public static List<Record> getPointsReceTodayInfo(String userid,int type) {
		List<Record> list = Db
				.find("select * from pointsReceive where puserid=" + userid
						+ " and ptid="+ type + " and to_days(receiveTime)=to_days(now())" );
		return list;
	}
	
	/**
	 * 查询一次性任务积分详情
	 */
	public static List<Record> getPointsOnlyOneInfo(String userid,int type) {
		List<Record> list = Db
				.find("select * from pointsReceive where puserid=" + userid
						+ " and ptid="+ type);
		return list;
	}
}
