package io.github.f401.jbplayer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.Nullable;
import androidx.media.AudioAttributesCompat;
import androidx.media.AudioFocusRequestCompat;
import androidx.media.AudioManagerCompat;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import android.content.BroadcastReceiver;
import android.app.PendingIntent;
import android.content.ComponentName;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
	private static final String TAG = "MusicService";
	private final AtomicBoolean mIsPrepareFinished = new AtomicBoolean(false);
	private RemoteCallbackList<IOnMusicChangeListener> mOnChangeCallback;
	private MusicQueue mMusicQueue;
    private List<MusicDetail> mMusicList;
	private MediaPlayer mPlayer;
	private MusicPlayMode mPlayMode = MusicPlayMode.SEQUENCE;
	private MediaSessionCompat mMediaSession;
	@Nullable
	private IMusicClient mMusicClient;
	
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
		public void setMusicClient(IMusicClient client) {
			mMusicClient = client;
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
					callback.loadFinished(new MusicList(mMusicList));
					Log.i(TAG, "Finished reading, Songs count " + mMusicList.size());
					return null;
				}
			});
		}

		@Override
		public void registerOnMusicChangeListener(IOnMusicChangeListener listener) {
			mOnChangeCallback.register(listener);
			Log.d(TAG, "Registered listener " + listener);
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
			if (mPlayer.isPlaying() && mIsPrepareFinished.get())
				mPlayer.pause();
		}

		@Override
		public void doContinue() {
			if (!mPlayer.isPlaying() && mIsPrepareFinished.get())
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
		mIsPrepareFinished.set(false);
		mPlayer.setOnPreparedListener(this);
		mPlayer.setOnCompletionListener(this);
		Log.i(TAG, "Trying to play music " + detail);
		mPlayer.setDataSource(detail.getPath().getPath());
		mPlayer.prepareAsync();
	}

	private void notifyClientPause() {
		if (mMusicClient != null) {
			try {
				mMusicClient.onChangeStateToPause();
			} catch (RemoteException e) {
				Log.e(TAG, "Failed to notify client", e);
			}
		}
	}

	private void notifyClientPlay() {
		if (mMusicClient != null) {
			try {
				mMusicClient.onChangeStateToPlay();
			} catch (RemoteException e) {
				Log.e(TAG, "Failed to notify client", e);
			}
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mPlayer = new MediaPlayer();
		mOnChangeCallback = new RemoteCallbackList<>();
		AudioFocusRequestCompat requestCompat =
				new AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
						.setAudioAttributes(new AudioAttributesCompat.Builder()
								.setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
								.setUsage(AudioAttributesCompat.USAGE_MEDIA)
								.build())
						.setOnAudioFocusChangeListener(new AudioManager.OnAudioFocusChangeListener() {
							@Override
							public void onAudioFocusChange(int focusChange) {
								Log.i(TAG, "focus change " + focusChange);
								if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
									notifyClientPause();
								}
							}
						})
						.build();
		AudioManagerCompat.requestAudioFocus((AudioManager) getSystemService(Context.AUDIO_SERVICE), requestCompat);
		mMediaSession = new MediaSessionCompat(this, "MusicService", new ComponentName(this, MediaBroadcast.class), null);
//		mMediaSession.setMediaButtonReceiver(PendingIntent.getBroadcast(this, 0, new Intent(this, MediaButtonReceiver.class), PendingIntent.FLAG_IMMUTABLE));
		mMediaSession.setCallback(new MediaSessionCompat.Callback() {
			@Override
			public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
				if (handleMediaButtonEvent(mediaButtonEvent)) return true;
				return super.onMediaButtonEvent(mediaButtonEvent);
			}
		});
		//mMediaSession.setMediaButtonReceiver(PendingIntent.getBroadcast(this, 0, new Intent(this, MediaBroadcast.class), 0));
		mMediaSession.setActive(true);
	}
	
	
	public static class MediaBroadcast extends BroadcastReceiver {
		
		public MediaBroadcast() {}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Intent m = new Intent(context, MusicService.class);
			m.putExtra("MediaButton", intent);
			context.startService(m);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.hasExtra("MediaButton")) {
			handleMediaButtonEvent((Intent) intent.getParcelableExtra("MediaButton"));
		}
		return super.onStartCommand(intent, flags, startId);
	}
	
	private boolean handleMediaButtonEvent(Intent mediaButtonEvent) {
		Log.i(TAG, "Recv media btn event " + mediaButtonEvent);
		KeyEvent event = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
		Log.i(TAG, "Keycode " + event.getKeyCode());
		try {
			switch (event.getKeyCode()) {
				case KeyEvent.KEYCODE_MEDIA_NEXT:
					playNextSong();
					return true;
				case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
					playPreviousSong();
					return true;
				case KeyEvent.KEYCODE_MEDIA_PAUSE:
					notifyClientPause();
					return true;
				case KeyEvent.KEYCODE_MEDIA_PLAY:
					notifyClientPlay();
					return true;
				case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
					if (mIsPrepareFinished.get()) {
						if (mPlayer.isPlaying()) {
							Log.i(TAG, "Playing change to pause");
							notifyClientPause();
						} else 
							notifyClientPlay();
					}
					return true;
				default:
					Log.w(TAG, "Unknown keycode " + event.getKeyCode());
			}
		} catch (IOException | RuntimeException e) {
			Log.e(TAG, "Failed to handle bluetooth ", e);
		}
		return false;
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
		mMediaSession.release();
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		mIsPrepareFinished.set(true);
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
