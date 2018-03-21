package yinian.app;

import java.util.ArrayList;
import java.util.List;

import yinian.utils.YinianUtils;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

public class YinianDAO {
	
	/**
	 * ͨ��userid��ȡ�û�������Ϣ
	 * 
	 * @param userid
	 * @param field
	 * @return
	 */
	public String getUserSingleInfo(String userid, String field) {
		Record record = new Record();
		String result = "";
		switch (field) {
		case "nickname":
			record = Db.findFirst("select unickname from users where userid ="
					+ userid + " ");
			result = record.get("unickname").toString();
			break;
		case "password":
			record = Db.findFirst("select upass from users where userid ="
					+ userid + " ");
			result = record.get("upass").toString();
			break;
		}
		return result;
	}
	
	/**
	 * ͨ��userid��ѯ�û�����Ϣ
	 * @param userid
	 * @param field
	 * @return
	 */
	public Record queryUserInfo(String userid,String field){
		Record record = new Record();
		String sql = "select "+field+" from users where userid= "+userid;
		record = Db.findFirst(sql);	
		return record;
	}
	
	/**
	 * �޸��û�������Ϣ
	 * @param userid
	 * @param data
	 * @param field
	 * @return
	 */
	public boolean modifyUserSingleInfo(String userid,String data,String field){
		boolean flag = true;
		switch(field){
		case "password":
			String encodePassword = YinianUtils.EncoderByMd5(data);
			int count = Db.update("update users set upass="+encodePassword+" where userid="+userid+" ");
			flag = (count==1);
		}
		return flag;
	}
	
	public List<Record> query(String field,String table,String range){
		String sql = "select "+field+" from "+table+" where "+range+" ";
		List<Record> list = Db.find(sql);
		return list;
	}
	
}
