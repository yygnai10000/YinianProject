package yinian.utils;

import java.util.List;

import yinian.common.CommonParam;
import yinian.model.SNSUserInfo;
import yinian.model.WeixinOauth2Token;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class WechatOauth {

	/**
	 * ��ȡ��ҳ��Ȩƾ֤
	 * 
	 * @param appId
	 *            �����˺ŵ�Ψһ��ʶ
	 * @param appSecret
	 *            �����˺ŵ���Կ
	 * @param code
	 * @return WeixinAouth2Token
	 */
	public static WeixinOauth2Token getOauth2AccessToken(String code) {
		WeixinOauth2Token wat = null;
		// ƴ�������ַ
		String requestUrl = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code";
		requestUrl = requestUrl.replace("APPID", CommonParam.wechatServeAppID);
		requestUrl = requestUrl.replace("SECRET", CommonParam.wechatServeSecretID);
		requestUrl = requestUrl.replace("CODE", code);
		// ��ȡ��ҳ��Ȩƾ֤
		JSONObject jsonObject = YinianUtils.httpsRequest(requestUrl, "GET", null);
		if (null != jsonObject) {
			try {
				wat = new WeixinOauth2Token();
				wat.setAccessToken(jsonObject.getString("access_token"));
				wat.setExpiresIn(jsonObject.getInt("expires_in"));
				wat.setRefreshToken(jsonObject.getString("refresh_token"));
				wat.setOpenId(jsonObject.getString("openid"));
				wat.setScope(jsonObject.getString("scope"));
			} catch (Exception e) {

			}
		}
		return wat;
	}

	/**
	 * ͨ����ҳ��Ȩ��ȡ�û���Ϣ
	 * 
	 * @param accessToken
	 *            ��ҳ��Ȩ�ӿڵ���ƾ֤
	 * @param openId
	 *            �û���ʶ
	 * @return SNSUserInfo
	 */
	@SuppressWarnings({ "deprecation", "unchecked" })
	public static SNSUserInfo getSNSUserInfo(String accessToken, String openId) {
		SNSUserInfo snsUserInfo = null;
		// ƴ�������ַ
		String requestUrl = "https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID";
		requestUrl = requestUrl.replace("ACCESS_TOKEN", accessToken).replace("OPENID", openId);
		// ͨ����ҳ��Ȩ��ȡ�û���Ϣ
		JSONObject jsonObject = YinianUtils.httpsRequest(requestUrl, "GET", null);

		if (null != jsonObject) {
			try {
				snsUserInfo = new SNSUserInfo();
				// �û��ı�ʶ
				snsUserInfo.setOpenId(jsonObject.getString("openid"));
				// unionID
				snsUserInfo.setUnionId(jsonObject.getString("unionid"));
				// �ǳ�
				snsUserInfo.setNickname(jsonObject.getString("nickname"));
				// �Ա�1�����ԣ�2��Ů�ԣ�0��δ֪��
				snsUserInfo.setSex(jsonObject.getInt("sex"));
				// �û����ڹ���
				snsUserInfo.setCountry(jsonObject.getString("country"));
				// �û�����ʡ��
				snsUserInfo.setProvince(jsonObject.getString("province"));
				// �û����ڳ���
				snsUserInfo.setCity(jsonObject.getString("city"));
				// �û�ͷ��
				snsUserInfo.setHeadImgUrl(jsonObject.getString("headimgurl"));
				// �û���Ȩ��Ϣ
				snsUserInfo.setPrivilegeList(JSONArray.toList(jsonObject.getJSONArray("privilege"), List.class));
			} catch (Exception e) {
			}
		}
		return snsUserInfo;
	}
}
