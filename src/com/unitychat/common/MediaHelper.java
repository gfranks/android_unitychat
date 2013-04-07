package com.unitychat.common;

import java.util.ArrayList;

import com.unitychat.SocksoApi;
import com.unitychat.Track;
import com.unitychat.SocksoApi.SocksoApiCompletionListener;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
	int currentFileIndex = 0, currentSeekPosition = 0, track_layout_id, track_number_id, track_name_id;
	boolean readyToPlay = false, isPaused = false, mediaHasInitialized = false, shouldRepeat = false, startRightAway = false;
	Button pause, play, next, prev, repeat, select_song, media_refresh;
	TextView now_playing;
	View superview;
	ListView songListView;
	Dialog trackDialog;
	
	public MediaHelper(View superview, int pause_id, int play_id, int next_id, 
			int prev_id, int repeat_id, int select_song_id, int now_playing_id, int track_layout_id, 
			int track_number_id, int track_name_id, int media_refresh_id) {
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
		media_refresh = (Button) superview.findViewById(media_refresh_id);
		media_refresh.setOnClickListener(this);
		now_playing = (TextView) superview.findViewById(now_playing_id);
		this.superview = superview;
		this.track_layout_id = track_layout_id;
		this.track_number_id = track_number_id;
		this.track_name_id = track_name_id;
		
		setViewsEnabled(false);
	}
	
	public void getMusic() {
		now_playing.setText("Loading tracks...");
		startRightAway = false;
		new SocksoApi(this, true);
	}
	
	public boolean isInitialized() {
		return mediaHasInitialized;
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
			startRightAway = true;
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
		
		if (v == media_refresh) {
			mediaPlayer.stop();
			getMusic();
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
		songListView = new ListView(superview.getContext());
		songListView.setAdapter(new TrackAdapter(superview.getContext(), musicFiles, 
				track_layout_id, track_number_id, track_name_id));
		songListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> av, View v, int pos, long id) {
				if (mediaPlayer.isPlaying()) {
					mediaPlayer.reset();
				}
				trackDialog.dismiss();
				currentFileIndex = pos;
				handlePlay();
			}
		});

		builder.setView(songListView);
		trackDialog = builder.create();
	
		trackDialog.show();
	}


	@Override
	public void SocksoApiSuccessful(ArrayList<Track> musicFiles,
			ArrayList<String> musicFilesTitles) {
		this.musicFiles = musicFiles;
		this.musicFilesTitles = musicFilesTitles;
		setViewsEnabled(true);
		mediaHasInitialized = true;
		readyToPlay = true;
		select_song.setEnabled(true);
		now_playing.setText("Ready to play");
		if (startRightAway) {
			handlePlay();
		}
	}

	@Override
	public void SocksoApiFailed(Exception e) {
		now_playing.setText("Error in media server connection");
	}
	
	private class TrackAdapter extends ArrayAdapter<Track> {

		ArrayList<Track> tracks;
		int track_layout_id, track_number_id, track_name_id;
		
		public TrackAdapter(Context context, ArrayList<Track> objects, int track_layout_id, int track_number_id, int track_name_id) {
			super(context, android.R.layout.simple_list_item_1, objects);
			this.tracks = objects;
			this.track_layout_id = track_layout_id;
			this.track_number_id = track_number_id;
			this.track_name_id = track_name_id;
		}
		
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater inflator = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflator.inflate(track_layout_id, null);
			}
			
			convertView.setEnabled(true);
			
			((TextView)convertView.findViewById(track_number_id)).setText(""+(position+1));
			((TextView)convertView.findViewById(track_name_id)).setText(tracks.get(position).getName());
			
			// set due to listview onItemClickListener not receiving callbacks
			convertView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mediaPlayer.isPlaying()) {
						mediaPlayer.reset();
					}
					trackDialog.dismiss();
					currentFileIndex = position;
					handlePlay();
				}
			});
			
			return convertView;
		}
		
	}
}