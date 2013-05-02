package com.hongen.db.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.database.sqlite.SQLiteDatabase;

import com.hongen.db.entity.FileInfoDb;
import com.hongen.updateapk.tools.Tools;

public class DbManager {
	private static final String TAG = "DbManager";
	private static final SqliteDAO sqlDao_ = SqliteDAO.getInstance();

	/**
	 * 获取每条线程已经下载的文件长度
	 * 
	 * @param path
	 * @return
	 */
	public static Map<Integer, Integer> getData(String path) {
		List<FileInfoDb> list = sqlDao_.loadAll(new FileInfoDb(), "downpath",
				new String[] { "" + path });
		Map<Integer, Integer> data = new HashMap<Integer, Integer>();
		if(Tools.listIsNotNull(list)){
			for (int i = 0, len = list.size(); i < len; i++) {
				FileInfoDb record = list.get(i);
				data.put(record.getThreadid(), record.getDownlength());
			}
		}		
		return data;
	}

	/**
	 * 保存每条线程已经下载的文件长度
	 * 
	 * @param path
	 * @param map
	 */
	public static void save(String path, Map<Integer, Integer> map) {// int threadid,
																// int position
		try {
			for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
				FileInfoDb record = new FileInfoDb();
				record.setDownpath(path);
				record.setThreadid(entry.getKey());
				record.setDownlength(entry.getValue());
				sqlDao_.insert(record);
			}
		} catch (Exception e) {

		}
	}
	
	/**
	 * 实时更新每条线程已经下载的文件长度
	 * @param path
	 * @param map
	 */
	public static void update(String path, Map<Integer, Integer> map){
		ArrayList<Object[]> list = new ArrayList<Object[]>();
		for(Map.Entry<Integer, Integer> entry : map.entrySet()){			
			Object[] item = new Object[]{entry.getValue(), path, entry.getKey()};
			list.add(item);
		}		
		update(path,list);
	}
	
	public static void update(String path, ArrayList<Object[]> list){
		String sql = "update filedownlog set downlength=? where threadid=? and downpath=?";
		sqlDao_.updateByTransaction(sql,list);		
	}
	
	/**
	 * 当文件下载完成后，删除对应的下载记录
	 * @param path
	 */
	public static void delete(String path){
		sqlDao_.deleteBySql("delete from filedownlog where downpath=?", new String[]{path});
	}
}
