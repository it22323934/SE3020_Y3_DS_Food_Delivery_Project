import {
  Badge,
  Button,
  Modal,
  Select,
  Spinner,
  Table,
  TextInput,
} from "flowbite-react";
import { useEffect, useState } from "react";
import {
  HiEye,
  HiOutlineExclamationCircle,
  HiOutlineUserGroup,
  HiOutlineX,
} from "react-icons/hi";
import { useSelector } from "react-redux";
import {
  FaCheck,
  FaCheckCircle,
  FaClipboardList,
  FaTimes,
  FaTimesCircle,
} from "react-icons/fa";
import ReactSelect from "react-select";
import { ToastContainer, toast } from "react-toastify";
import { authService } from "../service/authService";
import { RiGovernmentLine } from "react-icons/ri";
import { AiOutlineSearch } from "react-icons/ai";
import LoadingSpinner from "./LoadingSpinner";
import ReactPaginate from "react-paginate";
import { UserDetailsModal } from "./sub-components/user-managment/UserDetailsModal";
import { CreateUserModal } from "./sub-components/user-managment/CreateUserModal";
import { UpdateUserModal } from "./sub-components/user-managment/UpdateUserModal";
import { use } from "react";

export default function DashUserProfiles() {
  const { currentUser } = useSelector((state) => state.user);
  const [users, setUsers] = useState([]);
  const [filteredUsers, setFilteredUsers] = useState([]);
  const [showMore, setShowMore] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [userIdToDelete, setUserIdToDelete] = useState("");
  const [isDeleting, setIsDeleting] = useState(false);
  const [loading, setLoading] = useState(true);
  const [isDownloading, setIsDownloading] = useState(false);
  const [totalUsers, setTotalUser] = useState(0);
  const [totalActiveUsers, setTotalActiveUsers] = useState(0);
  const [totalInActiveUsers, setTotalInActiveUsers] = useState(0);
  const [userToShow, setUserToShow] = useState({});
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedUserForReport, setSelectedUserForReport] = useState(null);
  const [userSelectOptions, setUserSelectOptions] = useState([]);
  const [roleOptions, setRoleOptions] = useState([]);
  const [selectedRole, setSelectedRole] = useState(null);

  const fetchUser = async () => {
    try {
      const res = await authService.getAllUsers(currentUser.token);
      const data = await res.json();
      if (res.ok) {
        setUsers(data);
        setFilteredUsers(data);
        // Calculate stats
        setTotalUser(data.length);
        const enabledUsers = data.filter((user) => user.enabled);
        setTotalActiveUsers(enabledUsers.length);
        const disabledUsers = data.filter((user) => user.disabled);
        setTotalInActiveUsers(disabledUsers.length);
      }
      if (res.status === 401) {
        toast.error(data.message || "Unauthorized access");
        return;
      }
      if (res.status === 500) {
        toast.error(data.message || "Internal server error");
        return;
      }
      if (res.status === 400) {
        toast.error(data.message || "Bad request");
        return;
      }
    } catch (error) {
      console.log(error.message);
    }
  };

  const handleSearch = (e) => {
    const term = e.target.value;
    setSearchTerm(term);

    // Reset pagination when searching
    setPageNumber(0);

    if (term.trim() === "") {
      setFilteredUsers(users);
    } else {
      const filtered = users.filter(
        (user) =>
          user.username?.toLowerCase().includes(term.toLowerCase()) ||
          user.email?.toLowerCase().includes(term.toLowerCase()) ||
          user.phoneNumber?.toLowerCase().includes(term.toLowerCase())
      );
      setFilteredUsers(filtered);
    }
  };

  const handleDownloadReport = async () => {
    if (!selectedUserForReport) {
      toast.error("Please select a user first");
      return;
    }

    setIsDownloading(true);
    try {
      // Implement your download logic here
      const user = users.find((u) => u.id === selectedUserForReport.value);
      if (user) {
        // Call your API to generate report
        // const response = await authService.generateUserReport(user.id, currentUser.token);
        // For now, just simulate a download delay
        await new Promise((resolve) => setTimeout(resolve, 1500));
        toast.success(`Report for ${user.username} downloaded successfully`);
      }
    } catch (error) {
      toast.error(
        "Failed to download report: " + (error.message || "Unknown error")
      );
    } finally {
      setIsDownloading(false);
    }
  };

  // Add this function to extract unique roles from users
  const extractUniqueRoles = (usersArray) => {
    const allRoles = new Set();

    usersArray.forEach((user) => {
      if (user.roles && Array.isArray(user.roles)) {
        user.roles.forEach((role) => {
          allRoles.add(role.replace("ROLE_", ""));
        });
      }
    });

    return Array.from(allRoles).map((role) => ({
      value: role,
      label: role,
    }));
  };

  // Add this function to filter users by selected role
  const filterUsersByRole = (role) => {
    if (!role) {
      // If no role selected, show all users in select
      return users.map((user) => ({
        value: user.id,
        label: `${user.id} - ${user.username}`,
      }));
    }

    // Filter users that have the selected role
    const filteredByRole = users.filter(
      (user) =>
        user.roles &&
        user.roles.some(
          (userRole) => userRole.replace("ROLE_", "") === role.value
        )
    );

    return filteredByRole.map((user) => ({
      value: user.id,
      label: `${user.id} - ${user.username}`,
    }));
  };

  const handleRoleChange = (option) => {
    setSelectedRole(option);
    setSelectedUserForReport(null);
  };

  useEffect(() => {
    if (currentUser?.roles?.[0] === "ROLE_ADMIN") {
      fetchUser();
      setLoading(false);
    }
    if (users.length > 0) {
      const roleOpts = extractUniqueRoles(users);
      setRoleOptions(roleOpts);

      const userOpts = filterUsersByRole(selectedRole);
      setUserSelectOptions(userOpts);
    }
  }, [currentUser, selectedRole]);

  const [pageNumber, setPageNumber] = useState(0);
  const userPerPage = 5;

  const pageCount = Math.ceil(filteredUsers.length / userPerPage);

  const handlePageChange = ({ selected }) => {
    setPageNumber(selected);
  };

  const handleViewMore = (user) => {
    setShowMore(true);
    setUserToShow(user);
    setShowModal(true);
  };

  const handleUserCreated = () => {
    fetchUser();
  };

  const [selectedUser, setSelectedUser] = useState(null);
  const [showUpdateModal, setShowUpdateModal] = useState(false);

  const handleUpdateClick = (user) => {
    setSelectedUser(user);
    setShowUpdateModal(true);
  };

  const handleUserUpdated = () => {
    fetchUser();
    setShowUpdateModal(false);
    setSelectedUser(null);
  };

  const displayUsers = filteredUsers
    .slice(pageNumber * userPerPage, (pageNumber + 1) * userPerPage)
    .map((user) => (
      <Table.Body className="divide-y" key={user.id}>
        <Table.Row className="bg-white dark:border-gray-700 dark:bg-gray-800">
          <Table.Cell>{user.username}</Table.Cell>
          <Table.Cell>{user.email}</Table.Cell>
          <Table.Cell>{user.phoneNumber || "N/A"}</Table.Cell>
          <Table.Cell>
            {user.roles?.map((role) => (
              <Badge key={role} color="info" className="mr-1">
                {role.replace("ROLE_", "")}
              </Badge>
            ))}
          </Table.Cell>
          <Table.Cell>
            <Badge
              color={user.enabled ? "success" : "failure"}
              className="flex items-center justify-center px-3 py-2 rounded-lg"
            >
              {user.enabled ? (
                <FaCheckCircle color="green" size={16} className="mr-1" />
              ) : (
                <FaTimesCircle color="red" size={16} className="mr-1" />
              )}
            </Badge>
          </Table.Cell>
          <Table.Cell>
            <div className="flex items-center space-x-4">
              <Button
                color="gray"
                size="sm"
                onClick={() => handleViewMore(user)}
              >
                <HiEye className="mr-2 h-5 w-5" />
                View More
              </Button>
              <Button
                color="green"
                size="sm"
                outline
                onClick={() => handleUpdateClick(user)}
              >
                <FaClipboardList className="mr-2 h-5 w-5" />
                Update
              </Button>
            </div>
          </Table.Cell>
        </Table.Row>
      </Table.Body>
    ));

  return (
    <div className="table-auto overflow-x-scroll md:mx-auto p-3 scrollbar scrollbar-track-slate-100 scrollbar-thumb-slate-300 dark:scrollbar-track-slate-700 dark:scrollbar-thumb-slate-500">
      <ToastContainer />
      {loading ? (
        <LoadingSpinner />
      ) : (
        <>
          <div className="p-3 md:mx-auto">
            <div className="flex-wrap flex gap-4 justify-center">
              <div className="flex flex-col p-3 dark:bg-slate-800 gap-4 md:w-72 w-full rounded-md shadow-md">
                <div className="flex justify-between">
                  <div className="">
                    <h3 className="text-gray-500 text-md uppercase">
                      Total Users
                    </h3>
                    <p className="text-2xl">{totalUsers}</p>
                  </div>
                  <HiOutlineUserGroup className="bg-yellow-500 text-white text-5xl p-3 shadow-lg" />
                </div>
              </div>
              <div className="flex flex-col p-3 dark:bg-slate-800 gap-4 md:w-72 w-full rounded-md shadow-md">
                <div className="flex justify-between">
                  <div className="">
                    <h3 className="text-gray-500 text-md uppercase">
                      Total Enabled Users
                    </h3>
                    <p className="text-2xl">{totalActiveUsers}</p>
                  </div>
                  <HiOutlineUserGroup className="bg-green-500 text-white text-5xl p-3 shadow-lg" />
                </div>
              </div>
              <div className="flex flex-col p-3 dark:bg-slate-800 gap-4 md:w-72 w-full rounded-md shadow-md">
                <div className="flex justify-between">
                  <div className="">
                    <h3 className="text-gray-500 text-md uppercase">
                      Total Disabled Users
                    </h3>
                    <p className="text-2xl">{totalInActiveUsers}</p>
                  </div>
                  <HiOutlineUserGroup className="bg-red-500 text-white text-5xl p-3 shadow-lg" />
                </div>
              </div>
            </div>
          </div>
          <div>
            <div className="flex items-center mb-2">
              <Button
                outline
                gradientDuoTone="greenToBlue"
                className="ml-4 mr-5"
                onClick={() => setShowCreateModal(true)}
              >
                Add New User
              </Button>
              <TextInput
                type="text"
                placeholder="Search by name, email or phone"
                value={searchTerm}
                onChange={handleSearch}
                rightIcon={AiOutlineSearch}
                className="ml-1 bg-gray-50 border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-80 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500 mb"
              />

              <div className="flex items-center mb-2">
                {/* Role selector */}
                <div className="ml-4 w-48">
                  <ReactSelect
                    placeholder="Filter by role"
                    value={selectedRole}
                    onChange={handleRoleChange}
                    options={roleOptions}
                    isSearchable
                    isClearable
                    className="react-select-container"
                    classNamePrefix="react-select"
                    styles={{
                      control: (baseStyles) => ({
                        ...baseStyles,
                        backgroundColor: "white",
                        borderColor: "#D1D5DB",
                      }),
                      option: (baseStyles, { isFocused }) => ({
                        ...baseStyles,
                        backgroundColor: isFocused ? "#E5E7EB" : "white",
                        color: "black",
                      }),
                    }}
                  />
                </div>

                {/* User selector */}
                <div className="ml-4 w-64">
                  <ReactSelect
                    placeholder="Select a user for report"
                    value={selectedUserForReport}
                    onChange={(option) => setSelectedUserForReport(option)}
                    options={users.map((user) => ({
                      value: user.id,
                      label: `${user.id} - ${user.username}`,
                    }))}
                    isSearchable
                    isClearable
                    className="react-select-container"
                    classNamePrefix="react-select"
                    styles={{
                      control: (baseStyles) => ({
                        ...baseStyles,
                        backgroundColor: "white",
                        borderColor: "#D1D5DB",
                      }),
                      option: (baseStyles, { isFocused }) => ({
                        ...baseStyles,
                        backgroundColor: isFocused ? "#E5E7EB" : "white",
                        color: "black",
                      }),
                    }}
                  />
                </div>
              </div>

              <Button
                outline
                gradientDuoTone="greenToBlue"
                className="ml-4"
                onClick={handleDownloadReport}
                disabled={!selectedUserForReport || isDownloading}
              >
                {isDownloading ? (
                  <Spinner className="animate-spin" color="white" size="sm" />
                ) : (
                  "Download User Report"
                )}
              </Button>
            </div>
          </div>
          <div className="overflow-x-auto">
            {filteredUsers.length > 0 ? (
              <Table>
                <Table.Head>
                  <Table.HeadCell>Name</Table.HeadCell>
                  <Table.HeadCell>Email</Table.HeadCell>
                  <Table.HeadCell>Phone Number</Table.HeadCell>
                  <Table.HeadCell>Role</Table.HeadCell>
                  <Table.HeadCell>Status</Table.HeadCell>
                  <Table.HeadCell>Actions</Table.HeadCell>
                </Table.Head>
                {displayUsers}
              </Table>
            ) : (
              <p className="text-center py-4">No Users Available</p>
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

          {/* View More Modal */}
          <UserDetailsModal
            showModal={showModal}
            setShowModal={setShowModal}
            user={userToShow}
          />

          {/* Create user component */}
          <CreateUserModal
            show={showCreateModal}
            onClose={() => setShowCreateModal(false)}
            onSuccess={handleUserCreated}
            token={currentUser.token}
          />

          {/* Update User */}
          <UpdateUserModal
            show={showUpdateModal}
            onClose={() => setShowUpdateModal(false)}
            onSuccess={handleUserUpdated}
            userData={selectedUser}
            token={currentUser.token}
          />
        </>
      )}
    </div>
  );
}
