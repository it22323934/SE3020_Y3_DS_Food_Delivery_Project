import React from "react";
import { useState, useEffect } from "react";
import LoadingSpinner from "./LoadingSpinner";
import {
  Table,
  Button,
  TextInput,
  Badge,
  Modal,
  Spinner,
} from "flowbite-react";
import { HiEye, HiOutlineX } from "react-icons/hi";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import ReactPaginate from "react-paginate";
import {
  FaUsers,
  FaExclamationTriangle,
  FaCheckCircle,
  FaTimesCircle,
  FaClipboardList,
  FaThumbsUp,
  FaStore,
} from "react-icons/fa";
import { AiOutlineSearch } from "react-icons/ai";
import { RiGovernmentLine } from "react-icons/ri";
import Select from "react-select";
import { CreateRestaurantModal } from "./sub-components/restaurant-management/CreateRestaurantModal";
import { useSelector } from "react-redux";
import { restaurantService } from "../service/restaurantService";
import { UpdateRestaurantModal } from "./sub-components/restaurant-management/UpdateRestaurantModal";
import { ViewRestaurantModal } from "./sub-components/restaurant-management/ViewRestaurantModal";
export default function DashRestaurantManagement() {
  const [loading, setLoading] = useState(true);
  const [isDownloading, setIsDownloading] = useState(false);
  const [error, setError] = useState("");
  const [deleteModal, setDeleteModal] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [search, setSearch] = useState("");
  const [restaurants, setRestaurants] = useState([]);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const { currentUser } = useSelector((state) => state.user);
  const [showUpdateModal, setShowUpdateModal] = useState(false);
  const [selectedRestaurantId, setSelectedRestaurantId] = useState(null);
  const [viewModal, setViewModal] = useState(false);

  const fetchRestaurants = async () => {
    try {
      const response = await restaurantService.getAllRestaurants(
        currentUser.token
      );
      if (response.status === 200) {
        const data = await response.json();
        setRestaurants(data);
        setLoading(false);
      }
    } catch (error) {
      console.error("Error fetching restaurants:", error);
      setLoading(false);
    }
  };
  useEffect(() => {
    if (currentUser) {
      fetchRestaurants();
    }
  }, [currentUser]);

  const [pageNumber, setPageNumber] = useState(0);
  const restaurantsPerPage = 5;

  const pageCount = Math.ceil(restaurants.length / restaurantsPerPage);

  const handlePageChange = ({ selected }) => {
    setPageNumber(selected);
  };

  const handleRestaurantCreated = () => {
    setShowCreateModal(false);
    toast.success("Restaurant created successfully", {
      position: toast.POSITION.TOP_RIGHT,
      autoClose: 2000,
    });
  };

  const handleDisplayViewModal = (restaurant) => {
    setViewModal(true);
    setSelectedRestaurant(restaurant);
  };

  const displayRestaurants = restaurants
    .slice(
      pageNumber * restaurantsPerPage,
      (pageNumber + 1) * restaurantsPerPage
    )
    .map((restaurant) => (
      <Table.Body className="divide-y" key={restaurant?._id}>
        <Table.Row
          key={restaurant?._id}
          className="bg-white dark:border-gray-700 dark:bg-gray-800"
        >
          <Table.Cell>{restaurant.name}</Table.Cell>
          <Table.Cell>{restaurant.email}</Table.Cell>
          <Table.Cell>{restaurant.phoneNumber}</Table.Cell>
          <Table.Cell>
            <Button size="sm" color="gray" onClick={()=>handleDisplayViewModal(restaurant)}>
              <HiEye className="mr-2 h-5 w-5"/>
              View
            </Button>
          </Table.Cell>
          <Table.Cell>
            {
              <Badge
                color={
                  restaurant.enabled === true
                    ? "success"
                    : restaurant.enabled === false
                    ? "failure"
                    : "yellow"
                }
                style={{
                  fontSize: "1.2rem",
                  padding: "0.5rem 1rem",
                  borderRadius: "8px",
                  display: "flex", // Use flexbox
                  alignItems: "center", // Center vertically
                  justifyContent: "center", // Center horizontally
                }}
              ></Badge>
            }
          </Table.Cell>
          <Table.Cell>
            <div className="flex items-center space-x-4">
              <Button
                color="green"
                type="submit"
                onClick={() => handleEditClick(restaurant)}
                outline
              >
                <FaClipboardList className="mr-2 h-5 w-5" />
                Update
              </Button>
              <Button size="sm" color="failure" outline>
                <HiOutlineX className="mr-2 h-5 w-5" />
                Delete
              </Button>
            </div>
          </Table.Cell>
        </Table.Row>
      </Table.Body>
    ));

  const [selectedRestaurant, setSelectedRestaurant] = useState(null);

  const handleEditClick = (restaurant) => {
    setSelectedRestaurant(restaurant);
    setShowUpdateModal(true);
  };

  const handleRestaurantUpdated = () => {
    // Refresh restaurant list or update the state
    fetchRestaurants();
  };
  return (
    <div className="table-auto overflow-x-scroll md:mx-auto p-3 scrollbar scrollbar-track-slate-100 scrollbar-thumb-slate-300 dark:scrollbar-track-slate-700 dark:scrollbar-thumb-slate-500">
      <ToastContainer />
      {loading ? (
        <LoadingSpinner />
      ) : (
        <>
          <div className="p-3 md:mx-auto">
            <div className=" flex-wrap flex gap-4 justify-center">
              <div className="flex flex-col p-3 dark:bg-slate-800 gap-4 md:w-72 w-full rounded-md shadow-md">
                <div className="flex justify-between">
                  <div className="">
                    <h3 className="text-gray-500 text-md uppercase">
                      Total Restaurants
                    </h3>
                    <p className="text-2xl">{}</p>
                  </div>
                  <FaStore className="bg-yellow-500 text-white  text-5xl p-3 shadow-lg" />
                </div>
              </div>
              <div className="flex flex-col p-3 dark:bg-slate-800 gap-4 md:w-72 w-full rounded-md shadow-md">
                <div className="flex justify-between">
                  <div className="">
                    <h3 className="text-gray-500 text-md uppercase">
                      Active Restaurants
                    </h3>
                    <p className="text-2xl">{}</p>
                  </div>
                  <FaStore className="bg-green-500 text-white  text-5xl p-3 shadow-lg" />
                </div>
              </div>
              <div className="flex flex-col p-3 dark:bg-slate-800 gap-4 md:w-72 w-full rounded-md shadow-md">
                <div className="flex justify-between">
                  <div className="">
                    <h3 className="text-gray-500 text-md uppercase">
                      Inactive Restaurants
                    </h3>
                    <p className="text-2xl">{}</p>
                  </div>
                  <FaStore className="bg-red-500 text-white text-5xl p-3 shadow-lg" />
                </div>
              </div>
            </div>
          </div>
          <div>
            <div className=" flex items-center mb-2">
              <Button
                outline
                gradientDuoTone="greenToBlue"
                className="ml-4 mr-5"
                onClick={() => setShowCreateModal(true)}
              >
                Add New Restaurant
              </Button>
              <TextInput
                type="text"
                placeholder="Search by district name"
                onChange={(e) => setSearch(e.target.value)}
                rightIcon={AiOutlineSearch}
                className="ml-1 bg-gray-50 border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-80 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500 mb"
              />
              <Select
                className="ml-4"
                placeholder="Select a Restaurant"
                isSearchable
                isClearable
                styles={{
                  control: (provided) => ({
                    ...provided,
                    width: "200px",
                  }),
                  option: (provided) => ({
                    ...provided,
                    color: "black",
                  }),
                  singleValue: (provided) => ({
                    ...provided,
                    color: "black",
                  }),
                }}
              />
              <Button outline gradientDuoTone="greenToBlue" className=" ml-4">
                {isDownloading ? (
                  <Spinner className="animate-spin" color="white" size="sm" />
                ) : (
                  "Download District Report"
                )}
              </Button>
            </div>
          </div>
          <div className="overflow-x-auto">
            {restaurants.length > 0 ? (
              <Table>
                <Table.Head>
                  <Table.HeadCell>Name</Table.HeadCell>
                  <Table.HeadCell>Email</Table.HeadCell>
                  <Table.HeadCell>Phone</Table.HeadCell>
                  <Table.HeadCell>More</Table.HeadCell>
                  <Table.HeadCell>Status</Table.HeadCell>
                  <Table.HeadCell>Actions</Table.HeadCell>
                </Table.Head>
                {displayRestaurants}
              </Table>
            ) : (
              <p>No Restaurants Available</p>
            )}
            <div className="mt-9 center">
              <ReactPaginate
                previousLabel={"Previous"}
                nextLabel={"Next"}
                pageCount={pageCount}
                onPageChange={handlePageChange}
                containerClassName={"pagination flex justify-center"}
                previousLinkClassName={
                  "inline-flex items-center px-4 py-2 border border-gray-300 rounded-l-md bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-300 dark:hover:bg-gray-700"
                }
                nextLinkClassName={
                  "inline-flex items-center px-4 py-2 border border-gray-300 rounded-r-md bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-300 dark:hover:bg-gray-700"
                }
                disabledClassName={"opacity-50 cursor-not-allowed"}
                activeClassName={"bg-indigo-500 text-white"}
              />
            </div>
          </div>

          {/** Create Modal */}
          <CreateRestaurantModal
            show={showCreateModal}
            onClose={() => setShowCreateModal(false)}
            onSuccess={handleRestaurantCreated}
            token={currentUser.token}
          />

          {/** Update Modal */}
          <UpdateRestaurantModal
            show={showUpdateModal}
            onClose={() => setShowUpdateModal(false)}
            restaurant={selectedRestaurant}
            onSuccess={handleRestaurantUpdated}
            token={currentUser.token}
          />

          {/** Verify Modal */}
          <ViewRestaurantModal
            show={viewModal}
            onClose={() => setViewModal(false)}
            restaurant={selectedRestaurant}
            token={currentUser.token}
          />
        </>
      )}
    </div>
  );
}
