package com.mkyong.controller;

public class Utility {
	
	public static String getBasePath(){
		String basePath = null;
		
		switch (OSUtil.getOS()) {
		case WINDOWS:
			basePath = "C:\\temp\\";
                        break;
		case MAC:
			basePath = "/root/projects/xlsx-uploader/";
                        break;
		case LINUX:
			basePath = "/root/projects/xlsx-uploader/";
                        break;
		case SOLARIS:
			basePath = "/root/projects/xlsx-uploader/";
                        break;
		default:
			break;
		}	
		return basePath;
        }
	
}
