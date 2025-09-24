import { Button } from "flowbite-react";
import React from "react";
import { AiFillGoogleCircle } from "react-icons/ai";
import { GoogleAuthProvider, signInWithPopup, getAuth } from "firebase/auth";
import { app } from "../firebase";
import { useDispatch } from "react-redux";
import { signInSuccess } from "../redux/user/userSlice";
import { useNavigate } from "react-router-dom";
import { ToastContainer, toast } from "react-toastify";
import { authService } from "../service/authService";
import { sanitizeInput, escapeHtml } from "../utils/sanitize";
export default function OAuth() {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const auth = getAuth(app);
  const handleGoogleClick = async () => {
    const provider = new GoogleAuthProvider();
    provider.setCustomParameters({ prompt: "select_account" });
    try {
      const resultFromGoogle = await signInWithPopup(auth, provider);
      
      // Sanitize user data from Google
      let userData = {
        email: sanitizeInput(resultFromGoogle.user.email),
        name: sanitizeInput(resultFromGoogle.user.displayName),
        googlePhotoURL: sanitizeInput(resultFromGoogle.user.photoURL),
      };

      const res = await authService.google(userData);
      const data = await res.json();

      if (res.ok) {
        // Sanitize user data before dispatching to Redux store
        const sanitizedData = {
          ...data,
          email: escapeHtml(data.email),
          username: escapeHtml(data.username),
          name: escapeHtml(data.name),
          photoURL: sanitizeInput(data.photoURL),
        };
        dispatch(signInSuccess(sanitizedData));
        navigate("/");
      } else {
        toast.error(escapeHtml(data.message));
      }
    } catch (error) {
      console.error("Google OAuth Error:", error);
      toast.error("An error occurred during Google sign-in");
    }
  };

  return (
    <Button
      type="button"
      gradientDuoTone="pinkToOrange"
      outline
      onClick={handleGoogleClick}
    >
      <AiFillGoogleCircle className="w-6 h-6 mr-2" />
      Continue with Google
    </Button>
  );
}
