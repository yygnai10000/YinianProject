package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class IndustryPhoto extends Model<IndustryPhoto> {
	public static final IndustryPhoto dao = new IndustryPhoto();

	/**
	 * 获取所有行业类型
	 * 
	 * @return
	 */
	public static List<Record> getAllIndustryType() {
		List<Record> list = Db
				.find("select distinct industry from industryPhoto where status=0 ");
		return list;
	}

}
