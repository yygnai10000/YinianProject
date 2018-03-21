package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class GroupsIsTop extends Model<GroupsIsTop>{
	public static final GroupsIsTop dao = new GroupsIsTop();
	/*
	 * �������û�id�����ID�����ö���¼
	 */
	public List<Record> findByUseridAndGroupid(String userid,String groupid){
		String conds="";
		if(userid!=null){
			conds+=" and tUserId="+userid+" ";
		}
		if(groupid!=null){
			conds+=" and tGroupId="+groupid+" ";
		}
		return Db.find("select * from groupsistop where 1 "+conds+" order by topTime desc");
	}
	/*
	 * �������û�id�����ID�����ö�
	 */
	public boolean saveByUseridAndGroupid(String userid,String groupid){
		GroupsIsTop top=new GroupsIsTop();
		top.set("tGroupId", groupid).set("isTop", 1).set("tUserId", userid);
		return top.save();
	}
}
