import React, { useState, useEffect } from "react";
import { useSelector } from "react-redux";
import {
  Button,
  Card,
  Spinner,
  Table,
  Badge,
  TextInput,
  Dropdown,
} from "flowbite-react";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import {
  HiPlusCircle,
  HiOutlineSearch,
  HiOutlinePencilAlt,
  HiOutlineTrash,
  HiOutlineEye,
  HiFilter,
} from "react-icons/hi";
import { FaUtensils } from "react-icons/fa";
import { restaurantService } from "../service/restaurantService";
import ReactPaginate from "react-paginate";
import ViewMenuItemModal from "./sub-components/menu-item-management/ViewMenuItemModal";
import CreateMenuItemModal from "./sub-components/menu-item-management/CreateMenuItemModal";
import { menuCategoryService } from "../service/menuCategoryService";
import { menuItemService } from "../service/menuItemService";
import DeleteMenuItemModal from "./sub-components/menu-item-management/DeleteMenuItemModal";
import UpdateMenuItemModal from "./sub-components/menu-item-management/UpdateMenuItemModal";

export default function DashMenuItems() {
  const { currentUser } = useSelector((state) => state.user);
  const [loading, setLoading] = useState(true);
  const [menuItems, setMenuItems] = useState([]);
  const [filteredMenuItems, setFilteredMenuItems] = useState([]);
  const [restaurant, setRestaurant] = useState(null);
  const [search, setSearch] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("");
  const [availabilityFilter, setAvailabilityFilter] = useState("");
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showViewModal, setShowViewModal] = useState(false);
  const [showUpdateModal, setShowUpdateModal] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [selectedMenuItem, setSelectedMenuItem] = useState(null);
  const [categories, setCategories] = useState([]);
  const [pageNumber, setPageNumber] = useState(0);
  const itemsPerPage = 8;

  const pageCount = Math.ceil(filteredMenuItems.length / itemsPerPage);
  const displayMenuItems = filteredMenuItems.slice(
    pageNumber * itemsPerPage,
    (pageNumber + 1) * itemsPerPage
  );

  const handlePageChange = ({ selected }) => {
    setPageNumber(selected);
  };

  // Fetch the restaurant associated with the current user
  useEffect(() => {
    if (currentUser?.token) {
      fetchUserRestaurant();
    }
  }, [currentUser]);

  // Fetch menu items when restaurant is loaded
  useEffect(() => {
    if (restaurant) {
      fetchMenuItems();
      fetchCategories();
    }
  }, [restaurant]);

  // Apply filters and search
  useEffect(() => {
    if (!menuItems.length) return;

    let results = [...menuItems];

    // Apply search
    if (search.trim()) {
      results = results.filter(
        (item) =>
          item.name.toLowerCase().includes(search.toLowerCase()) ||
          (item.description &&
            item.description.toLowerCase().includes(search.toLowerCase()))
      );
    }

    // Apply category filter
    if (categoryFilter) {
      results = results.filter((item) => item.categoryId === categoryFilter);
    }

    // Apply availability filter
    if (availabilityFilter === "available") {
      results = results.filter((item) => item.available);
    } else if (availabilityFilter === "unavailable") {
      results = results.filter((item) => !item.available);
    }

    setFilteredMenuItems(results);
    setPageNumber(0);
  }, [search, categoryFilter, availabilityFilter, menuItems]);

  const fetchUserRestaurant = async () => {
    try {
      const response = await restaurantService.getRestaurantsByUserId(
        currentUser.id,
        currentUser.token
      );

      if (response.ok) {
        const data = await response.json();
        if (data && data.length > 0) {
          setRestaurant(data[0]); // Assuming the user is associated with one restaurant
        } else {
          toast.error("You don't have any restaurants assigned to you.");
        }
      } else {
        toast.error("Failed to fetch restaurant information");
      }
    } catch (error) {
      toast.error(`Error: ${error.message}`);
    }
  };

  const fetchMenuItems = async () => {
    if (!restaurant?.id) return;

    setLoading(true);
    try {
      const response = await menuItemService.getMenuItemsByRestaurantId(
        restaurant.id,
        currentUser.token
      );
      if (response.ok) {
        const data = await response.json();
        setMenuItems(data);
        setFilteredMenuItems(data);
      } else {
        const errorData = await response.json();
        toast.warning(errorData.error || "No menu items found");
        setMenuItems([]);
        setFilteredMenuItems([]);
      }
    } catch (error) {
      toast.error(`Error fetching menu items: ${error.message}`);
      setMenuItems([]);
      setFilteredMenuItems([]);
    } finally {
      setLoading(false);
    }
  };

  // Then replace the fetchCategories function with this corrected version
  const fetchCategories = async () => {
    if (!restaurant?.id || !currentUser?.token) return;

    try {
      const response = await menuCategoryService.getCategoriesByRestaurantId(
        restaurant.id,
        currentUser.token
      );

      if (response.ok) {
        const data = await response.json();

        // If we have categories, use them
        if (data && data.length > 0) {
          setCategories(data);
        } else {
          // No categories found
          setCategories([]);
          toast.info(
            "No menu categories found for this restaurant. Create some categories to organize your menu."
          );
        }
      } else {
        // Handle error response
        const errorData = await response.json();
        console.error("Failed to fetch categories:", errorData);
        setCategories([]);
        toast.warning("Failed to load menu categories");
      }
    } catch (error) {
      console.error("Error fetching categories:", error);
      setCategories([]);
      toast.error("Error loading menu categories");
    }
  };
  const handleCreateMenuItem = () => {
    setShowCreateModal(true);
  };

  const handleViewMenuItem = (menuItem) => {
    setSelectedMenuItem(menuItem);
    setShowViewModal(true);
  };

  const handleEditMenuItem = (menuItem) => {
    setSelectedMenuItem(menuItem);
    setShowUpdateModal(true);
  };

  const handleDeleteMenuItem = (menuItem) => {
    setSelectedMenuItem(menuItem);
    setShowDeleteModal(true);
  };

  const handleMenuItemCreated = () => {
    setShowCreateModal(false);
    fetchMenuItems();
    toast.success("Menu item created successfully");
  };

  const handleMenuItemUpdated = () => {
    setShowUpdateModal(false);
    fetchMenuItems();
    toast.success("Menu item updated successfully");
  };

  const confirmDelete = async () => {
    if (!selectedMenuItem) return;

    try {
      const response = await menuItemService.deleteMenuItem(
        selectedMenuItem.id,
        currentUser.token
      );

      if (response.ok) {
        toast.success("Menu item deleted successfully");
        fetchMenuItems();
      } else {
        const errorData = await response.json();
        toast.error(errorData.error || "Failed to delete menu item");
      }
    } catch (error) {
      toast.error(`Error: ${error.message}`);
    } finally {
      setShowDeleteModal(false);
    }
  };

  const resetFilters = () => {
    setSearch("");
    setCategoryFilter("");
    setAvailabilityFilter("");
  };

  if (!restaurant) {
    return (
      <div className="p-6 flex justify-center items-center">
        <Card className="max-w-md mx-auto">
          <div className="text-center p-6">
            <FaUtensils className="mx-auto text-5xl text-gray-400 mb-4" />
            <h2 className="text-xl font-bold mb-2">No Restaurant Found</h2>
            <p className="text-gray-600 mb-4">
              You need to be associated with a restaurant to manage menu items.
            </p>
            <Button color="dark" href="/dashboard">
              Return to Dashboard
            </Button>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-7xl mx-auto">
      <ToastContainer position="top-right" autoClose={3000} />

      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-800 mb-2 flex items-center">
            <FaUtensils className="mr-3 text-blue-600" />
            Menu Items
          </h1>
          <p className="text-gray-600">
            Manage menu items for {restaurant.name}
          </p>
        </div>
        <Button
          gradientDuoTone="purpleToBlue"
          onClick={handleCreateMenuItem}
          className="mt-4 md:mt-0"
        >
          <HiPlusCircle className="mr-2 h-5 w-5" />
          Add Menu Item
        </Button>
      </div>

      {/* Filters */}
      <Card className="mb-6 overflow-hidden">
        <div className="p-4">
          <div className="flex flex-wrap items-center gap-4">
            <div className="flex-grow md:max-w-md">
              <TextInput
                type="text"
                icon={HiOutlineSearch}
                placeholder="Search menu items..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-full"
              />
            </div>

            <div className="flex flex-wrap gap-3">
              <div>
                <Dropdown
                  label={
                    <div className="flex items-center">
                      <HiFilter className="mr-2" />
                      {categoryFilter
                        ? `Category: ${
                            categories.find((c) => c.id === categoryFilter)
                              ?.name || "Selected"
                          }`
                        : "Filter by Category"}
                    </div>
                  }
                  color="light"
                  size="sm"
                >
                  <Dropdown.Item onClick={() => setCategoryFilter("")}>
                    All Categories
                  </Dropdown.Item>
                  <Dropdown.Divider />
                  {categories.map((category) => (
                    <Dropdown.Item
                      key={category.id}
                      onClick={() => setCategoryFilter(category.id)}
                    >
                      {category.name}
                    </Dropdown.Item>
                  ))}
                </Dropdown>
              </div>

              <div>
                <Dropdown
                  label={
                    <div className="flex items-center">
                      <HiFilter className="mr-2" />
                      {availabilityFilter === "available"
                        ? "Available Items"
                        : availabilityFilter === "unavailable"
                        ? "Unavailable Items"
                        : "Filter by Availability"}
                    </div>
                  }
                  color="light"
                  size="sm"
                >
                  <Dropdown.Item onClick={() => setAvailabilityFilter("")}>
                    All Items
                  </Dropdown.Item>
                  <Dropdown.Divider />
                  <Dropdown.Item
                    onClick={() => setAvailabilityFilter("available")}
                  >
                    Available Only
                  </Dropdown.Item>
                  <Dropdown.Item
                    onClick={() => setAvailabilityFilter("unavailable")}
                  >
                    Unavailable Only
                  </Dropdown.Item>
                </Dropdown>
              </div>

              {(search || categoryFilter || availabilityFilter) && (
                <Button color="light" size="sm" onClick={resetFilters}>
                  Clear Filters
                </Button>
              )}
            </div>
          </div>
        </div>
      </Card>

      {/* Menu Items Table */}
      {loading ? (
        <div className="flex justify-center py-12">
          <Spinner size="xl" />
        </div>
      ) : (
        <Card>
          {filteredMenuItems.length > 0 ? (
            <>
              <Table hoverable striped>
                <Table.Head>
                  <Table.HeadCell>Image</Table.HeadCell>
                  <Table.HeadCell>Name</Table.HeadCell>
                  <Table.HeadCell>Price</Table.HeadCell>
                  <Table.HeadCell>Category</Table.HeadCell>
                  <Table.HeadCell>Status</Table.HeadCell>
                  <Table.HeadCell>Actions</Table.HeadCell>
                </Table.Head>
                <Table.Body className="divide-y">
                  {displayMenuItems.map((item) => (
                    <Table.Row
                      key={item.id}
                      className="bg-white dark:border-gray-700 dark:bg-gray-800"
                    >
                      <Table.Cell>
                        <div className="h-14 w-14 rounded-md overflow-hidden">
                          {item.imageUrl ? (
                            <img
                              src={item.imageUrl}
                              alt={item.name}
                              className="h-full w-full object-cover"
                            />
                          ) : (
                            <div className="h-full w-full bg-gray-200 flex items-center justify-center">
                              <FaUtensils className="text-gray-400" />
                            </div>
                          )}
                        </div>
                      </Table.Cell>
                      <Table.Cell className="font-medium text-gray-900 dark:text-white">
                        {item.name}
                      </Table.Cell>
                      <Table.Cell>${item.price.toFixed(2)}</Table.Cell>
                      <Table.Cell>
                        {categories.find((c) => c.id === item.categoryId)
                          ?.name || "Uncategorized"}
                      </Table.Cell>
                      <Table.Cell>
                        {item.available ? (
                          <Badge color="success" className="px-3 py-1.5">
                            Available
                          </Badge>
                        ) : (
                          <Badge color="failure" className="px-3 py-1.5">
                            Unavailable
                          </Badge>
                        )}
                      </Table.Cell>
                      <Table.Cell>
                        <div className="flex gap-2">
                          <Button
                            size="xs"
                            color="info"
                            onClick={() => handleViewMenuItem(item)}
                          >
                            <HiOutlineEye className="mr-1" />
                            View
                          </Button>
                          <Button
                            size="xs"
                            color="success"
                            onClick={() => handleEditMenuItem(item)}
                          >
                            <HiOutlinePencilAlt className="mr-1" />
                            Edit
                          </Button>
                          <Button
                            size="xs"
                            color="failure"
                            onClick={() => handleDeleteMenuItem(item)}
                          >
                            <HiOutlineTrash className="mr-1" />
                            Delete
                          </Button>
                        </div>
                      </Table.Cell>
                    </Table.Row>
                  ))}
                </Table.Body>
              </Table>

              {/* Pagination */}
              {pageCount > 1 && (
                <div className="flex items-center justify-center p-4">
                  <ReactPaginate
                    previousLabel={"Previous"}
                    nextLabel={"Next"}
                    pageCount={pageCount}
                    onPageChange={handlePageChange}
                    forcePage={pageNumber}
                    containerClassName="flex justify-center items-center space-x-1"
                    pageClassName="inline-flex"
                    pageLinkClassName="px-3 py-2 leading-tight text-gray-500 bg-white border border-gray-300 hover:bg-gray-100 hover:text-gray-700"
                    previousLinkClassName="px-3 py-2 leading-tight text-gray-500 bg-white border border-gray-300 rounded-l-lg hover:bg-gray-100 hover:text-gray-700"
                    nextLinkClassName="px-3 py-2 leading-tight text-gray-500 bg-white border border-gray-300 rounded-r-lg hover:bg-gray-100 hover:text-gray-700"
                    activeLinkClassName="px-3 py-2 text-blue-600 border border-blue-300 bg-blue-50 hover:bg-blue-100 hover:text-blue-700"
                    disabledLinkClassName="opacity-50 cursor-not-allowed"
                  />
                </div>
              )}
            </>
          ) : (
            <div className="flex flex-col items-center justify-center py-12">
              <FaUtensils className="text-gray-400 text-5xl mb-4" />
              <h3 className="text-xl font-semibold text-gray-700 mb-2">
                No menu items found
              </h3>
              <p className="text-gray-500 mb-6">
                {search || categoryFilter || availabilityFilter
                  ? "No items match your filter criteria"
                  : "Start by adding menu items to your restaurant"}
              </p>
              {(search || categoryFilter || availabilityFilter) && (
                <Button color="light" onClick={resetFilters}>
                  Clear Filters
                </Button>
              )}
            </div>
          )}
        </Card>
      )}

      {/* Create Menu Item Modal */}
      <CreateMenuItemModal
        show={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onSuccess={handleMenuItemCreated}
        restaurantId={restaurant.id}
        token={currentUser.token}
        categories={categories}
      />

      {/* View Menu Item Modal */}
      <ViewMenuItemModal
        show={showViewModal}
        onClose={() => setShowViewModal(false)}
        menuItem={selectedMenuItem}
        categoryName={
          selectedMenuItem &&
          categories.find((c) => c.id === selectedMenuItem?.categoryId)?.name
        }
      />

      {/* Delete Menu Item Modal */}
      <DeleteMenuItemModal
        show={showDeleteModal}
        onClose={() => setShowDeleteModal(false)}
        menuItem={selectedMenuItem}
        onConfirm={confirmDelete}
        isDeleting={false} // You could add a loading state for deletion if desired
      />

      <UpdateMenuItemModal
        show={showUpdateModal}
        onClose={() => setShowUpdateModal(false)}
        onSuccess={handleMenuItemUpdated}
        menuItem={selectedMenuItem}
        token={currentUser.token}
        categories={categories}
      />
    </div>
  );
}
