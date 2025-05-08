package io.github.f401.jbplayer;

import java.util.List;

public class MusicQueue {
    private final List<MusicDetail> queue;
    private int pos;

    public MusicQueue(List<MusicDetail> queue) {
        this.queue = queue;
        this.pos = 0;
    }

    public MusicDetail nextSong() {
        pos = (pos + 1) % queue.size();
        return queue.get(pos);
    }

    public MusicDetail currentSong() {
        return queue.get(pos);
    }

    public MusicDetail playPreviousMusic() {
        pos = pos == 0 ? queue.size() - 1 : (pos - 1) % queue.size();
        return queue.get(pos);
    }

    public void replaceCurrent(MusicDetail detail) {
        int idx = queue.indexOf(detail);
        if (idx != -1) {
            queue.remove(idx);
        }
        queue.add(pos, detail);
    }

    public void insertNext(MusicDetail detail) {
        int idx = queue.indexOf(detail);
        if (idx != -1) {
            queue.remove(idx);
        }
        queue.add((pos + 1) % queue.size(), detail);
    }
}
