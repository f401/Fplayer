// IOnMusicChangeListener.aidl
package io.github.f401.jbplayer;

import io.github.f401.jbplayer.MusicDetail;

interface IOnMusicChangeListener {
    void onChange(in MusicDetail detail);
}