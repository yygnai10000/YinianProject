package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class Picture extends Model<Picture> {
	public static final Picture dao = new Picture();

	public List<Record> GetCountByEid(int eid) {
		String sql = "select count(*) cnt from `pictures` where peid=" + eid + " and pstatus=0";
		List<Record> list = Db.find(sql);
		return list;
	}

	public List<Record> GetByEid(int eid) {
		String sql = "select pid,poriginal from `pictures` where peid=" + eid + " and pstatus=0";
		List<Record> list = Db.find(sql);
		return list;
	}

	public List<Record> GetByEids(String eid) {
		String sql = "select peid,pid,poriginal from `pictures` where peid in (" + eid + ") and pstatus=0";
		List<Record> list = Db.find(sql);
		return list;
	}
}
