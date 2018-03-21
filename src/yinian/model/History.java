package yinian.model;

import com.jfinal.plugin.activerecord.Model;

public class History extends Model<History> {
	public static final History dao = new History();

	/**
	 * 插入历史访问记录
	 * 
	 * @param historyUserID
	 * @param historySource
	 * @param historyVersion
	 * @param historyPort
	 * @return
	 */
	public static boolean InsertHistoryRecord(String historyUserID, String historySource, String historyVersion,
			String historyPort) {
		dao.set("historyUserID", historyUserID).set("historySource", historySource)
				.set("historyVersion", historyVersion).set("historyPort", historyPort);
		return dao.save();
	}

}
