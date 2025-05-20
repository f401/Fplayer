package io.github.f401.jbplayer;

import io.github.f401.jbplayer.IMusicServiceInitFinishCallback;
import io.github.f401.jbplayer.IOnMusicChangeListener;
import io.github.f401.jbplayer.MusicDetail;
import io.github.f401.jbplayer.MusicPlayMode;
import io.github.f401.jbplayer.IMusicClient;

interface IMusicService {
    oneway void setMusicClient(in IMusicClient client);

	oneway void fetchMusicList(String path, in IMusicServiceInitFinishCallback callback);

    oneway void registerOnMusicChangeListener(in IOnMusicChangeListener listener);

    long getCurrentMusicPosition();
    
    long getCurrentMusicDurtion();

    oneway void seekCurrentMusicTo(int msec);

    oneway void playPreviousSong();

    oneway void playNextSong();

    oneway void doPause();

    oneway void doContinue();

    oneway void replaceCurrentMusic(in MusicDetail music);

    MusicPlayMode getCurrentMode();

    oneway void setCurrentMode(in MusicPlayMode music);
}
