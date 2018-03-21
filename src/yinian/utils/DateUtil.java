package yinian.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {
	 public static int getDaysOfMonth(Date date) {  
	        Calendar calendar = Calendar.getInstance();  
	        calendar.setTime(date);  
	        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);  
	    }  
	  
	    public static void main(String[] args) {  
	    	try{
	        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");  
	        System.out.println(getDaysOfMonth(sdf.parse("2020-02-2")));  
	    	}catch(Exception e){
	    		e.printStackTrace();
	    	}
	    }  
}
