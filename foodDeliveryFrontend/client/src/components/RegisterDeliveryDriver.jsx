import React, { useState } from "react";
import axios from "axios";
import { HiOutlineTruck, HiOutlineUser, HiOutlinePhone, HiOutlineHome, 
  HiOutlineLocationMarker, HiOutlineIdentification, HiOutlineBadgeCheck } from "react-icons/hi";

export default function RegisterDeliveryDriver() {
  const [formData, setFormData] = useState({
    driverId: "",
    driverName: "",
    driverAddress: "",
    driverPhone: "",
    vehicleType: "bike",
    vehicleNumber: "",
    workingCity: ""
  });
  
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState(null);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    
    try {
      const response = await axios.post("http://localhost:8089/api/deliveryDriver", formData);
      setSuccess(true);
      setFormData({
        driverId: "",
        driverName: "",
        driverAddress: "",
        driverPhone: "",
        vehicleType: "bike",
        vehicleNumber: "",
        workingCity: ""
      });
      setTimeout(() => setSuccess(false), 5000); // Clear success message after 5 seconds
    } catch (error) {
      console.error("Error registering driver:", error);
      setError(error.response?.data?.message || "Failed to register driver. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-6 max-w-4xl mx-auto">
      {/* Header Section */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-800 mb-2">
          Register Delivery Driver
        </h1>
        <p className="text-gray-600">
          Enter the driver's details to register them in the system.
        </p>
      </div>

      {/* Success Message */}
      {success && (
        <div className="mb-6 bg-green-100 border-l-4 border-green-500 text-green-700 p-4 rounded-md flex items-center">
          <HiOutlineBadgeCheck className="h-5 w-5 mr-2" />
          <span>Driver registered successfully!</span>
        </div>
      )}

      {/* Error Message */}
      {error && (
        <div className="mb-6 bg-red-100 border-l-4 border-red-500 text-red-700 p-4 rounded-md">
          <p className="font-bold">Registration Failed</p>
          <p>{error}</p>
        </div>
      )}

      {/* Form */}
      <div className="bg-white shadow-md rounded-lg overflow-hidden">
        <div className="p-6">
          <form onSubmit={handleSubmit}>
            {/* Row 1: Driver ID and Name */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <HiOutlineIdentification className="inline-block mr-1 text-gray-500" /> 
                  Driver ID
                </label>
                <input
                  type="text"
                  placeholder="Enter ID"
                  name="driverId"
                  required
                  value={formData.driverId}
                  onChange={handleChange}
                  className="w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <HiOutlineUser className="inline-block mr-1 text-gray-500" /> 
                  Name
                </label>
                <input
                  type="text"
                  placeholder="Enter Full Name"
                  name="driverName"
                  required
                  value={formData.driverName}
                  onChange={handleChange}
                  className="w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                />
              </div>
            </div>

            {/* Row 2: Address and Phone */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <HiOutlineHome className="inline-block mr-1 text-gray-500" /> 
                  Address
                </label>
                <input
                  type="text"
                  placeholder="Enter Address"
                  name="driverAddress"
                  required
                  value={formData.driverAddress}
                  onChange={handleChange}
                  className="w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <HiOutlinePhone className="inline-block mr-1 text-gray-500" /> 
                  Phone Number
                </label>
                <input
                  type="text"
                  placeholder="Enter Phone Number"
                  name="driverPhone"
                  required
                  value={formData.driverPhone}
                  onChange={handleChange}
                  className="w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                />
              </div>
            </div>

            {/* Row 3: Vehicle Type and Number */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <HiOutlineTruck className="inline-block mr-1 text-gray-500" /> 
                  Vehicle Type
                </label>
                <select
                  name="vehicleType"
                  value={formData.vehicleType}
                  onChange={handleChange}
                  className="w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-orange-500 bg-white"
                >
                  <option value="bike">Bike</option>
                  <option value="car">Car</option>
                  <option value="threewheels">Three Wheels</option>
                  <option value="van">Van</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Vehicle Number
                </label>
                <input
                  type="text"
                  placeholder="Enter Vehicle Number"
                  name="vehicleNumber"
                  required
                  value={formData.vehicleNumber}
                  onChange={handleChange}
                  className="w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
                />
              </div>
            </div>

            {/* Row 4: Working City */}
            <div className="mb-6">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                <HiOutlineLocationMarker className="inline-block mr-1 text-gray-500" /> 
                Working City
              </label>
              <input
                type="text"
                placeholder="Enter Working City"
                name="workingCity"
                required
                value={formData.workingCity}
                onChange={handleChange}
                className="w-full px-4 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-orange-500"
              />
            </div>

            {/* Submit Button */}
            <div className="flex justify-center mt-8">
              <button
                type="submit"
                disabled={loading}
                className={`px-8 py-3 rounded-md font-medium text-white bg-orange-500 hover:bg-orange-600 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:ring-offset-2 transition-colors ${loading ? 'opacity-70 cursor-not-allowed' : ''}`}
              >
                {loading ? (
                  <span className="flex items-center">
                    <svg className="animate-spin -ml-1 mr-2 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Processing...
                  </span>
                ) : (
                  'Register Driver'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}