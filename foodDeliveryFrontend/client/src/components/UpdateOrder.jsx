import React, { useEffect, useState } from "react";
import {
  Container,
  Typography,
  TextField,
  Button,
  CircularProgress,
  Box,
  Paper,
} from "@mui/material";
import axios from "axios";
import { useParams, useNavigate } from "react-router-dom";

const UpdateOrderPage = () => {
  const { orderId } = useParams();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [remark, setRemark] = useState("");
  const [orderComplete, setOrderComplete] = useState(false);
  const [orderData, setOrderData] = useState(null); // store full order

  useEffect(() => {
    const fetchOrder = async () => {
      try {
        const res = await axios.get(`http://localhost:8081/api/driver-orders/order-by-id/${orderId}`);
        const currentOrder = res.data[0];

        if (currentOrder) {
          setOrderData(currentOrder);
          setRemark(currentOrder.remarks || "");
          setOrderComplete(currentOrder.orderComplete || false);
        }
      } catch (err) {
        console.error("Failed to fetch order:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchOrder();
  }, [orderId]);

  const handleUpdate = async () => {
    if (!orderData) return;

    const updatedOrder = {
      driverId: orderData.driverId,
      orderId: orderData.orderId,
      userId: orderData.userId,
      userName: orderData.userName,
      restaurantId: orderData.restaurantId,
      deliveryAddress: orderData.deliveryAddress,
      orderItems: orderData.orderItems,
      price: orderData.price,
      orderDate: orderData.orderDate,
      orderTime: orderData.orderTime,
      isOrderComplete: true,
      remarks: remark,
    };

    try {
      await axios.put(`http://localhost:8081/api/driver-orders/${orderId}`, updatedOrder);
      alert("Order updated successfully");
      navigate("/DeliveryAssignOrders"); // Change route if needed
    } catch (error) {
      console.error("Error updating order:", error);
      alert("Failed to update order");
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" mt={5}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Container maxWidth="sm">
      <Paper elevation={3} sx={{ p: 4, mt: 5 }}>
        <Typography variant="h5" gutterBottom>
          Update Order #{orderId}
        </Typography>
        <TextField
          fullWidth
          label="Remarks"
          value={remark}
          onChange={(e) => setRemark(e.target.value)}
          margin="normal"
        />
        <Button
          fullWidth
          variant="contained"
          color="primary"
          onClick={handleUpdate}
          sx={{ mt: 2 }}
        >
          Mark as Complete
        </Button>
      </Paper>
    </Container>
  );
};

export default UpdateOrderPage;
