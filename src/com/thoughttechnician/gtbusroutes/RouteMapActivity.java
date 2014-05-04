package com.thoughttechnician.gtbusroutes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import static java.util.concurrent.TimeUnit.SECONDS;

public class RouteMapActivity extends ActionBarActivity {
	private static final LatLng ATLANTA = new LatLng(33.7765,-84.4002);
	private static final String TAG = "RouteMapActivity";
	private static final String ROUTE_CONFIG_FILENAME = "route_config.xml";
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	private XMLHandler xmlHandler = null;
	private List<Route> routes = null;
	private List<Vehicle> vehicles = null;

	private Polyline[] redLines = null;
	private Polyline[] blueLines = null;
	private Polyline[] greenLines = null;
	private Polyline[] emoryLines = null;
	private Polyline[] trolleyLines = null;
	private Polyline[] ramblerLines = null;
	
	private CheckBox redCheckBox = null;
	private CheckBox blueCheckBox = null;
	private CheckBox greenCheckBox = null;
	private CheckBox emoryCheckBox = null;
	private CheckBox trolleyCheckBox = null;
	private CheckBox ramblerCheckBox = null;
	
	private List<List<LatLng>> redPaths = null;
	private List<List<LatLng>> bluePaths = null;
	private List<List<LatLng>> greenPaths = null;
	private List<List<LatLng>> emoryPaths = null;
	private List<List<LatLng>> trolleyPaths = null;
	private List<List<LatLng>> ramblerPaths = null;
	
	private List<Vehicle> redVehicles = null;
	private List<Vehicle> blueVehicles = null;
	private List<Vehicle> greenVehicles = null;
	private List<Vehicle> trolleyVehicles = null;
	private List<Vehicle> emoryVehicles = null;
	private List<Vehicle> ramblerVehicles = null;
	
	private GoogleMap map = null;
	private ScheduledFuture scheduledUpdater = null;
	private ScheduledExecutorService scheduler = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_route_map);
		
		mDrawerLayout = (DrawerLayout) findViewById(R.id.container);
		mDrawerToggle = new ActionBarDrawerToggle(
				this,
				mDrawerLayout,
				R.drawable.ic_drawer,
				R.string.drawer_open,
				R.string.drawer_close) {
			public void onDrawerClosed(View view) {
				super.onDrawerClosed(view);
				//getSupportActionBar().setTitle(title);
			}
			public void onDrawerOpened(View view) {
				super.onDrawerOpened(view);
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		
		scheduler = Executors.newScheduledThreadPool(1);
		if (connected()) {
			startUpdatingBuses();
			Log.e(TAG, "Started Updating Buses");
		}
		
		FragmentManager fm = getSupportFragmentManager();
		Fragment mapFragment = fm.findFragmentById(R.id.mapFragmentContainer);
		if (mapFragment == null) {
			GoogleMapOptions options = new GoogleMapOptions();
			options.mapType(GoogleMap.MAP_TYPE_TERRAIN);
			options.compassEnabled(true);
			mapFragment = SupportMapFragment.newInstance(options);
			fm.beginTransaction().add(R.id.mapFragmentContainer, mapFragment).commit();
		}
		map = ((SupportMapFragment)mapFragment).getMap();
		map.moveCamera(CameraUpdateFactory.newLatLngZoom(ATLANTA, 14));
		map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
		map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
			@Override
			public void onMapClick(LatLng arg0) {
				Log.d(TAG, "LatLng: " + arg0);

			}
		});
		redCheckBox = (CheckBox) findViewById(R.id.checkbox_red);
		blueCheckBox = (CheckBox) findViewById(R.id.checkbox_blue);
		greenCheckBox = (CheckBox) findViewById(R.id.checkbox_green);
		emoryCheckBox = (CheckBox) findViewById(R.id.checkbox_emory);
		trolleyCheckBox = (CheckBox) findViewById(R.id.checkbox_trolley);
		ramblerCheckBox = (CheckBox) findViewById(R.id.checkbox_rambler);
		
		try {
			if (routes == null) {
				xmlHandler = new XMLHandler();
//				routes = xmlHandler.parseRouteConfig(new BufferedInputStream(getResources().openRawResource(R.raw.my_route_config)));
//				vehicles = xmlHandler.parseVehicleLocation(new BufferedInputStream(getResources().openRawResource(R.raw.vehicle_location)));
				File downloadedFile = new File(getFilesDir() + "/" + ROUTE_CONFIG_FILENAME);
				if (downloadedFile.exists()) {
//					Log.d(TAG, "Downloaded File exists");
					routes = xmlHandler.parseRouteConfig(new BufferedInputStream(openFileInput("route_config.xml")));
				} else {
//					Log.d(TAG, "Downloaded File does not exist");
					routes = xmlHandler.parseRouteConfig(new BufferedInputStream(getResources().openRawResource(R.raw.my_route_config)));
				}
				
				vehicles = xmlHandler.parseVehicleLocation(new BufferedInputStream(getResources().openRawResource(R.raw.vehicle_location)));
			}

		} catch (Exception e) {
			Log.e(TAG, "Couldn't Do it!");
			e.printStackTrace();
		}
		//start updating route config in the background
		downloadRouteConfig();
		//should be unnecessary because it is called in route download task
		redrawPaths();
		
		redCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (redLines[0].isVisible() != isChecked) {
					for (int i = 0; i < redLines.length; i++) {
						redLines[i].setVisible(isChecked);
					}
				}
			}
		});
		
		blueCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (blueLines[0].isVisible() != isChecked) {
					for (int i = 0; i < blueLines.length; i++) {
						blueLines[i].setVisible(isChecked);
					}
				}
			}
		});
		
		greenCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				for (int i = 0; i < greenLines.length; i++) {
					greenLines[i].setVisible(isChecked);
				}
			}
		});
		
		emoryCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				for (int i = 0; i < emoryLines.length; i++) {
					emoryLines[i].setVisible(isChecked);
				}
			}
		});

		trolleyCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				for (int i = 0; i < trolleyLines.length; i++) {
					trolleyLines[i].setVisible(isChecked);
				}
			}
		});
		
		ramblerCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				for (int i = 0; i < ramblerLines.length; i++) {
					ramblerLines[i].setVisible(isChecked);
				}
			}
		});
		//is this just a revival?
		if (savedInstanceState != null) {
			
		} else {
			checkAllBoxes();
		}
		//maps tags to titles
		Map<String, String> titleMap = new Hashtable<String, String>();
		for (Route route : routes) {
			for (Stop stop : route.getStops()) {
//				Log.d(TAG, "Tag: " + stop.getTag() + " Title: " + stop.getTitle());
				String title = processTitle(stop.getTitle());
//				Log.d(TAG, "Tag: " + stop.getTag() + " Processed Title: " + title);
				if (!titleMap.values().contains(title)) {
					titleMap.put(stop.getTag(), title);
				}
				
			}
		}
		Collection<String> values = titleMap.values();
		ListView sideBar = (ListView) findViewById(R.id.left_drawer);
		String[] titleArr = values.toArray(new String[values.size()]);
		Arrays.sort(titleArr);
//		Log.d(TAG, "Array: " + titleArr);
		Adapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, titleArr);
//		Log.d(TAG, "Adapter: " + adapter);
		sideBar.setAdapter((ListAdapter)adapter);
	}
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
          return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }
	private void checkAllBoxes() {
		redCheckBox.setChecked(true);
		blueCheckBox.setChecked(true);
		greenCheckBox.setChecked(true);
		emoryCheckBox.setChecked(true);
		trolleyCheckBox.setChecked(true);
		ramblerCheckBox.setChecked(true);
	}
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		//savedInstanceState.
		super.onSaveInstanceState(savedInstanceState);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.route_map, menu);
		return true;
	}
 
	private String processTitle(String title) {
		String newTitle = new String(title);
		int ind = newTitle.indexOf(" - Arrival");
		if (ind != -1) {
			if (ind + 10 == newTitle.length()) {
				newTitle = newTitle.substring(0, ind);
			} else {
				newTitle = newTitle.substring(0, ind) + newTitle.substring(ind + 10, newTitle.length() - 1);
			}
		} else {
			ind = newTitle.indexOf(" - Hidden Departure");
			if (ind != -1) {
				if (ind + 19 == newTitle.length()) {
					newTitle = newTitle.substring(0, ind);
				} else {
					newTitle = newTitle.substring(0, ind) + newTitle.substring(ind + 19, newTitle.length() - 1);
				}
			}
		}
		ind = newTitle.indexOf("Street");
		if (ind != -1) {
//			Log.d(TAG, newTitle);
//			Log.d(TAG, "ind: " + ind);
//			Log.d(TAG, "ind + 6 : " + (ind + 6));
//			Log.d(TAG, "newTitle.length() - 1: " + (newTitle.length() - 1));
			if (ind + 6 == newTitle.length()) {
				newTitle = newTitle.substring(0, ind) + "St";
			} else {
				newTitle = newTitle.substring(0, ind) + "St" + newTitle.substring(ind + 6, newTitle.length() - 1);
			}
//			Log.d(TAG, "fixed: " + newTitle);
		}
		return newTitle;
	}
	private void downloadRouteConfig() {
		new DownloadRouteConfigTask().execute();
	}
	private void redrawPaths() {
		redPaths = routes.get(0).getPaths();
		bluePaths = routes.get(1).getPaths();
		greenPaths = routes.get(2).getPaths();
		trolleyPaths = routes.get(3).getPaths();
		emoryPaths = routes.get(4).getPaths();
		ramblerPaths = routes.get(5).getPaths();
		redLines = new Polyline[redPaths.size()];
		blueLines = new Polyline[bluePaths.size()];
		greenLines = new Polyline[greenPaths.size()];
		emoryLines = new Polyline[emoryPaths.size()];
		trolleyLines = new Polyline[trolleyPaths.size()];
		ramblerLines = new Polyline[ramblerPaths.size()];
		int redPathColor = Color.parseColor(routes.get(0).getColor());
		int bluePathColor = Color.parseColor(routes.get(1).getColor());
		int greenPathColor = Color.parseColor(routes.get(2).getColor());
		int trolleyPathColor = Color.parseColor(routes.get(3).getColor());
		int emoryPathColor = Color.parseColor(routes.get(4).getColor());
		int ramblerPathColor = Color.parseColor(routes.get(5).getColor());
		map.clear();
		Log.e(TAG, "Clearing Map and adding new Route Config");
		for (int i = 0 ; i < redLines.length; i++) {
			redLines[i] = map.addPolyline(new PolylineOptions().color(redPathColor)
			.addAll(redPaths.get(i)));
			redLines[i].setVisible(redCheckBox.isChecked());
		}
		for (int i = 0; i < blueLines.length; i++) {
			blueLines[i] = map.addPolyline(new PolylineOptions().color(bluePathColor)
			.addAll(bluePaths.get(i)));
			blueLines[i].setVisible(blueCheckBox.isChecked());
		}
		for (int i = 0; i < greenLines.length; i++) {
			greenLines[i] = map.addPolyline(new PolylineOptions().color(greenPathColor)
			.addAll(greenPaths.get(i)));
			greenLines[i].setVisible(greenCheckBox.isChecked());
		}
		for (int i = 0; i < emoryLines.length; i++) {
			emoryLines[i] = map.addPolyline(new PolylineOptions().color(emoryPathColor)
			.addAll(emoryPaths.get(i)));
			emoryLines[i].setVisible(emoryCheckBox.isChecked());
		}
		for (int i = 0; i < trolleyLines.length; i++) {
			trolleyLines[i] = map.addPolyline(new PolylineOptions().color(trolleyPathColor)
			.addAll(trolleyPaths.get(i)));
			trolleyLines[i].setVisible(trolleyCheckBox.isChecked());
		}
		for (int i = 0; i < ramblerLines.length; i++) {
			ramblerLines[i] = map.addPolyline(new PolylineOptions().color(ramblerPathColor)
			.addAll(ramblerPaths.get(i)));
			ramblerLines[i].setVisible(ramblerCheckBox.isChecked());
		}
	}
	private class DownloadRouteConfigTask extends AsyncTask<Void, Void, Void>{
		@Override
		protected Void doInBackground(Void...voids) {
			InputStream is = null;
			FileOutputStream os = null;
			try {
				URL url = new URL("http://gtwiki.info/nextbus/nextbus.php?a=georgia-tech&command=routeConfig");
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setReadTimeout(10000);
				connection.setConnectTimeout(15000);
				connection.setRequestMethod("GET");
				connection.setDoInput(true);
				
				connection.connect();
				int response = connection.getResponseCode();
				Log.d(TAG, "Connection response is: " + response);
				is = connection.getInputStream();
				File file = new File(getFilesDir(), ROUTE_CONFIG_FILENAME);
				file.createNewFile();
				os = openFileOutput(ROUTE_CONFIG_FILENAME, Context.MODE_PRIVATE);
				byte[] buffer = new byte[1024];
				int charsRead;
				while((charsRead = is.read(buffer)) != -1) {
					os.write(buffer, 0, charsRead);
				}
				os.close();
				is.close();
				
			} catch (Exception e) {
				Log.e(TAG, "Could not do something within the route config task");
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						Log.e(TAG, "Could not close route info instream");
					}
				}
				if (os != null) {
					try {
						os.close();
					} catch (IOException e) {
						Log.e(TAG, "Could not close route info outstream");
					}
				}
			}
			
			return null;
		}
		@Override
		protected void onPostExecute(Void result){
			redrawPaths();
		}
	}
//	private class UpdateBusesTask extends AsyncTask<Void, Void, Void> {
//		@Override
//		protected Void doInBackground(Void...voids) {
//			try {
//				redVehicles = acquireVehicleLocations(new URL("http://gtwiki.info/nextbus/nextbus.php?a=georgia-tech&command=vehicleLocations&r=red"));
//				blueVehicles = acquireVehicleLocations(new URL("http://gtwiki.info/nextbus/nextbus.php?a=georgia-tech&command=vehicleLocations&r=blue"));
//				greenVehicles = acquireVehicleLocations(new URL("http://gtwiki.info/nextbus/nextbus.php?a=georgia-tech&command=vehicleLocations&r=green"));
//				trolleyVehicles = acquireVehicleLocations(new URL("http://gtwiki.info/nextbus/nextbus.php?a=georgia-tech&command=vehicleLocations&r=trolley"));
//				emoryVehicles = acquireVehicleLocations(new URL("http://gtwiki.info/nextbus/nextbus.php?a=georgia-tech&command=vehicleLocations&r=emory"));
//				ramblerVehicles = acquireVehicleLocations(new URL("http://gtwiki.info/nextbus/nextbus.php?a=georgia-tech&command=vehicleLocations&r=rambler"));
//			} catch (Exception e) {
//				Toast.makeText(getApplication(), "Could not Update Routes", Toast.LENGTH_LONG).show();
//			}
//			return null;
//		}
//		
//	}
	private List<Vehicle> acquireVehicleLocations(URL url) throws Exception{
		InputStream is = null;
		List<Vehicle> vehicleResults = null;
		try {
			//this is where you were working
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setReadTimeout(10000);
			connection.setConnectTimeout(15000);
			connection.setRequestMethod("GET");
			connection.setDoInput(true);
			
			connection.connect();
			int response = connection.getResponseCode();
			if (response != 200) {
				throw new Exception("Response Code was " + response);
			}
			//Log.d(TAG, "Connection response is: " + response);
			is = new BufferedInputStream(connection.getInputStream());
			vehicleResults = xmlHandler.parseVehicleLocation(is);
			is.close();
			
		} //catch (Exception e) {
			//Log.e(TAG, "Could not acquire vehicle locations");
			//e.printStackTrace();
		//}
		finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					Log.e(TAG, "Could not close route info instream");
				}
			}
		}
		return vehicleResults;
	}
	private boolean connected() {
		ConnectivityManager manager = (ConnectivityManager)
				getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();
		return info != null && info.isConnected();
	}
	private void startUpdatingBuses() {
		final Runnable updater = new Runnable() {
			public void run() {
				try {
					redVehicles = acquireVehicleLocations(new URL("http://gtwiki.info/nextbus/nextbus.php?a=georgia-tech&command=vehicleLocations&r=red"));
					blueVehicles = acquireVehicleLocations(new URL("http://gtwiki.info/nextbus/nextbus.php?a=georgia-tech&command=vehicleLocations&r=blue"));
					greenVehicles = acquireVehicleLocations(new URL("http://gtwiki.info/nextbus/nextbus.php?a=georgia-tech&command=vehicleLocations&r=green"));
					trolleyVehicles = acquireVehicleLocations(new URL("http://gtwiki.info/nextbus/nextbus.php?a=georgia-tech&command=vehicleLocations&r=trolley"));
					emoryVehicles = acquireVehicleLocations(new URL("http://gtwiki.info/nextbus/nextbus.php?a=georgia-tech&command=vehicleLocations&r=emory"));
					ramblerVehicles = acquireVehicleLocations(new URL("http://gtwiki.info/nextbus/nextbus.php?a=georgia-tech&command=vehicleLocations&r=rambler"));
					Log.e(TAG,"Successfully updated Buses");
				} catch (Exception e) {
					Log.e(TAG, "Could not update buses");
					e.printStackTrace();
					scheduledUpdater.cancel(true);
					Log.e(TAG, "Canceled bus updater");
				}
			}
		};
		scheduledUpdater = scheduler.scheduleAtFixedRate(updater, 10, 10, SECONDS);
	}
}
