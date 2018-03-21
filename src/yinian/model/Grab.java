package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class Grab extends Model<Grab> {
	public static final Grab dao = new Grab();

	/**
	 * 获取抢红包信息
	 * 
	 * @param redEnvelopID
	 * @return
	 */
	public List<Record> getRedEnvelopGrabInfo(String redEnvelopID) {
		List<Record> receiveInfo = Db
				.find("select userid,unickname,upic,grabMoney,grabTime from grab,users where userid=grabUserID and grabRedEnvelopID="
						+ redEnvelopID + "  ");
		return receiveInfo;
	}

	/**
	 * 判断用户是否抢过红包 true--抢过 false--未抢
	 * 
	 * @param userid
	 * @param envelopID
	 * @return
	 */
	public boolean JudgeUserIsGrabRedEnvelop(String userid, String redEnvelopID) {
		List<Record> list = Db
				.find("select * from grab where grabRedEnvelopID="
						+ redEnvelopID + " and grabUserID=" + userid + " ");
		return list.size() == 0 ? false : true;
	}
}
