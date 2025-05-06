import React, { useState, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import {
  Button,
  Card,
  Spinner,
  Table,
  Badge,
  TextInput,
  Dropdown,
  Alert,
  Modal,
} from "flowbite-react";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import { HiOutlineIdentification, HiOutlineClipboard } from "react-icons/hi";
import {
  HiOutlineRefresh,
  HiOutlineSearch,
  HiOutlineEye,
  HiFilter,
  HiInformationCircle,
  HiOutlineSortAscending,
  HiOutlineSortDescending,
  HiCheck,
  HiX,
  HiOutlineClock,
  HiOutlineClipboardCheck,
  HiOutlineClipboardList,
  HiOutlineTruck,
  HiOutlineLocationMarker,
  HiOutlinePhone,
  HiOutlineMail,
  HiOutlineUser,
  HiHome,
} from "react-icons/hi";
import {
  FaReceipt,
  FaShoppingBag,
  FaUtensils,
  FaRegCalendarAlt,
  FaMoneyBillWave,
  FaHistory,
} from "react-icons/fa";
import { orderService } from "../service/orderService";
import ReactPaginate from "react-paginate";
import LoadingSpinner from "./LoadingSpinner";
import OrderStatusTimeline from "./sub-components/order-management/OrderStatusTimeline";
import { useNavigate } from "react-router-dom";
import { useCart } from "../context/CartContext";

export default function UserOrderTracking() {
  const { currentUser } = useSelector((state) => state.user);
  const [orders, setOrders] = useState([]);
  const [filteredOrders, setFilteredOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentTab, setCurrentTab] = useState("all");
  const [searchTerm, setSearchTerm] = useState("");
  const [sortOrder, setSortOrder] = useState("desc");
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [pageNumber, setPageNumber] = useState(0);
  const [reordering, setReordering] = useState(false);
  const { addToCart, clearCart } = useCart();
  const itemsPerPage = 5;
  const dispatch = useDispatch();
  const navigate = useNavigate();

  // Order status counts
  const [totalOrders, setTotalOrders] = useState(0);
  const [activeOrders, setActiveOrders] = useState(0);
  const [completedOrders, setCompletedOrders] = useState(0);

  // Status display configs
  const statusColors = {
    PENDING: "warning",
    CONFIRMED: "info",
    PREPARING: "purple",
    READY_FOR_PICKUP: "success",
    OUT_FOR_DELIVERY: "indigo",
    DELIVERED: "success",
    CANCELLED: "failure",
  };

  const statusIcons = {
    PENDING: <HiOutlineClock className="mr-1" />,
    CONFIRMED: <HiOutlineClipboardCheck className="mr-1" />,
    PREPARING: <FaUtensils className="mr-1" />,
    READY_FOR_PICKUP: <HiOutlineClipboardList className="mr-1" />,
    OUT_FOR_DELIVERY: <HiOutlineTruck className="mr-1" />,
    DELIVERED: <HiCheck className="mr-1" />,
    CANCELLED: <HiX className="mr-1" />,
  };

  const formatDate = (dateString) => {
    const options = {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    };
    return new Date(dateString).toLocaleString("en-US", options);
  };

  // Calculate total pages for pagination
  const pageCount = Math.ceil(filteredOrders.length / itemsPerPage);
  const displayOrders = filteredOrders.slice(
    pageNumber * itemsPerPage,
    (pageNumber + 1) * itemsPerPage
  );

  const handlePageChange = ({ selected }) => {
    setPageNumber(selected);
  };

  // Fetch orders when component mounts
  useEffect(() => {
    if (currentUser?.token) {
      fetchOrders();
    }
  }, [currentUser]);

  // Filter orders when tab changes or search term changes
  useEffect(() => {
    filterOrders();
  }, [orders, currentTab, searchTerm]);

  const fetchOrders = async () => {
    try {
      setLoading(true);
      const response = await orderService.getOrdersByUserId(
        currentUser.id,
        currentUser.token
      );

      if (response.ok) {
        const data = await response.json();
        // Sort by date desc by default
        const sortedOrders = data.sort(
          (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
        );
        setOrders(sortedOrders);
        setFilteredOrders(sortedOrders);
        setTotalOrders(sortedOrders.length);

        // Calculate active and completed orders
        const active = sortedOrders.filter((order) =>
          [
            "PENDING",
            "CONFIRMED",
            "PREPARING",
            "READY_FOR_PICKUP",
            "OUT_FOR_DELIVERY",
          ].includes(order.status)
        ).length;
        setActiveOrders(active);

        const completed = sortedOrders.filter(
          (order) => order.status === "DELIVERED"
        ).length;
        setCompletedOrders(completed);
      } else {
        throw new Error("Failed to fetch orders");
      }
    } catch (err) {
      setError(`Error: ${err.message}`);
      toast.error(`Failed to load orders: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const filterOrders = () => {
    if (!orders || !orders.length) return;

    let result = [...orders];

    // Filter by tab/status
    if (currentTab !== "all") {
      if (currentTab === "active") {
        result = result.filter((order) =>
          [
            "PENDING",
            "CONFIRMED",
            "PREPARING",
            "READY_FOR_PICKUP",
            "OUT_FOR_DELIVERY",
          ].includes(order.status)
        );
      } else if (currentTab === "completed") {
        result = result.filter((order) => order.status === "DELIVERED");
      } else if (currentTab === "cancelled") {
        result = result.filter((order) => order.status === "CANCELLED");
      } else {
        // Filter by specific status
        result = result.filter((order) => order.status === currentTab);
      }
    }

    // Filter by search term
    if (searchTerm) {
      const search = searchTerm.toLowerCase();
      result = result.filter(
        (order) =>
          order.id.toLowerCase().includes(search) ||
          (order.restaurant?.name &&
            order.restaurant.name.toLowerCase().includes(search)) ||
          order.status.toLowerCase().includes(search) ||
          (order.deliveryAddress &&
            `${order.deliveryAddress.street} ${order.deliveryAddress.city}`
              .toLowerCase()
              .includes(search))
      );
    }

    // Sort by date
    result.sort((a, b) => {
      const dateA = new Date(a.createdAt);
      const dateB = new Date(b.createdAt);
      return sortOrder === "desc" ? dateB - dateA : dateA - dateB;
    });

    setFilteredOrders(result);
    setPageNumber(0); // Reset to first page when filters change
  };

  const toggleSortOrder = () => {
    setSortOrder(sortOrder === "desc" ? "asc" : "desc");
  };

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
  };

  const resetFilters = () => {
    setSearchTerm("");
    setCurrentTab("all");
  };

  const openOrderDetails = (order) => {
    setSelectedOrder(order);
    setIsModalOpen(true);
  };

  // Calculate distance between two points using Haversine formula
  const calculateDistance = (lat1, lng1, lat2, lng2) => {
    if (!lat1 || !lng1 || !lat2 || !lng2) return 0;

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

  // Order status badge component for consistency
  const OrderStatusBadge = ({ status }) => (
    <Badge color={statusColors[status] || "gray"} className="whitespace-nowrap">
      {statusIcons[status]}
      {status.replace("_", " ")}
    </Badge>
  );

  // Get estimated delivery time based on status
  const getEstimatedDeliveryTime = (status) => {
    switch (status) {
      case "PENDING":
        return "Waiting for restaurant confirmation";
      case "CONFIRMED":
        return "15-30 minutes";
      case "PREPARING":
        return "20-35 minutes";
      case "READY_FOR_PICKUP":
        return "10-15 minutes";
      case "OUT_FOR_DELIVERY":
        return "5-10 minutes";
      case "DELIVERED":
        return "Delivered";
      case "CANCELLED":
        return "Cancelled";
      default:
        return "Unknown";
    }
  };
  
  if (loading && !orders.length) {
    return <LoadingSpinner />;
  }

  const handleReorder = async (order) => {
    try {
      setReordering(true);
      
      // Use the CartContext function directly
      clearCart();
      
      // Check if order has items
      if (!order.items || order.items.length === 0) {
        toast.error("Cannot reorder: this order has no items");
        return;
      }
      
      // Set restaurant data first
      const restaurantName = order.restaurant?.name || order.restaurantLocation?.name || "Restaurant";
      
      // Add each item from the order to the cart using the context function
      order.items.forEach((item) => {
        // Create cart item matching the expected structure
        const cartItem = {
          id: item.itemId, // CartContext expects 'id', not 'itemId'
          name: item.name,
          price: Number(item.price), // Ensure it's a number
          quantity: Number(item.quantity), // Ensure it's a number
          addOns: item.addOns || [],
          restaurantId: order.restaurantId,
          restaurantName: restaurantName
        };
        
        // Use the quantity directly as a parameter rather than in the item
        addToCart(cartItem, cartItem.quantity, cartItem.addOns);
      });
      
      // Close the modal
      setIsModalOpen(false);
      
      // Show success message
      toast.success("Items have been added to your cart!");
      
      // Wait a moment for the toast to show
      await new Promise((resolve) => setTimeout(resolve, 800));
      
      // Navigate to checkout page
      navigate('/checkout');
    } catch (error) {
      toast.error("Failed to reorder: " + error.message);
    } finally {
      setReordering(false);
    }
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <ToastContainer position="top-right" autoClose={3000} />

      {/* Header and Stats */}
      <div className="mb-6">
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between mb-6 gap-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-800 dark:text-white flex items-center">
              <FaHistory className="mr-3 text-blue-600" />
              My Order History
            </h1>
            <p className="text-gray-600 dark:text-gray-400 mt-1">
              Track and manage all your orders in one place
            </p>
          </div>
          <Button
            outline
            gradientDuoTone="purpleToBlue"
            onClick={fetchOrders}
            disabled={loading}
          >
            <HiOutlineRefresh
              className={`mr-2 h-5 w-5 ${loading ? "animate-spin" : ""}`}
            />
            Refresh Orders
          </Button>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
          <Card>
            <div className="flex justify-between items-center">
              <div>
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Total Orders
                </p>
                <h5 className="text-xl font-bold text-gray-900 dark:text-white">
                  {totalOrders}
                </h5>
              </div>
              <div className="rounded-full p-3 bg-blue-100 dark:bg-blue-900">
                <FaReceipt className="h-6 w-6 text-blue-600 dark:text-blue-400" />
              </div>
            </div>
          </Card>
          <Card>
            <div className="flex justify-between items-center">
              <div>
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Active Orders
                </p>
                <h5 className="text-xl font-bold text-gray-900 dark:text-white">
                  {activeOrders}
                </h5>
              </div>
              <div className="rounded-full p-3 bg-yellow-100 dark:bg-yellow-900">
                <HiOutlineClock className="h-6 w-6 text-yellow-600 dark:text-yellow-400" />
              </div>
            </div>
          </Card>
          <Card>
            <div className="flex justify-between items-center">
              <div>
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400">
                  Completed Orders
                </p>
                <h5 className="text-xl font-bold text-gray-900 dark:text-white">
                  {completedOrders}
                </h5>
              </div>
              <div className="rounded-full p-3 bg-green-100 dark:bg-green-900">
                <HiCheck className="h-6 w-6 text-green-600 dark:text-green-400" />
              </div>
            </div>
          </Card>
        </div>

        {/* Enhanced Filter Section */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
          <div className="col-span-1 md:col-span-2">
            <TextInput
              type="text"
              placeholder="Search by restaurant, order ID, status..."
              value={searchTerm}
              onChange={handleSearchChange}
              icon={HiOutlineSearch}
              className="w-full"
            />
          </div>

          <div className="flex gap-2">
            <Dropdown
              label={
                <div className="flex items-center">
                  <HiFilter className="mr-2" />
                  {currentTab === "all"
                    ? "All Orders"
                    : currentTab === "active"
                    ? "Active Orders"
                    : currentTab === "completed"
                    ? "Completed Orders"
                    : currentTab === "cancelled"
                    ? "Cancelled Orders"
                    : `${currentTab} Orders`}
                </div>
              }
              color="light"
              className="w-full"
            >
              <Dropdown.Item onClick={() => setCurrentTab("all")}>
                All Orders
              </Dropdown.Item>
              <Dropdown.Divider />
              <Dropdown.Item onClick={() => setCurrentTab("active")}>
                Active Orders
              </Dropdown.Item>
              <Dropdown.Item onClick={() => setCurrentTab("PENDING")}>
                {statusIcons["PENDING"]} Pending Orders
              </Dropdown.Item>
              <Dropdown.Item onClick={() => setCurrentTab("CONFIRMED")}>
                {statusIcons["CONFIRMED"]} Confirmed Orders
              </Dropdown.Item>
              <Dropdown.Item onClick={() => setCurrentTab("PREPARING")}>
                {statusIcons["PREPARING"]} Preparing Orders
              </Dropdown.Item>
              <Dropdown.Item onClick={() => setCurrentTab("READY_FOR_PICKUP")}>
                {statusIcons["READY_FOR_PICKUP"]} Ready For Pickup
              </Dropdown.Item>
              <Dropdown.Item onClick={() => setCurrentTab("OUT_FOR_DELIVERY")}>
                {statusIcons["OUT_FOR_DELIVERY"]} Out For Delivery
              </Dropdown.Item>
              <Dropdown.Divider />
              <Dropdown.Item onClick={() => setCurrentTab("completed")}>
                {statusIcons["DELIVERED"]} Completed Orders
              </Dropdown.Item>
              <Dropdown.Item onClick={() => setCurrentTab("cancelled")}>
                {statusIcons["CANCELLED"]} Cancelled Orders
              </Dropdown.Item>
            </Dropdown>

            <Button
              color="light"
              onClick={toggleSortOrder}
              className="whitespace-nowrap"
            >
              {sortOrder === "desc" ? (
                <HiOutlineSortDescending className="mr-1" />
              ) : (
                <HiOutlineSortAscending className="mr-1" />
              )}
              {sortOrder === "desc" ? "Newest" : "Oldest"}
            </Button>

            {(searchTerm || currentTab !== "all") && (
              <Button color="light" onClick={resetFilters}>
                Clear
              </Button>
            )}
          </div>
        </div>
      </div>

      {/* Orders Display Section */}
      {loading ? (
        <LoadingSpinner />
      ) : error ? (
        <Alert color="failure">
          <span className="font-medium">Error!</span> {error}
        </Alert>
      ) : (
        <>
          {filteredOrders.length > 0 ? (
            <div className="space-y-6">
              {/* Grid of order cards */}
              <div className="grid gap-6">
                {displayOrders.map((order) => (
                  <Card key={order.id} className="overflow-hidden">
                    <div className="flex flex-col md:flex-row justify-between gap-4">
                      {/* Order basic info */}
                      <div className="space-y-3 flex-1">
                        <div className="flex justify-between items-start">
                          <div>
                            <h5 className="text-lg font-bold tracking-tight text-gray-900 dark:text-white flex items-center gap-2">
                              <FaReceipt className="text-blue-600" />
                              {order.restaurant?.name || "Restaurant"}
                            </h5>
                            <p className="text-sm text-gray-500 dark:text-gray-400">
                              Order #{order.id.substring(0, 8)}...
                            </p>
                          </div>
                          <OrderStatusBadge status={order.status} />
                        </div>

                        <div className="flex items-center gap-2 text-sm">
                          <FaRegCalendarAlt className="text-gray-500" />
                          <span>{formatDate(order.createdAt)}</span>
                        </div>

                        <div className="flex gap-2 items-center">
                          <span className="font-medium text-gray-500">
                            Total:
                          </span>
                          <span className="font-bold text-lg">
                            ${order.total?.toFixed(2)}
                          </span>
                        </div>

                        <div className="flex items-center gap-1 text-sm text-gray-600 dark:text-gray-400">
                          <HiOutlineLocationMarker />
                          <span>
                            {order.deliveryAddress?.street},{" "}
                            {order.deliveryAddress?.city}
                          </span>
                        </div>

                        <div className="flex flex-wrap gap-2 mt-2">
                          <span className="text-sm text-gray-600 dark:text-gray-400">
                            {order.items?.length || 0}{" "}
                            {order.items?.length === 1 ? "item" : "items"}
                          </span>
                          {order.items?.slice(0, 2).map((item, i) => (
                            <Badge key={i} color="light" className="text-xs">
                              {item.quantity}x {item.name}
                            </Badge>
                          ))}
                          {order.items && order.items.length > 2 && (
                            <Badge color="light" className="text-xs">
                              +{order.items.length - 2} more
                            </Badge>
                          )}
                        </div>
                      </div>

                      {/* Right side with action button */}
                      <div className="flex flex-col justify-between items-end gap-4">
                        <div className="text-sm text-right">
                          <div className="text-gray-500">Estimated Time</div>
                          <div className="font-medium">
                            {getEstimatedDeliveryTime(order.status)}
                          </div>
                        </div>

                        <div className="flex flex-col gap-2">
                          <Button
                            color="light"
                            onClick={() => openOrderDetails(order)}
                            className="whitespace-nowrap"
                          >
                            <HiOutlineEye className="mr-2 h-4 w-4" />
                            View Details
                          </Button>

                          {/* For active orders, show a prominent "Track" button */}
                          {[
                            "PENDING",
                            "CONFIRMED",
                            "PREPARING",
                            "READY_FOR_PICKUP",
                            "OUT_FOR_DELIVERY",
                          ].includes(order.status) && (
                            <Button
                              gradientDuoTone="purpleToBlue"
                              size="sm"
                              onClick={() => openOrderDetails(order)}
                              className="whitespace-nowrap"
                            >
                              <HiOutlineTruck className="mr-2 h-4 w-4" />
                              Track Order
                            </Button>
                          )}
                        </div>
                      </div>
                    </div>
                  </Card>
                ))}
              </div>

              {/* Pagination */}
              {filteredOrders.length > itemsPerPage && (
                <div className="py-4 border-t border-gray-200 dark:border-gray-700">
                  <ReactPaginate
                    previousLabel={"Previous"}
                    nextLabel={"Next"}
                    pageCount={pageCount}
                    onPageChange={handlePageChange}
                    forcePage={pageNumber}
                    containerClassName="flex justify-center items-center space-x-1"
                    pageClassName="inline-flex"
                    pageLinkClassName="px-3 py-2 leading-tight text-gray-500 bg-white border border-gray-300 hover:bg-gray-100 hover:text-gray-700 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-white"
                    previousLinkClassName="px-3 py-2 leading-tight text-gray-500 bg-white border border-gray-300 rounded-l-lg hover:bg-gray-100 hover:text-gray-700 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-white"
                    nextLinkClassName="px-3 py-2 leading-tight text-gray-500 bg-white border border-gray-300 rounded-r-lg hover:bg-gray-100 hover:text-gray-700 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-white"
                    activeLinkClassName="!bg-blue-50 !text-blue-600 !border-blue-300 dark:!bg-gray-700 dark:!text-white"
                    disabledLinkClassName="opacity-50 cursor-not-allowed"
                    breakLabel="..."
                    breakLinkClassName="px-3 py-2 leading-tight text-gray-500 bg-white border border-gray-300 hover:bg-gray-100 hover:text-gray-700 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-white"
                  />
                </div>
              )}
            </div>
          ) : (
            <div className="text-center py-10">
              <div className="inline-block p-3 bg-gray-100 dark:bg-gray-700 rounded-full mb-4">
                <FaReceipt
                  size={30}
                  className="text-gray-400 dark:text-gray-500"
                />
              </div>
              <h3 className="text-lg font-medium text-gray-900 dark:text-white">
                No orders found
              </h3>
              <p className="text-gray-500 dark:text-gray-400 mt-2">
                {searchTerm || currentTab !== "all"
                  ? "No orders match your filter criteria."
                  : "Your do not have any orders."}
              </p>
            </div>
          )}
        </>
      )}

      {/* Order Details Modal */}
      {selectedOrder && (
        <Modal
          show={isModalOpen}
          size="3xl"
          onClose={() => setIsModalOpen(false)}
          dismissible
        >
          <Modal.Header className="text-xl">
            <div className="flex items-center">
              <FaReceipt className="mr-2 text-blue-600" />
              Order Details #{selectedOrder.id.substring(0, 8)}...
            </div>
          </Modal.Header>
          <Modal.Body>
            <div className="space-y-6">
              {/* Order Status and Tracking */}
              <Card className="mb-4">
                <div className="flex flex-col md:flex-row justify-between">
                  <div className="mb-4 md:mb-0">
                    <h5 className="text-lg font-bold tracking-tight text-gray-900 dark:text-white flex items-center">
                      <span className="mr-2">Order Status</span>
                      <OrderStatusBadge status={selectedOrder.status} />
                    </h5>
                    <p className="font-normal text-gray-700 dark:text-gray-400 mt-2">
                      {selectedOrder.status === "CANCELLED" ? (
                        <span className="text-red-500">
                          This order has been cancelled
                        </span>
                      ) : selectedOrder.status === "DELIVERED" ? (
                        <span className="text-green-500">
                          Order delivered successfully!
                        </span>
                      ) : (
                        <>
                          <span>Estimated time: </span>
                          <span className="font-medium">
                            {getEstimatedDeliveryTime(selectedOrder.status)}
                          </span>
                        </>
                      )}
                    </p>
                  </div>
                  {selectedOrder.status !== "CANCELLED" &&
                    selectedOrder.status !== "DELIVERED" && (
                      <div className="flex items-center">
                        <HiOutlineClock className="text-blue-500 mr-2" />
                        <span className="text-sm font-medium">
                          Order placed: {formatDate(selectedOrder.createdAt)}
                        </span>
                      </div>
                    )}
                </div>

                <div className="mt-4">
                  <OrderStatusTimeline status={selectedOrder.status} />
                </div>
              </Card>

              {/* Delivery Location and Map */}
              {selectedOrder.deliveryLocation ||
              selectedOrder.restaurantLocation ? (
                <Card>
                  <h5 className="text-lg font-bold tracking-tight text-gray-900 dark:text-white mb-4 flex items-center">
                    <HiOutlineTruck className="mr-2" />
                    Delivery Map
                  </h5>

                  {/* Static Map Display - Using Google Maps Static API */}
                  {selectedOrder.deliveryLocation &&
                    selectedOrder.restaurantLocation && (
                      <>
                        <div className="mb-4 aspect-[16/9] overflow-hidden rounded-lg">
                          <img
                            src={`https://maps.googleapis.com/maps/api/staticmap?size=600x300&zoom=14&markers=color:red|label:R|${selectedOrder.restaurantLocation.latitude},${selectedOrder.restaurantLocation.longitude}&markers=color:blue|label:D|${selectedOrder.deliveryLocation.latitude},${selectedOrder.deliveryLocation.longitude}&path=color:0x0000ff|weight:5|${selectedOrder.restaurantLocation.latitude},${selectedOrder.restaurantLocation.longitude}|${selectedOrder.deliveryLocation.latitude},${selectedOrder.deliveryLocation.longitude}&key=AIzaSyCms2-r4afPJIKiStBZUNuRx_4BdU2p9ps`}
                            alt="Delivery Map"
                            className="w-full h-full object-cover"
                          />
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-3">
                          <div className="p-3 bg-blue-50 dark:bg-gray-800 rounded-lg">
                            <div className="font-medium text-blue-700 dark:text-blue-400 mb-1">
                              Restaurant Location
                            </div>
                            <div className="text-sm text-gray-700 dark:text-gray-300">
                              {selectedOrder.restaurantLocation.name && (
                                <div className="font-medium mb-1">
                                  {selectedOrder.restaurantLocation.name}
                                </div>
                              )}
                              <div>
                                {selectedOrder.restaurantLocation.address ||
                                  "Address not available"}
                              </div>
                            </div>
                          </div>

                          <div className="p-3 bg-green-50 dark:bg-gray-800 rounded-lg">
                            <div className="font-medium text-green-700 dark:text-green-400 mb-1">
                              Your Delivery Location
                            </div>
                            <div className="text-sm text-gray-700 dark:text-gray-300">
                              {selectedOrder.deliveryLocation.address ||
                                "Address not available"}
                            </div>
                          </div>
                        </div>

                        {/* Calculate and show distance */}
                        <div className="mt-3 text-center">
                          <Badge color="indigo" size="xl">
                            <HiOutlineTruck className="mr-2" />
                            {calculateDistance(
                              selectedOrder.restaurantLocation.latitude,
                              selectedOrder.restaurantLocation.longitude,
                              selectedOrder.deliveryLocation.latitude,
                              selectedOrder.deliveryLocation.longitude
                            ).toFixed(1)}{" "}
                            km delivery distance
                          </Badge>
                        </div>
                      </>
                    )}

                  {/* If we only have restaurant location */}
                  {selectedOrder.restaurantLocation &&
                    !selectedOrder.deliveryLocation && (
                      <div className="p-3 bg-yellow-50 dark:bg-gray-800 rounded-lg text-center">
                        <div className="text-yellow-600 dark:text-yellow-400">
                          <HiInformationCircle className="inline-block mr-2 h-5 w-5" />
                          No delivery location coordinates available for this
                          order
                        </div>
                      </div>
                    )}

                  {/* If neither location is available */}
                  {!selectedOrder.restaurantLocation &&
                    !selectedOrder.deliveryLocation && (
                      <div className="p-3 bg-gray-50 dark:bg-gray-800 rounded-lg text-center">
                        <div className="text-gray-500">
                          No location data available for this order
                        </div>
                      </div>
                    )}
                </Card>
              ) : null}

              {/* Restaurant Info */}
              <Card>
                <h5 className="text-lg font-bold tracking-tight text-gray-900 dark:text-white mb-2 flex items-center">
                  <FaUtensils className="mr-2" />
                  Restaurant Information
                </h5>
                <div className="text-sm space-y-2">
                  <div className="font-medium text-lg">
                    {selectedOrder.restaurant?.name ||
                      selectedOrder.restaurantLocation?.name ||
                      "Restaurant"}
                  </div>
                  {selectedOrder.restaurant?.address && (
                    <div className="flex items-center gap-1">
                      <HiOutlineLocationMarker className="text-gray-500" />
                      <span>
                        {selectedOrder.restaurant.address.street},{" "}
                        {selectedOrder.restaurant.address.city}
                      </span>
                    </div>
                  )}
                  {selectedOrder.restaurantLocation?.address &&
                    !selectedOrder.restaurant?.address && (
                      <div className="flex items-center gap-1">
                        <HiOutlineLocationMarker className="text-gray-500" />
                        <span>{selectedOrder.restaurantLocation.address}</span>
                      </div>
                    )}
                  {selectedOrder.restaurant?.phone && (
                    <div className="flex items-center gap-1">
                      <HiOutlinePhone className="text-gray-500" />
                      <span>{selectedOrder.restaurant.phone}</span>
                    </div>
                  )}
                </div>
              </Card>

              {/* Order Info and Delivery */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <Card>
                  <h5 className="text-lg font-bold tracking-tight text-gray-900 dark:text-white mb-2 flex items-center">
                    <FaRegCalendarAlt className="mr-2" />
                    Order Information
                  </h5>
                  <div className="space-y-3 text-sm">
                    <div className="flex justify-between items-center">
                      <span className="text-gray-500 dark:text-gray-400">
                        Order ID:
                      </span>
                      <div className="relative flex items-center">
                        <span
                          className="font-mono text-sm bg-gray-100 dark:bg-gray-700 px-2 py-1 rounded-l flex items-center"
                          title={selectedOrder.id}
                        >
                          <HiOutlineIdentification className="mr-1.5 text-blue-500" />
                          {selectedOrder.id.substring(0, 8)}...
                          {selectedOrder.id.substring(
                            selectedOrder.id.length - 4
                          )}
                        </span>
                        <button
                          onClick={() => {
                            navigator.clipboard.writeText(selectedOrder.id);
                            toast.success("Order ID copied to clipboard!");
                          }}
                          className="bg-blue-500 hover:bg-blue-600 text-white px-2 py-1 rounded-r text-sm transition-colors"
                          title="Copy order ID"
                        >
                          <HiOutlineClipboard className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-500 dark:text-gray-400">
                        Date:
                      </span>
                      <span>{formatDate(selectedOrder.createdAt)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-500 dark:text-gray-400">
                        Payment Method:
                      </span>
                      <Badge color="purple" className="capitalize">
                        <FaMoneyBillWave className="mr-1" />
                        {selectedOrder.paymentMethod}
                      </Badge>
                    </div>
                    {/* Updated promotion display */}
                    {selectedOrder.promotion && (
                      <div className="flex justify-between">
                        <span className="text-gray-500 dark:text-gray-400">
                          Promotion:
                        </span>
                        <div className="flex items-center gap-2">
                          <Badge color="success">
                            {selectedOrder.promotion.code}
                          </Badge>
                          <span className="text-green-600">
                            -$
                            {selectedOrder.promotion.discountAmount.toFixed(2)}
                          </span>
                        </div>
                      </div>
                    )}
                  </div>
                </Card>

                <Card>
                  <h5 className="text-lg font-bold tracking-tight text-gray-900 dark:text-white mb-2 flex items-center">
                    <HiOutlineLocationMarker className="mr-2" />
                    Delivery Details
                  </h5>
                  <div className="space-y-3 text-sm">
                    <div>
                      <div className="font-medium">Delivery Address:</div>
                      <div>{selectedOrder.deliveryAddress?.street}</div>
                      <div>
                        {selectedOrder.deliveryAddress?.city},{" "}
                        {selectedOrder.deliveryAddress?.state}{" "}
                        {selectedOrder.deliveryAddress?.zipCode}
                      </div>
                    </div>
                    <div>
                      <div className="font-medium">Contact:</div>
                      <div className="flex items-center gap-1">
                        <HiOutlineUser className="text-gray-500" />
                        <span>{selectedOrder.contactInfo?.name}</span>
                      </div>
                      <div className="flex items-center gap-1">
                        <HiOutlinePhone className="text-gray-500" />
                        <span>{selectedOrder.contactInfo?.phone}</span>
                      </div>
                      <div className="flex items-center gap-1">
                        <HiOutlineMail className="text-gray-500" />
                        <span>{selectedOrder.contactInfo?.email}</span>
                      </div>
                    </div>
                    {selectedOrder.deliveryInstructions && (
                      <div className="mt-2 p-2 bg-gray-50 dark:bg-gray-700 rounded text-sm">
                        <div className="font-medium">
                          Delivery Instructions:
                        </div>
                        <div className="italic">
                          {selectedOrder.deliveryInstructions}
                        </div>
                      </div>
                    )}
                  </div>
                </Card>
              </div>

              {/* Order Items */}
              <Card>
                <h5 className="text-lg font-bold tracking-tight text-gray-900 dark:text-white mb-2 flex items-center">
                  <FaShoppingBag className="mr-2" />
                  Order Items
                </h5>
                <div className="overflow-x-auto">
                  <Table hoverable>
                    <Table.Head>
                      <Table.HeadCell>Item</Table.HeadCell>
                      <Table.HeadCell>Price</Table.HeadCell>
                      <Table.HeadCell>Quantity</Table.HeadCell>
                      <Table.HeadCell>Total</Table.HeadCell>
                    </Table.Head>
                    <Table.Body className="divide-y">
                      {selectedOrder.items?.map((item, index) => (
                        <React.Fragment key={index}>
                          <Table.Row>
                            <Table.Cell className="font-medium">
                              {item.name}
                            </Table.Cell>
                            <Table.Cell>${item.price?.toFixed(2)}</Table.Cell>
                            <Table.Cell>{item.quantity}</Table.Cell>
                            <Table.Cell className="font-medium">
                              ${item.itemTotal?.toFixed(2)}
                            </Table.Cell>
                          </Table.Row>
                          {/* Show add-ons for this item if any */}
                          {item.addOns && item.addOns.length > 0 && (
                            <Table.Row className="bg-gray-50 dark:bg-gray-800">
                              <Table.Cell colSpan={4} className="px-6 py-2">
                                <div className="text-xs text-gray-500 dark:text-gray-400">
                                  <span className="font-medium">Add-ons:</span>{" "}
                                  {item.addOns.map((addon, idx) => (
                                    <span key={idx} className="ml-2">
                                      {addon.name} (${addon.price?.toFixed(2)})
                                      {idx < item.addOns.length - 1 ? ", " : ""}
                                    </span>
                                  ))}
                                </div>
                              </Table.Cell>
                            </Table.Row>
                          )}
                        </React.Fragment>
                      ))}
                    </Table.Body>
                  </Table>
                </div>

                {/* Order Summary */}
                <div className="mt-4 border-t pt-4 space-y-1">
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600 dark:text-gray-400">
                      Subtotal:
                    </span>
                    <span>${selectedOrder.subtotal?.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600 dark:text-gray-400">
                      Tax:
                    </span>
                    <span>${selectedOrder.taxAmount?.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600 dark:text-gray-400">
                      Delivery Fee:
                    </span>
                    <span>${selectedOrder.deliveryFee?.toFixed(2)}</span>
                  </div>
                  {selectedOrder.discount > 0 && (
                    <div className="flex justify-between text-sm text-green-600 dark:text-green-400">
                      <span>Discount:</span>
                      <span>-${selectedOrder.discount?.toFixed(2)}</span>
                    </div>
                  )}
                  <div className="flex justify-between font-bold text-lg mt-1 pt-1 border-t border-gray-200 dark:border-gray-700">
                    <span>Total:</span>
                    <span className="text-blue-600 dark:text-blue-400">
                      ${selectedOrder.total?.toFixed(2)}
                    </span>
                  </div>
                </div>
              </Card>

              {/* Help section - only show for active orders */}
              {[
                "PENDING",
                "CONFIRMED",
                "PREPARING",
                "READY_FOR_PICKUP",
                "OUT_FOR_DELIVERY",
              ].includes(selectedOrder.status) && (
                <div className="bg-blue-50 dark:bg-blue-900/20 rounded-lg p-4 flex gap-3 items-start">
                  <HiInformationCircle className="text-blue-500 text-xl flex-shrink-0 mt-1" />
                  <div>
                    <h3 className="font-medium text-blue-700 dark:text-blue-400">
                      Need help with this order?
                    </h3>
                    <p className="text-sm text-blue-600 dark:text-blue-300 mt-1">
                      If you need to modify or cancel this order, please contact
                      the restaurant directly as soon as possible. For delivery
                      issues, use our customer support.
                    </p>
                    <div className="flex gap-3 mt-3">
                      <Button size="xs" outline gradientDuoTone="cyanToBlue">
                        Contact Restaurant
                      </Button>
                      <Button size="xs" outline gradientDuoTone="purpleToPink">
                        Customer Support
                      </Button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </Modal.Body>
          <Modal.Footer>
            <Button
              gradientDuoTone="purpleToBlue"
              onClick={() => setIsModalOpen(false)}
            >
              Close
            </Button>
            {selectedOrder.status === "DELIVERED" && (
              <Button
                color="light"
                onClick={() => handleReorder(selectedOrder)}
                disabled={reordering}
                className="flex items-center gap-2"
              >
                {reordering ? (
                  <Spinner size="sm" className="mr-2" />
                ) : (
                  <HiOutlineRefresh
                    className={`h-4 w-4 ${reordering ? "animate-spin" : ""}`}
                  />
                )}
                {reordering ? "Processing..." : "Reorder Items"}
              </Button>
            )}
          </Modal.Footer>
        </Modal>
      )}
    </div>
  );
}
