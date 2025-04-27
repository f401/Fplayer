package io.github.f401.jbplayer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import java.io.File;
import java.util.Collections;
import java.util.List;

public class MusicService extends Service {
	private static final String TAG = "MusicService";
    private List<MusicDetail> musicList;
	
    private final IMusicService.Stub BINDER = new IMusicService.Stub() {

		@Override
		public void fetchMusicList(final String path, final IMusicServiceInitFinishCallback callback) throws RemoteException {
			Log.i(TAG, "Started to fetch music list. from " + path);
			App.getThreadPool().submit(new Runnable() {

					@Override
					public void run() {
						try {
							List<MusicDetail> res = Utils.readMusicDetail(Utils.discoveryMusic(new File(path)));
							Collections.sort(res);
							musicList = res;
							callback.loadFinished(res);
						} catch (Exception e) {
							Log.e(TAG, "Error when reading ", e);
							throw new RuntimeException(e);
						}
						Log.i(TAG, "Finished reading, Songs count " + musicList.size());
					}
					
			});
		}
	};
    
    @Override
    public IBinder onBind(Intent intent) {
        return BINDER;
    }
    
	
}
