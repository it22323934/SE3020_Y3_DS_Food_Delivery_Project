import React, { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import DashSideBar from "../components/DashSideBar";
import DashProfile from "../components/DashProfile";
import DashUserProfiles from "../components/DashUserProfiles";
import DashRestaurantManagement from "../components/DashRestaurantManagement";
import DashMenuManagement from "../components/DashMenuManagement";
import DashCuisineManagement from "../components/DashCuisineManagement";
import DashMyRestaurant from "../components/DashMyRestaurant";
import DashMenuItemCategoryManagement from "../components/DashMenuItemCategoryManagement";
// import DashRestaurant from "../components/DashRestaurant";
// import DashMenuManagement from "../components/DashMenuManagement";
export default function Dashboard() {
  const location = useLocation();
  const [tab, setTab] = useState("");
  useEffect(() => {
    const urlParams = new URLSearchParams(location.search);
    const tabFromUrl = urlParams.get("tab");
    if (tabFromUrl) {
      setTab(tabFromUrl);
    }
  }, [location.search]);
  return (
    <div className="min-h-screen flex flex-col md:flex-row">
      {/* <DashSideBar /> */}
      <div className=" md:w-56">
        <DashSideBar />
      </div>
      {/* <DashProfile /> */}
      {tab === "profile" && <DashProfile />}
      {/** <DashUserProfiles/> */}
      {tab === "user-management" && <DashUserProfiles />}
      {/** <DashRestaurantManagment/> */}
      {tab === "restaurant-management" && <DashRestaurantManagement />}
      {/** <DashMenuManagement/> */}
      {tab === "menu-management" && <DashMenuManagement />}
      {/** <DashCuisineManagement/> */}
      {tab === "cuisine-management" && <DashCuisineManagement />}
      {/** <DashRestaurant/> */}
      {tab === "my-restaurant" && <DashMyRestaurant />}
      {/** <DashMenuItemCategoryManagement/> */}
      {tab === "menu-item-category-management" && (
        <DashMenuItemCategoryManagement />
      )}
    </div>
  );
}
