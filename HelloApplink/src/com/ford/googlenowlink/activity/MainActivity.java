package com.ford.googlenowlink.activity;

import com.ford.googlenowlink.applink.AppLinkActivity;
import com.ford.googlenowlink.applink.AppLinkApplication;
import com.ford.googlenowlink.R;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppLinkActivity {

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);        		
        AppLinkApplication app = AppLinkApplication.getInstance();
		if (app != null) {
			app.startSyncProxyService();
		}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.reset:
	        	AppLinkApplication.getInstance().endSyncProxyInstance();
	        	AppLinkApplication.getInstance().startSyncProxyService();
	            return true;
	        case R.id.about:
	        	AppLinkApplication.getInstance().showAppVersion(this);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	protected void onDestroy() {
		Log.v(AppLinkApplication.TAG, "onDestroy main");		
		super.onDestroy();
	}
}
