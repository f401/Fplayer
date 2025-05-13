package io.github.f401.jbplayer;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.github.f401.jbplayer.adapters.MusicListAdapter;
import io.github.f401.jbplayer.databinding.MainBinding;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import android.widget.SeekBar;

public class MainActivity extends AppCompatActivity {
	private static final String TAG = "MainActivity";
    private MainBinding binding;
	private IMusicService mService;
	private List<MusicDetail> mMusicList;
	private final AtomicBoolean isPositionUpdaterStarted = new AtomicBoolean(false);
	private final Handler mHandler = new Handler();
	private final Runnable MUSIC_POSITION_UPDATER = new Runnable() {
		@Override
		public void run() {
			long curr = 0;
			try {
				curr = mService.getCurrentMusicProgress() / 1000;
			} catch (RemoteException | RuntimeException e) {
				Log.e(TAG, "Pos update error ", e);
			}
			binding.mainMusicCurrPosTextView.setText(getString(R.string.min_second_time_fmt, curr / 60, curr % 60));
			binding.mainMusicSeekBar.setProgress((int) curr);
			if (isPositionUpdaterStarted.get()) mHandler.postDelayed(this, 1000);
		}
	};
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
	private MusicState mCurrentMusicState = MusicState.PAUSE;

	private enum MusicState { PAUSE, PLAYING }

	private void changeStateToPlaying() throws RemoteException {
		if (mCurrentMusicState == MusicState.PAUSE) {
			mCurrentMusicState = MusicState.PLAYING;
			mService.doContinue();
			binding.mainControllerBtnImg.setImageResource(android.R.drawable.ic_media_pause);
		}
	}
	
	private void changeStateToPause() throws RemoteException {
		if (mCurrentMusicState == MusicState.PLAYING) {
			mCurrentMusicState = MusicState.PAUSE;
			mService.doPause();
			binding.mainControllerBtnImg.setImageResource(android.R.drawable.ic_media_play);
		}
	}

	private void startPositionUpdater() {
		if (isPositionUpdaterStarted.compareAndSet(false, true)) {
			mHandler.post(MUSIC_POSITION_UPDATER);
		}
	}

	private void stopPositionUpdater() {
		if (isPositionUpdaterStarted.compareAndSet(true, false)) {
			mHandler.removeCallbacks(MUSIC_POSITION_UPDATER);
		}
	}
	
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
			mService.registerOnMusicChangeListener(new IOnMusicChangeListener.Stub() {
				@Override
				public void onChange(final MusicDetail detail) throws RemoteException {
					binding.mainMusicSeekBar.setMax((int) mService.getCurrentMusicDurtion() / 1000);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							applyDetailToStatusBar(detail);
							startPositionUpdater();
							try {
								changeStateToPlaying();
							} catch (RemoteException e) {
								Log.e("MainActivity", "Failed to play", e);
							}
						}
					});
				}
			});
		} catch (RemoteException e) {
			throw new RuntimeException(e);
		}

		binding.mainControllerLeftImgBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
                try {
                    mService.playPreviousSong();
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
		});

		binding.mainControllerRightImgBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
                try {
                    mService.playNextSong();
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
		});

		binding.mainControllerBtnImg.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
                try {
                    if (mCurrentMusicState == MusicState.PAUSE) {
						changeStateToPlaying();
					} else {
						changeStateToPause();
					}
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
		});
		
		binding.mainMusicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if (fromUser) {}
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}
			});
		binding.mainMusicSeekBar.setMax(100);
	}

	private void showMusicList() {
		binding.mainLoadingMusicProgressBar.setVisibility(View.GONE);
		binding.mainMusicList.setVisibility(View.VISIBLE);
		
		MusicListAdapter adapter = new MusicListAdapter(this, mMusicList);
		adapter.setOnViewClickListener(new MusicListAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(int position) {
                try {
                    mService.replaceCurrentMusic(mMusicList.get(position));
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
		});
		binding.mainMusicList.setLayoutManager(new LinearLayoutManager(this));
		binding.mainMusicList.setAdapter(adapter);
		DividerItemDecoration did = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
		binding.mainMusicList.addItemDecoration(did);

        try {
            binding.mainPlayModeTextView.setText(getString(mService.getCurrentMode().getDisplayId()));
			binding.mainPlayModeTextView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
                    try {
                        showChangePlayModeAlertDialog();
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                }
			});
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

	private void showChangePlayModeAlertDialog() throws RemoteException {
		new AlertDialog.Builder(this)
				.setSingleChoiceItems(R.array.music_play_mode_array, mService.getCurrentMode().getPos(), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
                        try {
                            mService.setCurrentMode(MusicPlayMode.valueOfArrayPos(which));
							binding.mainPlayModeTextView.setText(MusicPlayMode.valueOfArrayPos(which).getDisplayId());
                        } catch (RemoteException e) {
							Log.e(TAG, "Failed to set mode", e);
						}
						dialog.dismiss();
					}
				})
				.show();
	}

	private void applyDetailToStatusBar(@NonNull MusicDetail detail) {
		binding.mainTitleTextView.setText(detail.getTitle());
		binding.mainArtistTextView.setText(detail.getArtist());
		binding.mainMusicDurTextView.setText(getString(R.string.min_second_time_fmt, detail.getMin(), detail.getSecond()));
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
			binding.mainLoadingMusicProgressBar.setVisibility(View.GONE);
		}

        binding.toolbar.setNavigationIcon(R.drawable.ic_menu);
		binding.toolbar.setNavigationOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					binding.getRoot().openDrawer(GravityCompat.START);
				}
			});
		
		if (!TextUtils.isEmpty(App.getSearchRoot())) {
			Intent intent = new Intent(this, MusicService.class);
			startService(intent);
			bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		}
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Intent intent = new Intent(this, MusicService.class);
		stopPositionUpdater();
		stopService(intent);
		unbindService(mConnection);
	}
}
