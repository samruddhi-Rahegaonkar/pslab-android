package io.pslab.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.pslab.R;
import io.pslab.activity.guide.GuideActivity;
import io.pslab.communication.ScienceLab;
import io.pslab.communication.peripherals.I2C;
import io.pslab.others.CustomSnackBar;
import io.pslab.others.ScienceLabCommon;
import io.pslab.sensors.SensorADS1115;
import io.pslab.sensors.SensorAPDS9960;
import io.pslab.sensors.SensorBMP180;
import io.pslab.sensors.SensorCCS811;
import io.pslab.sensors.SensorHMC5883L;
import io.pslab.sensors.SensorMLX90614;
import io.pslab.sensors.SensorMPU6050;
import io.pslab.sensors.SensorMPU925X;
import io.pslab.sensors.SensorSHT21;
import io.pslab.sensors.SensorTSL2561;
import io.pslab.sensors.SensorVL53L0X;

public class SensorActivity extends GuideActivity {

    private I2C i2c;
    private ScienceLab scienceLab;
    private final Map<Integer, List<String>> sensorAddr = new LinkedHashMap<>();
    private final List<String> dataName = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ListView lvSensor;
    private TextView tvSensorScan;
    private Button buttonSensorAutoScan;

    public SensorActivity() {
        super(R.layout.sensor_main);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scienceLab = ScienceLabCommon.scienceLab;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.sensors);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        i2c = scienceLab.i2c;

        // Initialize the sensor map
        addSensorToMap(0x48, "ADS1115");
        addSensorToMap(0x77, "BMP180");
        addSensorToMap(0x5A, "MLX90614");
        addSensorToMap(0x5A, "CCS811");  // Duplicate address
        addSensorToMap(0x1E, "HMC5883L");
        addSensorToMap(0x68, "MPU6050");
        addSensorToMap(0x40, "SHT21");
        addSensorToMap(0x39, "TSL2561");
        addSensorToMap(0x39, "APDS9960");  // Duplicate address
        addSensorToMap(0x69, "MPU925x");
        addSensorToMap(0x29, "VL53L0X");

        adapter = new ArrayAdapter<>(getApplication(), R.layout.sensor_list_item, R.id.tv_sensor_list_item, dataName);

        buttonSensorAutoScan = findViewById(R.id.button_sensor_autoscan);
        tvSensorScan = findViewById(R.id.tv_sensor_scan);
        tvSensorScan.setText(getResources().getString(R.string.use_autoscan));
        lvSensor = findViewById(R.id.lv_sensor);
        lvSensor.setAdapter(adapter);

        buttonSensorAutoScan.setOnClickListener(v -> {
            buttonSensorAutoScan.setClickable(false);
            tvSensorScan.setText(getResources().getString(R.string.scanning));
            new PopulateSensors().execute();
        });

        lvSensor.setOnItemClickListener((parent, view, position, id) -> {
            String itemValue = (String) lvSensor.getItemAtPosition(position);
            Intent intent;
            switch (itemValue) {
                case "ADS1115":
                    intent = new Intent(getApplication(), SensorADS1115.class);
                    startActivity(intent);
                    break;
                case "BMP180":
                    intent = new Intent(getApplication(), SensorBMP180.class);
                    startActivity(intent);
                    break;
                case "MLX90614":
                    intent = new Intent(getApplication(), SensorMLX90614.class);
                    startActivity(intent);
                    break;
                case "HMC5883L":
                    intent = new Intent(getApplication(), SensorHMC5883L.class);
                    startActivity(intent);
                    break;
                case "MPU6050":
                    intent = new Intent(getApplication(), SensorMPU6050.class);
                    startActivity(intent);
                    break;
                case "SHT21":
                    intent = new Intent(getApplication(), SensorSHT21.class);
                    startActivity(intent);
                    break;
                case "TSL2561":
                    intent = new Intent(getApplication(), SensorTSL2561.class);
                    startActivity(intent);
                    break;
                case "MPU925x":
                    intent = new Intent(getApplication(), SensorMPU925X.class);
                    startActivity(intent);
                    break;
                case "VL53L0X":
                    intent = new Intent(getApplication(), SensorVL53L0X.class);
                    startActivity(intent);
                    break;
                case "CCS811":
                    intent = new Intent(getApplication(), SensorCCS811.class);
                    startActivity(intent);
                    break;
                case "APDS9960":
                    intent = new Intent(getApplication(), SensorAPDS9960.class);
                    startActivity(intent);
                    break;
                default:
                    CustomSnackBar.showSnackBar(findViewById(android.R.id.content),
                            "Sensor Not Supported", null, null, Snackbar.LENGTH_SHORT);
            }
        });
    }

    private void addSensorToMap(int address, String sensorName) {
        // Add the sensor to the map, creating a new list if necessary
        sensorAddr.computeIfAbsent(address, k -> new ArrayList<>()).add(sensorName);
    }

    private class PopulateSensors extends AsyncTask<Void, Void, Void> {
        private List<Integer> detectedAddresses;

        @Override
        protected Void doInBackground(Void... voids) {
            detectedAddresses = new ArrayList<>();
            dataName.clear();

            if (scienceLab.isConnected()) {
                try {
                    detectedAddresses = i2c.scan(null);
                } catch (IOException | NullPointerException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (scienceLab.isConnected() && detectedAddresses != null) {
                for (Integer address : detectedAddresses) {
                    if (sensorAddr.containsKey(address)) {
                        dataName.addAll(sensorAddr.get(address));
                    }
                }
            }

            // Add all sensors, even if not detected
            for (List<String> sensors : sensorAddr.values()) {
                for (String sensor : sensors) {
                    if (!dataName.contains(sensor)) {
                        dataName.add(sensor);
                    }
                }
            }
            Collections.sort(dataName);

            if (scienceLab.isConnected()) {
                tvSensorScan.setText(getString(R.string.not_connected));
            } else {
                tvSensorScan.setText(getString(R.string.not_connected));
            }

            adapter.notifyDataSetChanged();
            buttonSensorAutoScan.setClickable(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sensor_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.show_guide:
                toggleGuide();
                break;
            default:
                break;
        }
        return true;
    }
}
