package yinian.hx.comm.body;


import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.apache.commons.lang3.StringUtils;

import yinian.hx.wrapper.BodyWrapper;

public class ResetPasswordBody implements BodyWrapper{
    private String newPassword;

    public ResetPasswordBody(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public ContainerNode<?> getBody() {
        return JsonNodeFactory.instance.objectNode().put("newpassword", newPassword);
    }

    public Boolean validate() {
        return StringUtils.isNotBlank(newPassword);
    }
}
