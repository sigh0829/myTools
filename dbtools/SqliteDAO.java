package com.hongen.db.tools;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.hongen.db.annotation.Id;
import com.hongen.db.annotation.Table;
import com.hongen.db.annotation.Transient;
import com.hongen.updateapk.tools.ResourceHelper;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class SqliteDAO {
	private static final String TAG = "SqliteDao";
	private static SqliteDAO instance;
	private int conflictType = 2;
	
	private SqliteDAO(){		
	}
	
	public static SqliteDAO getInstance(){
		if(null == instance){
			instance = new SqliteDAO();
		}
		return instance;
	}
	
	
	public SQLiteDatabase getMySQLiteDatabase() {
		File f = ResourceHelper.getResourceFile(DataBaseConfig.DB_PATH,
				DataBaseConfig.DB_NAME);		
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(f, null);
		return db;
	}
	
	public int getPrimerkey(Object entity) {
		SQLiteDatabase db = getMySQLiteDatabase();
		Cursor cursor = db.rawQuery("select last_insert_rowid() from "
				+ getTableName(entity), null);
		cursor.moveToFirst();
		int strid = cursor.getInt(0);
		cursor.close();
		db.close();
		return strid;
	}
	
	//insert
	public <T> int insert(T entity) {
		return insert(entity, true);
	}
	
	private <T> int insert(T entity, boolean selective) {
		SQLiteDatabase db = getMySQLiteDatabase();
		ContentValues values = getContentValues(entity, selective);
		T exist_obj = this.loadByPrimaryKey(entity);
		if (exist_obj != null) {
			return 0;
		}

		long r = 0;
		r = db.insert(getTableName(entity), null, values);
		/*
		 * if(conflictType==2){ r=db.replace(getTableName(entity), null,values);
		 * }else{
		 * 
		 * r=db.insert(getTableName(entity), null, values); }
		 */
		
		db.close();

		if (r >= 0) {
			return (int) r;
		}		
		
		return -1;
	}
	
	
	//update 
	public int updateByPrimaryKey(Object entity) {
		return updateByPrimaryKey(entity, false);
	}
	
	public int updateByPrimaryKeySelective(Object entity) {
		return updateByPrimaryKey(entity, true);
	}

	private int updateByPrimaryKey(Object entity, boolean selective) {
		SQLiteDatabase db = getMySQLiteDatabase();
		ContentValues values = getContentValues(entity, selective);
		Object[] args = getPrimarySelectionAndArgs(entity);

		int r = db.update(getTableName(entity), values, (String) args[0],
				(String[]) args[1]);
		db.close();
		return r;
	}

	public int updateByPrimaryKey(String tableName, Object entity,
			boolean selective) {
		SQLiteDatabase db = getMySQLiteDatabase();
		ContentValues values = getContentValues(entity, selective);
		Object[] args = getPrimarySelectionAndArgs(entity);

		int r = db.update(tableName, values, (String) args[0],
				(String[]) args[1]);
		db.close();
		return r;
	}
	
	public boolean updateByTransaction(String sql,ArrayList<Object[]> list){
		boolean rs = false;
		SQLiteDatabase db = getMySQLiteDatabase();
		db.beginTransaction();
		try{
			for(int i=0,len=list.size();i<len;i++){
				db.execSQL(sql,list.get(i));
			}
			db.setTransactionSuccessful();
			rs = true;	
			return rs;
		}finally{
			db.endTransaction();
			db.close();			
		}
	}
	
	//delete
	public <T> int delete(T entity) {
		SQLiteDatabase db = getMySQLiteDatabase();
		Object[] args = getPrimarySelectionAndArgs(entity);
		int rs = db.delete(getTableName(entity), (String) args[0],(String[]) args[1]);
		db.close();
		return rs;
	}
	
	public <T> void deleteBySql(String sql, String[] selectionArgs) {
		SQLiteDatabase db = getMySQLiteDatabase();
		db.rawQuery(sql, selectionArgs);		
		db.close();
	}
	
	
	//composite method
	public <T> T loadByPrimaryKey(T entity) {
		SQLiteDatabase db = getMySQLiteDatabase();
		Object[] args = getPrimarySelectionAndArgs(entity);
		Cursor cursor = db.query(getTableName(entity), null, (String) args[0],
				(String[]) args[1], null, null, null);
		try {
			if (cursor.moveToNext()) {
				T db_entity = getEntity(cursor, entity);
				return db_entity;
			} else {
				return null;
			}
		} finally {
			cursor.close();
			db.close();
		}
	}

	public <T> T loadByPrimaryKey(String tableName, T entity) {
		SQLiteDatabase db = getMySQLiteDatabase();
		Object[] args = getPrimarySelectionAndArgs(entity);
		Cursor cursor = db.query(tableName, null, (String) args[0],
				(String[]) args[1], null, null, null);
		try {
			if (cursor.moveToNext()) {
				T db_entity = getEntity(cursor, entity);
				return db_entity;
			} else {
				return null;
			}
		} finally {
			cursor.close();
			db.close();
		}
	}

	public <T> List<T> getEntityListByCursor(T entity, Cursor cursor) {
		List<T> entities = new ArrayList<T>();
		try {
			if (cursor != null && cursor.moveToFirst()) {
				do {
					T obj = (T) entity.getClass().newInstance();
					getEntity(cursor, obj);
					entities.add(obj);
				} while (cursor.moveToNext());
			}
			return entities;
		} catch (Exception e) {
			Log.e(TAG, "" + e, e);
			return entities;
		} finally {
			//cursor.close();
		}
	}

	public <T> List<T> loadAll(T entity, String columnName,
			String[] selectionArgs) {
		SQLiteDatabase db = getMySQLiteDatabase();
		String selection = columnName + "=?";
		Cursor cursor = db.query(getTableName(entity), null, selection,
				selectionArgs, null, null, null);
		List<T> rs = getEntityListByCursor(entity, cursor);
		cursor.close();
		db.close();
		return rs;
	}

	public <T> List<T> loadAll(T entity, String orderBy) {
		SQLiteDatabase db = getMySQLiteDatabase();
		Cursor cursor = db.query(getTableName(entity), null, null, null, null,
				null, orderBy);
		List<T> rs = getEntityListByCursor(entity, cursor);
		cursor.close();
		db.close();
		return rs;
	}

	public <T> List<T> loadAll(T entity, String[] columns, String selection,
			String[] selectionArgs, String groupBy, String having,
			String orderBy) {
		SQLiteDatabase db = getMySQLiteDatabase();
		Cursor cursor = db.query(getTableName(entity), columns, selection,
				selectionArgs, groupBy, having, orderBy);
		List<T> rs = getEntityListByCursor(entity, cursor);
		cursor.close();
		db.close();
		return rs;
	}

	public <T> List<T> loadAllBySql(T entity, String sql, String[] selectionArgs) {
		SQLiteDatabase db = getMySQLiteDatabase();
		Cursor cursor = db.rawQuery(sql, selectionArgs);
		List<T> rs = getEntityListByCursor(entity, cursor);
		cursor.close();
		db.close();
		return rs;
	}

	public int getRecordsCount(Object entity, String[] columns,
			String selection, String[] selectionArgs, String groupBy,
			String having, String orderBy) {
		SQLiteDatabase db = getMySQLiteDatabase();
		Cursor cursor = db.query(getTableName(entity), columns, selection,
				selectionArgs, groupBy, having, orderBy);
		int rs = cursor.getCount();
		cursor.close();
		db.close();
		return rs;
	}

	public int getRecordsCount(String sql, String[] selectionArgs) {
		SQLiteDatabase db = getMySQLiteDatabase();
		Cursor cursor = db.rawQuery(sql, selectionArgs);
		int rs = cursor.getCount();
		cursor.close();
		db.close();
		return rs;
	}

	public int getSumByKey(Object entity, String sumcolumnName,
			String columnName, String[] selectionArgs) {
		SQLiteDatabase db = getMySQLiteDatabase();
		String selection = columnName + "=?";
		String[] columns = new String[] { "SUM(" + sumcolumnName + ")" };
		Cursor cursor = db.query(getTableName(entity), columns, selection,
				selectionArgs, null, null, null, null);
		cursor.moveToFirst();
		int sum = cursor.getInt(0);
		cursor.close();
		db.close();
		return sum;
	}

	public int getIntResult(String sql, String[] selectionArgs) {
		SQLiteDatabase db = getMySQLiteDatabase();
		Cursor cursor = db.rawQuery(sql, selectionArgs);
		cursor.moveToFirst();
		int sum = cursor.getInt(0);
		cursor.close();
		db.close();
		return sum;
	}

	public <T> T getRecordBySql(T Entity, String sql, String[] selectionArgs) {
		SQLiteDatabase db = getMySQLiteDatabase();
		Cursor cursor = db.rawQuery(sql, selectionArgs);
		List<T> list = getEntityListByCursor(Entity, cursor);
		cursor.close();
		db.close();
		if (null != list && list.size() > 0) {
			return list.get(0);
		} else {
			return null;
		}
	}

	
	
	//reflect
	/**	 
	 * @param entity
	 * @return
	 */
	private Object[] getPrimarySelectionAndArgs(Object entity) {
		Object[] ret = new Object[2];
		String selection = null;
		List<String> args = new ArrayList<String>();
		try {
			Class<?> entity_class = entity.getClass();
			Field[] fs = entity_class.getDeclaredFields();
			for (Field f : fs) {
				if (isPrimaryKey(f)) {
					Method get = getGetMethod(entity_class, f);
					if (get != null) {
						Object o = get.invoke(entity);
						String value = null;
						if (o != null) {
							value = o.toString();
							if (selection == null) {
								selection = f.getName() + "=?";
							} else {
								selection += " AND " + f.getName() + "=?";
							}

							args.add(value);

						} else {
							throw new RuntimeException("Primary key: "
									+ f.getName() + " must not be null");
						}
					}
				}
			}
			if (selection == null) {
				throw new RuntimeException("Primary key not found!");
			}

			ret[0] = selection;
			ret[1] = args.toArray(new String[args.size()]);
			return ret;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	
	private <T> T getEntity(Cursor cursor, T entity) {
		try {
			Class<?> entity_class = entity.getClass();

			Field[] fs = entity_class.getDeclaredFields();
			for (Field f : fs) {
				int index = cursor.getColumnIndex(f.getName());
				if (index >= 0) {
					Method set = getSetMethod(entity_class, f);
					if (set != null) {
						String value = cursor.getString(index);
						if (cursor.isNull(index)) {
							value = null;
						}
						Class<?> type = f.getType();
						if (type == String.class) {
							set.invoke(entity, value);
						} else if (type == int.class || type == Integer.class) {
							set.invoke(entity, value == null ? (Integer) null
									: Integer.parseInt(value));
						} else if (type == float.class || type == Float.class) {
							set.invoke(entity, value == null ? (Float) null
									: Float.parseFloat(value));
						} else if (type == long.class || type == Long.class) {
							set.invoke(entity, value == null ? (Long) null
									: Long.parseLong(value));
						} else if (type == Date.class) {
							set.invoke(entity, value == null ? (Date) null
									: stringToDateTime(value));
						} else {
							set.invoke(entity, value);
						}
					}
				}
			}
			return entity;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	
	/**
	 * 转化对象到ContentValues
	 * 
	 * @param entity
	 * @param selective
	 * @return
	 */
	private ContentValues getContentValues(Object entity, boolean selective) {
		ContentValues values = new ContentValues();
		try {
			Class<?> entity_class = entity.getClass();
			Field[] fs = entity_class.getDeclaredFields();
			for (Field f : fs) {
				if (isTransient(f) == false && isPrimaryKey(f) == false) {
					Method get = getGetMethod(entity_class, f);
					if (get != null) {
						Object o = get.invoke(entity);
						if (!selective || (selective && o != null)) {
							String name = f.getName();
							Class<?> type = f.getType();
							if (type == String.class) {
								values.put(name, (String) o);
							} else if (type == int.class
									|| type == Integer.class) {
								/*
								 * if((((Integer) o) == 0) && isPrimaryKey(f)){
								 * values.put(name,"NULL"); }else{
								 * values.put(name,(Integer)o); }
								 */
								values.put(name, (Integer) o);

							} else if (type == float.class
									|| type == Float.class) {
								values.put(name, (Float) o);
							} else if (type == long.class || type == Long.class) {
								values.put(name, (Long) o);
							} else if (type == Date.class) {
								values.put(name, datetimeToString((Date) o));
							} else {
								values.put(name, o.toString());
							}
						}
					}
				}
			}
			return values;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private String datetimeToString(Date d) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if (d != null) {
			return sdf.format(d);
		}
		return null;
	}

	private Date stringToDateTime(String s) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if (s != null) {
			try {
				return sdf.parse(s);
			} catch (ParseException e) {
				Log.e(TAG, "时间转换为: " + s, e);
			}
		}
		return null;
	}

	private Method getGetMethod(Class<?> entity_class, Field f) {
		String fn = f.getName();
		String mn = "get" + fn.substring(0, 1).toUpperCase() + fn.substring(1);
		try {
			return entity_class.getDeclaredMethod(mn);
		} catch (NoSuchMethodException e) {
			Log.w(TAG, "Method: " + mn + " not found.");

			return null;
		}
	}

	private Method getSetMethod(Class<?> entity_class, Field f) {
		String fn = f.getName();
		String mn = "set" + fn.substring(0, 1).toUpperCase() + fn.substring(1);
		try {
			return entity_class.getDeclaredMethod(mn, f.getType());
		} catch (NoSuchMethodException e) {
			Log.w(TAG, "Method: " + mn + " not found.");

			return null;
		}
	}
	
	//for annotation
	private boolean isPrimaryKey(Field f) {
		Annotation an = f.getAnnotation(Id.class);
		if (an != null) {
			return true;
		}
		return false;
	}

	private boolean isTransient(Field f) {
		Annotation an = f.getAnnotation(Transient.class);
		if (an != null) {
			return true;
		}
		return false;
	}

	private String getTableName(Object entity) {
		Table table = entity.getClass().getAnnotation(Table.class);
		String name = table.name();
		return name;
	}

	public int getConflictType() {
		return conflictType;
	}

	public void setConflictType(int conflictType) {
		this.conflictType = conflictType;
	}

}
