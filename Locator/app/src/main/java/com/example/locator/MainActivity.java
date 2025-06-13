package com.example.locator;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;


interface DoSomethingWithFormatedLocation {
    void execute(String s);
}

interface DoSomethingWithLocation {
    void execute(Location l);
}

public class MainActivity extends AppCompatActivity {

    // text views
    TextView textViewLocation;
    TextView textViewStartingPoint;
    TextView textViewDistance;

    // attributes for getting location stuff
    private FusedLocationProviderClient fusedLocationClient;
    CancellationToken cancellationToken;
    Location startLocation;

    // other attributes
    File documentationFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // init documentationFile
        File fileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        documentationFile = new File(fileDir, "range_test_location_results.txt");

        // init for getting location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        cancellationToken = cancellationTokenSource.getToken();

        // init text views
        textViewStartingPoint = findViewById(R.id.textViewStartingPoint);
        textViewDistance = findViewById(R.id.textViewDistance);
        textViewLocation = findViewById(R.id.textViewLocation);

        // init start button
        Button buttonStart = findViewById(R.id.buttonStart);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                textViewStartingPoint.setText("Preparing environment...");
                
                getCurrentGeoLocation(
                        s -> {
                            textViewStartingPoint.setText("- Starting point: -\n" + s);

                            // write start point into documentation file
                            String initText = "- Starting point: -\n" + s + "-----\n";
                            try {
                                writeToDocumentationFile(initText, false);
                            } catch (IOException e) {
                                textViewStartingPoint.setText("Internal error. Could not write to documentation file.");
                                throw new RuntimeException(e);
                            }
                        },
                        l -> startLocation = l
                );
            }
        });

        // init distance button
        Button buttonDistance = findViewById(R.id.buttonDistance);
        buttonDistance.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                textViewDistance.setText("Calculating distance from starting point...");

                getCurrentGeoLocation(
                        s -> {},
                        l -> {
                            if (startLocation == null) {  // check if initialization has not happened yet
                                textViewDistance.setText("Please press Start!");
                            } else {
                                String formattedDistance = l.distanceTo(startLocation) + " m";
                                textViewDistance.setText(formattedDistance);
                            }
                        }
                );
            }
        });

        // init test button
        Button testButton = findViewById(R.id.buttonLocation);
        testButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                textViewLocation.setText("Fetching location...");

                getCurrentGeoLocation(
                        s -> {
                            textViewLocation.setText(s);
                            String textToWriteToDocumentationFile = "---\n" + s + "---\n";
                            try {
                                writeToDocumentationFile(textToWriteToDocumentationFile, true);
                            } catch (FileNotFoundException e) {
                                textViewStartingPoint.setText("Internal error. Documentation file could not been found.");
                                throw new RuntimeException(e);
                            } catch (IOException e) {
                                textViewStartingPoint.setText("Internal error. Could not write to documentation file.");
                                throw new RuntimeException(e);
                            }
                        },
                        l -> {}
                );
            }
        });
    }

    /**
     * Gets the geographical location and exposes result for both given lambda functions.
     * 
     * @param doSomethingWithFormatedLocation Lambda function, which gets the fetched geo location as a formatted string.
     * @param doSomethingWithLocation Lambda function, which gets the fetched geo location as a instance of Location.
     */
    private void getCurrentGeoLocation(DoSomethingWithFormatedLocation doSomethingWithFormatedLocation, DoSomethingWithLocation doSomethingWithLocation) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            textViewLocation.setText("Please grant permission for accessing geographical location.");
        }

        // see https://developer.android.com/reference/android/location/Location.html for more information
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken)
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location == null) {
                            textViewLocation.setText("Please try again");
                        } else {
                            double lat = location.getLatitude();
                            double lon = location.getLongitude();
                            String latString = Location.convert(lat, Location.FORMAT_DEGREES);
                            String lonString = Location.convert(lon, Location.FORMAT_DEGREES);

                            double alt = location.getAltitude();
                            float acc = location.getAccuracy();
                            long time = location.getTime();

                            String formatedLocation = formatLocationData(latString, lonString, Float.toString(acc), Double.toString(alt), Long.toString(time));

                            doSomethingWithFormatedLocation.execute(formatedLocation);
                            doSomethingWithLocation.execute(location);
                        }
                    }
                });
    }

    /**
     * Returns a formated String with given contents of parameters.
     *
     * @param latitude The latitude of the geo location.
     * @param longitude The longitude of the geo location.
     * @param accuracy The estimated horizontal accuracy radius in meters of this location at the 68th percentile confidence level.
     * @param altitude The altitude of a geo location in meters above the WGS84 reference ellipsoid.
     * @param unixTime The Unix epoch time.
     * @return A formated String.
     */
    private String formatLocationData(String latitude, String longitude, String accuracy, String altitude, String unixTime) {
        String locationData = "Lat: "+latitude + "\n"
                            + "Lon: "+longitude + "\n"
                            + "Acc: "+accuracy + "\n"
                            + "Alt: "+altitude + "\n"
                            + "Tme: "+unixTime + "\n";

        return locationData;
    }

    /**
     * Writes to the documentation file.
     *
     * @param text The content to write into the documentation file.
     * @param appendToFile If false, the file will be completely overwritten. If true, the text will be appended to the file content.
     * @throws IOException If a I/O exception occurs.
     */
    private void writeToDocumentationFile(String text, boolean appendToFile) throws IOException {
        FileWriter writer = new FileWriter(documentationFile, appendToFile);
        writer.write(text);
        writer.close();
    }
}