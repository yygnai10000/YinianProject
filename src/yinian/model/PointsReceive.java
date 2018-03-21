package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class PointsReceive extends Model<PointsReceive> {
	public static final PointsReceive dao = new PointsReceive();
	
	/**
	 * ��ѯ���ּ�¼����
	 */
	public static List<Record> getPointsReceInfo(String userid) {
		List<Record> list = Db
				.find("select * from pointsReceive where puserid=" + userid
						+ "");
		return list;
	}
	
	/**
	 * ��ѯ���ջ�������
	 */
	public static List<Record> getPointsReceTodayInfo(String userid,int type) {
		List<Record> list = Db
				.find("select * from pointsReceive where puserid=" + userid
						+ " and ptid="+ type + " and to_days(receiveTime)=to_days(now())" );
		return list;
	}
	
	/**
	 * ��ѯһ���������������
	 */
	public static List<Record> getPointsOnlyOneInfo(String userid,int type) {
		List<Record> list = Db
				.find("select * from pointsReceive where puserid=" + userid
						+ " and ptid="+ type);
		return list;
	}
}
