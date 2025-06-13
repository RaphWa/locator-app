# locator-app
This android application enables the user to document their geographical location into a text file on the android device.

This app was successfully tested on the following device: Galaxy A32 5G.
Used Android-Version: 13.

## Usage
The app contains three buttons and three corresponding text views.
The documentation file, into which the locations are documented, is located at /Documents/range_test_location_results.txt on the android device.

The button 'Start' resets the documentation file and sets the starting point.

The button 'Calculate distance from starting point' calculates the distance in meters from the current location to the starting point.

The button 'Get current location' documents the current location in the documentation file.

## Documentation style
The longitude and latitude are written in the following format: Location.FORMAT_DEGREES (for more information see [this documentation](https://developer.android.com/reference/android/location/Location.html)). 

| Short name  | Full name |
| ------------- | ------------- |
| Lat  | Latitude  |
| Lon  | Longitude  |
| Acc  | Accuracy  |
| Alt  | Altitude  |
| Tme  | Unix epoch time  |