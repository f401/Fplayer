package io.github.f401.jbplayer;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.PowerManager;
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
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener {
	public static final String EXTRA_START_FIRST = "io.github.f401.music.START_MUSIC_SERVICE_FIRST";
	public static final String EXTRA_COMMAND = "io.github.f401.music.CMD";
	public static final String CMD_PAUSE = "pause";
	private static final String TAG = "MusicService";
	private static final int STATE_FIRST = 0;
	private static final int STATE_FINISH = 1;
	private static final int STATE_PREPARING = 2;
	private final AtomicInteger mPrepareState = new AtomicInteger(STATE_FIRST);
	private RemoteCallbackList<IOnMusicChangeListener> mOnChangeCallback;
	private MusicQueue mMusicQueue;
    private List<MusicDetail> mMusicList;
	private MediaPlayer mPlayer;
	private MusicPlayMode mPlayMode = MusicPlayMode.SEQUENCE;
	private MediaSessionCompat mMediaSession;
	private AudioFocusRequestCompat mAudioRequest;
	@Nullable
	private IMusicClient mMusicClient;
	private Future<?> mPrepareTask;
	
    private final IMusicService.Stub BINDER = new IMusicService.Stub() {

		@Override
		public long getCurrentMusicDurtion() {
			return mPrepareState.get() == STATE_FINISH ? mPlayer.getDuration() : 0;
		}

		@Override
		public void seekCurrentMusicTo(int msec) {
			if (mPrepareState.get() == STATE_FINISH) mPlayer.seekTo(msec);
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
			return mPrepareState.get() == STATE_FINISH ? mPlayer.getCurrentPosition() : 0;
		}

		@Override
		public void playPreviousSong() {
            MusicService.this.playPreviousSong();
        }

		@Override
		public void playNextSong() {
            MusicService.this.playNextSong();
        }

		@Override
		public void doPause() {
			MusicService.this.doPause();
		}

		@Override
		public void doContinue() {
			MusicService.this.doContinue();
		}

		@Override
		public void replaceCurrentMusic(MusicDetail music) {
			replaceCurrentSongAndPlay(music);
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

	private void doContinue() {
		if (mPrepareState.get() == STATE_FINISH && !mPlayer.isPlaying())
			mPlayer.start();
	}

	private void doPause() {
		if (mPrepareState.get() == STATE_FINISH && mPlayer.isPlaying())
			mPlayer.pause();
	}

	/** Before invoke it, you should fix queue */
	private void doPlayMusic(final MusicDetail detail) {
		if (mPrepareTask != null && !mPrepareTask.isDone()) {
			Log.i(TAG, "Interprete thread");
			mPrepareTask.cancel(true);
		}
		mPrepareTask = App.getThreadPool().submit(new Runnable() {

				@Override
				public void run() {
					if (!mPrepareState.compareAndSet(STATE_FIRST, STATE_PREPARING)) {
						for (; !mPrepareState.compareAndSet(STATE_FINISH, STATE_PREPARING) ; ) {}
					}
					mPlayer.reset();
					mPlayer.setWakeMode(MusicService.this, PowerManager.PARTIAL_WAKE_LOCK);
					Log.i(TAG, "Trying to play music " + detail);
					try {
						mPlayer.setDataSource(detail.getPath().getPath());
						mPlayer.prepare();
						if (!Thread.currentThread().isInterrupted()) {
							Log.d(TAG, "Playing " + detail);
							mPlayer.start();
							notifyMusicChange();
						}
					} catch (IllegalStateException | IllegalArgumentException | IOException e) {
						mPlayer.reset();
						Log.w(TAG, "Err" , e);
						throw new RuntimeException(e);
					} finally {
						mPrepareState.set(STATE_FINISH);
					}
				}
			
		});
		
		
	}

	private void notifyClientAndPause() {
		if (mMusicClient != null) {
			try {
				mMusicClient.onChangeStateToPause();
			} catch (RemoteException e) {
				Log.e(TAG, "Failed to notify client", e);
			}
		}
		doPause();
	}

	private void notifyClientAndPlay() {
		if (mMusicClient != null) {
			try {
				mMusicClient.onChangeStateToPlay();
			} catch (RemoteException e) {
				Log.e(TAG, "Failed to notify client", e);
			}
		}
		doContinue();
	}

	@Override
	public void onCreate() {
//		MediaRouter r = (MediaRouter) getSystemService(Context.MEDIA_ROUTER_SERVICE);

		mPlayer = new MediaPlayer();
		mOnChangeCallback = new RemoteCallbackList<>();
		mAudioRequest =
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
									notifyClientAndPause();
								}
							}
						})
						.build();
		AudioManagerCompat.requestAudioFocus((AudioManager) getSystemService(Context.AUDIO_SERVICE), mAudioRequest);
		mMediaSession = new MediaSessionCompat(this, "MusicService", new ComponentName(this, MediaBroadcast.class), null);
		mMediaSession.setCallback(new MediaSessionCompat.Callback() {
			@Override
			public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
				Log.i(TAG, "Got from callback btn event");
				if (handleMediaButtonEvent(mediaButtonEvent)) return true;
				return super.onMediaButtonEvent(mediaButtonEvent);
			}
		});
		mMediaSession.setActive(true);
	}
	
	
	public static class MediaBroadcast extends BroadcastReceiver {
		
		public MediaBroadcast() {}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Intent m = new Intent(context, MusicService.class);
			switch (intent.getAction()) {
				case Intent.EXTRA_KEY_EVENT: {
					m.putExtra("MediaButton", intent);
					context.startService(m);
					break;
				}
				case BluetoothDevice.ACTION_ACL_CONNECTED:
					Log.i(TAG, "Recv bluetooth c");
					break;
				case BluetoothDevice.ACTION_ACL_DISCONNECTED:
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					m.putExtra(EXTRA_COMMAND, CMD_PAUSE);
					context.startService(m);
					break;
				default:
					Log.w(TAG, "Unknown action " + intent.getAction());
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			if (intent.hasExtra("MediaButton")) {
				handleMediaButtonEvent((Intent) intent.getParcelableExtra("MediaButton"));
			} else if (intent.getBooleanExtra(EXTRA_START_FIRST, false)) {
				Log.i(TAG, "Started service");
			} else if (intent.hasExtra(EXTRA_COMMAND)) {
				Log.i(TAG, "Recv cmd " + intent.getStringExtra(EXTRA_COMMAND));
				switch (intent.getStringExtra(EXTRA_COMMAND)) {
					case CMD_PAUSE: {
						notifyClientAndPause();
						break;
					}
				}
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}

	private long mLastClickTime = 0;
	private boolean handleMediaButtonEvent(Intent mediaButtonEvent) {
		Log.i(TAG, "Recv media btn event " + mediaButtonEvent);
		KeyEvent event = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
		Log.i(TAG, "Keyevent " + event);
		try {
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				mLastClickTime = event.getEventTime();
			} else if (event.getAction() == KeyEvent.ACTION_UP) {
				if (event.getEventTime() - mLastClickTime < 2000) {
					switch (event.getKeyCode()) {
						case KeyEvent.KEYCODE_MEDIA_NEXT:
							playNextSong();
							return true;
						case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
							playPreviousSong();
							return true;
						case KeyEvent.KEYCODE_MEDIA_PAUSE:
							notifyClientAndPause();
							return true;
						case KeyEvent.KEYCODE_MEDIA_PLAY:
							notifyClientAndPlay();
							return true;
						case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
							if (mPrepareState.get() == STATE_FINISH) {
								if (mPlayer.isPlaying()) {
									notifyClientAndPause();
								} else {
									notifyClientAndPlay();
								}
							}
							return true;
						default:
							Log.w(TAG, "Unknown keycode " + event.getKeyCode());
					}
				}
				mLastClickTime = 0;
			}

		} catch (RuntimeException e) {
			Log.e(TAG, "Failed to handle bluetooth ", e);
		}
		return false;
	}

	@Override
    public IBinder onBind(Intent intent) {
        return BINDER;
    }

	@Override
	public boolean onUnbind(Intent intent) {
		mMusicClient = null;
		if (mPrepareState.get() == STATE_FINISH || !mPlayer.isPlaying()) {
			stopSelf();
		}
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mPlayer.stop();
		mPlayer.release();
		mMediaSession.release();
		AudioManagerCompat.abandonAudioFocusRequest((AudioManager) getSystemService(Context.AUDIO_SERVICE), mAudioRequest);
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {
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
       
    }

	private void replaceCurrentSongAndPlay(MusicDetail detail) {
		if (mMusicQueue == null) {
			Log.w(TAG, "Empty music queue for request replay");
			return;
		}
		mMusicQueue.replaceCurrent(detail);
		replayCurrentSong();
	}

	private void replayCurrentSong() {
		if (mMusicQueue == null) {
			Log.w(TAG, "Empty music queue for request replay");
			return;
		}
		MusicDetail detail = mMusicQueue.currentSong();
		doPlayMusic(detail);
	}

	private void playNextSong() {
		if (mMusicQueue == null) {
			Log.w(TAG, "Empty music queue for request next");
			return;
		}
		MusicDetail detail = mMusicQueue.nextSong();
		doPlayMusic(detail);
	}

	private void playPreviousSong() {
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
