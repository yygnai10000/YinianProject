package yinian.controller;

import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Record;
import yinian.service.MessageCenterService;
import yinian.utils.JsonData;

import java.util.List;

/**
 * ��Ϣ���Ŀ�����������Ϣ���ĵĽӿ�
 *
 * @author yu_chen
 * @date 2018/3/16 14:48
 */
public class MessageCenterController extends Controller {

    /**
     * json������
     */
    private JsonData jsonData = new JsonData();
    /**
     * ��Ϣ���ķ���
     */
    private MessageCenterService messageCenterService = new MessageCenterService();

    /**
     * ��ȡ��Ϣ���ĵ�״̬
     */
    public void getAllMsgStatus() {
        String userId = this.getPara("userId");
        List<Record> records = messageCenterService.showMyMsgStatus(userId);
        renderText(jsonData.getSuccessJson(records));
    }


    /**
     * ��ȡ�������ڵ���������
     * Ĭ�Ϸ�ҳ20��
     */
    public void getAllComments() {
        String userId = this.getPara("userId");
        String commentId = this.getPara("commentId");
        List<Record> records = messageCenterService.selectAllComments(userId, commentId);
        renderText(jsonData.getSuccessJson(records));
    }

    /**
     * ��ȡ�������ڵ����е�������
     * Ĭ�Ϸ�ҳ20��
     */
    public void getAllLikes() {
        String userId = this.getPara("userId");
        String likeId = this.getPara("likeId");
        List<Record> records = messageCenterService.selectAllLikes(userId, likeId);
        renderText(jsonData.getSuccessJson(records));
    }

    /**
     * ��ȡĳ����Ϣ
     */
    public void readMsg() {
        String userId = this.getPara("userId");
        String msgType = this.getPara("msgType");
        messageCenterService.readMyMsg(userId, msgType);
        render(jsonData.getSuccessJson());
    }
}
