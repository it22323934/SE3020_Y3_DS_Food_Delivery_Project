import React, { useEffect, useState } from 'react';
import { GoogleMap, LoadScript, Marker } from '@react-google-maps/api';
import axios from 'axios';

const containerStyle = {
  width: '100%',
  height: '400px',
};

function LocationMap() {
  const [currentLocation, setCurrentLocation] = useState(null);

  useEffect(() => {
    const watchId = navigator.geolocation.watchPosition(
      (position) => {
        setCurrentLocation({
          lat: position.coords.latitude,
          lng: position.coords.longitude,
        });
      },
      (error) => {
        console.error('Error getting location:', error);
        alert('Could not get your current location.');
        // Optional fallback
        setCurrentLocation({ lat: 6.9271, lng: 79.8612 });
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0,
      }
    );

    // Cleanup on unmount
    return () => navigator.geolocation.clearWatch(watchId);
  }, []);

  const handleSendLocation = async () => {
    try {
      if (!currentLocation) {
        alert('Location not available yet!');
        return;
      }

      const response = await axios.post('http://localhost:8083/api/location', {
        latitude: currentLocation.lat,
        longitude: currentLocation.lng,
        userId:"R1",
      });

      if (response.status === 200) {
        alert('Location sent successfully!');
      } else {
        alert('Failed to send location.');
      }
    } catch (error) {
      console.error('Error sending location:', error);
      alert('Error sending location.');
    }
  };

  return (
    <div>
      <LoadScript googleMapsApiKey="AIzaSyCms2-r4afPJIKiStBZUNuRx_4BdU2p9ps">
        {currentLocation && (
          <GoogleMap
            mapContainerStyle={containerStyle}
            center={currentLocation}
            zoom={15}
          >
            <Marker position={currentLocation} />
          </GoogleMap>
        )}
      </LoadScript>

      <button onClick={handleSendLocation}>Send Location</button>
    </div>
  );
}

export default LocationMap;
