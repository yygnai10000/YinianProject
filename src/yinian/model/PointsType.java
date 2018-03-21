package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class PointsType extends Model<PointsType>{
	public static final PointsType dao = new PointsType();
	
	/**
	 * ²éÑ¯»ý·Ö×´Ì¬
	 */
	public List<Record> getPointsTypeInfo(){
		List<Record> list = Db.find("select * from pointsType");
		return list;
	}
}
