package yinian.model;

import java.util.Date;

import com.jfinal.plugin.activerecord.Model;

public class PersonalAlbum extends Model<PersonalAlbum>{
	public static final PersonalAlbum dao = new PersonalAlbum();
	public static String insert(String userid, String pename, Date petime, String pepic){
		PersonalAlbum pa = new PersonalAlbum();
		pa.set("userid", userid)
		.set("pename", pename)
		.set("petime", petime)
		.set("pepic", pepic)
		.set("pestatus", 0);
		pa.save();
		return pa.getLong("peid").toString();
	}
}
