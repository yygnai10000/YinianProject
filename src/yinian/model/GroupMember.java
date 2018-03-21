package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class GroupMember extends Model<GroupMember> {
	public static final GroupMember dao = new GroupMember();

	/**
	 * ��������Ա
	 * 
	 * @param userid
	 * @param groupid
	 * @return
	 */
	public boolean AddGroupMember(int userid, int groupid) {
		boolean flag = false;
		GroupMember gm = new GroupMember().set("gmgroupid", groupid).set("gmuserid", userid);
		flag = gm.save();
		// ���·���������Ա�����ֶ�
		int count = Db.update("update groups set gnum = gnum+1 where groupid='" + groupid + "' ");
		return (flag && (count == 1));
	}

	/**
	 * �ж��û��Ƿ��������
	 * 
	 * @param userid
	 * @param groupid
	 * @return true--���� false --��
	 */
	public boolean judgeUserIsInTheAlbum(int userid, int groupid) {
		List<Record> judge = Db
				.find("select * from groupmembers where gmgroupid=" + groupid + " and gmuserid=" + userid + " ");
		return judge.size() == 0;
	}

	/**
	 * ���¿ռ������г�Ա���¶�̬
	 * 
	 * @param groupid
	 */
	public void UpdateAllGroupMembersNewDynamic(String groupid) {
		Db.update("update groupmembers set gmnotify=1 where gmgroupid =" + groupid + " ");
	}

	/**
	 * ���¿ռ��ڵ�����Ա���¶�̬
	 * 
	 * @param groupid
	 * @param userid
	 */
	public void UpdateSingleGroupMenberNoNewDynamic(String groupid, String userid) {
		Db.update("update groupmembers set gmnotify=0 where gmgroupid =" + groupid + " and gmuserid = " + userid + " ");
	}

	/**
	 * ��ȡ�ռ������г�Ա��ID
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
	 * ��ȡ�ռ��ڳ���ǰ�û�������г�Ա��ID
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
	 * ��ȡ�ռ��ڳ���ǰ�û����г�Ա��openID
	 */
	public static List<Record> GetAllUserOpenIDInTheSpaceExceptCurrentUser(String groupid, String userid) {
		List<Record> list = Db.find("select uopenid from users,groupmembers where userid=gmuserid and gmgroupid = "
				+ groupid + " and gmuserid !=" + userid + " and gmIsPush=1 and uopenid IS NOT NULL and gmstatus=0 ");
		return list;
	}
	
	/**
	 * ��ȡ�ռ��ڳ���ǰ�û����г�Ա��openID��formid
	 */
	public static List<Record> GetAllUserOpenIDAndFormID(String groupid,String userid) {
		List<Record> list = Db.find("select uopenid,userid from users,groupmembers where userid=gmuserid and userid=userID and gmgroupid = "
				+ groupid + " and uopenid IS NOT NULL and gmstatus=0 and userid<> "+userid);
		return list;
	}
	

}
