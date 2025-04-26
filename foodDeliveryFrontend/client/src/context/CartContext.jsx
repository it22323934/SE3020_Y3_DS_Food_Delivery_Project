import React, {
  createContext,
  useContext,
  useReducer,
  useEffect,
  useState,
} from "react";
import { toast } from "react-toastify";

const CartContext = createContext();

const initialState = {
  items: [],
  restaurantId: null, // To ensure all items are from the same restaurant
  subtotal: 0,
  tax: 0.08, // 8% tax rate - can be configured as needed
  deliveryFee: 3.99,
  total: 0,
};

// Types of actions
const actionTypes = {
  ADD_TO_CART: "ADD_TO_CART",
  REMOVE_FROM_CART: "REMOVE_FROM_CART",
  UPDATE_QUANTITY: "UPDATE_QUANTITY",
  CLEAR_CART: "CLEAR_CART",
  SET_RESTAURANT: "SET_RESTAURANT",
  UPDATE_TOTALS: "UPDATE_TOTALS",
};

// Helper to create a unique key for a cart item including its addons
const getCartItemKey = (itemId, addOns = []) => {
  if (!addOns.length) return itemId;

  const addOnKeys = addOns
    .map((addon) => `${addon.addOn.id}-${addon.quantity}`)
    .sort()
    .join(",");

  return `${itemId}_${addOnKeys}`;
};

// Helper to calculate add-on total
const calculateAddOnTotal = (addOns) => {
  if (!addOns || !addOns.length) return 0;

  return addOns.reduce((total, addon) => {
    return total + addon.addOn.price * addon.quantity;
  }, 0);
};

// Helper to calculate item total including add-ons
const calculateItemTotal = (item, quantity, selectedAddOns) => {
  const basePrice = item.onPromotion ? item.discountedPrice : item.price;
  const addOnTotal = calculateAddOnTotal(selectedAddOns);

  return basePrice * quantity + addOnTotal;
};

// Reducer function
function cartReducer(state, action) {
  switch (action.type) {
    case actionTypes.ADD_TO_CART: {
      const { item, quantity, selectedAddOns, restaurantId } = action.payload;

      // Check if we need to clear cart (items from different restaurant)
      if (
        state.restaurantId &&
        state.restaurantId !== restaurantId &&
        state.items.length > 0
      ) {
        return {
          ...initialState,
          items: [
            {
              id: item.id,
              name: item.name,
              price: Number(
                item.onPromotion ? item.discountedPrice : item.price
              ),
              originalPrice: Number(item.price),
              quantity,
              imageUrl: item.imageUrl,
              addOns: selectedAddOns,
              itemTotal: calculateItemTotal(item, quantity, selectedAddOns),
              restaurantId,
            },
          ],
          restaurantId: restaurantId || state.restaurantId,
        };
      }

      // Generate a unique key for this item + addons combination
      const cartItemKey = getCartItemKey(item.id, selectedAddOns);

      // Check if this exact item (with same addons) is already in cart
      const existingItemIndex = state.items.findIndex(
        (cartItem) =>
          getCartItemKey(cartItem.id, cartItem.addOns) === cartItemKey
      );

      if (existingItemIndex >= 0) {
        // Update existing item
        const updatedItems = [...state.items];
        updatedItems[existingItemIndex] = {
          ...updatedItems[existingItemIndex],
          quantity: updatedItems[existingItemIndex].quantity + quantity,
          itemTotal: calculateItemTotal(
            item,
            updatedItems[existingItemIndex].quantity + quantity,
            selectedAddOns
          ),
        };

        return {
          ...state,
          items: updatedItems,
          restaurantId: restaurantId || state.restaurantId,
        };
      }

      // Add new item
      return {
        ...state,
        items: [
          ...state.items,
          {
            id: item.id,
            name: item.name,
            price: Number(item.onPromotion ? item.discountedPrice : item.price),
            originalPrice: Number(item.price || 0),
            quantity: Number(quantity),
            imageUrl: item.imageUrl,
            addOns: selectedAddOns,
            itemTotal: calculateItemTotal(item, quantity, selectedAddOns),
            restaurantId,
          },
        ],
        restaurantId: restaurantId || state.restaurantId,
      };
    }

    case actionTypes.REMOVE_FROM_CART: {
      const { cartItemKey } = action.payload;

      const updatedItems = state.items.filter(
        (item) => getCartItemKey(item.id, item.addOns) !== cartItemKey
      );

      // If no items left, reset restaurantId
      const updatedRestaurantId =
        updatedItems.length > 0 ? state.restaurantId : null;

      return {
        ...state,
        items: updatedItems,
        restaurantId: updatedRestaurantId,
      };
    }
    case actionTypes.UPDATE_QUANTITY: {
      const { cartItemKey, quantity } = action.payload;

      if (quantity <= 0) {
        // Remove item if quantity is 0 or less
        return cartReducer(state, {
          type: actionTypes.REMOVE_FROM_CART,
          payload: { cartItemKey },
        });
      }

      const updatedItems = state.items.map((item) => {
        if (getCartItemKey(item.id, item.addOns) === cartItemKey) {
          // Use the same logic for calculating item total as when adding to cart
          const itemPrice = Number(item.price || 0);
          const newQuantity = Number(quantity);
          const addOnTotal = calculateAddOnTotal(item.addOns);

          return {
            ...item,
            quantity: newQuantity,
            itemTotal: itemPrice * newQuantity + addOnTotal,
          };
        }
        return item;
      });

      return {
        ...state,
        items: updatedItems,
      };
    }

    case actionTypes.CLEAR_CART:
      return initialState;

    case actionTypes.SET_RESTAURANT: {
      const { restaurant } = action.payload;

      return {
        ...state,
        restaurantName: restaurant.name,
        restaurantId: restaurant.id,
      };
    }

    case actionTypes.UPDATE_TOTALS: {
      const { subtotal, taxAmount, total } = action.payload;
      return {
        ...state,
        subtotal,
        taxAmount,
        total,
      };
    }

    default:
      return state;
  }
}

export function CartProvider({ children }) {
  const [state, dispatch] = useReducer(cartReducer, initialState);
  const [cartDrawerOpen, setCartDrawerOpen] = useState(false);

  useEffect(() => {
    try {
      // Ensure we're using numeric values
      const subtotal = state.items.reduce(
        (total, item) => total + Number(item.itemTotal || 0),
        0
      );
      const taxAmount = subtotal * Number(state.tax || 0);
      const deliveryFee =
        state.items.length > 0 ? Number(state.deliveryFee || 0) : 0;
      const total = subtotal + taxAmount + deliveryFee;

      // Debug totals
      console.log("Cart totals:", { subtotal, taxAmount, deliveryFee, total });

      dispatch({
        type: actionTypes.UPDATE_TOTALS,
        payload: { subtotal, taxAmount, total },
      });
    } catch (error) {
      console.error("Error calculating totals:", error);
    }
  }, [state.items, state.tax, state.deliveryFee]);

  const addToCart = (item, quantity, selectedAddOns) => {
    if (!item || quantity <= 0) return;

    // Check if adding from a different restaurant
    if (
      state.restaurantId &&
      state.restaurantId !== item.restaurantId &&
      state.items.length > 0
    ) {
      // Confirm with user before clearing cart
      const confirmed = window.confirm(
        "Your cart contains items from a different restaurant. Adding this item will clear your current cart. Continue?"
      );

      if (!confirmed) return;

      toast.info("Cart cleared - items from previous restaurant removed");
    }

    console.log("Adding to cart - item:", {
      id: item.id,
      name: item.name,
      price: typeof item.price,
      priceValue: item.price,
      discountedPrice: typeof item.discountedPrice,
      discountedValue: item.discountedPrice,
      onPromotion: item.onPromotion,
      finalPrice: item.onPromotion ? item.discountedPrice : item.price,
    });

    dispatch({
      type: actionTypes.ADD_TO_CART,
      payload: {
        item,
        quantity,
        selectedAddOns,
        restaurantId: item.restaurantId,
      },
    });

    console.log("Item added to cart:", item, quantity, selectedAddOns);
  };

  const removeFromCart = (cartItemKey) => {
    dispatch({
      type: actionTypes.REMOVE_FROM_CART,
      payload: { cartItemKey },
    });

    toast.info("Item removed from cart");
  };

  const updateQuantity = (cartItemKey, quantity) => {
    dispatch({
      type: actionTypes.UPDATE_QUANTITY,
      payload: { cartItemKey, quantity },
    });
  };

  const clearCart = () => {
    dispatch({ type: actionTypes.CLEAR_CART });
    toast.info("Cart cleared");
  };

  const openCartDrawer = () => setCartDrawerOpen(true);
  const closeCartDrawer = () => setCartDrawerOpen(false);

  const setRestaurant = (restaurant) => {
    dispatch({ type: "SET_RESTAURANT", payload: { restaurant } });
  };

  // Values to be provided to consuming components
  const value = {
    cart: state,
    cartCount: state.items.reduce((count, item) => count + item.quantity, 0),
    addToCart,
    removeFromCart,
    updateQuantity,
    setRestaurant,
    clearCart,
    cartDrawerOpen,
    openCartDrawer,
    closeCartDrawer,
    getCartItemKey, // Export helper function for components
  };

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export const useCart = () => useContext(CartContext);
