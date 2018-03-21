package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class InviteList extends Model<InviteList> {
	public static final InviteList dao = new InviteList();

	/**
	 * ≈–∂œ «∑Ò“—æ≠—˚«Î true -- “——˚«Î false --Œ¥—˚«Î
	 */
	public static boolean JudgeIsInvite(String invite, String beInvite) {
		List<Record> list = Db
				.find("select * from inviteList where inviteUserid=" + invite
						+ " and beInvitedUserid=" + beInvite + " ");
		return !(list.size() == 0);
	}

	/**
	 * ≤Â»Î—˚«Î
	 */
	public static boolean InsertInviteInfo(String invite, String beInvite) {
		InviteList in = new InviteList().set("inviteUserid", invite).set(
				"beInvitedUserid", beInvite);
		return in.save();
	}

}
