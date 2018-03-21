package yinian.hx.comm.body;

import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.apache.commons.lang3.StringUtils;

import yinian.hx.wrapper.BodyWrapper;

public class ModifyNicknameBody implements BodyWrapper{

    private String nickname;

    public ModifyNicknameBody(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public ContainerNode<?> getBody() {
        return JsonNodeFactory.instance.objectNode().put("nickname", nickname);
    }

    public Boolean validate() {
        return StringUtils.isNotBlank(nickname);
    }
}
