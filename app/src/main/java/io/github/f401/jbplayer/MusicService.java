package io.github.f401.jbplayer;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
	private static final String TAG = "MusicService";
	private RemoteCallbackList<IOnMusicChangeListener> mOnChangeCallback;
	private MusicQueue mMusicQueue;
    private List<MusicDetail> mMusicList;
	private MediaPlayer mPlayer;
	
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
							mMusicList = res;
							mMusicQueue = new MusicQueue(res);
							callback.loadFinished(res);
						} catch (Exception e) {
							Log.e(TAG, "Error when reading ", e);
							throw new RuntimeException(e);
						}
						Log.i(TAG, "Finished reading, Songs count " + mMusicList.size());
					}
					
			});
		}

		@Override
		public void registerOnMusicChangeListener(IOnMusicChangeListener listener) throws RemoteException {
			mOnChangeCallback.register(listener);
		}
	};

	private void doPlayMusic(MusicDetail detail) throws IOException {
		mPlayer.reset();
		mPlayer.setOnPreparedListener(this);
		mPlayer.setOnCompletionListener(this);
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mPlayer.setDataSource(detail.getPath().getPath());
		mPlayer.prepareAsync();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mPlayer = new MediaPlayer();
		mOnChangeCallback = new RemoteCallbackList<>();
	}

	@Override
    public IBinder onBind(Intent intent) {
        return BINDER;
    }

	@Override
	public void onDestroy() {
		super.onDestroy();
		mPlayer.stop();
		mPlayer.release();
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		mp.start();
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
        try {
            nextSong();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	private void nextSong() throws IOException {
		MusicDetail detail = mMusicQueue.nextSong();
		doPlayMusic(detail);
		notifyMusicChange();
	}

	private void notifyMusicChange() {
		int cnt = mOnChangeCallback.beginBroadcast();
		for (int i = 0; i < cnt; ++i) {
            try {
                mOnChangeCallback.getBroadcastItem(i).onChange(mMusicQueue.currentSong());
            } catch (RemoteException e) {
                Log.e(TAG, "ERROR WHEN INVOKE CALLBACK", e);
            }
        }
		mOnChangeCallback.finishBroadcast();
	}
}
