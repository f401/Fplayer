package io.github.f401.jbplayer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
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
import android.util.Log;
import android.text.TextUtils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public class Utils {
	
	private static final Set<String> SUFFIX = new HashSet<>();
	static {
		SUFFIX.add(".mp3");
		SUFFIX.add(".flac");
	}

	// 将中文转为拼音，英文直接返回
	public static String getPinyin(String input) {
		StringBuilder pinyin = new StringBuilder();
		final HanyuPinyinOutputFormat fmt = new HanyuPinyinOutputFormat();
		fmt.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		fmt.setVCharType(HanyuPinyinVCharType.WITH_V);
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			// 检查是否为中文字符
			if (Character.toString(c).matches("[\\u4e00-\\u9fa5]")) {
				// 获取字符的拼音
                String[] pinyinArray = null;
                try {
                    pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, fmt);
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    throw new RuntimeException(e);
                }
                if (pinyinArray != null) {
					pinyin.append(pinyinArray[0]);
				}
			} else {
				// 非中文字符直接追加
				pinyin.append(c);
			}
		}
		return pinyin.toString();
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
				Log.i("Utils", "Got file " + file);
				res.add(file);
			}
		}
		return res;
	}
	
	public static MusicDetail readMusicDetail(File file) throws CannotReadException, InvalidAudioFrameException, IOException, TagException, ReadOnlyFileException {
		AudioFile a = AudioFileIO.read(file);
		String title = a.getTag().getFirst(FieldKey.TITLE);
		String artist = a.getTag().getFirst(FieldKey.ARTIST);
		if (TextUtils.isEmpty(title)) {
			String name = file.getName();
			name = name.substring(0, name.lastIndexOf("."));
			artist = name.substring(0, name.indexOf("-")).trim();
			title = name.substring(name.indexOf("-") + 1).trim();
		}
		return new MusicDetail(
			title, artist,
			a.getAudioHeader().getTrackLength() / 60,
			a.getAudioHeader().getTrackLength() % 60,
			file);
	}
	
	public static List<MusicDetail> readMusicDetail(List<File> file) throws CannotReadException, InvalidAudioFrameException, IOException, TagException, ReadOnlyFileException  {
		ArrayList<MusicDetail> res = new ArrayList<>();
		Log.i("MusicService", "Trying to process " + file.size());
		for (File f : file) { 
			res.add(readMusicDetail(f));
		}
		Log.i("MusicService", "Finished process ");
		return res;
	}
}
