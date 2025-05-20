package io.github.f401.jbplayer;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class MusicList implements Parcelable {
    public List<MusicDetail> mList;

    public MusicList(List<MusicDetail> mList) {
        this.mList = mList;
    }

    protected MusicList(Parcel in) {
        mList = new ArrayList<>();
        final int sz = in.readInt();
        for (int i = 0; i < sz; ++i) {
            mList.add((MusicDetail) in.readParcelable(getClass().getClassLoader()));
        }
    }

    public static final Creator<MusicList> CREATOR = new Creator<MusicList>() {
        @Override
        public MusicList createFromParcel(Parcel in) {
            return new MusicList(in);
        }

        @Override
        public MusicList[] newArray(int size) {
            return new MusicList[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeInt(mList.size());
        for (MusicDetail detail : mList) {
            parcel.writeParcelable(detail, 0);
        }
    }
}
