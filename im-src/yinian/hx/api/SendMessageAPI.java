package yinian.hx.api;

import yinian.hx.comm.body.AudioMessageBody;
import yinian.hx.comm.body.CommandMessageBody;
import yinian.hx.comm.body.ImgMessageBody;
import yinian.hx.comm.body.MessageBody;
import yinian.hx.comm.body.TextMessageBody;
import yinian.hx.comm.body.VideoMessageBody;



/**
 * This interface is created for RestAPI of Sending Message, it should be
 * synchronized with the API list.
 * 
 * @author Eric23 2016-01-05
 * @see http://docs.easemob.com/doku.php?id=start:100serverintegration:
 *      50messages
 */
public interface SendMessageAPI {
	/**
	 * 发�?�消�? <br>
	 * POST
	 * 
	 * @param payload
	 *            消息�?
	 * @return
	 * @see MessageBody
	 * @see TextMessageBody
	 * @see ImgMessageBody
	 * @see AudioMessageBody
	 * @see VideoMessageBody
	 * @see CommandMessageBody
	 */
	Object sendMessage(Object payload);
}
