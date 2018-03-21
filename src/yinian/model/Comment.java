package yinian.model;

import java.util.ArrayList;
import java.util.List;

import yinian.utils.JsonData;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.ehcache.CacheKit;

public class Comment extends Model<Comment> {
	public static final Comment dao = new Comment();
	private static String selectForComment = "SELECT cid,ceid,A.userid cuid ,A.unickname cunickname,A.upic cpic , B.userid ruid , B.unickname runickname, B.upic rpic ,ccontent,ctime,cplace";

	/**
	 * 获取单条动态的评论
	 * 
	 * @param eid
	 * @return
	 */
	public List<Record> GetSingleEventComments(int eid) {
		List<Record> list = Db.find(selectForComment
				+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and eid="
				+ eid + " and cstatus=0 ORDER BY ceid,ctime asc");
		return list;
	}

	/**
	 * 获取空间内的全部评论
	 * 
	 * @param groupid
	 * @return
	 */
	public List<Record> GetAllCommentsOfOneSpace(String groupid) {
		List<Record> list = new ArrayList<Record>();
		if (!groupid.equals("1065266")) {
			list = Db.find(selectForComment
					+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and egroupid="
					+ groupid + " and cstatus=0 ORDER BY ceid,ctime asc");
		}

		return list;
	}
	/**
	 * 获取空间内的全部评论 by lk
	 * 
	 * @param groupid
	 * @return
	 */
	public List<Record> GetAllCommentsOfOneSpaceByLk(String groupid,String eids) {
		List<Record> list = new ArrayList<Record>();
		if (!groupid.equals("1065266")) {
			list = Db.find(selectForComment
					+ " from users A,users B,comments,events where egroupid="
					+ groupid + " and cstatus=0  and eid in ( "+eids+" ) and ceduserid=B.userid and A.userid=cuserid and eid=ceid  ORDER BY ceid,ctime asc");
		}

		return list;
	}
	/**
	 * 获取动态列表内的评论
	 */
	public static List<Record> GetCommentsInEventList(List<Record> eventList) {

		String eid = "";
		for (Record record : eventList) {
			String temp = record.get("eid").toString();
			eid += (temp + ",");
		}
		List<Record> commentsList = new ArrayList<Record>();
		if (!eid.equals("")) {
			eid = eid.substring(0, eid.length() - 1);
			commentsList = Db.find(selectForComment
					+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and eid in ("
					+ eid + ") and cstatus=0 ORDER BY ceid,ctime asc");
		}

		return commentsList;
	}

	/**
	 * 获取用户上传的动态中的评论
	 */
	public static List<Record> GetCommentsOfUserUploadEvents(String userid) {
		List<Record> list = Db.find(selectForComment
				+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and euserid ='"
				+ userid + "' and cstatus=0 ORDER BY ceid,ctime asc ");
		return list;
	}

	/**
	 * 获取空间成员上传的动态中的评论
	 */
	public static List<Record> GetCommentsOfSpaceMemberUploadEvents(int userid, int groupid) {
		List<Record> list = Db.find(selectForComment
				+ " from users A,users B,comments,events where ceduserid=B.userid and A.userid=cuserid and eid=ceid and egroupid= '"
				+ groupid + "' and euserid = '" + userid + "' and cstatus=0 ORDER BY ceid,ctime asc ");
		return list;
	}
	/**
	 * 获取动态的评论  Comment.class
	 */
	public List<Record> GetEventComment(String eid,String cid,String type) {
		List<Record> list = new ArrayList<Record>();
		if(type.equals("initialize")) {
			list = Db.find(selectForComment
					+ " from users A,users B,comments,events where cstatus=0  and eid="+eid+" and ceduserid=B.userid and A.userid=cuserid and eid=ceid  ORDER BY cid desc limit 10");
		}else {
			list = Db.find(selectForComment
					+ " from users A,users B,comments,events where cstatus=0  and eid="+eid+" and cid<"+cid+
					" and ceduserid=B.userid and A.userid=cuserid and eid=ceid  ORDER BY cid desc limit 10");
		}
		
		return list;
	}
}
