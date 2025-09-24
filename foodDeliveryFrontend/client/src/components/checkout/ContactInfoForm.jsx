import React from "react";
import PropTypes from 'prop-types';
import { Card, TextInput, Label } from "flowbite-react";
import { HiUser, HiMail, HiPhone } from "react-icons/hi";
import { sanitizeInput } from "../../utils/sanitize";

const ContactInfoForm = ({ formData, formErrors, handleChange }) => {
  return (
    <Card className="mb-6">
      <h2 className="text-xl font-semibold mb-4 flex items-center">
        <HiUser className="mr-2 text-blue-600" /> Contact Information
      </h2>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
        <div>
          <Label htmlFor="fullName" value="Full Name" />
          <TextInput
            id="fullName"
            name="fullName"
            value={sanitizeInput(formData.fullName)}
            onChange={handleChange}
            placeholder="John Doe"
            color={formErrors.fullName ? "failure" : undefined}
            helperText={formErrors.fullName}
            required
          />
        </div>

        <div>
          <Label htmlFor="email" value="Email" />
          <TextInput
            id="email"
            name="email"
            type="email"
            value={sanitizeInput(formData.email)}
            onChange={handleChange}
            placeholder="john@example.com"
            color={formErrors.email ? "failure" : undefined}
            helperText={formErrors.email}
            icon={HiMail}
            required
          />
        </div>

        <div>
          <Label htmlFor="phone" value="Phone Number" />
          <TextInput
            id="phone"
            name="phone"
            value={sanitizeInput(formData.phone)}
            onChange={handleChange}
            placeholder="(555) 123-4567"
            color={formErrors.phone ? "failure" : undefined}
            helperText={formErrors.phone}
            icon={HiPhone}
            required
          />
        </div>
      </div>
    </Card>
  );
};

ContactInfoForm.propTypes = {
  formData: PropTypes.shape({
    fullName: PropTypes.string,
    email: PropTypes.string,
    phone: PropTypes.string,
  }).isRequired,
  formErrors: PropTypes.shape({
    fullName: PropTypes.string,
    email: PropTypes.string,
    phone: PropTypes.string,
  }),
  handleChange: PropTypes.func.isRequired,
};

ContactInfoForm.defaultProps = {
  formErrors: {},
};

export default ContactInfoForm;