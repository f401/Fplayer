package io.github.f401.jbplayer;

import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
	private static final String TAG = "MusicService";
	private RemoteCallbackList<IOnMusicChangeListener> mOnChangeCallback;
	private MusicQueue mMusicQueue;
    private List<MusicDetail> mMusicList;
	private MediaPlayer mPlayer;
	private MusicPlayMode mPlayMode = MusicPlayMode.SEQUENCE;
	
    private final IMusicService.Stub BINDER = new IMusicService.Stub() {

		@Override
		public void fetchMusicList(final String path, final IMusicServiceInitFinishCallback callback) throws RemoteException {
			Log.i(TAG, "Started to fetch music list. from " + path);
			App.getThreadPool().submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					List<MusicDetail> res = Utils.readMusicDetail(Utils.discoveryMusic(new File(path)));
					Collections.sort(res);
					mMusicList = res;
					mMusicQueue = new MusicQueue(res);
					callback.loadFinished(res);
					Log.i(TAG, "Finished reading, Songs count " + mMusicList.size());
					return null;
				}
			});
		}

		@Override
		public void registerOnMusicChangeListener(IOnMusicChangeListener listener) {
			mOnChangeCallback.register(listener);
		}

		@Override
		public long getCurrentMusicProgress() {
			return mPlayer.getCurrentPosition();
		}

		@Override
		public void playPreviousSong() {
            try {
                MusicService.this.playPreviousSong();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

		@Override
		public void playNextSong() {
            try {
                MusicService.this.playNextSong();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

		@Override
		public void doPause() throws RemoteException {
			if (mPlayer.isPlaying())
				mPlayer.pause();
		}

		@Override
		public void doContinue() throws RemoteException {
			if (!mPlayer.isPlaying())
				mPlayer.start();
		}

		@Override
		public void replaceCurrentMusic(MusicDetail music) throws RemoteException {
			mMusicQueue.replaceCurrent(music);
            try {
                replayCurrentSong();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

		@Override
		public MusicPlayMode getCurrentMode() {
			return mPlayMode;
		}

		@Override
		public void setCurrentMode(MusicPlayMode music) throws RemoteException {
			mPlayMode = music;
		}
	};

	/** Before invoke it, you should fix queue */
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
		notifyMusicChange();
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
        try {
			playNextSong();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	private void replayCurrentSong() throws IOException {
		MusicDetail detail = mMusicQueue.currentSong();
		doPlayMusic(detail);
		notifyMusicChange();
	}

	private void playNextSong() throws IOException {
		switch (mPlayMode) {
			case SEQUENCE: {
				MusicDetail detail = mMusicQueue.nextSong();
				doPlayMusic(detail);
				notifyMusicChange();
				break;
			}
			case RANDOM:
				mMusicQueue.replaceCurrent(mMusicList.get(App.RANDOM.nextInt(mMusicList.size())));
			case LOOP:  // fall through
				replayCurrentSong();
		}
	}

	private void playPreviousSong() throws IOException {
		MusicDetail detail = mMusicQueue.playPreviousMusic();
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
