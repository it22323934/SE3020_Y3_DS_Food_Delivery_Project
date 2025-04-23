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
import { MdLocalShipping, MdRestaurantMenu } from "react-icons/md";
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

  // Helper function to check if user has a specific role
  const hasRole = (role) => {
    return currentUser?.roles?.includes(role);
  };

  // Determine the primary role for display
  const getPrimaryRoleLabel = () => {
    if (!currentUser?.roles || currentUser.roles.length === 0) return "User";

    if (hasRole("ROLE_ADMIN")) return "Admin";
    if (hasRole("ROLE_RESTAURANT_ADMIN")) return "Restaurant Admin";
    if (hasRole("ROLE_DRIVER")) return "Driver";
    return "User";
  };

  return (
    <Sidebar className="w-full md:w-56">
      <Sidebar.Items>
        <Sidebar.ItemGroup className="flex flex-col gap-1">
          {currentUser &&
            (hasRole("ROLE_ADMIN") || hasRole("ROLE_RESTAURANT_ADMIN")) && (
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
                {hasRole("ROLE_ADMIN") && (
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

          {currentUser && hasRole("ROLE_ADMIN") && (
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
          )}

          {currentUser && hasRole("ROLE_RESTAURANT_ADMIN") && (
            <>
              <Link to="/dashboard?tab=menu-item-category-management">
                <Sidebar.Item
                  active={tab === "menu-item-category-management"}
                  icon={MdRestaurantMenu}
                  labelColor="dark"
                  as="div"
                >
                  Menu Categories
                </Sidebar.Item>
              </Link>
              <Link to="/dashboard?tab=my-restaurant">
                <Sidebar.Item
                  active={tab === "my-restaurant"}
                  icon={FaStore}
                  labelColor="dark"
                  as="div"
                >
                  My Restaurant
                </Sidebar.Item>
              </Link>
            </>
          )}

          {/* Cuisine management for restaurants */}
          {currentUser && hasRole("ROLE_RESTAURANT") && (
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
          )}

          {/* Profile link for all users */}
          <Link to="/dashboard?tab=profile">
            <Sidebar.Item
              active={tab === "profile"}
              icon={HiUser}
              label={getPrimaryRoleLabel()}
              labelColor="dark"
              as="div"
            >
              Profile
            </Sidebar.Item>
          </Link>


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
