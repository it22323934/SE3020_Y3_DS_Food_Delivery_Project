import React, { useState, useEffect } from 'react';
import { Elements } from '@stripe/react-stripe-js';
import { loadStripe } from '@stripe/stripe-js';
import { Spinner, Alert } from 'flowbite-react';
import CheckoutForm from '../CheckoutForm';

const StripePaymentWrapper = ({ clientSecret, onSuccess }) => {
  const [stripePromise, setStripePromise] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const initializeStripe = async () => {
      try {
        const key = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY;
        
        if (!key) {
          throw new Error("Stripe publishable key is missing");
        }
        
        // Log the key format (first few chars) to check it looks right
        console.log("Key format check:", key.substring(0, 8) + "...");

        const stripeInstance = await loadStripe(key);
        setStripePromise(stripeInstance);
      } catch (err) {
        console.error("Stripe initialization error:", err);
        setError("Failed to initialize payment system. Please try again later.");
      } finally {
        setLoading(false);
      }
    };

    initializeStripe();
  }, []);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center p-8">
        <Spinner size="xl" />
        <p className="text-gray-600 dark:text-gray-400 mt-4">
          Initializing payment system...
        </p>
      </div>
    );
  }

  if (error || !stripePromise) {
    return (
      <Alert color="failure">
        <p>{error || "Unable to load payment system. Please refresh or try again later."}</p>
      </Alert>
    );
  }

  return (
    <Elements 
      stripe={stripePromise} 
      options={{ 
        clientSecret,
        appearance: { 
          theme: 'stripe',
          variables: {
            colorPrimary: '#0570de',
            colorBackground: '#ffffff',
            colorText: '#30313d',
            colorDanger: '#df1b41',
            fontFamily: 'Arial, sans-serif',
            spacingUnit: '4px',
            borderRadius: '4px'
          }
        }
      }}
    >
      <CheckoutForm 
        onSuccess={onSuccess} 
        clientSecret={clientSecret} 
      />
    </Elements>
  );
};

export default StripePaymentWrapper;