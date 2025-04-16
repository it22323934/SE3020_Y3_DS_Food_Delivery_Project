import { Sidebar } from "flowbite-react";
import {
  HiUser,
  HiArrowSmRight,
  HiDocument,
  HiDocumentText,
  HiOutlineUserGroup,
  HiAnnotation,
  HiChartPie,
} from "react-icons/hi";
import React, { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { signOutSuccess } from "../redux/user/userSlice";
import { useDispatch, useSelector } from "react-redux";
import {
  FaMapMarkerAlt,
  FaTrashAlt,
  FaTruck,
  FaCreditCard,
  FaStore,
  FaUtensils,
} from "react-icons/fa";
import { GiCook, GiRecycle } from "react-icons/gi";
import { RiGovernmentLine } from "react-icons/ri";
import { MdLocalShipping } from "react-icons/md";
import { authService } from "../service/authService";
export default function DashSideBar() {
  const dispatch = useDispatch();
  const location = useLocation();
  const [tab, setTab] = useState("");
  const { currentUser } = useSelector((state) => state.user);
  useEffect(() => {
    const urlParams = new URLSearchParams(location.search);
    const tabFromUrl = urlParams.get("tab");
    if (tabFromUrl) {
      setTab(tabFromUrl);
    }
  }, [location.search]);

  const handleSignout = async () => {
    try {
      const response = await authService.logout(currentUser.token);
      if (response.status === 200) {
        dispatch(signOutSuccess());
      } else {
        console.error("Failed to sign out");
      }
    } catch (error) {
      console.log(error.message);
    }
  };
  return (
    <Sidebar className="w-full md:w-56">
      <Sidebar.Items>
        <Sidebar.ItemGroup className="flex flex-col gap-1">
          {currentUser &&
            (currentUser?.roles[0] === "ROLE_ADMIN" ||
              currentUser?.roles[0] === "ROLE_RESTAURANT_ADMIN") && (
              <>
                {/* Dashboard Tab - visible to both admin types */}
                <Link to="/dashboard?tab=waste-management-dashboard">
                  <Sidebar.Item
                    active={tab === "dash" || !tab}
                    icon={HiChartPie}
                    labelColor="dark"
                    as="div"
                  >
                    Dashboard
                  </Sidebar.Item>
                </Link>

                {/* Restaurant Management - visible to both admin types */}
                <Link to="/dashboard?tab=restaurant-management">
                  <Sidebar.Item
                    active={tab === "restaurant-management"}
                    icon={FaStore}
                    labelColor="dark"
                    as="div"
                  >
                    Restaurant
                  </Sidebar.Item>
                </Link>

                {/* Menu Management - visible to both admin types */}
                <Link to="/dashboard?tab=menu-management">
                  <Sidebar.Item
                    active={tab === "menu-management"}
                    icon={FaUtensils}
                    labelColor="dark"
                    as="div"
                  >
                    Menu Item
                  </Sidebar.Item>
                </Link>

                {/* Items that only the main admin should see */}
                {currentUser.roles[0] === "ROLE_ADMIN" && (
                  <>
                    <Link to="/dashboard?tab=user-management">
                      <Sidebar.Item
                        active={tab === "user-management"}
                        icon={HiOutlineUserGroup}
                        labelColor="dark"
                        as="div"
                      >
                        Users
                      </Sidebar.Item>
                    </Link>
                  </>
                )}
              </>
            )}
          {currentUser && currentUser?.roles[0] === "ROLE_ADMIN" && (
            <>
              <Link to="/dashboard?tab=cuisine-management">
                <Sidebar.Item
                  active={tab === "cuisine-management"}
                  icon={GiCook}
                  labelColor="dark"
                  as="div"
                >
                  Cuisine Type
                </Sidebar.Item>
              </Link>
            </>
          )}
          <Link to="/dashboard?tab=profile">
            <Sidebar.Item
              active={tab === "profile"}
              icon={HiUser}
              label={
                currentUser.roles && currentUser.roles.length > 0
                  ? currentUser.roles[0] === "ROLE_ADMIN"
                    ? "Admin"
                    : currentUser.roles[0] === "ROLE_RESTAURANT_ADMIN"
                    ? "Restaurant Admin"
                    : currentUser.roles[0] === "ROLE_DRIVER"
                    ? "Driver"
                    : "User"
                  : "User"
              }
              labelColor="dark"
              as="div"
            >
              Profile
            </Sidebar.Item>
          </Link>
          {/* {currentUser.isDriver && (
            <Link to="/dashboard?tab=driver-requests">
              <Sidebar.Item
                active={tab === "driver-requests"}
                icon={HiDocumentText}
                as="div"
              >
                Waste Requests
              </Sidebar.Item>
            </Link>
          )}
          {currentUser.isAdmin && (
            <>
              <Link to="/dashboard?tab=users">
                <Sidebar.Item
                  active={tab === "users"}
                  icon={HiOutlineUserGroup}
                  as="div"
                >
                  Users
                </Sidebar.Item>
              </Link>
              <Link to="/dashboard?tab=districts">
                <Sidebar.Item
                  active={tab === "districts"}
                  icon={RiGovernmentLine} // Replace with chosen icon
                  as="div"
                >
                  Districts
                </Sidebar.Item>
              </Link>
              <Link to="/dashboard?tab=waste-categories">
                <Sidebar.Item
                  active={tab === "waste-categories"}
                  icon={GiRecycle} // Replace with chosen icon
                  as="div"
                >
                  Waste Category
                </Sidebar.Item>
              </Link>
              <Link to="/dashboard?tab=waste-requests">
                <Sidebar.Item
                  active={tab === "waste-requests"}
                  icon={FaTrashAlt} // Replace with chosen icon
                  as="div"
                >
                  Waste Request
                </Sidebar.Item>
              </Link>
              <Link to="/dashboard?tab=user-payments">
                <Sidebar.Item
                  active={tab === "user-payments"}
                  icon={FaCreditCard} // Replace with chosen icon
                  as="div"
                >
                  Payments
                </Sidebar.Item>
              </Link>
              <Link to="/dashboard?tab=admin-waste-requests">
                <Sidebar.Item
                  active={tab === "admin-waste-requests"}
                  label={currentUser.isAdmin ? "Admin" : "User"}
                  labelColor="dark"
                  icon={FaTrashAlt} // Replace with chosen icon
                  as="div"
                >
                  Requests
                </Sidebar.Item>
              </Link>
              <Link to="/dashboard?tab=admin-payments">
                <Sidebar.Item
                  active={tab === "admin-payments"}
                  label={currentUser.isAdmin ? "Admin" : "User"}
                  labelColor="dark"
                  icon={FaCreditCard} // or FaMoneyBillAlt
                  as="div"
                >
                  Payments
                </Sidebar.Item>
              </Link>
              <Link to="/dashboard?tab=waste-drivers">
                <Sidebar.Item
                  active={tab === "waste-drivers"}
                  icon={MdLocalShipping} // Replace with chosen icon
                  as="div"
                >
                  Drivers
                </Sidebar.Item>
              </Link>
              <Link to="/dashboard?tab=add-device">
                <Sidebar.Item
                  active={tab === "add-device"}
                  icon={MdLocalShipping} // Replace with chosen icon
                  as="div"
                >
                  Add Device
                </Sidebar.Item>
              </Link>
              <Link to="#">
                <Sidebar.Item
                  active={tab === "comments"}
                  icon={HiAnnotation}
                  as="div"
                >
                  Inquiries
                </Sidebar.Item>
              </Link>
            </>
          )} */}
          <Sidebar.Item
            icon={HiArrowSmRight}
            className="cursor-pointer"
            onClick={handleSignout}
          >
            Sign Out
          </Sidebar.Item>
        </Sidebar.ItemGroup>
      </Sidebar.Items>
    </Sidebar>
  );
}
