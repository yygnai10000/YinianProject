package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class Grab extends Model<Grab> {
	public static final Grab dao = new Grab();

	/**
	 * ��ȡ�������Ϣ
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
	 * �ж��û��Ƿ�������� true--���� false--δ��
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
