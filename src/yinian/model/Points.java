package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class Points extends Model<Points> {
	public static final Points dao = new Points();
	
	/**
	 * 查询积分信息
	 */
	public static List<Record> getPointsInfo(String userid) {
		List<Record> list = Db
				.find("select * from points where puserid=" + userid
						+ "");
		return list;
	}
	
}
