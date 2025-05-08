import React, { useState, useEffect } from "react";
import { useSelector } from "react-redux";
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
  HiOutlineIdentification,
  HiOutlineClipboard,
} from "react-icons/hi";
import {
  FaReceipt,
  FaShoppingBag,
  FaUtensils,
  FaRegCalendarAlt,
  FaMoneyBillWave,
} from "react-icons/fa";
import { orderService } from "../service/orderService";
import { restaurantService } from "../service/restaurantService";
import ReactPaginate from "react-paginate";
import LoadingSpinner from "./LoadingSpinner";
import UpdateOrderStatusDropdown from "./sub-components/order-management/UpdateOrderStatusDropdown";
import OrderStatusTimeline from "./sub-components/order-management/OrderStatusTimeline";

export default function DashMyOrdersRestaurantManagement() {
  const { currentUser } = useSelector((state) => state.user);
  const [orders, setOrders] = useState([]);
  const [filteredOrders, setFilteredOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [restaurant, setRestaurant] = useState(null);
  const [currentTab, setCurrentTab] = useState("all");
  const [searchTerm, setSearchTerm] = useState("");
  const [sortOrder, setSortOrder] = useState("desc");
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [pageNumber, setPageNumber] = useState(0);
  const itemsPerPage = 5;

  // Order status counts
  const [totalOrders, setTotalOrders] = useState(0);
  const [activeOrders, setActiveOrders] = useState(0);
  const [completedOrders, setCompletedOrders] = useState(0);

  // Status workflows and display configs
  const statusWorkflow = {
    PENDING: ["CONFIRMED", "CANCELLED"],
    CONFIRMED: ["PREPARING", "CANCELLED"],
    PREPARING: ["READY_FOR_PICKUP", "CANCELLED"],
    READY_FOR_PICKUP: ["OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED"],
    OUT_FOR_DELIVERY: ["DELIVERED", "CANCELLED"],
    DELIVERED: [],
    CANCELLED: [],
  };

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

  // Fetch restaurant information and then orders
  useEffect(() => {
    if (currentUser?.token) {
      fetchRestaurant();
    }
  }, [currentUser]);

  // Fetch orders when restaurant ID is available
  useEffect(() => {
    if (restaurant?.id && currentUser?.token) {
      fetchOrders();
    }
  }, [restaurant]);

  // Filter orders when tab changes or search term changes
  useEffect(() => {
    filterOrders();
  }, [orders, currentTab, searchTerm]);

  const fetchRestaurant = async () => {
    try {
      setLoading(true);
      const response = await restaurantService.getRestaurantsByUserId(
        currentUser.id,
        currentUser.token
      );

      if (response.ok) {
        const data = await response.json();
        if (data && data.length > 0) {
          setRestaurant(data[0]);
        } else {
          setError("No restaurant found for your account");
        }
      } else {
        setError("Failed to fetch restaurant details");
      }
    } catch (err) {
      setError(`Error: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const fetchOrders = async () => {
    try {
      setLoading(true);
      const response = await orderService.getOrdersByRestaurantId(
        restaurant.id,
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
    if (!orders.length) return;

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
          order.contactInfo.name.toLowerCase().includes(search) ||
          order.contactInfo.phone.includes(search) ||
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

  // Handle status update from the separate component
  const handleStatusUpdated = (orderId, newStatus) => {
    // Update orders list
    const updatedOrders = orders.map((order) =>
      order.id === orderId ? { ...order, status: newStatus } : order
    );
    setOrders(updatedOrders);

    // Update selected order if needed
    if (selectedOrder && selectedOrder.id === orderId) {
      setSelectedOrder({ ...selectedOrder, status: newStatus });
    }

    // Update counts after status change
    const active = updatedOrders.filter((order) =>
      [
        "PENDING",
        "CONFIRMED",
        "PREPARING",
        "READY_FOR_PICKUP",
        "OUT_FOR_DELIVERY",
      ].includes(order.status)
    ).length;
    setActiveOrders(active);

    const completed = updatedOrders.filter(
      (order) => order.status === "DELIVERED"
    ).length;
    setCompletedOrders(completed);
  };

  // Order status badge component for consistency
  const OrderStatusBadge = ({ status }) => (
    <Badge color={statusColors[status] || "gray"} className="whitespace-nowrap">
      {statusIcons[status]}
      {status.replace("_", " ")}
    </Badge>
  );

  if (loading && !restaurant) {
    return <LoadingSpinner />;
  }

  if (!restaurant) {
    return (
      <div className="p-4">
        <Alert color="info" icon={HiInformationCircle}>
          <span className="font-medium">No restaurant found!</span> You don't
          have any restaurants assigned to your account. Please contact the
          system administrator.
        </Alert>
      </div>
    );
  }

  // Add this function for distance calculation
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

  return (
    <div className="table-auto overflow-x-scroll md:mx-auto p-3 scrollbar scrollbar-track-slate-100 scrollbar-thumb-slate-300 dark:scrollbar-track-slate-700 dark:scrollbar-thumb-slate-500">
      <ToastContainer position="top-right" autoClose={3000} />

      {/* Stats Cards - Enhanced with better styling and container */}
      <div className="p-4 md:mx-auto mb-6 bg-white dark:bg-gray-800 rounded-lg shadow-sm">
        <h2 className="text-lg font-medium text-gray-700 dark:text-white mb-4 flex items-center">
          <HiInformationCircle className="mr-2 text-blue-600" />
          Order Statistics
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="flex p-4 bg-gradient-to-br from-blue-50 to-blue-100 dark:from-gray-700 dark:to-gray-800 rounded-lg shadow-sm">
            <div className="flex-1">
              <h3 className="text-gray-600 dark:text-gray-300 text-sm font-medium uppercase tracking-wider">
                Total Orders
              </h3>
              <p className="text-2xl font-bold text-gray-800 dark:text-white mt-2">
                {totalOrders}
              </p>
            </div>
            <div className="flex items-center justify-center">
              <div className="bg-blue-500 text-white p-3 rounded-lg shadow-lg">
                <FaReceipt size={24} />
              </div>
            </div>
          </div>

          <div className="flex p-4 bg-gradient-to-br from-yellow-50 to-yellow-100 dark:from-gray-700 dark:to-gray-800 rounded-lg shadow-sm">
            <div className="flex-1">
              <h3 className="text-gray-600 dark:text-gray-300 text-sm font-medium uppercase tracking-wider">
                Active Orders
              </h3>
              <p className="text-2xl font-bold text-gray-800 dark:text-white mt-2">
                {activeOrders}
              </p>
            </div>
            <div className="flex items-center justify-center">
              <div className="bg-yellow-500 text-white p-3 rounded-lg shadow-lg">
                <HiOutlineClock size={24} />
              </div>
            </div>
          </div>

          <div className="flex p-4 bg-gradient-to-br from-green-50 to-green-100 dark:from-gray-700 dark:to-gray-800 rounded-lg shadow-sm">
            <div className="flex-1">
              <h3 className="text-gray-600 dark:text-gray-300 text-sm font-medium uppercase tracking-wider">
                Completed Orders
              </h3>
              <p className="text-2xl font-bold text-gray-800 dark:text-white mt-2">
                {completedOrders}
              </p>
            </div>
            <div className="flex items-center justify-center">
              <div className="bg-green-500 text-white p-3 rounded-lg shadow-lg">
                <HiCheck size={24} />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Header and Filters - Improved layout with better hierarchy */}
      <div className="mb-6 bg-white dark:bg-gray-800 p-4 rounded-lg shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-gray-200 dark:border-gray-700 pb-4 mb-4">
          <div className="flex-1">
            <h2 className="text-xl font-semibold text-gray-800 dark:text-white flex items-center">
              <FaReceipt className="mr-2 text-blue-600" />
              {restaurant.name} - Orders Management
            </h2>
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
              Manage and track all customer orders for your restaurant
            </p>
          </div>
          <div>
            <Button
              outline
              gradientDuoTone="purpleToBlue"
              onClick={fetchOrders}
              disabled={loading}
              className="flex items-center"
            >
              <HiOutlineRefresh
                className={`mr-2 h-5 w-5 ${loading ? "animate-spin" : ""}`}
              />
              Refresh Orders
            </Button>
          </div>
        </div>

        {/* Enhanced Filter Section with better layout */}
        <div className="space-y-4">
          {/* Search */}
          <div className="w-full">
            <TextInput
              type="text"
              placeholder="Search orders by ID, customer name, phone..."
              value={searchTerm}
              onChange={handleSearchChange}
              icon={HiOutlineSearch}
              className="w-full"
            />
          </div>

          {/* Filter Controls */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="col-span-1">
              <Dropdown
                label={
                  <div className="flex items-center justify-between w-full">
                    <div className="flex items-center">
                      <HiFilter className="mr-2" />
                      <span className="truncate">
                        {currentTab === "all"
                          ? "All Orders"
                          : currentTab === "active"
                          ? "Active Orders"
                          : currentTab === "completed"
                          ? "Completed Orders"
                          : currentTab === "cancelled"
                          ? "Cancelled Orders"
                          : `${currentTab.replace("_", " ")} Orders`}
                      </span>
                    </div>
                  </div>
                }
                color="light"
                className="w-full md:w-auto"
                dismissOnClick={true}
              >
                <Dropdown.Item
                  onClick={() => setCurrentTab("all")}
                  className="flex items-center"
                >
                  <span className="mr-2">🔄</span> All Orders
                </Dropdown.Item>
                <Dropdown.Divider />
                <Dropdown.Item
                  onClick={() => setCurrentTab("active")}
                  className={
                    currentTab === "active"
                      ? "bg-blue-50 dark:bg-blue-900/20"
                      : ""
                  }
                >
                  {statusIcons["PENDING"]} Active Orders
                </Dropdown.Item>
                <Dropdown.Item
                  onClick={() => setCurrentTab("PENDING")}
                  className={
                    currentTab === "PENDING"
                      ? "bg-blue-50 dark:bg-blue-900/20"
                      : ""
                  }
                >
                  {statusIcons["PENDING"]} Pending Orders
                </Dropdown.Item>
                <Dropdown.Item
                  onClick={() => setCurrentTab("CONFIRMED")}
                  className={
                    currentTab === "CONFIRMED"
                      ? "bg-blue-50 dark:bg-blue-900/20"
                      : ""
                  }
                >
                  {statusIcons["CONFIRMED"]} Confirmed Orders
                </Dropdown.Item>
                <Dropdown.Item
                  onClick={() => setCurrentTab("PREPARING")}
                  className={
                    currentTab === "PREPARING"
                      ? "bg-blue-50 dark:bg-blue-900/20"
                      : ""
                  }
                >
                  {statusIcons["PREPARING"]} Preparing Orders
                </Dropdown.Item>
                <Dropdown.Item
                  onClick={() => setCurrentTab("READY_FOR_PICKUP")}
                  className={
                    currentTab === "READY_FOR_PICKUP"
                      ? "bg-blue-50 dark:bg-blue-900/20"
                      : ""
                  }
                >
                  {statusIcons["READY_FOR_PICKUP"]} Ready For Pickup
                </Dropdown.Item>
                <Dropdown.Item
                  onClick={() => setCurrentTab("OUT_FOR_DELIVERY")}
                  className={
                    currentTab === "OUT_FOR_DELIVERY"
                      ? "bg-blue-50 dark:bg-blue-900/20"
                      : ""
                  }
                >
                  {statusIcons["OUT_FOR_DELIVERY"]} Out For Delivery
                </Dropdown.Item>
                <Dropdown.Divider />
                <Dropdown.Item
                  onClick={() => setCurrentTab("completed")}
                  className={
                    currentTab === "completed"
                      ? "bg-blue-50 dark:bg-blue-900/20"
                      : ""
                  }
                >
                  {statusIcons["DELIVERED"]} Completed Orders
                </Dropdown.Item>
                <Dropdown.Item
                  onClick={() => setCurrentTab("cancelled")}
                  className={
                    currentTab === "cancelled"
                      ? "bg-blue-50 dark:bg-blue-900/20"
                      : ""
                  }
                >
                  {statusIcons["CANCELLED"]} Cancelled Orders
                </Dropdown.Item>
              </Dropdown>
            </div>

            <div className="col-span-1 flex gap-2">
              <Button
                color="light"
                onClick={toggleSortOrder}
                className="whitespace-nowrap flex items-center flex-1"
              >
                {sortOrder === "desc" ? (
                  <HiOutlineSortDescending className="mr-1" />
                ) : (
                  <HiOutlineSortAscending className="mr-1" />
                )}
                {sortOrder === "desc" ? "Newest First" : "Oldest First"}
              </Button>

              {(searchTerm || currentTab !== "all") && (
                <Button
                  color="light"
                  onClick={resetFilters}
                  className="whitespace-nowrap"
                >
                  <HiFilter className="mr-1" />
                  Clear Filters
                </Button>
              )}
            </div>
          </div>

          {/* Active Filters Display */}
          {(searchTerm || currentTab !== "all") && (
            <div className="flex flex-wrap items-center gap-2 pt-3 border-t border-gray-200 dark:border-gray-700">
              <div className="bg-blue-50 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300 text-xs font-medium px-3 py-1.5 rounded-lg mr-2">
                <span className="flex items-center">
                  <HiFilter className="mr-2" />
                  Filters Applied
                </span>
              </div>

              {searchTerm && (
                <Badge color="info" className="px-2.5 py-1 text-xs">
                  Search: "{searchTerm}"
                </Badge>
              )}

              {currentTab !== "all" && (
                <Badge
                  color={
                    currentTab === "completed"
                      ? "success"
                      : currentTab === "cancelled"
                      ? "failure"
                      : currentTab === "active"
                      ? "warning"
                      : statusColors[currentTab] || "purple"
                  }
                  className="px-2.5 py-1 text-xs"
                >
                  {currentTab === "active"
                    ? "Active Orders"
                    : currentTab === "completed"
                    ? "Completed Orders"
                    : currentTab === "cancelled"
                    ? "Cancelled Orders"
                    : `${currentTab.replace("_", " ")} Orders`}
                </Badge>
              )}

              <Button
                color="light"
                size="xs"
                onClick={resetFilters}
                className="ml-auto"
              >
                <HiX className="mr-1 h-3 w-3" />
                Clear All
              </Button>
            </div>
          )}
        </div>
      </div>

      {/* Orders Display Section */}
      {loading ? (
        <LoadingSpinner />
      ) : (
        <>
          {filteredOrders.length > 0 ? (
            <div className="overflow-x-auto">
              <Table>
                <Table.Head>
                  <Table.HeadCell>Customer</Table.HeadCell>
                  <Table.HeadCell>Date</Table.HeadCell>
                  <Table.HeadCell>Total</Table.HeadCell>
                  <Table.HeadCell>Status</Table.HeadCell>
                  <Table.HeadCell>Actions</Table.HeadCell>
                </Table.Head>
                <Table.Body className="divide-y">
                  {displayOrders.map((order) => (
                    <Table.Row
                      key={order.id}
                      className="bg-white dark:border-gray-700 dark:bg-gray-800"
                    >
                      <Table.Cell>
                        <div className="flex flex-col">
                          <span>{order.contactInfo.name}</span>
                          <span className="text-xs text-gray-500">
                            {order.contactInfo.phone}
                          </span>
                        </div>
                      </Table.Cell>
                      <Table.Cell>
                        <div className="whitespace-nowrap">
                          {formatDate(order.createdAt)}
                        </div>
                      </Table.Cell>
                      <Table.Cell className="font-medium">
                        ${order.total.toFixed(2)}
                      </Table.Cell>
                      <Table.Cell>
                        <OrderStatusBadge status={order.status} />
                      </Table.Cell>
                      <Table.Cell>
                        <div className="flex items-center space-x-2">
                          <Button
                            size="xs"
                            color="info"
                            onClick={() => openOrderDetails(order)}
                          >
                            <HiOutlineEye className="mr-1 h-4 w-4" />
                            View
                          </Button>

                          {/* Using our new component here */}
                          <UpdateOrderStatusDropdown
                            order={order}
                            statusWorkflow={statusWorkflow}
                            statusIcons={statusIcons}
                            statusColors={statusColors}
                            token={currentUser.token}
                            onStatusUpdated={handleStatusUpdated}
                            size="xs"
                          />
                        </div>
                      </Table.Cell>
                    </Table.Row>
                  ))}
                </Table.Body>
              </Table>

              {/* ReactPaginate Pagination - Style matching menu items */}
              {filteredOrders.length > itemsPerPage && (
                <div className="py-4 mt-4 border-t border-gray-200 dark:border-gray-700">
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
                  : "Your restaurant hasn't received any orders yet."}
              </p>
              {(searchTerm || currentTab !== "all") && (
                <Button color="light" className="mt-4" onClick={resetFilters}>
                  Clear Filters
                </Button>
              )}
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
            Order Details #{selectedOrder.id.substring(0, 8)}...
          </Modal.Header>
          <Modal.Body>
            <div className="space-y-6">
              {/* Order Status Section - Enhanced with better styling */}
              <div>
                {/* Status Card with Visual Improvements */}
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-gradient-to-r from-blue-50 to-white dark:from-gray-800 dark:to-gray-750 p-5 rounded-lg shadow-md border border-blue-100 dark:border-gray-700 mb-8 transition-all duration-200">
                  <div className="flex flex-col">
                    <div className="text-sm text-blue-600 dark:text-blue-400 mb-2 uppercase tracking-wide font-medium flex items-center">
                      <HiInformationCircle className="mr-1.5 h-4 w-4" />
                      Current Status
                    </div>
                    <div className="scale-110 origin-left transition-transform hover:scale-105 mb-1">
                      <OrderStatusBadge status={selectedOrder.status} />
                    </div>
                    <div className="text-xs text-gray-500 dark:text-gray-400 mt-2 flex items-center">
                      <HiOutlineClock className="mr-1.5 h-3.5 w-3.5" />
                      Last updated:{" "}
                      {new Date(
                        selectedOrder.updatedAt || selectedOrder.createdAt
                      ).toLocaleString()}
                    </div>
                  </div>

                  {/* Status update dropdown - keeping all props the same */}
                  <div className="sm:self-center">
                    <UpdateOrderStatusDropdown
                      order={selectedOrder}
                      statusWorkflow={statusWorkflow}
                      statusIcons={statusIcons}
                      statusColors={statusColors}
                      token={currentUser.token}
                      onStatusUpdated={handleStatusUpdated}
                      size="sm"
                      showAsButton={true}
                    />
                  </div>
                </div>

                {/* Visual timeline with enhanced styling */}
                <div className="relative pt-3 pb-8 px-2 mb-8">
                  {/* Timeline track background */}
                  <div className="absolute top-0 left-0 right-0 h-1.5 bg-gradient-to-r from-gray-100 via-blue-50 to-gray-100 dark:from-gray-700 dark:via-blue-900/20 dark:to-gray-700 rounded-full shadow-inner"></div>

                  {/* Add a subtle animation for the timeline */}
                  <style jsx>{`
                    @keyframes fadeIn {
                      from {
                        opacity: 0;
                        transform: translateY(10px);
                      }
                      to {
                        opacity: 1;
                        transform: translateY(0);
                      }
                    }
                    .animate-timeline-fadeIn {
                      animation: fadeIn 0.6s ease-out forwards;
                    }
                  `}</style>

                  <div className="animate-timeline-fadeIn">
                    <OrderStatusTimeline
                      status={selectedOrder.status}
                      className="mt-4"
                    />
                  </div>
                </div>
              </div>

              {/* Order Info and Customer */}
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
                    <HiOutlineUser className="mr-2" />
                    Customer Information
                  </h5>
                  <div className="space-y-3 text-sm">
                    <div className="flex items-center gap-2">
                      <HiOutlineUser className="text-gray-500" />
                      <span>{selectedOrder.contactInfo.name}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <HiOutlinePhone className="text-gray-500" />
                      <span>{selectedOrder.contactInfo.phone}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <HiOutlineMail className="text-gray-500" />
                      <span>{selectedOrder.contactInfo.email}</span>
                    </div>
                  </div>
                </Card>
              </div>

              {/* Delivery Location and Map */}
              {selectedOrder.deliveryLocation ||
              selectedOrder.restaurantLocation ? (
                <Card>
                  <h5 className="text-lg font-bold tracking-tight text-gray-900 dark:text-white mb-4 flex items-center">
                    <HiOutlineLocationMarker className="mr-2" />
                    Delivery Location
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
                              {selectedOrder.restaurantLocation.address ||
                                "Address not available"}
                            </div>
                          </div>

                          <div className="p-3 bg-green-50 dark:bg-gray-800 rounded-lg">
                            <div className="font-medium text-green-700 dark:text-green-400 mb-1">
                              Delivery Location
                            </div>
                            <div className="text-sm text-gray-700 dark:text-gray-300">
                              {selectedOrder.deliveryLocation.address ||
                                "Address not available"}
                            </div>
                          </div>
                        </div>

                        {/* Calculate and show distance */}
                        {selectedOrder.deliveryLocation &&
                          selectedOrder.restaurantLocation && (
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
                          )}
                      </>
                    )}

                  {/* If we only have restaurant location */}
                  {selectedOrder.restaurantLocation &&
                    !selectedOrder.deliveryLocation && (
                      <div className="p-3 bg-yellow-50 dark:bg-gray-800 rounded-lg text-center">
                        <div className="text-yellow-600 dark:text-yellow-400">
                          <HiInformationCircle className="inline-block mr-2 h-5 w-5" />
                          No delivery location data available
                        </div>
                      </div>
                    )}

                  {/* If we have neither location */}
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

              {/* Delivery Address */}
              <Card>
                <h5 className="text-lg font-bold tracking-tight text-gray-900 dark:text-white mb-2 flex items-center">
                  <HiOutlineLocationMarker className="mr-2" />
                  Delivery Address
                </h5>
                <div className="text-sm">
                  <div>{selectedOrder.deliveryAddress.street}</div>
                  <div>
                    {selectedOrder.deliveryAddress.city},{" "}
                    {selectedOrder.deliveryAddress.state}{" "}
                    {selectedOrder.deliveryAddress.zipCode}
                  </div>
                </div>
                {selectedOrder.deliveryInstructions && (
                  <div className="mt-2 p-2 bg-gray-50 dark:bg-gray-700 rounded text-sm">
                    <div className="font-medium">Delivery Instructions:</div>
                    <div className="italic">
                      {selectedOrder.deliveryInstructions}
                    </div>
                  </div>
                )}
              </Card>

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
                      {selectedOrder.items.map((item, index) => (
                        <React.Fragment key={index}>
                          <Table.Row>
                            <Table.Cell className="font-medium">
                              {item.name}
                            </Table.Cell>
                            <Table.Cell>${item.price.toFixed(2)}</Table.Cell>
                            <Table.Cell>{item.quantity}</Table.Cell>
                            <Table.Cell className="font-medium">
                              ${item.itemTotal.toFixed(2)}
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
                                      {addon.name} (${addon.price.toFixed(2)})
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
                    <span>${selectedOrder.subtotal.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600 dark:text-gray-400">
                      Tax:
                    </span>
                    <span>${selectedOrder.taxAmount.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600 dark:text-gray-400">
                      Delivery Fee:
                    </span>
                    <span>${selectedOrder.deliveryFee.toFixed(2)}</span>
                  </div>
                  {selectedOrder.discount > 0 && (
                    <div className="flex justify-between text-sm text-green-600 dark:text-green-400">
                      <span>Discount:</span>
                      <span>-${selectedOrder.discount.toFixed(2)}</span>
                    </div>
                  )}
                  <div className="flex justify-between font-bold text-lg mt-1 pt-1 border-t border-gray-200 dark:border-gray-700">
                    <span>Total:</span>
                    <span className="text-blue-600 dark:text-blue-400">
                      ${selectedOrder.total.toFixed(2)}
                    </span>
                  </div>
                </div>
              </Card>
            </div>
          </Modal.Body>
          <Modal.Footer>
            <Button
              gradientDuoTone="purpleToBlue"
              onClick={() => setIsModalOpen(false)}
            >
              Close
            </Button>
          </Modal.Footer>
        </Modal>
      )}
    </div>
  );
}
