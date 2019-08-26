package com.mkyong.controller;

import java.util.ArrayList;
import java.util.List;

public class UploadData {

	private String filename;
    private String pid;
    private String processor;
    private long itemCount;
    private List<List<String>> data = new ArrayList<List<String>>();
    private boolean isLast;
    
    public UploadData() {
		// TODO Auto-generated constructor stub
	}
    
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public String getPid() {
		return pid;
	}
	public void setPid(String pid) {
		this.pid = pid;
	}
	public String getProcessor() {
		return processor;
	}
	public void setProcessor(String processor) {
		this.processor = processor;
	}
	public long getItemCount() {
		return itemCount;
	}
	public void setItemCount(long itemCount) {
		this.itemCount = itemCount;
	}
	public List<List<String>> getData() {
		return data;
	}
	public void setData(List<List<String>> data) {
		this.data = data;
	}

	public boolean isLast() {
		return isLast;
	}

	public void setLast(boolean isLast) {
		this.isLast = isLast;
	}

	
}
