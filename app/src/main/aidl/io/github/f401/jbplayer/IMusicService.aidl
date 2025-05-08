package io.github.f401.jbplayer;

import io.github.f401.jbplayer.IMusicServiceInitFinishCallback;
import io.github.f401.jbplayer.IOnMusicChangeListener;
import io.github.f401.jbplayer.MusicDetail;
import io.github.f401.jbplayer.MusicPlayMode;

interface IMusicService {
	void fetchMusicList(String path, in IMusicServiceInitFinishCallback callback);

    void registerOnMusicChangeListener(in IOnMusicChangeListener listener);

    long getCurrentMusicProgress();

    void playPreviousSong();

    void playNextSong();

    void doPause();

    void doContinue();

    void replaceCurrentMusic(in MusicDetail music);

    MusicPlayMode getCurrentMode();

    void setCurrentMode(in MusicPlayMode music);
}
