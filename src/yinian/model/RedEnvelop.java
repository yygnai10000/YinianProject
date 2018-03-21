package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class RedEnvelop extends Model<RedEnvelop> {
	public static final RedEnvelop dao = new RedEnvelop();

	/**
	 * 获取动态的红包基本信息
	 * 
	 * @param eid
	 * @return
	 */
	public List<Record> getEventEnvelopBasicInfo(String eid) {
		List<Record> redEnvelopInfo = Db
				.find("select redEnvelopID,redEnvelopTotalNum,redEnvelopTotalMoney,redEnvelopRemainNum,redEnvelopRemainMoney from redEnvelop where redEnvelopEventID="
						+ eid + " ");
		return redEnvelopInfo;

	}

}
