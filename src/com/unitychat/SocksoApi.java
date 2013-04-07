package com.unitychat;

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

import android.os.AsyncTask;
import android.util.Log;

public class SocksoApi {

	ArrayList<Track> musicFiles;
	ArrayList<String> musicFilesTitles;
	SocksoApiCompletionListener listener;
	GetPlayListTracksTask getPlayListTracksTask;
	
//	static String playlistBaseUrl = "http://99.70.171.164:4444/api/playlists/";
//	static String playlistBaseUrl = "http://192.168.1.121:4444/api/playlists/";
	static String trackBaseUrl = "http://192.168.1.121:4444/stream/";
	String apiTracksUrl = "http://192.168.1.121:4444/api/playlists/0";
	
	public SocksoApi(SocksoApiCompletionListener listener, boolean auto_start) {
		this.listener = listener;
		getPlayListTracksTask = new GetPlayListTracksTask();
		if (auto_start) {
			getTracks();
		}
	}
	
	public void getTracks() {
		getPlayListTracksTask.execute(apiTracksUrl);
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
				if (listener != null) {
					listener.SocksoApiFailed(e);
				}
			} catch (JSONException e) {
				e.printStackTrace();
				if (listener != null) {
					listener.SocksoApiFailed(e);
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if (listener != null) {
				listener.SocksoApiSuccessful(musicFiles, musicFilesTitles);
			}
		}
	}
	
	public interface SocksoApiCompletionListener {
		public void SocksoApiSuccessful(ArrayList<Track> musicFiles, 
				ArrayList<String> musicFilesTitles);
		public void SocksoApiFailed(Exception e);
	}
}
