package com.atom.clond.companion.activity;

import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.text.TextUtils;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.animation.Animation;
import com.amap.api.maps.model.animation.ScaleAnimation;
import com.atom.clond.companion.R;
import com.atom.clond.companion.adapter.ChatAdapter;
import com.atom.clond.companion.bean.DeviceBean;
import com.atom.clond.companion.bean.MessageBean;
import com.atom.clond.companion.bean.MessageListBean;
import com.atom.clond.companion.common.Constants;
import com.atom.clond.companion.common.GlobalValue;
import com.atom.clond.companion.http.HttpCallback;
import com.atom.clond.companion.req.HttpRequest;
import com.atom.clond.companion.utils.ByteUtils;
import com.atom.clond.companion.utils.ToastUtils;
import com.atom.clond.companion.ws.WsEventListener;
import com.atom.clond.companion.ws.WsManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 设备通讯页面
 *
 * @author bohan.chen
 */
public class DeviceChatActivity extends AppCompatActivity implements WsEventListener {

    private EditText etMsg;
    private ChatAdapter mAdapter;
    private SwipeRefreshLayout srl;
    private LinearLayout llChat;
    private LinearLayout llFun1;
    private LinearLayout llFun2;
    private TextView tvTitle;
    private RecyclerView rv;
    private Button mBtnSend,mBtnClean;
    private Handler mMainHandler;

    private int pageNo = 1;

    private DeviceBean mDeviceBean;
    private WsManager wsManager;


    /**
     * 是否为16进制发送
     */
    private boolean isHexSend = false;

    /**
     * 是否16进制显示
     */
    private boolean isHexShow = false;
    /**
     * 随机生成的uuid
     */
    private String uuid;
    public AMap aMap;
    public MapView mMapView;
    private UiSettings mUiSettings;
    private CheckBox mStyleCheckbox;
    private SharedPreferences sp;//编辑器
    private TextView trip_information;
    public Marker PeiQiMarker;//佩奇标签
    public LatLng latLng;//地理位置
    public MarkerOptions markerOption;
    double latitude=0,latitude_last=0;
    double longitude=0,longitude_last=0;
    int bloodOxygen=0,heartRate=0,bloodPressureHigh=0,bloodPressureLow=0,trip=0;
    int bloodOxygen_last=0,heartRate_last=0,bloodPressureHigh_last=0,bloodPressureLow_last=0,trip_last=0;
    String p;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_chat);
        MapInit(savedInstanceState);
        initView();

    }
    /**
     * 初始化AMap对象
     */
    private void MapInit(@Nullable Bundle savedInstanceState) {
        mMapView = findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);//创建地图
        if (aMap == null) {
            aMap = mMapView.getMap();
            modeEcho();
            mUiSettings =  aMap.getUiSettings();
            mUiSettings.setCompassEnabled(true);//显示指南针
            mUiSettings.setScaleControlsEnabled(true);//显示比例尺
            mUiSettings.setLogoBottomMargin(-69);//隐藏logo

            MyLocationStyle myLocationStyle = new MyLocationStyle();//初始化定位蓝点样式类
            myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。

            aMap.moveCamera(CameraUpdateFactory.zoomTo(aMap.getCameraPosition().zoom));
            aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
            aMap.getUiSettings().setMyLocationButtonEnabled(true);//设置默认定位按钮是否显示，非必需设置。
            aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        }

    }
    /**
     * 软件设置回显
     */
    private void modeEcho() {

        mStyleCheckbox = findViewById(R.id.nightmap);
        // 要想回显CheckBox的状态 我们需要取得数据
        // [1] 还需要获得SharedPreferences
        sp = getSharedPreferences("isChecked", 0);
        boolean result = sp.getBoolean("choose", false); // 这里就是开始取值了 false代表的就是如果没有得到对应数据我们默认显示为false
        if(result){
            aMap.setMapType(AMap.MAP_TYPE_NIGHT);//夜景地图模式
        }else{
            aMap.setMapType(AMap.MAP_TYPE_NORMAL);// 矢量地图模式
        }
        // 把得到的状态设置给CheckBox组件
        mStyleCheckbox.setChecked(result);
        mStyleCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                //isChecked = mStyleCheckbox.isChecked();
                if(isChecked) {
                    sp = getSharedPreferences("isChecked", 0);
                    // 使用编辑器来进行操作
                    SharedPreferences.Editor edit = sp.edit();
                    // 将勾选的状态保存起来
                    edit.putBoolean("choose", true); // 这里的choose就是一个key 通过这个key我们就可以得到对应的值
                    // 最好我们别忘记提交一下
                    edit.commit();
                    aMap.setMapType(AMap.MAP_TYPE_NIGHT);//夜景地图模式
                }else{
                    sp = getSharedPreferences("isChecked", 0);
                    // 使用编辑器来进行操作
                    SharedPreferences.Editor edit = sp.edit();
                    // 将勾选的状态保存起来
                    edit.putBoolean("choose", false); // 这里的choose就是一个key 通过这个key我们就可以得到对应的值
                    // 最好我们别忘记提交一下
                    edit.commit();
                    aMap.setMapType(AMap.MAP_TYPE_NORMAL);// 矢量地图模式
                }
            }
        });
    }

    private void initView() {
        getHistoryRecord();
 //       trip_information = findViewById(R.id.trip_information);
        mBtnSend=findViewById(R.id.btnSend);
        mBtnSend.setText(mDeviceBean.getName());
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage("hhh");
            }
        });

 //       mBtnClean=findViewById(R.id.btnClean);
//        mBtnClean.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                trip_information.setText("");
//                ToastUtils.showShort("删除成功");
//            }
//        });
//        if(mDeviceBean.getShow_id().equals(1000)) {
//            mBtnClean.setVisibility(View.INVISIBLE);
//            trip_information.setVisibility(View.INVISIBLE);
//        }
//        if(mDeviceBean.getShow_id().equals(1002)) {
//            mBtnClean.setVisibility(View.VISIBLE);
//            trip_information.setVisibility(View.VISIBLE);
//        }
        mMainHandler = new Handler(Looper.getMainLooper());

        connectDevice();

    }
    /**
     * 弹出提示
     */
    private void showAlert(boolean isSuccess, String actionName) {
        if (isSuccess) {
            ToastUtils.showShort(actionName + "成功");
        } else {
            ToastUtils.showShort(actionName + "失败");
        }
    }
    /**
     * 获取聊天记录
     */
    private void getHistoryRecord() {
        mDeviceBean = getIntent().getParcelableExtra(ActivityKey.DEVICE_BEAN);
        System.out.println(ActivityKey.DEVICE_BEAN);
        HttpRequest.getChatHisData(pageNo, GlobalValue.orgId, mDeviceBean.getNumber(), new HttpCallback<MessageListBean>() {
            @Override
            public void onSuccess(MessageListBean data) {
            }

            @Override
            public void onError(int code, String errMsg) {
                ToastUtils.showShort(errMsg);
            }

            @Override
            public void onFinish() {
            }
        });
    }

    /**
     * 连接设备
     */
    private void connectDevice() {
        wsManager = new WsManager();
        uuid = UUID.randomUUID().toString();
        String url = Constants.WS_SERVER_ADDRESS + Constants.HTTP_TOKEN + "/org/" + GlobalValue.orgId + "?token=" + uuid;
        wsManager.init(url, mDeviceBean, this);
    }

    /**
     * 发送数据
     */
    private void sendMessage(String msg) {
        showAlert(wsManager.subDevice(), "订阅");
        if (!TextUtils.isEmpty(msg)) {
            byte[] dataByte;
           // etMsg.setText("");
            MessageBean messageBean = new MessageBean();
            messageBean.setName("TX");
            if (isHexSend) {
                dataByte = ByteUtils.hexStringToByteArray(msg);
                messageBean.setHexData(msg);
                messageBean.setNormalData(new String(dataByte));
            } else {
                dataByte = msg.getBytes();
                messageBean.setNormalData(msg);
                messageBean.setHexData(ByteUtils.str2HexStr(msg));
            }
            messageBean.setDataByte(dataByte);
            if (wsManager.sendMsg(dataByte)) {

                //ToastUtils.showShort("发送成功");
            } else {
            //    ToastUtils.showShort("发送失败");
            }
        }
    }



    @Override
    public void onFailed(final String errMsg) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {

                //ToastUtils.showShort(errMsg);
            }
        });
    }

    @Override
    public void onRecMessage(final MessageBean data) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                String str=data.getNormalData();
                System.out.println(str);
                if(str.length()>10) {
//                    if(mDeviceBean.getShow_id().equals("1002")) {
//                        longitude = Double.valueOf(str.substring(str.indexOf("longitude:") + "longitude:".length(), str.indexOf(",latitude")));
//                        if (longitude != 0) longitude_last = longitude;
//                        latitude = Double.valueOf(str.substring(str.indexOf("latitude:") + "latitude:".length(), str.indexOf(",blood_pressure_H")));
//                        if (latitude != 0) latitude_last = latitude;
//                        bloodPressureHigh = Integer.valueOf(str.substring(str.indexOf("blood_pressure_H:") + "blood_pressure_H:".length(), str.indexOf(",blood_pressure_L")));
//                        if (bloodPressureHigh != 0) bloodPressureHigh_last = bloodPressureHigh;
//                        bloodPressureLow = Integer.valueOf(str.substring(str.indexOf("blood_pressure_L:") + "blood_pressure_L:".length(), str.indexOf(",Heart_rate")));
//                        if (bloodPressureLow != 0) bloodPressureLow_last = bloodPressureLow;
//                        heartRate = Integer.valueOf(str.substring(str.indexOf("Heart_rate:") + "Heart_rate:".length(), str.length()));
//                        if (heartRate != 0) heartRate_last = heartRate;
//                        p="状态:上线"+"\n"+"心率:"+String.valueOf(heartRate)+"\n"+"血压:"+String.valueOf(bloodPressureLow)+"~"+String.valueOf(bloodPressureHigh);
//                    }
                    if(mDeviceBean.getShow_id().equals("1000")) {
                        longitude=Double.valueOf(str.substring(str.indexOf("longitude:")+"longitude:".length(),str.indexOf(",latitude")));
                        latitude=Double.valueOf(str.substring(str.indexOf("latitude:")+"latitude:".length(),str.indexOf(",heartRate")));
                        heartRate= Integer.valueOf(str.substring(str.indexOf("heartRate:")+"heartRate:".length(),str.indexOf(",bloodOxygen")));
                        bloodOxygen=Integer.valueOf(str.substring(str.indexOf("bloodOxygen:")+"bloodOxygen:".length(),str.length()));
                        p="状态:上线"+"\n"+"心率:"+String.valueOf(heartRate)+"\n"+"血氧:"+String.valueOf(bloodOxygen);
                    }
                    latLng = new LatLng(latitude+0.00055,longitude+0.00675);
                    markerOption = new MarkerOptions()
                            .position(latLng)
                            .title(mDeviceBean.getName())
                            .snippet(p)
                            .visible(true)
                            .icon(BitmapDescriptorFactory
                                    .fromBitmap(BitmapFactory
                                            .decodeResource(getResources(),image(mDeviceBean.getShow_id()))))
                            .setFlat(true);//设置marker平贴地图效果

                    Animation animation = new ScaleAnimation(0,1,0,1);
                    animation.setInterpolator(new LinearInterpolator());
                    animation.setDuration(1000);//整个移动所需要的时间

                    CameraPosition cameraPosition = new CameraPosition(latLng, aMap.getCameraPosition().zoom, 0, 30);
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
                    aMap.moveCamera(cameraUpdate);

                    PeiQiMarker=aMap.addMarker(markerOption);
                    PeiQiMarker.setAnimation(animation); /*设置动画*/
                    PeiQiMarker.startAnimation();//*开始动画*/
                    PeiQiMarker.showInfoWindow();//展示消息
                }else trip=Integer.valueOf(str.substring(str.indexOf("trip:")+"trip:".length(),str.length()));
                if(trip>0&&trip<5)
                {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// HH:mm:ss
                    //获取当前时间
                    Date date = new Date(System.currentTimeMillis());
                    trip_information.setText(simpleDateFormat.format(date)+"老人摔倒");
                }
            }
        });
    }
    public int image(String Id) {
        if(Id.equals("1000")) return R.drawable.peiqi;
        if(Id.equals("1002")) return R.drawable.oldman2;
        else return 0;
    }
    @Override
    public void onDisconnect() {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                ToastUtils.showShort("设备已断开");
            }
        });
    }

    @Override
    public void onConnect() {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                ToastUtils.showShort("设备已连接");
            }
        });
    }

    @Override
    protected void onDestroy() {
        wsManager.close();
        super.onDestroy();
    }
}
