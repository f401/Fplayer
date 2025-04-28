package io.github.f401.jbplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.github.f401.jbplayer.adapters.MusicListAdapter;
import io.github.f401.jbplayer.databinding.MainBinding;
import java.util.List;

public class MainActivity extends AppCompatActivity {
	
    private MainBinding binding;
	private IMusicService mService;
	private List<MusicDetail> mMusicList;
	private final ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = IMusicService.Stub.asInterface(service);
			onServiceStarted();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
		}
	};
	
	private void onServiceStarted() {
		try {
			mService.fetchMusicList(App.getSearchRoot(), new IMusicServiceInitFinishCallback.Stub() {

					@Override
					public void loadFinished(final List<MusicDetail> src) throws RemoteException {
						runOnUiThread(new Runnable() {

								@Override
								public void run() {
									mMusicList = src;
									showMusicList();
								}
							});
					}
			});

		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void showMusicList() {
		binding.mainLoadingMusic.setVisibility(View.INVISIBLE);
		binding.mainMusicList.setVisibility(View.VISIBLE);
		
		MusicListAdapter adapter = new MusicListAdapter(this, mMusicList);
		binding.mainMusicList.setAdapter(adapter);
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = MainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11及以上
			if (!Environment.isExternalStorageManager()) {
				Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
				intent.setData(Uri.parse("package:" + getPackageName()));
				startActivityForResult(intent, 1024);
			}
		}
		
		if (TextUtils.isEmpty(App.getSearchRoot())) {
			binding.mainLoadingMusic.setVisibility(View.INVISIBLE);
		}
		
        binding.toolbar.setNavigationIcon(R.drawable.ic_menu);
		binding.toolbar.setNavigationOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					binding.getRoot().openDrawer(Gravity.START);
				}
			});
		binding.mainMusicList.setLayoutManager(new LinearLayoutManager(this));
		
		if (!TextUtils.isEmpty(App.getSearchRoot())) {
			Intent intent = new Intent(this, MusicService.class);
			bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		}
		//new ViewModelProvider(this, new ViewModelProvider.NewInstanceFactory()).get(
    }
	
}
