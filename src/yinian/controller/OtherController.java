package yinian.controller;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

public class OtherController extends Controller {

	//
	public void Test() {
		List<Record> temp = Db.find("select GROUP_CONCAT(groupid) as num from groups where gsource='Ð¡³ÌÐò' and gstatus=0 and gOrigin=0 and gtime >= '2017-01-11'");
		String sql = "SELECT groupid,DATE(euploadtime) as euploadtime FROM groups,`events` WHERE groupid = egroupid AND groupid IN ("+temp.get(0).getStr("num")+") AND DATE(euploadtime) >= '2017-01-11'";
		
		List<Record> list = Db.find(sql);
		Set<String> set = new HashSet<String>();
		for (int i = 0; i < list.size(); i++) {
			String groupid = list.get(i).get("groupid").toString();
			String euploadtime = list.get(i).get("euploadtime").toString();
			for (int j = i + 1; j < list.size(); j++) {
				String groupid1 = list.get(j).get("groupid").toString();
				String euploadtime1 = list.get(j).get("euploadtime").toString();
				if (groupid.equals(groupid1)
						&& !(euploadtime.equals(euploadtime1))) {
					set.add(groupid);
					break;
				}
			}
		}
		Iterator<String> it = set.iterator();
		String result = "";
		while (it.hasNext()) {
			result += it.next() + ",";
		}
		System.out.println(set.size());
		System.out.println(result);
	}

}
