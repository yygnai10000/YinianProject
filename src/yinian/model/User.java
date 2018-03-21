package yinian.model;

import java.math.BigDecimal;
import java.util.List;

import yinian.app.YinianDataProcess;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class User extends Model<User> {
	public static final User dao = new User();
	private YinianDataProcess dataProcess = new YinianDataProcess();

	/**
	 * ��ѯ�û���¼������Ϣ
	 * 
	 * @return List<Record> -> userid,upass,uwechatid
	 */
	public static List<Record> QueryUserLoginBasicInfo(String account, String filed) {
		List<Record> list = Db.find("select userid,upass,uwechatid from users where " + filed + "='" + account + "'");
		return list;
	}

	/**
	 * ��ȡ�û�����ϴ���40����Ƭ
	 */
	public List<Record> GetUserRecentlyUploadPicture(String userid) {
		List<Record> list = Db.find("select pid,poriginal from users,`events`,pictures where userid=" + userid
				+ " and userid=euserid and eid=peid and estatus=0 and pstatus=0 ORDER BY puploadtime desc limit 40");
		return list;
	}

	/**
	 * ��ʼ���û���Ƭǽ
	 */
	public List<Record> initializeUserPhotoWall(String userid) {
		List<Record> list = Db.find(
				"SELECT GROUP_CONCAT(eid SEPARATOR \",\") AS eid,DATE_FORMAT(euploadtime, \"%Y-%m-%d\") AS euploadtime FROM `events` WHERE eMain NOT IN (4, 5) AND estatus IN (0, 3) AND euserid = "
						+ userid + " GROUP BY DATE_FORMAT(euploadtime, \"%Y-%m-%d\") DESC LIMIT 10");
		return list;
	}

	/**
	 * �����û���Ƭǽ
	 */
	public List<Record> loadingUserPhotoWall(String userid, String date) {
		List<Record> list = Db.find(
				"SELECT GROUP_CONCAT(eid SEPARATOR \",\") AS eid, DATE_FORMAT(euploadtime, \"%Y-%m-%d\") AS euploadtime FROM `events` WHERE eMain NOT IN (4, 5) AND estatus IN (0, 3) AND DATE_FORMAT(euploadtime, \"%Y-%m-%d\") < '"
						+ date + "' AND euserid = " + userid
						+ " GROUP BY DATE_FORMAT(euploadtime, \"%Y-%m-%d\") DESC LIMIT 10");
		return list;
	}

	/**
	 * ��ʼ���û�����Ƶǽ
	 */
	public List<Record> initializeUserVideoWall(String userid) {
		List<Record> list = Db.find("select pid,poriginal from `events`,pictures where eid=peid and euserid=" + userid
				+ " and estatus=0 and pstatus=0 and eMain=4 ORDER BY puploadtime desc limit 30");

		return list;
	}

	/**
	 * �����û�����Ƶǽ
	 */
	public List<Record> loadingUserVideoWall(String userid, String pid) {
		List<Record> list = Db.find("select pid,poriginal from `events`,pictures where eid=peid and euserid=" + userid
				+ " and estatus=0 and pstatus=0 and pid<" + pid + " and eMain=4 ORDER BY puploadtime desc limit 30");

		return list;
	}

	/**
	 * ��ȡ�û�����ϴ���4����ַ
	 */
	public List<Record> GetUserRecentlyAddress(String userid) {
		List<Record> list = Db.find("select eplace from `events` where euserid=" + userid
				+ " and eplace is not null and eplace not in ('','��С�������꡻') order by eid desc limit 4 ");
		return list;

	}

	/**
	 * �����û����ô洢�ռ�
	 */
	public boolean updateUserStoragePlace(String userid, Double number, String type) {
		boolean flag = false;
		User user = new User().findById(userid);
		Double useSpace = user.getDouble("uusespace");
		switch (type) {
		case "add":
			useSpace += number;
			break;
		case "reduce":
			useSpace -= number;
			break;
		}
		user.set("uusespace", useSpace);
		flag = user.update();
		return flag;
	}

	/**
	 * �����û��洢�ռ�����
	 */
	public boolean IncreaseUserTotalStorageSpace(String userid, Double num) {
		boolean flag = false;
		User user = dao.findById(userid);
		Double totalSpace = user.getDouble("utotalspace");
		totalSpace += num;
		user.set("utotalspace", totalSpace);
		flag = user.update();
		return flag;

	}

	/**
	 * ��ȡ�û��ϴ���Ƭ��
	 */
	public int GetUserUploadPicNum(String userid) {
		List<Record> list = Db
				.find("select count(*) as num from `events`,pictures where eid=peid and euserid=" + userid + "");
		int num = Integer.parseInt(list.get(0).get("num").toString());
		return num;
	}

	/**
	 * �ж��û�����Ƿ����
	 * 
	 * @param userid
	 * @param num
	 * @return
	 */
	public boolean JudgeUserBalanceIsEnough(String userid, BigDecimal money) {

		User user = dao.findById(userid);
		BigDecimal balance = new BigDecimal(user.get("uBalance").toString());
		// ���С�����ѽ��ֵΪ-1 ����false
		return balance.compareTo(money) == -1 ? false : true;
	}

	/**
	 * �����û����
	 */
	public boolean UpdateUserBalance(String userid, BigDecimal num, String type) {
		User user = dao.findById(userid);
		BigDecimal balance = new BigDecimal(user.get("uBalance").toString());
		switch (type) {
		case "add":
			balance = balance.add(num);
			break;
		case "sub":
			balance = balance.subtract(num);
			break;
		default:
			break;
		}
		user.set("uBalance", balance);
		return user.update();
	}

	/**
	 * �����û�id��ȡͷ���ǳ� by lk
	 * 
	 */
	public List<Record> getUserNameAndPic(int uid) {
		return Db.find("select upic,unickname from users where userid=" + uid);
	}

}
