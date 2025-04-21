import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Card, TextInput, Spinner, Badge, Dropdown, RangeSlider } from 'flowbite-react';
import { 
  FaSearch, 
  FaUtensils, 
  FaStar, 
  FaClock, 
  FaMapMarkerAlt,
  FaLocationArrow,
  FaDirections
} from 'react-icons/fa';
import { restaurantService } from '../service/restaurantService';
import { cuisineTypeService } from '../service/cuisineService';
import { useSelector } from 'react-redux';

export default function Home() {
  const [restaurants, setRestaurants] = useState([]);
  const [cuisines, setCuisines] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCuisine, setSelectedCuisine] = useState('');
  
  // Location related states
  const [userLocation, setUserLocation] = useState(null);
  const [locationLoading, setLocationLoading] = useState(false);
  const [locationError, setLocationError] = useState(null);
  const [distanceRadius, setDistanceRadius] = useState(5); // Default 5km radius
  const [showNearby, setShowNearby] = useState(false);
  const [nearbyRestaurants, setNearbyRestaurants] = useState([]);
  
  const navigate = useNavigate();
  const { currentUser } = useSelector(state => state.user);
  
  // Get user's current location
  const getUserLocation = () => {
    if (!navigator.geolocation) {
      setLocationError('Geolocation is not supported by your browser');
      return;
    }
    
    setLocationLoading(true);
    setLocationError(null);
    
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setUserLocation({
          lat: position.coords.latitude,
          lng: position.coords.longitude
        });
        setLocationLoading(false);
        setShowNearby(true);
        
        // Fetch nearby restaurants when location is obtained
        fetchNearbyRestaurants(
          position.coords.latitude, 
          position.coords.longitude, 
          distanceRadius
        );
      },
      (error) => {
        setLocationError('Unable to retrieve your location');
        setLocationLoading(false);
        console.error('Geolocation error:', error);
      }
    );
  };
  
  // Fetch nearby restaurants
  const fetchNearbyRestaurants = async (lat, lng, radius) => {
    try {
      setLoading(true);
      const response = await restaurantService.getNearbyRestaurants(lat, lng, radius,currentUser?.token);
      
      if (!response.ok) {
        throw new Error('Failed to fetch nearby restaurants');
      }
      
      const data = await response.json();
      setNearbyRestaurants(data);
    } catch (err) {
      console.error('Error fetching nearby restaurants:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };
  
  // Update radius and fetch new nearby restaurants
  const handleRadiusChange = (value) => {
    setDistanceRadius(value);
    
    if (userLocation) {
      fetchNearbyRestaurants(userLocation.lat, userLocation.lng, value);
    }
  };
  
  // Fetch restaurants and cuisines on component mount
  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        
        // Fetch all restaurants
        let restaurantResponse;
        if (currentUser?.token) {
          restaurantResponse = await restaurantService.getAllRestaurants(currentUser.token);
        } else {
          // For public access without authentication
          restaurantResponse = await fetch(`http://localhost:8089/api/restaurants/public`);
        }
        
        if (!restaurantResponse.ok) {
          throw new Error('Failed to fetch restaurants');
        }
        
        const restaurantData = await restaurantResponse.json();
        setRestaurants(restaurantData);
        
        // Fetch cuisine types
        if (currentUser?.token) {
          const cuisineResponse = await cuisineTypeService.getAllCuisineTypes(currentUser.token);
          
          if (cuisineResponse.ok) {
            const cuisineData = await cuisineResponse.json();
            setCuisines(cuisineData);
          }
        }
      } catch (err) {
        console.error('Error fetching data:', err);
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };
    
    fetchData();
    
    // Check if we have user's location in local storage
    const savedLocation = localStorage.getItem('userLocation');
    if (savedLocation) {
      try {
        const locationData = JSON.parse(savedLocation);
        setUserLocation(locationData);
        
        // Fetch nearby restaurants with saved location
        fetchNearbyRestaurants(locationData.lat, locationData.lng, distanceRadius);
        setShowNearby(true);
      } catch (err) {
        console.error('Error parsing saved location:', err);
      }
    }
    
    // If user is logged in and has location in their profile
    if (currentUser?.latitude && currentUser?.longitude) {
      setUserLocation({
        lat: currentUser.latitude,
        lng: currentUser.longitude
      });
      
      // Fetch nearby restaurants with profile location
      fetchNearbyRestaurants(currentUser.latitude, currentUser.longitude, distanceRadius);
      setShowNearby(true);
    }
  }, [currentUser]);
  
  // Save location to local storage when it changes
  useEffect(() => {
    if (userLocation) {
      localStorage.setItem('userLocation', JSON.stringify(userLocation));
    }
  }, [userLocation]);
  
  // Calculate distance between two coordinates in kilometers
  const calculateDistance = (lat1, lng1, lat2, lng2) => {
    if (!lat1 || !lng1 || !lat2 || !lng2) return null;
    
    const R = 6371; // Radius of Earth in km
    const dLat = (lat2 - lat1) * (Math.PI / 180);
    const dLng = (lng2 - lng1) * (Math.PI / 180);
    const a = 
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) * 
      Math.sin(dLng / 2) * Math.sin(dLng / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    const distance = R * c;
    
    return distance;
  };
  
  // Filter restaurants based on search query and selected cuisine
  const getFilteredRestaurants = () => {
    const restList = showNearby ? nearbyRestaurants : restaurants;
    
    return restList.filter(restaurant => {
      const matchesSearch = restaurant.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          (restaurant.description && restaurant.description.toLowerCase().includes(searchQuery.toLowerCase()));
      
      const matchesCuisine = !selectedCuisine || 
                            (restaurant.cuisineTypeIds && 
                            restaurant.cuisineTypeIds.some(cuisine => cuisine.id === selectedCuisine));
      
      return matchesSearch && matchesCuisine;
    });
  };
  
  const filteredRestaurants = getFilteredRestaurants();
  
  // Handle restaurant click
  const handleRestaurantClick = (restaurantId) => {
    navigate(`/restaurant/${restaurantId}`);
  };
  
  // Handle search input change
  const handleSearchChange = (e) => {
    setSearchQuery(e.target.value);
  };
  
  // Handle cuisine selection
  const handleCuisineChange = (cuisineId) => {
    setSelectedCuisine(cuisineId === selectedCuisine ? '' : cuisineId);
  };
  
  // Toggle between all restaurants and nearby restaurants
  const toggleNearbyFilter = () => {
    if (!userLocation) {
      getUserLocation();
    } else {
      setShowNearby(!showNearby);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-gradient-to-r from-orange-500 to-red-600 text-white py-16">
        <div className="container mx-auto px-4">
          <h1 className="text-4xl md:text-5xl font-bold text-center mb-4">
            Food Delivery
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
              className="w-full"
            />
          </div>
        </div>
      </header>

      <main className="container mx-auto px-4 py-8">
        <div className="flex flex-wrap items-center justify-between gap-4 mb-8">
          {/* Cuisine Filter */}
          <div className="flex-1">
            {cuisines.length > 0 && (
              <div className="mb-4">
                <h2 className="text-lg font-semibold mb-3">Filter by Cuisine</h2>
                <div className="flex flex-wrap gap-2">
                  {cuisines.map(cuisine => (
                    <Button
                      key={cuisine.id}
                      color={selectedCuisine === cuisine.id ? "success" : "light"}
                      size="sm"
                      onClick={() => handleCuisineChange(cuisine.id)}
                    >
                      {cuisine.name}
                    </Button>
                  ))}
                </div>
              </div>
            )}
          </div>
          
          {/* Location Filter */}
          <div className="flex-none">
            <Button
              color={showNearby ? "success" : "light"}
              className="flex items-center"
              onClick={toggleNearbyFilter}
              disabled={locationLoading}
            >
              {locationLoading ? (
                <>
                  <Spinner size="sm" className="mr-2" />
                  Finding your location...
                </>
              ) : (
                <>
                  <FaLocationArrow className="mr-2" />
                  {showNearby ? "Show All Restaurants" : "Show Nearby Restaurants"}
                </>
              )}
            </Button>
            
            {locationError && (
              <div className="mt-2 text-xs text-red-600">{locationError}</div>
            )}
          </div>
        </div>
        
        {/* Distance Radius Slider (only show when viewing nearby restaurants) */}
        {showNearby && userLocation && (
          <div className="mb-8 bg-white p-4 rounded-lg shadow">
            <h3 className="text-lg font-medium mb-2">Distance: {distanceRadius} km</h3>
            <input
              type="range"
              min="1"
              max="20"
              value={distanceRadius}
              onChange={(e) => handleRadiusChange(parseInt(e.target.value))}
              className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
            />
            <div className="flex justify-between text-xs text-gray-500 mt-1">
              <span>1 km</span>
              <span>10 km</span>
              <span>20 km</span>
            </div>
          </div>
        )}
        
        {/* Loading State */}
        {loading && (
          <div className="flex justify-center items-center py-12">
            <Spinner size="xl" />
            <span className="ml-2">Loading restaurants...</span>
          </div>
        )}
        
        {/* Error State */}
        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
            <p>Error: {error}</p>
          </div>
        )}
        
        {/* No Results */}
        {!loading && !error && filteredRestaurants.length === 0 && (
          <div className="text-center py-12">
            <FaUtensils className="mx-auto text-gray-400 text-5xl mb-4" />
            <h2 className="text-2xl font-bold text-gray-600">No restaurants found</h2>
            <p className="text-gray-500 mt-2">
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
          {filteredRestaurants.map(restaurant => {
            // Calculate distance if we have user location and restaurant location
            const distance = userLocation && restaurant.latitude && restaurant.longitude
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
                className="hover:shadow-lg transition-shadow cursor-pointer"
                onClick={() => handleRestaurantClick(restaurant.id)}
              >
                {/* Restaurant Image */}
                <div className="w-full h-48 overflow-hidden">
                  <img 
                    src={restaurant.restaurantImageUrl || "https://via.placeholder.com/300x200?text=No+Image"} 
                    alt={restaurant.name} 
                    className="w-full h-full object-cover"
                  />
                </div>
                
                {/* Restaurant Info */}
                <div className="p-5">
                  <h5 className="text-xl font-bold tracking-tight text-gray-900 dark:text-white">
                    {restaurant.name}
                  </h5>
                  
                  {/* Cuisine Tags */}
                  <div className="flex flex-wrap gap-1 mt-2">
                    {restaurant.cuisineTypes && restaurant.cuisineTypes.map(cuisine => (
                      <Badge key={cuisine.id} color="info" className="px-2 py-1">
                        {cuisine.name}
                      </Badge>
                    ))}
                  </div>
                  
                  {/* Address and Distance */}
                  <div className="mt-2 text-sm text-gray-500">
                    {restaurant.address && (
                      <div className="flex items-center">
                        <FaMapMarkerAlt className="mr-1" />
                        <span className="truncate">{restaurant.address}</span>
                      </div>
                    )}
                    
                    {/* Show distance if available */}
                    {distance !== null && (
                      <div className="flex items-center mt-1 text-blue-600">
                        <FaDirections className="mr-1" />
                        <span>{distance.toFixed(1)} km away</span>
                      </div>
                    )}
                  </div>
                  
                  {/* Rating & Delivery Time */}
                  <div className="flex justify-between mt-3">
                    <div className="flex items-center">
                      <FaStar className="text-yellow-400 mr-1" />
                      <span className="font-medium">
                        {restaurant.rating ? restaurant.rating.toFixed(1) : "New"}
                      </span>
                    </div>
                    <div className="flex items-center">
                      <FaClock className="text-gray-500 mr-1" />
                      <span>{restaurant.deliveryTime || "30-45"} min</span>
                    </div>
                  </div>
                  
                  {/* Description */}
                  {restaurant.description && (
                    <p className="mt-3 text-gray-600 line-clamp-2">
                      {restaurant.description}
                    </p>
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