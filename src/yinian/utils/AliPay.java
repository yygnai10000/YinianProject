package yinian.utils;

import yinian.common.CommonParam;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayFundTransOrderQueryModel;
import com.alipay.api.domain.AlipayFundTransToaccountTransferModel;
import com.alipay.api.request.AlipayFundTransOrderQueryRequest;
import com.alipay.api.request.AlipayFundTransToaccountTransferRequest;
import com.alipay.api.response.AlipayFundTransOrderQueryResponse;
import com.alipay.api.response.AlipayFundTransToaccountTransferResponse;

public class AliPay {

	private static AlipayClient alipayClient = new DefaultAlipayClient(
			CommonParam.AlipayGateWay, CommonParam.AliAppID,
			CommonParam.AliAppSecretKey, "json", "utf-8",
			CommonParam.AlipayOpenKey, "RSA2");

	/**
	 * 转账
	 * 
	 * @param model
	 * @return
	 * @throws AlipayApiException
	 */
	public static boolean AliPayWithdraw(AlipayFundTransToaccountTransferModel model)
			throws AlipayApiException {
		AlipayFundTransToaccountTransferResponse response = transferToResponse(model);
		String result = response.getBody();
		System.out.println("transfer result>" + result);
		if (response.isSuccess()) {
			return true;
		} else {
			// 调用查询接口查询数据
			JSONObject jsonObject = JSONObject.parseObject(result);
			String out_biz_no = jsonObject.getJSONObject(
					"alipay_fund_trans_toaccount_transfer_response").getString(
					"out_biz_no");
			AlipayFundTransOrderQueryModel queryModel = new AlipayFundTransOrderQueryModel();
			model.setOutBizNo(out_biz_no);
			boolean isSuccess = transferQuery(queryModel);
			if (isSuccess) {
				return true;
			}
		}
		return false;

	}

	/**
	 * 转账相应
	 * 
	 * @param model
	 * @return
	 * @throws AlipayApiException
	 */
	public static AlipayFundTransToaccountTransferResponse transferToResponse(
			AlipayFundTransToaccountTransferModel model)
			throws AlipayApiException {
		AlipayFundTransToaccountTransferRequest request = new AlipayFundTransToaccountTransferRequest();
		request.setBizModel(model);
		return alipayClient.execute(request);
	}

	/**
	 * 转账结果查询
	 * 
	 * @param model
	 * @return
	 * @throws AlipayApiException
	 */
	public static boolean transferQuery(AlipayFundTransOrderQueryModel model)
			throws AlipayApiException {
		AlipayFundTransOrderQueryResponse response = transferQueryToResponse(model);

		System.out.println("transferQuery result>" + response.getBody());
		if (response.isSuccess()) {
			return true;
		}
		return false;
	}

	/**
	 * 查询相应
	 * 
	 * @param model
	 * @return
	 * @throws AlipayApiException
	 */
	public static AlipayFundTransOrderQueryResponse transferQueryToResponse(
			AlipayFundTransOrderQueryModel model) throws AlipayApiException {
		AlipayFundTransOrderQueryRequest request = new AlipayFundTransOrderQueryRequest();
		request.setBizModel(model);
		return alipayClient.execute(request);
	}
}
