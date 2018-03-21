package yinian.schedule;

import java.util.List;
import java.util.Random;
import java.util.TimerTask;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

public class AddPeopleCount extends TimerTask {
	
	@Override
	public void run() {
		List<Record> list = Db.find("select * from yntemp where id=46");
		if(null!=list && list.size()!=0) {
			Long num = Long.parseLong(list.get(0).get("remark"));
			int s = getRandom(10000,99999);
			System.out.println(s);
			Long count = num + s;
			Db.update("update yntemp set remark="+count+" where id=46");			
		}
    }
	
	public static int getRandom(int min, int max){
	    Random random = new Random();
	    int s = random.nextInt(max) % (max - min + 1) + min;
	    return s;
	}
}
