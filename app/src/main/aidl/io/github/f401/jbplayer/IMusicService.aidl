package io.github.f401.jbplayer;

import io.github.f401.jbplayer.IMusicServiceInitFinishCallback;
import io.github.f401.jbplayer.IOnMusicChangeListener;

interface IMusicService {
	void fetchMusicList(String path, in IMusicServiceInitFinishCallback callback);

    void registerOnMusicChangeListener(in IOnMusicChangeListener listener);

    long getCurrentMusicProgress();

    void playPreviousSong();

    void playNextSong();

    void doPause();

    void doContinue();
}
