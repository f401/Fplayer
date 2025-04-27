package io.github.f401.jbplayer;

import io.github.f401.jbplayer.IMusicServiceInitFinishCallback;

interface IMusicService {
	void fetchMusicList(String path, in IMusicServiceInitFinishCallback callback);
}
