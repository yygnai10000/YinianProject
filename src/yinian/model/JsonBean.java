package yinian.model;

import java.util.List;
import java.util.Map;

import com.jfinal.plugin.activerecord.Record;

public class JsonBean {
	private int code;
	private String msg;
	private List<Record> data;

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public List<Record> getData() {
		return data;
	}

	public void setData(List<Record> data) {
		this.data = data;
	}
}
