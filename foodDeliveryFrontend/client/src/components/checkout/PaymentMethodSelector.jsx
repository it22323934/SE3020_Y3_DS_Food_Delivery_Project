import React from "react";
import { Card, Radio, Label } from "flowbite-react";
import { HiCreditCard } from "react-icons/hi";
import { FaCcVisa, FaCcMastercard } from "react-icons/fa";

const PaymentMethodSelector = ({ paymentMethod, setPaymentMethod }) => {
  // Force card as payment method since it's the only accepted one
  React.useEffect(() => {
    setPaymentMethod("card");
  }, [setPaymentMethod]);

  return (
    <Card className="mb-6">
      <h2 className="text-xl font-semibold mb-4 flex items-center">
        <HiCreditCard className="mr-2 text-blue-600" /> Payment Method
      </h2>

      <div className="space-y-4">
        <div className="flex items-center pl-4 border border-gray-200 rounded dark:border-gray-700 bg-blue-50 dark:bg-blue-900/10">
          <Radio
            id="card-payment"
            name="paymentMethod"
            value="card"
            checked={true}
            readOnly
            className="w-4 h-4"
          />
          <Label
            htmlFor="card-payment"
            className="w-full py-4 ml-2 text-sm font-medium text-gray-900 dark:text-gray-300"
          >
            <div className="flex items-center">
              Credit/Debit Card
              <div className="ml-auto flex space-x-2">
                <FaCcVisa className="text-blue-700 text-2xl" />
                <FaCcMastercard className="text-red-600 text-2xl" />
              </div>
            </div>
          </Label>
        </div>

        <div className="p-4 border border-gray-200 rounded dark:border-gray-700">
          <p className="text-sm text-gray-600 dark:text-gray-400">
            Card details will be collected on the next screen.
          </p>
        </div>
        
        <div className="p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
          <p className="text-xs text-gray-500 dark:text-gray-400">
            Note: Currently, only credit/debit card payments are accepted on our platform.
          </p>
        </div>
      </div>
    </Card>
  );
};

export default PaymentMethodSelector;