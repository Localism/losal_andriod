package co.localism.losal.activities;

import java.io.IOException;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParseException;
import com.larvalabs.svgandroid.SVGParser;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseTwitterUtils;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Picture;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import co.localism.losal.R;
import co.localism.losal.SVGHandler;

public class ActivateSocialActivity extends Activity {

	private static final String tag = "ActivateSocialActivity";
	private OnClickListener fb_onclick, tw_onclick, insta_onclick;

	private static final int icon_width = 60;
	private static final int icon_height = 60;
	private ParseUser currentUser; 
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_activate_social);
		
		currentUser = ParseUser.getCurrentUser();
		if (currentUser != null) {
		  // do stuff with the user
		} else {
		  // show the signup or login screen
		}
		
		LinearLayout ll = (LinearLayout) findViewById(R.id.ll_social_bg);
		ll.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// onBackPressed();
			}
		});
		setup_onclick_listeners();
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				icon_width, icon_height);

		LinearLayout ll_footer = (LinearLayout) findViewById(R.id.ll_social_footer);
		ll_footer.addView(
				new SVGHandler().svg_to_imageview(this, R.raw.localism),
				ll_footer.getChildCount());

		// add twitter icon and set onclick
		LinearLayout ll_tw = (LinearLayout) findViewById(R.id.ll_activate_tw);
		ll_tw.addView(new SVGHandler().svg_to_imageview(this, R.raw.tw, 0.6f),
				0, params);
		ll_tw.setOnClickListener(tw_onclick);
		TextView tv_tw = (TextView) findViewById(R.id.tv_activate_tw_text);
		tv_tw.setAlpha(0.6f);
		// add fb icon and set onclick
		LinearLayout ll_fb = (LinearLayout) findViewById(R.id.ll_activate_fb);
		ll_fb.addView(new SVGHandler().svg_to_imageview(this, R.raw.fb2,
				R.color.android_green, R.color.white, 1f), 0, params);
		ll_fb.setOnClickListener(fb_onclick);

		// add instagram icon and set onclick
		LinearLayout ll_insta = (LinearLayout) findViewById(R.id.ll_activate_insta);
		ll_insta.addView(
				new SVGHandler().svg_to_imageview(this, R.raw.insta, 0.6f), 0,
				params);
		TextView tv_insta = (TextView) findViewById(R.id.tv_activate_insta_text);
		tv_insta.setAlpha(0.6f);
		ll_insta.setOnClickListener(insta_onclick);

	}

	private void setup_onclick_listeners() {
		fb_onclick = new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d("activate", "start login acti");
				Intent i = new Intent(getApplicationContext(),
						FbLoginActivity.class);
				startActivity(i);
			}
		};
		
		tw_onclick = new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d("activate", "start twitter login ");
//				twitterLogin();
				linkTwitterUser();
			}
		};
	}
	
	private void twitterLogin(){
		ParseTwitterUtils.initialize(getResources().getString(R.string.tw_consumer_key), getResources().getString(R.string.tw_consumer_secret));

	
		ParseTwitterUtils.logIn(this, new LogInCallback() {
			  @Override
			  public void done(ParseUser user, ParseException err) {
			    if (user == null) {
			      Log.d("MyApp", "Uh oh. The user cancelled the Twitter login.");
			    } else if (user.isNew()) {
			      Log.d("MyApp", "User signed up and logged in through Twitter!");
			    } else {
			      Log.d("MyApp", "User logged in through Twitter!");
			    }
			  }
			});
	
	}
	
	private void linkTwitterUser(){
		ParseTwitterUtils.initialize(getResources().getString(R.string.tw_consumer_key), getResources().getString(R.string.tw_consumer_secret));

		if (!ParseTwitterUtils.isLinked(currentUser)) {
			  ParseTwitterUtils.link(currentUser, this, new SaveCallback() {
			    @Override
			    public void done(ParseException ex) {
			      if (ParseTwitterUtils.isLinked(currentUser)) {
			        Log.d("MyApp", "Woohoo, user logged in with Twitter!");
//			        add this info to user_info
					SharedPreferences user_info = getSharedPreferences("UserInfo",
							MODE_PRIVATE);
					 SharedPreferences.Editor prefEditor = user_info.edit();
					 prefEditor.putBoolean("hasTwitter", true);
					 prefEditor.commit();
			      }
			    }
			  });
			}
	}
	
}
