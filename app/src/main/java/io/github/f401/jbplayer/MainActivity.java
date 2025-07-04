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
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.f401.jbplayer.adapters.MusicListAdapter;
import io.github.f401.jbplayer.databinding.MainBinding;

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
				curr = mService.getCurrentMusicPosition() / 1000;
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

	private void changeIconToPlay() {
		if (mCurrentMusicState == MusicState.PAUSE) {
			mCurrentMusicState = MusicState.PLAYING;
			binding.mainControllerBtnImg.setImageResource(android.R.drawable.ic_media_pause);
		}
	}

	private void changeIconToPause() {
		if (mCurrentMusicState == MusicState.PLAYING) {
			mCurrentMusicState = MusicState.PAUSE;
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

	private void fetchMusicList(final int mode) {
		try {
			mService.fetchMusicList(mode, App.getSearchRoot(), new IMusicServiceInitFinishCallback.Stub() {

					@Override
					public void loadFinished(final MusicList src) throws RemoteException {
						runOnUiThread(new Runnable() {

								@Override
								public void run() {
									Log.i(TAG, "Got music from service");
									mMusicList = src.mList;
									showMusicList();
								}
							});
					}
				});
		} catch (RemoteException e) {
			Log.e(TAG, "Failed to fetch music list", e);
		}
	}
	
	private void onServiceStarted() {
		fetchMusicList(MusicService.MODE_CACHED_THEN_NOTIFY);
		try {
			mService.registerOnMusicChangeListener(new IOnMusicChangeListener.Stub() {
				@Override
				public void onChange(final MusicDetail detail) throws RemoteException {
					binding.mainMusicSeekBar.setMax((int) mService.getCurrentMusicDurtion() / 1000);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							applyDetailToStatusBar(detail);
							startPositionUpdater();
							changeIconToPlay();
						}
					});
				}
			});

			mService.setMusicClient(new IMusicClient.Stub() {
				@Override
				public void onChangeStateToPlay() throws RemoteException {
					changeIconToPlay();
				}

				@Override
				public void onChangeStateToPause() throws RemoteException {
					changeIconToPause();
				}
			});
		} catch (RemoteException e) {
			Log.e(TAG, "Failed to init because ", e);
		}

		binding.mainControllerLeftImgBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
                try {
                    mService.playPreviousSong();
                } catch (RemoteException e) {
					Log.e(TAG, "", e);
                }
            }
		});

		binding.mainControllerRightImgBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
                try {
                    mService.playNextSong();
                } catch (RemoteException e) {
                    Log.e(TAG, "", e);
                }
            }
		});

		binding.mainControllerBtnImg.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
                try {
                    if (mCurrentMusicState == MusicState.PAUSE) {
						changeIconToPlay();
						mService.doContinue();
					} else {
						changeIconToPause();
						mService.doPause();
					}
                } catch (RemoteException e) {
					Log.e(TAG, "", e);
                }
            }
		});
		
		binding.mainMusicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if (fromUser) {
                        try {
                            mService.seekCurrentMusicTo(progress * 1000);
                        } catch (RemoteException e) {
                            Log.e(TAG, "Failed to seek progress to " + progress, e);
                        }
                    }
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}
			});
	}

	private void showMusicList() {
		binding.mainLoadingMusicProgressBar.setVisibility(View.INVISIBLE);
		binding.mainMusicList.setVisibility(View.VISIBLE);
		
		MusicListAdapter adapter = new MusicListAdapter(this, mMusicList);
		adapter.setOnViewClickListener(new MusicListAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(int position) {
                try {
                    mService.replaceCurrentMusic(mMusicList.get(position));
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to replace current music ", e);
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
				.create().show();
	}
	
	private void showMusicPathEditDialog() {
		final EditText text = new EditText(this);
		text.setText(App.getSearchRoot());
		text.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		
		new AlertDialog.Builder(this)
			.setView(text)
			.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					App.setSearchRoot(text.getText().toString());
					fetchMusicList(MusicService.MODE_FORCE);
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.create().show();
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
		setSupportActionBar(binding.toolbar);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11及以上
			if (!Environment.isExternalStorageManager()) {
				Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
				intent.setData(Uri.parse("package:" + getPackageName()));
				startActivityForResult(intent, 1024);
			}
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
			intent.putExtra(MusicService.EXTRA_START_FIRST, true);
			startService(intent);
			bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		}
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
//		Intent intent = new Intent(this, MusicService.class);
		stopPositionUpdater();
//		stopService(intent);
		unbindService(mConnection);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.main_menu_edit_path) {
			showMusicPathEditDialog();
		} else if (item.getItemId() == R.id.main_menu_refresh) {
			fetchMusicList(MusicService.MODE_FORCE);
		}
		return super.onOptionsItemSelected(item);
	}
	
	
}
