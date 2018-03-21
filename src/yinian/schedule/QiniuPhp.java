package yinian.schedule;

import java.util.TimerTask;
import java.util.UUID;

import yinian.utils.HttpUtils;

public class QiniuPhp extends TimerTask{

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	public static void main(String[] a){
		HttpUtils utils=new HttpUtils();
		UUID uuid = UUID.randomUUID();
		System.out.println(utils.sendPost("http://localhost/~liukai/YinianProject/pic2.php", "name=_"+uuid));
	}

}
