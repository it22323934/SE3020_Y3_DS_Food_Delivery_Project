import React, { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
// import DashSideBar from "../components/DashSideBar";
// import DashProfile from "../components/DashProfile";
// import DashUserProfiles from "../components/DashUserProfiles";
// import DashDistrict from "../components/DashDistrict";
// import DashWasteReuest from "../components/DashWasteRequest";
// import DashWasteRequest from "../components/DashWasteRequest";
 import DashWasteDrivers from "../components/DashWasteDrivers";
// import DashWasteCategory from "../components/DashWasteCategory";
// import DashAdminWasteRequests from "../components/DashAdminWasteRequests";
// import DashDriverRequests from "../components/DashDriverRequests";
// //import DashAdminPayments from "../components/DashAdminPayments";
//import DashMyPayments from "../components/DashMyPayments";
//import DashWasteDashBoard from "../components/DashWasteDashBoard";
//import DashRestaurant from "../components/DashRestaurant";
//import DashMenuManagement from "../components/DashMenuManagement";
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
      {tab === "users" && <DashUserProfiles/>}
      {/** <DashRestaurant/> */}
      {tab === "restaurant-management" && <DashRestaurant/>}
      {/** <DashMenuManagement/> */}
      {tab === "menu-management" && <DashMenuManagement/>}

    </div>
  );
}
