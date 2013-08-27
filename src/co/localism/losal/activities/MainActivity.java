package co.localism.losal.activities;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import co.localism.losal.FetchFeed;
import co.localism.losal.FetchNotices;
import co.localism.losal.R;
import co.localism.losal.SVGHandler;
import co.localism.losal.SetUpSlidingMenu;
import co.localism.losal.R.layout;
import co.localism.losal.R.menu;
import co.localism.losal.adapters.NoticeAdapter;
import co.localism.losal.adapters.PostAdapter;
import co.localism.losal.async.InstagramRequests;

import co.localism.losal.listens.PersonalOptionsOnClickListeners;
import co.localism.losal.objects.Notice;
import co.localism.losal.objects.Post;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.parse.FindCallback;
import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseAnalytics;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseTwitterUtils;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import android.os.Bundle;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends ListActivity{// implements Observer {// ,
																	// OnItemClickListener
																	// {

	public Context ctx = this;
	public static PostAdapter listadapter;
	public static ArrayList<Post> posts = new ArrayList<Post>();
	public static ArrayList<Notice> notices = new ArrayList<Notice>();

	private static final String tag = "MainActivity";
	public static final String KEY_UPDATE = "co.localism.losal.MainActivity.updateView";
	public BroadcastReceiver receiver = null;
	public static int POST_DAYS = 7;
	public static Date LAST_POST_DATE = null;
	private boolean loadingMore = false;
	private static FetchFeed ff;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		SlidingMenu sm = new SetUpSlidingMenu(this, SlidingMenu.SLIDING_CONTENT);
		new PersonalOptionsOnClickListeners(
				(LinearLayout) findViewById(R.id.po), this);
		
		ActionBar a = getActionBar();
		// a.setIcon(new SVGHandler().svg_to_drawable(ctx, R.raw.left_chevron));
		a.setDisplayHomeAsUpEnabled(true);
		ff = new FetchFeed();
//		ff.addObserver(this);

		Parse.initialize(this, getResources().getString(R.string.parse_app_id),
				getResources().getString(R.string.parse_client_key));
		ParseTwitterUtils.initialize(
				getResources().getString(R.string.tw_consumer_key),
				getResources().getString(R.string.tw_consumer_secret));
		ParseAnalytics.trackAppOpened(getIntent());
		loginParseUser();
	
		// ParseFacebookUtils.initialize(getResources().getString(R.string.fb_app_id));

		// createTestparseUser();
		SharedPreferences user_info = getSharedPreferences("UserInfo",
				MODE_PRIVATE);
		// SharedPreferences.Editor prefEditor = user_info.edit();
		// prefEditor.putBoolean("registered", true);
		// prefEditor.putString("user_type", "student");
		// prefEditor.commit();
		ListView lv = getListView();
		boolean pauseOnScroll = true; // or true
		boolean pauseOnFling = true; // or false
		PauseOnScrollListener listener = new PauseOnScrollListener(
				PostAdapter.mImageLoader, pauseOnScroll, pauseOnFling);
//		lv.setOnScrollListener(listener);
//		posts.add(new Post());
//		posts.add(new Post());
		listadapter = new PostAdapter(ctx, R.layout.post, posts, 1);
		setListAdapter(listadapter);

		getPosts();
		getNotices();
		//Here is where the magic happens
		/*lv.setOnScrollListener(new OnScrollListener(){
			//useless here, skip!
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {}

			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
				//what is the bottom iten that is visible
				int lastInScreen = firstVisibleItem + visibleItemCount;
				Log.d(tag, "last in screen: "+lastInScreen );
				Log.d(tag, "total item count: "+totalItemCount );
				Log.d(tag, "loading more: "+loadingMore );

				//is the bottom item visible & not loading more already ? Load more !
				if(totalItemCount > 0 && (lastInScreen == totalItemCount) && !(loadingMore)){
					Log.i(tag, "onscroll: calling new thread");
//					getPosts();
//					Thread thread =  new Thread(null, loadMoreListItems);
//					thread.start();
				}
			}
		});*/
	}

	private void getNotices() {
		FetchNotices fn = new FetchNotices();
//		fn.addObserver(this);
		notices = fn.fetch();
		ListView myList = (ListView) findViewById(R.id.notices_list);
		ListAdapter adapter = new NoticeAdapter(this,
				R.layout.notice_list_item, notices, 1);
		myList.setAdapter(adapter);
	}

	
	private ArrayList<Post> newposts = new ArrayList<Post>();
	/**
	 * This checks whether we need to pull data from parse or if we can just
	 * create our posts object from our serialized file.
	 */
	public void getPosts() {

		// check current time against the time that we serialized last.
//		SharedPreferences dm = getSharedPreferences("DataManager", MODE_PRIVATE);
		// TODO: Time isn't the best option for this. While the app is running
		// we should check parse every 5 min
		// The school can manually push new data whenever they want.
		// Also on start up we should check parse but maybe load serialized data
		// first and then replace that or
		// add onto it when a new payload is ready
//		final Calendar c = Calendar.getInstance();
//		if (c.get(Calendar.MONTH) == dm.getInt("LastUpdateMonth", -1)) {
//			if (c.get(Calendar.DAY_OF_MONTH) == dm.getInt("LastUpdateDay", -1))
//				if ((c.get(Calendar.HOUR_OF_DAY) < 7 && dm.getInt(
//						"LastUpdateDay", -1) < 7)
//						|| (c.get(Calendar.HOUR_OF_DAY) < 19 && dm.getInt(
//								"LastUpdateDay", -1) < 19)) {
//					Log.d(tag, "chose to deserialize the data");
//					posts = (ArrayList<Post>) fromFile("posts.ser");
//				}
//		} else {
			Log.d(tag, "chose to fetch new data");

			try {
				ff.fetch(listadapter);
				// posts = ff.fetch();
//				posts = 
//					return	ff.fetch();
//				posts.addAll(ff.fetch());
				
//				newposts = ff.fetch();
//				posts.addAll(newposts);
			} catch (Exception e) {
				Log.d(tag, e.toString());
			}
//		}
		if(listadapter == null){
//			posts = ff.fetch();
//			listadapter = new PostAdapter(ctx, R.layout.post, posts, 1);
//			listadapter.add(new Post());
//			Post aa = (Post) listadapter.getItem(2);
			updateView();
		}else{
			updateView();

//			newposts = ff.fetch();
		}
//		return posts;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		menu.findItem(R.id.notices).setIcon(
				new SVGHandler().svg_to_drawable(ctx, R.raw.lightning));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.notices) {

			return true;
		}
		return false;
	}

	public void updateView() {
		Log.d(tag, "updateView called");
//		posts.addAll(newposts);
//		setListAdapter(listadapter);
		loadingMore = false;
		// getListView().setOnItemClickListener(this);
		// findViewById(R.id.iv_post_image).setClickable(false);

	}

	//@Override
	public void update(Observable arg0, Object arg1) {
		Log.d(tag, "update called");
		// toFile("posts.ser", posts);
		updateView();
	}

	@Override
	public void onResume() {
		super.onResume();
		updateView();
	}

	private void loginParseUser() {
		// Log.i(tag, ParseUser.getCurrentUser().toString());

		ParseUser.logInInBackground("joe", "1234", new LogInCallback() {
			public void done(ParseUser user, ParseException e) {
				if (user != null) {
					Log.i(tag, "Hooray! The user is logged in.");
					// Hooray! The user is logged in.
				} else {
					Log.i(tag, "login failed. e: " + e.toString());

					// Signup failed. Look at the ParseException to see what
					// happened.
				}
			}
		});
	}

	private void createTestparseUser() {
		ParseUser user = new ParseUser();
		user.setUsername("joe");
		user.setPassword("1234");
		user.setEmail("joeczubiak@gmail.com");

		// other fields can be set just like with ParseObject
		// user.put("phone", "650-253-0000");

		user.signUpInBackground(new SignUpCallback() {
			public void done(ParseException e) {
				if (e == null) {
					// Hooray! Let them use the app now.
				} else {
					// Sign up didn't succeed. Look at the ParseException
					// to figure out what went wrong
				}
			}
		});
	}

	public void toFile(String file_name, Object o) {
		try {
			// use buffering
			OutputStream file = openFileOutput(file_name, Context.MODE_PRIVATE);
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			try {
				output.writeObject(serializeObject(o));
			} finally {
				output.close();
				final Calendar c = Calendar.getInstance();
				int month = c.get(Calendar.MONTH);
				int day = c.get(Calendar.DAY_OF_MONTH);
				int hour = c.get(Calendar.HOUR_OF_DAY);

				SharedPreferences sp = getSharedPreferences("DataManager",
						MODE_PRIVATE);
				// int luMonth = sp.getInt("LastUpdateMonth", -1);
				// int luDay = sp.getInt("LastUpdateDay", -1);
				SharedPreferences.Editor prefEditor = sp.edit();
				prefEditor.putInt("LastUpdateMonth", month);
				prefEditor.putInt("LastUpdateDay", day);
				prefEditor.putInt("LastUpdateHour", hour);
				// prefEditor.putInt("LastUpdateMinute", minute);//do we need
				// minute?

				prefEditor.commit();
			}
		} catch (IOException ex) {
			Log.e("toFile", "Cannot perform output." + ex.toString());
		}
	}

	public Object fromFile(String file_name) {
		try {
			// use buffering
			InputStream file = openFileInput(file_name);
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream(buffer);
			try {
				// deserialize the List
				return deserializeObject((byte[]) input.readObject());
			} finally {
				input.close();
			}
		} catch (ClassNotFoundException ex) {
			Log.e("", "Cannot perform input. Class not found." + ex.toString());
		} catch (IOException ex) {
			Log.e("", "Cannot perform input" + ex.toString());
		}
		return null;
	}

	// @SuppressLint("ParserError")
	public static byte[] serializeObject(Object o) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		try {
			ObjectOutput out = new ObjectOutputStream(bos);
			out.writeObject(o);
			out.close();

			// Get the bytes of the serialized object
			byte[] buf = bos.toByteArray();

			return buf;
		} catch (IOException ioe) {
			Log.e("serializeObject", "error", ioe);

			return null;
		}
	}

	public static Object deserializeObject(byte[] b) {
		try {
			ObjectInputStream in = new ObjectInputStream(
					new ByteArrayInputStream(b));
			Object object = in.readObject();
			in.close();

			return object;
		} catch (ClassNotFoundException cnfe) {
			Log.e("deserializeObject", "class not found error", cnfe);

			return null;
		} catch (IOException ioe) {
			Log.e("deserializeObject", "io error", ioe);

			return null;
		}
	}

	// @Override
	// protected void onListItemClick(ListView l, View v, int position, long id)
	// {
	// super.onListItemClick(l, v, position, id);
	// Log.d(tag, ""+v.getId());
	// // Object p = this.getListAdapter().getItem(position);
	// // Log.d(tag, "p = "+p.toString());
	// Log.d(tag, "text = "+posts.get(position).getText());
	// switch (v.getId()) {
	// case R.id.iv_post_image:
	// Log.d(tag, "image pressed");
	// Intent intent = new Intent(this, FullScreenImageActivity.class);
	// intent.putExtra("imageURL", posts.get(position).getUrl());
	// startActivity(intent);
	// break;
	// case R.id.ll_social_like_area:
	// Log.d(tag, "like area");
	// // socialLikeClicked(posts.get(position));
	// break;
	// case R.id.iv_social_like_icon:
	// Log.d(tag, "like icon");
	// posts.get(position);
	// // socialLikeClicked(posts.get(position));
	// break;
	// case R.id.iv_social_site_icon:
	// Log.d(tag, "site icon");
	// // socialLikeClicked(posts.get(position));
	// break;
	// }
	//
	// }

	public static void favoriteTweet(String id) {
		Log.d(tag, "favoriteTwee called");

		HttpClient client = new DefaultHttpClient();
		HttpGet verifyGet = new HttpGet(
				"https://api.twitter.com/1/account/verify_credentials.json");
		https: // api.twitter.com/1.1/favorites/create.json
		ParseTwitterUtils.getTwitter().signRequest(verifyGet);
		try {
			HttpResponse response = client.execute(verifyGet);

			Log.d(tag, "tw resp: " + response.toString());

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/*
	 * @Override public void onItemClick(AdapterView<?> av, View v, int
	 * position, long id) { // TODO Auto-generated method stub Log.d(tag,
	 * "v "+v.getId()); Log.d(tag, "av "+av.getId()); Log.d(tag, "id "+id);
	 * Log.d(tag, "v tag "+v.getTag());
	 * 
	 * // Object p = this.getListAdapter().getItem(position); // Log.d(tag,
	 * "p = "+p.toString()); Log.d(tag,
	 * "text = "+posts.get(position).getText()); switch (v.getId()) { case
	 * R.id.iv_post_image: Log.d(tag, "image pressed"); Intent intent = new
	 * Intent(this, FullScreenImageActivity.class); intent.putExtra("imageURL",
	 * posts.get(position).getUrl()); startActivity(intent); break; case
	 * R.id.ll_social_like_area: Log.d(tag, "like area"); //
	 * socialLikeClicked(posts.get(position)); break; case
	 * R.id.iv_social_like_icon: Log.d(tag, "like icon"); posts.get(position);
	 * // socialLikeClicked(posts.get(position)); break; case
	 * R.id.iv_social_site_icon: Log.d(tag, "site icon"); //
	 * socialLikeClicked(posts.get(position)); break; } }
	 */

	private Runnable loadMoreListItems = new Runnable() {
		private int itemsPerPage = 15;

		@Override
		public void run() {
			// Set flag so we cant load new items 2 at the same time
			loadingMore = true;
			
			getPosts();
			// Reset the array that holds the new items
//			posts = new ArrayList<Post>();
			// Simulate a delay, delete this on a production environment!
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			// Get 15 new listitems
//			for (int i = 0; i < itemsPerPage ; i++) {
				// Fill the item with some bogus information
//				myListItems.add("Date: " + (d.get(Calendar.MONTH) + 1) + "/"
//						+ d.get(Calendar.DATE) + "/" + d.get(Calendar.YEAR));
//				 +1 day
//				d.add(Calendar.DATE, 1);
//			}
			// Done! now continue on the UI thread
			
//			runOnUiThread(returnRes);
		}
	};

	
	//Since we cant update our UI from a thread this Runnable takes care of that!
	private Runnable returnRes = new Runnable() {
		@Override
		public void run() {
			updateView();
		}
			//Loop thru the new items and add them to the adapter
//			if(posts != null && posts.size() > 0){
//	              		for(int i=0;i < posts.size();i++)
//	              			listadapter.add(posts.get(i));
//	          		 }
//			//Update the Application title
//	       		setTitle("Neverending List with " + String.valueOf(adapter.getCount()) + " items");
//			//Tell to the adapter that changes have been made, this will cause the list to refresh
//	       		listadapter.notifyDataSetChanged();
//			//Done loading more.
//	           		loadingMore = false;
//	       	}
	   };
//	   
	
}
