package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class GroupCanPublish extends Model<GroupCanPublish> {
	public static final GroupCanPublish dao = new GroupCanPublish();
	//by lk 根据空间id和能发布的TYPE查找
	public int getGroupPublishByType(String groupid,String typeid){
		String sql="select count(*) cnt from groupCanPublish where pGroupId='"+groupid+"'  and pGroupType='"+typeid+"' and pStatus=0";
		List<Record> list=Db.find(sql);
		int cnt=0;
		if(!list.isEmpty()){
			cnt=list.get(0).getLong("cnt").intValue();
		}
		return cnt;
	}
//	public List<Record> getDialogByGroupid(String groupid,String typeid){
//		String sql="select * from groupCanPublish where pGroupId='"+groupid+"'  and pGroupType='"+typeid+"' and pStatus=0";
//		return Db.find(sql);
//	}
}
