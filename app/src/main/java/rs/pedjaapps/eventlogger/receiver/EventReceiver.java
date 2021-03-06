package rs.pedjaapps.eventlogger.receiver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Date;
import java.util.Locale;

import rs.pedjaapps.eventlogger.MainActivity;
import rs.pedjaapps.eventlogger.App;
import rs.pedjaapps.eventlogger.R;
import rs.pedjaapps.eventlogger.constants.Constants;
import rs.pedjaapps.eventlogger.constants.EventLevel;
import rs.pedjaapps.eventlogger.constants.EventType;
import rs.pedjaapps.eventlogger.model.Event;
import rs.pedjaapps.eventlogger.model.EventDao;
import rs.pedjaapps.eventlogger.model.Icon;
import rs.pedjaapps.eventlogger.service.EventService;
import rs.pedjaapps.eventlogger.utility.Utility;

/**
 * Created by pedja on 11.4.14..
 */
public class EventReceiver extends BroadcastReceiver
{
	public static final String INTENT_ACTION_APP_LAUNCHED = "rs.pedjaapps.elroothelper.APP_LAUNCHED";
	public static final String INTENT_EXTRA_PACKAGE_NAME = "rs.pedjaapps.elroothelper.PACKAGE_NAME";
	
    public static void sendLocalBroadcast(Event event)
    {
        sendLocalBroadcast(event, App.getInstance());
    }

    public static void sendLocalBroadcast(Event event, Context context)
    {
        Intent intent = new Intent();
        intent.setAction(MainActivity.ACTION_ADD_EVENT);
        intent.putExtra(MainActivity.EXTRA_EVENT, event);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if(isInitialStickyBroadcast())
            return;
        if(intent == null || intent.getAction() == null /*|| intent.getExtras() == null*/)
        {
            return;
        }
        context.startService(new Intent(context, EventService.class));
        Bundle extras = intent.getExtras();
        if(extras != null)
        {
            extras.isEmpty();// has effect of unparcelling Bundle
            for (String s : extras.keySet())
            {
                Log.d(Constants.LOG_TAG, "Bundle extras map key: " + s);
            }
        }
		if(intent.getAction().equals(INTENT_ACTION_APP_LAUNCHED))
		{
			String packageName = intent.getStringExtra(INTENT_EXTRA_PACKAGE_NAME);
			if(packageName == null)return;
			String appName = Utility.getNameForPackage(context, packageName);
			
			Event event = new Event();
			event.setTimestamp(new Date());
			event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
			event.setType(EventType.getIntForType(EventType.app));
			event.setShort_desc(context.getString(R.string.app_started, appName));
			event.setLong_desc(context.getString(R.string.app_started_desc, appName, packageName));
            Icon icon = new Icon();
            icon.setIcon(Utility.getApplicationIcon(context, packageName));
            long iconId = App.getInstance().getDaoSession().getIconDao().insert(icon);
			event.setIcon_id(iconId);
			EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
			eventDao.insert(event);
			EventReceiver.sendLocalBroadcast(event);
		}
        if(intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION))
        {
            /*networkinfo(NetworkInfo), bssid(string), linkProperties(LinkProperties)*/
            /*wifiInfo=SSID: WiredSSID, BSSID: 01:80:c2:00:00:03, MAC: 08:00:27:64:07:04, Supplicant state: COMPLETED, RSSI: -65, Link speed: 0, Net ID: 0, Metered hint: false*/
            NetworkInfo networkInfo = extras != null ? (NetworkInfo) extras.getParcelable("networkInfo") : null;
            if(networkInfo == null || networkInfo.getState() != NetworkInfo.State.CONNECTED && networkInfo.getState() != NetworkInfo.State.DISCONNECTED) return;
            WifiInfo wifiInfo = extras.getParcelable("wifiInfo");
            if(networkInfo.getState() == NetworkInfo.State.CONNECTED && wifiInfo == null)return;
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.wifi));
            if(networkInfo.isConnected())
            {
                event.setShort_desc(context.getString(R.string.wifi_state_changed, "green", context.getString(R.string.connected_upper)));
                event.setLong_desc(context.getString(R.string.wifi_connected_description, wifiInfo.getSSID(), wifiInfo.getBSSID(), wifiInfo.getMacAddress(), wifiInfo.getIpAddress()));
            }
            else
            {
                event.setShort_desc(context.getString(R.string.wifi_state_changed, "red", context.getString(R.string.disconnected_upper)));
                event.setLong_desc(context.getString(R.string.wifi_disconnected_description));
            }

            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION))
        {
            /*wifi_state(int), previous_wifi_state(int)*/
            int wifiState = extras != null ? extras.getInt("wifi_state") : -1;
            if (wifiState != WifiManager.WIFI_STATE_DISABLED && wifiState != WifiManager.WIFI_STATE_ENABLED)
                return;
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.wifi));
            switch (wifiState)
            {
                case WifiManager.WIFI_STATE_DISABLED:
                    event.setShort_desc(context.getString(R.string.wifi_state_changed, "red", context.getString(R.string.disabled_upper)));
                    event.setLong_desc(context.getString(R.string.wifi_toggle_description, context.getString(R.string.disabled_lower)));
                    break;
                case WifiManager.WIFI_STATE_ENABLED:
                    event.setShort_desc(context.getString(R.string.wifi_state_changed, "green", context.getString(R.string.enabled_upper)));
                    event.setLong_desc(context.getString(R.string.wifi_toggle_description, context.getString(R.string.enabled_lower)));
                    break;
            }
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED))
        {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if(state != BluetoothAdapter.STATE_OFF && state != BluetoothAdapter.STATE_ON
                    && state != BluetoothAdapter.STATE_CONNECTED && state != BluetoothAdapter.STATE_DISCONNECTED) return;
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.bluetooth));
            switch (state)
            {
                case BluetoothAdapter.STATE_OFF:
                    event.setShort_desc(context.getString(R.string.bluetooth_state_changed, "red", context.getString(R.string.disabled_upper)));
                    event.setLong_desc(context.getString(R.string.bluetooth_toggle_description, context.getString(R.string.disabled_lower)));
                    break;
                case BluetoothAdapter.STATE_ON:
                    event.setShort_desc(context.getString(R.string.bluetooth_state_changed, "green", context.getString(R.string.enabled_upper)));
                    event.setLong_desc(context.getString(R.string.bluetooth_toggle_description, context.getString(R.string.enabled_lower)));
                    break;
                case BluetoothAdapter.STATE_CONNECTED:

                    break;
                case BluetoothAdapter.STATE_DISCONNECTED:

                    break;
            }
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED))
        {
            BluetoothDevice device = extras != null ? (BluetoothDevice) extras.getParcelable(BluetoothDevice.EXTRA_DEVICE) : null;
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.bluetooth));
            event.setShort_desc(context.getString(R.string.bluetooth_state_changed, "green", context.getString(R.string.connected_upper)));
            if(device != null)
            {
                event.setLong_desc(context.getString(R.string.bluetooth_connected_description, device.getAddress(), device.getName()));
            }

            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED))
        {
            BluetoothDevice device = extras != null ? (BluetoothDevice) extras.getParcelable(BluetoothDevice.EXTRA_DEVICE) : null;
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.bluetooth));
            event.setShort_desc(context.getString(R.string.bluetooth_state_changed, "red", context.getString(R.string.disconnected_upper)));
            if(device != null)
            {
                event.setLong_desc(context.getString(R.string.bluetooth_disconnected_description, device.getAddress(), device.getName()));
            }

            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED"))
        {
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.sms));
            event.setShort_desc(context.getString(R.string.sms_received));

            SmsMessage[] messages = new SmsMessage[0];
            try
            {
                Object[] pdus = (Object[]) extras.get("pdus");
                messages = new SmsMessage[pdus.length];
                for(int i = 0; i < pdus.length; i++)
                {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            String address = messages[0].getOriginatingAddress();
            event.setLong_desc(context.getString(R.string.received_sms_desc, address));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) || intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON"))
        {
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.boot));
            event.setShort_desc(context.getString(R.string.boot_completed));
            event.setLong_desc(context.getString(R.string.boot_completed_desc));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_TIME_CHANGED))
        {
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.warning));
            event.setType(EventType.getIntForType(EventType.time));
            event.setShort_desc(context.getString(R.string.time_changed));
            event.setLong_desc(context.getString(R.string.time_changed_desc));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_DATE_CHANGED))
        {
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.warning));
            event.setType(EventType.getIntForType(EventType.date));
            event.setShort_desc(context.getString(R.string.date_changed));
            event.setLong_desc(context.getString(R.string.date_changed_desc));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED))
        {
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.warning));
            event.setType(EventType.getIntForType(EventType.time));
            event.setShort_desc(context.getString(R.string.timezone_changed));
            event.setLong_desc(context.getString(R.string.timezone_changed_desc, extras != null ? extras.getString("time-zone") : "n/a"));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_AIRPLANE_MODE_CHANGED))
        {
            boolean isEnabled = extras != null && extras.getBoolean("state");
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.airplane));
            event.setShort_desc(context.getString(R.string.airplane_mode_toggled, isEnabled ? "green" : "red", context.getString(isEnabled ? R.string.enabled_upper : R.string.disabled_upper)));
            event.setLong_desc(context.getString(R.string.airplane_mode_desc, context.getString(isEnabled ? R.string.enabled_lower : R.string.disabled_lower)));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_WALLPAPER_CHANGED))
        {
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.wallpaper));
            event.setShort_desc(context.getString(R.string.wallpaper_changed));
            event.setLong_desc(context.getString(R.string.wallpaper_changed_desc));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(LocationManager.PROVIDERS_CHANGED_ACTION))
        {
            final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            boolean gpsEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.gps));
            event.setShort_desc(context.getString(R.string.gps_state_changed, gpsEnabled ? "green" : "red", context.getString(gpsEnabled ? R.string.enabled_upper : R.string.disabled_upper)));
            event.setLong_desc(context.getString(R.string.gps_state_changed_desc, context.getString(gpsEnabled ? R.string.enabled_lower : R.string.disabled_lower)));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION"))
        {
            /*android.media.EXTRA_VOLUME_STREAM_TYPE=2*/
            /*android.media.EXTRA_PREV_VOLUME_STREAM_VALUE=6*/
            /*android.media.EXTRA_VOLUME_STREAM_VALUE=7*/
            /*2=ring, 3=media*/
            if(extras != null)
            {
                int prev = extras.getInt("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE");
                int now = extras.getInt("android.media.EXTRA_VOLUME_STREAM_VALUE");
                if(prev == now)return;
            }
            String prevVolume = extras != null ? "" + extras.getInt("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE") : context.getString(R.string.na).toUpperCase();
            String newVolume = extras != null ? "" + extras.getInt("android.media.EXTRA_VOLUME_STREAM_VALUE") : context.getString(R.string.na).toUpperCase();
            int streamType = extras != null ? extras.getInt("android.media.EXTRA_VOLUME_STREAM_TYPE") : -1;
            String type = "";
            if(streamType == 2) type = context.getString(R.string.ringer);
            else if(streamType == 3) type = context.getString(R.string.media);
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.volume));
            event.setShort_desc(context.getString(R.string.volume_changed, newVolume));
            event.setLong_desc(context.getString(R.string.volume_changed_desc, type, prevVolume, newVolume));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL))
        {
            /*android.intent.extra.PHONE_NUMBER=2222*/
            String phoneNum = extras != null ? extras.getString(Intent.EXTRA_PHONE_NUMBER) : context.getString(R.string.na).toUpperCase();
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.call));
            event.setShort_desc(context.getString(R.string.outgoing_call, phoneNum));
            event.setLong_desc(context.getString(R.string.outgoing_call_desc, phoneNum));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED))
        {
            if(extras != null && !TelephonyManager.EXTRA_STATE_RINGING.equals(extras.getString(TelephonyManager.EXTRA_STATE)))
            {
                return;
            }
            String phoneNum = extras != null ? extras.getString(TelephonyManager.EXTRA_INCOMING_NUMBER) : context.getString(R.string.na).toUpperCase();
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.call));
            event.setShort_desc(context.getString(R.string.incoming_call, phoneNum));
            event.setLong_desc(context.getString(R.string.incoming_call_desc));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_CONFIGURATION_CHANGED))
        {
            boolean landscape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            String newOrientation = landscape ? context.getString(R.string.landscape) : context.getString(R.string.portrait);
            String oldOrientation = !landscape ? context.getString(R.string.landscape) : context.getString(R.string.portrait);
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.orientation));
            event.setShort_desc(context.getString(R.string.orientation_changed, newOrientation.toUpperCase()));
            event.setLong_desc(context.getString(R.string.orientation_changed_desc, oldOrientation, newOrientation));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_REBOOT))
        {
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.boot));
            event.setShort_desc(context.getString(R.string.device_reboot));
            event.setLong_desc(context.getString(R.string.reboot_desc));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_SHUTDOWN))
        {
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.boot));
            event.setShort_desc(context.getString(R.string.device_shutdown));
            event.setLong_desc(context.getString(R.string.shutdown_desc));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_POWER_CONNECTED))
        {
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.usb));
            event.setShort_desc(context.getString(R.string.power_connected));
            event.setLong_desc(context.getString(R.string.power_connected_desc));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED))
        {
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.usb));
            event.setShort_desc(context.getString(R.string.power_disconnected));
            event.setLong_desc(context.getString(R.string.power_disconnected_desc));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED) && false)//TODO add option to log all battery options
        {
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.battery));
            if(extras != null)
            {
                String sState = context.getString(R.string.unknown).toUpperCase();
                String sColor = "red";
                int state = extras.getInt(BatteryManager.EXTRA_STATUS);
                switch (state)
                {
                    case BatteryManager.BATTERY_STATUS_CHARGING:
                        sState = context.getString(R.string.charging).toUpperCase();
                        sColor = "green";
                        break;
                    case BatteryManager.BATTERY_STATUS_DISCHARGING:
                        sState = context.getString(R.string.discharging).toUpperCase();
                        sColor = "yellow";
                        break;
                    case BatteryManager.BATTERY_STATUS_FULL:
                        sState = context.getString(R.string.full).toUpperCase();
                        sColor = "blue";
                        break;
                    case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                        sState = context.getString(R.string.not_charging).toUpperCase();
                        sColor = "red";
                        break;
                    case BatteryManager.BATTERY_STATUS_UNKNOWN:
                        sState = context.getString(R.string.unknown).toUpperCase();
                        sColor = "red";
                        break;
                }
                int level = extras.getInt(BatteryManager.EXTRA_LEVEL);
                String batteryHealth = context.getString(R.string.good).toUpperCase();
                int health = extras.getInt(BatteryManager.EXTRA_HEALTH);
                switch (health)
                {
                    case BatteryManager.BATTERY_HEALTH_COLD:
                        batteryHealth = context.getString(R.string.cold).toUpperCase();
                        break;
                    case BatteryManager.BATTERY_HEALTH_DEAD:
                        batteryHealth = context.getString(R.string.dead).toUpperCase();
                        break;
                    case BatteryManager.BATTERY_HEALTH_GOOD:
                        batteryHealth = context.getString(R.string.good).toUpperCase();
                        break;
                    case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                        batteryHealth = context.getString(R.string.over_voltage).toUpperCase();
                        break;
                    case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                        batteryHealth = context.getString(R.string.overheat).toUpperCase();
                        break;
                    case BatteryManager.BATTERY_HEALTH_UNKNOWN:
                        batteryHealth = context.getString(R.string.unknown).toUpperCase();
                        break;
                    case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                        batteryHealth = context.getString(R.string.unspecified_failure).toUpperCase();
                        break;
                }
                event.setShort_desc(context.getString(R.string.battery_state_changed, sColor, sState));
                event.setLong_desc(context.getString(R.string.battery_state_desc, sState, level, batteryHealth,
                        extras.getString(BatteryManager.EXTRA_TECHNOLOGY), extras.getString(BatteryManager.EXTRA_TEMPERATURE), extras.getString(BatteryManager.EXTRA_VOLTAGE)));
            }
            else
            {
                event.setShort_desc(context.getString(R.string.battery_state_changed, "red", context.getString(R.string.na).toUpperCase()));
                event.setLong_desc(context.getString(R.string.battery_state_desc, context.getString(R.string.na).toUpperCase(),
                        context.getString(R.string.na).toUpperCase(), context.getString(R.string.na).toUpperCase(),
                        context.getString(R.string.na).toUpperCase(), context.getString(R.string.na).toUpperCase(),
                        context.getString(R.string.na).toUpperCase()));
            }

            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_BATTERY_LOW))
        {
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.warning));
            event.setType(EventType.getIntForType(EventType.battery));
            event.setShort_desc(context.getString(R.string.battery_low));
            event.setLong_desc(context.getString(R.string.battery_low_desc));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_BATTERY_OKAY))
        {
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.ok));
            event.setType(EventType.getIntForType(EventType.battery));
            event.setShort_desc(context.getString(R.string.battery_ok));
            event.setLong_desc(context.getString(R.string.battery_ok_desc));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_LOCALE_CHANGED))
        {
            String locale = Locale.getDefault().getDisplayLanguage();
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.locale));
            event.setShort_desc(context.getString(R.string.locale_changed, locale));
            event.setLong_desc(context.getString(R.string.locale_changed_desc, locale));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
        {
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.screen));
            event.setShort_desc(context.getString(R.string.screen_off));
            event.setLong_desc(context.getString(R.string.screen_off_desc));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_SCREEN_ON))
        {
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.screen));
            event.setShort_desc(context.getString(R.string.screen_on));
            event.setLong_desc(context.getString(R.string.screen_on_desc));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_USER_PRESENT))
        {
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.screen));
            event.setShort_desc(context.getString(R.string.screen_unlocked));
            event.setLong_desc(context.getString(R.string.screen_unlocked_desc));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_HEADSET_PLUG))
        {
            int state = extras != null ? extras.getInt("state") : -1;//1=plug, 0=unplug
            String name = extras != null ? extras.getString("name") : context.getString(R.string.na).toUpperCase();
            int hasMic = extras != null ? extras.getInt("microphone") : -1;
            String sHasMic = context.getString(R.string.na);
            if(hasMic == 1)
            {
                sHasMic = context.getString(R.string.yes);
            }
            else if(hasMic == 2)
            {
                sHasMic = context.getString(R.string.no);
            }
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.headset));
            if(state == 1)
            {
                event.setShort_desc(context.getString(R.string.headset_plugged, name));
                event.setLong_desc(context.getString(R.string.headset_plugged_desc, name, sHasMic));
            }
            else if(state == 0)
            {
                event.setShort_desc(context.getString(R.string.headset_unplugged, name));
                event.setLong_desc(context.getString(R.string.headset_unplugged_desc, name, sHasMic));
            }
            else
            {
                event.setShort_desc(context.getString(R.string.headset_status_changed));
                event.setLong_desc(context.getString(R.string.headset_unknown_desc));
            }

            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_MEDIA_SCANNER_STARTED))
        {
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.media));
            event.setShort_desc(context.getString(R.string.scanner_started));
            event.setLong_desc(context.getString(R.string.scanner_started_desc));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_MEDIA_SCANNER_FINISHED))
        {
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.info));
            event.setType(EventType.getIntForType(EventType.media));
            event.setShort_desc(context.getString(R.string.scanner_finished));
            event.setLong_desc(context.getString(R.string.scanner_finished_desc));
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED))
        {
            boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            String pn = intent.getData().toString().split(":")[1];
            if(!replacing)
            {
                Event event = new Event();
                event.setTimestamp(new Date());
                event.setLevel(EventLevel.getIntForLevel(EventLevel.warning));
                event.setType(EventType.getIntForType(EventType.app));
                event.setShort_desc(context.getString(R.string.app_removed));
                event.setLong_desc(context.getString(R.string.app_removed_desc, pn));
                EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
                eventDao.insert(event);
                sendLocalBroadcast(event);
            }
        }
        if(intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED))
        {
            boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            String pn = intent.getData().toString().split(":")[1];
            if(!replacing)
            {
                String appName = Utility.getNameForPackage(context, pn);
                Event event = new Event();
                event.setTimestamp(new Date());
                event.setLevel(EventLevel.getIntForLevel(EventLevel.warning));
                event.setType(EventType.getIntForType(EventType.app));
                event.setShort_desc(context.getString(R.string.app_installed, appName));
                event.setLong_desc(context.getString(R.string.app_installed_desc, appName, pn));
                Icon icon = new Icon();
                icon.setIcon(Utility.getApplicationIcon(context, pn));
                long iconId = App.getInstance().getDaoSession().getIconDao().insert(icon);
                event.setIcon_id(iconId);
                EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
                eventDao.insert(event);
                sendLocalBroadcast(event);
            }
        }
        if(intent.getAction().equals(Intent.ACTION_PACKAGE_DATA_CLEARED))
        {
            String pn = intent.getData().toString().split(":")[1];
            String appName = Utility.getNameForPackage(context, pn);
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.warning));
            event.setType(EventType.getIntForType(EventType.app));
            event.setShort_desc(context.getString(R.string.app_data_cleared, appName));
            event.setLong_desc(context.getString(R.string.app_data_cleared_desc, appName, pn));
            Icon icon = new Icon();
            icon.setIcon(Utility.getApplicationIcon(context, pn));
            long iconId = App.getInstance().getDaoSession().getIconDao().insert(icon);
            event.setIcon_id(iconId);
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_PACKAGE_RESTARTED))
        {
            String pn = intent.getData().toString().split(":")[1];
            String appName = Utility.getNameForPackage(context, pn);
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.warning));
            event.setType(EventType.getIntForType(EventType.app));
            event.setShort_desc(context.getString(R.string.app_killed, appName));
            event.setLong_desc(context.getString(R.string.app_killed_desc, appName, pn));
            Icon icon = new Icon();
            icon.setIcon(Utility.getApplicationIcon(context, pn));
            long iconId = App.getInstance().getDaoSession().getIconDao().insert(icon);
            event.setIcon_id(iconId);
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
        if(intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED))
        {
            String pn = intent.getData().toString().split(":")[1];
            String appName = Utility.getNameForPackage(context, pn);
            Event event = new Event();
            event.setTimestamp(new Date());
            event.setLevel(EventLevel.getIntForLevel(EventLevel.warning));
            event.setType(EventType.getIntForType(EventType.app));
            event.setShort_desc(context.getString(R.string.app_reinstalled, appName));
            event.setLong_desc(context.getString(R.string.app_reinstalled_desc, appName, pn));
            Icon icon = new Icon();
            icon.setIcon(Utility.getApplicationIcon(context, pn));
            long iconId = App.getInstance().getDaoSession().getIconDao().insert(icon);
            event.setIcon_id(iconId);
            EventDao eventDao = App.getInstance().getDaoSession().getEventDao();
            eventDao.insert(event);
            sendLocalBroadcast(event);
        }
    }
}
