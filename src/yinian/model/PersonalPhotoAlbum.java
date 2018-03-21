package yinian.model;

import java.util.Date;

import com.jfinal.plugin.activerecord.Model;

public class PersonalPhotoAlbum extends Model<PersonalPhotoAlbum>{
	public static final PersonalPhotoAlbum dao = new PersonalPhotoAlbum();
	public static String insert(String userid,String proiginal
			,String md5,String createtime,
			String createmonth,Date uploadtime,int status, String cover, String storage, int type){
		PersonalPhotoAlbum ppa = new PersonalPhotoAlbum();
		ppa.set("userid", userid)
		.set("poriginal", proiginal)
		.set("md5", md5)
		.set("createtime", createtime)
		.set("createmonth", createmonth)
		.set("uploadtime", uploadtime)
		.set("status", status)
		.set("cover", cover)
		.set("storage", storage==""?0:storage)
		.set("type", type);
//		ppa.set("userid", 1)
//		.set("proiginal", 2)
//		.set("md5", 3)
//		.set("createtime", "2017-01-01 11:11:11")
//		.set("createmonth", "2017-01-01 11:11:11")
//		.set("uploadtime", "2017-01-01 11:11:11")
//		.set("status", 0);
		ppa.save();
		return ppa.getLong("aid").toString();
	}
}
