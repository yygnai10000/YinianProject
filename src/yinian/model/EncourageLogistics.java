package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class EncourageLogistics extends Model<EncourageLogistics> {
	public static final EncourageLogistics dao = new EncourageLogistics();
	
	/**
	 * ��ȡ�û���Ʒ��Ϣ����
	 */
	public List<Record> getUserEncourageInfo(String userid){
		List<Record> encourageInfo = Db.find("select * from encouragelogistics where elUserID= " + userid);
		return encourageInfo;
	}
	
	/**
	 * ͨ����Ʒ���ͻ�ȡ��Ʒ��Ϣ
	 */
	public List<Record> getUserEncourageInfo(String userid,String key){
		String sql = "select * from encouragelogistics where elUserID=" + userid + 
				" and elType=" + "'" + key + "'" +" ";
		System.out.println(sql);
		List<Record> encourageInfo = Db.find(sql);
		
		return encourageInfo ;
		
	}
	
}
