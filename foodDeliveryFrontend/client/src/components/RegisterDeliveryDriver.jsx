import React, { useState } from "react";
import axios from "axios";

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

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
    //       const response = await axios.post("http://localhost:9002/api/deliveryDriver", formData);  
      const response = await axios.post("http://localhost:8089/api/deliveryDriver", formData);
      alert("Driver Registered: " + response.data);
    } catch (error) {
      console.error("Error registering driver:", error);
      alert("Failed to register driver");
    }
  };

  const styles = {
    container: {
      maxWidth: "800px",
      margin: "0 auto",
      padding: "20px",
      fontFamily: "Arial, sans-serif"
    },
    form: {
      backgroundColor: "#f9f9f9",
      padding: "20px",
      borderRadius: "8px",
      boxShadow: "0 2px 4px rgba(0, 0, 0, 0.1)"
    },
    row: {
      display: "flex",
      flexWrap: "wrap",
      margin: "0 -10px 15px -10px"
    },
    column: {
      flex: "1",
      minWidth: "250px",
      padding: "0 10px",
      marginBottom: "15px"
    },
    h3: {
      marginBottom: "8px",
      color: "#333",
      fontSize: "16px"
    },
    input: {
      width: "100%",
      padding: "10px",
      border: "1px solid #ddd",
      borderRadius: "4px",
      fontSize: "14px",
      boxSizing: "border-box"
    },
    select: {
      width: "100%",
      padding: "10px",
      border: "1px solid #ddd",
      borderRadius: "4px",
      fontSize: "14px",
      backgroundColor: "white",
      boxSizing: "border-box"
    },
    button: {
      backgroundColor: "#4CAF50",
      color: "white",
      padding: "12px 20px",
      border: "none",
      borderRadius: "4px",
      cursor: "pointer",
      fontSize: "16px",
      display: "block",
      margin: "20px auto 0",
      width: "200px",
      transition: "background-color 0.3s"
    },
    buttonHover: {
      backgroundColor: "#45a049"
    }
  };

  return (
    <div style={styles.container}>
      <form style={styles.form} onSubmit={handleSubmit}>
        <div style={styles.row}>
          <div style={styles.column}>
            <h3 style={styles.h3}>Driver ID</h3>
            <input
              type="text"
              placeholder="Enter ID"
              name="driverId"
              value={formData.driverId}
              onChange={handleChange}
              style={styles.input}
            />
          </div>
          <div style={styles.column}>
            <h3 style={styles.h3}>Name</h3>
            <input
              type="text"
              placeholder="Enter Name"
              name="driverName"
              value={formData.driverName}
              onChange={handleChange}
              style={styles.input}
            />
          </div>
        </div>

        <div style={styles.row}>
          <div style={styles.column}>
            <h3 style={styles.h3}>Address</h3>
            <input
              type="text"
              placeholder="Enter Address"
              name="driverAddress"
              value={formData.driverAddress}
              onChange={handleChange}
              style={styles.input}
            />
          </div>
          <div style={styles.column}>
            <h3 style={styles.h3}>Phone No</h3>
            <input
              type="text"
              placeholder="Enter Phone No"
              name="driverPhone"
              value={formData.driverPhone}
              onChange={handleChange}
              style={styles.input}
            />
          </div>
        </div>

        <div style={styles.row}>
          <div style={styles.column}>
            <h3 style={styles.h3}>Vehicle Type</h3>
            <select
              name="vehicleType"
              value={formData.vehicleType}
              onChange={handleChange}
              style={styles.select}
            >
              <option value="bike">Bike</option>
              <option value="car">Car</option>
              <option value="threewheels">Three Wheels</option>
              <option value="van">Van</option>
            </select>
          </div>
          <div style={styles.column}>
            <h3 style={styles.h3}>Vehicle Number</h3>
            <input
              type="text"
              placeholder="Enter Vehicle No"
              name="vehicleNumber"
              value={formData.vehicleNumber}
              onChange={handleChange}
              style={styles.input}
            />
          </div>
        </div>

        <div style={styles.row}>
          <div style={styles.column}>
            <h3 style={styles.h3}>Working City</h3>
            <input
              type="text"
              placeholder="Enter Working City"
              name="workingCity"
              value={formData.workingCity}
              onChange={handleChange}
              style={styles.input}
            />
          </div>
        </div>

        <button
          type="submit"
          style={styles.button}
          onMouseOver={(e) =>
            (e.target.style.backgroundColor = styles.buttonHover.backgroundColor)
          }
          onMouseOut={(e) =>
            (e.target.style.backgroundColor = styles.button.backgroundColor)
          }
        >
          Submit
        </button>
      </form>
    </div>
  );
}
