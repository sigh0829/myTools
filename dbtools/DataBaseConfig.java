package com.hongen.db.tools;

public class DataBaseConfig {
	public static final String DB_NAME = "filedownload.db";
	public static final String DB_PATH = "/mnt/sdcard/hongen/";
	
	public static final String CreatDownloadInfoDb = "CREATE TABLE IF NOT EXISTS filedownlog("
			+ "id         INTEGER PRIMARY KEY asc AUTOINCREMENT,"
			+ "downpath       VARCHAR(100),"
			+ "threadid     INTEGER NOT NULL DEFAULT 0,"
			+ "downlength     INTEGER NOT NULL DEFAULT 0" + ");"; 
	
}
