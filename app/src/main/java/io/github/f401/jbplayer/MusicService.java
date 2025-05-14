package io.github.f401.jbplayer;

import android.app.Service;
import android.content.Intent;
import android.icu.text.Transliterator;
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
import androidx.annotation.Nullable;

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
		public long getCurrentMusicDurtion() {
			return mPlayer.getDuration();
		}

		@Override
		public void seekCurrentMusicTo(int msec) {
			mPlayer.seekTo(msec);
		}

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
		public long getCurrentMusicPosition() {
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
		public void playNextSong() throws RemoteException {
            try {
                MusicService.this.playNextSong();
            } catch (IOException e) {
                throw new RemoteException("IOException " + Log.getStackTraceString(e));
            }
        }

		@Override
		public void doPause() {
			if (mPlayer.isPlaying())
				mPlayer.pause();
		}

		@Override
		public void doContinue() {
			if (!mPlayer.isPlaying())
				mPlayer.start();
		}

		@Override
		public void replaceCurrentMusic(MusicDetail music) throws RemoteException {
            try {
				replaceCurrentSongAndPlay(music);
            } catch (IOException e) {
				throw new RemoteException("IOException " + Log.getStackTraceString(e));
			}
        }

		@Override
		public MusicPlayMode getCurrentMode() {
			return mPlayMode;
		}

		@Override
		public void setCurrentMode(MusicPlayMode music) {
			mPlayMode = music;
		}
	};

	/** Before invoke it, you should fix queue */
	private void doPlayMusic(MusicDetail detail) throws IOException {
		mPlayer.reset();
		mPlayer.setOnPreparedListener(this);
		mPlayer.setOnCompletionListener(this);
		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		Log.i(TAG, "Trying to play music " + detail);
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
			switch (mPlayMode) {
				case SEQUENCE:
					playNextSong();
					break;
				case RANDOM:
					replaceCurrentSongAndPlay(mMusicList.get(App.RANDOM.nextInt(mMusicList.size())));
					break;
				case LOOP:
					replayCurrentSong();
			}
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	private void replaceCurrentSongAndPlay(MusicDetail detail) throws IOException {
		if (mMusicQueue == null) {
			Log.w(TAG, "Empty music queue for request replay");
			return;
		}
		mMusicQueue.replaceCurrent(detail);
		replayCurrentSong();
	}

	private void replayCurrentSong() throws IOException {
		if (mMusicQueue == null) {
			Log.w(TAG, "Empty music queue for request replay");
			return;
		}
		MusicDetail detail = mMusicQueue.currentSong();
		doPlayMusic(detail);
	}

	private void playNextSong() throws IOException {
		if (mMusicQueue == null) {
			Log.w(TAG, "Empty music queue for request next");
			return;
		}
		MusicDetail detail = mMusicQueue.nextSong();
		doPlayMusic(detail);
	}

	private void playPreviousSong() throws IOException {
		if (mMusicQueue == null) {
			Log.w(TAG, "Empty music queue for request previous");
			return;
		}
		MusicDetail detail = mMusicQueue.playPreviousMusic();
		doPlayMusic(detail);
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
