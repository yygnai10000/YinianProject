package yinian.service;

import java.util.List;

import yinian.app.YinianDataProcess;
import yinian.model.Charm;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

public class DivineService {

	private YinianDataProcess dataProcess = new YinianDataProcess();

	/**
	 * 获取运势签为第几签
	 * 
	 * @param list
	 * @return
	 */
	public List<Record> getDivineNumber(List<Record> list) {
		for (Record record : list) {
			String fateID = record.get("fateID").toString();
			fateID = dataProcess.numberToChineseNumber(fateID);
			record.set("fateNumber", fateID);
		}
		return list;
	}

	/**
	 * 获取灵签
	 * 
	 * @param userid
	 * @param charmNo
	 * @return
	 */
	public boolean GetCharm(String userid, String charmNo) {
		String No = "";
		switch (charmNo) {
		case "1":
			No = "charmOne";
			break;
		case "2":
			No = "charmTwo";
			break;
		case "3":
			No = "charmThree";
			break;
		case "4":
			No = "charmFour";
			break;
		case "5":
			No = "charmFive";
			break;
		case "6":
			No = "charmSix";
			break;
		case "7":
			No = "charmSeven";
			break;
		case "8":
			No = "charmEight";
			break;
		case "9":
			No = "charmNine";
			break;
		case "10":
			No = "charmTen";
			break;
		default:
			No = "charmOne";
			break;
		}
		List<Record> list = Db.find("select * from charm where charmUserID="
				+ userid + "");
		int count = 0;
		if (list.size() == 0) {
			Charm charm = new Charm().set("charmUserID", userid);
			if (charm.save()) {
				count = Db.update("update charm set " + No
						+ "=1 where charmUserID=" + userid + " ");
			}
		} else {
			count = Db.update("update charm set " + No
					+ "=1 where charmUserID=" + userid + " ");
		}

		return (count == 1);
	}
}
