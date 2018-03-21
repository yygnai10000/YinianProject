package yinian.utils.test;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.Map;

import net.sf.json.JSONObject;
import yinian.common.CommonParam;

public class PagerTest {
	public static void main(String[] a){
		Long ts=System.currentTimeMillis();
		String url="http://zhoo.zhenhuaonline.cn/api/in/thirdparty/op_app/v1/handle?app_id=a_qLCogTZJjJbMU8&timestamp=1500713349&sign=";
		//String params=getMD5("timestamp="+ts+"api=User.GetVmOpenidapp_username=lktest001create_user=1");
		//&secret=bjSjRNJmXvBE16faKxo3caLcFjGSZ5bV
		String param1 = getMD5("api=User.Tissue.Order.TicketOrdertissue_ticket=f496f44aa2f1secret=bjSjRNJmXvBE16faKxo3caLcFjGSZ5bV");
		url+=param1;
		param1="api=User.Tissue.Order.TicketOrder&tissue_ticket=f496f44aa2f1&secret=bjSjRNJmXvBE16faKxo3caLcFjGSZ5bV";
		String jsonStr = sendPost(url, param1);
		/*JSONObject jo = JSONObject.fromObject(jsonStr);
		String json = jo.get("api_data").toString();
		JSONObject js1 = JSONObject.fromObject(json);
		String vm_openid = js1.getString("vm_openid").toString();*/
		System.err.println("加密后： " + url); 
		System.err.println("post后： " + jsonStr);
		/*System.err.println(json);
		System.err.println(vm_openid);
		*/
		/*//获取用户可领取纸巾数
		String param2 = getMD5("api=User.Tissue.DrawTissuevm_openid="+vm_openid+"timestamp="+ts + "zOH7c2QJb63hyejLsFWGJ3mJ58P5UsXZ");
		url += param2;
		param2 = "api=User.GetVmOpenid&vm_openid"+vm_openid;
		
		String jsonStr2 = sendPost(url, param2);
		JSONObject jo2 = JSONObject.fromObject(jsonStr2);
		String json2 = jo2.get("api_data").toString();
		JSONObject js2 = JSONObject.fromObject(json2);
		String tissue_num = js2.getString("tissue_num").toString();
		System.err.println("加密后： " + url); 
		System.err.println("post后： " + jsonStr2);
		System.err.println(json2);
		System.err.println(tissue_num);*/
		
		//纸巾出纸下单
	}
	
	public static void main_1(String[] a){
		Long ts=System.currentTimeMillis();
		String url="http://www.zhenhuaonline.cn/api/in/thirdparty/op_app/v1/handle?"
				+ "app_id=a_MOkygXH5luDUjV&timestamp="+ts+"&sign=";
		//String params=getMD5("timestamp="+ts+"api=User.GetVmOpenidapp_username=lktest001create_user=1");
		String param1 = getMD5("api=User.GetVmOpenidapp_username=leiyutestcreate_user=1"+"timestamp="+ts + "zOH7c2QJb63hyejLsFWGJ3mJ58P5UsXZ");
		url+=param1;
		param1="api=User.GetVmOpenid&app_username=leiyutest&create_user=1";
		String jsonStr = sendPost(url, param1);
		/*JSONObject jo = JSONObject.fromObject(jsonStr);
		String json = jo.get("api_data").toString();
		JSONObject js1 = JSONObject.fromObject(json);
		String vm_openid = js1.getString("vm_openid").toString();*/
		System.err.println("加密后： " + url); 
		System.err.println("post后： " + jsonStr);
		/*System.err.println(json);
		System.err.println(vm_openid);
		*/
		/*//获取用户可领取纸巾数
		String param2 = getMD5("api=User.Tissue.DrawTissuevm_openid="+vm_openid+"timestamp="+ts + "zOH7c2QJb63hyejLsFWGJ3mJ58P5UsXZ");
		url += param2;
		param2 = "api=User.GetVmOpenid&vm_openid"+vm_openid;
		
		String jsonStr2 = sendPost(url, param2);
		JSONObject jo2 = JSONObject.fromObject(jsonStr2);
		String json2 = jo2.get("api_data").toString();
		JSONObject js2 = JSONObject.fromObject(json2);
		String tissue_num = js2.getString("tissue_num").toString();
		System.err.println("加密后： " + url); 
		System.err.println("post后： " + jsonStr2);
		System.err.println(json2);
		System.err.println(tissue_num);*/
		
		//纸巾出纸下单
	}
	
	
//	public String sign(String param){
//		
//	}
	 // 测试方法  
//    public static void main(String[] args) {  
//    String pwd = "123456";  
//    System.out.println("加密前： " + pwd);  
//    System.err.println("加密后： " + MD5Util.getMD5(pwd));  
//    }  
  
    /** 
     * 生成md5 
     *  
     * @param message 
     * @return 
     */  
    public static String getMD5(String message) {  
    String md5str = "";  
    try {  
        // 1 创建一个提供信息摘要算法的对象，初始化为md5算法对象  
        MessageDigest md = MessageDigest.getInstance("MD5");  
  
        // 2 将消息变成byte数组  
        byte[] input = message.getBytes();  
  
        // 3 计算后获得字节数组,这就是那128位了  
        byte[] buff = md.digest(input);  
  
        // 4 把数组每一字节（一个字节占八位）换成16进制连成md5字符串  
        md5str = bytesToHex(buff);
        md5str = md5str.toLowerCase();
        System.out.println(md5str);
  
    } catch (Exception e) {  
        e.printStackTrace();  
    }  
    return md5str;  
    }  
    
    /** 
     * 二进制转十六进制 
     *  
     * @param bytes 
     * @return 
     */  
    public static String bytesToHex(byte[] bytes) {  
    StringBuffer md5str = new StringBuffer();  
    // 把数组每一字节换成16进制连成md5字符串  
    int digital;  
    for (int i = 0; i < bytes.length; i++) {  
        digital = bytes[i];  
  
        if (digital < 0) {  
        digital += 256;  
        }  
        if (digital < 16) {  
        md5str.append("0");  
        }  
        md5str.append(Integer.toHexString(digital));  
    }  
    return md5str.toString().toUpperCase();  
    } 
    /**
     * 向指定 URL 发送POST方法的请求
     * 
     * @param url
     *            发送请求的 URL
     * @param param
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果
     */
    public static String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！"+e);
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally{
            try{
                if(out!=null){
                    out.close();
                }
                if(in!=null){
                    in.close();
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
        return result;
    }    
}