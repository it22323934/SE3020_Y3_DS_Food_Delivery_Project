import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

const AllOrders = () => {
  const [deliveries, setDeliveries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    fetch("http://localhost:8089/api/deliveryReplication")
      .then((response) => {
        if (!response.ok) {
          return response.text().then(text => {
            throw new Error(`HTTP error! Status: ${response.status}, Message: ${text}`);
          });
        }
        return response.json();
      })
      .then((data) => {
        setDeliveries(data);
        setLoading(false);
      })
      .catch((error) => {
        console.error("Error fetching data:", error);
        setLoading(false);
      });
  }, []);

  const getOrderIdStyle = (assignDriver, orderDeliveredComplete) => {
    if (!assignDriver) {
      return 'text-red-600 font-medium'; // Red for unassigned
    }
    if (assignDriver && !orderDeliveredComplete) {
      return 'text-orange-500 font-medium'; // Orange for in-transit
    }
    return 'text-green-600 font-medium'; // Green for delivered
  };

  const getStatusBadge = (assignDriver, orderDeliveredComplete) => {
    if (!assignDriver) {
      return (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">
          Pending
        </span>
      );
    }
    if (assignDriver && !orderDeliveredComplete) {
      return (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">
          In Transit
        </span>
      );
    }
    return (
      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
        Delivered
      </span>
    );
  };

  const filteredDeliveries = searchTerm 
    ? deliveries.filter(delivery => 
        delivery.orderId.toString().includes(searchTerm) ||
        delivery.restaurantId.toString().includes(searchTerm) ||
        (delivery.deliveryAddress && delivery.deliveryAddress.toLowerCase().includes(searchTerm.toLowerCase())) ||
        (delivery.driverName && delivery.driverName.toLowerCase().includes(searchTerm.toLowerCase()))
      )
    : deliveries;

  // Loading state
  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-orange-500"></div>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-full">
      {/* Header and Search Section */}
      <div className="mb-6">
        <div className="flex justify-between items-center flex-wrap gap-4 mb-6">
          <h1 className="text-2xl font-bold text-gray-800">
            Order Dashboard
          </h1>
          <div className="relative">
            <input
              type="text"
              placeholder="Search orders..."
              className="pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
            <svg 
              className="absolute left-3 top-2.5 h-5 w-5 text-gray-400" 
              fill="none" 
              stroke="currentColor" 
              viewBox="0 0 24 24"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 mb-6">
          <div className="bg-white rounded-lg shadow p-4 border-l-4 border-orange-500">
            <p className="text-sm uppercase text-gray-600 font-medium">Total Orders</p>
            <p className="text-2xl font-bold">{deliveries.length}</p>
          </div>
          <div className="bg-white rounded-lg shadow p-4 border-l-4 border-green-500">
            <p className="text-sm uppercase text-gray-600 font-medium">Delivered</p>
            <p className="text-2xl font-bold">
              {deliveries.filter(delivery => delivery.orderDeliveredComplete).length}
            </p>
          </div>
          <div className="bg-white rounded-lg shadow p-4 border-l-4 border-yellow-500">
            <p className="text-sm uppercase text-gray-600 font-medium">In Transit</p>
            <p className="text-2xl font-bold">
              {deliveries.filter(delivery => delivery.isAssignDriver && !delivery.orderDeliveredComplete).length}
            </p>
          </div>
          <div className="bg-white rounded-lg shadow p-4 border-l-4 border-red-500">
            <p className="text-sm uppercase text-gray-600 font-medium">Pending</p>
            <p className="text-2xl font-bold">
              {deliveries.filter(delivery => !delivery.isAssignDriver).length}
            </p>
          </div>
        </div>
      </div>

      {/* Table Section */}
      <div className="bg-white shadow rounded-lg overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-[#FF5A1F]">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-white uppercase tracking-wider border border-white">
                  Order ID
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-white uppercase tracking-wider border border-white">
                  Restaurant ID
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-white uppercase tracking-wider border border-white">
                  Delivery Address
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-white uppercase tracking-wider border border-white">
                  Order Items
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-white uppercase tracking-wider border border-white">
                  Price
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-white uppercase tracking-wider border border-white">
                  Date
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-white uppercase tracking-wider border border-white">
                  Time
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-white uppercase tracking-wider border border-white">
                  Status
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-white uppercase tracking-wider border border-white">
                  Driver
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-white uppercase tracking-wider border border-white">
                  Track
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredDeliveries.length > 0 ? (
                filteredDeliveries.map((delivery) => (
                  <tr key={delivery.orderId} className="hover:bg-gray-50">
                    <td className={`px-4 py-3 whitespace-nowrap ${getOrderIdStyle(delivery.isAssignDriver, delivery.orderDeliveredComplete)}`}>
                      #{delivery.orderId}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-blue-100 text-blue-800">
                        {delivery.restaurantId}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="text-sm text-gray-900 max-w-xs truncate">
                        {delivery.deliveryAddress}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="text-sm text-gray-900 max-w-xs truncate">
                        {delivery.orderItems.join(", ")}
                      </div>
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">${delivery.price}</div>
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                      {delivery.orderDate}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                      {delivery.orderTime}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      {getStatusBadge(delivery.isAssignDriver, delivery.orderDeliveredComplete)}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      {delivery.driverName ? (
                        <div>
                          <div className="text-sm font-medium text-gray-900">{delivery.driverName}</div>
                          <div className="text-xs text-gray-500">{delivery.driverPhoneNo}</div>
                        </div>
                      ) : (
                        <span className="text-sm text-gray-500">Unassigned</span>
                      )}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                      <button
                        onClick={() => navigate(`/CustomerTrackingOrder/${delivery.userId}/${delivery.orderId}`)}
                        className="inline-flex items-center px-3 py-1 border border-transparent text-sm font-medium rounded-md text-white bg-[#52be80] hover:bg-[#45a163] focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500"
                      >
                        <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
                        </svg>
                        Map
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="10" className="px-4 py-8 text-center text-gray-500">
                    <svg className="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
                    </svg>
                    <p className="mt-2 font-medium">No orders found</p>
                    {searchTerm && <p className="text-sm">Try adjusting your search criteria</p>}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="bg-gray-50 px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6">
          <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
            <div>
              <p className="text-sm text-gray-700">
                Showing <span className="font-medium">1</span> to{" "}
                <span className="font-medium">{filteredDeliveries.length}</span> of{" "}
                <span className="font-medium">{filteredDeliveries.length}</span> results
              </p>
            </div>
            <div>
              <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
                <a href="#" className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50">
                  Previous
                </a>
                <a href="#" className="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700 hover:bg-gray-50">
                  1
                </a>
                <a href="#" className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50">
                  Next
                </a>
              </nav>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AllOrders;