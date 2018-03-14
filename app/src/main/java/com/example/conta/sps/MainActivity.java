package com.example.conta.sps;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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

public class MainActivity extends Activity implements OnClickListener {

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
    private Button up;

    private Button right;

    private Button down;

    private Button left;

    private TextView feedback;

    private EditText CellNumber;

    private Map<String, Integer> Strength;

    private HashMap<String, Map<String, Integer>> CellData;

    private HashMap<String, Map<String, Integer>> CellDataMain;

    private HashMap<String, Map<String, List<Integer>>> locateData;

    private ArrayList<AccelData> trainedDataAcc;

    private HashMap<String, HashMap<String, List<Float>>> bayesianData;

    private String CD;


    private void saveVectors(HashMap<String, Map<String, Integer>> data) {

        long filetime = System.currentTimeMillis();
        String filename = filetime + " trainingData " + CD + ".csv";
        File file = new File(getExternalFilesDir(null), filename);

        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(file);
            outputStream.write("TimeStamp, Cell, Direction, BSSID, SSID, Strength \n".getBytes());
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



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create items.
        up = (Button) findViewById(R.id.up);
        CellNumber = (EditText) findViewById(R.id.CellNumber);
        right = (Button) findViewById(R.id.right);
        down = (Button) findViewById(R.id.down);
        left = (Button) findViewById(R.id.left);
        feedback = (TextView) findViewById(R.id.feedback);


        // Set listener for the button.
        up.setOnClickListener(this);
        right.setOnClickListener(this);
        down.setOnClickListener(this);
        left.setOnClickListener(this);

        // Set the sensor manager

        // Read Bayesian trained and processed data
        bayesianData =  readBayesianData();
        System.out.print(bayesianData);




    }

    public HashMap<String, HashMap<String, List<Float>>> readBayesianData () {

        HashMap<String, HashMap<String, List<Float>>> trainedBayesianData;
        HashMap<String, List<Float>> musigmaCell;
        trainedBayesianData = new HashMap<String, HashMap<String, List<Float>>>();
        LinkedList<Float> musigma;

        String filename = "distribution_cleaned.csv";
        File file = new File(getExternalFilesDir(null), filename);
        FileInputStream inputStream;

        String BSSID;
        String cellName;
        float mu;
        float sigma;

        try {

            inputStream = new FileInputStream(file);

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            try {

                String line;
                while ((line = reader.readLine()) != null) {
                    //trainedStrength  = new HashMap<String, List<Integer>>();

                    musigmaCell = new HashMap<String, List<Float>>();
                    musigma = new LinkedList<Float>();

                    String[] RowData = line.split(",");
                    cellName = RowData[0];
                    BSSID = RowData[1];
                    mu = Float.parseFloat(RowData[2]);
                    sigma = Float.parseFloat(RowData[3]);

                    musigma.add(mu);
                    musigma.add(sigma);

                    musigmaCell.put(cellName, musigma);

                    if (trainedBayesianData.containsKey(BSSID)) {
                        trainedBayesianData.get(BSSID).put(cellName, musigma);
                    } else {
                        trainedBayesianData.put(BSSID, musigmaCell);

                    }


                }




            } catch (Exception e) {
                e.printStackTrace();
            }

            inputStream.close();



        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return trainedBayesianData;
    }


        // onResume() registers the accelerometer for listening the events
    protected void onResume() {
        super.onResume();
    }

    // onPause() unregisters the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
    }

    private void collectData(final String CellDirection){
        CellDataMain = new HashMap<String, Map<String, Integer>>();

        new Timer().scheduleAtFixedRate(new TimerTask(){
            private int counter = 0;
            private int pointer = 0;

            @Override
            public void run(){

                feedback.setText("\n\tCell Number: " + CellDirection + "\n\tRecord Number " + counter);

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
                    String str = scanResult.BSSID + "," + scanResult.SSID;
                    Strength.put(str, scanResult.level);
                }

                //String cellNum = String.valueOf(CellNumber.getText());
                //CellData.put(cellNum, Strength);
                long time = System.currentTimeMillis();
                String timest = time + "," + CellDirection;
                CellDataMain.put(timest, Strength);


                if(++counter > 9) {
                    MainActivity.this.saveVectors(CellDataMain);
                    cancel();
                    feedback.setText("Recording Complete. Please move to next Cell or Direction");
                    return;
                }
            }
        },0,5000);


    }



        @Override
    public void onClick(View v) {

        switch (v.getId()){

            case R.id.up:{

                CD = CellNumber.getText() + ",D1";
                this.collectData(CD);

                break;

            }

            case R.id.right: {

                CD = CellNumber.getText() + ",D2";
                this.collectData(CD);

                break;
            }

            case R.id.left:{

                CD = CellNumber.getText() + ",D4";
                this.collectData(CD);

                break;
            }

            case R.id.down: {

                CD = CellNumber.getText() + ",D3";
                this.collectData(CD);

                break;
            }




        }


    }


}
