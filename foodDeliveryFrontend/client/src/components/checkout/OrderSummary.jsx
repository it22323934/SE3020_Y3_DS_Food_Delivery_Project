import React from "react";
import { Card } from "flowbite-react";
import { HiShoppingBag, HiDocumentText, HiCheck } from "react-icons/hi";

const OrderSummary = ({ 
  cart, 
  restaurantLocation, 
  formatPrice,
  compact = false 
}) => {
  return (
    <Card className={compact ? "mb-6" : "sticky top-4"}>
      <h2 className="text-xl font-semibold mb-4 flex items-center">
        <HiShoppingBag className="mr-2 text-blue-600" /> Order Summary
      </h2>

      {!compact && (
        <div className="divide-y dark:divide-gray-700">
          {cart.items.map((item, index) => (
            <div key={index} className="py-3 flex justify-between">
              <div>
                <div className="font-medium text-gray-800 dark:text-white flex items-center">
                  <span className="bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-300 text-xs font-semibold px-2 py-0.5 rounded mr-2">
                    {item.quantity}x
                  </span>
                  {item.name}
                </div>

                {item.addOns && item.addOns.length > 0 && (
                  <div className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    {item.addOns.map((addon, idx) => (
                      <span key={idx}>
                        +{addon.quantity}x {addon.name}
                        {idx < item.addOns.length - 1 && ", "}
                      </span>
                    ))}
                  </div>
                )}
              </div>
              <div className="text-gray-700 dark:text-gray-300">
                ${formatPrice(item.itemTotal)}
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="border-t dark:border-gray-700 pt-4 mt-4 space-y-2">
        <div className="flex justify-between pb-2 border-b border-gray-200 dark:border-gray-700">
          <span>
            {cart.items.length} item{cart.items.length !== 1 ? "s" : ""}
          </span>
          <span>${formatPrice(cart.subtotal)}</span>
        </div>

        {cart.discountAmount > 0 && (
          <div className="flex justify-between pb-2 text-green-600 dark:text-green-400">
            <span className="flex items-center">
              {!compact && <HiDocumentText className="mr-1" />}
              Discount{" "}
              {cart.appliedPromotion
                ? `(${cart.appliedPromotion.code})`
                : ""}
            </span>
            <span>-${formatPrice(cart.discountAmount)}</span>
          </div>
        )}

        <div className="flex justify-between text-gray-600 dark:text-gray-400">
          <span>Tax</span>
          <span>${formatPrice(cart.taxAmount)}</span>
        </div>

        <div className="flex justify-between text-gray-600 dark:text-gray-400">
          <span>Delivery Fee</span>
          <span>${formatPrice(cart.deliveryFee || 0)}</span>
        </div>

        <div className="flex justify-between pt-3 border-t dark:border-gray-700 font-bold text-lg text-gray-900 dark:text-white">
          <span>Total</span>
          <span>${formatPrice(cart.total)}</span>
        </div>
      </div>

      {!compact && (
        <>
          {cart.appliedPromotion && (
            <div className="mt-4 bg-green-50 dark:bg-green-900/20 p-3 rounded-lg border border-green-200 dark:border-green-800">
              <div className="flex items-center text-green-700 dark:text-green-400">
                <HiCheck className="mr-1.5 text-green-500" />
                <span className="font-medium">
                  Promo code{" "}
                  <span className="font-bold">
                    {cart.appliedPromotion.code}
                  </span>{" "}
                  applied
                </span>
              </div>
              <div className="text-xs text-gray-600 dark:text-gray-400 mt-1">
                {cart.appliedPromotion.description}
              </div>
            </div>
          )}

          {/* Restaurant info */}
          {restaurantLocation && (
            <div className="mt-4 bg-gray-50 dark:bg-gray-800 p-3 rounded-lg border border-gray-200 dark:border-gray-700">
              <h3 className="font-medium text-gray-800 dark:text-white mb-2">
                Restaurant Information
              </h3>
              <p className="text-sm text-gray-600 dark:text-gray-400">
                {restaurantLocation.name}
              </p>
              <p className="text-xs text-gray-500 dark:text-gray-500">
                {restaurantLocation.address}
              </p>
              <div className="text-xs text-gray-500 dark:text-gray-500 mt-1">
                Maximum delivery distance: 20 km
              </div>
            </div>
          )}

          <div className="mt-4 text-xs text-gray-500 dark:text-gray-400">
            <p>
              By placing your order, you agree to our Terms of Service and
              Privacy Policy.
            </p>
          </div>
        </>
      )}
    </Card>
  );
};

export default OrderSummary;