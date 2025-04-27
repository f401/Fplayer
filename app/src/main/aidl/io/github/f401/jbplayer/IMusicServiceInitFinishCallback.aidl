package io.github.f401.jbplayer;

import io.github.f401.jbplayer.MusicDetail;

interface IMusicServiceInitFinishCallback {
	void loadFinished(in List<MusicDetail> src);
}
