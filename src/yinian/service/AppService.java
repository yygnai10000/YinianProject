package yinian.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jfinal.plugin.activerecord.Record;

import yinian.app.YinianDataProcess;
import yinian.model.Event;
import yinian.model.Like;
import yinian.model.Picture;
import yinian.utils.QiniuOperate;

public class AppService {
	/**
	 * 获取点赞排行
	 * 
	 * @param groupid
	 * @param uid
	 * @param searchLimit
	 * @return
	 */
	public List<Record> GetListByElikeAndGroup(int groupid, int uid, int searchLimit) {
		List<Record> eventList = new Event().GetListByGroup(groupid, searchLimit);// 获取动态
		YinianDataProcess ydp=new YinianDataProcess();
		String ids = "";
		for (Record r : eventList) {
			// r.set("pictures", null);
			ids += r.getLong("eid") + ",";
		}
		if (ids.length() > 0) {
			ids = ids.substring(0, ids.length() - 1);
		}
		// 获取每个动态的图片
		List<Record> allPictureList = new Picture().GetByEids(ids);
		Map<Integer, List<Record>> map = new HashMap<>();
		for (Record r : allPictureList) {
			int eid = r.getLong("peid").intValue();
			QiniuOperate q = new QiniuOperate();
			// String url = q.getDownloadToken(r.getStr("poriginal") +
			// "?imageView2/2/w/200");
			// r.set("thumbImg", url);
			if(r.get("poriginal").toString().indexOf(".mp4")!=-1||r.get("poriginal").toString().indexOf(".MP4")!=-1
					||r.get("poriginal").toString().indexOf(".3gp")!=-1||r.get("poriginal").toString().indexOf(".3GP")!=-1
					||r.get("poriginal").toString().indexOf(".avi")!=-1||r.get("poriginal").toString().indexOf(".AVI")!=-1
					||r.get("poriginal").toString().indexOf(".flv")!=-1||r.get("poriginal").toString().indexOf(".FLV")!=-1
					||r.get("poriginal").toString().indexOf(".MKV")!=-1||r.get("poriginal").toString().indexOf(".mkv")!=-1){
				String pcover=ydp.getVideoCover(r.get("poriginal").toString(),"4");
				r.set("thumbnail", pcover);
				// 中等缩略图授权
				r.set("midThumbnail", pcover);
				// 原图授权
				r.set("poriginal", pcover);
			}else{
				// 缩略图授权
				r.set("thumbnail", q.getDownloadToken(r.get("poriginal").toString() + "?imageView2/2/w/250"));
				// 中等缩略图授权
				r.set("midThumbnail", q.getDownloadToken(r.get("poriginal").toString() + "?imageView2/2/w/500"));
				// 原图授权
				r.set("poriginal", q.getDownloadToken(r.get("poriginal").toString()));
			}
			if (null != map.get(eid)) {
				map.get(eid).add(r);
			} else {
				List<Record> rlist = new ArrayList<Record>();
				rlist.add(r);
				map.put(eid, rlist);
			}
		}
		// 获取每个动态自己的点赞情况
		List<Record> userLikeList = new Like().GetCountListByEventidAndUid(ids, uid);
		Map<Integer, List<Record>> likemap = new HashMap<>();
		for (Record r : userLikeList) {
			int eid = r.getLong("likeEventID").intValue();
			if (null != likemap.get(eid)) {
				likemap.get(eid).add(r);
			} else {
				List<Record> rlist = new ArrayList<Record>();
				rlist.add(r);
				likemap.put(eid, rlist);
			}
		}
		for (Record r : eventList) {
			if(r.get("eMain").toString().equals("4")){
				r.set("eMain", 0);
			}
			r.set("pictures", map.get(r.getLong("eid").intValue()));
			r.set("like", likemap.get(r.getLong("eid").intValue()));
		}
		return eventList;
		// returnList.set("", value)

	}
}
