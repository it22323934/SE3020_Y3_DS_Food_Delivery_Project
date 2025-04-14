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
import { cuisineTypeService } from "../service/cuisineService";
import { GiCook } from "react-icons/gi";
import { useSelector } from "react-redux";
import { CreateCuisineModal } from "./sub-components/cuisine-management/CreateCuisineModal";
import { UpdateCuisineModal } from "./sub-components/cuisine-management/UpdateCuisineModal";
import { ViewCuisineModal } from "./sub-components/cuisine-management/ViewCuisineModal";
export default function DashCuisineManagement() {
  const [loading, setLoading] = useState(true);
  const [isDownloading, setIsDownloading] = useState(false);
  const [error, setError] = useState("");
  const [deleteModal, setDeleteModal] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [search, setSearch] = useState("");
  const [totalCuisines, setTotalCuisines] = useState(0);
  const [totalActiveCuisines, setTotalActiveCuisines] = useState(0);
  const [totalInactiveCuisines, setTotalInactiveCuisines] = useState(0);
  const [cuisines, setCuisines] = useState([]);
  const [selectedCuisines, setSelectedCuisines] = useState([]);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const { currentUser } = useSelector((state) => state.user);
  const [showViewModal, setShowViewModal] = useState(false);
  const [selectedViewCuisine, setSelectedViewCuisine] = useState(null);
  const [showUpdateModal, setShowUpdateModal] = useState(false);
  const [selectedCuisine, setSelectedCuisine] = useState(null);
  const handleUpdateClick = (cuisine) => {
    setSelectedCuisine(cuisine);
    setShowUpdateModal(true);
  };
  const fetchCuisines = async () => {
    setLoading(true);
    try {
      const response = await cuisineTypeService.getAllCuisineTypes(
        currentUser.token
      );
      const data = await response.json();
      if (response.status === 200) {
        setCuisines(data);
        setTotalCuisines(data.length);
        setTotalActiveCuisines(
          data.filter((cuisine) => cuisine.isActive).length
        );
        setTotalInactiveCuisines(
          data.filter((cuisine) => !cuisine.isActive).length
        );
        setLoading(false);
      }
      if (response.status === 404) {
        toast.error(data.message);
        setLoading(false);
      }
      if (response.status === 500) {
        toast.error(data.message);
        setLoading(false);
      }
    } catch (error) {
      setLoading(false);
      setError(error.message);
      toast.error("Something went wrong while fetching cuisines");
    }
  };

  useEffect(() => {
    fetchCuisines();
  }, [search]);

  const [pageNumber, setPageNumber] = useState(0);
  const cuisinesPerPage = 5;

  const pageCount = Math.ceil(cuisines.length / cuisinesPerPage);

  const handlePageChange = ({ selected }) => {
    setPageNumber(selected);
  };

  const handleViewClick = (cuisine) => {
    setSelectedViewCuisine(cuisine);
    setShowViewModal(true);
  };

  const displayCuisines = cuisines
    .slice(pageNumber * cuisines, (pageNumber + 1) * cuisinesPerPage)
    .map((cuisine) => (
      <Table.Body className="divide-y" key={cuisine.id}>
        <Table.Row
          key={cuisine.id}
          className="bg-white dark:border-gray-700 dark:bg-gray-800"
        >
          <Table.Cell>{cuisine.name}</Table.Cell>
          <Table.Cell>
            <Button
              size="sm"
              color="purple"
              onClick={() => handleViewClick(cuisine)}
            >
              <HiEye className="mr-2 h-5 w-5" />
              View
            </Button>
          </Table.Cell>
          <Table.Cell>
            {
              <Badge
                color={
                  cuisine.active === true
                    ? "success"
                    : cuisine.active === false
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
              >
                {cuisine.active ? (
                  <FaCheckCircle color="green" size={20} /> // Checkmark icon for verified
                ) : (
                  <FaTimesCircle color="red" size={20} /> // Cross icon for not verified
                )}
              </Badge>
            }
          </Table.Cell>

          {cuisine.restaurants?.length > 0 ? (
            <Table.Cell className="text-center">
              <p className="text-gray-500 text-sm">
                {cuisine.restaurants?.length}
              </p>
            </Table.Cell>
          ) : (
            <Table.Cell className="text-center">
              <p className="text-gray-500 text-sm">0</p>
            </Table.Cell>
          )}

          <Table.Cell>
            <div className="flex items-center space-x-4">
              <Button
                color="green"
                type="submit"
                outline
                onClick={() => handleUpdateClick(cuisine)}
              >
                <FaClipboardList className="mr-2 h-5 w-5" />
                Update
              </Button>
              <Button color="blue" type="button" outline>
                <FaStore className="mr-2 h-5 w-5" />
                Manage Restaurants
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

  const handleCuisineCreated = () => {
    fetchCuisines();
  };

  const handleCuisineUpdated = () => {
    fetchCuisines();
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
                      Total Cuisines
                    </h3>
                    <p className="text-2xl">{totalCuisines}</p>
                  </div>
                  <GiCook className="bg-yellow-500 text-white  text-5xl p-3 shadow-lg" />
                </div>
              </div>
              <div className="flex flex-col p-3 dark:bg-slate-800 gap-4 md:w-72 w-full rounded-md shadow-md">
                <div className="flex justify-between">
                  <div className="">
                    <h3 className="text-gray-500 text-md uppercase">
                      Active Cuisines
                    </h3>
                    <p className="text-2xl">{totalActiveCuisines}</p>
                  </div>
                  <GiCook className="bg-green-500 text-white  text-5xl p-3 shadow-lg" />
                </div>
              </div>
              <div className="flex flex-col p-3 dark:bg-slate-800 gap-4 md:w-72 w-full rounded-md shadow-md">
                <div className="flex justify-between">
                  <div className="">
                    <h3 className="text-gray-500 text-md uppercase">
                      Inactive Cuisines
                    </h3>
                    <p className="text-2xl">{totalInactiveCuisines}</p>
                  </div>
                  <GiCook className="bg-red-500 text-white text-5xl p-3 shadow-lg" />
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
                Add New Cuisine
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
                placeholder="Select a Supplier"
                isSearchable
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
                  "Download Cuisine Report"
                )}
              </Button>
            </div>
          </div>
          <div className="overflow-x-auto">
            {cuisines.length > 0 ? (
              <Table>
                <Table.Head>
                  <Table.HeadCell>Name</Table.HeadCell>
                  <Table.HeadCell>More Details</Table.HeadCell>
                  <Table.HeadCell>Status</Table.HeadCell>
                  <Table.HeadCell>Total Restaurants</Table.HeadCell>
                  <Table.HeadCell>Actions</Table.HeadCell>
                </Table.Head>
                {displayCuisines}
              </Table>
            ) : (
              <p>No Reports Available</p>
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

          {/** Create Cuisine Modal */}
          <CreateCuisineModal
            show={showCreateModal}
            onClose={() => setShowCreateModal(false)}
            onSuccess={handleCuisineCreated}
            token={currentUser.token}
          />

          {/** Update Cuisine Modal */}
          <UpdateCuisineModal
            show={showUpdateModal}
            onClose={() => setShowUpdateModal(false)}
            onSuccess={handleCuisineUpdated}
            cuisineData={selectedCuisine}
            token={currentUser.token}
          />

          {/** View Cuisine Modal */}
          <ViewCuisineModal
            show={showViewModal}
            onClose={() => setShowViewModal(false)}
            cuisineData={selectedViewCuisine}
          />
        </>
      )}
    </div>
  );
}
