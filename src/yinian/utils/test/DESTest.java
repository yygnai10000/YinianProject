package yinian.utils.test;

import java.net.URL;
import java.net.URLDecoder;

import yinian.common.CommonParam;
import yinian.utils.DES;

public class DESTest {
	public static void main(String[] a){
		String a1="1";
		try{
			String b="Uga7jZc%2FaoQ%3D";
			b=URLDecoder.decode(b);
		a1=DES.decryptDES(b, CommonParam.DESSecretKey);
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			System.out.println(a1);
		}
	}
}
