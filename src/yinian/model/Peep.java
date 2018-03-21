package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class Peep extends Model<Peep> {
	public static final Peep dao = new Peep();

	// �ж��û�������ĳ�ռ��Ƿ�鿴�˶Է�
	public boolean JudgeIsUserPeepOtherToday(int peepUserid, int peepBeUserid,
			int groupid, String date) {

		List<Peep> judge = dao.find("select * from peep where peepUserid="
				+ peepUserid + " and peepBeUserid=" + peepBeUserid
				+ " and peepGroupid=" + groupid + " and peepDate='" + date
				+ "' ");
		return judge.size() == 0 ? false : true;
	}

	// ��ȡ�����û��Ѳ鿴������
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

	// ��ȡ�����û��Ѳ鿴������
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
