package io.github.f401.jbplayer;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import io.github.f401.jbplayer.databinding.MainBinding;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.core.text.TextUtilsCompat;
import android.text.TextUtils;
import android.os.IInterface;

public class MainActivity extends AppCompatActivity {
	
	
    private MainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = MainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
		
		if (TextUtils.isEmpty(App.getSearchRoot())) {
			binding.mainLoadingMusic.setVisibility(View.INVISIBLE);
		}
		
        binding.toolbar.setNavigationIcon(R.drawable.ic_menu);
		binding.toolbar.setNavigationOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					binding.getRoot().openDrawer(Gravity.START);
				}
			});
		binding.mainMusicList.setLayoutManager(new LinearLayoutManager(this));
		//new ViewModelProvider(this, new ViewModelProvider.NewInstanceFactory()).get(
    }
	
}
