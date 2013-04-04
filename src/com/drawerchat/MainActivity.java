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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.drawerchat.R;
import com.drawerchat.models.Friend;
import com.drawerchat.models.Friend.Status;
import com.drawerchat.widget.DrawerChatHelper;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity implements OnPreparedListener, OnCompletionListener, OnClickListener {

	MediaPlayer mediaPlayer;
	TextView mediaPlayIndicator;
	Thread t;
	JSONArray playlists;
	ArrayList<String> musicFiles;
	ArrayList<String> musicFilesTitles;
	ImageButton prev, playPause, next;
	boolean readyToPlay = false, isPaused = false, mediaHasInitialized = false;
	int currentSeekPosition = 0;
	int currentFileIndex = 0;
	String playlistBaseUrl = "http://192.168.1.121:4444/api/playlists/";
	String trackBaseUrl = "http://192.168.1.121:4444/stream/";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        DrawerChatHelper helper = new DrawerChatHelper(this, R.id.drawer, false, 0, true, "Game");
        helper.addNewFriend(new Friend("Ty", "You suck at this!", Status.INACTIVE));
        helper.addNewFriend(new Friend("Garrett", "You suck at this!", Status.ACTIVE));
        helper.addNewFriend(new Friend("Dee", "You suck at this!", Status.INGAME));
      
		mediaPlayer = new MediaPlayer();
		setupViews();
		new GetPlayListTask().execute(playlistBaseUrl);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    public void setupViews() {
		mediaPlayIndicator = (TextView) findViewById(R.id.media_play_indicator);
		mediaPlayIndicator.setText("Preparing Playlist Selection...");
		
		prev = (ImageButton) findViewById(R.id.media_play_prev);
		prev.setEnabled(false);
		prev.setOnClickListener(this);
		playPause = (ImageButton) findViewById(R.id.media_play_play);
		playPause.setOnClickListener(this);
		next = (ImageButton) findViewById(R.id.media_play_next);
		next.setEnabled(false);
		next.setOnClickListener(this);
    }
    
    public void setupPlaylistDialog() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Select Playlist");

    	ListView modeList = new ListView(this);
    	ArrayList<String> playlistNames = new ArrayList<String>();
    	for (int i=0; i<playlists.length(); i++) {
    		try {
    			JSONObject obj = playlists.getJSONObject(i);
    			playlistNames.add(obj.getString("name"));
    		} catch (JSONException e) {
    			e.printStackTrace();
    		}
    	}
    	ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, playlistNames);
    	modeList.setAdapter(modeAdapter);

    	builder.setView(modeList);
    	final Dialog dialog = builder.create();
    	
    	modeList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> av, View v, int pos, long id) {
				dialog.dismiss();
				try {
	    			JSONObject obj = playlists.getJSONObject(pos);
	    			String playlistUrl = playlistBaseUrl + obj.getString("id");
	    			Log.v("Chosen Playlist URL", playlistUrl);
	    			new GetPlayListTracksTask().execute(playlistUrl);
	    		} catch (JSONException e) {
	    			e.printStackTrace();
	    		}
			}
    	});

    	dialog.show();
    }
    
    private class GetPlayListTask extends  AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... params) {
			try {
				HttpClient client = new DefaultHttpClient();
				HttpGet get = new HttpGet(params[0]);
				HttpResponse response = client.execute(get);
				InputStream is = response.getEntity().getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is)); 
		        StringBuilder sb = new StringBuilder(); 

		        String line = null; 
	            while ((line = reader.readLine()) != null) { 
	            	sb.append(line + "\n"); 
	            } 
	            
	            String json = sb.toString().replace(",}", "}");
	            Log.v("PLAYLISTS", json);
	            playlists = new JSONArray(json);
	            
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
    		if (playlists != null) {
    			setupPlaylistDialog();
    		}
    	}
    }
    
    private class GetPlayListTracksTask extends  AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... params) {
            musicFiles = new ArrayList<String>();
            musicFilesTitles = new ArrayList<String>();
            
			try {
				HttpClient client = new DefaultHttpClient();
				HttpGet get = new HttpGet(params[0]);
				HttpResponse response = client.execute(get);
				InputStream is = response.getEntity().getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is)); 
		        StringBuilder sb = new StringBuilder(); 

		        String line = null; 
	            while ((line = reader.readLine()) != null) { 
	            	sb.append(line + "\n"); 
	            } 
	            
	            JSONObject obj = new JSONObject(sb.toString());
	            JSONArray tracks = obj.getJSONArray("tracks");
	            
	            for (int i=0; i<tracks.length(); i++) {
	            	JSONObject track = tracks.getJSONObject(i);
	            	String trackName = track.getString("name").replaceAll("[!@$%^&*()_+-=<>?:\"\';`~\\s]","");
	            	musicFiles.add(trackBaseUrl+track.getString("id")+"/"+track.getJSONObject("artist").get("name")
	            			+"-"+trackName+"3");
	            	musicFilesTitles.add(track.getString("name"));
	            }
	            
	            Log.v("HERE ARE THE FILES", musicFiles.toString());
	            Log.v("HERE ARE THE FILE'S Titles", musicFilesTitles.toString());
	            
			} catch (IOException e) {
				
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
    	}
    }
    
    public void startMediaPlayer() {
		try {
			currentSeekPosition = 0;
			mediaPlayer.setDataSource(musicFiles.get(currentFileIndex));
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
			} else {
				prev.setEnabled(false);
			}
			if (currentFileIndex < musicFiles.size()-1) {
				next.setEnabled(true);
			} else {
				next.setEnabled(false);
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
				playPause.setImageResource(android.R.drawable.ic_media_pause);
				if (currentFileIndex < musicFiles.size()) {
					next.setEnabled(true);
				} else {
					next.setEnabled(false);
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
		playPause.setImageResource(android.R.drawable.ic_media_pause);
		
		mediaPlayIndicator.setText(musicFilesTitles.get(currentFileIndex));
    }
    
    public void handlePause() {
    	mediaPlayer.pause();
    	currentSeekPosition = mediaPlayer.getCurrentPosition();
		isPaused = true;
		playPause.setImageResource(android.R.drawable.ic_media_play);

		mediaPlayIndicator.setText("PAUSED");
    }
    
    public void playNext() {
    	++currentFileIndex;
		if (currentFileIndex < musicFiles.size()) {
			mediaPlayer.reset();
			startMediaPlayer();
			if (currentFileIndex > 0) {
				prev.setEnabled(true);
			} else {
				prev.setEnabled(false);
			}
			if (currentFileIndex < musicFiles.size()-1) {
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