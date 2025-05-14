package io.github.f401.jbplayer;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

enum MusicPlayMode implements Parcelable {
    SEQUENCE(R.string.sequence_play, 0),
    RANDOM(R.string.random_play, 1),
    LOOP(R.string.single_loop_play, 2);

    @NonNull
    public static MusicPlayMode valueOfArrayPos(int pos) {
        switch (pos) {
            case 0: return SEQUENCE;
            case 1: return RANDOM;
            case 2: return LOOP;
        }
        Log.e("MusicPlayMode", "Unknown pos " + pos);
        throw new RuntimeException("Unexpected pos " + pos);
    }

    @StringRes
    private final int display;
    private final int pos;

    MusicPlayMode(@StringRes int display, int pos) {
        this.display = display;
        this.pos = pos;
    }

    public static final Creator<MusicPlayMode> CREATOR = new Creator<MusicPlayMode>() {
        @Override
        public MusicPlayMode createFromParcel(Parcel in) {
            return MusicPlayMode.valueOf(in.readString());
        }

        @Override
        public MusicPlayMode[] newArray(int size) {
            return new MusicPlayMode[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(name());
    }

    @StringRes
    public int getDisplayId() {
        return this.display;
    }

    public int getPos() {
        return pos;
    }
}
