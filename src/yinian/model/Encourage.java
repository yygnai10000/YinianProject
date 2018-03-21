package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class Encourage extends Model<Encourage> {
	public static final Encourage dao = new Encourage();

	/**
	 * 获取激励信息
	 * 
	 * @param userid
	 * @return
	 */
	public static List<Record> getEncourageInfo(String userid) {
		List<Record> list = Db
				.find("select * from encourage where encourageUserID=" + userid
						+ "");
		// 无数据，插入数据后再查询
		if (list.size() == 0) {
			Encourage en = new Encourage().set("encourageUserID", userid);
			try {
				en.save();
			} catch (Exception e) {
				// 不处理，不论成功与否最终都要返回数据
			} finally {
				list = Db.find("select * from encourage where encourageUserID="
						+ userid + "");
			}
		}

		return list;
	}

	/**
	 * 获取APP端激励信息
	 */
	public static List<Record> getAppEncourageInfo(String userid) {
		List<Record> list = Db
				.find("select encourageID,encourageUserID,appInviteNum,appInvite,appSign,appWiFiOpen,appShareMoments,appLoginWeb from encourage where encourageUserID="
						+ userid + "");
		// 无数据，插入数据后再查询
		if (list.size() == 0) {
			Encourage en = new Encourage().set("encourageUserID", userid);
			en.save();
			list = Db
					.find("select encourageID,encourageUserID,appInviteNum,appInvite,appSign,appWiFiOpen,appShareMoments,appLoginWeb from encourage where encourageUserID="
							+ userid + "");
		}

		return list;
	}

	/**
	 * 获取激励信息数据ID
	 */
	public String getEncourageIDbyUserID(String userid) {
		List<Record> encourageInfo = getEncourageInfo(userid);
		String encourageID = "";
		if (encourageInfo.size() == 0) {
			Encourage en = new Encourage().set("encourageUserID", userid);
			en.save();
			encourageID = en.get("encourageID").toString();
		} else {
			encourageID = encourageInfo.get(0).get("encourageID").toString();
		}
		return encourageID;
	}

	/**
	 * 设置某项目不可领取
	 */
	public static boolean SetOneFieldCanNotBeGet(String userid, String field) {
		int count = Db.update("update encourage set " + field
				+ "=0 where encourageUserID=" + userid + " ");
		return count == 1;
	}

	/**
	 * 设置某项目可领取
	 */
	public static boolean SetOneFieldCanBeGet(String userid, String field) {
		int count = Db.update("update encourage set " + field
				+ "=1 where encourageUserID=" + userid + " ");
		return count == 1;
	}

	/**
	 * 设置某项目已领取
	 */
	public static boolean SetOneFieldAlreadyBeGet(String userid, String field) {
		int count = Db.update("update encourage set " + field
				+ "=2 where encourageUserID=" + userid + " ");
		return count == 1;
	}

}
