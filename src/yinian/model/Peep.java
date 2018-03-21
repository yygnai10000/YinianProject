package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class Peep extends Model<Peep> {
	public static final Peep dao = new Peep();

	// 判断用户今日在某空间是否查看了对方
	public boolean JudgeIsUserPeepOtherToday(int peepUserid, int peepBeUserid,
			int groupid, String date) {

		List<Peep> judge = dao.find("select * from peep where peepUserid="
				+ peepUserid + " and peepBeUserid=" + peepBeUserid
				+ " and peepGroupid=" + groupid + " and peepDate='" + date
				+ "' ");
		return judge.size() == 0 ? false : true;
	}

	// 获取当天用户已查看的张数
	public int GetPeepPhotoNum(int peepUserid, int peepBeUserid,
			int groupid, String date) {
		List<Peep> judge = dao
				.find("select peepPhoto from peep where peepUserid="
						+ peepUserid + " and peepBeUserid=" + peepBeUserid
						+ " and peepGroupid=" + groupid + " and peepDate='"
						+ date + "' ");
		if (judge.size() == 0)
			return 0;

		return judge.get(0).get("peepPhoto").toString().split(",").length;
	}

	// 获取当天用户已查看的数据
	public List<Record> GetPeepDataToday(int peepUserid, int peepBeUserid,
			int groupid, String date) {
		List<Record> list = Db
				.find("select peepID,peepPhoto from peep where peepUserid="
						+ peepUserid + " and peepBeUserid=" + peepBeUserid
						+ " and peepGroupid=" + groupid + " and peepDate='"
						+ date + "' ");
		return list;
	}
}
