package io.github.f401.jbplayer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.github.f401.jbplayer.MusicDetail;
import io.github.f401.jbplayer.databinding.MusicListItemBinding;
import java.util.ArrayList;
import java.util.List;

public class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.MusicItemHolder> {
	
	private Context context;
	private List<MusicDetail> mData;

	public MusicListAdapter(Context context, List<MusicDetail> mData) {
		this.context = context;
		this.mData = mData;
	}
	
	@Override
	public int getItemCount() {
		return mData.size();
	}

	@Override
	public void onBindViewHolder(MusicListAdapter.MusicItemHolder vH, int p) {
		MusicDetail detail = mData.get(p);
		vH.author.setText(detail.getArtist());
		vH.title.setText(detail.getTitle());
		vH.time.setText(detail.getMin() + ":" + detail.getSecond());
	}

	@Override
	public MusicListAdapter.MusicItemHolder onCreateViewHolder(ViewGroup viewGroup, int p) {
		MusicListItemBinding binding = MusicListItemBinding.inflate(LayoutInflater.from(context), viewGroup, false);
		return new MusicItemHolder(binding.getRoot());
	}

    
    public static class MusicItemHolder extends RecyclerView.ViewHolder {
		
		@NonNull
		public final TextView author;

		@NonNull
		public final TextView time;

		@NonNull
		public final TextView title;
		
		public MusicItemHolder(View view) {
			super(view);
			MusicListItemBinding binding = MusicListItemBinding.bind(view);
			title = binding.musiclistitemTitle;
			time = binding.musiclistitemTime;
			author = binding.musiclistitemAuthor;
		}
	}
    
}
