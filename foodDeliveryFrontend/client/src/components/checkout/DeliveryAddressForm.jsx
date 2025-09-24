import React from "react";
import { Card, TextInput, Label, Textarea } from "flowbite-react";
import { HiLocationMarker } from "react-icons/hi";
import { sanitizeInput } from "../../utils/sanitize";

const DeliveryAddressForm = ({ formData, formErrors, handleChange }) => {
  // Wrap handleChange to sanitize inputs
  const handleSanitizedChange = (e) => {
    const sanitizedEvent = {
      ...e,
      target: {
        ...e.target,
        value: sanitizeInput(e.target.value)
      }
    };
    handleChange(sanitizedEvent);
  };
  return (
    <Card className="mb-6">
      <h2 className="text-xl font-semibold mb-4 flex items-center">
        <HiLocationMarker className="mr-2 text-blue-600" /> Delivery Address
      </h2>

      <div className="space-y-4">
        <div>
          <Label htmlFor="street" value="Street Address" />
          <TextInput
            id="street"
            name="address.street"
            value={formData.address.street}
            onChange={handleSanitizedChange}
            placeholder="123 Main St"
            color={formErrors.street ? "failure" : undefined}
            helperText={formErrors.street}
            required
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <Label htmlFor="city" value="City" />
            <TextInput
              id="city"
              name="address.city"
              value={formData.address.city}
              onChange={handleSanitizedChange}
              placeholder="New York"
              color={formErrors.city ? "failure" : undefined}
              helperText={formErrors.city}
              required
            />
          </div>

          <div>
            <Label htmlFor="state" value="State" />
            <TextInput
              id="state"
              name="address.state"
              value={formData.address.state}
              onChange={handleSanitizedChange}
              placeholder="NY"
              color={formErrors.state ? "failure" : undefined}
              helperText={formErrors.state}
              required
            />
          </div>

          <div>
            <Label htmlFor="zipCode" value="ZIP Code" />
            <TextInput
              id="zipCode"
              name="address.zipCode"
              value={formData.address.zipCode}
              onChange={handleSanitizedChange}
              placeholder="10001"
              color={formErrors.zipCode ? "failure" : undefined}
              helperText={formErrors.zipCode}
              required
            />
          </div>
        </div>

        <div>
          <Label
            htmlFor="deliveryInstructions"
            value="Delivery Instructions (Optional)"
          />
          <Textarea
            id="deliveryInstructions"
            name="deliveryInstructions"
            value={formData.deliveryInstructions}
            onChange={handleSanitizedChange}
            placeholder="Apartment number, gate code, or special instructions"
            rows={3}
          />
        </div>
      </div>
    </Card>
  );
};

export default DeliveryAddressForm;