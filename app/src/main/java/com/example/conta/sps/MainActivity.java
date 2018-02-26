package com.example.conta.sps;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map;
import java.io.FileOutputStream;
import java.io.File;
import java.util.TreeMap;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


/**
 * Created by Bhavya Jain on 20 Feb 2018.
 */

public class MainActivity extends Activity implements OnClickListener, SensorEventListener {

    /**
     * The wifi manager.
     */
    private WifiManager wifiManager;

    private SensorManager sensorManager;

    private Sensor accelerometer;

    private WifiInfo wifiInfo;

    private ArrayList<AccelData> sensorData;

    /**
     * Accelerometer x value
     */
    private double x = 0;
    /**
     * Accelerometer y value
     */
    private double y = 0;
    /**
     * Accelerometer z value
     */
    private double z = 0;

    /**
     * The text view.
     */
    private TextView textRssi;
    /**
     * The button.
     */
    private Button buttonTrain;

    private Button buttonLocate;

    private Button buttonWalk;

    private Button buttonRecord;

    private Button buttonStop;

    private EditText CellNumber;

    private Map<String, Integer> Strength;

    private HashMap<String, Map<String, Integer>> CellData;

    private HashMap<String, Map<String, Integer>> CellDataMain;

    private HashMap<String, Map<String, List<Integer>>> locateData;

    private ArrayList<AccelData> trainedDataAcc;


    private void saveVectors(HashMap<String, Map<String, Integer>> data) {

        String filename = "trainingData.csv";
        File file = new File(getExternalFilesDir(null), filename);

        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(file);
            outputStream.write("Cell, Hotspot, Strength \n".getBytes());
            for (Map.Entry<String, Map<String, Integer>> entryCell : data.entrySet())
            {
                for (Map.Entry<String, Integer> entryStrength : entryCell.getValue().entrySet())
                {
                    String line = entryCell.getKey() + "," + entryStrength.getKey() + ", " + entryStrength.getValue() + "\n";
                    outputStream.write(line.getBytes());
                }
            }
            outputStream.close();
        } catch (Exception e) {
            System.out.println("Error in File Writing");
            e.printStackTrace();
        }
    }


    private ArrayList<AccelData> loadValuesAcc() {
        ArrayList<AccelData> tmp = new ArrayList<AccelData>();

        String filename = "trainedDataAcc.csv";
        File file = new File(getExternalFilesDir(null), filename);
        FileInputStream inputStream;

        try {

            inputStream = new FileInputStream(file);

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // y, z, 0 = standing OR 1 = walking
                String[] RowData = line.split(";");
                Long type = Long.parseLong(RowData[2]);
                //Double x = Double.parseDouble(RowData[0]);
                Double y = Double.parseDouble(RowData[0]);
                Double z = Double.parseDouble(RowData[1]);
                tmp.add(new AccelData(type, 0, y, z));

            }
        } catch (Exception e){
            e.printStackTrace();
        }

        return tmp;
    }

    private String kNN(AccelData point, ArrayList<AccelData> trainedData) {
        Map<Double, Long> distanceMap = new TreeMap<Double, Long>();
        for(AccelData trainedPoint: trainedData) {
            //Double dX = trainedPoint.getX() - point.getX();
            Double dY = trainedPoint.getY() - point.getY();
            Double dZ = trainedPoint.getZ() - point.getZ();
            Double d = Math.sqrt(Math.pow(dY, 2) + Math.pow(dZ, 2));
            distanceMap.put(d,trainedPoint.getTimestamp());
        }

        // get k nearest
        int countWalking = 0;
        int countStanding = 0;
        int k = 0;
        for(Map.Entry<Double, Long> kNN: distanceMap.entrySet()) {
            if(k > 3) {
                break;
            } else {
                k++;
            }

            if(kNN.getValue() == 0) {
                countStanding++;
            } else {
                countWalking++;
            }
        }

        if(countStanding > countWalking) {
            return "STANDING";
        } else {
            return "WALKING";
        }
    }

    private HashMap<String, Map<String, List<Integer>>> loadValues() {

        HashMap<String, Map<String, List<Integer>>> trainedData;
        HashMap<String, List<Integer>> trainedStrength;
        List<Integer> maxmin;


        trainedData = new HashMap<String, Map<String, List<Integer>>>();

        String filename = "trainedData2.csv";
        File file = new File(getExternalFilesDir(null), filename);
        FileInputStream inputStream;

        String hotspot;
        String cellName;
        int min;
        int max;

        try{

            inputStream = new FileInputStream(file);

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            try{

                String line;
                while ((line =reader.readLine()) != null){
                    trainedStrength  = new HashMap<String, List<Integer>>();
                    maxmin = new LinkedList<Integer>();
                    String[] RowData = line.split(";");
                    hotspot = RowData[0];
                    cellName = RowData[1];
                    max = Integer.parseInt(RowData[2]);
                    min = Integer.parseInt(RowData[3]);
                    maxmin.add(max);
                    maxmin.add(min);
                    trainedStrength.put(cellName, maxmin);
                    if(trainedData.containsKey(hotspot)){
                        trainedData.get(hotspot).put(cellName, maxmin);
                    }
                    else {
                        trainedData.put(hotspot, trainedStrength);

                    }

                }


            }   catch (Exception e){
                e.printStackTrace();
            }

            inputStream.close();


        }   catch (Exception e){
            System.out.println("Error in File Reading");
            e.printStackTrace();
        }



        return trainedData;
    }

    private void startRecording() {
        // if the default accelerometer exists
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // set accelerometer
            accelerometer = sensorManager
                    .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            // register 'this' as a listener that updates values. Each time a sensor value changes,
            // the method 'onSensorChanged()' is called.
            sensorManager.registerListener(this, accelerometer, sensorManager.SENSOR_DELAY_NORMAL);
        } else {
            // No accelerometer!
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create items.
        textRssi = (TextView) findViewById(R.id.textRSSI);
        buttonTrain = (Button) findViewById(R.id.buttonTrain);
        CellNumber = (EditText) findViewById(R.id.CellNumber);
        buttonLocate = (Button) findViewById(R.id.buttonLocate);
        buttonWalk = (Button) findViewById(R.id.buttonWalk);
        buttonRecord = (Button) findViewById(R.id.buttonRecord);
        buttonStop = (Button) findViewById(R.id.buttonStop);


        CellDataMain = new HashMap<String, Map<String, Integer>>();
        // Set listener for the button.
        buttonTrain.setOnClickListener(this);
        buttonLocate.setOnClickListener(this);
        buttonWalk.setOnClickListener(this);
        buttonRecord.setOnClickListener(this);
        buttonStop.setOnClickListener(this);

        sensorData = new ArrayList<AccelData>();
        trainedDataAcc = this.loadValuesAcc();

        // Set the sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);



    }

    private AccelData getMean(ArrayList<AccelData> data, int start, int end) {
        Double sumX = 0.0;
        Double sumY = 0.0;
        Double sumZ = 0.0;
        int count = 0;
        for(AccelData point: data) {
            if(count >= start && count < end) {
                sumX += point.getX();
                sumY += point.getY();
                sumZ += point.getZ();
            } else {
                count++;
            }
        }
        Double avgX = sumX/data.size();
        Double avgY = sumY/data.size();
        Double avgZ = sumZ/data.size();
        return new AccelData(0, avgX,avgY, avgZ);
    }

    // onResume() registers the accelerometer for listening the events
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    // onPause() unregisters the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing.
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // get the the x,y,z values of the accelerometer
        x = event.values[0];
        y = event.values[1];
        z = event.values[2];
        long timestamp = System.currentTimeMillis();
        AccelData data = new AccelData(timestamp, x, y, z);
        sensorData.add(data);

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){

            case R.id.buttonRecord:{
                this.textRssi.setText(this.kNN(new AccelData(0, 0, 3,7), this.trainedDataAcc));
                //this.startRecording();
                break;

            }

            case R.id.buttonWalk: {
                this.startRecording();
                final ArrayList<AccelData> avgPoints = new ArrayList<AccelData>();
                new Timer().scheduleAtFixedRate(new TimerTask(){
                    private int counter = 0;
                    private int pointer = 0;

                    @Override
                    public void run(){
                        int size = sensorData.size();
                        avgPoints.add(MainActivity.this.getMean(sensorData, pointer, size));
                        pointer = size;
                        if(++counter > 3) {
                            sensorManager.unregisterListener(MainActivity.this);
                            cancel();
                            AccelData fin = MainActivity.this.getMean(avgPoints, 0, avgPoints.size());
                            avgPoints.clear();
                            textRssi.setText(fin.toString());
                            return;
                        }
                    }
                },650,650);

                break;
            }

            case R.id.buttonStop:{
                sensorManager.unregisterListener(this);

                String filename = "accTrainingData";
                String fileEx = ".csv";

                File file = new File(getExternalFilesDir(null), filename + fileEx);

                int check  = 0;
                int filenum = 1;

                while (check == 0){
                    if (file.isFile()){
                        String fileNamenew = filename + filenum + fileEx;
                        file = new File(getExternalFilesDir(null), fileNamenew);
                        filenum++;
                    }
                    else {
                        check = 1;
                    }
                }

                FileOutputStream outputStream;
                try {
                    outputStream = new FileOutputStream(file);
                    outputStream.write("Timestamp, X, Y, Z \n".getBytes());
                    int s = sensorData.size();
                    for (int i = 0; i < s; i++)
                    {
                        AccelData d = (AccelData) sensorData.get(i);
                        String line = d.getTimestamp() + "," + d.getX() + "," + d.getY() + "," + d.getZ() + "\n";
                        outputStream.write(line.getBytes());
                    }
                    outputStream.close();
                } catch (Exception e) {
                    System.out.println("Error in File Writing");
                    e.printStackTrace();
                }
                sensorData.clear();

                break;
            }

            case R.id.buttonTrain: {
                // Set text.
                textRssi.setText("\n\tScan all access points:");

                textRssi.setText("\n\tCell Number: " + CellNumber.getText());

                Strength = new HashMap<String, Integer>();
                CellData = new HashMap<String, Map<String, Integer>>();

                // Set wifi manager.
                wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                // Start a wifi scan.
                wifiManager.startScan();
                // Store results in a list.
                List<ScanResult> scanResults = wifiManager.getScanResults();
                // Write results to a label
                List<ScanResult> desiredResult = new ArrayList<ScanResult>();
                Strength.clear();
                for (ScanResult scanResult : scanResults) {
                    if (scanResult.SSID.equals("eduroam")){

                        textRssi.setText(textRssi.getText() + "\n\tBSSID = "
                                + scanResult.BSSID + "  SSID = "
                                + scanResult.SSID + "  RSSI"
                                + scanResult.level + "dBm");
                        desiredResult.add(scanResult);
                        Strength.put(scanResult.BSSID, scanResult.level);
                    }
                }
                textRssi.setText("\n\t" + desiredResult);
                String cellNum = String.valueOf(CellNumber.getText());
                CellData.put(cellNum, Strength);
                CellDataMain.put(cellNum, Strength);
                textRssi.setText("\n\tCell 1: " + CellDataMain.get("C1") +
                        "\n\tCell 2: " + CellDataMain.get("C2") +
                        "\n\tCell 3: " + CellDataMain.get("C3"));

                this.saveVectors(CellDataMain);

                break;
            }



            case R.id.buttonLocate: {
                System.out.println("LOCATE BUTTON PRESSED");
                Strength = new HashMap<String, Integer>();
                CellData = new HashMap<String, Map<String, Integer>>();
                Map<String, List<Integer>> hotspot;
                int C1 = 0;
                int C2 = 0;
                int C3 = 0;
                int T = 0;
                float C1P = 0.0f;
                float C2P = 0.0f;
                float C3P = 0.0f;



                // Set wifi manager.
                wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                // Start a wifi scan.
                wifiManager.startScan();
                // Store results in a list.
                List<ScanResult> scanResults = wifiManager.getScanResults();
                // Write results to a label
                Strength.clear();
                for (ScanResult scanResult : scanResults) {
                    if (scanResult.SSID.equals("eduroam")){

                        textRssi.setText(textRssi.getText() + "\n\tBSSID = "
                                + scanResult.BSSID + "  SSID = "
                                + scanResult.SSID + "  RSSI"
                                + scanResult.level + "dBm");
                        Strength.put(scanResult.BSSID, scanResult.level);
                    }
                }

                locateData = new HashMap<String, Map<String, List<Integer>>>();

                locateData = this.loadValues();

                for (Map.Entry<String, Integer> entryStrength : Strength.entrySet())
                {
                    if (locateData.containsKey(entryStrength.getKey())){

                        hotspot = new HashMap<String, List<Integer>>();
                        hotspot = locateData.get(entryStrength.getKey());

                        for (Map.Entry<String, List<Integer>> cellStrength: hotspot.entrySet()){
                            if ((entryStrength.getValue() < cellStrength.getValue().get(0)) & entryStrength.getValue() > cellStrength.getValue().get(1)){
                                switch (cellStrength.getKey()){
                                    case "C1":{
                                        C1++;
                                        T++;
                                        break;
                                    }

                                    case  "C2":{
                                        C2++;
                                        T++;
                                        break;
                                    }

                                    case  "C3":{
                                        C3++;
                                        T++;
                                        break;
                                    }

                                }
                            }
                        }

                    }

                }

                System.out.println("C1: " + C1 + "C2: " + C2 + "C3: " + C3 + "Total: " + T);


                if ((C1 > C2) & (C1 > C3)){
                    textRssi.setText("\n\t Cell 1 \n\t");
                }
                else if ((C2 > C1) & (C2 > C3)){
                    textRssi.setText("\n\t Cell 2 \n\t");
                }
                else {
                    textRssi.setText("\n\t Cell 3 \n\t");
                }

                break;
            }
        }


    }


}
