package com.unitychat;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class Track {

	public static final String TRACKS = "tracks";
	private static final String ID = "id";
	private static final String NAME = "name";
	private static final String NUMBER = "number";
	private static final String ARTIST = "artist";
	private static final String ALBUM = "album";
	public static final String COVER_PREFIX = "tr";

	private long id = 0;
	private long serverId = 0;
	private String name;
	private int trackNumber;
	private int duration;
	private String image;
	private long albumId;
	private long artistId;
	private String album;
	private String artist;

	public Track() {
	}

	public Track(String name, String artist, String album) {
		this.name = name;
		this.artist = artist;
		this.setAlbum(album);
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getServerId() {
		return serverId;
	}

	public void setServerId(long serverId) {
		this.serverId = serverId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public int getTrackNumber() {
		return trackNumber;
	}

	public void setTrackNumber(int trackNumber) {
		this.trackNumber = trackNumber;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public long getAlbumId() {
		return albumId;
	}

	public void setAlbumId(long albumId) {
		this.albumId = albumId;
	}

	public long getArtistId() {
		return artistId;
	}

	public void setArtistId(long artistId) {
		this.artistId = artistId;
	}
	
	public String getFileName(){
		String temp = getName().replaceAll("[!@$%^&*()_+-=<>?:\"\';`~\\s]","");
		return SocksoApi.trackBaseUrl+this.getServerId()+"/"+this.getName()+"-"+temp+"3";
	}
	@Override
	public String toString() {
		return this.name;
	}

	public static Track fromJSON(JSONObject jsonObj) {
		Track track = new Track();
		try {
			track.setServerId(jsonObj.getInt(ID));
			track.setName(jsonObj.getString(NAME));
			track.setTrackNumber(jsonObj.getInt(NUMBER));

			JSONObject artistJSON = jsonObj.getJSONObject(ARTIST);
			track.setArtist(artistJSON.getString(NAME));
			track.setArtistId(artistJSON.getInt(ID));

			JSONObject albumJSON = jsonObj.getJSONObject(ALBUM);
			track.setAlbum(albumJSON.getString(NAME));
			track.setAlbumId(albumJSON.getInt(ID));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return track;
	}

	public static ArrayList<Track> fromJSONArray(JSONArray jsonArray){
		ArrayList<Track> tracks = new ArrayList<Track>();	
		try {
			if (jsonArray != null) {
				for (int i = 0; i < jsonArray.length(); i++) {
					tracks.add(fromJSON(jsonArray.getJSONObject(i)));
				}
			}
		}
		catch(JSONException e){
			e.printStackTrace();
		}
		return tracks;
	}
}
