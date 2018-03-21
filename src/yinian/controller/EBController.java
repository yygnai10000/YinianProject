package yinian.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import sun.misc.BASE64Decoder;
import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.interceptor.CrossDomain;
import yinian.model.Address;
import yinian.model.Contact;
import yinian.model.Coupon;
import yinian.model.EBOrder;
import yinian.model.Item;
import yinian.model.Receive;
import yinian.model.Redpacket;
import yinian.service.EBService;
import yinian.utils.DES;
import yinian.utils.JsonData;
import yinian.utils.QiniuOperate;
import yinian.model.Cart;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.jfinal.kit.JsonKit;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.UploadManager;

@Before(CrossDomain.class)
public class EBController extends Controller {

	private String jsonString; // 返回的json字符串
	private JsonData jsonData = new JsonData(); // json操作类
	private YinianDataProcess dataProcess = new YinianDataProcess();// 数据处理类
	private EBService service = new EBService(); // 电商业务类
	private EBService TxService = enhance(EBService.class);

	public void test() {
		String isMail = this.getPara("isMail");
		if (isMail != null && isMail.equals("yes")) {
			System.out.println("haha");
		}
	}

	/**
	 * 显示商品列表
	 */
	public void ShowGoodsList() {
		List<Record> list = Db
				.find("select ebGoodsID,ebGoodsName,ebGoodsPrice,ebGoodsOrigin,ebGoodsUnit,ebGoodsIntroduce,ebGoodsPic from ebgoods where ebGoodsStatus=0 ");
		jsonString = jsonData.getJson(0, "success", list);
		renderText(jsonString);
	}

	/**
	 * 联系客服
	 */
	public void ContactCustomerService() {
		String userid = this.getPara("userid");
		String content = this.getPara("content");
		String name = this.getPara("name");
		String wechatID = this.getPara("wechatID");
		String email = this.getPara("email");
		String phonenumber = this.getPara("phonenumber");
		Contact contact = new Contact().set("contactUserID", userid)
				.set("contactContent", content).set("contactName", name)
				.set("contactWechatID", wechatID).set("contactEmail", email)
				.set("contactPhoneNumber", phonenumber);
		jsonString = dataProcess.insertFlagResult(contact.save());
		renderText(jsonString);
	}

	/**
	 * 显示收货地址列表
	 */
	public void ShowAllAddress() {
		String userid = this.getPara("userid");
		List<Record> list = Db
				.find("select addressID,addressReceiver,addressPhoneNumber,addressProvince,addressCity,addressArea,addressDetail,isDefault from address where addressUserID="
						+ userid + " and addressStatus=0 ");
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 显示用户默认收货地址
	 */
	public void ShowUserFirstAddress() {
		String userid = this.getPara("userid");
		List<Record> list = Db
				.find("select addressID,addressReceiver,addressPhoneNumber,addressProvince,addressCity,addressArea,addressDetail from address where addressUserID="
						+ userid + " and addressStatus=0 and isDefault=1");
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 获取收货地址详情
	 */
	public void GetAddressInfo() {
		String addressID = this.getPara("addressID");
		List<Record> list = Db
				.find("select addressID,addressReceiver,addressPhoneNumber,addressProvince,addressCity,addressArea,addressDetail,isDefault from address where addressID="
						+ addressID + " ");
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 更换默认收货地址
	 */
	@Before(Tx.class)
	public void ChangeDefaultAddress() {
		String userid = this.getPara("userid");
		String addressID = this.getPara("addressID");
		Db.update("update address set isDefault=0 where addressUserID="
				+ userid + " ");
		int count = Db.update("update address set isDefault=1 where addressID="
				+ addressID + " ");
		jsonString = dataProcess.updateFlagResult(count == 1);
		renderText(jsonString);
	}

	/**
	 * 管理收货地址
	 */
	@Before(Tx.class)
	public void ManageAddress() {
		String userid = this.getPara("userid");
		String type = this.getPara("type");
		String addressID = this.getPara("addressID");
		String receiver = this.getPara("receiver");
		String phone = this.getPara("phone");
		String province = this.getPara("province");
		String city = this.getPara("city");
		String area = this.getPara("area");
		String detail = this.getPara("detail");

		Address address = new Address();
		switch (type) {
		case "add":
			address.set("addressUserID", userid)
					.set("addressReceiver", receiver)
					.set("addressPhoneNumber", phone)
					.set("addressProvince", province).set("addressCity", city)
					.set("addressArea", area).set("addressDetail", detail);
			// 判断用户是否有收货地址，没有的话该条地址为默认地址
			List<Record> judgeList = Db
					.find("select * from address where addressUserID=" + userid
							+ " and addressStatus=0 ");
			if (judgeList.size() == 0) {
				address.set("isDefault", 1);
			}
			if (address.save()) {
				List<Record> list = dataProcess.makeSingleParamToList(
						"addressID", address.get("addressID").toString());
				jsonString = jsonData.getSuccessJson(list);
			} else {
				jsonString = jsonData.getJson(-50, "插入数据失败");
			}
			break;
		case "modify":
			Address updateAddress = new Address().findById(addressID);
			updateAddress.set("addressReceiver", receiver)
					.set("addressPhoneNumber", phone)
					.set("addressProvince", province).set("addressCity", city)
					.set("addressArea", area).set("addressDetail", detail);
			jsonString = dataProcess.updateFlagResult(updateAddress.update());
			break;
		case "delete":
			Address deleteAddress = new Address().findById(addressID);
			deleteAddress.set("addressStatus", 1);
			// 判断这条被删除的地址是否为默认地址
			String isDefault = deleteAddress.get("isDefault").toString();
			if (isDefault.equals("1")) {
				// 判断用户是否有收货地址，有的话将第一条设置为默认地址
				List<Record> deleteJudgeList = Db
						.find("select * from address where addressUserID="
								+ userid + "  and addressStatus=0 ");
				if (deleteJudgeList.size() != 0) {
					String id = deleteJudgeList.get(0).get("addressID")
							.toString();
					Address address1 = new Address().findById(id);
					address1.set("isDefault", 1);
					address1.update();
				}
			}

			jsonString = dataProcess.updateFlagResult(deleteAddress.update());
			break;
		}
		renderText(jsonString);
	}

	/**
	 * 显示订单列表
	 */
	public void ShowOrderList() {
		String userid = this.getPara("userid");
		List<Record> orderList = Db
				.find("select ebOrderID,ebOrderNumber,ebOrderTotalNum,ebGoodsOrigin,ebOrderTotalPrice,ebOrderStatus,ebOrderLogisticsCompany,ebOrderLogisticsNumber,ebOrderPlaceTime,"
						+ "addressReceiver,addressPhoneNumber,addressProvince,addressCity,addressArea,addressDetail,ebGoodsID "
						+ "from address,eborders,ebgoods,items "
						+ "where ebOrderID=itemEBOrderID and itemEBGoodsID=ebGoodsID and ebOrderAddressID=addressID and ebOrderUserID="
						+ userid + " order by ebOrderID desc ");

		for (Record record : orderList) {
			String ebOrderID = record.get("ebOrderID").toString();
			List<Record> itemList = Db
					.find("select itemID,itemNum,ebGoodsID,ebGoodsName,ebGoodsEnName,ebGoodsPrice,ebGoodsUnit,ebGoodsPic "
							+ "from ebgoods,items where ebGoodsID=itemEBGoodsID and itemEBOrderID="
							+ ebOrderID + " ");
			record.set("item", itemList);
		}
		orderList = dataProcess.changeOrderStatusIntoWord(orderList);
		jsonString = jsonData.getSuccessJson(orderList);
		renderText(jsonString);
	}

	/**
	 * 添加购买商品
	 */
	@Before(Tx.class)
	public void AddItem() {
		String userid = this.getPara("userid");
		String goodsID = this.getPara("goodsID");
		String picture = this.getPara("picture");
		String audio = this.getPara("audio");
		String audioTime = this.getPara("audioTime");
		String from = this.getPara("from");
		String to = this.getPara("to");
		String content = this.getPara("content");
		String cover = this.getPara("cover");
		String result = this.getPara("result");

		// 图片处理
		if (!(picture.equals("")) && picture != null) {
			picture = dataProcess.EBPicStringProcess(picture);
		}
		Item item = new Item();
		switch (goodsID) {
		case "1":
			// 声音明信片
			// 语音处理
			audio = dataProcess.singleOpenResourcePrefix(audio);
			result = CommonParam.qiniuOpenAddress + result;
			item.set("itemEBGoodsID", goodsID).set("itemNum", 1)
					.set("itemPic", picture).set("itemAudio", audio)
					.set("itemText", content).set("itemFrom", from)
					.set("itemTo", to).set("itemCover", cover)
					.set("itemAudioTime", audioTime).set("itemResult", result);
			break;
		case "2":
			// lomo卡
			item.set("itemEBGoodsID", goodsID).set("itemNum", 1)
					.set("itemPic", picture).set("itemAudio", audio)
					.set("itemAudioTime", audioTime);
			break;
		case "3":
			// 图钉相纸
			item.set("itemEBGoodsID", goodsID).set("itemNum", 1)
					.set("itemPic", picture).set("itemAudio", audio)
					.set("itemAudioTime", audioTime);
			break;
		case "4":
			// 照片拼图
			item.set("itemEBGoodsID", goodsID).set("itemNum", 1)
					.set("itemPic", picture).set("itemAudio", audio)
					.set("itemAudioTime", audioTime);
			break;

		}
		if (item.save()) {
			String itemID = item.get("itemID").toString();
			List<Record> list = Db
					.find("select ebGoodsName,ebGoodsPrice,ebGoodsUnit,ebGoodsPic from ebgoods where ebGoodsID="
							+ goodsID + " ");
			// 计算优惠金额
			List<Record> coupon = Db
					.find("select couponID,couponLimit from coupon where couponLimit in (select max(couponLimit) from coupon where couponType="
							+ goodsID
							+ " and couponStatus=0 and couponUserID="
							+ userid
							+ ") and couponType="
							+ goodsID
							+ " and couponStatus=0 and couponUserID="
							+ userid
							+ " limit 1 ");
			if (coupon.size() == 0) {
				list.get(0).set("itemID", itemID).set("couponPrice", 0);
			} else {
				list.get(0)
						.set("itemID", itemID)
						.set("couponPrice",
								coupon.get(0).get("couponLimit").toString())
						.set("postage", 0);
				// 插入优惠券信息
				item.set("itemCouponID", coupon.get(0).get("couponID")
						.toString());
				item.update();
			}
			
			// 计算邮费
			if (goodsID.equals("1")) {
				list.get(0).set("postage", CommonParam.postcardMailFee);
			}

			jsonString = jsonData.getSuccessJson(list);
		} else {
			jsonString = jsonData.getJson(-50, "插入数据失败");
		}
		renderText(jsonString);
	}

	/**
	 * 下单
	 */
	@Before(Tx.class)
	public void PlaceOrder() {
		// 获取数据
		String userid = this.getPara("userid");
		String addressID = this.getPara("addressID");
		String itemID = this.getPara("itemID");
		String num = this.getPara("num");
		String goodsID = this.getPara("goodsID");// 非必要参数
		String isMail = this.getPara("isMail"); // 是否快递 两个值:yes,no
		// 获取对象
		Item item = new Item().findById(itemID);
		// 计算订单总价
		String goodsPrice = Db
				.findFirst(
						"select ebGoodsPrice from ebgoods where ebGoodsID="
								+ item.get("itemEBGoodsID").toString() + " ")
				.get("ebGoodsPrice").toString();
		double totalPrice = ((Double.parseDouble(goodsPrice)) * (Double
				.parseDouble(num)));
		if (isMail != null && isMail.equals("yes")) {
			// 总价中加上运费
			totalPrice += CommonParam.postcardMailFee;
		}

		// 获取优惠券价格
		String couponID = item.get("itemCouponID").toString();
		Double couponLimit = 0.00;
		if (!couponID.equals("0")) {
			Coupon coupon = new Coupon().findById(couponID);
			couponLimit = Double.parseDouble(coupon.get("couponLimit")
					.toString());
		}
		// 能否进行合成封底的判断标志，在第一次下单后，值改成true则需进行合成
		boolean canComposeFlag = false;
		String globalOrderNumber = null;
		// 判断是否已经下单，已下单的话，则不新增订单，只更改地址，数量，避免用户更改
		if (item.get("itemEBOrderID") == null) {
			EBOrder order = new EBOrder();

			// 获取订单号
			String orderNumber = service.createOrderNumber();
			// 保存订单
			order.set("ebOrderNumber", orderNumber)
					.set("ebOrderUserID", userid)
					.set("ebOrderAddressID", addressID)
					.set("ebOrderTotalNum", num)
					.set("ebOrderTotalPrice", totalPrice);
			if (isMail != null && isMail.equals("yes")) {
				// 订单中计入邮费和需要邮寄
				order.set("ebOrderPostFee", CommonParam.postcardMailFee).set(
						"ebOrderIsMail", 1);
			}
			if (order.save()) {
				String orderID = order.get("ebOrderID").toString();

				// *******判断商品类型是不是等于5，是则修改item内的字段，此地方是暂时的*******//
				if (goodsID != null && !(goodsID.equals(""))
						&& goodsID.equals("5")) {
					item.set("itemEBGoodsID", 5);
				}
				// ************************End*****************************//

				// 更新商品条目
				item.set("itemEBOrderID", orderID).set("itemNum", num);
				if (item.update()) {
					Record record = new Record().set("ebOrderID", orderID)
							.set("ebOrderTotalPrice", totalPrice)
							.set("ebOrderNumber", orderNumber)
							.set("couponLimit", couponLimit);

					// *************如果是LOMO卡，则发送网络请求，预先下载图片***********//
					if (goodsID != null && !(goodsID.equals(""))
							&& goodsID.equals("2")) {
						String itemPic = item.get("itemPic").toString();
						dataProcess.sentNetworkRequest(
								CommonParam.LomoCardPictureDownload, itemPic);
					}
					// **********************End*****************************//

					List<Record> result = new ArrayList<Record>();
					result.add(record);
					jsonString = jsonData.getSuccessJson(result);

					// 合成标志改为true
					canComposeFlag = true;
					globalOrderNumber = orderNumber;
				} else {
					jsonString = jsonData.getJson(-51, "更新数据失败");
				}
			} else {
				jsonString = jsonData.getJson(-50, "插入数据失败");
			}
		} else {

			String orderID = item.get("itemEBOrderID").toString();
			EBOrder order = new EBOrder().findById(orderID);
			String orderNumber = order.get("ebOrderNumber").toString();
			order.set("ebOrderAddressID", addressID)
					.set("ebOrderTotalNum", num)
					.set("ebOrderTotalPrice", totalPrice);
			if (order.update()) {
				Record record = new Record().set("ebOrderID", orderID)
						.set("ebOrderTotalPrice", totalPrice)
						.set("ebOrderNumber", orderNumber)
						.set("couponLimit", couponLimit);
				List<Record> temp = new ArrayList<Record>();
				temp.add(record);
				jsonString = jsonData.getSuccessJson(temp);
			} else {
				jsonString = jsonData.getJson(-51, "更新数据失败");
			}
		}
		renderText(jsonString);

		// 数据返回后，如果是声音明信片，进行封底的合成
		String itemEBGoodsID = item.get("itemEBGoodsID").toString();
		if (itemEBGoodsID.equals("1") && canComposeFlag) {
			service.ComposePostcardBottom(itemID, globalOrderNumber);
		}
	}

	/**
	 * 购物车结算
	 */
	public void BuyGoodsByCart() {
		// 获取参数
		String userid = this.getPara("userid");
		String addressID = this.getPara("addressID");
		String itemInfo = this.getPara("itemInfo");
		String isMail = this.getPara("isMail");// 是否快递 两个值:yes,no,明信片才有的值

		// 解析item并获取相应的List<Record>对象
		List<Record> itemList = service.parseItemInfoString(itemInfo);

		// 计算订单总价,商品总数
		Record totalRecord = service
				.getTotalNumAndGoodsPriceFromItemList(itemList);
		double totalPrice = totalRecord.getDouble("totalPrice");
		int totalNum = totalRecord.getInt("totalNum");

		// 计算是否增加运费
		if (isMail != null && isMail.equals("yes")) {
			totalPrice += CommonParam.postcardMailFee;
		}

		// 获取订单号
		String orderNumber = service.createOrderNumber();
		// 记录订单数据
		EBOrder order = new EBOrder();
		order.set("ebOrderNumber", orderNumber).set("ebOrderUserID", userid)
				.set("ebOrderAddressID", addressID)
				.set("ebOrderTotalNum", totalNum)
				.set("ebOrderTotalPrice", totalPrice);
		if (isMail != null && isMail.equals("yes")) {
			// 订单中计入邮费和需要邮寄
			order.set("ebOrderPostFee", CommonParam.postcardMailFee).set(
					"ebOrderIsMail", 1);
		}

		// 生成订单
		if (order.save()) {
			// 获取订单ID
			String orderID = order.get("ebOrderID").toString();

			// 更新itemList中item数据并返回更新后的结果
			itemList = TxService.conbineItemListWithOrderID(orderID, itemList);

			if (itemList == null) {
				// 为null说明方法操作执行失败
				jsonString = jsonData.getJson(-51, "更新数据失败");
			} else {
				Record record = new Record().set("ebOrderID", orderID)
						.set("ebOrderTotalPrice", totalPrice)
						.set("ebOrderNumber", orderNumber)
						.set("couponLimit", null);
				List<Record> result = new ArrayList<Record>();
				result.add(record);
				jsonString = jsonData.getSuccessJson(result);
			}
		} else {
			jsonString = jsonData.getJson(-50, "插入数据失败");
		}
		renderText(jsonString);
		
		// 数据返回到前端后，后台自我执行数据处理
		service.dataprocessAfterPlaceOrder(orderNumber, itemList);
		
		
	}

	/**
	 * 支付成功
	 */
	public void PaySuccess() {
		// 获取数据
		String userid = this.getPara("userid");
		String ebOrderID = this.getPara("ebOrderID");
		String payWay = this.getPara("payWay");
		// 生成支付成功时间
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String date = sdf.format(new Date());
		int count = Db
				.update("update eborders set ebOrderStatus=1,ebOrderPayTime='"
						+ date + "',ebOrderPayWay='" + payWay
						+ "' where ebOrderID=" + ebOrderID + " ");

		// 将优惠券信息改成已使用
		List<Record> list = Db
				.find("select itemCouponID from items where itemEBOrderID="
						+ ebOrderID + " ");
		for (Record record : list) {
			String couponID = record.get("itemCouponID").toString();
			if (!(couponID.equals("0"))) {
				Db.update("update coupon set couponStatus=1 where couponID="
						+ couponID + " ");
			}
		}

		jsonString = dataProcess.updateFlagResult(count == 1);
		renderText(jsonString);
	}

	/**
	 * 获取商品条目中商品的信息
	 */
	public void GetSingleItemInfo() {
		String itemID = this.getPara("itemID");
		List<Record> list = Db
				.find("select ebGoodsID,ebGoodsName,ebGoodsPrice,ebGoodsUnit,ebGoodsOrigin,ebGoodsPic from ebgoods,items where ebGoodsID=itemEBGoodsID and itemID="
						+ itemID + "  ");
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 获取商品介绍
	 */
	public void GetGoodsIntroduce() {
		String goodsID = this.getPara("goodsID");
		Record record = Db
				.findFirst("select ebGoodsName,ebIntroducePic,ebIntroduceText,ebIntroduceLongPic from ebgoods where ebGoodsID="
						+ goodsID + " ");
		String[] pic = record.get("ebIntroducePic").toString().split(",");
		String[] text = record.get("ebIntroduceText").toString().split("&nbsp");
		List<Record> list = new ArrayList<Record>();
		for (int i = 0; i < pic.length; i++) {
			Record temp = new Record();
			temp.set("ebIntroducePic", CommonParam.qiniuOpenAddress + pic[i])
					.set("ebIntroduceText", text[i]);
			list.add(temp);
		}
		Record result = new Record()
				.set("info", list)
				.set("name", record.get("ebGoodsName").toString())
				.set("ebIntroduceLongPic",
						record.get("ebIntroduceLongPic").toString());
		List<Record> resultList = new ArrayList<Record>();
		resultList.add(result);
		jsonString = jsonData.getSuccessJson(resultList);
		renderText(jsonString);
	}

	/**
	 * 显示管理端内订单信息
	 */
	public void ShowOrderInfo() {
		String goodsID = this.getPara("goodsID");
		List<Record> list = Db
				.find("select unickname,ebOrderNumber,addressReceiver,addressPhoneNumber,addressProvince,addressCity,"
						+ "addressArea,addressDetail,itemResult,itemCover,itemNum,itemPic,itemAudio,itemText,itemFrom,itemTo,ebOrderStatus "
						+ "from users,eborders,ebgoods,items,address "
						+ "where userid=ebOrderUserID and ebOrderID=itemEBOrderID and itemEBGoodsID=ebGoodsID "
						+ "and ebOrderAddressID=addressID and ebGoodsID="
						+ goodsID + " and ebOrderStatus in(1) ");
		list = dataProcess.changeOrderStatusIntoWord(list);
		for (Record record : list) {
			String addressProvince = record.get("addressProvince").toString();
			String addressCity = record.get("addressCity").toString();
			String addressArea = record.get("addressArea").toString();
			String addressDetail = record.get("addressDetail").toString();
			String address = addressProvince + addressCity + addressArea
					+ addressDetail;
			record.remove("addressProvince");
			record.remove("addressCity");
			record.remove("addressArea");
			record.remove("addressDetail");
			record.set("address", address);
		}
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 显示订单内的所有照片
	 */
	public void ShowAllPhotosInOrder() {
		String orderNumber = this.getPara("orderNumber");
		List<Record> list = Db
				.find("select itemPic from items,eborders where ebOrderID=itemEBOrderID and ebOrderNumber='"
						+ orderNumber + "' ");
		String[] array = list.get(0).get("itemPic").toString().split(",");
		Record record = new Record().set("picture", array);
		List<Record> result = new ArrayList<Record>();
		result.add(record);
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
	}

	/**
	 * 显示用户反馈
	 */
	public void ShowUserFeedback() {
		List<Record> list = Db.find("select * from contact");
		for (Record record : list) {
			String status = record.get("contactStatus").toString();
			if (status.equals("0")) {
				record.set("contactStatus", "未处理");
			} else {
				record.set("contactStatus", "已处理");
			}
		}
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 修改订单状态
	 */
	public void ModifyOrderStatus() {
		String orderNum = this.getPara("orderNum");
		String status = this.getPara("status");
		int num = dataProcess.changeOrderStatusIntoNumber(status);
		int count = Db.update("update eborders set ebOrderStatus=" + num
				+ " where ebOrderNumber='" + orderNum + "' ");
		jsonString = dataProcess.updateFlagResult(count == 1);
		renderText(jsonString);
	}

	/**
	 * 修改用户反馈状态
	 */
	public void ModifyContactStatus() {
		String type = this.getPara("type");
		String contactID = this.getPara("contactID");
		Contact contact = new Contact().findById(contactID);
		switch (type) {
		case "未处理":
			contact.set("contactStatus", 0);
			break;
		case "已处理":
			contact.set("contactStatus", 1);
			break;
		}
		jsonString = dataProcess.updateFlagResult(contact.update());
		renderText(jsonString);
	}

	/**
	 * 获取红包状态
	 */
	public void GetRedPacketStatus() {
		String redPacketID = this.getPara("redPacketID");
		String userid = this.getPara("userid");
		try {
			redPacketID = DES.decryptDES(redPacketID, CommonParam.DESSecretKey);
			List<Record> redPacketList = Db
					.find("select redPacketStatus from redpacket where redPacketID="
							+ redPacketID + " ");
			if (redPacketList.size() == 0) {
				jsonString = jsonData.getJson(2020, "红包不存在");
			} else {
				Record record = new Record();
				List<Record> receiveList = Db
						.find("select receiveStatus from receive where receiveRPid="
								+ redPacketID
								+ " and receiveUserID="
								+ userid
								+ " ");
				if (receiveList.size() == 0) {
					record.set("status", "未领取");
				} else {
					String receiveStatus = receiveList.get(0)
							.get("receiveStatus").toString();
					switch (receiveStatus) {
					case "0":
						record.set("status", "已领取");
						break;
					case "1":
						record.set("status", "已超时");
						break;
					case "2":
						record.set("status", "未领取");
						break;
					}
				}
				List<Record> list = new ArrayList<Record>();
				list.add(record);
				jsonString = jsonData.getSuccessJson(list);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			jsonString = jsonData.getJson(1014, "解密失败");
		}

		renderText(jsonString);
	}

	/**
	 * 领取红包
	 */
	public void ReceiveRedPacket() {
		String redPacketID = this.getPara("redPacketID");
		String userid = this.getPara("userid");
		String isOverTime = this.getPara("isOverTime");
		String phonenumber = this.getPara("phonenumber");
		try {
			redPacketID = DES.decryptDES(redPacketID, CommonParam.DESSecretKey);
			Receive receive = new Receive();
			if (isOverTime.equals("0")) {
				// 未超时
				// 判断是否已经领取过
				List<Record> receiveList = Db
						.find("select receiveStatus from receive where receiveRPid="
								+ redPacketID
								+ " and receiveUserID="
								+ userid
								+ " ");
				if (receiveList.size() == 0) {
					receive.set("receiveRPid", redPacketID)
							.set("receiveUserID", userid)
							.set("receiveStatus", 0);

					if (receive.save()) {
						String receiveID = receive.get("receiveID").toString();
						// 生成优惠券
						Record couponRecord = service
								.CreatShareRedPacketCoupon();
						// 插入优惠券数据
						couponRecord.set("couponReceiveID", receiveID).set(
								"couponUserID", userid);
						Coupon coupon = new Coupon().put(couponRecord);
						coupon.save();
						// 判断手机和微信
						int judge = service.judgePhoneAndWechatIsBind(userid,
								phonenumber);
						if (judge != 0 && judge != -2) {
							// 排除已绑定的情况
							if (judge == -1) {
								// 未绑定，绑定手机和微信
								service.bindPhoneToUserid(userid, phonenumber);
							} else {
								// 存在另一个账户
								receive.set("receiveUserID", judge);
								receive.remove("receiveID");
								if (receive.save()) {
									// 给该账户也插入优惠券数据
									receiveID = receive.get("receiveID")
											.toString();
									couponRecord.set("couponReceiveID",
											receiveID).set("couponUserID",
											judge);
									coupon = new Coupon().put(couponRecord);
									coupon.save();
								}
							}
						}
						// 拼接返回信息
						String name = couponRecord.get("couponName").toString();
						String price = couponRecord.get("couponLimit")
								.toString();
						// 返回优惠券信息
						Record record = new Record().set("result", name + price
								+ "元优惠券");
						List<Record> list = new ArrayList<Record>();
						list.add(record);
						jsonString = jsonData.getSuccessJson(list);
					} else {
						jsonString = jsonData.getJson(-50, "数据插入到数据库失败");
					}
				} else {
					jsonString = jsonData.getJson(2024, "已领过该红包");
				}
			} else {
				// 超时
				receive.set("receiveRPid", redPacketID)
						.set("receiveUserID", userid).set("receiveStatus", 1);
				jsonString = dataProcess.insertFlagResult(receive.save());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			jsonString = jsonData.getJson(1014, "解密失败");
		}

		renderText(jsonString);
	}

	/**
	 * 显示优惠券列表
	 */
	public void ShowCouponList() {
		String userid = this.getPara("userid");
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String today = sdf.format(new Date());
		List<Record> list = Db
				.find("select couponID,couponName,couponLimit,couponRange,couponType,couponDate,couponTime,couponStatus from coupon where couponUserID="
						+ userid + " and couponDate>='"+today+"' and couponStatus=0 ");
		list = dataProcess.changeCouponStatusIntoWord(list);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 通过优惠码获取优惠券
	 */
	@Before(Tx.class)
	public void GetCouponByCouponCode() {
		String userid = this.getPara("userid");
		String couponCode = this.getPara("couponCode");
		List<Record> redPacketInfo = Db
				.find("select redPacketID,redPacketType,redPacketPriceLimit,redPacketTimeLimit,redPacketStatus from redpacket where redPacketCode='"
						+ couponCode + "' ");
		if (couponCode.equals("忆年系统默认分享红包") || redPacketInfo.size() == 0) {
			jsonString = jsonData.getJson(2021, "优惠码不存在");
		} else {
			String redPacketID = redPacketInfo.get(0).get("redPacketID")
					.toString();
			String redPacketStatus = redPacketInfo.get(0)
					.get("redPacketStatus").toString();
			if (redPacketStatus.equals("0")) {
				// 优惠码正常使用中，判断用户是否领取
				List<Record> judge = Db
						.find("select receiveStatus from receive where receiveRPid="
								+ redPacketID
								+ " and receiveUserID="
								+ userid
								+ " ");
				if (judge.size() == 0) {
					// 说明未领取，执行领取的方法
					Receive receive = new Receive().set("receiveRPid",
							redPacketID).set("receiveUserID", userid);
					if (receive.save()) {
						String receiveID = receive.get("receiveID").toString();
						// 生成优惠券数据，插入到数据库中并返回
						TxService.CreatAgentRedPacketCoupon(redPacketInfo,
								userid, receiveID);

						//将一次性红包的状态设置为已领取
						String redPacketType = redPacketInfo.get(0).get("redPacketType").toString();
						if(redPacketType.equals("3")){
							Redpacket redPacket = new Redpacket().findById(redPacketID);
							redPacket.set("redPacketStatus", 1);
							redPacket.update();
						}
						
						List<Record> list = Db
								.find("select couponID,couponName,couponLimit,couponRange,couponType,couponDate,couponTime,couponStatus from coupon where couponReceiveID="
										+ receiveID + " ");
						jsonString = jsonData.getSuccessJson(list);

					}
				} else {
					String receiveStatus = judge.get(0).get("receiveStatus")
							.toString();
					switch (receiveStatus) {
					case "0":
						jsonString = jsonData.getJson(2024, "已领过该红包");
						break;
					case "1":
						jsonString = jsonData.getJson(2025, "领取红包操作超时");
						break;
					default:
						jsonString = jsonData.getJson(-54, "未知异常");
						break;
					}
				}

			} else {
				switch (redPacketStatus) {
				case "1":
					jsonString = jsonData.getJson(2022, "优惠券已领完");
					break;
				case "2":
					jsonString = jsonData.getJson(2023, "优惠码已过期");
					break;
				default:
					jsonString = jsonData.getJson(-54, "未知异常");
					break;
				}
			}

		}
		renderText(jsonString);

	}

	/**
	 * 生成分享红包
	 */
	public void CreateShareRedPacket() {
		String ebOrderID = this.getPara("ebOrderID");
		String userid = this.getPara("userid");
		Redpacket red = new Redpacket().set("redPacketOwner", userid)
				.set("redPacketType", 0).set("redPacketOrderID", ebOrderID);
		if (red.save()) {
			String redPacketID = red.get("redPacketID").toString();
			try {
				redPacketID = DES.encryptDES(redPacketID,
						CommonParam.DESSecretKey);
				jsonString = jsonData.getSuccessJson(dataProcess
						.makeSingleParamToList("redPacketID", redPacketID));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				jsonString = jsonData.getJson(1029, "加密失败");
			}
		} else {
			jsonString = jsonData.getJson(-50, "插入数据失败");
		}
		renderText(jsonString);
	}

	/**
	 * Web端截取图片后将图片上传至七牛后返回地址
	 * 
	 * @throws FileNotFoundException
	 */
	public void UploadCutPictureToQiniuAndReturnURL()
			throws FileNotFoundException {
		// 获取数据
		String data = this.getPara("data");
		System.out.println(data);
		// 获取图片base64字符串
		JSONObject jo = JSONObject.parseObject(data);
		String imgString = jo.getString("imgstring");
		// 生成文件名
		Random random = new Random();
		String filename = String.valueOf(random.nextInt(100000))
				+ String.valueOf(System.currentTimeMillis())
				+ String.valueOf(random.nextInt(100000)) + ".jpg";
		// base64解码,并存储图片
		BASE64Decoder decoder = new BASE64Decoder();
		try {
			// Base64解码
			byte[] bytes = decoder.decodeBuffer(imgString);
			for (int i = 0; i < bytes.length; ++i) {
				if (bytes[i] < 0) {// 调整异常数据
					bytes[i] += 256;
				}
			}
			// 生成jpeg图片
			OutputStream out = new FileOutputStream(filename);
			out.write(bytes);
			out.flush();
			out.close();

		} catch (Exception e) {
			System.out.println(e);
		}
		// 上传七牛云
		// 创建上传对象
		UploadManager uploadManager = new UploadManager();
		QiniuOperate qiniu = new QiniuOperate();
		// 调用put方法上传
		try {
			Response res = uploadManager.put(filename, filename,
					qiniu.getUploadToken());
			// 打印返回的信息
			System.out.println(res.bodyString());
		} catch (QiniuException e) {
			Response r = e.response;
			// 请求失败时打印的异常的信息
			System.out.println(r.toString());
			try {
				// 响应的文本信息
				System.out.println(r.bodyString());
			} catch (QiniuException e1) {
				// ignore
			}
		}
		// 删除文件
		File file = new File(filename);
		file.delete();
		// 返回结果
		filename = CommonParam.qiniuOpenAddress + filename;
		System.out.println(filename);
		List<Record> list = dataProcess.makeSingleParamToList("filePath",
				filename);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);

	}

	/**
	 * 查看购物车
	 */
	public void ShowCart() {
		String userid = this.getPara("userid");
		List<Record> list = Db
				.find("select cartID,ebGoodsName,ebGoodsEnName,ebGoodsPrice,ebGoodsPic,ebGoodsUnit,cartItemNum,cartItemID,ebGoodsID from cart,items,ebgoods where cartItemID=itemID and itemEBGoodsID=ebGoodsID and cartUserID="
						+ userid + " and cartStatus=0 and ebGoodsStatus=0  ");

		Set<Integer> set = new HashSet<Integer>();
		ArrayList<List<Record>> array = new ArrayList<List<Record>>();
		// 这里有点问题，不应该直接给个20，应该做成动态的，获取商品ID的最大值即可
		List<Record> no = new ArrayList<Record>();
		for (int i = 0; i < 20; i++) {
			array.add(no);
		}

		for (Record record : list) {
			boolean flag = false;
			List<Record> temp = new ArrayList<Record>();
			int goodsID = Integer.parseInt(record.get("ebGoodsID").toString());

			flag = set.contains(goodsID);
			if (flag) {
				array.get(goodsID).add(record);
			} else {
				temp.add(record);
				array.set(goodsID, temp);
			}
			set.add(goodsID);
		}
		// 拼接结果
		Iterator<Integer> it = set.iterator();
		List<Record> result = new ArrayList<Record>();
		while (it.hasNext()) {
			int id = it.next();
			List<Record> temp = array.get(id);
			String goodsName = temp.get(0).get("ebGoodsName").toString();
			Record record = new Record().set("goodsInfo", temp).set(
					"goodsName", goodsName);
			result.add(record);
		}

		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);
	}

	/**
	 * 查看商品详情
	 */
	public void GetItemDetail() {
		String itemID = this.getPara("itemID");
		List<Record> list = Db
				.find("select itemEBGoodsID,itemPic,itemCover,itemAudio,itemAudioTime,itemText,itemFrom,itemTo from items where itemID="
						+ itemID + "  ");
		String[] array = list.get(0).get("itemPic").toString().split(",");
		list.get(0).set("itemPic", array);
		jsonString = jsonData.getSuccessJson(list);
		renderText(jsonString);
	}

	/**
	 * 管理购物车
	 */
	public void ManageCart() {
		String userid = this.getPara("userid");
		String itemID = this.getPara("itemID");
		String cartID = this.getPara("cartID");
		String modifyInfo = this.getPara("modifyInfo");
		String type = this.getPara("type");
		Cart cart = null;
		switch (type) {
		case "add":
			List<Record> list = Db.find("select * from cart where cartUserID="
					+ userid + " and cartItemID=" + itemID
					+ " and cartStatus=0 ");
			if (list.size() == 0) {
				cart = new Cart().set("cartUserID", userid)
						.set("cartItemID", itemID).set("cartItemNum", 1);
				jsonString = dataProcess.insertFlagResult(cart.save());
			} else {
				String tempCartID = list.get(0).get("cartID").toString();
				cart = new Cart().findById(tempCartID);
				int cartGoodsNum = Integer.parseInt(cart.get("cartItemNum")
						.toString());
				cart.set("cartItemNum", cartGoodsNum + 1);
				jsonString = dataProcess.updateFlagResult(cart.update());
			}
			break;
		case "modify":
			// 解析modifyInfo数据
			boolean flag = TxService.modifyCartInfo(userid, modifyInfo);
			jsonString = dataProcess.updateFlagResult(flag);
			break;
		case "remove":
			cart = new Cart().findById(cartID);
			cart.set("cartStatus", 1);
			jsonString = dataProcess.deleteFlagResult(cart.update());
			break;
		}
		renderText(jsonString);
	}

	/**
	 * 获取结算时使用信息
	 */
	@Before(Tx.class)
	public void GetInfoBeforePay() {
		// 获取参数
		String userid = this.getPara("userid");
		String itemInfo = this.getPara("itemInfo");
		// 返回数据列表
		List<Record> result = new ArrayList<Record>();
		Record resultRecord = new Record();

		// 获取默认收货地址
		List<Record> addressList = Db
				.find("select addressID,addressReceiver,addressPhoneNumber,addressProvince,addressCity,addressArea,addressDetail from address where addressUserID="
						+ userid + " and addressStatus=0 and isDefault=1 ");
		resultRecord.set("address", addressList);

		// 获取邮费信息
		List<Record> postFeeList = dataProcess.makeSingleParamToList(
				"postCardFee", CommonParam.postcardMailFee);
		resultRecord.set("postFee", postFeeList);

		// 获取优惠券信息
		// 解析item并获取相应的List<Record>对象
		JSONObject jo = JSONObject.parseObject(itemInfo);
		JSONArray ja = jo.getJSONArray("data");

		// 按商品类别进行获取优惠券信息
		List<Record> couponList = new ArrayList<Record>();
		for (int i = 0; i < ja.size(); i++) {
			double totalCouponPrice = 0.00;
			JSONObject tempObject = ja.getJSONObject(i);
			int goodsID = tempObject.getIntValue("ebGoodsID");
			JSONArray tempArray = tempObject.getJSONArray("itemInfo");

			// 计算优惠券金额
			int size = tempArray.size();
			List<Record> coupon = Db
					.find("select couponID,couponLimit from coupon where couponType="
							+ goodsID
							+ " and couponStatus=0 and couponUserID="
							+ userid
							+ " order by couponLimit desc limit "
							+ size + " ");
			// 计算优惠券总额，并更新相应的item
			for (int j = 0; j < coupon.size(); j++) {
				int couponID = Integer.parseInt(coupon.get(j).get("couponID")
						.toString());
				double couponLimit = Double.parseDouble(coupon.get(j)
						.get("couponLimit").toString());
				// 计算价格
				totalCouponPrice += couponLimit;
				// item对象中插入优惠券数据
				JSONObject itemObject = tempArray.getJSONObject(j);
				int itemID = itemObject.getIntValue("itemID");
				Item item = new Item().findById(itemID);
				item.set("itemCouponID", couponID);
				item.update();
			}
			Record record = new Record().set("totalCouponPrice",
					totalCouponPrice).set("goodsID", goodsID);
			couponList.add(record);
		}
		resultRecord.set("coupon", couponList);
		result.add(resultRecord);
		jsonString = jsonData.getSuccessJson(result);
		renderText(jsonString);

	}

	// /**
	// * 下单
	// */
	// @Before(Tx.class)
	// public void PlaceOrder() {
	// // 获取数据
	// String userid = this.getPara("userid");
	// String addressID = this.getPara("addressID");
	// String itemID = this.getPara("itemID");
	// String num = this.getPara("num");
	// String goodsID = this.getPara("goodsID");
	// // 获取对象
	// Item item = new Item().findById(itemID);
	// // 计算订单总价
	// String goodsPrice = Db
	// .findFirst(
	// "select ebGoodsPrice from ebgoods where ebGoodsID="
	// + item.get("itemEBGoodsID").toString() + " ")
	// .get("ebGoodsPrice").toString();
	// double totalPrice = ((Double.parseDouble(goodsPrice)) * (Double
	// .parseDouble(num)));
	// // 获取优惠券价格
	// String couponID = item.get("itemCouponID").toString();
	// Double couponLimit = 0.00;
	// if (!couponID.equals("0")) {
	// Coupon coupon = new Coupon().findById(couponID);
	// couponLimit = Double.parseDouble(coupon.get("couponLimit")
	// .toString());
	// }
	// // 能否进行合成封底的判断标志，在第一次下单后，值改成true则需进行合成
	// boolean canComposeFlag = false;
	// String globalOrderNumber = null;
	// // 判断是否已经下单，已下单的话，则不新增订单，只更改地址，数量，避免用户更改
	// if (item.get("itemEBOrderID") == null) {
	// EBOrder order = new EBOrder();
	//
	// // 获取订单号
	// SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	// String date = sdf.format(new Date());
	// Random random = new Random();
	// String orderNumber = "YN" + date;
	// // 判断订单号是否重复
	// boolean flag = true;
	// List<Record> list = Db.find("select ebOrderNumber from eborders ");
	// while (flag) {
	// int temp = random.nextInt(100000000);
	// orderNumber += temp;
	// for (Record record : list) {
	// String number = record.getStr("ebOrderNumber");
	// if (number.equals(orderNumber)) {
	// flag = true;
	// break;
	// } else {
	// flag = false;
	// }
	// }
	// }
	// // 保存订单
	// order.set("ebOrderNumber", orderNumber)
	// .set("ebOrderUserID", userid)
	// .set("ebOrderAddressID", addressID)
	// .set("ebOrderTotalNum", num)
	// .set("ebOrderTotalPrice", totalPrice);
	// if (order.save()) {
	// String orderID = order.get("ebOrderID").toString();
	//
	// // *******判断商品类型是不是等于5，是则修改item内的字段，此地方是暂时的*******//
	// if (goodsID != null && !(goodsID.equals(""))
	// && goodsID.equals("5")) {
	// item.set("itemEBGoodsID", 5);
	// }
	// // ************************End*****************************//
	//
	// // 更新商品条目
	// item.set("itemEBOrderID", orderID).set("itemNum", num);
	// if (item.update()) {
	// Record record = new Record().set("ebOrderID", orderID)
	// .set("ebOrderTotalPrice", totalPrice)
	// .set("ebOrderNumber", orderNumber)
	// .set("couponLimit", couponLimit);
	//
	// // *************如果是LOMO卡，则发送网络请求，预先下载图片***********//
	// if (goodsID != null && !(goodsID.equals(""))
	// && goodsID.equals("2")) {
	// String itemPic = item.get("itemPic").toString();
	// dataProcess.sentNetworkRequest(
	// CommonParam.LomoCardPictureDownload, itemPic);
	// }
	// // **********************End*****************************//
	//
	// List<Record> result = new ArrayList<Record>();
	// result.add(record);
	// jsonString = jsonData.getSuccessJson(result);
	//
	// // 合成标志改为true
	// canComposeFlag = true;
	// globalOrderNumber = orderNumber;
	// } else {
	// jsonString = jsonData.getJson(-51, "更新数据失败");
	// }
	// } else {
	// jsonString = jsonData.getJson(-50, "插入数据失败");
	// }
	// } else {
	//
	// String orderID = item.get("itemEBOrderID").toString();
	// EBOrder order = new EBOrder().findById(orderID);
	// String orderNumber = order.get("ebOrderNumber").toString();
	// order.set("ebOrderAddressID", addressID)
	// .set("ebOrderTotalNum", num)
	// .set("ebOrderTotalPrice", totalPrice);
	// if (order.update()) {
	// Record record = new Record().set("ebOrderID", orderID)
	// .set("ebOrderTotalPrice", totalPrice)
	// .set("ebOrderNumber", orderNumber)
	// .set("couponLimit", couponLimit);
	// List<Record> temp = new ArrayList<Record>();
	// temp.add(record);
	// jsonString = jsonData.getSuccessJson(temp);
	// } else {
	// jsonString = jsonData.getJson(-51, "更新数据失败");
	// }
	// }
	// renderText(jsonString);
	//
	// // 数据返回后，如果是声音明信片，进行封底的合成
	// String itemEBGoodsID = item.get("itemEBGoodsID").toString();
	// if (itemEBGoodsID.equals("1") && canComposeFlag) {
	// service.ComposePostcardBottom(itemID, globalOrderNumber);
	// }
	// }

}
