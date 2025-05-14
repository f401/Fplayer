package io.github.f401.jbplayer;

import java.io.File;

import android.icu.text.Transliterator;
import android.os.Parcelable;
import android.os.Parcel;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import java.text.Collator;
import java.text.RuleBasedCollator;
import java.util.Collections;

public class MusicDetail implements Comparable<MusicDetail>, Parcelable {

	public static final Parcelable.Creator<MusicDetail> CREATOR = new Parcelable.Creator<MusicDetail>() {

		@Override
		public MusicDetail createFromParcel(Parcel source) {

			return new MusicDetail(
				source.readString(),
				source.readString(),
				source.readInt(), source.readInt(),
				(File) source.readSerializable()
			);
		}

		@Override
		public MusicDetail[] newArray(int size) {
			return new MusicDetail[size];
		}
	};
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(title);
		dest.writeString(artist);
		dest.writeInt(min);
		dest.writeInt(second);
		dest.writeSerializable(path);
	}

    private String title;
	private String artist;
    private int min, second;
	private String pinyinTitle, pinyinArtist;
	private File path;

	public MusicDetail(String title, String artist, int min, int second, File path) {
		this.title = title;
		this.artist = artist;
		this.min = min;
		this.second = second;
		this.path = path;
		this.pinyinTitle = Utils.getPinyin(title);
		this.pinyinArtist = Utils.getPinyin(artist);
	}

	public void setPath(File path) {
		this.path = path;
	}

	public File getPath() {
		return path;
	}
	
	@Override
	public int compareTo(MusicDetail o) {
		Collator col = Collator.getInstance();
		if (equals(o)) return 0;
		int cmp = col.compare(pinyinTitle, o.pinyinTitle);
		if (cmp != 0) return cmp;
		cmp = col.compare(pinyinArtist, o.pinyinArtist);
		return cmp;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MusicDetail) {
			MusicDetail detail = (MusicDetail) obj;
			return title.equals(detail.title) &&
				artist.equals(detail.artist) &&
				path.equals(detail.path) &&
				min == detail.min && second == detail.second;
		}
		return false;
	}
	

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getArtist() {
		return artist;
	}

	public void setMin(int min) {
		this.min = min;
	}

	public int getMin() {
		return min;
	}

	public void setSecond(int second) {
		this.second = second;
	}

	public int getSecond() {
		return second;
	}

	@NonNull
	@Override
	public String toString() {
		return "MusicDetail{" +
				"title='" + title + '\'' +
				", artist='" + artist + '\'' +
				", min=" + min +
				", second=" + second +
				", path=" + path +
				'}';
	}
}
