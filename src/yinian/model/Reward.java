package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class Reward extends Model<Reward> {
	public static final Reward dao = new Reward();

	/**
	 * 获取最近30条中奖信息
	 * 
	 * @return
	 */
	public static List<Record> GetRecentThirtyRewardInfo(String groupid) {
		List<Record> list = Db
				.find("select unickname,upic,RewardType,RewardContent from users,reward where userid=RewardUserID and RewardGroupID=1065266 order by RewardID desc limit 30");
		return list;
	}

}
