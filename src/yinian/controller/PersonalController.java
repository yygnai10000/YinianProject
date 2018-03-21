package yinian.controller;

import java.util.List;

import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Record;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.interceptor.CrossDomain;
import yinian.service.PersonalService;
import yinian.utils.JsonData;

public class PersonalController extends Controller {
	private String jsonString; // 返回的json字符串
	private JsonData jsonData = new JsonData(); // json操作类
	/*
	 * 用户个人云相册上传图片
	 */
	@Before(CrossDomain.class)
	public void SetUserPublishList() throws Exception {
		jsonString = jsonData.getJson(2, "请求参数错误");
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String picAddress=this.getPara("picAddress")==null?"":this.getPara("picAddress");
		String md5=this.getPara("md5")==null?"":this.getPara("md5");
		String createTime=this.getPara("createTime")==null?"":this.getPara("createTime");
		if(!userid.equals("")&&!picAddress.equals("")&&!md5.equals("")&&!createTime.equals("")){
			PersonalService service=new PersonalService();
			List<Record> list=service.insertPhoto(userid, picAddress, md5, createTime, createTime, 0);
			if(!list.isEmpty()){
				boolean hasRepeat = false;//是否含有重复上传的图片
				for(Record r:list) {
					if(r.getBoolean("isRepeat")) {
						hasRepeat = true;
					}
					r.remove("isRepeat");
				}
				if(hasRepeat) {
					jsonString = jsonData.getJson(0, "successWihRepeat", list);		
				}else {
					jsonString = jsonData.getSuccessJson(list);		
				}
			}
		}
		renderText(jsonString);
		//String mode=this.getPara("mode");
	}
	/*
	 * 获取用户个人云相册图片
	 */
	@Before(CrossDomain.class)
	public void GetUserPublushList()throws Exception{
		jsonString = jsonData.getJson(2, "请求参数错误");
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String mode = this.getPara("mode")==null?"day":this.getPara("mode");
		PersonalService service=new PersonalService();
		if(!userid.equals("")){
			jsonString = jsonData.getSuccessJson(service.findListByUserIdAndMode(userid, mode));
		}
		renderText(jsonString);
	}
	/*
	 * pc端获取用户个人云相册图片
	 */
	@Before(CrossDomain.class)
	public void PcGetUserPublushList()throws Exception{
		jsonString = jsonData.getJson(2, "请求参数错误");
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String mode = this.getPara("mode")==null?"day":this.getPara("mode");
		String endDate = this.getPara("endDate")==null?"":this.getPara("endDate");
		PersonalService service=new PersonalService();
		if(!userid.equals("")){
			jsonString = jsonData.getSuccessJson(service.pcFindListByUserIdAndMode(userid, mode,endDate));
		}
		renderText(jsonString);
	}
	/*
	 * pc端获取用户个人云相册图片,按上传时间
	 */
	@Before(CrossDomain.class)
	public void PcGetUserPublushListByUploadTime()throws Exception{
		jsonString = jsonData.getJson(2, "请求参数错误");
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String mode = this.getPara("mode")==null?"day":this.getPara("mode");
		String endDate = this.getPara("endDate")==null?"":this.getPara("endDate");
		PersonalService service=new PersonalService();
		if(!userid.equals("")){
			jsonString = jsonData.getSuccessJson(service.pcFindListByUserIdAndModeAndUploadtime(userid, mode,endDate));
		}
		renderText(jsonString);
	}
	/**
	 * 删除用户上传的图片(可批量)
	 * @throws Exception
	 */
	@Before(CrossDomain.class)
	public void DeleteUserPublushList()throws Exception{
		jsonString = jsonData.getJson(2, "请求参数错误");
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String aid = this.getPara("aid")==null?"":this.getPara("aid");
		PersonalService service=new PersonalService();
		if(!userid.equals("")&&!aid.equals("")){
			if(service.isCanDel(userid, aid)){
				if(service.deletePhoto(aid,userid)){
					jsonString = jsonData.getSuccessJson();
				}
			}
		}
		renderText(jsonString);
	}
	/*
	 * 根据用户id和图片id获取图片原图地址和压缩地址（单张）
	 */
	@Before(CrossDomain.class)
	public void GetUserPhotoAddress()throws Exception{
		jsonString = jsonData.getJson(2, "请求参数错误");
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String aid = this.getPara("aid")==null?"":this.getPara("aid");
		PersonalService service=new PersonalService();
		if(!userid.equals("")&&!aid.equals("")){
			jsonString = jsonData.getSuccessJson(service.getUserPhotoById(userid, aid));
		}
		renderText(jsonString);
	}
	/*
	 * 根据用户id和图片id设图片置备注信息
	 */
	@Before(CrossDomain.class)
	public void SetUserPhotoRemark() throws Exception{
		jsonString = jsonData.getJson(2, "请求参数错误");
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String aid = this.getPara("aid")==null?"":this.getPara("aid");
		String remark = this.getPara("remark")==null?"":this.getPara("remark");
		PersonalService service = new PersonalService();
		if(!userid.equals("")&&!aid.equals("")){
			jsonString = jsonData.getSuccessJson(service.setPhotoRemark(userid, aid, remark));
		}
		renderText(jsonString);
	}
	/*  批量下载使用
	 * 根据用户id和图片id获取图片原图地址（多张）
	 */
	@Before(CrossDomain.class)
	public void GetUserPhotoDownLoadAddress()throws Exception{
		jsonString = jsonData.getJson(2, "请求参数错误");
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String aid = this.getPara("aid")==null?"":this.getPara("aid");
		PersonalService service=new PersonalService();
		if(!userid.equals("")&&!aid.equals("")){
			jsonString = jsonData.getSuccessJson(service.getUserPhotoAddressById(userid, aid));
		}
		renderText(jsonString);
	}
	/**
	 * 上传短视频
	 */
	@Before(CrossDomain.class)
	public void UploadShortVideo() {
		YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类
		String userid = this.getPara("userid");
		String address = this.getPara("address");
		String md5 = this.getPara("md5")==null?"":this.getPara("md5");
		String createTime = this.getPara("createTime")==null?"":this.getPara("createTime");
		String cover = this.getPara("cover");
		String storage = this.getPara("storage");
		//String time = this.getPara("time");
		//String groupid = this.getPara("groupid");
		//String content = this.getPara("content");
		//String storage = this.getPara("storage");
		//String place = this.getPara("place");
		//String source = this.getPara("source");

		// 判断存储空间是否有传
		//double storagePlace = ((storage == null || storage.equals("")) ? 0.00 : Double.parseDouble(storage));
		cover = (cover == null ? "" : cover);
		//time = (time == null ? "0" : time);

		// 资源地址加前缀
		address = CommonParam.qiniuPrivateAddress + address;
		// 视频鉴黄,视频封面图片鉴黄true为色情视频
		boolean videoJudge = dataProcess.VideoVerify(address);
		boolean coverJudge = false;
		if(!cover.equals(""))
		 coverJudge = dataProcess.SinglePictureVerify(cover);
		
		if (videoJudge || coverJudge) {
			jsonString = jsonData.getJson(1039, "资源违规");
		} else {
			if(!userid.equals("")&&!address.equals("")&&!createTime.equals("")){
				PersonalService service=new PersonalService();
				List<Record> list=service.insertShortVedio(userid, address, md5, createTime, cover, storage, 4);
				if(!list.isEmpty()){
					jsonString = jsonData.getSuccessJson(list);		
				}
			}
			// 支持同时上传到多个空间
		/*	String[] IDs = groupid.split(",");
			boolean flag = true;
			int eventID = 0;
			// 逐个空间上传
			for (int i = 0; i < IDs.length; i++) {
				// 同步标记,0--非同步 1--同步 ,第一个空间为原创，其他为同步
				int isSynchronize = (i == 0 ? 0 : 1);
				// 上传短视频
				int eid = TxService.uploadShortVedio(userid, IDs[i], address, content, storagePlace, place, cover, time,
						isSynchronize, source);
				eventID = eid;
				if (eid == 0) {
					flag = false;
					break;
				}
			}
			if (flag) {
				// 说明上传成功
				List<Record> result = service.getSingleEvent(eventID, source);// 获取动态的信息
				jsonString = jsonData.getSuccessJson(result);
			} else {
				jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
			}*/
		}

		// 返回结果
		renderText(jsonString);
	}
//	/**
//	 * 创建个人云相册
//	 */
//	@Before(CrossDomain.class)
//	public void createPersonalAlbum() throws Exception{
//		String userid = this.getPara("userid")==null?"":this.getPara("userid");
//		String pename = this.getPara("pename")==null?"":this.getPara("pename");
//		String pepicAddress = this.getPara("pepicAddress")==null?"":this.getPara("pepicAddress");
//		
//		if(!userid.equals("")&&!pename.equals("")&&!pepicAddress.equals("")) {
//			PersonalService service = new PersonalService();
//			jsonString = jsonData.getSuccessJson(service.insertPersonalAlbum(userid, pename, pepicAddress));
//		}
//		
//		renderText(jsonString);
//	}
	/**
	 * 根据用户id获取相册信息
	 */
	@Before(CrossDomain.class)
	public void getPersonalAlbum() throws Exception{
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		
		if(!userid.equals("")) {
			PersonalService service = new PersonalService();
			jsonString = jsonData.getSuccessJson(service.getPersonalAlbumInfo(userid));
		}
		
		renderText(jsonString);
	}
	/**
	 * 根据用户 id修改相册名
	 */
	@Before(CrossDomain.class)
	public void updatePersonalAlbumName() throws Exception{
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String pename = this.getPara("pename")==null?"":this.getPara("pename");
		
		if(!userid.equals("")) {
			PersonalService service = new PersonalService();
			jsonString = jsonData.getSuccessJson(service.setPersonalAlbumName(userid, pename));
		}
		
		renderText(jsonString);
	}
	/**
	 * 根据用户 id修改相册封面
	 */
	@Before(CrossDomain.class)
	public void updatePersonalAlbumPic() throws Exception{
		String userid = this.getPara("userid")==null?"":this.getPara("userid");
		String pepic = this.getPara("pepic")==null?"":this.getPara("pepic");
		
		if(!userid.equals("")) {
			PersonalService service = new PersonalService();
			jsonString = jsonData.getSuccessJson(service.setPersonalAlbumPic(userid, pepic));
		}
		
		renderText(jsonString);
	}
}
