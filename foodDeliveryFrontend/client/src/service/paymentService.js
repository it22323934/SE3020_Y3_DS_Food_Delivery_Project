// src/service/paymentService.js

const API_BASE_URL = "http://localhost:8082/api/payments";

export const paymentService = {
  /**
   * Get all payments (Admin only)
   * @param {string} token - JWT authentication token
   * @returns {Promise<Array>} - Array of payment objects
   */
  getAllPayments: async (token) => {
    try {
      const response = await fetch(`${API_BASE_URL}/admin/all`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${token}`
        }
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const contentType = response.headers.get("content-type");
      if (!contentType || !contentType.includes("application/json")) {
        throw new TypeError("Response is not JSON");
      }

      return await response.json();
    } catch (error) {
      console.error("Error in paymentService.getAllPayments:", error);
      throw error;
    }
  },

  /**
   * Get all payments for a specific user by email
   * @param {string} email - User's email address
   * @param {string} token - JWT authentication token
   * @returns {Promise<Array>} - Array of payment objects
   */
  getPaymentsByUser: async (email, token) => {
    const response = await fetch(`${API_BASE_URL}/user/${email}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`
      }
    });

    if (!response.ok) {
      throw new Error('Failed to fetch user payments');
    }
    return response.json();
  },

  /**
   * Get payment details by order ID
   * @param {string} orderId - Order ID
   * @param {string} token - JWT authentication token
   * @returns {Promise<Object>} - Payment details object
   */
  getPaymentByOrderId: async (orderId, token) => {
    const response = await fetch(`${API_BASE_URL}/${orderId}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`
      }
    });

    if (!response.ok) {
      throw new Error('Payment not found');
    }
    return response.json();
  }
};

