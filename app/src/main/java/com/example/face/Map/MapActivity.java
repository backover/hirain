package com.example.face.Map;

import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.*;
import com.amap.api.maps.model.*;
import com.example.face.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements LocationSource,AMapLocationListener, View.OnClickListener {
    private static final String TAG = "UDPSocket";

    private MapView mMapView;
    private AMap aMap;
    private UiSettings mUiSettings;
    private AMapLocationClient mAMapLocationClient;
    private AMapLocationClientOption mAMapLocationClientOption;
    private AMapLocationListener mAMapLocationListener;
    private Polyline mPolyline;

    private double[] GPSLat = new double[120];
    private double[] GPSLng = new double[120];
    private String route;
    private List<LatLng> points = new ArrayList<LatLng>();//经纬度数组

    private final Handler mHandler = new Handler();
    private Runnable udpRunnable;

    private Button btnConfirm;
    private TextView tvSpeed;
    private TextView tvBarrier;
    private TextView numDriveState;
    private TextView numAvoid;
    private TextView numLocation;
    private TextView numGPS;
    private TextView numActuator;
    private TextView numSensor;

    private Thread threadData;
    private UDPSocket mUDPSocket;
    private Boolean isThreadRunning = false;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        bindView();

        //UI更新数据显示
        udpRunnable = new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        };

        //地图初始化
        mMapView = (MapView) findViewById(R.id.map_view);
        mMapView.onCreate(savedInstanceState);
        aMap = mMapView.getMap();//获得地图
        aMap.getUiSettings().setZoomControlsEnabled(false);
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(31.944389236472063,118.90007793796346),16));


    }

    private void bindView() {
        View v = this.findViewById(R.id.buttonRoute1);
        v.setOnClickListener(this);
        v = this.findViewById(R.id.buttonRoute2);
        v.setOnClickListener(this);
        v = this.findViewById(R.id.buttonRoute3);
        v.setOnClickListener(this);

        btnConfirm = this.findViewById(R.id.buttonConfirm);
        tvSpeed = this.findViewById(R.id.tvSpeed);
        tvBarrier = this.findViewById(R.id.tvBarrier);
        numDriveState = this.findViewById(R.id.numDriveState);
        numAvoid = this.findViewById(R.id.numAvoid);
        numLocation = this.findViewById(R.id.numLocation);
        numGPS = this.findViewById(R.id.numGPS);
        numActuator = this.findViewById(R.id.numActuator);
        numSensor = this.findViewById(R.id.numSensor);

        btnConfirm.setOnClickListener(this);
    }

    /**
     * 启动数据处理线程
     */
    private void startDataThread() {
        threadData = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isThreadRunning) {
                    if (mUDPSocket != null && mUDPSocket.isReceiveFlag()) {
//                        etReceive.setText(mUDPSocket.getReceiveData());
//                        if (TextUtils.isDigitsOnly(mUDPSocket.getReceiveData())) {
//                            etReceive.setText(encodeData(Float.parseFloat(mUDPSocket.getReceiveData())).toString());
//                        }
                        Log.d(TAG,mUDPSocket.getReceiveData());
                        mHandler.post(udpRunnable);
                    }
                }
            }
        });
        threadData.start();
    }

    //更新UI
    private void updateUI() {
        tvSpeed.setText(mUDPSocket.getReceiveData());
    }

    private void mapInit() {
        //        locationPoint();

//        //初始化定位
//        mAMapLocationClient = new AMapLocationClient(getApplicationContext());
//        //初始化AMapLocationClientOption对象
//        mAMapLocationClientOption = new AMapLocationClientOption();
//        //监听回调
//        mAMapLocationListener = new AMapLocationListener() {
//            @Override
//            public void onLocationChanged(AMapLocation aMapLocation) {
//
//            }
//        };
//        //设置定位模式为高精度模式。
//        mAMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
//        //设置定位回调监听
//        mAMapLocationClient.setLocationListener(mAMapLocationListener);
//        //获取一次定位结果
//        mAMapLocationClientOption.setOnceLocation(true);
//        //设置是否返回地址信息（默认返回地址信息）
//        mAMapLocationClientOption.setNeedAddress(true);
//        //给定位客户端对象设置定位参数
//        mAMapLocationClient.setLocationOption(mAMapLocationClientOption);
//        //启动定位
//        mAMapLocationClient.startLocation();
    }

    private void locationPoint() {
        MyLocationStyle myLocationStyle = new MyLocationStyle();//初始化定位
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//设定定位风格
        myLocationStyle.interval(2000);//连续定位时间
        aMap.setMyLocationStyle(myLocationStyle);//设置定位类型
        aMap.setMyLocationEnabled(true);//开启定位

        //设置地图控件
        mUiSettings = aMap.getUiSettings();
        mUiSettings.setCompassEnabled(true);//显示指南针
        mUiSettings.setMyLocationButtonEnabled(true);//显示定位按钮
    }

    //读取Json格式数据 读地图坐标
    private void loadJson(String fileName) throws JSONException {
        StringBuilder stringBuilder = new StringBuilder();
        AssetManager assetManager = this.getAssets();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(assetManager.open(fileName)));
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject jsonObject = new JSONObject(stringBuilder.toString());
        JSONArray jsonArray = jsonObject.getJSONArray("points");
        route = jsonObject.getString("route_id");
        points.clear();
        for (int i=1; i< jsonArray.length(); i++) {
            JSONObject jsonObjectPoint = jsonArray.getJSONObject(i);
            GPSLat[i] = Double.parseDouble(jsonObjectPoint.getString("latitude"));
            GPSLng[i] = Double.parseDouble(jsonObjectPoint.getString("longitude"));
            points.add(new LatLng(GPSLat[i],GPSLng[i]));
        }
    }

    //路线绘制
    private void setUpMap(List<LatLng> list) {
        if (mPolyline != null) {
            mPolyline.remove();
        }
        if (list.size()>1) {
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(31.944389236472063,118.90007793796346),16));
            aMap.setMapTextZIndex(2);
            mPolyline = aMap.addPolyline(new PolylineOptions()
                    .addAll(list)
                    .width(5)
                    .color(Color.GREEN));
            Toast.makeText(this,"路线"+route,Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(this,"no path",Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        isThreadRunning = false;
        if (mUDPSocket != null) {
            mUDPSocket.stopUDPSocket();
        }
        if (threadData != null) {
            threadData.interrupt();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {

    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {

    }

    @Override
    public void deactivate() {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonRoute1:
                try {
                    loadJson("route1.json");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                setUpMap(points);
                break;
            case R.id.buttonRoute2:
                try {
                    loadJson("route2.json");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                setUpMap(points);
                break;
            case R.id.buttonRoute3:
                try {
                    loadJson("route3.json");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                setUpMap(points);
                break;
            case R.id.buttonConfirm:
                mUDPSocket = new UDPSocket(this);
                mUDPSocket.startUDPSocket();
                isThreadRunning = true;
                startDataThread();
                Toast.makeText(this,"已连接",Toast.LENGTH_SHORT).show();
        }
    }
}
