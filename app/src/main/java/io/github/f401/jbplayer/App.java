package io.github.f401.jbplayer;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.multidex.MultiDex;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App extends Application {
	public static final Random RANDOM = new Random();
	private static App global;
	private static ExecutorService threadpool;

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		MultiDex.install(this);
	}
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		CrashHandler.install(this);
		global = this;
		threadpool = Executors.newCachedThreadPool();
	}
    
	public static String getSearchRoot() {
		SharedPreferences pref = global.getSharedPreferences("defaults", Context.MODE_PRIVATE);
		return pref.getString("searchRoot", "/storage/emulated/0/Download/KuGouLite/c/");
//		return pref.getString("searchRoot", "/storage/emulated/0/Download/KuGouLite/c/");
	}
	
	public static void setSearchRoot(String path) {
		SharedPreferences.Editor editor = global.getSharedPreferences("defaults", Context.MODE_PRIVATE).edit();
		editor.putString("searchRoot", path);
		editor.apply();
	}
	
	public static ExecutorService getThreadPool() {
		return threadpool;
	}

    public static int dp2px(int dp) {
        return Math.round(global.getResources().getDisplayMetrics().density * dp);
    }
}
