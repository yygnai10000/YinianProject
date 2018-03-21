package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class FormID extends Model<FormID> {
	public static final FormID dao = new FormID();

	/**
	 * ≤Â»ÎformID
	 * 
	 * @param userid
	 * @param formID
	 */
	public static void insertFormID(String userid, String formID) {
		if (formID != null && !formID.equals("")) {
			List<Record> list = Db.find("select id from formid where userID=" + userid + " ");
			if (list.size() == 0) {
				FormID id = new FormID();
				id.set("userID", userid).set("formID", formID);
				id.save();
			} else {
				String id = list.get(0).get("id").toString();
				FormID form = dao.findById(id);
				form.set("formID", formID).set("status", 0);
				form.update();
			}
		}
	}
	public static void insert(String userid,String formID){
		FormID id = new FormID();
		id.set("userID", userid).set("formID", formID);
		id.save();
	}
}
