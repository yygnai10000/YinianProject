package yinian.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

public class TextLibrary extends Model<TextLibrary> {
	public static final TextLibrary dao = new TextLibrary();

	/**
	 * 获取所有文字类型
	 * 
	 * @return
	 */
	public List<Record> GetAllTextType() {
		List<Record> list = Db
				.find("select distinct textType from textLibrary where textStatus=0 ");
		return list;
	}

	/**
	 * 获取单个文字类型的文字,分页查询
	 * 
	 * @param textType
	 * @return
	 */
	public List<Record> GetTextOfOneTextType(String textType, int id) {
		List<Record> list;
		if (id <= 0) {
			list = Db
					.find("select textID,textContent,textSource from textLibrary where textStatus=0 and textType='"
							+ textType + "' limit 20 ");
		} else {
			list = Db
					.find("sselect textID,textContent,textSource from textLibrary where textStatus=0 and textType='"
							+ textType + "' and textID<" + id + " limit 20 ");
		}
		return list;
	}

}
