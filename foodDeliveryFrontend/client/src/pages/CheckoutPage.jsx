import React, { useState, useEffect } from 'react';
import { Button, Card, Alert, Spinner } from 'flowbite-react';
import { Elements } from '@stripe/react-stripe-js';
import { loadStripe } from '@stripe/stripe-js';
import CheckoutForm from '../components/CheckoutForm';
import paymentService from '../service/paymentService';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';

const stripePromise = loadStripe(import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY);

export default function CheckoutPage() {
  const { cart, clearCart } = useCart();
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [clientSecret, setClientSecret] = useState('');
  const [paymentIntentId, setPaymentIntentId] = useState('');
  const [paymentSuccess, setPaymentSuccess] = useState(false);
  const [orderDetails, setOrderDetails] = useState(null);

  useEffect(() => {
  console.log('Cart Total:', cart.total);
    if (!cart.items.length && !paymentSuccess) {
      window.location.href = '/';
      return;
    }

  // Add validation for zero amount
  if (cart.total <= 0) {
    setError('Invalid payment amount');
    setLoading(false);
    return;
  }

    const createPaymentIntent = async () => {
      try {
        setLoading(true);
        console.log('Submitting amount:', cart.total); // Debug
        const response = await paymentService.createPaymentIntent(
          {
            amount: cart.total,
            orderId: `order_${Date.now()}`,
            customerEmail: user?.email || 'test@example.com'
          },
          user?.token || 'mock_jwt_token'
        );

        setClientSecret(response.clientSecret);
        setPaymentIntentId(response.paymentIntentId);
        setError(null);
      } catch (err) {
        setError(err.message || 'Failed to initialize payment');
      } finally {
        setLoading(false);
      }
    };

      // Add debounce to prevent multiple calls
      const debounceTimer = setTimeout(createPaymentIntent, 300);
      return () => clearTimeout(debounceTimer);

    createPaymentIntent();
  }, [cart, user, paymentSuccess]);

  const handlePaymentSuccess = async (paymentIntent) => {
    try {
      const paymentRecord = await paymentService.confirmPayment(
        { paymentIntentId: paymentIntent.id },
        user?.token || 'mock_jwt_token'
      );

      if (paymentRecord.paymentStatus === 'succeeded') {
        clearCart();
        setOrderDetails({
          orderId: paymentRecord.orderId,
          email: user?.email || paymentIntent.receipt_email
        });
        setPaymentSuccess(true);
      } else {
        setError('Payment verification failed: ' +
          (paymentRecord.errorMessage || 'Unknown error'));
      }
    } catch (err) {
      console.error('Confirmation error:', err);
      setError('Payment confirmation failed: ' + err.message);
    }
  };

  if (paymentSuccess) {
    return (
      <div className="max-w-2xl mx-auto p-4 text-center">
        <div className="mb-6">
          <svg className="w-16 h-16 text-green-500 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path>
          </svg>
          <h1 className="text-2xl font-bold mb-2">Payment Successful!</h1>

          <div className="bg-gray-50 p-4 rounded-lg mb-4 text-left">
            <div className="mb-2">
              <span className="font-semibold">Order ID:</span> {orderDetails?.orderId || 'N/A'}
            </div>
            <div className="mb-2">
              <span className="font-semibold">Email:</span> {orderDetails?.email || 'N/A'}
            </div>
            <div>
              <span className="font-semibold">Amount:</span> ${cart.total.toFixed(2)}
            </div>
          </div>

          <p className="text-gray-600 mb-6">
            A confirmation has been sent to your email. Thank you for your order!
          </p>

          <Button
            onClick={() => window.location.href = '/'}
            gradientDuoTone="purpleToBlue"
            className="mx-auto"
          >
            Return to Home
          </Button>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <Spinner size="xl" />
        <span className="ml-3">Loading payment information...</span>
      </div>
    );
  }

  if (error) {
    return (
      <Alert color="failure" className="max-w-md mx-auto mt-8">
        {error}
      </Alert>
    );
  }

  return (
    <div className="max-w-2xl mx-auto p-4">
      <h1 className="text-2xl font-bold mb-6">Checkout</h1>

      <Card className="mb-4">
        <div className="flex justify-between font-bold text-lg mb-2">
          <span>Order Total:</span>
          <span>${cart.total.toFixed(2)}</span>
        </div>
        <p className="text-gray-600 text-sm">
          Paying as: {user?.email || 'test@example.com'}
        </p>
      </Card>

      {clientSecret && (
        <Elements stripe={stripePromise} options={{ clientSecret }}>
          <CheckoutForm
            onSuccess={handlePaymentSuccess}
            clientSecret={clientSecret}
          />
        </Elements>
      )}
    </div>
  );
}