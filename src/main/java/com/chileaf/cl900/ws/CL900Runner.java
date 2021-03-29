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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Order(1)
@Component
public class CL900Runner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        log.info("------------->" + "项目启动，now =" + new Date());
        ChileafApi api = ChileafApiUtil.getInstance();
        api.isDubug(false);
        api.setPerimeter(2340);
        api.connectDevice(8081, new ChileafCallBack() {
            @Override
            public void onComplete(Object result) {
                parseJson(api, result);
//                String cmd = api.getCmd();
//                System.out.println("hubid:" + api.getHubid() + "\tcommand：" + cmd);
//                try {
//                    WebSocketServer.sendMessage(api.getHubid() + api.getDeviceType(), null);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                if ("01".equals(cmd)) {
//                    String Devicetyp = api.getDeviceType();
//                    if ("Heart Rate Device".equals(Devicetyp)) {
//                        //只有当日志中的数据中的某个是00，80才会出卡路里、步数，只有当是07得时候才有电池
//                        System.out.println("Battery level：" + api.getBattery());
//                        System.out.println("Device ID：" + api.getDeviceID() + "\theart rate：" + api.getHeartrate() + "\tSteps:" + api.getStep() + "\tcalorie:" + api.getCalorie() + "(Kcal)"); //If there are calories, there are no step. If there are step, there are no calories
//                    } else if ("Rope skipping".equals(Devicetyp)) {
//                        System.out.println("Device ID：" + api.getDeviceID() + "\tCounter: " + api.getCounter() + "\tActivity time: " + api.getTime() + "s");
//                    } else if ("cadence".equals(Devicetyp)) {
//                        System.out.println("Device ID：" + api.getDeviceID() + "\tcadence: " + api.getCadence() + " RPM");
//                    } else if ("speed".equals(Devicetyp)) {
//                        System.out.println("Device ID：" + api.getDeviceID() + "\tspeed: " + api.getSpeed() + " km/h");
//                    }
//                    System.out.println("\tRssi" + api.getRssi());
//                } else if ("02".equals(cmd)) {
//                    System.out.println("Bluetooth name:" + api.getBleName());
//                } else if ("04".equals(cmd)) {
//                    //getDataSourceSelection:00:ant  01ble  02:ant+Bluetooth  ff:All shut down
//                    System.out.println("Transmission frequency：" + api.getFrequency() + "\thub data sources:" + api.getDataSourceSelection() + "\tRemark length:" + api.getRemarks() + "\t" + "Limit Bluetooth name length：" + api.getBlethNamelen() + "\trestricted scan Bluetooth UUID length：" + api.getBleUUIDlen() + "\tRestricted scan of Bluetooth rssis：" + api.getRssi() + "\tIntranet / Extranet logo：" + api.getSignals() + "\tIP length of server / TV box：" + api.getIPLength() + "\tPort length of server box：" + api.getPortlen());
//                }
            }
        });
    }

    private synchronized void parseJson(final ChileafApi api, Object object) {
        try {
            String cmd = api.getCmd();
            String deviceId = api.getDeviceID();
            if (!deviceId.isEmpty() && cmd.equals("01")) {
                Map<String,String> data = new HashMap<>();
                String deviceType = api.getDeviceType();
                switch (deviceType) {
                    case "Heart Rate Device":
                        data.put("deviceId", deviceId);
                        data.put("deviceType", deviceType);
                        data.put("heartRate", api.getHeartrate());
                        data.put("calorie", api.getCalorie());
                        data.put("step", api.getStep());
                        data.put("rssi", api.getRssi());
                        break;
                    case "Rope skipping":
                        data.put("deviceId", deviceId);
                        data.put("deviceType", deviceType);
                        data.put("count", api.getCounter());
                        data.put("time", api.getTime());
                        data.put("rssi", api.getRssi());
                        break;
                    case "cadence":
                        data.put("deviceId", deviceId);
                        data.put("deviceType", deviceType);
                        data.put("cadence", api.getCadence());
                        data.put("rssi", api.getRssi());
                        break;
                    case "speed":
                        data.put("deviceId", deviceId);
                        data.put("deviceType", deviceType);
                        data.put("speed", api.getSpeed());
                        data.put("rssi", api.getRssi());
                        break;
                }
                if (!data.isEmpty()) {
                    WebSocketServer.sendMessage(JSONObject.toJSONString(data), null);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

