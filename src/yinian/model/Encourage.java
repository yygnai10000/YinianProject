package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class Encourage extends Model<Encourage> {
	public static final Encourage dao = new Encourage();

	/**
	 * ��ȡ������Ϣ
	 * 
	 * @param userid
	 * @return
	 */
	public static List<Record> getEncourageInfo(String userid) {
		List<Record> list = Db
				.find("select * from encourage where encourageUserID=" + userid
						+ "");
		// �����ݣ��������ݺ��ٲ�ѯ
		if (list.size() == 0) {
			Encourage en = new Encourage().set("encourageUserID", userid);
			try {
				en.save();
			} catch (Exception e) {
				// ���������۳ɹ�������ն�Ҫ��������
			} finally {
				list = Db.find("select * from encourage where encourageUserID="
						+ userid + "");
			}
		}

		return list;
	}

	/**
	 * ��ȡAPP�˼�����Ϣ
	 */
	public static List<Record> getAppEncourageInfo(String userid) {
		List<Record> list = Db
				.find("select encourageID,encourageUserID,appInviteNum,appInvite,appSign,appWiFiOpen,appShareMoments,appLoginWeb from encourage where encourageUserID="
						+ userid + "");
		// �����ݣ��������ݺ��ٲ�ѯ
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
	 * ��ȡ������Ϣ����ID
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
	 * ����ĳ��Ŀ������ȡ
	 */
	public static boolean SetOneFieldCanNotBeGet(String userid, String field) {
		int count = Db.update("update encourage set " + field
				+ "=0 where encourageUserID=" + userid + " ");
		return count == 1;
	}

	/**
	 * ����ĳ��Ŀ����ȡ
	 */
	public static boolean SetOneFieldCanBeGet(String userid, String field) {
		int count = Db.update("update encourage set " + field
				+ "=1 where encourageUserID=" + userid + " ");
		return count == 1;
	}

	/**
	 * ����ĳ��Ŀ����ȡ
	 */
	public static boolean SetOneFieldAlreadyBeGet(String userid, String field) {
		int count = Db.update("update encourage set " + field
				+ "=2 where encourageUserID=" + userid + " ");
		return count == 1;
	}

}
