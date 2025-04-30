package io.github.f401.jbplayer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import io.github.f401.jbplayer.MusicDetail;
import io.github.f401.jbplayer.R;
import io.github.f401.jbplayer.databinding.MusicListItemBinding;
import java.util.ArrayList;
import java.util.List;

public class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.MusicItemHolder> {
	
	private final Context context;
	private final List<MusicDetail> mData;

	@Nullable
	private OnItemClickListener onViewClickListener;

	public interface OnItemClickListener {
		void onItemClick(int position);
	}

	public MusicListAdapter(Context context, List<MusicDetail> mData) {
		this(context, mData, null);
	}

	public MusicListAdapter(Context context, List<MusicDetail> mData, @Nullable OnItemClickListener onViewClickListener) {
		this.context = context;
		this.mData = mData;
        this.onViewClickListener = onViewClickListener;
    }

	@Nullable
	public OnItemClickListener getOnViewClickListener() {
		return onViewClickListener;
	}

	public void setOnViewClickListener(@Nullable OnItemClickListener onViewClickListener) {
		this.onViewClickListener = onViewClickListener;
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
		vH.time.setText(context.getString(R.string.min_second_time_fmt, detail.getMin(), detail.getSecond()));
		if (onViewClickListener != null) {
			vH.root.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (getOnViewClickListener() != null) getOnViewClickListener().onItemClick(vH.getAdapterPosition());
					}
				});
		}
	}

	@NonNull
	@Override
	public MusicListAdapter.MusicItemHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, final int p) {
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
		@NonNull
		public final RelativeLayout root;

		public MusicItemHolder(View view) {
			super(view);
			MusicListItemBinding binding = MusicListItemBinding.bind(view);
			title = binding.musiclistitemTitle;
			time = binding.musiclistitemTime;
			author = binding.musiclistitemAuthor;
			root = binding.getRoot();

		}
	}
    
}
