package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class EncourageLogistics extends Model<EncourageLogistics> {
	public static final EncourageLogistics dao = new EncourageLogistics();
	
	/**
	 * 获取用户奖品信息详情
	 */
	public List<Record> getUserEncourageInfo(String userid){
		List<Record> encourageInfo = Db.find("select * from encouragelogistics where elUserID= " + userid);
		return encourageInfo;
	}
	
	/**
	 * 通过奖品类型获取奖品信息
	 */
	public List<Record> getUserEncourageInfo(String userid,String key){
		String sql = "select * from encouragelogistics where elUserID=" + userid + 
				" and elType=" + "'" + key + "'" +" ";
		System.out.println(sql);
		List<Record> encourageInfo = Db.find(sql);
		
		return encourageInfo ;
		
	}
	
}
