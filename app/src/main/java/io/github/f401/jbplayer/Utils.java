package io.github.f401.jbplayer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.io.FilenameFilter;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;

public class Utils {
	
	private static final Set<String> SUFFIX = new HashSet<>();
	static {
		SUFFIX.add(".mp3");
		SUFFIX.add(".flac");
	}
    
   public static void write(File file, byte[] data) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        ByteArrayInputStream input = new ByteArrayInputStream(data);
        FileOutputStream output = new FileOutputStream(file);
        try {
            write(input, output);
        } finally {
            closeIO(input, output);
        }
    }
	
	public static void write(InputStream input, OutputStream output) throws IOException {
        byte[] buf = new byte[1024 * 8];
        int len;
        while ((len = input.read(buf)) != -1) {
            output.write(buf, 0, len);
        }
    }

    public static String toString(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        write(input, output);
        try {
            return output.toString("UTF-8");
        } finally {
            closeIO(input, output);
        }
    }

    public static void closeIO(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            try {
                if (closeable != null) closeable.close();
            } catch (IOException ignored) {}
        }
    }
    
	public static List<File> discoveryMusic(File root) {
		ArrayList<File> res = new ArrayList<>();
		File[] f = root.listFiles();
		for (File file : f) {
			if (file.isDirectory()) {
				res.addAll(discoveryMusic(file));
				continue;
			}
			String suff = file.getName().substring(file.getName().lastIndexOf("."));
			if (SUFFIX.contains(suff)) {
				res.add(file);
			}
		}
		return res;
	}
	
	public static MusicDetail readMusicDetail(File file) throws CannotReadException, InvalidAudioFrameException, IOException, TagException, ReadOnlyFileException {
		AudioFile a = AudioFileIO.read(file);
		return new MusicDetail(
			a.getTag().getFirst(FieldKey.TITLE), 
			a.getTag().getFirst(FieldKey.ARTIST),
			a.getAudioHeader().getTrackLength() / 60,
			a.getAudioHeader().getTrackLength() % 60,
			file);
	}
	
	public static List<MusicDetail> readMusicDetail(List<File> file) throws CannotReadException, InvalidAudioFrameException, IOException, TagException, ReadOnlyFileException  {
		ArrayList<MusicDetail> res = new ArrayList<>();
		for (File f : file) { 
			res.add(readMusicDetail(f));
		}
		return res;
	}
}
