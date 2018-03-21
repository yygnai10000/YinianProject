package yinian.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class Sign extends Model<Sign> {
	public static final Sign dao = new Sign();

	/**
	 * ��ȡ�û�ǩ����Ϣ
	 */
	public List<Record> getUserSignInInfo(String userid, String signType) {
		List<Record> signInfoList = Db
				.find("select signID,signStartDate,signEndDate,signFirstTime,signCount from sign where signUserID="
						+ userid + " and signType=" + signType + " ");
		return signInfoList;
	}

	/**
	 * ��ȡǩ������
	 * 
	 * @throws ParseException
	 */
	public int GetSignDay(String signStartDate, String signEndDate)
			throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		// ǩ������
		long to = sdf.parse(signEndDate).getTime();
		long from = sdf.parse(signStartDate).getTime();
		int signDay = ((int) ((to - from) / (1000 * 60 * 60 * 24)) + 1);
		return signDay;
	}

}
