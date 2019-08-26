package com.mkyong.controller;

import java.util.ArrayList;
import java.util.List;

public class ConfigContent {

	private List<String> rawColumns = new ArrayList<String>();
	private int columnCount;
	
	public List<String> getRawColumns() {
		return rawColumns;
	}
	public void setRawColumns(List<String> rawColumns) {
		this.rawColumns = rawColumns;
	}
	public int getColumnCount() {
		return columnCount;
	}
	public void setColumnCount(int columnCount) {
		this.columnCount = columnCount;
	}
	@Override
	public String toString() {
		return "ConfigContent [rawColumns=" + rawColumns + ", columnCount=" + columnCount + "]";
	}

}
