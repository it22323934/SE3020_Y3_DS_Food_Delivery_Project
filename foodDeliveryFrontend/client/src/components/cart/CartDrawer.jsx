import React from "react";
import { Button, Avatar } from "flowbite-react";
import { HiOutlineX, HiTrash, HiPlus, HiMinus } from "react-icons/hi";
import { useCart } from "../../context/CartContext";

export default function CartDrawer() {
  const {
    cart,
    cartDrawerOpen,
    closeCartDrawer,
    removeFromCart,
    updateQuantity,
    clearCart,
    getCartItemKey,
  } = useCart();

  if (!cartDrawerOpen) return null;

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black bg-opacity-50 z-40 transition-opacity"
        onClick={closeCartDrawer}
      ></div>

      {/* Cart Drawer */}
      <div className="fixed inset-y-0 right-0 w-full sm:w-96 bg-white dark:bg-gray-800 z-50 shadow-xl transform transition-transform duration-300 overflow-auto">
        {/* Header */}
        <div className="sticky top-0 bg-white dark:bg-gray-800 border-b dark:border-gray-700 p-4 flex justify-between items-center">
          <h2 className="text-xl font-bold text-gray-800 dark:text-white">
            Your Cart
          </h2>
          <button
            onClick={closeCartDrawer}
            className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
          >
            <HiOutlineX className="h-6 w-6 text-gray-600 dark:text-gray-300" />
          </button>
        </div>

        {/* Empty Cart State */}
        {cart.items.length === 0 ? (
          <div className="flex flex-col items-center justify-center p-8 text-center h-64">
            <div className="text-gray-400 dark:text-gray-500 text-5xl mb-4">
              🛒
            </div>
            <p className="text-gray-600 dark:text-gray-400 mb-6">
              Your cart is empty
            </p>
            <Button color="primary" onClick={closeCartDrawer}>
              Start Shopping
            </Button>
          </div>
        ) : (
          <>
            {/* Restaurant Name */}
            {cart.restaurantName && (
              <div className="bg-blue-50 dark:bg-blue-900/20 p-3 text-blue-700 dark:text-blue-300 text-sm">
                Items from:{" "}
                <span className="font-medium">{cart.restaurantName}</span>
              </div>
            )}

            {/* Cart Items */}
            <div className="divide-y dark:divide-gray-700 px-4">
              {cart.items.map((item) => {
                const cartItemKey = getCartItemKey(item.id, item.addOns);
                return (
                  <div key={cartItemKey} className="py-3">
                    <div className="flex gap-3">
                      {/* Item Image */}
                      {item.imageUrl ? (
                        <Avatar
                          img={item.imageUrl}
                          size="md"
                          rounded
                          className="flex-shrink-0"
                        />
                      ) : (
                        <div className="w-10 h-10 bg-gray-200 dark:bg-gray-700 rounded-full flex items-center justify-center flex-shrink-0">
                          <span className="text-gray-500 dark:text-gray-400 text-xs">
                            IMG
                          </span>
                        </div>
                      )}

                      {/* Item Details */}
                      <div className="flex-grow">
                        <div className="flex justify-between">
                          <p className="font-medium text-gray-800 dark:text-white">
                            {item.name}
                          </p>
                          <p className="font-medium text-gray-800 dark:text-white">
                            $
                            {isNaN(item.itemTotal)
                              ? "0.00"
                              : item.itemTotal.toFixed(2)}
                          </p>
                        </div>

                        {/* Price per item */}
                        <p className="text-sm text-gray-500 dark:text-gray-400">
                          ${item.price.toFixed(2)} each
                        </p>

                        {/* Add-ons */}
                        {item.addOns && item.addOns.length > 0 && (
                          <div className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                            {item.addOns.map((addon, idx) => (
                              <span key={idx}>
                                +{addon.quantity}x {addon.addOn.name} ($
                                {addon.addOn.price.toFixed(2)})
                                {idx < item.addOns.length - 1 && ", "}
                              </span>
                            ))}
                          </div>
                        )}

                        {/* Quantity Controls */}
                        <div className="mt-2 flex items-center justify-between">
                          <div className="flex items-center border rounded-lg overflow-hidden dark:border-gray-600">
                            <button
                              onClick={() =>
                                updateQuantity(cartItemKey, item.quantity - 1)
                              }
                              className="px-2 py-1 bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600"
                            >
                              <HiMinus className="h-3.5 w-3.5" />
                            </button>
                            <span className="px-3 py-1">{item.quantity}</span>
                            <button
                              onClick={() =>
                                updateQuantity(cartItemKey, item.quantity + 1)
                              }
                              className="px-2 py-1 bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600"
                            >
                              <HiPlus className="h-3.5 w-3.5" />
                            </button>
                          </div>

                          <button
                            onClick={() => removeFromCart(cartItemKey)}
                            className="text-red-600 dark:text-red-400 hover:text-red-800 dark:hover:text-red-300"
                          >
                            <HiTrash className="h-4 w-4" />
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Cart Summary */}
            <div className="px-4 pt-4 pb-6 bg-gray-50 dark:bg-gray-900 mt-auto sticky bottom-0 border-t dark:border-gray-700">
              <div className="space-y-1.5 mb-4">
                <div className="flex justify-between text-gray-600 dark:text-gray-400">
                  <span>Subtotal</span>
                  <span>
                    ${isNaN(cart.subtotal) ? "0.00" : cart.subtotal.toFixed(2)}
                  </span>
                </div>
                <div className="flex justify-between text-gray-600 dark:text-gray-400">
                  <span>Tax</span>
                  <span>
                    $
                    {isNaN(cart.taxAmount) ? "0.00" : cart.taxAmount.toFixed(2)}
                  </span>
                </div>
                <div className="flex justify-between text-gray-600 dark:text-gray-400">
                  <span>Delivery Fee</span>
                  <span>
                    $
                    {isNaN(cart.deliveryFee)
                      ? "0.00"
                      : cart.deliveryFee.toFixed(2)}
                  </span>
                </div>
                <div className="flex justify-between font-bold text-gray-900 dark:text-white pt-2 border-t dark:border-gray-700">
                  <span>Total</span>
                  <span>
                    ${isNaN(cart.total) ? "0.00" : cart.total.toFixed(2)}
                  </span>
                </div>
              </div>

              <div className="flex flex-col gap-2">
                <Button
                  color="success"
                  size="lg"
                  fullSized
                  onClick={() => {
                    closeCartDrawer();
                    // You can use window.location.href if you don't have router access
                    window.location.href = "/checkout";
                  }}
                >
                  Proceed to Checkout
                </Button>

                <Button color="light" size="sm" fullSized onClick={clearCart}>
                  <HiTrash className="mr-1" />
                  Clear Cart
                </Button>
              </div>
            </div>
          </>
        )}
      </div>
    </>
  );
}
