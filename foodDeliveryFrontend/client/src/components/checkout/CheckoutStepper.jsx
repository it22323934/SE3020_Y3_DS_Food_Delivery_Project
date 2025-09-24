import React from "react";
import PropTypes from 'prop-types';
import { HiCheck } from "react-icons/hi";

const CheckoutStepper = ({ currentStep, steps }) => {
  return (
    <div className="max-w-5xl mx-auto mb-8">
      <ol className="flex items-center w-full">
        {steps.map((step, index) => {
          const isActive = currentStep === index;
          const isCompleted = currentStep > index;

          return (
            <li
              key={index}
              className={`flex items-center ${
                index !== steps.length - 1 ? "w-full" : ""
              }`}
            >
              <div
                className={`flex items-center justify-center w-10 h-10 rounded-full 
                ${
                  isActive
                    ? "bg-blue-600 text-white"
                    : isCompleted
                    ? "bg-green-500 text-white"
                    : "bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300"
                }`}
              >
                {isCompleted ? (
                  <HiCheck className="w-5 h-5" />
                ) : (
                  <span className="flex items-center justify-center">
                    {step.icon || index + 1}
                  </span>
                )}
              </div>

              <div className="ml-2">
                <span
                  className={`text-sm font-medium ${
                    isActive
                      ? "text-blue-600 dark:text-blue-400"
                      : isCompleted
                      ? "text-green-600 dark:text-green-400"
                      : "text-gray-500 dark:text-gray-400"
                  }`}
                >
                  {step.label}
                </span>
              </div>

              {index !== steps.length - 1 && (
                <div
                  className={`flex-1 h-0.5 mx-4 ${
                    isCompleted
                      ? "bg-green-500"
                      : "bg-gray-200 dark:bg-gray-700"
                  }`}
                ></div>
              )}
            </li>
          );
        })}
      </ol>
    </div>
  );
};

CheckoutStepper.propTypes = {
  currentStep: PropTypes.number.isRequired,
  steps: PropTypes.arrayOf(PropTypes.shape({
    label: PropTypes.string.isRequired,
    icon: PropTypes.node,
  })).isRequired,
};

export default CheckoutStepper;