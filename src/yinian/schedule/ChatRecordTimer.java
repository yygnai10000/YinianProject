package yinian.schedule;

import java.util.TimerTask;

import yinian.controller.IMController;

public class ChatRecordTimer extends TimerTask {

	@Override
	public void run() {
		// TODO Auto-generated method stub
		IMController im = new IMController();
		im.ExportChatRecord();
	}

}
