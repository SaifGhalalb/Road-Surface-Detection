package com.example.roaddetectionapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private static final String TAG = "MainActivity";
    private SensorManager sensorManager;
    Sensor accelermeter, gyro;
    TextView mainText,persntage;
    StringBuilder accData = new StringBuilder();
    String temp = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mainText = (TextView) findViewById(R.id.mainText);
        persntage = (TextView) findViewById(R.id.presntage);


        RelativeLayout constraintLayout = findViewById(R.id.layout);
        AnimationDrawable animationDrawable = (AnimationDrawable) constraintLayout.getBackground();
        animationDrawable.setEnterFadeDuration(1000);
        animationDrawable.setExitFadeDuration(4000);
        animationDrawable.start();

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        sensorManager =(SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelermeter =sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyro=sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(MainActivity.this,accelermeter,sensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(MainActivity.this,gyro,sensorManager.SENSOR_DELAY_UI);


    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER) {

            predict(event.values[0],event.values[1],event.values[2],"acc");

        }

        if (event.sensor.getType()==Sensor.TYPE_GYROSCOPE) {

            predict(event.values[0],event.values[1],event.values[2],"gyro");

        }
        }


    String tempData ="";

    ArrayList<Double> yAccArr = new ArrayList<Double>();
    ArrayList<Double> xAccArr = new ArrayList<Double>();
    ArrayList<Double> zAccArr = new ArrayList<Double>();
    ArrayList<Double> xGyroArr = new ArrayList<Double>();
    ArrayList<Double> zGyroArr = new ArrayList<Double>();
    //ArrayList<Double> xMagnArr = new ArrayList<Double>();
    // ArrayList<Double> yMagnArr = new ArrayList<Double>();
    // ArrayList<Double> zMagnArr = new ArrayList<Double>();
    private int numOfRecord =30;

    private boolean done1=false,done2=false,done3=false;

    double sum_abs_Acc,sum_abs_Gyro;

    double avg_xAcc,avg_yAcc,avg_zAcc,avg_xGyro,avg_zGyro;

    double var_xAcc,var_yAcc,var_zAcc,var_xGyro,var_zGyro;

    double sD_xAcc,sD_yAcc,sD_zAcc,sD_xGyro,sD_zGyro;

    double min_xAcc=0,min_yAcc=0,min_zAcc=0,min_xGyro=0,min_zGyro=0;
    double max_xAcc=0,max_yAcc=0,max_zAcc=0,max_xGyro=0,max_zGyro=0;

    double sumAbs_MinMax_xAcc,sumAbs_MinMax_yAcc,sumAbs_MinMax_zAcc
            ,sumAbs_MinMax_xGyro,sumAbs_MinMax_zGyro;

    public void predict(double x, double y, double z, String type){

        //For the accelerometer ---------------------
        if (type.equals("acc") && !done1){
            done1=true;
            // adding Acc values to the array list
            xAccArr.add(x);
            yAccArr.add(y);
            zAccArr.add(z);
            // calculating the sum of absolute values for Acc
            sum_abs_Acc+=Math.abs(x)+Math.abs(y)+Math.abs(z);

        }


        //For the GyroScope-------------------
        if (type.equals("gyro") && done1 && !done2){
            done2=true;
            // adding Gyro values to the array list
            xGyroArr.add(x);
            zGyroArr.add(z);
            // calculating the sum of absolute values for Gyro
            sum_abs_Gyro+=Math.abs(x)+Math.abs(z);

        }


        //Collect every N record-------------------
        //Every 15 Sensors record are collected in One second
        if (done1 && done2  && numOfRecord==0) {

            // calculating data after collected
            calculateData(31);

            //clearing data to start a new record
            clearData();
            done3=true;
        }


        // counting For the N Sensors record---------------
        if (done1 && done2  ) {
            numOfRecord--;
            done2=done1=false;

        }


        // Call SMV to predict data after the N record got collected and calculated-----------
        if (done3 ){
            //Calling the SMV and show data to screen
            String response =sendToSVM(tempData);
            String [] dataOut=response.split("@");
            String roadType=dataOut[0];
            Float confidence=Float.parseFloat(dataOut[1]);
            mainText.setText(roadType);
            persntage.setText("Confidence: "+confidence+"%");

            if(roadType.contains("Bumpy")){ mainText.setTextColor(Color.RED); }
            else mainText.setTextColor(Color.BLACK);

            //clear the current record to start a new one
            tempData="";
            done2=done1=done3=false;

        }

    }


    /**
     * Call the SMV in python file and return predictions
     * @param sensorData The Sensors Data that get collected earlier
     * @return the predictions withe probability(Confidence)
     */
    public String sendToSVM(String sensorData){
        Date date = new Date();
        long timeMilli = date.getTime();

        Python py = Python.getInstance();
        PyObject pyobj = py.getModule("pyCodeApi");
        PyObject smvData = pyobj.callAttr("callSVM",sensorData);


        Date date2 = new Date();
        long timeMilli2 = date2.getTime();
        Log.d(TAG, "Time taken for one predict: " + (timeMilli2 - timeMilli));

        return (smvData.toString());
    }

    /**
     * Calculate the variance
     * @param list ArrayList to be calculated
     * @param average the Average of that ArrayList
     * @return the variance
     */
    public static double variance(ArrayList<Double> list ,double average) {
        double sumDiffsSquared = 0.0;
        double avg = average;
        for (Double value : list)
        {
            double diff = value - avg;
            diff *= diff;
            sumDiffsSquared += diff;
        }
        return sumDiffsSquared  / (list.size());
    }

    /**
     * Calculate the Average
     * @param num ArrayList to be calculated
     * @return the Average of that ArrayList
     */
    private double Average(ArrayList <Double> num) {
        double sum = 0;
        for (int i = 0; i < num.size(); i++) { sum+=num.get(i); }
        return sum/num.size();
    }

    public void calculateData(int recordsNumber){
    //calculating the average
    avg_xAcc=Average(xAccArr);
    avg_yAcc=Average(yAccArr);
    avg_zAcc=Average(zAccArr);
    avg_xGyro=Average(xGyroArr);
    avg_zGyro=Average(zGyroArr);

    //calculating the variance
    var_xAcc= variance(xAccArr,avg_xAcc);
    var_yAcc= variance(yAccArr,avg_yAcc);
    var_zAcc= variance(zAccArr,avg_zAcc);
    var_xGyro=variance(xGyroArr,avg_xGyro);
    var_zGyro=variance(zGyroArr,avg_zGyro);


    //calculating the Stander deviation
    sD_xAcc=Math.sqrt(var_xAcc);
    sD_yAcc=Math.sqrt(var_yAcc);
    sD_zAcc=Math.sqrt(var_zAcc);
    sD_xGyro=Math.sqrt(var_xGyro);
    sD_zGyro=Math.sqrt(var_zGyro);


    //calculating the min
    min_xAcc= Collections.min(xAccArr);
    min_yAcc= Collections.min(yAccArr);
    min_zAcc= Collections.min(zAccArr);
    min_xGyro=Collections.min(xGyroArr);
    min_zGyro=Collections.min(zGyroArr);

    //calculating the max
    max_xAcc=Collections.max(xAccArr);
    max_yAcc=Collections.max(yAccArr);
    max_zAcc=Collections.max(zAccArr);
    max_xGyro=Collections.max(xGyroArr);
    max_zGyro=Collections.max(zGyroArr);

    // calculating the absolute sum of min and max
    sumAbs_MinMax_xAcc=Math.abs(min_xAcc)+Math.abs(max_xAcc);
    sumAbs_MinMax_yAcc=Math.abs(min_yAcc)+Math.abs(max_yAcc);
    sumAbs_MinMax_zAcc=Math.abs(min_zAcc)+Math.abs(max_zAcc);
    sumAbs_MinMax_xGyro=Math.abs(min_xGyro)+Math.abs(max_xGyro);
    sumAbs_MinMax_zGyro=Math.abs(min_zGyro)+Math.abs(max_zGyro);

    //assemble date
    tempData+=avg_xAcc+","+avg_yAcc+","+avg_zAcc+","
            +avg_xGyro+","+avg_zGyro+","
            +sD_xAcc+","+sD_yAcc+","+sD_zAcc+","
            +sD_xGyro+","+sD_zGyro+","
            +var_xAcc+","+var_yAcc+","+var_zAcc+","
            +var_xGyro+","+var_zGyro+","
            +sum_abs_Acc+","+sum_abs_Gyro+","
            +sumAbs_MinMax_xAcc+","+sumAbs_MinMax_yAcc+","+sumAbs_MinMax_zAcc+","
            +sumAbs_MinMax_xGyro+","+sumAbs_MinMax_zGyro+","
            +min_xAcc+","+min_yAcc+","+min_zAcc+","
            +min_xGyro+","+min_zGyro+","
            +max_xAcc+","+max_yAcc+","+max_zAcc+","
            +max_xGyro+","+max_zGyro;

    numOfRecord=recordsNumber;
}




public void clearData(){

    xAccArr.clear();
    yAccArr.clear();
    zAccArr.clear();
    xGyroArr.clear();
    zGyroArr.clear();

    avg_xAcc=avg_yAcc=avg_zAcc=avg_xGyro=avg_zGyro=0;

    sumAbs_MinMax_xAcc=0;
    sumAbs_MinMax_yAcc=0;
    sumAbs_MinMax_zAcc=0;

    sumAbs_MinMax_xGyro=0;
    sumAbs_MinMax_zGyro=0;

    min_xAcc=min_yAcc=min_zAcc=min_xGyro=min_zGyro=0;
    max_xAcc=max_yAcc=max_zAcc=max_xGyro=max_zGyro=0;

    sum_abs_Gyro=sum_abs_Acc=0;

    var_xAcc= var_yAcc =var_zAcc= var_xGyro= var_zGyro= sD_xAcc= sD_yAcc= sD_zAcc= sD_xGyro= sD_zGyro=0;



}










    //end
    }
