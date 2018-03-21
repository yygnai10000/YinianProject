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
		System.err.println("���ܺ� " + url); 
		System.err.println("post�� " + jsonStr);
		/*System.err.println(json);
		System.err.println(vm_openid);
		*/
		/*//��ȡ�û�����ȡֽ����
		String param2 = getMD5("api=User.Tissue.DrawTissuevm_openid="+vm_openid+"timestamp="+ts + "zOH7c2QJb63hyejLsFWGJ3mJ58P5UsXZ");
		url += param2;
		param2 = "api=User.GetVmOpenid&vm_openid"+vm_openid;
		
		String jsonStr2 = sendPost(url, param2);
		JSONObject jo2 = JSONObject.fromObject(jsonStr2);
		String json2 = jo2.get("api_data").toString();
		JSONObject js2 = JSONObject.fromObject(json2);
		String tissue_num = js2.getString("tissue_num").toString();
		System.err.println("���ܺ� " + url); 
		System.err.println("post�� " + jsonStr2);
		System.err.println(json2);
		System.err.println(tissue_num);*/
		
		//ֽ���ֽ�µ�
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
		System.err.println("���ܺ� " + url); 
		System.err.println("post�� " + jsonStr);
		/*System.err.println(json);
		System.err.println(vm_openid);
		*/
		/*//��ȡ�û�����ȡֽ����
		String param2 = getMD5("api=User.Tissue.DrawTissuevm_openid="+vm_openid+"timestamp="+ts + "zOH7c2QJb63hyejLsFWGJ3mJ58P5UsXZ");
		url += param2;
		param2 = "api=User.GetVmOpenid&vm_openid"+vm_openid;
		
		String jsonStr2 = sendPost(url, param2);
		JSONObject jo2 = JSONObject.fromObject(jsonStr2);
		String json2 = jo2.get("api_data").toString();
		JSONObject js2 = JSONObject.fromObject(json2);
		String tissue_num = js2.getString("tissue_num").toString();
		System.err.println("���ܺ� " + url); 
		System.err.println("post�� " + jsonStr2);
		System.err.println(json2);
		System.err.println(tissue_num);*/
		
		//ֽ���ֽ�µ�
	}
	
	
//	public String sign(String param){
//		
//	}
	 // ���Է���  
//    public static void main(String[] args) {  
//    String pwd = "123456";  
//    System.out.println("����ǰ�� " + pwd);  
//    System.err.println("���ܺ� " + MD5Util.getMD5(pwd));  
//    }  
  
    /** 
     * ����md5 
     *  
     * @param message 
     * @return 
     */  
    public static String getMD5(String message) {  
    String md5str = "";  
    try {  
        // 1 ����һ���ṩ��ϢժҪ�㷨�Ķ��󣬳�ʼ��Ϊmd5�㷨����  
        MessageDigest md = MessageDigest.getInstance("MD5");  
  
        // 2 ����Ϣ���byte����  
        byte[] input = message.getBytes();  
  
        // 3 ��������ֽ�����,�������128λ��  
        byte[] buff = md.digest(input);  
  
        // 4 ������ÿһ�ֽڣ�һ���ֽ�ռ��λ������16��������md5�ַ���  
        md5str = bytesToHex(buff);
        md5str = md5str.toLowerCase();
        System.out.println(md5str);
  
    } catch (Exception e) {  
        e.printStackTrace();  
    }  
    return md5str;  
    }  
    
    /** 
     * ������תʮ������ 
     *  
     * @param bytes 
     * @return 
     */  
    public static String bytesToHex(byte[] bytes) {  
    StringBuffer md5str = new StringBuffer();  
    // ������ÿһ�ֽڻ���16��������md5�ַ���  
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
     * ��ָ�� URL ����POST����������
     * 
     * @param url
     *            ��������� URL
     * @param param
     *            ����������������Ӧ���� name1=value1&name2=value2 ����ʽ��
     * @return ������Զ����Դ����Ӧ���
     */
    public static String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // �򿪺�URL֮�������
            URLConnection conn = realUrl.openConnection();
            // ����ͨ�õ���������
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // ����POST�������������������
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // ��ȡURLConnection�����Ӧ�������
            out = new PrintWriter(conn.getOutputStream());
            // �����������
            out.print(param);
            // flush������Ļ���
            out.flush();
            // ����BufferedReader����������ȡURL����Ӧ
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("���� POST ��������쳣��"+e);
            e.printStackTrace();
        }
        //ʹ��finally�����ر��������������
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