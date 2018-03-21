package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class GroupMember extends Model<GroupMember> {
	public static final GroupMember dao = new GroupMember();

	/**
	 * 插入相册成员
	 * 
	 * @param userid
	 * @param groupid
	 * @return
	 */
	public boolean AddGroupMember(int userid, int groupid) {
		boolean flag = false;
		GroupMember gm = new GroupMember().set("gmgroupid", groupid).set("gmuserid", userid);
		flag = gm.save();
		// 更新分组表中组成员数量字段
		int count = Db.update("update groups set gnum = gnum+1 where groupid='" + groupid + "' ");
		return (flag && (count == 1));
	}

	/**
	 * 判断用户是否在相册中
	 * 
	 * @param userid
	 * @param groupid
	 * @return true--不在 false --在
	 */
	public boolean judgeUserIsInTheAlbum(int userid, int groupid) {
		List<Record> judge = Db
				.find("select * from groupmembers where gmgroupid=" + groupid + " and gmuserid=" + userid + " ");
		return judge.size() == 0;
	}

	/**
	 * 更新空间内所有成员有新动态
	 * 
	 * @param groupid
	 */
	public void UpdateAllGroupMembersNewDynamic(String groupid) {
		Db.update("update groupmembers set gmnotify=1 where gmgroupid =" + groupid + " ");
	}

	/**
	 * 更新空间内当个成员无新动态
	 * 
	 * @param groupid
	 * @param userid
	 */
	public void UpdateSingleGroupMenberNoNewDynamic(String groupid, String userid) {
		Db.update("update groupmembers set gmnotify=0 where gmgroupid =" + groupid + " and gmuserid = " + userid + " ");
	}

	/**
	 * 获取空间内所有成员的ID
	 * 
	 * @param groupid
	 * @return
	 */
	public List<Record> GetAllUseridInTheSpace(String groupid) {
		List<Record> list = Db
				.find("select gmuserid from groupmembers where gmgroupid = " + groupid + " and gmstatus=0 ");
		return list;
	}

	/**
	 * 获取空间内除当前用户外的所有成员的ID
	 * 
	 * @param groupid
	 * @return
	 */
	public List<Record> GetAllUseridInTheSpaceExceptCurrentUser(String groupid, String userid) {
		List<Record> list = Db.find("select gmuserid from groupmembers where gmgroupid = " + groupid
				+ " and gmuserid !=" + userid + " and gmstatus=0 ");
		return list;
	}

	/**
	 * 获取空间内除当前用坏所有成员的openID
	 */
	public static List<Record> GetAllUserOpenIDInTheSpaceExceptCurrentUser(String groupid, String userid) {
		List<Record> list = Db.find("select uopenid from users,groupmembers where userid=gmuserid and gmgroupid = "
				+ groupid + " and gmuserid !=" + userid + " and gmIsPush=1 and uopenid IS NOT NULL and gmstatus=0 ");
		return list;
	}
	
	/**
	 * 获取空间内除当前用坏所有成员的openID和formid
	 */
	public static List<Record> GetAllUserOpenIDAndFormID(String groupid,String userid) {
		List<Record> list = Db.find("select uopenid,userid from users,groupmembers where userid=gmuserid and userid=userID and gmgroupid = "
				+ groupid + " and uopenid IS NOT NULL and gmstatus=0 and userid<> "+userid);
		return list;
	}
	

}
