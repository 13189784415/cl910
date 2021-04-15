package com.chileaf.cl910.ws;

import com.alibaba.fastjson.JSONObject;
import com.kili.util.ChileafApi;
import com.kili.util.ChileafApiUtil;
import com.kili.util.ChileafCallBack;
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
public class CL910Runner implements ApplicationRunner {

    private float mDistance = 0;
    private static final int mWheel = 2340;
    private static final long mReset = 2 * 1000;
    private static final long mInterval = 3 * 1000;
    private static final long mOffline = 2 * 60 * 1000;
    private static final ConcurrentHashMap<String, Long> mCadence = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> mSpeed = new ConcurrentHashMap<>();
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
        }, 0, mInterval);
        Timer reset = new Timer();
        reset.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkReset();
            }
        }, 0, mReset);
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

    /**
     * 设备超过离线时间需要发送离线消息
     */
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
                mSpeed.remove(next.getKey());
            }
        }
    }

    /**
     * 设备超过重置时间(3秒)没数据需要清除转速和踏频，保留距离
     */
    private synchronized void checkReset() {
        log.info("设备列表 Cadence:" + mCadence.toString());
        for (Map.Entry<String, Long> next : mCadence.entrySet()) {
            long current = System.currentTimeMillis();
            long time = mCacheTime.get(next.getKey());
            long delta = current - time;
            if (delta >= mInterval) {
                log.info("重置超时设备:" + next.getKey());
                Map<String, Object> cache = new HashMap<>();
                cache.put("deviceId", next.getKey());
                cache.put("deviceName", "Cadence");
                cache.put("deviceType", "3");
                cache.put("cadence", 0);
                cache.put("rssi", 0);
                try {
                    WebSocketServer.sendMessage(JSONObject.toJSONString(cache), null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        log.info("设备列表 Speed:" + mSpeed.toString());
        for (Map.Entry<String, Long> next : mSpeed.entrySet()) {
            long current = System.currentTimeMillis();
            long time = mCacheTime.get(next.getKey());
            long delta = current - time;
            if (delta >= mInterval) {
                log.info("重置超时设备:" + next.getKey());
                Map<String, Object> cache = new HashMap<>();
                cache.put("deviceId", next.getKey());
                cache.put("deviceName", "Speed");
                cache.put("deviceType", "4");
                cache.put("distance", "");
                cache.put("speed", 0);
                cache.put("rssi", 0);
                try {
                    WebSocketServer.sendMessage(JSONObject.toJSONString(cache), null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
    private int mCount = 0;

    private synchronized void parseJson(final ChileafApi api, Object object) {
        try {
            String cmd = api.getCmd();
            String deviceId = api.getDeviceID();
            if (deviceId.contains("1007")) {
                this.mCount++;
            }
            log.info("设备Id:" + deviceId + "次数:" + mCount);
            if (!deviceId.isEmpty() && cmd.equals("01")) {
                Map<String, Object> data = new HashMap<>();
                String deviceType = api.getDeviceType();
                long current = System.currentTimeMillis();
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
                        //计算距离,记录首次开始时间
                        if (mCadence.get(deviceId) == null) {
                            mCadence.put(deviceId, current);
                            data.put("cadence", 0);
                        }
                        data.put("rssi", api.getRssi());
                        break;
                    case "speed":
                        data.put("deviceId", deviceId);
                        data.put("deviceType", "4");
                        String speed = api.getSpeed();
                        if (isNotValid(speed)) {
                            return;
                        }
                        data.put("deviceName", "Speed");
                        data.put("speed", speed);
                        //计算距离,记录首次开始时间
                        if (mSpeed.get(deviceId) == null) {
                            mSpeed.put(deviceId, current);
                            data.put("distance", 0);
                            mDistance = 0;
                        } else {
                            long delta = current - mCacheTime.get(deviceId);
                            if (delta > 0 && delta < mInterval) {
                                //(((delta / 1000) * 1000) / (60 * 60)) km/h -> m/s
                                float distance = delta / 3600f * mWheel / 1000f;
                                mDistance += distance;
                                data.put("distance", String.format("%.2f", mDistance));
                            } else {
                                data.put("distance", "");
                            }
                        }
                        data.put("rssi", api.getRssi());
                        break;
                }
                if (!data.isEmpty()) {
                    WebSocketServer.sendMessage(JSONObject.toJSONString(data), null);
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

