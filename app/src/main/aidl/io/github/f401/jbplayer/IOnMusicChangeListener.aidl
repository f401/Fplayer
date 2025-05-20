// IOnMusicChangeListener.aidl
package io.github.f401.jbplayer;

import io.github.f401.jbplayer.MusicDetail;

oneway interface IOnMusicChangeListener {
    void onChange(in MusicDetail detail);
}