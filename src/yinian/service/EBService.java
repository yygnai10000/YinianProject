package yinian.service;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import yinian.app.YinianDataProcess;
import yinian.common.CommonParam;
import yinian.draw.ComposePicture;
import yinian.model.Cart;
import yinian.model.Coupon;
import yinian.model.Item;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Before;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.jfinal.plugin.activerecord.tx.Tx;

public class EBService {

	private YinianDataProcess dataProcess = new YinianDataProcess();

	/**
	 * 判断手机和微信号码是否绑定 已绑定返回0，已存在另外的用户返回跟手机号码相对应的userid，未绑定返回-1，该userid已绑定其他号码返回-2
	 * 
	 * @param userid
	 * @return
	 */
	public int judgePhoneAndWechatIsBind(String userid, String phone) {
		List<Record> list = Db.find("select userid from users where uphone='"
				+ phone + "' ");
		if (list.size() == 0) {
			// 没有该号码，说明未绑定,判断该用户的信息内是否绑定了其他号码
			List<Record> list2 = Db
					.find("select uphone from users where userid=" + userid
							+ " ");
			if (list2.size() == 0) {
				return -1;
			} else {
				return -2;
			}
		} else {
			String temp = list.get(0).get("userid").toString();
			if (temp.equals(userid)) {
				// 已绑定
				return 0;
			} else {
				return Integer.parseInt(temp);
			}
		}
	}

	/**
	 * 绑定手机号码到用户信息中
	 * 
	 * @param userid
	 * @param phone
	 * @return
	 */
	public boolean bindPhoneToUserid(String userid, String phone) {
		int count = Db.update("update users set uphone='" + phone
				+ "' where userid=" + userid + " ");
		return count == 1;
	}

	/**
	 * 生成分享红包优惠券
	 */
	public Record CreatShareRedPacketCoupon() {
		List<Record> list = Db
				.find("select ebGoodsID from ebgoods where ebGoodsStatus=0 ");
		int size = list.size();
		int temp = 1 + (int) (Math.random() * size);
		DecimalFormat dcmFmt = new DecimalFormat("0.00");
		Random rand = new Random();
		float f = 0;

		// 设置过期时间，默认为7天
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, 7);
		Date date = cal.getTime();
		String expired = sdf.format(date);

		Record record = new Record();

		switch (temp) {
		case 1:
			f = (float) (1 + rand.nextFloat() * 2);
			record.set("couponName", "声音明信片").set("couponRange", "定制声音明信片");
			// 明信片，价格为5元
			break;
		case 2:
			f = (float) (3 + rand.nextFloat() * 5);
			record.set("couponName", "LOMO卡").set("couponRange", "定制LOMO卡");
			// LOMO卡，价格为39.8元
			break;
		case 3:
			f = (float) (3 + rand.nextFloat() * 7);
			record.set("couponName", "照片拼图").set("couponRange", "定制照片拼图");
			// 照片拼图，价格为49.8元
			break;
		case 4:
			f = (float) (10 + rand.nextFloat() * 10);
			record.set("couponName", "图钉相纸").set("couponRange", "定制图钉相纸");
			// 图钉相纸，价格为458元
			break;
		}
		String price = dcmFmt.format(f);
		record.set("couponLimit", price).set("couponType", temp)
				.set("couponDate", expired);

		return record;
	}

	/**
	 * 生成分享红包优惠券
	 */
	@Before(Tx.class)
	public List<Coupon> CreatAgentRedPacketCoupon(List<Record> redPacketInfo,
			String userid, String receiveID) {
		List<Coupon> result = new ArrayList<Coupon>();
		String[] redPacketPriceLimit = redPacketInfo.get(0)
				.get("redPacketPriceLimit").toString().split(",");
		String[] redPacketTimeLimit = redPacketInfo.get(0)
				.get("redPacketTimeLimit").toString().split(",");

		// 设置过期时间，默认为7天
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		Coupon coupon;
		for (int i = 0; i < redPacketPriceLimit.length; i++) {
			if (!redPacketPriceLimit[i].equals("0")) {
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DATE, Integer.parseInt(redPacketTimeLimit[i]));
				Date date = cal.getTime();
				String expired = sdf.format(date);
				coupon = new Coupon().set("couponUserID", userid)
						.set("couponReceiveID", receiveID)
						.set("couponLimit", redPacketPriceLimit[i])
						.set("couponType", i + 1).set("couponDate", expired);
				coupon = addCouponNameAndRange(coupon, i + 1);
				result.add(coupon);
				coupon.save();
			}
		}
		return result;
	}

	/**
	 * 添加优惠券名称和优惠券使用范围
	 * 
	 * @param coupon
	 * @param goodsID
	 * @return
	 */
	public Coupon addCouponNameAndRange(Coupon coupon, int goodsID) {
		switch (goodsID) {
		case 1:
			coupon.set("couponName", "声音明信片").set("couponRange", "定制声音明信片");
			// 明信片，价格为5元
			break;
		case 2:
			coupon.set("couponName", "LOMO卡").set("couponRange", "定制LOMO卡");
			// LOMO卡，价格为39.8元
			break;
		case 3:
			coupon.set("couponName", "照片拼图").set("couponRange", "定制照片拼图");
			// 照片拼图，价格为49.8元
			break;
		case 4:
			coupon.set("couponName", "图钉相纸").set("couponRange", "定制图钉相纸");
			// 图钉相纸，价格为458元
			break;

		}
		return coupon;

	}

	/**
	 * 合成封底
	 * 
	 * @param itemID
	 * @param orderNumber
	 * @return
	 */
	public boolean ComposePostcardBottom(String itemID, String orderNumber) {
		// 获取数据
		Item item = new Item().findById(itemID);
		String picture = item.get("itemPic");
		String audio = item.get("itemAudio");
		String text = item.get("itemText");
		String from = item.get("itemFrom");
		String to = item.get("itemTo");
		// 生成文件名
		String filename = orderNumber
				+ String.valueOf(System.currentTimeMillis()) + ".jpg";
		// 获取文件存储路径
		String temp = "C:/Users/Zad/Desktop/temp/" + filename;
		String path = CommonParam.postcardBottomSaveRelativePath + filename;
		// 合成图片
		ComposePicture.MakePostcardBottom(from, to, text, picture, audio,
				orderNumber, path);
		// 修改数据库数据
		item.set("itemResult", CommonParam.postcardBottomAbsolutePath
				+ filename);
		return item.update();
	}

	/**
	 * 解析item数据字符串
	 */
	public List<Record> parseItemInfoString(String itemInfo) {
		/**
		 * 测试数据 —— { "data": [ { "ebGoodsID ": 1, "itemInfo": [ { "num": 2,
		 * "itemID":1 }, { "num": 2, "itemID":2 } ] }, { "ebGoodsID ": "2",
		 * "itemInfo": [ { "num": 2, "itemID":3 }, { "num": 2, "itemID":4 } ] },
		 * ] }
		 */
		List<Record> result = new ArrayList<Record>();
		JSONObject jo = JSONObject.parseObject(itemInfo);
		JSONArray ja = jo.getJSONArray("data");

		for (int i = 0; i < ja.size(); i++) {
			JSONObject tempObject = ja.getJSONObject(i);
			JSONArray tempArray = tempObject.getJSONArray("itemInfo");

			for (int j = 0; j < tempArray.size(); j++) {
				int itemID = tempArray.getJSONObject(j).getIntValue("itemID");
				int num = tempArray.getJSONObject(j).getIntValue("num");
				Item item = new Item().findById(itemID);

				Record record = new Record()
						.set("itemID", itemID)
						.set("num", num)
						.set("ebGoodsID",
								Integer.parseInt(item.get("itemEBGoodsID")
										.toString()));
				result.add(record);
			}
		}
		return result;
	}

	/**
	 * 获取item信息中的商品总价、总量
	 * 
	 * @param itemInfo
	 * @return
	 */
	public Record getTotalNumAndGoodsPriceFromItemList(List<Record> list) {
		Double totalPrice = 0.0;
		int totalNum = 0;
		// 获取当前电商中所有商品的ID及对应价格
		List<Record> goodsPrice = Db
				.find("select ebGoodsID,ebGoodsPrice from ebgoods where ebGoodsStatus=0");
		// 用Record来存储数据，减少查询时间
		Record goodsRecord = new Record();
		for (Record temp : goodsPrice) {
			goodsRecord.set(temp.get("ebGoodsID").toString(),
					temp.getDouble("ebGoodsPrice"));
		}
		for (Record temp : list) {
			totalNum += temp.getInt("num");
			Item item = new Item().findById(temp.getInt("itemID"));
			String ebGoodsID = item.get("itemEBGoodsID").toString();
			totalPrice = goodsRecord.getDouble(ebGoodsID);
		}
		Record record = new Record().set("totalPrice", totalPrice).set(
				"totalNum", totalNum);
		return record;
	}

	/**
	 * 获取订单号
	 * 
	 * @return
	 */
	public String createOrderNumber() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String date = sdf.format(new Date());
		Random random = new Random();
		String orderNumber = "YN" + date;
		// 判断订单号是否重复
		boolean flag = true;
		List<Record> list = Db.find("select ebOrderNumber from eborders ");
		while (flag) {
			int temp = random.nextInt(100000000);
			orderNumber += temp;
			for (Record record : list) {
				String number = record.getStr("ebOrderNumber");
				if (number.equals(orderNumber)) {
					flag = true;
					break;
				} else {
					flag = false;
				}
			}
		}
		return orderNumber;
	}

	/**
	 * 将所有item与订单想关联，如果该item已关联订单，则生成一个新item关联新订单
	 * 
	 * @param ebOrderID
	 * @param itemList
	 * @return
	 */
	@Before(Tx.class)
	public List<Record> conbineItemListWithOrderID(String ebOrderID,
			List<Record> itemList) {
		for (Record record : itemList) {
			Item item = new Item().findById(record.get("itemID").toString());
			int num = Integer.parseInt(record.get("num").toString());
			// 判断是否需要新建一个新的item
			if (item.get("itemEBOrderID") == null) {
				item.set("itemEBOrderID", ebOrderID).set("itemNum", num);
				if (!item.update()) {
					// 不成功，跳出循环，并把itemList清空，上级可以此来做判断
					itemList = null;
					break;
				}
			} else {
				item.remove("itemID").remove("itemEBOrderID");
				item.set("itemEBOrderID", ebOrderID).set("itemNum", num);
				if (!item.save()) {
					// 不成功，跳出循环，并把itemList清空，上级可以此来做判断
					itemList = null;
					break;
				}
				// 更新itemList
				int itemID = Integer.parseInt(item.get("itemID").toString());
				record.set("itemID", itemID);
			}
		}
		return itemList;
	}

	/**
	 * 下单后的数据处理
	 * 
	 * @param orderNumber
	 * @param itemList
	 * @return
	 */
	public boolean dataprocessAfterPlaceOrder(String orderNumber,
			List<Record> itemList) {
		for (Record record : itemList) {
			int ebGoodsID = Integer
					.parseInt(record.get("ebGoodsID").toString());
			String itemID = record.get("itemID").toString();
			Item item = new Item().findById(itemID);
			switch (ebGoodsID) {
			case 1:
				// 明信片，合成封底
				ComposePostcardBottom(itemID, orderNumber);
				break;
			case 2:
				// LOMO卡，发送网络请求预下载图片
				String itemPic = item.get("itemPic").toString();
				dataProcess.sentNetworkRequest(
						CommonParam.LomoCardPictureDownload, itemPic);
				break;
			default:
				break;
			}
		}
		return true;
	}

	/**
	 * 修改购物车信息
	 */
	@Before(Tx.class)
	public boolean modifyCartInfo(String userid, String modifyInfo) {
		boolean flag = true;
		JSONArray array = JSONArray.parseArray(modifyInfo);
		for (int i = 0; i < array.size(); i++) {
			JSONObject object = array.getJSONObject(i);
			int number = object.getIntValue("number");
			int cartID = object.getIntValue("cartID");
			Cart cart = new Cart().findById(cartID);
			String cartUserID = cart.get("cartUserID").toString();
			if (userid.equals(cartUserID)) {
				cart.set("cartItemNum", number);
				flag = cart.update();
				if (!flag) {
					break;
				}
			} else {
				flag = false;
				break;
			}
		}
		return flag;
	}
}
