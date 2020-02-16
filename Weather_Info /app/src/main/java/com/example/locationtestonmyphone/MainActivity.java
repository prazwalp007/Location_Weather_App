package com.example.locationtestonmyphone;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.locationtestonmyphone.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private TextView mTextViewResult;
    //Variables to hold present longitude and latitude of user
    double longitude;
    double latitude;
    String temperature;
    String humidity;
    String windSpeed;
    String precipProbability;

    String [] temperatureforHours = new String [5];
    String [] weekTempHigh = new String [7];
    String [] weekTempLow = new String [7];


    double averageTempfor48hrs;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        requestPermission();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //funtion to get lastlocation of user
        fusedLocationClient.getLastLocation().addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();

                }
            }
        });


        //send http request and get JSON data
        Button button = findViewById(R.id.getlocation);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){

                OkHttpClient client = new OkHttpClient();
                String url = "https://api.darksky.net/forecast/9dd6d2102ca8e1516b1a774b979baf8f/" + Double.toString(latitude) +"," + Double.toString(longitude);
                Request request = new Request.Builder()
                        .url(url)
                        .build();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()){
                            final String myResponse = response.body().string();
                            //Use Json Parser
                            try {
                                JSONObject obj1 = new JSONObject(myResponse);
                                JSONObject obj2 = (JSONObject) obj1.get("currently");
                                JSONObject obj3 = (JSONObject) obj1.get("hourly");

                                //for general info
                                temperature = obj2.get("temperature").toString();
                                humidity = obj2.get("humidity").toString();
                                windSpeed = obj2.get("windSpeed").toString();
                                precipProbability = obj2.get("precipProbability").toString();


                                //for hourly temp for next 5 hrs
                                JSONArray arrayTempforHours1 = (JSONArray) obj3.get("data");
                                for (int i = 0; i < 5 ; i++ ){
                                    JSONObject obj4 = (JSONObject) arrayTempforHours1.get(i);
                                    temperatureforHours[i] = obj4.get("temperature").toString();
                                }

                                //temp for next 48 hrs

                                JSONArray arrayTempforHours2 =  (JSONArray) obj3.get("data");
                                double num = 0;
                                for (int i = 0; i < 48; i++){
                                    JSONObject obj5 = (JSONObject) arrayTempforHours2.get(i);
                                    String temp = obj5.get("temperature").toString();
                                    averageTempfor48hrs += Double.valueOf(temp);
                                }

                                averageTempfor48hrs = averageTempfor48hrs/48.00;



                                //predicted temp for upcoming week
                                JSONObject obj6 = (JSONObject) obj1.get("daily");
                                JSONArray arrayTempforWeek = (JSONArray) obj6.get("data");
                                for (int i = 0; i < 7; i++){
                                    JSONObject obj7 = (JSONObject) arrayTempforWeek.get(i);
                                    weekTempHigh[i] = obj7.get("temperatureHigh").toString();
                                    weekTempLow[i] = obj7.get("temperatureLow").toString();
                                }


                                //show observed temp at present location at specified time


                            }catch (Exception e){}



                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //weather info
                                    TextView textView = findViewById(R.id.location);
                                    String output1 = "temperature = " + temperature + "\nhumidity = " + humidity + "\nwindSpeed = " + windSpeed + "\nprecipProbability = " + precipProbability;
                                    textView.setText(output1);

                                    //temp for next 5 hrs
                                    TextView textView1 = findViewById(R.id.tempforNext5hrs);
                                    String outputTemp = "";
                                    for (int i = 0; i < 5; i++){
                                        outputTemp = outputTemp + temperatureforHours[i] + " ";
                                    }
                                    String tempforNext5hrs = "Temp for next 5 hrs = " + outputTemp;
                                    textView1. setText(tempforNext5hrs);


                                    //average temp for next 48 hrs
                                    TextView textView2 = findViewById(R.id.avgtemp);
                                    String avgTempOutput = "Average temp for Next 48 hrs = " + String.format("%.2f", averageTempfor48hrs);
                                    textView2.setText(avgTempOutput);


                                    //temp for next 7 days
                                    TextView textView3 = findViewById(R.id.tempforweek);
                                    String weeklyTempHI = "";
                                    String weeklyTempLOW = "";
                                    for (int i = 0; i < 7 ; i++){
                                        weeklyTempHI = weeklyTempHI + weekTempHigh[i] +" ";
                                        weeklyTempLOW = weeklyTempLOW + weekTempLow[i] +" ";
                                    }
                                    weeklyTempHI = "High Temp for next week " + weeklyTempHI;
                                    weeklyTempLOW = "\nLow Temp for next week " + weeklyTempLOW;

                                    textView3.setText(weeklyTempHI+weeklyTempLOW);
                                }
                            });
                        }
                    }
                });
            }

        });

    }


    private void requestPermission(){
        ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, 1);
    }


}


