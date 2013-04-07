package com.unitychat.common;

import java.util.ArrayList;

import com.unitychat.SocksoApi;
import com.unitychat.Track;
import com.unitychat.SocksoApi.SocksoApiCompletionListener;

import android.app.AlertDialog;
import android.app.Dialog;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MediaHelper implements OnPreparedListener, OnCompletionListener, OnClickListener, SocksoApiCompletionListener {
	
	MediaPlayer mediaPlayer;
	ArrayList<Track> musicFiles;
	ArrayList<String> musicFilesTitles;
	int currentFileIndex = 0, currentSeekPosition = 0;
	boolean readyToPlay = false, isPaused = false, mediaHasInitialized = false, shouldRepeat = false;
	Button pause, play, next, prev, repeat, select_song;
	TextView now_playing;
	View superview;
	
	public MediaHelper(View superview, int pause_id, int play_id, int next_id, 
			int prev_id, int repeat_id, int select_song_id, int now_playing_id) {
		mediaPlayer = new MediaPlayer();
		pause = (Button) superview.findViewById(pause_id);
		pause.setOnClickListener(this);
		play = (Button) superview.findViewById(play_id);
		play.setOnClickListener(this);
		next = (Button) superview.findViewById(next_id);
		next.setOnClickListener(this);
		prev = (Button) superview.findViewById(prev_id);
		prev.setOnClickListener(this);
		repeat = (Button) superview.findViewById(repeat_id);
		repeat.setOnClickListener(this);
		select_song = (Button) superview.findViewById(select_song_id);
		select_song.setOnClickListener(this);
		now_playing = (TextView) superview.findViewById(now_playing_id);
		this.superview = superview;
		
		setViewsEnabled(false);
	}
	
	public void getMusic() {
		handlePlay();
	}

	private void startMediaPlayer() {
		try {
			currentSeekPosition = 0;
			mediaPlayer.setDataSource(musicFiles.get(currentFileIndex)
					.getFileName());
			mediaPlayer.prepareAsync();
			mediaPlayer.setOnPreparedListener(this);
			mediaPlayer.setOnCompletionListener(this);

			now_playing.setText(musicFilesTitles.get(currentFileIndex));
		} catch (Exception e) {
			Log.i("Exception", "Exception in streaming mediaplayer e = " + e);
		}
	}

	private void playPrevious() {
		if (!shouldRepeat && mediaPlayer.getCurrentPosition() == 15) {
			--currentFileIndex;
		}
		if (currentFileIndex < musicFiles.size() && currentFileIndex >= 0) {
			mediaPlayer.reset();
			startMediaPlayer();
			if (currentFileIndex > 0) {
				prev.setEnabled(true);
			} else {
				prev.setEnabled(false);
			}
			if (currentFileIndex < musicFiles.size() - 1) {
				next.setEnabled(true);
			} else {
				next.setEnabled(false);
			}
		}
	}

	private void handlePlay() {
		if (!mediaHasInitialized) {
			new SocksoApi(this, true);
			return;
		}
		if (isPaused) {
			mediaPlayer.seekTo(currentSeekPosition);
			mediaPlayer.start();
			isPaused = false;
		} else {
			startMediaPlayer();
			isPaused = false;
			if (currentFileIndex < musicFiles.size()-1) {
				next.setEnabled(true);
			} else {
				next.setEnabled(false);
			}
			if (currentFileIndex > 0) {
				prev.setEnabled(true);
			} else {
				prev.setEnabled(false);
			}
		}
	}

	private void handlePause() {
		mediaPlayer.pause();
		currentSeekPosition = mediaPlayer.getCurrentPosition();
		isPaused = true;
	}

	private void playNext() {
		if (!shouldRepeat) {
			++currentFileIndex;
		}
		if (currentFileIndex < musicFiles.size()) {
			mediaPlayer.reset();
			startMediaPlayer();
			if (currentFileIndex > 0) {
				prev.setEnabled(true);
			} else {
				prev.setEnabled(false);
			}
			if (currentFileIndex < musicFiles.size() - 1) {
				next.setEnabled(true);
			} else {
				next.setEnabled(false);
			}
		} else {
			next.setEnabled(false);
		}
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		playNext();
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		mp.start();
	}
	
	@Override
	public void onClick(View v) {
		if (v == next) {
			playNext();
		}

		if (v == prev) {
			playPrevious();
		}

		if (v == play && (!mediaHasInitialized || readyToPlay)) {
			handlePlay();
		}
		
		if (v == pause) {
			handlePause();
		}
		
		if (v == repeat) {
			shouldRepeat = !shouldRepeat;
			displayRepeatToast();
		}
		
		if (v == select_song) {
			setupTrackSelectionDialog();
		}
	}
	
	public void setViewsEnabled(boolean enabled) {
		pause.setEnabled(enabled);
		prev.setEnabled(enabled);
		next.setEnabled(enabled);
		repeat.setEnabled(enabled);
		select_song.setEnabled(enabled);
	}
	
	private void displayRepeatToast() {
		String text = "Media set to repeat";
		if (!shouldRepeat) {
			text = "Media no longer repeating";
		}
		Toast.makeText(superview.getContext(), text, Toast.LENGTH_SHORT).show();
	}
	
	public void setupTrackSelectionDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(superview.getContext());
		builder.setTitle("Select Song");
		ListView songListView = new ListView(superview.getContext());
		ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(superview.getContext(),
				android.R.layout.simple_list_item_1, musicFilesTitles);
		songListView.setAdapter(modeAdapter);
		builder.setView(songListView);
		final Dialog dialog = builder.create();
	
		songListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> av, View v, int pos, long id) {
				if (mediaPlayer.isPlaying()) {
					mediaPlayer.reset();
				}
				dialog.dismiss();
				currentFileIndex = pos;
				handlePlay();
			}
		});
	
		dialog.show();
	}


	@Override
	public void SocksoApiSuccessful(ArrayList<Track> musicFiles,
			ArrayList<String> musicFilesTitles) {
		this.musicFiles = musicFiles;
		this.musicFilesTitles = musicFilesTitles;
		setViewsEnabled(true);
		mediaHasInitialized = true;
		readyToPlay = true;
		handlePlay();
	}

	@Override
	public void SocksoApiFailed(Exception e) {
		now_playing.setText("Error in media server connection");
	}
}