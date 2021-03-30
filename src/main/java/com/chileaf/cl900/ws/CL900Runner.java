package com.chileaf.cl900.ws;

import com.alibaba.fastjson.JSONObject;
import com.sdk.common.ChileafApi;
import com.sdk.common.ChileafApiUtil;
import com.sdk.common.ChileafCallBack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Order(1)
@Component
public class CL900Runner implements ApplicationRunner {

    private static final int mWheel = 2340;
    private static final long mOffline = 10000;
    private static final ConcurrentHashMap<String, Long> mCadence = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> mCacheTime = new ConcurrentHashMap<>();

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始解析设备数据，时间:" + new Date());
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkOffline();
            }
        }, 0, 5000);
        ChileafApi api = ChileafApiUtil.getInstance();
        api.isDubug(false);
        api.setPerimeter(2340);
        api.connectDevice(8081, new ChileafCallBack() {
            @Override
            public void onComplete(Object result) {
                parseJson(api, result);
            }
        });
    }

    private synchronized void checkOffline() {
        log.info("缓存设备列表:" + mCacheTime.toString());
        for (Map.Entry<String, Long> next : mCacheTime.entrySet()) {
            long current = System.currentTimeMillis();
            long delta = current - next.getValue();
            if (delta >= mOffline) {
                Map<String, Object> cache = new HashMap<>();
                cache.put("deviceId", next.getKey());
                cache.put("deviceType", "-1");
                try {
                    WebSocketServer.sendMessage(JSONObject.toJSONString(cache), null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                log.info("移除离线设备:" + next.getKey());
                mCacheTime.remove(next.getKey());
                mCadence.remove(next.getKey());
            }
        }
    }

    /**
     * type
     * 1:Heart Rate Device
     * 2:Rope skipping
     * 3:cadence
     * 4:speed
     */
    private synchronized void parseJson(final ChileafApi api, Object object) {
        try {
            String cmd = api.getCmd();
            String deviceId = api.getDeviceID();
            if (!deviceId.isEmpty() && cmd.equals("01")) {
                Map<String, Object> data = new HashMap<>();
                String deviceType = api.getDeviceType();
                switch (deviceType) {
                    case "Heart Rate Device":
                        data.put("deviceId", deviceId);
                        data.put("deviceType", "1");
                        String heartRate = api.getHeartrate();
                        if (isNotValid(heartRate)) {
                            return;
                        }
                        data.put("deviceName", "HeartRate Monitor");
                        data.put("heartRate", Integer.parseInt(heartRate));
                        data.put("calorie", api.getCalorie());
                        data.put("step", api.getStep());
                        data.put("rssi", api.getRssi());
                        break;
                    case "Rope skipping":
                        data.put("deviceId", deviceId);
                        data.put("deviceType", "2");
                        String count = api.getCounter();
                        if (isNotValid(count)) {
                            return;
                        }
                        data.put("deviceName", "Rope Skipping");
                        data.put("count", count);
                        data.put("time", api.getTime());
                        data.put("rssi", api.getRssi());
                        break;
                    case "cadence":
                        data.put("deviceId", deviceId);
                        data.put("deviceType", "3");
                        String cadence = api.getCadence();
                        if (isNotValid(cadence)) {
                            return;
                        }
                        data.put("deviceName", "Cadence");
                        data.put("cadence", cadence);
                        data.put("rssi", api.getRssi());
                        break;
                    case "speed":
                        data.put("deviceId", deviceId);
                        data.put("deviceType", "4");
                        String speed = api.getSpeed();
                        if (isNotValid(speed)) {
                            return;
                        }
                        data.put("deviceName", "Cadence");
                        data.put("speed", speed);
                        //计算距离,记录首次开始时间
                        long current = System.currentTimeMillis();
                        if (mCadence.get(deviceId) == null) {
                            mCadence.put(deviceId, current);
                            data.put("distance", 0);
                        } else {
                            long start = mCadence.get(deviceId);
                            long delta = current - start;
                            if (delta > 0) {
                                //(((delta / 1000) * 1000) / (60 * 60)) km/h -> m/s
                                int distance = (int) delta / 3600 * mWheel / 1000;
                                data.put("distance", distance);
                            } else {
                                data.put("distance", "");
                            }
                        }
                        data.put("rssi", api.getRssi());
                        break;
                }
                if (!data.isEmpty()) {
                    WebSocketServer.sendMessage(JSONObject.toJSONString(data), null);
                    long current = System.currentTimeMillis();
                    if (mCacheTime.get(deviceId) != null) {
                        mCacheTime.replace(deviceId, current);
                    } else {
                        mCacheTime.put(deviceId, current);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isNotValid(String value) {
        return value.isEmpty() || Float.parseFloat(value) == 0f;
    }

}

