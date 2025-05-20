package io.github.f401.jbplayer;

import io.github.f401.jbplayer.MusicList;

interface IMusicServiceInitFinishCallback {
	void loadFinished(in MusicList src);
}
