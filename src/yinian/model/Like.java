package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class Like extends Model<Like> {
	public static final Like dao = new Like();

	public List<Record> GetCountListByEventid(int eid) {
		String sql = "select count(*) cnt from `like` where likeEventID=" + eid + " and likeStatus !=1";
		List<Record> list = Db.find(sql);
		return list;
	}

	public List<Record> GetCountListByEventidAndUid(String eids, int uid) {
		String sql = "select likeEventID,count(*) cnt " + "from `like` where " + "likeEventID in (" + eids
				+ ") and likeStatus !=1 and likeUserID=" + uid + " group by likeEventID";
		List<Record> list = Db.find(sql);
		return list;
	}

	/**
	 * 初始化动态点赞列表
	 */
	public List<Record> InitializeEventLikeLike(String eid) {
		List<Record> result = Db
				.find("select likeID,userid,upic,unickname from users,`like` where userid=likeUserID and likeEventID="
						+ eid + " and likeStatus!=1 order by likeID asc limit 20 ");
		return result;
	}

	/**
	 * 加载动态点赞列表
	 */
	public List<Record> LoadingEventLikeLike(String eid, String likeID) {
		List<Record> result = Db
				.find("select likeID,userid,upic,unickname from users,`like` where userid=likeUserID and likeEventID="
						+ eid + " and likeID>" + likeID + " and likeStatus!=1 order by likeID asc limit 20");
		return result;
	}
}