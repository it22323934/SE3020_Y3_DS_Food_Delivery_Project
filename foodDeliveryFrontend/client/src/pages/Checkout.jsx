import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useCart } from "../context/CartContext";
import { useSelector } from "react-redux";
import { sanitizeInput, sanitizeObject, sanitizeEmail, sanitizePhone, sanitizeCoordinates } from "../utils/sanitize";
import {
  Button,
  Card,
  TextInput,
  Label,
  Radio,
  Textarea,
  Alert,
  Spinner,
  Badge,
  Modal,
} from "flowbite-react";
import {
  HiLocationMarker,
  HiCreditCard,
  HiCash,
  HiShoppingBag,
  HiUser,
  HiPhone,
  HiMail,
  HiDocumentText,
  HiChevronLeft,
  HiCheck,
  HiClock,
  HiExclamation,
  HiArrowSmRight,
  HiOutlineCheckCircle,
  HiOutlineDocumentText,
  HiOutlineCreditCard,
  HiOutlineLocationMarker,
} from "react-icons/hi";
import { FaCcVisa, FaCcMastercard, FaCcPaypal } from "react-icons/fa";
import { orderService } from "../service/orderService";
import { LoadScript, GoogleMap, Marker, Circle } from "@react-google-maps/api";
import GooglePlacesAutocomplete from "react-google-places-autocomplete";
import DeliveryAddressForm from "../components/checkout/DeliveryAddressForm";
import { restaurantService } from "../service/restaurantService";
import { Elements } from "@stripe/react-stripe-js";
import { loadStripe } from "@stripe/stripe-js";
import CheckoutForm from "../components/CheckoutForm";
import StripePaymentWrapper from "../components/checkout/StripePaymentWrapper";
import paymentService from "../service/paymentService";
import OrderSummary from "../components/checkout/OrderSummary";
import DeliveryLocationMap from "../components/checkout/DeliveryLocationMap";
import ContactInfoForm from "../components/checkout/ContactInfoForm";
import PaymentMethodSelector from "../components/checkout/PaymentMethodSelector";

// Load Stripe outside of component render
const stripePromise = loadStripe(import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY);

export default function Checkout() {
  const navigate = useNavigate();
  const { currentUser } = useSelector((state) => state.user);
  const { cart, clearCart } = useCart();

  // Form state
  const [formData, setFormData] = useState({
    fullName: currentUser?.fullName || "",
    email: currentUser?.email || "",
    phone: currentUser?.phone || "",
    address: {
      street: "",
      city: "",
      state: "",
      zipCode: "",
    },
    deliveryLocation: {
      latitude: "",
      longitude: "",
      address: "",
      formattedAddress: "",
    },
    deliveryInstructions: "",
  });
  const [formErrors, setFormErrors] = useState({});

  // Order processing state
  const [orderProcessing, setOrderProcessing] = useState(false);
  const [orderSuccess, setOrderSuccess] = useState(false);
  const [orderError, setOrderError] = useState("");
  const [orderId, setOrderId] = useState(null);

  // Payment state
  const [paymentMethod, setPaymentMethod] = useState("card");
  const [showPaymentStep, setShowPaymentStep] = useState(false);
  const [clientSecret, setClientSecret] = useState("");
  const [paymentError, setPaymentError] = useState("");
  const [paymentProcessing, setPaymentProcessing] = useState(false);
  const [paymentSuccess, setPaymentSuccess] = useState(false);
  const [paymentMessage, setPaymentMessage] = useState("");
  const [subtotal, setSubtotal] = useState(0);

  // Map state
  const [isMapLoaded, setIsMapLoaded] = useState(false);
  const [locationValue, setLocationValue] = useState(null);
  const [restaurantLocation, setRestaurantLocation] = useState(null);
  const [distanceToRestaurant, setDistanceToRestaurant] = useState(null);
  const [locationError, setLocationError] = useState("");
  const [restaurantId, setRestaurantId] = useState(null);

  // Checkout flow
  const [checkoutStep, setCheckoutStep] = useState(0);
  const [orderData, setOrderData] = useState(null);

  // Map configuration
  const mapContainerStyle = {
    width: "100%",
    height: "300px",
    borderRadius: "0.5rem",
  };

  // Get restaurant location when cart changes
  useEffect(() => {
    const fetchRestaurantLocation = async () => {
      // Extract restaurant IDs from cart items - more reliable than cart.restaurantId
      const cartRestaurantIds = cart.items
        .filter((item) => item.restaurantId)
        .map((item) => item.restaurantId);

      // No items with restaurant IDs
      if (cartRestaurantIds.length === 0) {
        console.warn("No restaurant IDs found in cart items");
        return;
      }

      // Check if all items have the same restaurant ID (they should)
      const allSameRestaurant = cartRestaurantIds.every(
        (id) => id === cartRestaurantIds[0]
      );
      if (!allSameRestaurant) {
        console.error(
          "Mixed restaurant items detected in cart",
          cartRestaurantIds
        );
        // You could handle this situation (perhaps show an error or use the most common ID)
      }

      // Use the first restaurant ID from cart items
      const restaurantId = cartRestaurantIds[0];
      setRestaurantId(restaurantId);

      try {
        const response = await restaurantService.getRestaurantById(
          restaurantId,
          currentUser?.token
        );

        if (response.ok) {
          const restaurant = await response.json();
          console.log("Restaurant data:", restaurant);

          if (restaurant) {
            setRestaurantLocation({
              lat: parseFloat(restaurant.latitude),
              lng: parseFloat(restaurant.longitude),
              name: restaurant.name,
              address: restaurant.address || restaurant.formattedAddress,
              geoLocation: restaurant.location,
            });
          } else {
            console.error("Restaurant not found in the response");
          }
        }
      } catch (error) {
        console.error("Error fetching restaurant location:", error);
      }
    };

    // Only fetch if cart has items
    if (cart.items.length > 0) {
      fetchRestaurantLocation();
    }
  }, [cart.items, currentUser?.token]);

  // Custom Stepper Component
  const CheckoutStepper = ({ currentStep, steps }) => {
    return (
      <div className="max-w-5xl mx-auto mb-8">
        <ol className="flex items-center w-full">
          {steps.map((step, index) => {
            const isActive = currentStep === index;
            const isCompleted = currentStep > index;

            return (
              <li
                key={index}
                className={`flex items-center ${
                  index !== steps.length - 1 ? "w-full" : ""
                }`}
              >
                <div
                  className={`flex items-center justify-center w-10 h-10 rounded-full 
                ${
                  isActive
                    ? "bg-blue-600 text-white"
                    : isCompleted
                    ? "bg-green-500 text-white"
                    : "bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300"
                }`}
                >
                  {isCompleted ? (
                    <HiCheck className="w-5 h-5" />
                  ) : (
                    <span className="flex items-center justify-center">
                      {step.icon || index + 1}
                    </span>
                  )}
                </div>

                <div className="ml-2">
                  <span
                    className={`text-sm font-medium ${
                      isActive
                        ? "text-blue-600 dark:text-blue-400"
                        : isCompleted
                        ? "text-green-600 dark:text-green-400"
                        : "text-gray-500 dark:text-gray-400"
                    }`}
                  >
                    {step.label}
                  </span>
                </div>

                {index !== steps.length - 1 && (
                  <div
                    className={`flex-1 h-0.5 mx-4 ${
                      isCompleted
                        ? "bg-green-500"
                        : "bg-gray-200 dark:bg-gray-700"
                    }`}
                  ></div>
                )}
              </li>
            );
          })}
        </ol>
      </div>
    );
  };

  // Calculate distance between two points using Haversine formula
  const calculateDistance = (lat1, lng1, lat2, lng2) => {
    if (!lat1 || !lng1 || !lat2 || !lng2) return null;

    const R = 6371; // Radius of Earth in kilometers
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

  // Update distance whenever location changes
  useEffect(() => {
    if (
      restaurantLocation &&
      formData.deliveryLocation.latitude &&
      formData.deliveryLocation.longitude
    ) {
      const distance = calculateDistance(
        parseFloat(formData.deliveryLocation.latitude),
        parseFloat(formData.deliveryLocation.longitude),
        restaurantLocation.lat,
        restaurantLocation.lng
      );

      setDistanceToRestaurant(distance);

      // Validate distance
      if (distance > 20) {
        setLocationError(
          `This location is too far (${distance.toFixed(
            1
          )} km) from the restaurant. Maximum delivery distance is 20 km.`
        );
      } else {
        setLocationError("");
      }
    }
  }, [formData.deliveryLocation, restaurantLocation]);

  // Redirect if cart is empty
  useEffect(() => {
    if (cart.items.length === 0 && !orderSuccess) {
      navigate("/");
    }
  }, [cart.items, navigate, orderSuccess]);

  // Initialize payment intent when shifting to payment step
  useEffect(() => {
    if (
      showPaymentStep &&
      paymentMethod === "card" &&
      orderData &&
      !clientSecret
    ) {
      initializePayment();
    }
  }, [showPaymentStep, orderData]);

  // Handle location selection from Google Places
  const handleLocationSelect = (place) => {
    setLocationValue(place);

    if (place?.value?.place_id && isMapLoaded) {
      const geocoder = new window.google.maps.Geocoder();
      geocoder.geocode({ placeId: place.value.place_id }, (results, status) => {
        if (status === "OK" && results[0]) {
          const lat = results[0].geometry.location.lat();
          const lng = results[0].geometry.location.lng();
          const coordinates = sanitizeCoordinates({ lat, lng });
          const sanitizedAddress = sanitizeInput(place.label);

          // Update delivery location
          setFormData({
            ...formData,
            deliveryLocation: {
              ...formData.deliveryLocation,
              latitude: coordinates.lat,
              longitude: coordinates.lng,
              address: sanitizedAddress,
              formattedAddress: sanitizedAddress,
            },
            // Also update address fields based on the selected place
            address: {
              ...formData.address,
              street: sanitizeInput(extractAddressComponent(
                results[0],
                "route",
                "street_number"
              )),
              city: sanitizeInput(extractAddressComponent(results[0], "locality")),
              state: sanitizeInput(extractAddressComponent(
                results[0],
                "administrative_area_level_1"
              )),
              zipCode: sanitizeInput(extractAddressComponent(results[0], "postal_code")),
            },
          });
        }
      });
    }
  };

  // Helper function to extract address components from Google Maps result
  const extractAddressComponent = (result, type, additionalType = null) => {
    const component = result.address_components.find((component) =>
      component.types.includes(type)
    );

    if (additionalType && !component) {
      const additionalComponent = result.address_components.find((component) =>
        component.types.includes(additionalType)
      );

      if (additionalComponent) return additionalComponent.long_name;
    }

    return component ? component.long_name : "";
  };

  // Handle map click to set location
  const handleMapClick = (event) => {
    const lat = event.latLng.lat();
    const lng = event.latLng.lng();
    const coordinates = sanitizeCoordinates({ lat, lng });

    // Update coordinates immediately
    setFormData({
      ...formData,
      deliveryLocation: {
        ...formData.deliveryLocation,
        latitude: coordinates.lat,
        longitude: coordinates.lng,
      },
    });

    // Only attempt reverse geocoding if map is loaded
    if (isMapLoaded) {
      const geocoder = new window.google.maps.Geocoder();
      geocoder.geocode({ location: { lat: coordinates.lat, lng: coordinates.lng } }, (results, status) => {
        if (status === "OK" && results[0]) {
          const sanitizedAddress = sanitizeInput(results[0].formatted_address);
          setFormData({
            ...formData,
            deliveryLocation: {
              latitude: coordinates.lat,
              longitude: coordinates.lng,
              address: sanitizedAddress,
              formattedAddress: sanitizedAddress,
            },
            // Update address fields based on the geocoded result
            address: {
              street: sanitizeInput(extractAddressComponent(
                results[0],
                "route",
                "street_number"
              )),
              city: sanitizeInput(extractAddressComponent(results[0], "locality")),
              state: sanitizeInput(extractAddressComponent(
                results[0],
                "administrative_area_level_1"
              )),
              zipCode: sanitizeInput(extractAddressComponent(results[0], "postal_code")),
            },
          });
        }
      });
    }
  };

  const validateForm = () => {
    const errors = {};

    if (!formData.fullName) errors.fullName = "Full name is required";
    if (!formData.email) errors.email = "Email is required";
    else if (!/\S+@\S+\.\S+/.test(formData.email))
      errors.email = "Email is invalid";

    if (!formData.phone) errors.phone = "Phone number is required";

    if (!formData.address.street) errors.street = "Street address is required";
    if (!formData.address.city) errors.city = "City is required";
    if (!formData.address.state) errors.state = "State is required";
    if (!formData.address.zipCode) errors.zipCode = "ZIP code is required";

    // Validate location
    if (
      !formData.deliveryLocation.latitude ||
      !formData.deliveryLocation.longitude
    ) {
      errors.location = "Please select a delivery location on the map";
    } else if (distanceToRestaurant > 20) {
      errors.location = `Selected location is too far (${distanceToRestaurant.toFixed(
        1
      )} km) from the restaurant. Maximum delivery distance is 20 km.`;
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    let sanitizedValue = value;

    // Apply appropriate sanitization based on field type
    if (name === "email" || name === "contactInfo.email") {
      sanitizedValue = sanitizeEmail(value);
    } else if (name === "phone" || name === "contactInfo.phone") {
      sanitizedValue = sanitizePhone(value);
    } else {
      sanitizedValue = sanitizeInput(value);
    }

    if (name.includes(".")) {
      const [parent, child] = name.split(".");
      setFormData({
        ...formData,
        [parent]: {
          ...formData[parent],
          [child]: sanitizedValue,
        },
      });
    } else {
      setFormData({
        ...formData,
        [name]: sanitizedValue,
      });
    }
  };

  // Initialize payment intent for card payments
  const initializePayment = async () => {
    try {
      setPaymentProcessing(true);
      const response = await paymentService.createPaymentIntent(
        {
          amount: cart.total,
          orderId: `pending_${Date.now()}`,
          customerEmail: formData.email || currentUser?.email,
        },
        currentUser?.token
      );

      setClientSecret(response.clientSecret);
      setPaymentError("");
    } catch (error) {
      console.error("Error initializing payment:", error);
      setPaymentError("Failed to initialize payment. Please try again.");
    } finally {
      setPaymentProcessing(false);
    }
  };

  // Handle form submission
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      window.scrollTo(0, 0);
      return;
    }

    // Create order object
    const order = {
      userId: currentUser?.id,
      restaurantId: restaurantId,
      items: cart.items.map((item) => ({
        itemId: item.id,
        name: sanitizeInput(item.name),
        quantity: item.quantity,
        price: item.price,
        addOns: (item.addOns || []).map(addon => ({
          ...addon,
          name: sanitizeInput(addon.name)
        })),
        itemTotal: item.itemTotal,
      })),
      subtotal: cart.subtotal,
      taxAmount: cart.taxAmount,
      deliveryFee: cart.deliveryFee || 0,
      discount: cart.discountAmount,
      total: cart.total,
      promotion: cart.appliedPromotion
        ? {
            code: sanitizeInput(cart.appliedPromotion.code),
            discountAmount: cart.discountAmount,
          }
        : null,
      deliveryAddress: {
        street: sanitizeInput(formData.address.street),
        city: sanitizeInput(formData.address.city),
        state: sanitizeInput(formData.address.state),
        zipCode: sanitizeInput(formData.address.zipCode),
      },
      deliveryLocation: {
        latitude: sanitizeCoordinates({
          latitude: formData.deliveryLocation.latitude,
          longitude: formData.deliveryLocation.longitude
        }).latitude,
        longitude: sanitizeCoordinates({
          latitude: formData.deliveryLocation.latitude,
          longitude: formData.deliveryLocation.longitude
        }).longitude,
        address: sanitizeInput(
          formData.deliveryLocation.formattedAddress ||
          formData.deliveryLocation.address
        ),
      },
      restaurantLocation: {
        latitude: sanitizeCoordinates({
          lat: restaurantLocation?.lat,
          lng: restaurantLocation?.lng
        }).lat,
        longitude: sanitizeCoordinates({
          lat: restaurantLocation?.lat,
          lng: restaurantLocation?.lng
        }).lng,
        address: sanitizeInput(restaurantLocation?.address),
        name: sanitizeInput(restaurantLocation?.name),
      },
      deliveryInstructions: sanitizeInput(formData.deliveryInstructions),
      contactInfo: {
        name: sanitizeInput(formData.fullName),
        email: sanitizeEmail(formData.email),
        phone: sanitizePhone(formData.phone),
      },
      paymentMethod: sanitizeInput(paymentMethod),
      status: "pending",
      paymentStatus: paymentMethod === "cash" ? "pending" : "awaiting_payment",
    };

    setOrderData(order);

    // If cash payment, create order immediately
    if (paymentMethod === "cash") {
      await createOrder(order);
    }
    // If card payment, move to payment step
    else if (paymentMethod === "card") {
      setShowPaymentStep(true);
      setCheckoutStep(1);
    }
  };

  // Create order in database
  const createOrder = async (orderData) => {
    setOrderProcessing(true);
    setOrderError("");

    try {
      const response = await orderService.createOrder(
        orderData,
        currentUser?.token
      );

      if (!response.ok) {
        throw new Error("Failed to place order");
      }

      const data = await response.json();
      console.log("Order created successfully:", data);
      setOrderId(data.id || "unknown");
      setSubtotal(data.subtotal);
      setOrderSuccess(true);
      clearCart();
    } catch (error) {
      console.error("Error placing order:", error);
      setOrderError("Failed to place order. Please try again.");
    } finally {
      setOrderProcessing(false);
    }
  };

  // Handle successful payment
  const handlePaymentSuccess = async (paymentIntent) => {
    try {
      setPaymentMessage(
        "Payment processed successfully! Completing your order..."
      );

      // Verify payment was successful
      const paymentRecord = await paymentService.confirmPayment(
        { paymentIntentId: paymentIntent.id },
        currentUser?.token
      );

      if (paymentRecord.paymentStatus === "succeeded") {
        // Update order data with payment information
        const orderWithPayment = {
          ...orderData,
          paymentStatus: "paid",
          paymentDetails: {
            paymentIntentId: paymentIntent.id,
            paymentMethod: "card",
            paymentAmount: cart.total,
          },
        };

        // Create the order after successful payment
        await createOrder(orderWithPayment);

        // Show success message temporarily
        setTimeout(() => {
          setPaymentSuccess(true);
          setShowPaymentStep(false);
          setCheckoutStep(2);
        }, 1500);
      } else {
        setPaymentError(
          "Payment verification failed: " +
            (paymentRecord.errorMessage || "Unknown error")
        );
      }
    } catch (error) {
      console.error("Payment confirmation error:", error);
      setPaymentError("Payment confirmation failed: " + error.message);
    }
  };

  // Get map center based on available data
  const getMapCenter = () => {
    // If user has selected a delivery location, use that
    if (
      formData.deliveryLocation.latitude &&
      formData.deliveryLocation.longitude
    ) {
      return {
        lat: parseFloat(formData.deliveryLocation.latitude),
        lng: parseFloat(formData.deliveryLocation.longitude),
      };
    }

    // If restaurant location is available, use that
    if (restaurantLocation) {
      return {
        lat: restaurantLocation.lat,
        lng: restaurantLocation.lng,
      };
    }

    // Default to a fallback location
    return { lat: 6.9271, lng: 79.8612 }; // Colombo, Sri Lanka
  };

  const formatPrice = (value) => {
    const numValue = Number(value);
    return isNaN(numValue) ? "0.00" : numValue.toFixed(2);
  };

  // Show order confirmation when successful
  if (orderSuccess) {
    return (
      <div className="container mx-auto my-8 px-4">
        <Card className="max-w-2xl mx-auto border-0 shadow-lg overflow-hidden">
          <div className="bg-gradient-to-r from-green-500 to-teal-500 text-white p-8 text-center -mx-4 -mt-4">
            <div className="flex justify-center mb-4">
              <div className="bg-white rounded-full p-4 shadow-md">
                <HiCheck className="w-12 h-12 text-green-500" />
              </div>
            </div>
            <h1 className="text-3xl font-bold mb-2">
              Order Placed Successfully!
            </h1>
            <p className="text-white text-opacity-90 mb-1">
              Thank you for your order. Your food is being prepared!
            </p>
            {paymentMethod === "card" && (
              <div className="inline-block bg-white bg-opacity-20 text-sm px-3 py-1 rounded-full mt-1">
                <div className="flex items-center">
                  <HiOutlineCheckCircle className="mr-1" />
                  Payment Completed
                </div>
              </div>
            )}
          </div>

          <div className="p-6">
            <div className="bg-gray-50 dark:bg-gray-800 p-5 rounded-lg mb-5 shadow-inner">
              <h2 className="font-semibold text-lg mb-3 flex items-center">
                <HiOutlineDocumentText className="mr-2 text-green-600" />
                Order Details
              </h2>

              <div className="space-y-2 text-sm">
                <div className="flex justify-between py-2 border-b border-gray-200 dark:border-gray-700">
                  <span className="font-medium flex items-center">
                    <Badge color="purple" className="mr-2">
                      Order ID
                    </Badge>
                  </span>
                  <span className="text-gray-700 dark:text-gray-300 font-mono">
                    {orderId || "N/A"}
                  </span>
                </div>

                <div className="flex justify-between py-2 border-b border-gray-200 dark:border-gray-700">
                  <span className="font-medium">Payment Method:</span>
                  <span className="flex items-center">
                    {paymentMethod === "card" ? (
                      <>
                        <HiCreditCard className="mr-1 text-blue-500" />
                        Credit/Debit Card
                      </>
                    ) : (
                      <>
                        <HiCash className="mr-1 text-green-500" />
                        Cash on Delivery
                      </>
                    )}
                  </span>
                </div>

                <div className="flex justify-between py-2 border-b border-gray-200 dark:border-gray-700">
                  <span className="font-medium">Total Amount:</span>
                  <span className="text-gray-900 dark:text-white font-bold">
                    ${formatPrice(subtotal)}
                  </span>
                </div>

                <div className="flex justify-between py-2 border-b border-gray-200 dark:border-gray-700">
                  <span className="font-medium">Email:</span>
                  <span className="text-gray-700 dark:text-gray-300">
                    {formData.email}
                  </span>
                </div>

                <div className="flex justify-between py-2">
                  <span className="font-medium">Delivery Address:</span>
                  <span className="text-gray-700 dark:text-gray-300 text-right">
                    {formData.address.street}, {formData.address.city}
                  </span>
                </div>
              </div>
            </div>

            <div className="flex items-center justify-center p-4 bg-gray-50 dark:bg-gray-800 rounded-lg mb-6">
              <HiMail className="text-gray-500 mr-2" />
              <p className="text-gray-600 dark:text-gray-400 text-sm">
                A confirmation email has been sent with your order details
              </p>
            </div>

            <div className="flex flex-col sm:flex-row justify-center gap-4 mt-8">
              <Button
                color="light"
                onClick={() => navigate("/restaurants")}
                className="flex items-center justify-center"
              >
                <HiChevronLeft className="mr-1" /> Browse Restaurants
              </Button>

              <Button
                gradientDuoTone="greenToBlue"
                onClick={() => navigate("/dashboard?tab=my-orders")}
                className="flex items-center justify-center"
              >
                <HiClock className="mr-1" /> Track Order
              </Button>
            </div>
          </div>
        </Card>
      </div>
    );
  }

  // Show payment processing message
  if (paymentMessage) {
    return (
      <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 z-50">
        <div className="bg-white dark:bg-gray-800 p-8 rounded-xl shadow-2xl max-w-md w-full mx-4">
          <div className="flex flex-col items-center text-center">
            <div className="bg-green-100 rounded-full p-4 mb-4">
              <HiCheck className="w-10 h-10 text-green-500" />
            </div>

            <h2 className="text-2xl font-bold mb-3 text-green-700 dark:text-green-400">
              Payment Successful!
            </h2>

            <p className="text-gray-700 dark:text-gray-300 mb-4">
              {paymentMessage}
            </p>

            <div className="flex items-center justify-center space-x-2 mt-2">
              <Spinner size="md" color="success" />
              <span className="text-gray-600 dark:text-gray-400 text-sm">
                Processing your order...
              </span>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto my-8 px-4">
      <div className="max-w-5xl mx-auto mb-8">
        <CheckoutStepper
          currentStep={checkoutStep}
          steps={[
            {
              label: "Delivery Information",
              icon: <HiOutlineLocationMarker className="w-5 h-5" />,
            },
            {
              label: "Payment",
              icon: <HiOutlineCreditCard className="w-5 h-5" />,
            },
            {
              label: "Order Complete",
              icon: <HiOutlineCheckCircle className="w-5 h-5" />,
            },
          ]}
        />
      </div>

      <h1 className="text-2xl font-bold mb-6 text-gray-900 dark:text-white">
        {showPaymentStep ? "Complete Payment" : "Checkout"}
      </h1>

      {orderError && (
        <Alert color="failure" className="mb-6">
          {orderError}
        </Alert>
      )}

      {paymentError && (
        <Alert color="failure" className="mb-6">
          {paymentError}
        </Alert>
      )}

      {/* Payment Step */}
      {showPaymentStep && (
        <div className="max-w-3xl mx-auto">
          <Button
            color="light"
            onClick={() => {
              setShowPaymentStep(false);
              setCheckoutStep(0);
              setClientSecret("");
              setPaymentError("");
            }}
            className="mb-4"
          >
            <HiChevronLeft className="mr-1" /> Back to Delivery Information
          </Button>

          <Card className="mb-6">
            <div className="bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-gray-800 dark:to-gray-700 p-4 -mx-4 -mt-4 mb-4 border-b border-gray-200 dark:border-gray-700">
              <h2 className="font-bold text-lg flex items-center">
                <HiCreditCard className="mr-2 text-blue-600" />
                Complete Payment
              </h2>
              <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
                Your order will be confirmed after successful payment
              </p>
            </div>

            {paymentProcessing ? (
              <div className="flex flex-col items-center justify-center py-8">
                <Spinner size="lg" />
                <p className="text-gray-600 dark:text-gray-400 mt-4">
                  Preparing secure payment form...
                </p>
              </div>
            ) : clientSecret ? (
              <StripePaymentWrapper
                clientSecret={clientSecret}
                onSuccess={handlePaymentSuccess}
              />
            ) : (
              <div className="text-center py-6">
                <p className="text-red-500">
                  Unable to initialize payment. Please try again.
                </p>
                <Button
                  color="blue"
                  onClick={initializePayment}
                  className="mt-4"
                >
                  Retry
                </Button>
              </div>
            )}
          </Card>

          {/* Order Summary Preview */}
          <Card className="mb-6">
            <h3 className="font-medium text-gray-800 dark:text-white flex items-center mb-3">
              <HiShoppingBag className="mr-2 text-blue-600" /> Order Summary
            </h3>

            <div className="space-y-2 text-sm">
              <div className="flex justify-between pb-2 border-b border-gray-200 dark:border-gray-700">
                <span>
                  {cart.items.length} item{cart.items.length !== 1 ? "s" : ""}
                </span>
                <span>${formatPrice(cart.subtotal)}</span>
              </div>

              {cart.discountAmount > 0 && (
                <div className="flex justify-between pb-2 text-green-600 dark:text-green-400">
                  <span>Discount</span>
                  <span>-${formatPrice(cart.discountAmount)}</span>
                </div>
              )}

              <div className="flex justify-between pb-2">
                <span>Tax + Delivery</span>
                <span>
                  ${formatPrice(cart.taxAmount + (cart.deliveryFee || 0))}
                </span>
              </div>

              <div className="flex justify-between pt-2 font-bold border-t border-gray-200 dark:border-gray-700">
                <span>Total</span>
                <span>${formatPrice(cart.total)}</span>
              </div>
            </div>
          </Card>
        </div>
      )}

      {/* Delivery Information Step */}
      {!showPaymentStep && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Order Details Form */}
          <div className="lg:col-span-2">
            <form onSubmit={handleSubmit}>
              <ContactInfoForm
                formData={formData}
                formErrors={formErrors}
                handleChange={handleChange}
              />

              {/* Delivery Location Card with Google Maps */}
              <DeliveryLocationMap
                formData={formData}
                formErrors={formErrors}
                locationValue={locationValue}
                handleLocationSelect={handleLocationSelect}
                isMapLoaded={isMapLoaded}
                setIsMapLoaded={setIsMapLoaded}
                restaurantLocation={restaurantLocation}
                distanceToRestaurant={distanceToRestaurant}
                handleMapClick={handleMapClick}
                getMapCenter={getMapCenter}
                mapContainerStyle={mapContainerStyle}
              />

              <DeliveryAddressForm
                formData={formData}
                formErrors={formErrors}
                handleChange={handleChange}
              />

              <PaymentMethodSelector
                paymentMethod={paymentMethod}
                setPaymentMethod={setPaymentMethod}
              />

              <div className="flex justify-between mt-6">
                <Button color="light" onClick={() => navigate(-1)}>
                  <HiChevronLeft className="mr-2" /> Back
                </Button>

                <Button
                  type="submit"
                  color="success"
                  disabled={
                    orderProcessing ||
                    (distanceToRestaurant && distanceToRestaurant > 20)
                  }
                  className="px-8"
                >
                  {orderProcessing ? (
                    <>
                      <Spinner size="sm" className="mr-3" />
                      Processing...
                    </>
                  ) : (
                    <>
                      {paymentMethod === "card"
                        ? "Proceed to Payment"
                        : "Place Order"}
                      <HiArrowSmRight className="ml-2" />
                    </>
                  )}
                </Button>
              </div>
            </form>
          </div>

          {/* Order Summary */}
          <div className="lg:col-span-1">
            <OrderSummary
              cart={cart}
              restaurantLocation={restaurantLocation}
              formatPrice={formatPrice}
            />
          </div>
        </div>
      )}
    </div>
  );
}
