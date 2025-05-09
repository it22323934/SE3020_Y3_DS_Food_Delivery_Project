import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  Button,
  Card,
  TextInput,
  Carousel,
  Badge,
} from "flowbite-react";
import {
  FaSearch,
  FaUtensils,
  FaStar,
  FaClock,
  FaMapMarkerAlt,
  FaMotorcycle,
  FaSignInAlt,
  FaUserPlus,
  FaTimes,
  FaArrowRight,
  FaCheck,
  FaHeart,
  FaMobileAlt,
  FaShoppingBag,
} from "react-icons/fa";
import { useSelector } from "react-redux";

export default function Home() {
  const [searchQuery, setSearchQuery] = useState("");
  const navigate = useNavigate();
  const { currentUser } = useSelector((state) => state.user);
  const isLoggedIn = !!currentUser;

  // Featured categories (could be fetched from API)
  const featuredCategories = [
    { id: 1, name: "Pizza", icon: "🍕", color: "bg-red-500" },
    { id: 2, name: "Burgers", icon: "🍔", color: "bg-yellow-500" },
    { id: 3, name: "Sushi", icon: "🍣", color: "bg-blue-500" },
    { id: 4, name: "Vegetarian", icon: "🥗", color: "bg-green-500" },
    { id: 5, name: "Desserts", icon: "🍰", color: "bg-pink-500" },
    { id: 6, name: "Drinks", icon: "🍹", color: "bg-purple-500" },
  ];

  // Featured offers (could be fetched from API)
  const promotions = [
    {
      id: 1,
      title: "50% OFF First Order",
      description: "Use code WELCOME50 at checkout",
      image: "https://images.unsplash.com/photo-1504674900247-0877df9cc836?q=80&w=1170&auto=format&fit=crop",
      color: "from-orange-500 to-red-500",
    },
    {
      id: 2,
      title: "Free Delivery Weekend",
      description: "No minimum order required",
      image: "https://images.unsplash.com/photo-1513104890138-7c749659a591?q=80&w=1170&auto=format&fit=crop",
      color: "from-blue-500 to-purple-500",
    },
    {
      id: 3,
      title: "Healthy Menu Options",
      description: "Discover our selection of nutritious meals",
      image: "https://images.unsplash.com/photo-1511690656952-34342bb7c2f2?q=80&w=1064&auto=format&fit=crop",
      color: "from-green-400 to-emerald-500",
    },
  ];

  // Handle search submission
  const handleSearch = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/restaurants?search=${encodeURIComponent(searchQuery)}`);
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

  // Navigate to all restaurants page
  const handleExploreRestaurants = () => {
    navigate("/restaurants");
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 transition-colors duration-200">
      {/* Hero Section with Enhanced Design */}
      <header className="relative bg-gradient-to-r from-orange-500 to-red-600 dark:from-orange-700 dark:to-red-800">
        <div className="absolute inset-0 bg-black/10 backdrop-blur-sm"></div>
        <div className="relative container mx-auto px-4 py-20 md:py-32">
          <div className="grid md:grid-cols-2 gap-8 items-center">
            <div className="text-left">
              <h1 className="text-4xl md:text-6xl font-extrabold text-white mb-6 leading-tight">
                Delicious Food <br />
                <span className="text-yellow-300">Delivered Fast</span>
              </h1>
              <p className="text-xl text-white opacity-90 mb-8 max-w-lg">
                Order from the best local restaurants with easy, contactless delivery
              </p>
              
              {/* Action Buttons */}
              <div className="flex flex-wrap gap-4">
                <Button 
                  size="xl" 
                  gradientDuoTone="pinkToOrange" 
                  onClick={handleExploreRestaurants}
                  className="font-semibold text-lg"
                >
                  Explore Restaurants
                  <FaArrowRight className="ml-2" />
                </Button>
                
                {!isLoggedIn && (
                  <Button 
                    size="xl" 
                    color="light" 
                    onClick={handleSignupClick}
                    className="font-semibold text-lg"
                  >
                    Sign Up
                    <FaUserPlus className="ml-2" />
                  </Button>
                )}
              </div>
            </div>
            
            {/* Search Form */}
            <div className="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-xl">
              <h2 className="text-2xl font-bold text-gray-800 dark:text-white mb-4">
                Find your favorite food
              </h2>
              <form onSubmit={handleSearch} className="space-y-4">
                <div className="relative">
                  <TextInput
                    id="search"
                    type="text"
                    placeholder="Search dishes or restaurants..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    icon={FaSearch}
                    className="w-full"
                    sizing="lg"
                    required
                  />
                  {searchQuery && (
                    <button
                      type="button"
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700"
                      onClick={() => setSearchQuery("")}
                    >
                      <FaTimes />
                    </button>
                  )}
                </div>
                <Button 
                  type="submit" 
                  color="success" 
                  fullSized
                  disabled={!searchQuery.trim()}
                >
                  <FaSearch className="mr-2" />
                  Search Food & Restaurants
                </Button>
                <p className="text-sm text-gray-500 dark:text-gray-400 text-center">
                  Popular: Pizza, Burger, Sushi, Vegan
                </p>
              </form>
            </div>
          </div>
        </div>
      </header>

      <main className="container mx-auto px-4 py-10 relative z-10">
        {/* Login Banner - Only for non-authenticated users */}
        {!isLoggedIn && (
          <div className="mb-12 overflow-hidden rounded-xl bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-gray-800 dark:to-gray-700 border border-blue-100 dark:border-gray-600 shadow-lg transform transition duration-300 hover:shadow-xl">
            <div className="p-1 bg-gradient-to-r from-blue-500 to-indigo-600"></div>
            <div className="p-6">
              <div className="flex flex-col md:flex-row items-center justify-between w-full gap-4">
                <div>
                  <h3 className="text-2xl font-bold text-gray-800 dark:text-white mb-2">
                    Create an account to get started
                  </h3>
                  <p className="text-gray-600 dark:text-gray-300">
                    Sign up to unlock exclusive deals and save your favorite restaurants
                  </p>
                </div>
                <div className="flex flex-col sm:flex-row gap-4 w-full md:w-auto">
                  <Button
                    color="blue"
                    size="lg"
                    onClick={handleLoginClick}
                    className="font-medium"
                  >
                    <FaSignInAlt className="mr-2" />
                    Log In
                  </Button>
                  <Button
                    gradientDuoTone="purpleToPink"
                    size="lg"
                    onClick={handleSignupClick}
                    className="font-medium"
                  >
                    <FaUserPlus className="mr-2" />
                    Sign Up Now
                  </Button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Featured Categories */}
        <section className="mb-16">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-2xl md:text-3xl font-bold text-gray-800 dark:text-white">
              Explore Categories
            </h2>
            <Button 
              color="light" 
              onClick={handleExploreRestaurants}
              className="hidden sm:flex"
            >
              View All
              <FaArrowRight className="ml-2" />
            </Button>
          </div>
          
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-4">
            {featuredCategories.map((category) => (
              <div 
                key={category.id} 
                onClick={() => navigate(`/restaurants?category=${category.id}`)}
                className="flex flex-col items-center p-4 bg-white dark:bg-gray-800 rounded-xl shadow-md hover:shadow-lg transition-all cursor-pointer border border-gray-100 dark:border-gray-700"
              >
                <div className={`${category.color} w-16 h-16 rounded-full flex items-center justify-center text-3xl mb-3`}>
                  {category.icon}
                </div>
                <h3 className="font-semibold text-gray-800 dark:text-white">
                  {category.name}
                </h3>
              </div>
            ))}
          </div>
        </section>

        {/* Promotional Carousel */}
        <section className="mb-16">
          <h2 className="text-2xl md:text-3xl font-bold text-gray-800 dark:text-white mb-6">
            Special Offers
          </h2>
          
          <div className="h-64 sm:h-80">
            <Carousel slideInterval={5000}>
              {promotions.map((promo) => (
                <div 
                  key={promo.id} 
                  className="relative h-full rounded-lg overflow-hidden cursor-pointer"
                  onClick={() => navigate('/promotions')}
                >
                  <img 
                    src={promo.image} 
                    alt={promo.title} 
                    className="w-full h-full object-cover"
                  />
                  <div className={`absolute inset-0 bg-gradient-to-r ${promo.color} opacity-80`}></div>
                  <div className="absolute inset-0 flex flex-col justify-center p-8 text-white">
                    <h3 className="text-3xl font-bold mb-2">{promo.title}</h3>
                    <p className="text-lg">{promo.description}</p>
                    <Button 
                      color="light" 
                      className="mt-4 w-fit"
                    >
                      Learn More
                    </Button>
                  </div>
                </div>
              ))}
            </Carousel>
          </div>
        </section>

        {/* How It Works Section */}
        <section className="mb-16 bg-white dark:bg-gray-800 rounded-2xl p-8 shadow-md">
          <h2 className="text-2xl md:text-3xl font-bold text-gray-800 dark:text-white mb-8 text-center">
            How It Works
          </h2>
          
          <div className="grid md:grid-cols-3 gap-8">
            <div className="flex flex-col items-center text-center">
              <div className="w-20 h-20 rounded-full bg-blue-100 dark:bg-blue-900 flex items-center justify-center mb-4">
                <FaSearch className="text-3xl text-blue-600 dark:text-blue-300" />
              </div>
              <h3 className="text-xl font-semibold mb-2 text-gray-800 dark:text-white">
                1. Search
              </h3>
              <p className="text-gray-600 dark:text-gray-300">
                Find your favorite restaurant or discover new ones near you
              </p>
            </div>
            
            <div className="flex flex-col items-center text-center">
              <div className="w-20 h-20 rounded-full bg-orange-100 dark:bg-orange-900 flex items-center justify-center mb-4">
                <FaShoppingBag className="text-3xl text-orange-600 dark:text-orange-300" />
              </div>
              <h3 className="text-xl font-semibold mb-2 text-gray-800 dark:text-white">
                2. Order
              </h3>
              <p className="text-gray-600 dark:text-gray-300">
                Choose from a wide variety of dishes and add them to your cart
              </p>
            </div>
            
            <div className="flex flex-col items-center text-center">
              <div className="w-20 h-20 rounded-full bg-green-100 dark:bg-green-900 flex items-center justify-center mb-4">
                <FaMotorcycle className="text-3xl text-green-600 dark:text-green-300" />
              </div>
              <h3 className="text-xl font-semibold mb-2 text-gray-800 dark:text-white">
                3. Delivery
              </h3>
              <p className="text-gray-600 dark:text-gray-300">
                Track your order in real-time and enjoy your meal at home
              </p>
            </div>
          </div>
        </section>

        {/* App Features */}
        <section className="mb-16 grid md:grid-cols-2 gap-8">
          <div className="bg-gradient-to-br from-purple-500 to-indigo-600 rounded-2xl p-8 text-white shadow-xl">
            <div className="flex flex-col h-full justify-between">
              <div>
                <h2 className="text-2xl font-bold mb-4">Download Our Appplication</h2>
                <p className="mb-6">Order on-the-go, track your delivery in real time, and get exclusive mobile-only offers.</p>
                <ul className="space-y-2 mb-8">
                  <li className="flex items-center">
                    <FaCheck className="mr-2 text-green-300" />
                    <span>Exclusive app-only deals</span>
                  </li>
                  <li className="flex items-center">
                    <FaCheck className="mr-2 text-green-300" />
                    <span>Save your favorite restaurants</span>
                  </li>
                  <li className="flex items-center">
                    <FaCheck className="mr-2 text-green-300" />
                    <span>Real-time delivery tracking</span>
                  </li>
                </ul>
              </div>
              <div className="flex space-x-4">
                <Button color="light">
                  <FaMobileAlt className="mr-2" />
                  App Store
                </Button>
                <Button color="light">
                  <FaMobileAlt className="mr-2" />
                  Google Play
                </Button>
              </div>
            </div>
          </div>
          
          <div className="bg-gradient-to-br from-amber-400 to-orange-500 rounded-2xl p-8 text-white shadow-xl">
            <div className="flex flex-col h-full justify-between">
              <div>
                <h2 className="text-2xl font-bold mb-4">Become a Partner Restaurant</h2>
                <p className="mb-6">Join Flavor Fleet and reach more customers. We handle the delivery, you focus on the food.</p>
                <ul className="space-y-2 mb-8">
                  <li className="flex items-center">
                    <FaCheck className="mr-2 text-green-300" />
                    <span>Grow your business</span>
                  </li>
                  <li className="flex items-center">
                    <FaCheck className="mr-2 text-green-300" />
                    <span>Manage orders easily</span>
                  </li>
                  <li className="flex items-center">
                    <FaCheck className="mr-2 text-green-300" />
                    <span>Access new customers</span>
                  </li>
                </ul>
              </div>
              <Button color="light">
                Register Your Restaurant
              </Button>
            </div>
          </div>
        </section>

        {/* User Testimonials */}
        <section className="mb-16">
          <h2 className="text-2xl md:text-3xl font-bold text-gray-800 dark:text-white mb-8 text-center">
            What Our Customers Say
          </h2>
          
          <div className="grid md:grid-cols-3 gap-6">
            {[
              {
                name: "Sarah Johnson",
                comment: "The delivery was super fast and the food arrived hot. Will definitely order again!",
                rating: 5
              },
              {
                name: "Mike Williams",
                comment: "Great selection of restaurants. I love being able to track my order in real-time.",
                rating: 4
              },
              {
                name: "Emily Chen",
                comment: "The app is so easy to use and the customer service is excellent. My go-to for food delivery.",
                rating: 5
              }
            ].map((testimonial, index) => (
              <Card key={index} className="overflow-hidden">
                <div className="flex justify-between items-start">
                  <div className="font-semibold text-gray-800 dark:text-white">
                    {testimonial.name}
                  </div>
                  <div className="flex items-center">
                    {[...Array(5)].map((_, i) => (
                      <FaStar 
                        key={i} 
                        className={i < testimonial.rating ? 
                          "text-yellow-400" : "text-gray-300"}
                        size={18}
                      />
                    ))}
                  </div>
                </div>
                <p className="text-gray-600 dark:text-gray-300 mt-2">
                  "{testimonial.comment}"
                </p>
              </Card>
            ))}
          </div>
        </section>

        {/* CTA Section */}
        <section className="text-center mb-8">
          <h2 className="text-3xl font-bold text-gray-800 dark:text-white mb-4">
            Ready to Order?
          </h2>
          <p className="text-xl text-gray-600 dark:text-gray-300 mb-8 max-w-2xl mx-auto">
            Delicious food is just a few clicks away. Browse our restaurants and have your favorite meals delivered to your door.
          </p>
          <Button 
            size="xl" 
            gradientDuoTone="purpleToBlue"
            onClick={handleExploreRestaurants}
            className="font-semibold text-lg"
          >
            Explore Restaurants
            <FaArrowRight className="ml-2" />
          </Button>
        </section>
      </main>
    </div>
  );
}