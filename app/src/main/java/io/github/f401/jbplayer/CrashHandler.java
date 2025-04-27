package io.github.f401.jbplayer;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import android.os.Process;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
	private static final String TAG = "CrashHandler";
	private static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss");
    private static Context mContext;
	private static CrashHandler INSTANCE;
	
	public static void install(Context context) {
		if (mContext == null) {
			mContext = context;
			INSTANCE = new CrashHandler();
			Thread.setDefaultUncaughtExceptionHandler(INSTANCE);
		}
	}
    
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		Log.e(TAG, "Got exception", e);
		writeLog(buildLog(e));
		Process.killProcess(Process.myPid());
		System.exit(1);
	}
	
	private void writeLog(String log) {
		String time = DATE_FORMAT.format(new Date());
		File file = new File(mContext.getExternalCacheDir(), "crash_" + time + ".txt");
		try {
			Utils.write(file, log.getBytes("UTF-8"));
		} catch (Throwable e) {
			e.printStackTrace();
		} 
	}
	
	private String buildLog(Throwable throwable) {
		String time = DATE_FORMAT.format(new Date());

		String versionName = "unknown";
		long versionCode = 0;
		try {
			PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
			versionName = packageInfo.versionName;
			versionCode = Build.VERSION.SDK_INT >= 28 ? packageInfo.getLongVersionCode() : packageInfo.versionCode;
		} catch (Throwable ignored) {}

		LinkedHashMap<String, String> head = new LinkedHashMap<String, String>();
		head.put("Time Of Crash", time);
		head.put("Device", String.format("%s, %s", Build.MANUFACTURER, Build.MODEL));
		head.put("Android Version", String.format("%s (%d)", Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
		head.put("App Version", String.format("%s (%d)", versionName, versionCode));
		head.put("Support Abis", Build.VERSION.SDK_INT >= 21 && Build.SUPPORTED_ABIS != null ? Arrays.toString(Build.SUPPORTED_ABIS): "unknown");
		head.put("Fingerprint", Build.FINGERPRINT);

		StringBuilder builder = new StringBuilder();

		for (String key : head.keySet()) {
			if (builder.length() != 0) builder.append("\n");
			builder.append(key);
			builder.append(" :    ");
			builder.append(head.get(key));
		}

		builder.append("\n\n");
		builder.append(Log.getStackTraceString(throwable));

		return builder.toString(); 
	}
}
