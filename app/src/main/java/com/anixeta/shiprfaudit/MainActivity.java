package com.anixeta.shiprfaudit;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.*;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

public class MainActivity extends Activity {
    static final int LOCATION_REQUEST = 41, CSV_REQUEST = 42;
    final int BG=0xff0b1525, PANEL=0xff1c2b40, WHITE=0xfff5f8fd, SUB=0xffbacbd9, TEAL=0xff6ae2cb, AMBER=0xffffbd70;
    WifiManager wifi;
    LinearLayout root, results, history;
    TextView status, summary;
    EditText area;
    android.content.SharedPreferences prefs;
    final List<ScanResult> scans=new ArrayList<>();
    final Handler handler=new Handler(Looper.getMainLooper());
    boolean receiverAdded=false;
    final BroadcastReceiver receiver=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent i){refreshScan("Wi-Fi scan completed (results may be cached)");}
    };

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs=getSharedPreferences("audit",MODE_PRIVATE);
        wifi=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(18),dp(22),dp(18),dp(35));scroll.addView(root);setContentView(scroll);
        root.addView(text("AUTHORIZED WI-FI SURVEY • OFFLINE",11,TEAL,true));
        root.addView(text("Ship RF Audit",30,WHITE,true));
        root.addView(text("Scan nearby access points and record observations.",14,SUB,false));
        gap(root,15);
        LinearLayout intro=card();root.addView(intro);
        status=text("Ready to scan",15,WHITE,true);intro.addView(status);
        gap(intro,7);
        intro.addView(text("SSID matches are clues only. This app cannot identify a device owner or detect Starlink when its Wi-Fi is off.",12,SUB,false));
        gap(intro,12);
        Button scan=button("SCAN NEARBY WI-FI",true);intro.addView(scan);
        scan.setOnClickListener(v->startScan());
        gap(intro,6);
        Button settings=button("Open Location settings",false);intro.addView(settings);
        settings.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
        LinearLayout survey=card();root.addView(survey);
        survey.addView(text("SURVEY DECK / AREA",12,TEAL,true));gap(survey,8);
        area=new EditText(this);area.setSingleLine(true);area.setTextColor(WHITE);area.setHintTextColor(SUB);
        area.setHint("Example: Deck 2 corridor");area.setText(prefs.getString("area",""));survey.addView(area);
        LinearLayout box=card();root.addView(box);
        box.addView(text("DETECTED ACCESS POINTS",12,TEAL,true));
        summary=text("No scan yet",13,SUB,false);box.addView(summary);gap(box,10);
        results=new LinearLayout(this);results.setOrientation(1);box.addView(results);
        LinearLayout stored=card();root.addView(stored);
        stored.addView(text("SAVED OBSERVATIONS",12,TEAL,true));gap(stored,10);
        Button export=button("EXPORT CSV",true);stored.addView(export);export.setOnClickListener(v->exportCsv());
        Button clear=button("Clear saved readings",false);stored.addView(clear);
        clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Clear saved observations?").setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{prefs.edit().remove("records").apply();showHistory();}).show());
        history=new LinearLayout(this);history.setOrientation(1);stored.addView(history);
        root.addView(text("Use only with approval from your ship's IT and Security management. Readings remain on this device unless exported.",12,SUB,false));
        showHistory();
        IntentFilter filter=new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,filter,Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(receiver,filter);
        receiverAdded=true;
    }
    int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    TextView text(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(dp(2),dp(4),dp(2),dp(4));
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    void gap(LinearLayout c,int h){View v=new View(this);c.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    LinearLayout card(){
        LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(dp(14),dp(14),dp(14),dp(14));
        GradientDrawable bg=new GradientDrawable();bg.setColor(PANEL);bg.setCornerRadius(dp(14));c.setBackground(bg);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(13);c.setLayoutParams(p);return c;
    }
    Button button(String label,boolean highlighted){
        Button b=new Button(this);b.setAllCaps(false);b.setText(label);b.setTextSize(13);
        b.setTextColor(highlighted?BG:WHITE);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        GradientDrawable bg=new GradientDrawable();bg.setColor(highlighted?TEAL:BG);bg.setCornerRadius(dp(10));b.setBackground(bg);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.topMargin=dp(5);b.setLayoutParams(p);return b;
    }
    boolean permitted(){
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED &&
            (Build.VERSION.SDK_INT<33||checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES)==PackageManager.PERMISSION_GRANTED);
    }
    boolean locationOn(){
        LocationManager l=(LocationManager)getSystemService(LOCATION_SERVICE);return l!=null&&l.isLocationEnabled();
    }
    void startScan(){
        if(!permitted()){
            if(Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.NEARBY_WIFI_DEVICES},LOCATION_REQUEST);
            else requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST);
            status.setText("Grant scan permissions and try again.");return;
        }
        if(!locationOn()){status.setText("Enable Android Location, then tap Scan.");return;}
        if(wifi==null||!wifi.isWifiEnabled()){status.setText("Turn on Wi-Fi, then tap Scan.");return;}
        try{
            boolean ok=wifi.startScan();status.setText(ok?"Scanning nearby Wi-Fi…":"Scan throttled; showing available cached readings");
            if(!ok)refreshScan("Cached scan results");else handler.postDelayed(()->refreshScan("Latest available Wi-Fi scan"),4000);
        }catch(SecurityException ex){status.setText("Android blocked Wi-Fi scanning permission.");}
    }
    @Override public void onRequestPermissionsResult(int code,String[] perms,int[] grants){
        super.onRequestPermissionsResult(code,perms,grants);if(code==LOCATION_REQUEST&&permitted())startScan();
    }
    void refreshScan(String message){
        if(!permitted()||!locationOn())return;
        try{
            scans.clear();scans.addAll(wifi.getScanResults());
            Collections.sort(scans,(a,b)->Integer.compare(b.level,a.level));
            status.setText(message);showScans();
        }catch(Exception ex){status.setText("Unable to read Wi-Fi results: "+ex.getClass().getSimpleName());}
    }
    boolean match(String ssid){return ssid!=null&&ssid.toLowerCase(Locale.ROOT).contains("starlink");}
    String name(String ssid){return ssid==null||ssid.trim().isEmpty()?"(hidden SSID)":ssid;}
    String band(int f){return f>=5925?"6 GHz":f>=4900?"5 GHz":"2.4 GHz";}
    void showScans(){
        results.removeAllViews();int matches=0;for(ScanResult s:scans)if(match(s.SSID))matches++;
        summary.setText(scans.size()+" access points • "+matches+" Starlink name matches");
        if(scans.isEmpty()){results.addView(text("No nearby networks returned. Enable Wi-Fi, Location and permissions; try again.",13,SUB,false));return;}
        for(ScanResult s:scans){
            boolean m=match(s.SSID);
            LinearLayout entry=new LinearLayout(this);entry.setOrientation(1);entry.setPadding(dp(9),dp(8),dp(9),dp(8));
            results.addView(entry);
            entry.addView(text(name(s.SSID)+(m?"  [NAME MATCH]":""),16,m?AMBER:WHITE,true));
            entry.addView(text("BSSID: "+s.BSSID,12,SUB,false));
            entry.addView(text(s.level+" dBm  •  "+band(s.frequency)+"  •  "+s.frequency+" MHz",12,SUB,false));
            Button save=button("Save this reading",false);entry.addView(save);save.setOnClickListener(v->save(s));
            gap(entry,7);
        }
    }
    JSONArray saved(){
        try{return new JSONArray(prefs.getString("records","[]"));}catch(Exception e){return new JSONArray();}
    }
    void save(ScanResult s){
        String a=area.getText().toString().trim();
        if(a.isEmpty()){area.setError("Enter deck / survey area");area.requestFocus();return;}
        prefs.edit().putString("area",a).apply();JSONArray data=saved();JSONObject item=new JSONObject();
        try{
            item.put("time",new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).format(new Date()));
            item.put("area",a);item.put("ssid",name(s.SSID));item.put("bssid",s.BSSID);
            item.put("rssi",s.level);item.put("mhz",s.frequency);item.put("starlink_name_match",match(s.SSID));
            if(data.length()>=500){JSONArray trimmed=new JSONArray();for(int i=1;i<data.length();i++)trimmed.put(data.getJSONObject(i));data=trimmed;}
            data.put(item);prefs.edit().putString("records",data.toString()).apply();
            Toast.makeText(this,"Reading saved locally",Toast.LENGTH_SHORT).show();showHistory();
        }catch(Exception ex){Toast.makeText(this,"Unable to save reading",Toast.LENGTH_SHORT).show();}
    }
    void showHistory(){
        history.removeAllViews();JSONArray data=saved();
        history.addView(text(data.length()+" observations stored locally",13,SUB,false));
        for(int i=data.length()-1;i>=Math.max(0,data.length()-5);i--){
            JSONObject o=data.optJSONObject(i);if(o!=null)history.addView(text(o.optString("time")+"  "+o.optString("area")+"\n"+o.optString("ssid")+"  "+o.optInt("rssi")+" dBm",12,WHITE,false));
        }
    }
    static String quote(String s){return "\""+(s==null?"":s.replace("\"","\"\""))+"\"";}
    String csv(){
        StringBuilder b=new StringBuilder("timestamp,area,ssid,bssid,rssi_dbm,frequency_mhz,starlink_name_match\r\n");
        JSONArray data=saved();for(int i=0;i<data.length();i++){
            JSONObject o=data.optJSONObject(i);if(o==null)continue;
            b.append(quote(o.optString("time"))).append(',').append(quote(o.optString("area"))).append(',')
            .append(quote(o.optString("ssid"))).append(',').append(quote(o.optString("bssid"))).append(',')
            .append(o.optInt("rssi")).append(',').append(o.optInt("mhz")).append(',').append(o.optBoolean("starlink_name_match")).append("\r\n");
        }return b.toString();
    }
    void exportCsv(){
        if(saved().length()==0){Toast.makeText(this,"Save a reading first",Toast.LENGTH_SHORT).show();return;}
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("text/csv");i.addCategory(Intent.CATEGORY_OPENABLE);
        i.putExtra(Intent.EXTRA_TITLE,"ShipRFAudit-"+new SimpleDateFormat("yyyyMMdd-HHmm",Locale.US).format(new Date())+".csv");
        startActivityForResult(i,CSV_REQUEST);
    }
    @Override protected void onActivityResult(int code,int result,Intent data){
        super.onActivityResult(code,result,data);
        if(code!=CSV_REQUEST||result!=RESULT_OK||data==null||data.getData()==null)return;
        try(OutputStream out=getContentResolver().openOutputStream(data.getData())){
            out.write(csv().getBytes(StandardCharsets.UTF_8));out.flush();
            Toast.makeText(this,"CSV exported",Toast.LENGTH_LONG).show();
        }catch(Exception e){Toast.makeText(this,"Export failed",Toast.LENGTH_LONG).show();}
    }
    @Override protected void onDestroy(){super.onDestroy();handler.removeCallbacksAndMessages(null);if(receiverAdded)unregisterReceiver(receiver);}
}
