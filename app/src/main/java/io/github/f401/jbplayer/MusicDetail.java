package io.github.f401.jbplayer;

import java.io.File;
import android.os.Parcelable;
import android.os.Parcel;

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
	private File path;

	public MusicDetail(String title, String artist, int min, int second, File path) {
		this.title = title;
		this.artist = artist;
		this.min = min;
		this.second = second;
		this.path = path;
	}

	public void setPath(File path) {
		this.path = path;
	}

	public File getPath() {
		return path;
	}
	
	@Override
	public int compareTo(MusicDetail o) {
		if (equals(o)) return 0;
		int cmp = title.compareToIgnoreCase(o.title);
		if (cmp != 0) return cmp;
		cmp = artist.compareToIgnoreCase(o.artist);
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
}
