package com.example.conta.sps;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.io.FileOutputStream;
import java.io.File;

import android.app.Activity;
import android.net.wifi.ScanResult;
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

public class MainActivity extends Activity implements OnClickListener{

    /**
     * The wifi manager.
     */
    private WifiManager wifiManager;
    /**
     * The text view.
     */
    private TextView textRssi;
    /**
     * The button.
     */
    private Button buttonTrain;

    private Button buttonLocate;

    private EditText CellNumber;

    private Map<String, Integer> Strength;

    private HashMap<String, Map<String, Integer>> CellData;

    private HashMap<String, Map<String, Integer>> CellDataMain;

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

    private HashMap<String, Map<String, List<Integer>>> locateData;


    private HashMap<String, Map<String, List<Integer>>> loadValues() {

        HashMap<String, Map<String, List<Integer>>> trainedData;
        HashMap<String, List<Integer>> trainedStrength;
        List<Integer> maxmin;


        trainedData = new HashMap<String, Map<String, List<Integer>>>();

        String filename = "trainedData.csv";
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
        textRssi = (TextView) findViewById(R.id.textRSSI);
        buttonTrain = (Button) findViewById(R.id.buttonTrain);
        CellNumber = (EditText) findViewById(R.id.CellNumber);
        buttonLocate = (Button) findViewById(R.id.buttonLocate);


        CellDataMain = new HashMap<String, Map<String, Integer>>();
        // Set listener for the button.
        buttonTrain.setOnClickListener(this);
        buttonLocate.setOnClickListener(this);
    }

    // onResume() registers the accelerometer for listening the events
    protected void onResume() {
        super.onResume();
    }

    // onPause() unregisters the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){

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



                break;



            }
        }


    }


}
