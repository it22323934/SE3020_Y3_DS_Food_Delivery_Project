import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Button, Card, Badge, Spinner, Tabs } from 'flowbite-react';
import { FaArrowLeft, FaStar, FaClock, FaMapMarkerAlt, FaPhoneAlt, FaUtensils, FaDollarSign } from 'react-icons/fa';
import { publicRestaurantService } from '../service/public/publicService';
import { useSelector } from 'react-redux';

export default function RestaurantDetail() {
  const { id } = useParams();
  const { currentUser } = useSelector(state => state.user);
  
  const [restaurant, setRestaurant] = useState(null);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Fetch restaurant and menu categories on component mount
  useEffect(() => {
    const fetchRestaurantData = async () => {
      try {
        setLoading(true);
        
        // Fetch restaurant details
        const restaurantResponse = await publicRestaurantService.getRestaurantById(id, currentUser?.token);
        
        if (!restaurantResponse.ok) {
          throw new Error('Failed to fetch restaurant details');
        }
        
        const restaurantData = await restaurantResponse.json();
        setRestaurant(restaurantData);
        
        // Fetch menu categories
        const categoriesResponse = await publicRestaurantService.getCategoriesByRestaurantId(
          id,
          currentUser?.token
        );
        
        if (categoriesResponse.ok) {
          const categoriesData = await categoriesResponse.json();
          // Sort categories by display order
          setCategories(categoriesData.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0)));
        } else {
          throw new Error('Failed to fetch menu categories');
        }
      } catch (err) {
        console.error('Error fetching data:', err);
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };
    
    fetchRestaurantData();
  }, [id, currentUser]);
  
  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-screen bg-gray-50 dark:bg-gray-900">
        <Spinner size="xl" className="text-blue-600 dark:text-blue-400" />
        <span className="ml-2 text-gray-700 dark:text-gray-300">Loading restaurant details...</span>
      </div>
    );
  }
  
  if (error || !restaurant) {
    return (
      <div className="container mx-auto px-4 py-8 bg-gray-50 dark:bg-gray-900">
        <div className="bg-red-100 dark:bg-red-900 border border-red-400 dark:border-red-700 text-red-700 dark:text-red-300 px-4 py-3 rounded mb-4">
          <p>Error: {error || "Restaurant not found"}</p>
        </div>
        <Link to="/">
          <Button color="gray" className="dark:bg-gray-700 dark:text-white">
            <FaArrowLeft className="mr-2" />
            Back to Home
          </Button>
        </Link>
      </div>
    );
  }
  
  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 transition-colors duration-200">
      {/* Restaurant Banner */}
      <div 
        className="h-64 bg-cover bg-center flex items-end relative"
        style={{ backgroundImage: `url(${restaurant.coverImageUrl || restaurant.bannerImageUrl || "https://via.placeholder.com/1200x400?text=Restaurant+Cover"})` }}
      >
        <div className="absolute inset-0 bg-black bg-opacity-50"></div>
        <div className="container mx-auto px-4 pb-4 relative">
          <Link to="/">
            <Button color="light" className="z-10 dark:bg-gray-700 dark:text-white dark:hover:bg-gray-600">
              <FaArrowLeft className="mr-2" />
              Back to Home
            </Button>
          </Link>
        </div>
      </div>
      
      <div className="container mx-auto px-4 py-8">
        {/* Restaurant Info */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md dark:shadow-gray-700 p-6 mb-6">
          <div className="flex flex-col md:flex-row md:justify-between md:items-center">
            <div>
              <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">{restaurant.name}</h1>
              
              {/* Cuisine Tags */}
              <div className="flex flex-wrap gap-1 mb-3">
                {restaurant.cuisineTypes && restaurant.cuisineTypes.map(cuisine => (
                  <Badge key={cuisine.id} color="info" className="px-2 py-1">
                    {cuisine.name}
                  </Badge>
                ))}
              </div>
              
              <p className="text-gray-600 dark:text-gray-300">{restaurant.description}</p>
            </div>
            
            <div className="mt-4 md:mt-0 md:text-right">
              <div className="flex items-center justify-end">
                <FaStar className="text-yellow-400 dark:text-yellow-300 mr-1" />
                <span className="font-medium text-lg text-gray-900 dark:text-white">
                  {restaurant.rating ? restaurant.rating.toFixed(1) : "New"}
                </span>
              </div>
              
              <div className="flex items-center justify-end mt-2">
                <FaClock className="text-gray-500 dark:text-gray-400 mr-1" />
                <span className="text-gray-700 dark:text-gray-300">{restaurant.deliveryTime || "30-45"} min delivery</span>
              </div>
            </div>
          </div>
          
          <hr className="my-4 border-gray-200 dark:border-gray-700" />
          
          {/* Additional Info */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="flex items-center text-gray-700 dark:text-gray-300">
              <FaMapMarkerAlt className="text-red-500 dark:text-red-400 mr-2" />
              <span>{restaurant.address || "Address not provided"}</span>
            </div>
            
            {restaurant.phoneNumber && (
              <div className="flex items-center text-gray-700 dark:text-gray-300">
                <FaPhoneAlt className="text-blue-500 dark:text-blue-400 mr-2" />
                <span>{restaurant.phoneNumber}</span>
              </div>
            )}
          </div>
        </div>
        
        {/* Menu Categories */}
        <h2 className="text-2xl font-bold text-gray-800 dark:text-white mb-4 flex items-center">
          <FaUtensils className="mr-2" />
          Menu
        </h2>
        
        {categories.length === 0 ? (
          <div className="bg-gray-100 dark:bg-gray-800 rounded-lg p-6 text-center">
            <p className="text-gray-600 dark:text-gray-400">No menu categories available</p>
          </div>
        ) : (
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-700">
            <Tabs style="underline" className="dark:border-gray-700">
              {categories.filter(cat => cat.active).map(category => (
                <Tabs.Item 
                  key={category.id} 
                  title={category.name} 
                  active={categories.indexOf(category) === 0}
                  className="dark:text-white"
                >
                  <div className="p-4">
                    {category.description && (
                      <p className="text-gray-600 dark:text-gray-300 mb-4">{category.description}</p>
                    )}
                    
                    <MenuItemsByCategorySection 
                      categoryId={category.id} 
                      token={currentUser?.token}
                    />
                  </div>
                </Tabs.Item>
              ))}
            </Tabs>
          </div>
        )}
      </div>
    </div>
  );
}

// Component to fetch and display menu items for a category
function MenuItemsByCategorySection({ categoryId, token }) {
  const [menuItems, setMenuItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchMenuItems = async () => {
      try {
        setLoading(true);
        const response = await publicRestaurantService.getMenuItemsByCategoryId(categoryId, token);
        
        if (response.ok) {
          const data = await response.json();
          setMenuItems(data);
        } else {
          throw new Error('Failed to fetch menu items');
        }
      } catch (err) {
        console.error('Error fetching menu items:', err);
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };
    
    fetchMenuItems();
  }, [categoryId, token]);
  
  if (loading) {
    return (
      <div className="flex justify-center py-8">
        <Spinner className="text-blue-600 dark:text-blue-400" />
        <span className="ml-2 text-gray-700 dark:text-gray-300">Loading menu items...</span>
      </div>
    );
  }
  
  if (error) {
    return (
      <div className="bg-red-100 dark:bg-red-900 border border-red-400 dark:border-red-700 text-red-700 dark:text-red-300 px-4 py-3 rounded mb-4">
        <p>Error: {error}</p>
      </div>
    );
  }
  
  if (menuItems.length === 0) {
    return (
      <div className="text-center py-6">
        <p className="text-gray-500 dark:text-gray-400">No menu items in this category</p>
      </div>
    );
  }
  
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-2 gap-4">
      {menuItems.map(item => (
        <Card key={item.id} className="hover:shadow-md dark:bg-gray-800 dark:border-gray-700">
          <div className="flex">
            {item.imageUrl && (
              <div className="w-24 h-24 flex-shrink-0">
                <img 
                  src={item.imageUrl} 
                  alt={item.name} 
                  className="w-full h-full object-cover rounded-md"
                />
              </div>
            )}
            <div className={item.imageUrl ? "ml-4 flex-1" : "flex-1"}>
              <h5 className="text-lg font-semibold text-gray-900 dark:text-white">{item.name}</h5>
              {item.description && (
                <p className="text-gray-600 dark:text-gray-300 text-sm line-clamp-2">{item.description}</p>
              )}
              <div className="mt-2 font-bold flex items-center">
                <FaDollarSign className="text-green-600 dark:text-green-400" />
                <span className="text-gray-800 dark:text-gray-100">{parseFloat(item.price).toFixed(2)}</span>
              </div>
            </div>
          </div>
        </Card>
      ))}
    </div>
  );
}