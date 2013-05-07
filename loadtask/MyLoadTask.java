package com.hongen.loadtask;

import com.hongen.myitemsv3.Main;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.View;
import android.widget.LinearLayout;

public class MyLoadTask extends AsyncTask<Void, Void, Boolean>{
	private static final String TAG ="MyLoadTask";
	private Main context;
	private LinearLayout progressbar;
	
	
	public MyLoadTask(Context context) {
		this.context = (Main) context;
	}
	
	public void setProgressbar(LinearLayout progressbar) {
		this.progressbar = progressbar;
	}
	
	public void setContext(Context context) {
		this.context = (Main) context;
	}
	
	public Context getContext() {
		return this.context;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		// TODO Auto-generated method stub
		context.doOnLoad();
		return true;
	}
	
	@Override
	protected void onPostExecute(Boolean isFinsh) {
		super.onPostExecute(isFinsh);
		if (progressbar.isShown()) {
			progressbar.setVisibility(View.GONE);
		}
		context.doAfterLoad();
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (!progressbar.isShown()) {
			progressbar.setVisibility(View.VISIBLE);
		}

	}	

}
