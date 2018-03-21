package yinian.schedule;

public class CanUserdMoney {	
	private int money;
	private static CanUserdMoney instance = new CanUserdMoney();  
    private CanUserdMoney (){}  
    public static CanUserdMoney getInstance() {  
    	return instance;  
    }  
//    private int money;
////	INSTANCE;
////	private int money;
	public int getMoney(){
		return money;
	}
	public void setMoney(int money){
		this.money=money;
	}
}
