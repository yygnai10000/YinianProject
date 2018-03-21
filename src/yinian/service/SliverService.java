package yinian.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.utils.JsonData;
import yinian.utils.QiniuOperate;

public class SliverService {

	private String jsonString;// 返回结果
	private JsonData jsonData = new JsonData(); // json操作类
	private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类
	private QiniuOperate operate = new QiniuOperate();

	/**
	 * 获取好友照片
	 */
	public List<Record> GetFriendPhoto(int num, int beUserid) {
		Set<Integer> key = new HashSet<Integer>();
		List<Record> userPhoto = Db
				.find("select address,shootTime from userPhoto where userid="
						+ beUserid + " and status=0 order by shootTime desc ");
		int size = userPhoto.size();

		// 处理图片,按年划分
		List<Record> photoList = new ArrayList<Record>();
		Record tempRecord = new Record();
		List<String> addressList = new ArrayList<String>();
		if (size != 0) {
			String temp = userPhoto.get(0).get("shootTime").toString()
					.substring(0, 4);
			for (Record record : userPhoto) {
				String address = record.get("address").toString();
				String year = record.get("shootTime").toString()
						.substring(0, 4);
				if (year.equals(temp)) {
					addressList.add(address);
					tempRecord.set("year", year).set("photo", addressList);
				} else {
					photoList.add(tempRecord);
					addressList = new ArrayList<String>();
					addressList.add(address);
					tempRecord = new Record().set("year", year).set("photo",
							addressList);
					temp = year;
				}
			}
			photoList.add(tempRecord);
		}

		for (Record record : photoList) {
			List<String> photo = record.get("photo");
			record.remove("photo");
			record.set("num", photo.size());
		}

		// // 图片数组
		// String[] picArray = new String[num];
		// int count = 0;
		//
		// if (size < num) {
		// for (int i = 0; i < size; i++) {
		// picArray[i] = operate.getDownloadToken(userPhoto.get(i)
		// .get("address").toString());
		// count++;
		// }
		// for (int i = 0; i < num - size; i++) {
		// picArray[count] = CommonParam.promptPhotoInPeep;
		// count++;
		// }
		// } else {
		// for (int i = 0; i < num; i++) {
		// int random;
		// while (true) {
		// random = (int) (Math.random() * (size - 1));
		// if (!key.contains(random)) {
		// break;
		// }
		// }
		// key.add(random);
		// String address = operate.getDownloadToken(userPhoto.get(random)
		// .get("address").toString());
		// picArray[i] = address;
		// }
		// }

		return photoList;
	}
}
