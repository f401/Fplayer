package io.github.f401.jbplayer;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.Context;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class App extends Application {
	public static final Random RANDOM = new Random();
	private static App global;
	private static ExecutorService threadpool;
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
	
	public static ExecutorService getThreadPool() {
		return threadpool;
	}
}
