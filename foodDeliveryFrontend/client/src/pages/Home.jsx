import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  Button,
  Card,
  TextInput,
  Spinner,
  Badge,
  Dropdown,
  RangeSlider,
  Alert,
} from "flowbite-react";
import {
  FaSearch,
  FaUtensils,
  FaStar,
  FaClock,
  FaMapMarkerAlt,
  FaLocationArrow,
  FaDirections,
  FaLock,
  FaSignInAlt,
  FaUserPlus,
} from "react-icons/fa";
import { publicRestaurantService } from "../service/public/publicService";
import { useSelector } from "react-redux";

export default function Home() {
  const [restaurants, setRestaurants] = useState([]);
  const [cuisines, setCuisines] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCuisine, setSelectedCuisine] = useState("");

  // Location related states
  const [userLocation, setUserLocation] = useState(null);
  const [locationLoading, setLocationLoading] = useState(false);
  const [locationError, setLocationError] = useState(null);
  const [distanceRadius, setDistanceRadius] = useState(5); // Default 5km radius
  const [showNearby, setShowNearby] = useState(false);
  const [nearbyRestaurants, setNearbyRestaurants] = useState([]);

  const navigate = useNavigate();
  const { currentUser } = useSelector((state) => state.user);
  const isLoggedIn = !!currentUser;

  // Get user's current location
  const getUserLocation = () => {
    if (!navigator.geolocation) {
      setLocationError("Geolocation is not supported by your browser");
      return;
    }

    setLocationLoading(true);
    setLocationError(null);

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setUserLocation({
          lat: position.coords.latitude,
          lng: position.coords.longitude,
        });
        setLocationLoading(false);
        setShowNearby(true);

        // Fetch nearby restaurants when location is obtained
        if (isLoggedIn) {
          fetchNearbyRestaurants(
            position.coords.latitude,
            position.coords.longitude,
            distanceRadius
          );
        }
      },
      (error) => {
        setLocationError("Unable to retrieve your location");
        setLocationLoading(false);
        console.error("Geolocation error:", error);
      }
    );
  };

  // Fetch nearby restaurants
  const fetchNearbyRestaurants = async (lat, lng, radius) => {
    if (!isLoggedIn) return;

    try {
      setLoading(true);
      const response = await publicRestaurantService.getNearbyRestaurants(
        lat,
        lng,
        radius,
        currentUser?.token
      );

      if (!response.ok) {
        throw new Error("Failed to fetch nearby restaurants");
      }

      const data = await response.json();
      setNearbyRestaurants(data);
    } catch (err) {
      console.error("Error fetching nearby restaurants:", err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Update radius and fetch new nearby restaurants
  const handleRadiusChange = (value) => {
    setDistanceRadius(value);

    if (userLocation && isLoggedIn) {
      fetchNearbyRestaurants(userLocation.lat, userLocation.lng, value);
    }
  };

  // Fetch restaurants and cuisines on component mount
  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);

        // Always show some restaurant previews even for logged-out users
        const restaurantResponse =
          await publicRestaurantService.getAllEnabledRestaurants(
            currentUser?.token
          );

        if (!restaurantResponse.ok) {
          throw new Error("Failed to fetch restaurants");
        }

        const restaurantData = await restaurantResponse.json();
        setRestaurants(restaurantData);

        // Fetch cuisine types only if user is logged in
        if (isLoggedIn) {
          const cuisineResponse =
            await publicRestaurantService.getActiveCuisineTypes(
              currentUser?.token
            );

          if (cuisineResponse.ok) {
            const cuisineData = await cuisineResponse.json();
            setCuisines(cuisineData);
          }
        }
      } catch (err) {
        console.error("Error fetching data:", err);
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchData();

    // Only process location if user is logged in
    if (isLoggedIn) {
      // Check if we have user's location in local storage
      const savedLocation = localStorage.getItem("userLocation");
      if (savedLocation) {
        try {
          const locationData = JSON.parse(savedLocation);
          setUserLocation(locationData);

          // Fetch nearby restaurants with saved location
          fetchNearbyRestaurants(
            locationData.lat,
            locationData.lng,
            distanceRadius
          );
          setShowNearby(true);
        } catch (err) {
          console.error("Error parsing saved location:", err);
        }
      }

      // If user is logged in and has location in their profile
      if (currentUser?.latitude && currentUser?.longitude) {
        setUserLocation({
          lat: currentUser.latitude,
          lng: currentUser.longitude,
        });

        // Fetch nearby restaurants with profile location
        fetchNearbyRestaurants(
          currentUser.latitude,
          currentUser.longitude,
          distanceRadius
        );
        setShowNearby(true);
      }
    }
  }, [currentUser]);

  // Save location to local storage when it changes
  useEffect(() => {
    if (userLocation && isLoggedIn) {
      localStorage.setItem("userLocation", JSON.stringify(userLocation));
    }
  }, [userLocation, isLoggedIn]);

  // Calculate distance between two coordinates in kilometers
  const calculateDistance = (lat1, lng1, lat2, lng2) => {
    if (!lat1 || !lng1 || !lat2 || !lng2) return null;

    const R = 6371; // Radius of Earth in km
    const dLat = (lat2 - lat1) * (Math.PI / 180);
    const dLng = (lng2 - lng1) * (Math.PI / 180);
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1 * (Math.PI / 180)) *
        Math.cos(lat2 * (Math.PI / 180)) *
        Math.sin(dLng / 2) *
        Math.sin(dLng / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    const distance = R * c;

    return distance;
  };

  // Filter restaurants based on search query and selected cuisine
  const getFilteredRestaurants = () => {
    const restList = showNearby ? nearbyRestaurants : restaurants;

    return restList.filter((restaurant) => {
      const matchesSearch =
        restaurant.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (restaurant.description &&
          restaurant.description
            .toLowerCase()
            .includes(searchQuery.toLowerCase()));

      // Only filter by cuisine if logged in and cuisine is selected
      const matchesCuisine =
        !isLoggedIn ||
        !selectedCuisine ||
        (restaurant.cuisineTypeIds &&
          restaurant.cuisineTypeIds.some(
            (cuisine) => cuisine.id === selectedCuisine
          ));

      return matchesSearch && matchesCuisine;
    });
  };

  const filteredRestaurants = getFilteredRestaurants();

  // Handle restaurant click
  const handleRestaurantClick = (restaurantId) => {
    if (isLoggedIn) {
      navigate(`/restaurant/${restaurantId}`);
    } else {
      // For non-logged in users, prompt to login instead of navigating
      navigate("/login", { state: { from: `/restaurant/${restaurantId}` } });
    }
  };

  // Handle search input change
  const handleSearchChange = (e) => {
    setSearchQuery(e.target.value);
  };

  // Handle cuisine selection
  const handleCuisineChange = (cuisineId) => {
    if (!isLoggedIn) return;
    setSelectedCuisine(cuisineId === selectedCuisine ? "" : cuisineId);
  };

  // Toggle between all restaurants and nearby restaurants
  const toggleNearbyFilter = () => {
    if (!isLoggedIn) {
      navigate("/login");
      return;
    }

    if (!userLocation) {
      getUserLocation();
    } else {
      setShowNearby(!showNearby);
    }
  };

  // Navigate to login page
  const handleLoginClick = () => {
    navigate("/sign-in");
  };

  // Navigate to signup page
  const handleSignupClick = () => {
    navigate("/sign-up");
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 transition-colors duration-200">
      <header className="bg-gradient-to-r from-orange-500 to-red-600 dark:from-orange-700 dark:to-red-800 text-white py-16 relative">
        <div className="container mx-auto px-4">
          <h1 className="text-4xl md:text-5xl font-bold text-center mb-4">
            Flavour Fleet
          </h1>
          <p className="text-xl text-center mb-8">
            Order delicious food from the best restaurants near you
          </p>

          {/* Search Bar */}
          <div className="max-w-md mx-auto">
            <TextInput
              id="search"
              type="text"
              placeholder="Search for restaurants, cuisines, or dishes..."
              value={searchQuery}
              onChange={handleSearchChange}
              icon={FaSearch}
              className="w-full dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400"
            />
          </div>
        </div>
      </header>

      <main className="container mx-auto px-4 py-8">
        {/* Login Alert for Non-Authenticated Users */}
        {/* Login Alert for Non-Authenticated Users - Enhanced Version */}
        {!isLoggedIn && (
          <div className="mb-6 overflow-hidden rounded-lg bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-gray-800 dark:to-gray-700 border border-blue-100 dark:border-gray-600 shadow-lg transition-all duration-300 transform hover:shadow-xl">
            <div className="p-1 bg-gradient-to-r from-blue-500 to-indigo-600"></div>
            <div className="p-4 sm:p-6">
              <div className="flex flex-col sm:flex-row items-center justify-between w-full">
                <div className="flex items-start sm:items-center mb-4 sm:mb-0">
                  <div className="flex-shrink-0 bg-blue-100 dark:bg-gray-700 p-3 rounded-full mr-4">
                    <FaLock className="h-6 w-6 text-blue-600 dark:text-blue-400" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-gray-800 dark:text-white mb-1">
                      Limited Access Mode
                    </h3>
                    <p className="text-gray-600 dark:text-gray-300">
                      Log in to unlock all features including restaurant
                      details, cuisine filtering, and food ordering.
                    </p>
                  </div>
                </div>
                <div className="flex flex-col sm:flex-row gap-3 w-full sm:w-auto mt-3 sm:mt-0">
                  <Button
                    color="blue"
                    size="md"
                    onClick={handleLoginClick}
                    className="transition-transform duration-200 hover:scale-105 font-medium"
                  >
                    <FaSignInAlt className="mr-2" />
                    Log In
                  </Button>
                  <Button
                    color="light"
                    size="md"
                    onClick={handleSignupClick}
                    className="transition-transform duration-200 hover:scale-105 font-medium border-2 border-blue-600 dark:border-blue-500"
                  >
                    <FaUserPlus className="mr-2" />
                    Sign Up
                  </Button>
                </div>
              </div>
            </div>
          </div>
        )}

        <div className="flex flex-wrap items-center justify-between gap-4 mb-8">
          {/* Cuisine Filter - Only show if logged in and cuisines are available */}
          <div className="flex-1">
            {isLoggedIn && cuisines.length > 0 ? (
              <div className="mb-4">
                <h2 className="text-lg font-semibold mb-3 text-gray-900 dark:text-white">
                  Filter by Cuisine
                </h2>
                <div className="flex flex-wrap gap-2">
                  {cuisines.map((cuisine) => (
                    <Button
                      key={cuisine.id}
                      color={
                        selectedCuisine === cuisine.id ? "success" : "light"
                      }
                      size="sm"
                      onClick={() => handleCuisineChange(cuisine.id)}
                      className="dark:border-gray-700"
                    >
                      {cuisine.name}
                    </Button>
                  ))}
                </div>
              </div>
            ) : (
              !isLoggedIn && (
                <div className="mb-4">
                  <h2 className="text-lg font-semibold mb-3 text-gray-900 dark:text-white">
                    Filter by Cuisine
                  </h2>
                  <p className="text-sm text-gray-500">
                    <FaLock className="inline mr-2" />
                    Log in to filter restaurants by cuisine type
                  </p>
                </div>
              )
            )}
          </div>

          {/* Location Filter */}
          <div className="flex-none">
            <Button
              color={showNearby ? "success" : "light"}
              className="flex items-center dark:border-gray-700"
              onClick={toggleNearbyFilter}
              disabled={locationLoading || !isLoggedIn}
            >
              {locationLoading ? (
                <>
                  <Spinner size="sm" className="mr-2" />
                  Finding your location...
                </>
              ) : (
                <>
                  <FaLocationArrow className="mr-2" />
                  {!isLoggedIn
                    ? "Login to view nearby"
                    : showNearby
                    ? "Show All Restaurants"
                    : "Show Nearby Restaurants"}
                </>
              )}
            </Button>

            {locationError && (
              <div className="mt-2 text-xs text-red-600 dark:text-red-400">
                {locationError}
              </div>
            )}
          </div>
        </div>

        {/* Distance Radius Slider (only show when viewing nearby restaurants and logged in) */}
        {isLoggedIn && showNearby && userLocation && (
          <div className="mb-8 bg-white dark:bg-gray-800 p-4 rounded-lg shadow dark:shadow-gray-700">
            <h3 className="text-lg font-medium mb-2 text-gray-900 dark:text-white">
              Distance: {distanceRadius} km
            </h3>
            <input
              type="range"
              min="1"
              max="20"
              value={distanceRadius}
              onChange={(e) => handleRadiusChange(parseInt(e.target.value))}
              className="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer"
            />
            <div className="flex justify-between text-xs text-gray-500 dark:text-gray-400 mt-1">
              <span>1 km</span>
              <span>10 km</span>
              <span>20 km</span>
            </div>
          </div>
        )}

        {/* Loading State */}
        {loading && (
          <div className="flex justify-center items-center py-12">
            <Spinner size="xl" className="text-blue-600 dark:text-blue-400" />
            <span className="ml-2 text-gray-700 dark:text-gray-300">
              Loading restaurants...
            </span>
          </div>
        )}

        {/* Error State */}
        {error && (
          <div className="bg-red-100 dark:bg-red-900 border border-red-400 dark:border-red-700 text-red-700 dark:text-red-300 px-4 py-3 rounded mb-4">
            <p>Error: {error}</p>
          </div>
        )}

        {/* No Results */}
        {!loading && !error && filteredRestaurants.length === 0 && (
          <div className="text-center py-12">
            <FaUtensils className="mx-auto text-gray-400 dark:text-gray-600 text-5xl mb-4" />
            <h2 className="text-2xl font-bold text-gray-600 dark:text-gray-300">
              No restaurants found
            </h2>
            <p className="text-gray-500 dark:text-gray-400 mt-2">
              {showNearby
                ? "No restaurants found within the selected radius. Try increasing the distance."
                : searchQuery
                ? "Try a different search term or filter"
                : "There are no restaurants available at the moment"}
            </p>
          </div>
        )}

        {/* Restaurant List */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredRestaurants.map((restaurant) => {
            // Calculate distance if we have user location and restaurant location
            const distance =
              userLocation && restaurant.latitude && restaurant.longitude
                ? calculateDistance(
                    userLocation.lat,
                    userLocation.lng,
                    restaurant.latitude,
                    restaurant.longitude
                  )
                : null;

            return (
              <Card
                key={restaurant.id}
                className="hover:shadow-lg transition-shadow cursor-pointer dark:border-gray-700 dark:bg-gray-800"
                onClick={() => handleRestaurantClick(restaurant.id)}
              >
                {/* Restaurant Image */}
                <div className="w-full h-48 overflow-hidden relative">
                  <img
                    src={
                      restaurant.restaurantImageUrl ||
                      "https://via.placeholder.com/300x200?text=No+Image"
                    }
                    alt={restaurant.name}
                    className={`w-full h-full object-cover ${
                      !isLoggedIn ? "opacity-80" : ""
                    }`}
                  />

                  {/* Login Required Overlay */}
                  {!isLoggedIn && (
                    <div className="absolute inset-0 flex items-center justify-center bg-black bg-opacity-30">
                      <div className="bg-black bg-opacity-70 p-2 rounded text-white">
                        <FaLock className="inline mr-1" /> Login to view details
                      </div>
                    </div>
                  )}
                </div>

                {/* Restaurant Info */}
                <div className="p-5">
                  <h5 className="text-xl font-bold tracking-tight text-gray-900 dark:text-white">
                    {restaurant.name}
                  </h5>

                  {/* Cuisine Tags */}
                  <div className="flex flex-wrap gap-1 mt-2">
                    {isLoggedIn &&
                      restaurant.cuisineTypes &&
                      restaurant.cuisineTypes.map((cuisine) => (
                        <Badge
                          key={cuisine.id}
                          color="info"
                          className="px-2 py-1"
                        >
                          {cuisine.name}
                        </Badge>
                      ))}
                    {!isLoggedIn && (
                      <span className="text-sm text-gray-500">
                        <FaLock className="inline mr-1" /> Login to view cuisine
                        types
                      </span>
                    )}
                  </div>

                  {/* Address and Distance */}
                  <div className="mt-2 text-sm text-gray-500 dark:text-gray-400">
                    {restaurant.address && (
                      <div className="flex items-center">
                        <FaMapMarkerAlt className="mr-1" />
                        <span className="truncate">{restaurant.address}</span>
                      </div>
                    )}

                    {/* Show distance if available and logged in */}
                    {isLoggedIn && distance !== null && (
                      <div className="flex items-center mt-1 text-blue-600 dark:text-blue-400">
                        <FaDirections className="mr-1" />
                        <span>{distance.toFixed(1)} km away</span>
                      </div>
                    )}
                  </div>

                  {/* Rating & Delivery Time */}
                  <div className="flex justify-between mt-3">
                    <div className="flex items-center">
                      <FaStar className="text-yellow-400 dark:text-yellow-300 mr-1" />
                      <span className="font-medium text-gray-900 dark:text-white">
                        {restaurant.rating
                          ? restaurant.rating.toFixed(1)
                          : "New"}
                      </span>
                    </div>
                    <div className="flex items-center text-gray-500 dark:text-gray-400">
                      <FaClock className="mr-1" />
                      <span>{restaurant.deliveryTime || "30-45"} min</span>
                    </div>
                  </div>

                  {/* Description */}
                  {restaurant.description && (
                    <p className="mt-3 text-gray-600 dark:text-gray-300 line-clamp-2">
                      {restaurant.description}
                    </p>
                  )}

                  {/* Login Button - For non-authenticated users */}
                  {!isLoggedIn && (
                    <Button
                      color="info"
                      className="w-full mt-4"
                      onClick={(e) => {
                        e.stopPropagation();
                        navigate("/login", {
                          state: { from: `/restaurant/${restaurant.id}` },
                        });
                      }}
                    >
                      <FaSignInAlt className="mr-2" />
                      Login to View Details
                    </Button>
                  )}
                </div>
              </Card>
            );
          })}
        </div>
      </main>
    </div>
  );
}
