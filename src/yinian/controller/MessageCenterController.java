package yinian.controller;

import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Record;
import yinian.service.MessageCenterService;
import yinian.utils.JsonData;

import java.util.List;

/**
 * 消息中心控制器处理消息中心的接口
 *
 * @author yu_chen
 * @date 2018/3/16 14:48
 */
public class MessageCenterController extends Controller {

    /**
     * json操作类
     */
    private JsonData jsonData = new JsonData();
    /**
     * 消息中心服务
     */
    private MessageCenterService messageCenterService = new MessageCenterService();

    /**
     * 获取消息中心的状态
     */
    public void getAllMsgStatus() {
        String userId = this.getPara("userId");
        List<Record> records = messageCenterService.showMyMsgStatus(userId);
        renderText(jsonData.getSuccessJson(records));
    }


    /**
     * 获取近半年内的所有评论
     * 默认分页20条
     */
    public void getAllComments() {
        String userId = this.getPara("userId");
        String commentId = this.getPara("commentId");
        List<Record> records = messageCenterService.selectAllComments(userId, commentId);
        renderText(jsonData.getSuccessJson(records));
    }

    /**
     * 获取近半年内的所有点赞数据
     * 默认分页20条
     */
    public void getAllLikes() {
        String userId = this.getPara("userId");
        String likeId = this.getPara("likeId");
        List<Record> records = messageCenterService.selectAllLikes(userId, likeId);
        renderText(jsonData.getSuccessJson(records));
    }

    /**
     * 读取某条消息
     */
    public void readMsg() {
        String userId = this.getPara("userId");
        String msgType = this.getPara("msgType");
        messageCenterService.readMyMsg(userId, msgType);
        render(jsonData.getSuccessJson());
    }
}
