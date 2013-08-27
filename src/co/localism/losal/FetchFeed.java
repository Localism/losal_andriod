package co.localism.losal;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Observable;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import co.localism.losal.activities.MainActivity;
import co.localism.losal.adapters.PostAdapter;
import co.localism.losal.objects.Notice;
import co.localism.losal.objects.Post;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class FetchFeed {//extends Observable {

	private static final String tag = "FetchFeed";
	private PostAdapter pa;
	
	public FetchFeed() {
		// fetch();
	}

	
	public void initialFetch(){
		getAppSettings();
	}
	
	public void getAppSettings(){
		
		ParseQuery<ParseObject> query = ParseQuery.getQuery("AppSettings");
		

		query.findInBackground(new FindCallback<ParseObject>() {
			public void done(List<ParseObject> List, ParseException e) {
				if (e == null) {
					Log.d(tag, "Retrieved " + List.size() + " notices");
					MainActivity.POST_DAYS = List.get(0).getInt("queryIntervalDays");
//					MainActivity.posts = fetch();
				} else {
					Log.d(tag, "Error: " + e.getMessage());
				}
			}
		});
		
	}
	
	public ArrayList<Post> fetch(PostAdapter pa) {
		this.pa = pa;
		final Calendar cal = Calendar.getInstance();
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Posts");
		final ArrayList<Post> posts;
		posts = new ArrayList<Post>();
		// query.whereEqualTo("socialNetworkName", "Instagram");
		query.whereEqualTo("status", "1");
		query.addDescendingOrder("postTime");
		Log.d(tag, "Last Post Date"+MainActivity.LAST_POST_DATE);
		
		if(MainActivity.LAST_POST_DATE == null){
			Log.d(tag, "initial fetch");

			getAppSettings();
//			then start with less than now
			query.whereLessThan("postTime", cal.getTime());
			cal.add(Calendar.DATE, -7);
			query.whereGreaterThan("postTime", cal.getTime());
		}else{
			Log.d(tag, " fetch MORE");

			query.whereLessThan("postTime", MainActivity.LAST_POST_DATE);
			Date d = MainActivity.LAST_POST_DATE;
			cal.setTime(d);
			cal.add(Calendar.DATE, -7);
			query.whereGreaterThan("postTime", cal.getTime());
		}
		
		

		query.include("user");
		// query.whereStartsWith("socialNetworkName", "Instagram");
		query.findInBackground(new FindCallback<ParseObject>() {
			public void done(List<ParseObject> postList, ParseException e) {
				if (e == null) {
					Log.d(tag, "Retrieved " + postList.size() + " posts");
//					MainActivity.posts = createPosts(postList);
					posts.addAll(createPosts(postList));
					triggerObservers();
				} else {
					Log.d(tag, "Error: " + e.getMessage());
				}
			}
		});
//		pa.addAll(posts);
//		pa.notifyDataSetChanged();
		return posts;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * This fetches the feed data from Parse. Sends it to {@link createPosts()}
	 * to parse the data. Then triggers observer from MainActivity to update the
	 * listadapter.
	 * 
	 * @return ArrayList<Post>
	 */
	public ArrayList<Post> fetch() {
		final Calendar cal = Calendar.getInstance();
		ParseQuery<ParseObject> query = ParseQuery.getQuery("Posts");
		final ArrayList<Post> posts;
		posts = new ArrayList<Post>();
		// query.whereEqualTo("socialNetworkName", "Instagram");
		query.whereEqualTo("status", "1");
		query.addDescendingOrder("postTime");
		Log.d(tag, "Last Post Date"+MainActivity.LAST_POST_DATE);
		
		if(MainActivity.LAST_POST_DATE == null){
			Log.d(tag, "initial fetch");

			getAppSettings();
//			then start with less than now
			query.whereLessThan("postTime", cal.getTime());
			cal.add(Calendar.DATE, -7);
			query.whereGreaterThan("postTime", cal.getTime());
		}else{
			Log.d(tag, " fetch MORE");

			query.whereLessThan("postTime", MainActivity.LAST_POST_DATE);
			Date d = MainActivity.LAST_POST_DATE;
			cal.setTime(d);
			cal.add(Calendar.DATE, -7);
			query.whereGreaterThan("postTime", cal.getTime());
		}
		
		

		query.include("user");
		// query.whereStartsWith("socialNetworkName", "Instagram");
		query.findInBackground(new FindCallback<ParseObject>() {
			public void done(List<ParseObject> postList, ParseException e) {
				if (e == null) {
					Log.d(tag, "Retrieved " + postList.size() + " posts");
//					MainActivity.posts = createPosts(postList);
//					posts.addAll(
							createPosts(postList);
							//);
					triggerObservers();
				} else {
					Log.d(tag, "Error: " + e.getMessage());
				}
			}
		});
		return posts;
	}

	/**
	 * Should only be called from within fetch(). Parses out the ParseObjects
	 * into Post objects.
	 * 
	 * @param postsList
	 * @return ArrayList<Post>
	 */
	private ArrayList<Post> createPosts(List<ParseObject> postsList) {
		ArrayList<Post> posts = new ArrayList<Post>();
		Log.d(tag, "empty: " + postsList.isEmpty());

		if (postsList != null) {

			for (int i = 0; i < postsList.size(); i++) {
				Post p = new Post();

				p.setPostTime(postsList.get(i).getDate("postTime"));
				MainActivity.LAST_POST_DATE = p.getPostTime();
				// (postsList.get(i).getString("featured"));
				p.setSocialNetworkPostId(postsList.get(i).getString(
						"socialNetworkPostID"));
				// (postsList.get(i).getString("createdAt"));
				p.setParseObjectId(postsList.get(i).getString("objectId"));

				// if(postsList.get(i).getParseObject("user") != null);
				try {
					// postsList.get(i).get("user").toString();
//					Log.d(tag, ""+ postsList.get(i).getParseObject("user").toString());
					// Log.d(tag, ""+
					// postsList.get(i).getParseObject("user").toString());

					p.setName(postsList.get(i).getParseObject("user")
							.getString("firstName"));
					p.setClassYear(postsList.get(i).getParseObject("user")
							.getString("year"));
					p.setUserIcon(postsList.get(i).getParseObject("user")
							.getString("icon"));
					p.setFaveColor(postsList.get(i).getParseObject("user")
							.getString("faveColor"));
				} catch (Exception e) {
					Log.e(tag, e.toString());
					p.setName("");// placeholder data
				}
				// Log.d(tag, ""+
				// postsList.get(i).getParseObject("user").get("firstName"));

				p.setText(postsList.get(i).getString("text"));
//				Log.d(tag, postsList.get(i).getString("text"));
				p.setSocialNetworkName(postsList.get(i).getString(
						"socialNetworkName"));
				try {
					p.setUrl(postsList.get(i).getString("url"));
				} catch (NullPointerException npe) {
					p.setUrl("");
				}
//				p.setClassYear(3);// placeholder data
//				posts.add(p);
				pa.add(p);
//				pa.notifyDataSetChanged();
			}
		}
		pa.setStatus(false);
		return posts;
	}

	
	
	
	
	/**
	 * Notifies MainActivity that the data set has changed
	 */
	private void triggerObservers() {
		Log.d(tag, "triggerObservers called");
//		setChanged();
//		notifyObservers();
	}
}
