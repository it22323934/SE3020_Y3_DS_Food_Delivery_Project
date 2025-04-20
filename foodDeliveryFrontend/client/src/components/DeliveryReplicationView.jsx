import React, { useEffect, useState } from "react";
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, CircularProgress, Typography } from "@mui/material";

const DeliveryReplicationView = () => {
  const [deliveries, setDeliveries] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Fetch delivery replication data from backend
    fetch("http://localhost:9001/api/deliveryReplication")
      .then((response) => response.json())
      .then((data) => {
        setDeliveries(data); // Set the data to state
        setLoading(false); // Stop loading
      })
      .catch((error) => {
        console.error("Error fetching data:", error);
        setLoading(false);
      });
  }, []);

  // If loading, show a loading spinner
  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
      </div>
    );
  }

  const getOrderIdStyle = (assignDriver, orderDeliveredComplete) => {
    if (!assignDriver) {
      return { color: 'red' }; // Red color if Assign Driver is false
    }
    if (assignDriver && !orderDeliveredComplete) {
      return { color: 'orange' }; // Orange color if Assign Driver is true and Delivered is false
    }
    return { color: 'green' }; // Green color if Delivered is true
  };

  return (
    <div style={{ padding: '20px' }}>
      <Typography variant="h4" gutterBottom align="center">
        Delivery Replication Data
      </Typography>
      <TableContainer component={Paper} style={{ maxWidth: '100%', overflowX: 'auto' }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Order ID</TableCell>
              <TableCell>User Name</TableCell>
              <TableCell>User Phone</TableCell>
              <TableCell>Restaurant ID</TableCell>
              <TableCell>Delivery Address</TableCell>
              <TableCell>Order Items</TableCell>
              <TableCell>Price</TableCell>
              <TableCell>Order Date</TableCell>
              <TableCell>Order Time</TableCell>
              <TableCell>Assign Driver</TableCell>
              <TableCell>Driver Name</TableCell>
              <TableCell>Driver Phone</TableCell>
              <TableCell>Delivered</TableCell>
              <TableCell>Driver Remark</TableCell>
              <TableCell>User Remark</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {deliveries.map((delivery) => (
              <TableRow key={delivery.orderId}>
                <TableCell style={getOrderIdStyle(delivery.isAssignDriver, delivery.orderDeliveredComplete)}>
                  {delivery.orderId}
                </TableCell>
                <TableCell>{delivery.userName}</TableCell>
                <TableCell>{delivery.userPhoneNo}</TableCell>
                <TableCell>{delivery.restaurantId}</TableCell>
                <TableCell>{delivery.deliveryAddress}</TableCell>
                <TableCell>{delivery.orderItems.join(", ")}</TableCell>
                <TableCell>{delivery.price}</TableCell>
                <TableCell>{delivery.orderDate}</TableCell>
                <TableCell>{delivery.orderTime}</TableCell>
                <TableCell>{delivery.isAssignDriver ? "Yes" : "No"}</TableCell> 
                <TableCell>{delivery.driverName}</TableCell>
                <TableCell>{delivery.driverPhoneNo}</TableCell>
                <TableCell>{delivery.orderDeliveredComplete ? "Yes" : "No"}</TableCell>
                <TableCell>{delivery.driverRemark}</TableCell>
                <TableCell>{delivery.userRemark}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </div>
  );
};

export default DeliveryReplicationView;
