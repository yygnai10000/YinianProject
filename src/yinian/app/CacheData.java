package yinian.app;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.ehcache.CacheKit;

public class CacheData {

	public static Set<String> GetAllSpaceInviteCode() {
		Set<String> inviteCodeSet = CacheKit.get("ServiceCache",
				"inviteCodeSet");
		if (inviteCodeSet == null) {
			inviteCodeSet = new HashSet<String>();
			List<Record> list = Db.find("select distinct ginvite from groups ");
			for (Record record : list) {
				inviteCodeSet.add(record.getStr("ginvite"));
			}
			CacheKit.put("ServiceCache", "inviteCodeSet", inviteCodeSet);
		}
		return inviteCodeSet;
	}

}
