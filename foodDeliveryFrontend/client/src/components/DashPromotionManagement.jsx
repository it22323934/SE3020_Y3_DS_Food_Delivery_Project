import React, { useState, useEffect } from "react";
import {
  Table,
  Button,
  TextInput,
  Badge,
  Modal,
  Spinner,
  Alert,
  Tooltip,
} from "flowbite-react";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import ReactPaginate from "react-paginate";
import {
  FaTag,
  FaRegCalendarAlt,
  FaExclamationTriangle,
  FaTimesCircle,
  FaPlusCircle,
  FaEdit,
  FaTrashAlt,
  FaStore,
  FaEye,
  FaFilter,
} from "react-icons/fa";
import { AiOutlineSearch } from "react-icons/ai";
import { useSelector } from "react-redux";
import { promotionService } from "../service/promotionService";
import { restaurantService } from "../service/restaurantService";
import LoadingSpinner from "./LoadingSpinner";
import CreatePromotionModal from "./sub-components/promotion-management/CreatePromotionModal";
import UpdatePromotionModal from "./sub-components/promotion-management/UpdatePromotionModal";
import ViewPromotionModal from "./sub-components/promotion-management/ViewPromotionModal";

export default function DashPromotionManagement() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [deleteModal, setDeleteModal] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [promotions, setPromotions] = useState([]);
  const [restaurant, setRestaurant] = useState(null);
  const [totalPromotions, setTotalPromotions] = useState(0);
  const [activePromotions, setActivePromotions] = useState(0);
  const [expiredPromotions, setExpiredPromotions] = useState(0);
  const [search, setSearch] = useState("");
  const [filteredPromotions, setFilteredPromotions] = useState([]);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showUpdateModal, setShowUpdateModal] = useState(false);
  const [showViewModal, setShowViewModal] = useState(false);
  const [selectedPromotion, setSelectedPromotion] = useState(null);
  const [promotionToDelete, setPromotionToDelete] = useState(null);
  const { currentUser } = useSelector((state) => state.user);
  const [statusFilter, setStatusFilter] = useState("");
  const [minDiscount, setMinDiscount] = useState("");
  const [maxDiscount, setMaxDiscount] = useState("");
  const [dateRangeFilter, setDateRangeFilter] = useState("all"); // 'all', 'active', 'upcoming', 'expired'

  const [pageNumber, setPageNumber] = useState(0);
  const promotionsPerPage = 5;

  useEffect(() => {
    if (currentUser?.token) {
      fetchRestaurantInfo();
    }
  }, [currentUser]);

  useEffect(() => {
    if (restaurant?.id) {
      fetchPromotions();
    }
  }, [restaurant]);

  useEffect(() => {
    if (promotions.length > 0) {
      let filtered = promotions;

      // Text search filter
      if (search) {
        filtered = filtered.filter(
          (promo) =>
            promo.code.toLowerCase().includes(search.toLowerCase()) ||
            promo.description.toLowerCase().includes(search.toLowerCase())
        );
      }

      // Status filter
      if (statusFilter) {
        filtered = filtered.filter((promo) => {
          const { status } = getPromotionStatus(promo);
          return status === statusFilter;
        });
      }

      // Discount range filter
      if (minDiscount !== "") {
        filtered = filtered.filter(
          (promo) => promo.discountPercentage >= parseInt(minDiscount)
        );
      }

      if (maxDiscount !== "") {
        filtered = filtered.filter(
          (promo) => promo.discountPercentage <= parseInt(maxDiscount)
        );
      }

      // Date range filter
      if (dateRangeFilter !== "all") {
        const now = new Date();

        if (dateRangeFilter === "active") {
          filtered = filtered.filter((promo) => {
            const startDate = new Date(promo.startDate);
            const endDate = new Date(promo.endDate);
            return startDate <= now && endDate >= now && promo.active !== false;
          });
        } else if (dateRangeFilter === "upcoming") {
          filtered = filtered.filter((promo) => {
            const startDate = new Date(promo.startDate);
            return startDate > now && promo.active !== false;
          });
        } else if (dateRangeFilter === "expired") {
          filtered = filtered.filter((promo) => {
            const endDate = new Date(promo.endDate);
            return endDate < now || promo.active === false;
          });
        }
      }

      setFilteredPromotions(filtered);
      setPageNumber(0);
    }
  }, [
    search,
    promotions,
    statusFilter,
    minDiscount,
    maxDiscount,
    dateRangeFilter,
  ]);

  const resetFilters = () => {
    setSearch("");
    setStatusFilter("");
    setMinDiscount("");
    setMaxDiscount("");
    setDateRangeFilter("all");
  };

  const fetchRestaurantInfo = async () => {
    try {
      const response = await restaurantService.getRestaurantsByUserId(
        currentUser.id,
        currentUser.token
      );

      if (response.ok) {
        const data = await response.json();
        if (data && data.length > 0) {
          setRestaurant(data[0]);
        } else {
          toast.info(
            "You don't have any restaurants assigned to you. Please contact the system administrator."
          );
        }
      } else {
        toast.error("Failed to fetch restaurant details");
      }
    } catch (error) {
      console.error("Error fetching restaurant:", error);
      setError(error.message);
    }
  };

  const fetchPromotions = async () => {
    try {
      setLoading(true);
      const response = await promotionService.getPromotionsByRestaurantId(
        restaurant.id,
        currentUser.token
      );

      if (response.ok) {
        const data = await response.json();
        setPromotions(data);

        // Calculate statistics
        const now = new Date();
        const active = data.filter(
          (promo) =>
            new Date(promo.endDate) > now && new Date(promo.startDate) <= now
        ).length;

        const expired = data.filter(
          (promo) => new Date(promo.endDate) < now
        ).length;

        setTotalPromotions(data.length);
        setActivePromotions(active);
        setExpiredPromotions(expired);
      } else {
        const errorText = await response.text();
        console.error("Failed to fetch promotions:", errorText);
        setPromotions([]);
      }
    } catch (error) {
      console.error("Error fetching promotions:", error);
      setError(error.message);
    } finally {
      setLoading(false);
    }
  };

  const handlePageChange = ({ selected }) => {
    setPageNumber(selected);
  };

  const handleViewClick = (promotion) => {
    setSelectedPromotion(promotion);
    setShowViewModal(true);
  };

  const handleUpdateClick = (promotion) => {
    setSelectedPromotion(promotion);
    setShowUpdateModal(true);
  };

  const handleDeleteClick = (promotion) => {
    setPromotionToDelete(promotion);
    setDeleteModal(true);
  };

  const handleDeleteConfirm = async () => {
    if (!promotionToDelete) return;

    setIsDeleting(true);
    try {
      const response = await promotionService.deletePromotion(
        promotionToDelete.id,
        currentUser.token
      );

      if (response.ok) {
        toast.success("Promotion deleted successfully");
        fetchPromotions();
      } else {
        const errorText = await response.text();
        toast.error("Failed to delete promotion: " + errorText);
      }
    } catch (error) {
      toast.error("Error deleting promotion");
    } finally {
      setDeleteModal(false);
      setPromotionToDelete(null);
      setIsDeleting(false);
    }
  };

  const handlePromotionCreated = () => {
    setShowCreateModal(false);
    fetchPromotions();
    toast.success("Promotion created successfully");
  };

  const handlePromotionUpdated = () => {
    setShowUpdateModal(false);
    fetchPromotions();
    toast.success("Promotion updated successfully");
  };

  const getPromotionStatus = (promotion) => {
    // First, check if promotion has been manually deactivated
    if (promotion.active === false) {
      return { status: "inactive", label: "Inactive", color: "gray" };
    }

    // If not manually deactivated, check date-based status
    const now = new Date();
    const startDate = new Date(promotion.startDate);
    const endDate = new Date(promotion.endDate);

    if (startDate > now) {
      return { status: "upcoming", label: "Upcoming", color: "warning" };
    } else if (endDate < now) {
      return { status: "expired", label: "Expired", color: "failure" };
    } else {
      return { status: "active", label: "Active", color: "success" };
    }
  };

  const pageCount = Math.ceil(filteredPromotions.length / promotionsPerPage);
  const displayedPromotions = filteredPromotions.slice(
    pageNumber * promotionsPerPage,
    (pageNumber + 1) * promotionsPerPage
  );

  if (loading && !restaurant) {
    return <LoadingSpinner />;
  }

  if (!restaurant) {
    return (
      <div className="p-4">
        <Alert color="info" icon={FaExclamationTriangle}>
          <span className="font-medium">No restaurant found!</span> You don't
          have any restaurants assigned to your account. Please contact the
          system administrator.
        </Alert>
      </div>
    );
  }

  return (
    <div className="table-auto overflow-x-scroll md:mx-auto p-3 scrollbar scrollbar-track-slate-100 scrollbar-thumb-slate-300 dark:scrollbar-track-slate-700 dark:scrollbar-thumb-slate-500">
      <ToastContainer />

      {/* Stats Cards - Enhanced with better styling and container */}
      <div className="p-4 md:mx-auto mb-6 bg-white dark:bg-gray-800 rounded-lg shadow-sm">
        <h2 className="text-lg font-medium text-gray-700 dark:text-white mb-4 flex items-center">
          <FaFilter className="mr-2 text-blue-600" />
          Promotion Statistics
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="flex p-4 bg-gradient-to-br from-blue-50 to-blue-100 dark:from-gray-700 dark:to-gray-800 rounded-lg shadow-sm">
            <div className="flex-1">
              <h3 className="text-gray-600 dark:text-gray-300 text-sm font-medium uppercase tracking-wider">
                Total Promotions
              </h3>
              <p className="text-2xl font-bold text-gray-800 dark:text-white mt-2">
                {totalPromotions}
              </p>
            </div>
            <div className="flex items-center justify-center">
              <div className="bg-blue-500 text-white p-3 rounded-lg shadow-lg">
                <FaTag size={24} />
              </div>
            </div>
          </div>

          <div className="flex p-4 bg-gradient-to-br from-green-50 to-green-100 dark:from-gray-700 dark:to-gray-800 rounded-lg shadow-sm">
            <div className="flex-1">
              <h3 className="text-gray-600 dark:text-gray-300 text-sm font-medium uppercase tracking-wider">
                Active Promotions
              </h3>
              <p className="text-2xl font-bold text-gray-800 dark:text-white mt-2">
                {activePromotions}
              </p>
            </div>
            <div className="flex items-center justify-center">
              <div className="bg-green-500 text-white p-3 rounded-lg shadow-lg">
                <FaTag size={24} />
              </div>
            </div>
          </div>

          <div className="flex p-4 bg-gradient-to-br from-red-50 to-red-100 dark:from-gray-700 dark:to-gray-800 rounded-lg shadow-sm">
            <div className="flex-1">
              <h3 className="text-gray-600 dark:text-gray-300 text-sm font-medium uppercase tracking-wider">
                Expired Promotions
              </h3>
              <p className="text-2xl font-bold text-gray-800 dark:text-white mt-2">
                {expiredPromotions}
              </p>
            </div>
            <div className="flex items-center justify-center">
              <div className="bg-red-500 text-white p-3 rounded-lg shadow-lg">
                <FaRegCalendarAlt size={24} />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Page Header and Actions */}
      <div className="mb-8 bg-white dark:bg-gray-800 p-4 rounded-lg shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-gray-200 dark:border-gray-700 pb-4 mb-4">
          <div className="flex-1">
            <h2 className="text-xl font-semibold text-gray-800 dark:text-white flex items-center">
              <FaStore className="mr-2 text-blue-600" />
              {restaurant.name} - Promotions & Discounts
            </h2>
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
              Manage discounts and special offers for your customers
            </p>
          </div>
          <div>
            <Button
              outline
              gradientDuoTone="greenToBlue"
              onClick={() => setShowCreateModal(true)}
              className="flex items-center"
            >
              <FaPlusCircle className="mr-2" />
              Create Promotion
            </Button>
          </div>
        </div>

        {/* Filters Section */}
        <div className="space-y-5">
          {/* Search */}
          <div className="w-full">
            <TextInput
              type="text"
              placeholder="Search promotions by code or description..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              rightIcon={AiOutlineSearch}
            />
          </div>

          {/* Filter Controls */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {/* Status Filter */}
            <div className="col-span-1">
              <label
                htmlFor="statusFilter"
                className="block mb-2 text-sm font-medium text-gray-900 dark:text-white"
              >
                Status
              </label>
              <select
                id="statusFilter"
                className="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <option value="">All Statuses</option>
                <option value="active">Active</option>
                <option value="upcoming">Upcoming</option>
                <option value="expired">Expired</option>
                <option value="inactive">Inactive</option>
              </select>
            </div>

            {/* Date Range Filter */}
            <div className="col-span-1">
              <label
                htmlFor="dateRangeFilter"
                className="block mb-2 text-sm font-medium text-gray-900 dark:text-white"
              >
                Date Range
              </label>
              <select
                id="dateRangeFilter"
                className="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500"
                value={dateRangeFilter}
                onChange={(e) => setDateRangeFilter(e.target.value)}
              >
                <option value="all">All Dates</option>
                <option value="active">Currently Active</option>
                <option value="upcoming">Upcoming</option>
                <option value="expired">Expired</option>
              </select>
            </div>

            {/* Discount Range Filter */}
            <div className="col-span-1">
              <label className="block mb-2 text-sm font-medium text-gray-900 dark:text-white">
                Discount Range (%)
              </label>
              <div className="flex items-center gap-3">
                <TextInput
                  id="minDiscount"
                  type="number"
                  placeholder="Min %"
                  value={minDiscount}
                  onChange={(e) => setMinDiscount(e.target.value)}
                  className="flex-1"
                  min="0"
                  max="100"
                />
                <span className="text-gray-500 dark:text-gray-400">to</span>
                <TextInput
                  id="maxDiscount"
                  type="number"
                  placeholder="Max %"
                  value={maxDiscount}
                  onChange={(e) => setMaxDiscount(e.target.value)}
                  className="flex-1"
                  min="0"
                  max="100"
                />
              </div>
            </div>
          </div>

          {/* Active Filters Display */}
          <div className="pt-4 border-t border-gray-200 dark:border-gray-700 flex flex-wrap items-center gap-3">
            {search ||
            statusFilter ||
            minDiscount ||
            maxDiscount ||
            dateRangeFilter !== "all" ? (
              <>
                <div className="bg-blue-50 text-blue-800 px-3 py-2 rounded-lg text-sm flex items-center dark:bg-blue-900/30 dark:text-blue-300 mr-auto">
                  <FaFilter className="mr-2" />
                  <span className="font-medium">Filters Applied:</span>
                  <span className="ml-1">
                    {filteredPromotions.length} of {promotions.length}{" "}
                    promotions shown
                  </span>
                </div>

                <div className="flex flex-wrap gap-2">
                  {search && (
                    <Badge
                      color="info"
                      className="px-3 py-1.5 text-sm flex items-center"
                    >
                      Search: "{search}"
                      <button
                        onClick={() => setSearch("")}
                        className="ml-2 hover:text-red-500"
                      >
                        <FaTimesCircle />
                      </button>
                    </Badge>
                  )}

                  {statusFilter && (
                    <Badge
                      color="purple"
                      className="px-3 py-1.5 text-sm flex items-center"
                    >
                      Status:{" "}
                      {statusFilter.charAt(0).toUpperCase() +
                        statusFilter.slice(1)}
                      <button
                        onClick={() => setStatusFilter("")}
                        className="ml-2 hover:text-red-500"
                      >
                        <FaTimesCircle />
                      </button>
                    </Badge>
                  )}

                  {dateRangeFilter !== "all" && (
                    <Badge
                      color="indigo"
                      className="px-3 py-1.5 text-sm flex items-center"
                    >
                      Date:{" "}
                      {dateRangeFilter.charAt(0).toUpperCase() +
                        dateRangeFilter.slice(1)}
                      <button
                        onClick={() => setDateRangeFilter("all")}
                        className="ml-2 hover:text-red-500"
                      >
                        <FaTimesCircle />
                      </button>
                    </Badge>
                  )}

                  {(minDiscount || maxDiscount) && (
                    <Badge
                      color="success"
                      className="px-3 py-1.5 text-sm flex items-center"
                    >
                      Discount: {minDiscount || "0"}% to {maxDiscount || "100"}%
                      <button
                        onClick={() => {
                          setMinDiscount("");
                          setMaxDiscount("");
                        }}
                        className="ml-2 hover:text-red-500"
                      >
                        <FaTimesCircle />
                      </button>
                    </Badge>
                  )}

                  <Button color="light" size="xs" onClick={resetFilters}>
                    <FaTimesCircle className="mr-2" />
                    Clear All
                  </Button>
                </div>
              </>
            ) : (
              <p className="text-sm text-gray-500 dark:text-gray-400">
                No filters applied. Showing all promotions.
              </p>
            )}
          </div>
        </div>
      </div>

      {loading ? (
        <LoadingSpinner />
      ) : filteredPromotions.length > 0 ? (
        <div className="overflow-x-auto">
          <Table>
            <Table.Head>
              <Table.HeadCell>Code</Table.HeadCell>
              <Table.HeadCell>Description</Table.HeadCell>
              <Table.HeadCell>Discount</Table.HeadCell>
              <Table.HeadCell>Valid Period</Table.HeadCell>
              <Table.HeadCell>Status</Table.HeadCell>
              <Table.HeadCell>Actions</Table.HeadCell>
            </Table.Head>
            <Table.Body className="divide-y">
              {displayedPromotions.map((promotion) => {
                const { status, label, color } = getPromotionStatus(promotion);
                return (
                  <Table.Row
                    key={promotion.id}
                    className="bg-white dark:border-gray-700 dark:bg-gray-800"
                  >
                    <Table.Cell className="font-medium">
                      {promotion.code}
                    </Table.Cell>
                    <Table.Cell className="max-w-xs truncate">
                      <Tooltip content={promotion.description}>
                        {promotion.description}
                      </Tooltip>
                    </Table.Cell>
                    <Table.Cell>
                      {promotion.discountPercentage}%
                      <div className="text-xs text-gray-500">
                        (max ${promotion.maxDiscount})
                      </div>
                    </Table.Cell>
                    <Table.Cell>
                      <div className="text-xs">
                        <div className="mb-1">
                          Start:{" "}
                          {new Date(promotion.startDate).toLocaleDateString()}
                        </div>
                        <div>
                          End:{" "}
                          {new Date(promotion.endDate).toLocaleDateString()}
                        </div>
                      </div>
                    </Table.Cell>
                    <Table.Cell>
                      <Badge
                        color={color}
                        className="px-2.5 py-1.5 rounded-full text-xs font-medium"
                      >
                        {label}
                      </Badge>
                    </Table.Cell>
                    <Table.Cell>
                      <div className="flex items-center space-x-2">
                        <Button
                          size="sm"
                          color="purple"
                          onClick={() => handleViewClick(promotion)}
                        >
                          <FaEye className="mr-2 h-4 w-4" />
                          View
                        </Button>
                        <Button
                          color="success"
                          size="sm"
                          outline
                          onClick={() => handleUpdateClick(promotion)}
                        >
                          <FaEdit className="mr-2 h-4 w-4" />
                          Edit
                        </Button>
                        <Button
                          color="failure"
                          size="sm"
                          outline
                          onClick={() => handleDeleteClick(promotion)}
                        >
                          <FaTrashAlt className="mr-2 h-4 w-4" />
                          Delete
                        </Button>
                      </div>
                    </Table.Cell>
                  </Table.Row>
                );
              })}
            </Table.Body>
          </Table>

          {filteredPromotions.length > 0 && pageCount > 1 && (
            <div className="py-4 mt-4 border-t border-gray-200 dark:border-gray-700">
              <ReactPaginate
                previousLabel={"Previous"}
                nextLabel={"Next"}
                pageCount={pageCount}
                onPageChange={handlePageChange}
                forcePage={pageNumber}
                containerClassName="flex justify-center items-center space-x-1"
                pageClassName="inline-flex"
                pageLinkClassName="px-3 py-2 leading-tight text-gray-500 bg-white border border-gray-300 hover:bg-gray-100 hover:text-gray-700 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-white"
                previousLinkClassName="px-3 py-2 leading-tight text-gray-500 bg-white border border-gray-300 rounded-l-lg hover:bg-gray-100 hover:text-gray-700 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-white"
                nextLinkClassName="px-3 py-2 leading-tight text-gray-500 bg-white border border-gray-300 rounded-r-lg hover:bg-gray-100 hover:text-gray-700 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-white"
                activeLinkClassName="!bg-blue-50 !text-blue-600 !border-blue-300 dark:!bg-gray-700 dark:!text-white"
                disabledLinkClassName="opacity-50 cursor-not-allowed"
                breakLabel="..."
                breakLinkClassName="px-3 py-2 leading-tight text-gray-500 bg-white border border-gray-300 hover:bg-gray-100 hover:text-gray-700 dark:bg-gray-800 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-gray-700 dark:hover:text-white"
              />
            </div>
          )}
        </div>
      ) : (
        <div className="text-center py-10">
          <div className="inline-block p-3 bg-gray-100 dark:bg-gray-700 rounded-full mb-4">
            <FaTag size={30} className="text-gray-400 dark:text-gray-500" />
          </div>
          <h3 className="text-lg font-medium text-gray-900 dark:text-white">
            No promotions found
          </h3>
          <p className="text-gray-500 dark:text-gray-400 mt-2">
            {search
              ? "No promotions match your search."
              : "This restaurant doesn't have any promotions yet."}
          </p>
          <Button
            gradientDuoTone="greenToBlue"
            className="mt-4"
            onClick={() => setShowCreateModal(true)}
          >
            <FaPlusCircle className="mr-2" />
            Create First Promotion
          </Button>
        </div>
      )}

      {/* Modals */}
      <CreatePromotionModal
        show={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onSuccess={handlePromotionCreated}
        restaurantId={restaurant.id}
        token={currentUser.token}
      />

      {selectedPromotion && (
        <>
          <UpdatePromotionModal
            show={showUpdateModal}
            onClose={() => setShowUpdateModal(false)}
            onSuccess={handlePromotionUpdated}
            promotion={selectedPromotion}
            token={currentUser.token}
          />
          <ViewPromotionModal
            show={showViewModal}
            onClose={() => setShowViewModal(false)}
            promotion={selectedPromotion}
          />
        </>
      )}

      {/* Delete Confirmation Modal */}
      <Modal
        show={deleteModal}
        size="md"
        popup
        onClose={() => setDeleteModal(false)}
      >
        <Modal.Header />
        <Modal.Body>
          <div className="text-center">
            <FaExclamationTriangle className="mx-auto mb-4 h-14 w-14 text-red-500" />
            <h3 className="mb-5 text-lg font-normal text-gray-500 dark:text-gray-400">
              Are you sure you want to delete this promotion?
            </h3>
            <div className="flex justify-center gap-4">
              <Button
                color="failure"
                onClick={handleDeleteConfirm}
                disabled={isDeleting}
              >
                {isDeleting ? (
                  <>
                    <Spinner size="sm" className="mr-2" />
                    Deleting...
                  </>
                ) : (
                  "Yes, delete it"
                )}
              </Button>
              <Button color="gray" onClick={() => setDeleteModal(false)}>
                Cancel
              </Button>
            </div>
          </div>
        </Modal.Body>
      </Modal>
    </div>
  );
}
