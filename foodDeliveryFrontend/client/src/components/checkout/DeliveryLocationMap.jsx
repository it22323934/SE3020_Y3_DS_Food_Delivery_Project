import React from "react";
import PropTypes from 'prop-types';
import { Card, Label } from "flowbite-react";
import { HiLocationMarker, HiCheck, HiExclamation } from "react-icons/hi";
import { LoadScript, GoogleMap, Marker, Circle } from "@react-google-maps/api";
import GooglePlacesAutocomplete from "react-google-places-autocomplete";
import { sanitizeInput, sanitizeCoordinates } from "../../utils/sanitize";

const DeliveryLocationMap = ({
  formData,
  formErrors,
  locationValue,
  handleLocationSelect,
  isMapLoaded,
  setIsMapLoaded,
  restaurantLocation,
  distanceToRestaurant,
  handleMapClick,
  getMapCenter,
  mapContainerStyle,
}) => {
  return (
    <Card className="mb-6">
      <h2 className="text-xl font-semibold mb-4 flex items-center">
        <HiLocationMarker className="mr-2 text-blue-600" /> Delivery Location
      </h2>

      <div className="space-y-4">
        <LoadScript
          googleMapsApiKey="AIzaSyCms2-r4afPJIKiStBZUNuRx_4BdU2p9ps"
          libraries={["places"]}
          onLoad={() => {
            console.log("Google Maps API loaded successfully");
            setIsMapLoaded(true);
          }}
          onError={(error) =>
            console.error("Google Maps API loading failed:", error)
          }
        >
          <div className="mb-4">
            <Label
              htmlFor="location"
              value="Search for delivery address"
              className="mb-2"
            />
            <GooglePlacesAutocomplete
              apiKey="AIzaSyCms2-r4afPJIKiStBZUNuRx_4BdU2p9ps"
              selectProps={{
                value: locationValue,
                onChange: handleLocationSelect,
                placeholder:
                  sanitizeInput(formData.deliveryLocation.address) || "Search for an address...",
                styles: {
                  control: (provided) => ({
                    ...provided,
                    padding: "4px",
                    borderColor: "#D1D5DB",
                    boxShadow: "none",
                  }),
                },
              }}
            />
          </div>

          <div className="mt-4 mb-4">
            <Label value="Pin Your Delivery Location" />
            <p className="text-sm text-gray-500 mb-2">
              Click on the map to set your precise delivery location
            </p>
            {isMapLoaded && (
              <GoogleMap
                mapContainerStyle={mapContainerStyle}
                zoom={14}
                center={getMapCenter()}
                onClick={handleMapClick}
              >
                {/* Restaurant marker */}
                {restaurantLocation && (
                  <>
                    <Marker
                      position={restaurantLocation}
                      icon={{
                        url: "http://maps.google.com/mapfiles/ms/icons/red-dot.png",
                        labelOrigin: { x: 15, y: -10 },
                      }}
                      label={{
                        text: "Restaurant",
                        color: "#C53030",
                        fontWeight: "bold",
                      }}
                    />
                    {/* 20km delivery radius */}
                    <Circle
                      center={restaurantLocation}
                      radius={20000} // 20km in meters
                      options={{
                        strokeColor: "#4299E1",
                        strokeOpacity: 0.8,
                        strokeWeight: 2,
                        fillColor: "#4299E1",
                        fillOpacity: 0.1,
                      }}
                    />
                  </>
                )}

                {/* User's selected delivery location */}
                {formData.deliveryLocation.latitude &&
                  formData.deliveryLocation.longitude && (
                    <Marker
                      position={{
                        lat: parseFloat(sanitizeCoordinates(formData.deliveryLocation.latitude)),
                        lng: parseFloat(sanitizeCoordinates(formData.deliveryLocation.longitude)),
                      }}
                      icon={{
                        url: "http://maps.google.com/mapfiles/ms/icons/blue-dot.png",
                        labelOrigin: { x: 15, y: -10 },
                      }}
                      label={{
                        text: "Delivery",
                        color: "#2B6CB0",
                        fontWeight: "bold",
                      }}
                    />
                  )}
              </GoogleMap>
            )}

            {/* Distance information */}
            {distanceToRestaurant !== null && (
              <div
                className={`mt-2 p-2 rounded text-sm ${
                  distanceToRestaurant > 20
                    ? "bg-red-50 text-red-700 border border-red-200"
                    : "bg-green-50 text-green-700 border border-green-200"
                }`}
              >
                <div className="flex items-center">
                  {distanceToRestaurant > 20 ? (
                    <HiExclamation className="mr-1 flex-shrink-0" />
                  ) : (
                    <HiCheck className="mr-1 flex-shrink-0" />
                  )}
                  <span>
                    Distance to restaurant:{" "}
                    <strong>{distanceToRestaurant.toFixed(1)} km</strong>
                    {distanceToRestaurant > 20 && " (Out of delivery range)"}
                  </span>
                </div>
              </div>
            )}

            {formErrors.location && (
              <p className="text-sm text-red-500 mt-1">{formErrors.location}</p>
            )}
          </div>
        </LoadScript>
      </div>
    </Card>
  );
};

DeliveryLocationMap.propTypes = {
  formData: PropTypes.shape({
    deliveryLocation: PropTypes.shape({
      address: PropTypes.string,
      latitude: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
      longitude: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    }).isRequired,
  }).isRequired,
  formErrors: PropTypes.shape({
    location: PropTypes.string,
  }),
  locationValue: PropTypes.shape({
    label: PropTypes.string,
    value: PropTypes.shape({
      description: PropTypes.string,
      place_id: PropTypes.string,
    }),
  }),
  handleLocationSelect: PropTypes.func.isRequired,
  isMapLoaded: PropTypes.bool.isRequired,
  setIsMapLoaded: PropTypes.func.isRequired,
  restaurantLocation: PropTypes.shape({
    lat: PropTypes.number.isRequired,
    lng: PropTypes.number.isRequired,
  }),
  distanceToRestaurant: PropTypes.number,
  handleMapClick: PropTypes.func.isRequired,
  getMapCenter: PropTypes.func.isRequired,
  mapContainerStyle: PropTypes.shape({
    width: PropTypes.string,
    height: PropTypes.string,
  }).isRequired,
};

DeliveryLocationMap.defaultProps = {
  formErrors: {},
  locationValue: null,
  restaurantLocation: null,
  distanceToRestaurant: null,
};

export default DeliveryLocationMap;