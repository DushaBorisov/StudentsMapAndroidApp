package com.example.studentmap.Fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.studentmap.JsonParser;
import com.example.studentmap.MapViewModel;
import com.example.studentmap.Place;
import com.example.studentmap.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MapFragment extends Fragment implements OnMapReadyCallback {
    public MapFragment() {
        currentLocation = new Location("");
        currentLocation.setLatitude(55.751244);
        currentLocation.setLongitude(37.618423);
    }

    private MapView mMapView;
    private GoogleMap map;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    FusedLocationProviderClient client;
    Location currentLocation;
    HashMap<String, Integer> markers = new HashMap<String, Integer>();

    String url;

    MapViewModel mapViewModel;

    Bundle bundle;

    Spinner spType;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.map, container, false);
        mMapView = rootView.findViewById(R.id.mapView);
        mapViewModel = new ViewModelProvider(getActivity()).get(MapViewModel.class);
        initGoogleMap(savedInstanceState);

        Button btnFind;

        spType = rootView.findViewById(R.id.sp_type);
        btnFind = rootView.findViewById(R.id.btn_find);


        String[] placeNameList = {"Бары", "Кинотеатры", "Рестораны", "Кафе", "Клубы", "Магазины", "Торговые центры"};
        String[] placeTypeList = {"bar", "movie_theater", "restaurant", "cafe", "night_club", "supermarket", "shopping_mall"};


        spType.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, placeNameList));


        bundle = this.getArguments();
        if (mapViewModel.getUrl() != null && bundle == null) {
            spType.setSelection(mapViewModel.getSpinner());
            LiveData<List<Place>> data = mapViewModel.getData();
            data.observe(getActivity(), new Observer<List<Place>>() {
                @Override
                public void onChanged(List<Place> places) {
                    for (int i = 0; i < places.size(); i++) {
                        String name = places.get(i).getName();
                        LatLng latLng = new LatLng(places.get(i).getLatitude(), places.get(i).getLongitude());
                        MarkerOptions options = new MarkerOptions();
                        options.position(latLng);
                        options.title(name).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                        ;
                        markers.put(map.addMarker(options).getId(), i);
                        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                            @Override
                            public void onInfoWindowClick(Marker marker) {
                                if (markers.get(marker.getId()) != null) {
                                    Bundle bundle = new Bundle();
                                    bundle.putSerializable("place", places.get(markers.get(marker.getId())));
                                    PlaceCardFragment placeCardFragment = new PlaceCardFragment();
                                    placeCardFragment.setArguments(bundle);
                                    getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, placeCardFragment, "PlaceCardFragment").commit();
                                }
                            }
                        });
                    }
                }
            });
        }
        btnFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int i = spType.getSelectedItemPosition();
                mapViewModel.setSpinner(i);
                url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                        "?location=" + currentLocation.getLatitude() + "," + currentLocation.getLongitude() +
                        "&radius=2000" + "&language=ru" + "&type=" + placeTypeList[i] + "&sensor=true" +
                        "&key=" + "AIzaSyBomRHM2cJo2o33ZULSbZHbisJs4JZQSKE";

                mapViewModel = new ViewModelProvider(getActivity()).get(MapViewModel.class);
                if (url != null) {
                    mapViewModel.setUrl(url);
                }

                LiveData<List<Place>> data = mapViewModel.getData();

                data.observe(getActivity(), new Observer<List<Place>>() {
                    @Override
                    public void onChanged(List<Place> places) {
                        map.clear();
                        LatLng mlatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                        MarkerOptions mOptions = new MarkerOptions().position(mlatLng).title("Вы здесь").visible(true);
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(mlatLng, 13));
                        mOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
                        map.addMarker(mOptions);
                        for (int i = 0; i < places.size(); i++) {
                            String name = places.get(i).getName();
                            LatLng latLng = new LatLng(places.get(i).getLatitude(), places.get(i).getLongitude());
                            MarkerOptions options = new MarkerOptions();
                            options.position(latLng);
                            options.title(name).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                            markers.put(map.addMarker(options).getId(), i);
                            map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                                @Override
                                public void onInfoWindowClick(Marker marker) {
                                    if (markers.get(marker.getId()) != null) {
                                        Bundle bundle = new Bundle();
                                        bundle.putSerializable("place", places.get(markers.get(marker.getId())));
                                        PlaceCardFragment placeCardFragment = new PlaceCardFragment();
                                        placeCardFragment.setArguments(bundle);
                                        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, placeCardFragment, "PlaceCardFragment").commit();
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });

        return rootView;
    }

    private void initGoogleMap(Bundle savedInstanceState) {
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mMapView.onCreate(mapViewBundle);
        getCurrentLocation();
    }

    public void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }
        client = LocationServices.getFusedLocationProviderClient(getActivity());
        Task<Location> task = client.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = location;
                    mapViewModel.setCurrentLocation(currentLocation);
                }
                mMapView.getMapAsync(MapFragment.this);
            }
        });
        mapViewModel.setCurrentLocation(currentLocation);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        map = googleMap;
        //LatLng latLng = new LatLng(55.751244, 37.618423);
        MarkerOptions options = new MarkerOptions().position(latLng).title("Вы здесь").visible(true);
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));
        map.addMarker(options);
        if (bundle != null) {
            spType.setSelection(mapViewModel.getSpinner());
            Place place = (Place) bundle.getSerializable("place");
            LatLng latLngPlace = new LatLng(place.getLatitude(), place.getLongitude());
            MarkerOptions optionsPlace = new MarkerOptions();
            optionsPlace.position(latLngPlace);
            optionsPlace.title(place.getName()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
            ;
            map.addMarker(optionsPlace);
            map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("place", place);
                    PlaceCardFragment placeCardFragment = new PlaceCardFragment();
                    placeCardFragment.setArguments(bundle);
                    getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, placeCardFragment, "PlaceCardFragment").commit();
                }
            });
            String urlRoute = "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                    +currentLocation.getLatitude() + "," + currentLocation.getLongitude() +
                    "&destination=" + place.getLatitude() + "," + place.getLongitude() +
                    "&mode=walking&sensor=true&key=AIzaSyBomRHM2cJo2o33ZULSbZHbisJs4JZQSKE";

            mapViewModel.setUrlRoute(urlRoute);
            LiveData<List<List<HashMap<String, String>>>> dataRoutes = mapViewModel.getDataRoutes();
            dataRoutes.observe(getActivity(), new Observer<List<List<HashMap<String, String>>>>() {
                @Override
                public void onChanged(List<List<HashMap<String, String>>> routes) {

                    ArrayList<LatLng> points = null;
                    PolylineOptions lineOptions = null;
                    MarkerOptions markerOptions = new MarkerOptions();

                    // Traversing through all the routes
                    for (int i = 0; i < routes.size(); i++) {
                        points = new ArrayList<LatLng>();
                        lineOptions = new PolylineOptions();

                        // Fetching i-th route
                        List<HashMap<String, String>> path = routes.get(i);

                        // Fetching all the points in i-th route
                        for (int j = 0; j < path.size(); j++) {
                            HashMap<String, String> point = path.get(j);

                            double lat = Double.parseDouble(point.get("lat"));
                            double lng = Double.parseDouble(point.get("lng"));
                            LatLng position = new LatLng(lat, lng);

                            points.add(position);
                        }

                        // Adding all the points in the route to LineOptions
                        lineOptions.addAll(points);
                        lineOptions.width(10);

                        lineOptions.color(Color.rgb(126, 32, 142));
                    }

                    map.addPolyline(lineOptions);
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 101:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocation();
                }
                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }

        mMapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    public void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        //getFragmentManager().beginTransaction().remove(MapFragment.this).commit();
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

}