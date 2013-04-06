package com.drawerchat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.database.DataSetObserver;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.drawerchat.models.Friend;
import com.drawerchat.models.Friend.Status;
import com.drawerchat.widget.DrawerChatHelper;

public class MainActivity extends Activity implements OnPreparedListener,
		OnCompletionListener, OnClickListener {

	MediaPlayer mediaPlayer;
	TextView mediaPlayIndicator;
	Thread t;
	ArrayList<Track> playlists;
	ArrayList<Track> musicFiles;
	ArrayList<String> musicFilesTitles;
	ImageButton prev, playPause, next;
	boolean readyToPlay = false, isPaused = false, mediaHasInitialized = false;
	int currentSeekPosition = 0;
	int currentFileIndex = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		DrawerChatHelper helper = new DrawerChatHelper(this, R.id.drawer,
				false, 0, true, "Game");
		helper.addNewFriend(new Friend("Ty", "You suck at this!",
				Status.INACTIVE));
		helper.addNewFriend(new Friend("Garrett", "You suck at this!",
				Status.ACTIVE));
		helper.addNewFriend(new Friend("Dee", "You suck at this!",
				Status.INGAME));

		mediaPlayer = new MediaPlayer();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection Add Other Item Later
		switch (item.getItemId()) {
		case R.id.music_settings:
			setupViews();
			new GetPlayListTracksTask()
					.execute("http://192.168.1.121:4444/api/playlists/0");
			break;
		default:
			break;
		}
		return true;
	}

	public void setupViews() {
		mediaPlayIndicator = (TextView) findViewById(R.id.media_play_indicator);
		mediaPlayIndicator.setText("Preparing Playlist Selection...");
		prev = (ImageButton) findViewById(R.id.media_play_prev);
		prev.setEnabled(false);
		prev.setImageResource(R.drawable.previous_button_disable);
		prev.setOnClickListener(this);
		playPause = (ImageButton) findViewById(R.id.media_play_play);
		playPause.setOnClickListener(this);
		next = (ImageButton) findViewById(R.id.media_play_next);
		next.setEnabled(false);
		next.setImageResource(R.drawable.next_button_disable);
		next.setOnClickListener(this);
	}

	public void setupPlaylistDialog(ArrayList<String> songNames) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select Song");
		ListView songListView = new ListView(this);
		ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, songNames);
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
				String playlistUrl = SocksoApi.playlistBaseUrl + pos + 1;
				Log.v("Chosen SONG URL", playlistUrl);
				handlePlayPause();
			}
		});

		dialog.show();
	}

	private class GetPlayListTracksTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... params) {
			musicFiles = new ArrayList<Track>();
			musicFilesTitles = new ArrayList<String>();

			try {
				Log.v("IN BACKGROUND TASK", "ONLY SEE ONCE");
				HttpClient client = new DefaultHttpClient();
				HttpGet get = new HttpGet(params[0]);
				HttpResponse response = client.execute(get);
				InputStream is = response.getEntity().getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is));
				StringBuilder sb = new StringBuilder();

				String line = null;
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}

				musicFiles = Track.fromJSONArray(new JSONObject(sb.toString())
						.getJSONArray("tracks"));

				for (int i = 0; i < musicFiles.size(); i++) {
					Track track = musicFiles.get(i);
					musicFilesTitles.add(track.getName());
					Log.v("HERE ARE THE FILES", track.getFileName());
				}

			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			mediaPlayIndicator.setText("Ready To Play");
			readyToPlay = true;
			setupPlaylistDialog(musicFilesTitles);
		}
	}

	public void startMediaPlayer() {
		try {
			currentSeekPosition = 0;
			mediaPlayer.setDataSource(musicFiles.get(currentFileIndex)
					.getFileName());
			mediaPlayer.prepareAsync();
			mediaPlayer.setOnPreparedListener(this);
			mediaPlayer.setOnCompletionListener(this);
		} catch (Exception e) {
			Log.i("Exception", "Exception in streaming mediaplayer e = " + e);
			mediaPlayIndicator.setText("Error Loading Media");
		}
	}

	public void playPrevious() {
		--currentFileIndex;
		if (currentFileIndex < musicFiles.size() && currentFileIndex >= 0) {
			mediaPlayer.reset();
			startMediaPlayer();
			if (currentFileIndex > 0) {
				prev.setEnabled(true);
				prev.setImageResource(R.drawable.previous_button);
			} else {
				prev.setEnabled(false);
				prev.setImageResource(R.drawable.previous_button_disable);
			}
			if (currentFileIndex < musicFiles.size() - 1) {
				next.setEnabled(true);
				next.setImageResource(R.drawable.next_button);
			} else {
				next.setEnabled(false);
				next.setImageResource(R.drawable.next_button_disable);
			}
		}
	}

	public void handlePlayPause() {
		if (!mediaPlayer.isPlaying() && musicFiles.size() > 0) {
			if (isPaused) {
				handlePlay();
			} else {
				startMediaPlayer();
				isPaused = false;
				playPause.setImageResource(R.drawable.play_button);
				if (currentFileIndex < musicFiles.size()-1) {
					next.setEnabled(true);
					next.setImageResource(R.drawable.next_button);
				} else {
					next.setEnabled(false);
					next.setImageResource(R.drawable.next_button_disable);
				}
				if (currentFileIndex > 0) {
					prev.setEnabled(true);
					prev.setImageResource(R.drawable.previous_button);
				} else {
					prev.setEnabled(false);
					prev.setImageResource(R.drawable.previous_button_disable);
				}
			}
		} else if (mediaPlayer.isPlaying() && musicFiles.size() > 0) {
			if (isPaused) {
				handlePlay();
			} else {
				handlePause();
			}
		}
	}

	public void handlePlay() {
		mediaPlayer.seekTo(currentSeekPosition);
		mediaPlayer.start();
		isPaused = false;
		playPause.setImageResource(R.drawable.play_button);

		mediaPlayIndicator.setText(musicFilesTitles.get(currentFileIndex));
	}

	public void handlePause() {
		mediaPlayer.pause();
		currentSeekPosition = mediaPlayer.getCurrentPosition();
		isPaused = true;
		playPause.setImageResource(R.drawable.pause_button);

		mediaPlayIndicator.setText("PAUSED");
	}

	public void playNext() {
		++currentFileIndex;
		if (currentFileIndex < musicFiles.size()) {
			mediaPlayer.reset();
			startMediaPlayer();
			if (currentFileIndex > 0) {
				prev.setEnabled(true);
				prev.setImageResource(R.drawable.previous_button);
			} else {
				prev.setEnabled(false);
				prev.setImageResource(R.drawable.previous_button_disable);
			}
			if (currentFileIndex < musicFiles.size() - 1) {
				next.setEnabled(true);
				next.setImageResource(R.drawable.next_button);
			} else {
				next.setEnabled(false);
				next.setImageResource(R.drawable.next_button_disable);
			}
		} else {
			next.setEnabled(false);
			next.setImageResource(R.drawable.next_button_disable);
		}
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		playNext();
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		mp.start();
		mediaPlayIndicator.setText(musicFilesTitles.get(currentFileIndex));
	}

	@Override
	public void onClick(View v) {
		if (v == next) {
			playNext();
		}

		if (v == prev) {
			playPrevious();
		}

		if (v == playPause && readyToPlay) {
			handlePlayPause();
		}
	}
}